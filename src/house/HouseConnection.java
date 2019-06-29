package house;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import com.google.protobuf.Any;

import beans.Measurement;
import proto.HouseProto.Goodbye;
import proto.HouseProto.Hello;
import proto.HouseProto.LastSync;
import proto.HouseProto.Measure;

public class HouseConnection implements Runnable {
	
	private static final int maxRetry = 10;
	
	private HouseManager manager;

	private Socket socket;
	private InputStream in;
	private OutputStream out;
	
	private volatile boolean alive = true;
	
	private String id;
	
	public HouseConnection(HouseManager manager, String localId, Socket socket) throws IOException {
		this.manager = manager;
		this.socket = socket;
		this.in = socket.getInputStream();
		this.out = socket.getOutputStream();
		
		new Thread(this).start();
		
		Hello hello = Hello.newBuilder()
				.setId(localId)
				.build();
		
		hello.writeTo(out);
	}
	
	public String getId() {
		return id;
	}
	
	public void kill() {
		alive = false;
		manager.remove(this.id);
	}
	
	@Override
	public void run() {
		
		try {
			Hello hello = Hello.parseFrom(in);
			this.id = hello.getId();
			manager.add(this);
		} catch (IOException e) {
			e.printStackTrace();
			alive = false;
			return;
		}
		
		int retry = 0;
		while (alive && retry < maxRetry) {
			Any msg;
			try {
				msg = Any.parseFrom(in);
			} catch (IOException e) {
				e.printStackTrace();
				retry++;
				continue;
			}
			retry = 0;
			
			if (msg.is(Goodbye.class)) {
				
			} else if (msg.is(Measure.class)) {
				
			} else if (msg.is(LastSync.class)) {
				
			} 

		}
		
		kill();
		
		try {
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}
