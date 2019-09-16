package house;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.protobuf.Any;

import beans.Measurement;
import proto.HouseProto.AskBoost;
import proto.HouseProto.EndBoost;
import proto.HouseProto.Goodbye;
import proto.HouseProto.Measure;
import proto.HouseProto.MeasureList;
import simulator.SmartMeterSimulator;
import utils.Async;
import utils.Http;

public class HouseManager {

	private Map<String, HouseConnection> connections = new HashMap<>();
	private Map<String, Measurement> measures = new HashMap<>();

	private int houseCount = 1;
	private int measureCount = 0;

	private String localId;
	private String master;

	private boolean ready = false;

	private ServerSocket socket;
	private int port;

	public static final int BOOST_INACTIVE = 0;
	public static final int BOOST_REQUESTED = 2;
	public static final int BOOST_ACTIVE = 3;

	private int boost = 0;
	private long boostTimestamp = 0;
	
	private Map<String, Boolean> boostStatus;
	private Set<String> pendingBoost;
	
	private SmartMeterSimulator sim;
	private Http http;

	public HouseManager(String localId, Http http) throws IOException {
		this(localId, 0, http);
	}

	public HouseManager(String localId, int port, Http http) throws IOException {
		this.localId = localId;
		this.master = localId;
		this.http = http;

		measures.put(localId, new Measurement(localId, Double.NaN, 0));

		startWait(port);
	}

	private void startWait(int port) throws IOException {
		socket = new ServerSocket(port);
		this.port = socket.getLocalPort();
		System.out.println("Listening on " + this.port);

		Async.run(() -> {
			while(!socket.isClosed()) {
				try {
					Socket conn = socket.accept();
					new HouseConnection(this, conn);
				} catch (SocketException e) {
					
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});

	}
	
	public void setSimultor(SmartMeterSimulator sim) {
		this.sim = sim;
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
			http.post("stats/add", Arrays.asList(m));
		});

	}

	public synchronized void add(HouseConnection house) {
		connections.put(house.getId(), house);
		
		if (!measures.containsKey(house.getId())) {
			measures.put(house.getId(), new Measurement(house.getId(), Double.NaN, 0));
		}

		if (house.getId().compareTo(localId) < 0) {
			master = house.getId();
		}
		houseCount++;
	}

	public synchronized void remove(String id) {
		assert (measures.containsKey(id));
		
		setBoost(id, false);

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

		Measurement old = new Measurement(m.getId(), Double.NaN, 0);
		int delta = 1;

		if (measures.containsKey(m.getId())) {
			old = measures.get(m.getId());
			delta = Double.isNaN(old.getValue()) ? 1 : -1;
		}

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

			double value = 0;
			long timestamp = 0;

			List<Measurement> ms = getMeasurements();

			MeasureList.Builder listBuilder = MeasureList.newBuilder();
			for (Measurement m: ms) {
				assert (!Double.isNaN(m.getValue()));

				timestamp = Math.max(timestamp, m.getTimestamp());
				value += m.getValue();

				Measurement m2 = new Measurement(m.getId(), Double.NaN, m.getTimestamp());
				measures.put(m2.getId(), m2);

				listBuilder.addMeasures(m2.toProtobuf());
			}
			measureCount = 0;

			List<Measurement> total = Arrays.asList(new Measurement("", value, timestamp));

			Any msg = Any.pack(listBuilder.build());
			Async.run(() -> {

				http.post("stats/add", total);

				for (HouseConnection conn: connections.values()) {
					conn.write(msg);
				}

			});

		}
	}

	public synchronized void stop() {
		
		sim.stopMeGently();

		http.delete("houses/remove/"+localId);
		
		for (HouseConnection conn: new ArrayList<>(connections.values())) {
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
	
	public synchronized long getBoostTimestamp() {
		return boostTimestamp;
	}

	public synchronized boolean isBoostActive() {
		return boost == BOOST_ACTIVE;
	}

	public synchronized int getBoostStatus() {
		return boost;
	}

	public synchronized void boost() {
		if (boost > BOOST_INACTIVE) return;

		System.out.println("waiting for boost approval");
		
		boost = BOOST_REQUESTED;
		boostTimestamp = System.currentTimeMillis();
		
		pendingBoost = new HashSet<>(connections.keySet());
		boostStatus = new HashMap<>();
		
		for (HouseConnection conn: connections.values()) {
			boostStatus.put(conn.getId(), false);
			conn.write(
				Any.pack(AskBoost.newBuilder()
					.setTimestamp(boostTimestamp)
					.build()
				)
			);
		}
		
		canBoost();
	}
	
	public synchronized void canBoost() {
		assert(boost == BOOST_REQUESTED);
		
		if (!pendingBoost.isEmpty()) return;
		
		int count = 0;
		for (Boolean active: boostStatus.values()) {
			if (active) count++;
		}
		
		if (count < 2) {
			
			Async.run(() -> {
				System.out.println("boost started");
				startBoost();
					
				System.out.println("boost ended");
				stopBoost();
			});
			
		}  else {
			System.out.println("too many houses boosting, waiting");
		}
	}
	
	private void startBoost() {
		boost = BOOST_ACTIVE;
		
		http.get("houses/boost/start/"+localId);
		try {
			sim.boost();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private synchronized void stopBoost() {
		assert(boost == BOOST_ACTIVE);
		
		boost = BOOST_INACTIVE;
		http.get("houses/boost/stop/"+localId);
		
		for (HouseConnection conn: connections.values()) {
			conn.write(Any.pack(EndBoost.newBuilder().build()));
		}
		
		
	}
	
	public synchronized void setBoost(String id, boolean val) {
		if (boost != BOOST_REQUESTED) return;

		pendingBoost.remove(id);
		boostStatus.put(id, val);
		
		canBoost();
	}
}
