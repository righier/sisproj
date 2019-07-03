package house;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.google.protobuf.Any;

import beans.Measurement;
import proto.HouseProto.Goodbye;
import proto.HouseProto.Measure;
import proto.HouseProto.MeasureList;
import utils.Async;
import utils.ClientPool;

public class HouseManager {

	private Map<String, HouseConnection> connections = new HashMap<String, HouseConnection>();
	private Map<String, Measurement> measures = new HashMap<String, Measurement>();

	private int houseCount;
	private int measureCount;

	private String localId;
	private String master;

	private boolean ready;

	private ServerSocket socket;
	private int port;

	public HouseManager(String localId) throws IOException {
		this(localId, 0);
	}

	public HouseManager(String localId, int port) throws IOException {
		this.localId = localId;

		measures.put(localId, new Measurement(localId, Double.NaN, 0));
		master = localId;
		houseCount = 1;
		measureCount = 0;

		ready = false;

		startWait(port);
	}

	private void startWait(int port) throws IOException {
		socket = new ServerSocket(port);
		this.port = socket.getLocalPort();
		System.out.println("Listening on " + this.port);
		Async.run(() -> {
			while(true) {
				try {
					Socket conn = socket.accept();
					new HouseConnection(this, conn);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});

	}

	public void addMeasurement(Measurement m) {
		update(m);

		Any msg = Any.pack(MeasureList.newBuilder().addMeasures(Measure.newBuilder()
			.setId(m.getId())
			.setValue(m.getValue())
			.setTimestamp(m.getTimestamp()))
			.build());

		for (HouseConnection house: connections.values()) {
			house.write(msg);
		}

		Async.run(() -> {
			Client client = ClientPool.get();
			Response response = client
				.target(ClientPool.getUrl())
				.path("stats/add")
				.request(MediaType.APPLICATION_JSON)
				.post(Entity.entity(Arrays.asList(m), MediaType.APPLICATION_JSON));
			ClientPool.add(client);
		});

	}

	public synchronized void add(HouseConnection house) {
		connections.put(house.getId(), house);
		measures.put(house.getId(), new Measurement(house.getId(), Double.NaN, 0));
		if (house.getId().compareTo(localId) < 0) {
			master = house.getId();
		}
		houseCount++;
	}

	public synchronized void remove(String id) {
		assert (measures.containsKey(id));

		Measurement m = measures.get(id);
		if (!Double.isNaN(m.getValue())) {
			measureCount--;
		}

		connections.remove(id);
		measures.remove(id);
		if (id.equals(master)) {
			master = localId;
			for (String house : connections.keySet()) {
				if (house.compareTo(master) < 0) {
					master = house;
				}
			}
		}
		houseCount--;

		sendSum();
	}

	public synchronized void update(Measurement m) {
		assert (measures.containsKey(m.getId()));

		Measurement old = measures.get(m.getId());
		int delta = Double.isNaN(old.getValue()) ? 1 : -1;

		if (old.compareTo(m) < 0) {
			measures.put(m.getId(), m);
			delta += Double.isNaN(m.getValue()) ? -1 : 1;
			measureCount += delta / 2;

			sendSum();
		}
	}

	public synchronized List<Measurement> getMeasurements() {
		return new ArrayList<Measurement>(measures.values());
	}

	public synchronized void sendSum() {

		if (ready && isMaster() && houseCount == measureCount) {

			System.out.println("sum "+localId);

			double value = 0;
			long timestamp = 0;
			int count = 0;

			List<Measurement> ms = getMeasurements();

			MeasureList.Builder listBuilder = MeasureList.newBuilder();
			for (Measurement m: ms) {
				assert (!Double.isNaN(m.getValue()));

				timestamp = Math.max(timestamp, m.getTimestamp());
				value += m.getValue();
				count++;

				Measurement m2 = new Measurement(m.getId(), Double.NaN, m.getTimestamp());
				measures.put(m2.getId(), m2);

				Measure measure = Measure.newBuilder().setId(m2.getId()).setValue(m2.getValue())
						.setTimestamp(m2.getTimestamp()).build();
				listBuilder.addMeasures(measure);
			}
			measureCount = 0;

			value /= count;

			List<Measurement> total = Arrays.asList(new Measurement("", value, timestamp));

			Any msg = Any.pack(listBuilder.build());
			Async.run(() -> {

				Client client = ClientPool.get();
				Response response = client.target(ClientPool.getUrl())
						.path("stats/add")
						.request(MediaType.APPLICATION_JSON)
						.post(Entity.entity(total, MediaType.APPLICATION_JSON));
				ClientPool.add(client);

				for (HouseConnection conn: connections.values()) {
					conn.write(msg);
				}

			});

		}
	}

	public synchronized void stop() {

		Client client = ClientPool.get();
		Response response = client.target(ClientPool.getUrl())
			.path("houses/remove/"+localId)
			.request(MediaType.APPLICATION_JSON)
			.delete();
		ClientPool.add(client);

		for (HouseConnection conn: connections.values()) {
			Any msg = Any.pack(Goodbye.newBuilder().build());
			conn.write(msg);
			conn.kill();
		}

		try {
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public synchronized boolean isMaster() {
		return master.equals(localId);
	}

	public synchronized void setReady(boolean value) {
		this.ready = value;
	}

	public synchronized boolean isReady() {
		return ready;
	}

	public String getLocalId() {
		return localId;
	}

	public int getPort() {
		return port;
	}
}
