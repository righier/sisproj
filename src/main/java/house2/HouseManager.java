package house2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

import beans.House;
import beans.Measurement;
import house2.SmartMeterBuffer;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import proto.HouseServiceGrpc;
import proto.HouseProto.BoostRequest;
import proto.HouseProto.BoostResponse;
import proto.HouseProto.Empty;
import proto.HouseProto.Hello;
import proto.HouseProto.Identifier;
import proto.HouseProto.MasterResponse;
import proto.HouseProto.Measure;
import proto.HouseServiceGrpc.HouseServiceStub;
import simulator.Buffer;
import simulator.SmartMeterSimulator;
import utils.WorkerPool;
import utils.Barrier;
import utils.BlockingStack;
import utils.Http;
import utils.StreamHelper;

public class HouseManager {

	final private WorkerPool async = new WorkerPool(0);
	final private Map<String, HouseChannel> channels = new HashMap<>();
	final private Map<String, Measurement> measures = new HashMap<>();

	final private String id;
	final private String ip;
	final private int port;

	final private Server listener;
	final private ServiceProvider provider;
	final private SmartMeterBuffer meterBuffer;
	final private SmartMeterSimulator simulator;
	final private Http http;

	private boolean master = false;
	private boolean stopping = false;

	private int measureCount = 0;
	
	public static final int BOOST_INACTIVE = 0;
	public static final int BOOST_REQUESTED = 2;
	public static final int BOOST_ACTIVE = 3;

	private int boost = 0;
	private long boostTimestamp = 0;

	private Map<String, Boolean> boostGrant = new HashMap<>();
	private Set<String> pendingBoost = new HashSet<>();

	public HouseManager(String id, String ip, int port, String serverAddr, boolean runSimulator) {
		this.id = id;
		this.ip = ip;

		// initialize local measurements
		measures.put(id, new Measurement(id, Double.NaN, 0));


		// start listening for incoming connections
		provider = new ServiceProvider(this);
		listener = ServerBuilder.forPort(port).addService(provider).build();
		
		try {
			listener.start();
			System.out.println("Started listening for connections");
		} catch (IOException e) {
			System.err.println("Failed to start HouseService");
			e.printStackTrace();
			System.exit(1);
		}
		this.port = port = listener.getPort();

		// Creating clients for rest server
		http = new Http(serverAddr);
		System.out.println("Created clients for rest server");

		Response r = http.post("houses/add", new House(this.id, this.ip, this.port));
		
		if (r.getStatus() == 200) {
			
			System.out.println("registered to network, id:" + getId());
			
			List<House> houses = r.readEntity(new GenericType<List<House>>() {});

			
			List<HouseChannel> anchestors = new ArrayList<>();
			for (House h: houses) {
				if (h.getId().equals(getId())) continue;
				System.out.println("hello to "+h.getId());
				HouseChannel c = new HouseChannel(h);
				anchestors.add(c);
				channels.put(h.getId(), c);
			}
			
			BlockingStack<String> offline = new BlockingStack<>();
			
			if (!anchestors.isEmpty()) {
				Barrier b = new Barrier();
				for (HouseChannel c: anchestors) {
						try {
							HouseServiceGrpc.newStub(c.getChannel()).joinNetwork(
									Hello.newBuilder()
									.setId(this.id)
									.setAddress(this.ip)
									.setPort(this.port)
									.build(), 
									StreamHelper.simple(
										(m) -> new Measurement(m), 
										(t) -> {
											System.out.println("ERRORE DI OPORCO");
											t.printStackTrace();
										}, 
										() -> System.out.println("completed")
									)
								);
						
						} catch(StatusRuntimeException e) {
							System.out.println("offline: "+c.getId());
							offline.push(c.getId());
						}
					
				}
				b.await();
			}
			
			boolean tmaster = anchestors.size() - offline.size() == 0;
			
			while(offline.size() > 0) {
				removeHouse(offline.pop());
			}
			
			master = tmaster;
			System.out.println("master: "+master);
			
		} else {
			System.err.println(r.getStatusInfo());
			System.exit(1);
		}
		

		// starting simulator if enabled
		meterBuffer = new SmartMeterBuffer(this);
		simulator = new SmartMeterSimulator(id, (Buffer) meterBuffer);
		if (runSimulator) {
			simulator.start();
			System.out.println("Started simulator");
		}

		while(true) {
			
		}
	}

	public synchronized boolean isMaster() {
		return master;
	}

	public synchronized void setMaster(boolean master) {
		this.master = master;
	}

	public String getId() {
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

		/*
		// sends measurement to every node
		for (HouseChannel channel: getChannels()) {
			System.out.println(channel.getId());
			async.run(() -> {
				StreamObserver<Measure> s = channel.async().setMeasurements(StreamHelper.ignore());
				s.onNext(m.toProtobuf());
				s.onCompleted();
			});
		}

		// sends measurement to rest server
		async.run(() -> {
			http.post("stats/add", Arrays.asList(m));
		});
		*/
	}

	public synchronized void addHouse(HouseChannel c) {
		channels.put(c.getId(), c);

		if (!measures.containsKey(c.getId())) {
			measures.put(c.getId(), new Measurement(c.getId(), Double.NaN, 0));
		}
	}

	public synchronized void removeHouse(String id) {
		channels.remove(id);
		Measurement m = measures.remove(id);
		if (m != null && !m.isNan()) {
			measureCount--;
		}

		sendSum();

		pendingBoost.remove(id);
		boostGrant.remove(id);
		canBoost();
	}
	
	public void update(Measure m) {
		update(new Measurement(m));
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

		final List<Measure> msgs = new ArrayList<>();

		for (Measurement m: getMeasurements()) {
			timestamp = Math.max(timestamp, m.getTimestamp());
			value += m.getValue();
			count++;

			Measurement m2 = new Measurement(m.getId(), Double.NaN, m.getTimestamp());
			measures.put(m2.getId(), m2);
			msgs.add(m2.toProtobuf());
		}

		measureCount = 0;

		Measurement avg = new Measurement("", value/count, timestamp);
		async.run(() -> {
			http.post("stats/add", avg);
		});

		for (HouseChannel c: getChannels()) {
			async.run(() -> {
				StreamObserver<Measure> s = c.async().setMeasurements(StreamHelper.ignore());
				for (Measure m: msgs) {
					s.onNext(m);
				}
				s.onCompleted();
			});
		}

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
		if (isBoostActive()) {
			
		}

		async.waitAllIdle();

		// notifies other node that we are leaving and close the channels
		Identifier msg = Identifier.newBuilder().setId(id).build();
		for (HouseChannel channel: getChannels()) {
			async.run(() -> {
				try {
					channel.blocking().leaveNetwork(msg);
				} catch(StatusRuntimeException e) {
					e.printStackTrace();
				}
				channel.shutdown();
			});
		}

		// waits until every connection is closed and every worker is stopped
		async.shutdown();

		// stop listening for remote calls
		listener.shutdown();
		while(!listener.isTerminated()) {
			try {
				listener.awaitTermination();
			} catch (InterruptedException e) {}
		}
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
		if (boost != BOOST_INACTIVE) return;
		
		pendingBoost.clear();
		boostGrant.clear();
		
		boost = BOOST_REQUESTED;
		boostTimestamp = System.currentTimeMillis();

		for (HouseChannel c: channels.values()) {
			boostGrant.put(c.getId(), false);
			pendingBoost.add(c.getId());
			c.async().askBoost(BoostRequest.newBuilder().setId(id).setTimestamp(boostTimestamp).build(), StreamHelper.simple(
					(BoostResponse r) -> setBoost(r.getId(), r.getGrant()), 
					StreamHelper.logError, 
					() -> {}
				));
		}
		
	}
	
	public synchronized void canBoost() {
		if (boost != BOOST_REQUESTED) return;

		if (!pendingBoost.isEmpty()) return;

		int count = 0;
		for (Boolean active: boostGrant.values()) {
			if (!active) count++;
		}

		if (count < 2) {
			boost = BOOST_ACTIVE;

			async.run(() -> {
				System.out.println("I AM "+id+" BOOOOOST");
				startBoost();

				System.out.println("I AM "+id+" END BOOST :(");
				stopBoost();
			});

		}  else {
			System.out.println("I AM "+id+" too much boost "+count);
		}
	}
	
	public synchronized void startBoost() {
		try {
			simulator.boost();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public synchronized void stopBoost() {
		assert(boost == BOOST_ACTIVE);

		boost = BOOST_INACTIVE;
		
		for (HouseChannel c: channels.values()) {
			c.async().endBoost(Identifier.newBuilder().setId(id).build(), StreamHelper.ignore());
		}

	}

	public synchronized void setBoost(String id, boolean grant) {
		if (boost != BOOST_REQUESTED) return;

		pendingBoost.remove(id);
		boostGrant.put(id, grant);

		canBoost();
	}

	public synchronized boolean canGrantBoost(String id, long timestamp) {
		if (boost == BOOST_ACTIVE) return false;
		if (boost == BOOST_INACTIVE) return true;
		if (boostTimestamp < timestamp) return true;
		if (boostTimestamp > timestamp) return false;
		return this.id.compareTo(id) > 0; // if two houses requested boost in the same millisecond the smaller id goes first
	}

}
