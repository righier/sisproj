package server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import proto.AdminServiceGrpc;
import proto.AdminServiceGrpc.AdminServiceStub;

public class Admins {

	private static Map<String, AdminChannel> channels = new HashMap<>();
	
	public static boolean add(String target) {
		synchronized(channels) {
			if (channels.containsKey(target)) return false;
			channels.put(target, new AdminChannel(target));
			return true;
		}
	}
	
	public static boolean remove(String target) {
		synchronized(channels) {
			if (!channels.containsKey(target)) return false;
			AdminChannel channel = channels.remove(target);
			channel.shutdown();
			return true;
		}
	}
	
	public static List<AdminChannel> getChannels() {
		synchronized(channels) {
			return new ArrayList<>(channels.values());
		}
	}
	
	public static class AdminChannel {
		private ManagedChannel channel;
		private AdminServiceStub async;
		
		public AdminChannel(String target) {
			channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
			async = AdminServiceGrpc.newStub(channel);
		}
		
		public AdminServiceStub async() {
			return async;
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
}
