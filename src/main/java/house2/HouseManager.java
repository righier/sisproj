package house2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

import beans.House;
import beans.Measurement;
import house2.SmartMeterBuffer;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import proto.HouseProto.Empty;
import proto.HouseProto.Hello;
import proto.HouseProto.Identifier;
import proto.HouseProto.MasterResponse;
import proto.HouseProto.Measure;
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
			
			List<House> houses = r.readEntity(new GenericType<List<House>>() {});
			
			List<HouseChannel> anchestors = new ArrayList<>();
			for (House h: houses) {
				HouseChannel c = new HouseChannel(h);
				anchestors.add(c);
				channels.put(h.getId(), c);
			}
			
			BlockingStack<String> offline = new BlockingStack<>();
			
			if (!anchestors.isEmpty()) {
				Barrier b = new Barrier();
				for (HouseChannel c: anchestors) {
					b.add(async.run(() -> {
						Iterator<Measure> it = c.blocking().joinNetwork(Hello.newBuilder()
								.setId(this.id)
								.setAddress(this.ip)
								.setPort(this.port)
								.build());
						
						if (it.hasNext()) {
							while(it.hasNext()) update(new Measurement(it.next()));
						} else offline.push(c.getId());
					
					}));
				}
				b.await();
			}
			
			boolean tmaster = anchestors.size() - offline.size() == 0;
			
			while(offline.size() > 0) {
				removeHouse(offline.pop());
			}

			master = tmaster;
		
			
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

		// sends measurement to every node
		for (HouseChannel channel: getChannels()) {
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

	public void boost() {
		// TODO Auto-generated method stub
		
	}

}
