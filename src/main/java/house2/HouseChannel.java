package house2;

import java.util.concurrent.TimeUnit;

import beans.House;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import proto.HouseServiceGrpc;
import proto.HouseServiceGrpc.HouseServiceBlockingStub;
import proto.HouseServiceGrpc.HouseServiceStub;

public class HouseChannel {
	private final String id;
	private final ManagedChannel channel;
	private final HouseServiceStub async;
	private final HouseServiceBlockingStub blocking;
	
	public HouseChannel(String id, String ip, int port) {
		this.id = id;
		channel = ManagedChannelBuilder.forAddress(ip, port).usePlaintext().build();
		async = HouseServiceGrpc.newStub(channel);
		blocking = HouseServiceGrpc.newBlockingStub(channel);
	}
	
	public HouseChannel(House h) {
		this(h.getId(), h.getAddr(), h.getPort());
	}

	public String getId() {
		return id;
	}
	
	public ManagedChannel getChannel() {
		return channel;
	}
	
	public HouseServiceStub async() {
		return async;
	}

	public HouseServiceBlockingStub blocking() {
		return blocking;
	}

	public void shutdown() {
		channel.shutdown();
		while(!channel.isTerminated()) {
			try {
				channel.awaitTermination(1, TimeUnit.MINUTES);
			} catch (InterruptedException e) {}
		}
	}
}
