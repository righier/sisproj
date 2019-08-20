package utils;

import com.google.protobuf.Any;
import com.google.protobuf.Message;

import house.HouseConnection;

public class MessageWait {
	
	private HouseConnection conn;
	private Class<? extends Message> c;
	private boolean waiting = true;
	
	public MessageWait(HouseConnection conn) {
		this.conn = conn;
		conn.addWaitLock(this);
	}
	
	public synchronized void wait(Class<? extends Message> c) {
		this.c = c;
		try {
			while(waiting) wait();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		conn.removeWaitLock(this);
	}
	
	public synchronized void check(Any msg) {
		if (msg.is(c)) {
			waiting = false;
			notify();
		}
	}
	
	public synchronized void stop() {
		waiting = false;
		notify();
	}

}
