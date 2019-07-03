package utils;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

public class ClientPool {

	private static final int CLIENT_COUNT = 4;
	private static Client pool[] = new Client[CLIENT_COUNT];
	private static int avail = CLIENT_COUNT;
	private static Object lock = new Object();
	private static String url = null;

	private ClientPool() {
	}

	public static void init(String url) {
		synchronized(lock) {
			if (ClientPool.url != null) return;
			ClientPool.url = url;
			for (int i = 0; i < CLIENT_COUNT; i++) {
				pool[i] = ClientBuilder.newClient();
			}
		}
	}

	public static Client get() {
		synchronized (lock) {

			while (avail == 0) {
				try {
					lock.wait();
				} catch (InterruptedException e) {
					return null;
				}
			}

			return pool[--avail];
		}
	}

	public static void add(Client c) {
		synchronized (lock) {
			if (avail == CLIENT_COUNT)
				return;

			pool[avail++] = c;

			lock.notify();
		}
	}

	public static String getUrl() {
		synchronized(lock) {
			return url;
		}
	}

}
