package house2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import beans.Measurement;
import house2.SmartMeterBuffer;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import proto.HouseProto.Identifier;
import proto.HouseProto.Measure;
import proto.HouseServiceGrpc.HouseServiceStub;
import simulator.Buffer;
import simulator.SmartMeterSimulator;
import utils.Async;
import utils.Barrier;
import utils.StreamHelper;

public class HouseManager {

	final private Map<String, HouseServiceStub> stubs = new HashMap<>();
	final private Map<String, Measurement> measures = new HashMap<>();

	final private String id;
	final private String ip;
	final private int port;

	final private Server server;
	final private ServiceProvider provider;
	final private SmartMeterBuffer meterBuffer;
	final private SmartMeterSimulator simulator;

	private boolean master = false;
	private boolean stopping = false;

	public HouseManager(String id, String ip, int port, boolean runSimulator) {
		this.id = id;
		this.ip = ip;

		meterBuffer = new SmartMeterBuffer(this);
		simulator = new SmartMeterSimulator(id, (Buffer) meterBuffer);
		if (runSimulator) simulator.start();

		provider = new ServiceProvider(this);
		server = ServerBuilder.forPort(port).addService(provider).build();
		try {
			server.start();
		} catch (IOException e) {
			System.err.println("Failed to start HouseService");
			e.printStackTrace();
			System.exit(1);
		}

		this.port = server.getPort();

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

	public void newLocalMeasurement(Measurement m) {

		update(m);

		Measure msg = Measure.newBuilder()
		.setId(m.getId())
		.setValue(m.getValue())
		.setTimestamp(m.getTimestamp())
		.build();


		for (HouseServiceStub stub: getStubs()) {

		}
		//TODO

	}

	public synchronized void addHouse(String id, String ip, int port) {
		//TODO
	}

	public synchronized void removeHouse(String id) {
		//TODO
	}

	public synchronized void update(Measurement m) {
		//TODO
	}

	public synchronized List<Measurement> getMeasurements() {
		return new ArrayList<Measurement>(measures.values());
	}

	public synchronized List<HouseServiceStub> getStubs() {
		return new ArrayList<HouseServiceStub>(stubs.values());
	}
	
	public synchronized boolean isStopping() {
		return stopping;
	}
	
	private synchronized void setStopping(boolean stopping) {
		this.stopping = stopping;
	}

	public void stop() {
		setStopping(true);
		
		// TODO Auto-generated method stub
		simulator.stopMeGently();
		
		if(isMaster()) {
			
		}

		Identifier msg = Identifier.newBuilder().setId(id).build();
		for (HouseServiceStub stub: getStubs()) {
			stub.leaveNetwork(msg, StreamHelper.ignore());
		}

		server.shutdown();
		while(!server.isTerminated()) {
			try {
				server.awaitTermination();
			} catch (InterruptedException e) {}
		}

		Barrier barrier = new Barrier();
		for (HouseServiceStub stub: getStubs()) {
			barrier.add(Async.run(() -> {
				ManagedChannel channel = (ManagedChannel)stub.getChannel();
				channel.shutdown();
				while(!channel.isTerminated()) {
					try {
						channel.awaitTermination(1, TimeUnit.MINUTES);
					} catch (InterruptedException e) {}
				}
			}));
		}

		barrier.await();

	}

}
