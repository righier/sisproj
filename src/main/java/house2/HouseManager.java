package house2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import beans.Measurement;
import house2.SmartMeterBuffer;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import proto.HouseProto.Empty;
import proto.HouseProto.Identifier;
import proto.HouseProto.MasterResponse;
import proto.HouseProto.Measure;
import simulator.Buffer;
import simulator.SmartMeterSimulator;
import utils.Async.ThreadPool;
import utils.Async.Worker;
import utils.Http;
import utils.StreamHelper;

public class HouseManager {

	final private ThreadPool async = new ThreadPool(0);
	final private Map<String, HouseChannel> channels = new HashMap<>();
	final private Map<String, Measurement> measures = new HashMap<>();

	final private String id;
	final private String ip;
	final private int port;

	final private Server listener;
	final private ServiceProvider provider;
	final private SmartMeterBuffer meterBuffer;
	final private SmartMeterSimulator simulator;
	final private Http server;

	private boolean master = false;
	private boolean stopping = false;
	
	private int measureCount = 0;

	public HouseManager(String id, String ip, int port, String serverAddr, boolean runSimulator) {
		this.id = id;
		this.ip = ip;

		meterBuffer = new SmartMeterBuffer(this);
		simulator = new SmartMeterSimulator(id, (Buffer) meterBuffer);
		if (runSimulator) simulator.start();

		provider = new ServiceProvider(this);
		listener = ServerBuilder.forPort(port).addService(provider).build();
		try {
			listener.start();
		} catch (IOException e) {
			System.err.println("Failed to start HouseService");
			e.printStackTrace();
			System.exit(1);
		}

		this.port = listener.getPort();
		
		server = new Http(serverAddr);
		

	}

	public synchronized boolean isMaster() {
		return master;
	}

	public synchronized void setMaster(boolean master) {
		this.master = master;
	}

	public String getLocalId() {
		return id;
	}

	public int getPort() {
		return port;
	}
	
	public String getIp() {
		return ip;
	}

	public void newLocalMeasurement(Measurement m) {
		update(m);

		// sends measurement to every node
		for (HouseChannel channel: getChannels()) {
			StreamObserver<Measure> s = channel.async().setMeasurements(StreamHelper.ignore());
			s.onNext(m.toProtobuf());
			s.onCompleted();
		}

		// sends measurement to rest server
		async.run(() -> {
			server.post("stats/add", Arrays.asList(m));
		});
	}

	public synchronized void addHouse(HouseChannel c) {
		String key = c.getId();
		channels.put(key, c);
		
		if (!measures.containsKey(key)) {
			measures.put(key, new Measurement(key, Double.NaN, 0));
		}
	}

	public synchronized void removeHouse(String id) {
		channels.remove(id);
		Measurement m = measures.remove(id);
		if (m != null && !m.isNan()) {
			measureCount--;
		}
		
		sendSum();
	}

	public synchronized void update(Measurement m) {
		Measurement old = measures.get(m.getId());
		if (old == null) return;
		
		if (old.compareTo(m) < 0) {
			measures.put(m.getId(), m);
			
			// checks if the number of measurements increased or decreased, then updates measureCount
			measureCount += (m.isNan() ? 0 : 1) - (old.isNan() ? 0 : 1);

			sendSum();
		}
	}
	
	public synchronized void sendSum() {
		
		if (!master || stopping || channels.size() != measureCount) return;
		
		double value = 0;
		long timestamp = 0;
		int count = 0;
		
		List<Measure> msgs = new ArrayList<>();
		
		for (Measurement m: getMeasurements()) {
			timestamp = Math.max(timestamp, m.getTimestamp());
			value += m.getValue();
			count++;
			
			Measurement m2 = new Measurement(m.getId(), Double.NaN, m.getTimestamp());
			measures.put(m2.getId(), m2);
			msgs.add(m2.toProtobuf());
		}
		
		measureCount = 0;
		
		Measurement avg = new Measurement("", value / count, timestamp);
		
		
		
		// TODO
		
	}

	public synchronized List<Measurement> getMeasurements() {
		return new ArrayList<Measurement>(measures.values());
	}
	
	public synchronized HouseChannel getChannel(String id) {
		return channels.get(id);
	}
	
	public synchronized List<HouseChannel> getChannels() {
		return new ArrayList<HouseChannel>(channels.values());
	}

	public synchronized boolean isStopping() {
		return stopping;
	}
	
	public void stop() {
		synchronized(this) {
			stopping = true;
		}
		
		if (!(Thread.currentThread() instanceof Worker)) {
			System.err.println("HouseManager.close() cannot be called inside a worker");
			return;
		}
		
		simulator.stopMeGently();
		
		// if we are the master node we need to elect a new master
		if(isMaster()) {
			
			List<HouseChannel> candidates = getChannels();
			while(!candidates.isEmpty()) {
				HouseChannel c = candidates.remove(candidates.size());
				try {
					MasterResponse response = c.blocking().newMaster(Empty.getDefaultInstance());
					if (response.getAccept()) break;
				} catch (StatusRuntimeException e) {
					e.printStackTrace();
				}
			}
			
		}
		
		//TODO what to do if you have boost?

		// notifies other node that we are leaving and close the channels
		Identifier msg = Identifier.newBuilder().setId(id).build();
		for (HouseChannel channel: getChannels()) {
			async.run(() -> {
				try {
					channel.blocking().leaveNetwork(Identifier.newBuilder().setId(id).build());
				} catch(StatusRuntimeException e) {
					e.printStackTrace();
				}
				channel.shutdown();
			});
		}

		// waits until every connection is closed
		async.waitAllIdle();

		// stop listening for remote calls
		listener.shutdown();
		while(!listener.isTerminated()) {
			try {
				listener.awaitTermination();
			} catch (InterruptedException e) {}
		}
	}

}
