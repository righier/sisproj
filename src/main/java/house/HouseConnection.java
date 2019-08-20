package house;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.MessageLite;
import com.google.protobuf.Descriptors.FieldDescriptor;

import beans.Measurement;
import proto.HouseProto.*;
import utils.MessageQueue;
import utils.MessageWait;
import utils.Async;

public class HouseConnection implements Runnable {

	private HouseManager manager;

	private Socket socket;
	private InputStream in;
	private OutputStream out;

	private MessageQueue queue;

	private volatile boolean alive = true;

	private String id = null;
	
	private List<MessageWait> waitLocks = new ArrayList<>();

	public HouseConnection(HouseManager manager, String remoteId, String host, int port)
			throws IOException {
		this.id = remoteId;

		Socket socket = new Socket(host, port);
		init(manager, socket);

		manager.add(this);

		Any msg = Any.pack(Hello.newBuilder().setId(manager.getLocalId()).build());
		write(msg);
		
		waitMessage(Welcome.class);
	}

	public HouseConnection(HouseManager manager, Socket socket) throws IOException {
		init(manager, socket);
	}
	
	public void waitMessage(Class<? extends Message> c) {
		new MessageWait(this).wait(c);
	}
	
	public void addWaitLock(MessageWait w) {
		synchronized(waitLocks) {
			waitLocks.add(w);
		}
	}
	
	public void removeWaitLock(MessageWait w) {
		synchronized(waitLocks) {
			waitLocks.remove(w);
		}
	}

	private void init(HouseManager manager, Socket socket) throws IOException {
		this.manager = manager;
		this.socket = socket;
		this.in = socket.getInputStream();
		this.out = socket.getOutputStream();

		this.queue = new MessageQueue(this.out);

		Async.run(this);
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
		queue.waitEmpty();

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
//			System.out.println(manager.getLocalId()+" from "+this.id+" "+msg.getField(field));

			try {
				if (msg.is(Hello.class)) {
					Hello hello = msg.unpack(Hello.class);
					this.id = hello.getId();
					manager.add(this);

					MeasureList.Builder list = MeasureList.newBuilder();
					for (Measurement m: manager.getMeasurements()) {
						list.addMeasures(m.toProtobuf());
					}

					write(Any.pack(list.build()));
					write(Any.pack(Welcome.newBuilder().build()));

				} else if (msg.is(Goodbye.class)) {
					manager.remove(id);

				} else if (msg.is(MeasureList.class)) {
					MeasureList list = msg.unpack(MeasureList.class);
					for (Measure m : list.getMeasuresList()) {
						manager.update(new Measurement(m));
					}

				} else if (msg.is(Welcome.class)) {
					
					// nothing to do
					
				} else if (msg.is(AskBoost.class)) {
					AskBoost ask = msg.unpack(AskBoost.class);
					int boost = manager.getBoostStatus();
					long t1 = manager.getBoostTimestamp();
					long t2 = ask.getTimestamp();
					boolean act = boost == HouseManager.BOOST_ACTIVE;
					boolean req = boost == HouseManager.BOOST_REQUESTED;
					boolean busy = act || (req && ( t1 < t2 || ( t1 == t2 && manager.getLocalId().compareTo(id) < 0)));
					
					if (busy) {
						write(Any.pack(DenyBoost.newBuilder().build()));
					} else {
						write(Any.pack(GrantBoost.newBuilder().build()));
					}

				} else if (msg.is(GrantBoost.class)) {
					
					//the remote house is not using boost
					manager.setBoost(id, false);
					
				} else if (msg.is(DenyBoost.class)) {

					//the remote house is using boost
					manager.setBoost(id, true);
					
				} else if (msg.is(EndBoost.class)) {
					
					manager.setBoost(id, false);
					
				} else {
					System.out.println("Unknown message format");
				}
				
				synchronized(waitLocks) {
					for (MessageWait w: waitLocks) {
						w.check(msg);
					}
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
