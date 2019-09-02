package utils;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.Queue;

import com.google.protobuf.MessageLite;

public class MessageQueue {

	private Queue<MessageLite> queue = new ArrayDeque<MessageLite>();
	private OutputStream out;
	private boolean running = true;
	private boolean stopped = false;

	public MessageQueue(OutputStream out) {
		this.out = out;
		new Thread(() -> {
			while(running) {
				sendMessage();
			}
		});
	}

	private void sendMessage() {
		MessageLite msg = null;
		synchronized(this) {
			try {
				while(queue.isEmpty()) {
					if (stopped) running = false;
					if (!running) return;
					this.wait();
				}
			} catch (InterruptedException e) {
				running = false;
				return;
			}
			msg = queue.poll();
			this.notify();
		}

		try {
			msg.writeDelimitedTo(out);
		} catch (IOException e) {
			running = false;
		}

	}

	public synchronized void add(MessageLite msg) {
		if (stopped) return;

		queue.add(msg);
		this.notify();
	}

	public synchronized void stop() {
		stopped = true;
		notifyAll();
	}

	public synchronized void waitEmpty() {
		try {
			while(!queue.isEmpty()) {
				this.wait();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		try {
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
