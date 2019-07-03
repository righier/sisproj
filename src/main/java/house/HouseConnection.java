package house;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.List;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageLite;
import com.google.protobuf.Descriptors.FieldDescriptor;

import beans.Measurement;
import proto.HouseProto.*;
import utils.MessageQueue;
import utils.Async;

public class HouseConnection implements Runnable {

	private HouseManager manager;

	private Socket socket;
	private InputStream in;
	private OutputStream out;

	private MessageQueue queue;

	private volatile boolean alive = true;

	private String id = null;

	public HouseConnection(HouseManager manager, String remoteId, String host, int port)
			throws IOException {
		this.id = remoteId;

		Socket socket = new Socket(host, port);
		init(manager, socket);

		Any msg = Any.pack(Hello.newBuilder().setId(manager.getLocalId()).build());
		write(msg);
		queue.waitEmpty();

		manager.add(this);
	}

	public HouseConnection(HouseManager manager, Socket socket) throws IOException {
		init(manager, socket);
	}

	private void init(HouseManager manager, Socket socket) throws IOException {
		this.manager = manager;
		this.socket = socket;
		this.in = socket.getInputStream();
		this.out = socket.getOutputStream();

		Async.run(this);

		this.queue = new MessageQueue(this.out);
	}

	public String getId() {
		return id;
	}

	public void write(MessageLite msg) {
		queue.add(msg);
	}

	public void kill() {
		alive = false;
		manager.remove(this.id);
		
		queue.stop();

		try {
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {

		while (alive) {
			Any msg;
			try {
				msg = Any.parseDelimitedFrom(in);
			} catch (IOException e) {
				e.printStackTrace();
				continue;
			}

			FieldDescriptor field = msg.getDescriptorForType().findFieldByName("type_url");
			System.out.println(manager.getLocalId()+" from "+this.id+" "+msg.getField(field));

			try {
				if (msg.is(Hello.class)) {
					Hello hello = msg.unpack(Hello.class);
					this.id = hello.getId();
					if (manager.isReady()) {
						List<Measurement> ms = manager.getMeasurements();
					}
					manager.add(this);
				} else if (msg.is(Goodbye.class)) {
					manager.remove(id);
				} else if (msg.is(MeasureList.class)) {
					MeasureList list = msg.unpack(MeasureList.class);
					for (Measure m : list.getMeasuresList()) {
						manager.update(new Measurement(m));
					}
				} else {
					System.out.println("Unknown message format");
				}
			} catch (InvalidProtocolBufferException e) {
				System.out.println("Invalid message format");
				e.printStackTrace();
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
