package utils;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public class Http {
	private static final MediaType json = MediaType.APPLICATION_JSON_TYPE;
	private static final int defaultPoolSize = 4;

	private static BlockingStack<Client> pool;
	private static String url;

	private Http() {}

	public static void init(String url) {
		init(url, defaultPoolSize);
	}

	public static void init(String url, int poolSize) {
		Http.url = url;
		Http.pool = new BlockingStack<>(poolSize);
		
		for (int i = 0; i < poolSize; i++) {
			Http.pool.push(ClientBuilder.newClient());
		}
	}
	
	public static <T> Response post(String path, T payload) {
		try (Managed c = new Managed()) {
			return c.init().path(path).request(json).post(Entity.entity(payload, json));
		} catch (Exception e) {
			return null;
		}
	}
	
	public static <T> Response put(String path, T payload) {
		try (Managed c = new Managed()) {
			return c.init().path(path).request(json).put(Entity.entity(payload, json));
		} catch (Exception e) {
			return null;
		}
	}

	public static Response delete(String path) {
		try (Managed c = new Managed()) {
			return c.init().path(path).request(json).delete();
		} catch (Exception e) {
			return null;
		}
	}

	public static <T> Response get(String path, T payload) {
		try (Managed c = new Managed()) {
			return c.init().path(path).request(json).get();
		} catch (Exception e) {
			return null;
		}
	}
	
	public static <T> Response head(String path, T payload) {
		try (Managed c = new Managed()) {
			return c.init().path(path).request(json).head();
		} catch (Exception e) {
			return null;
		}
	}
	
	private static class Managed implements AutoCloseable {
		private final Client c;
		
		public Managed() {
			c = Http.pool.pop();
		}
		
		public WebTarget init() {
			return c.target(Http.url);
		}

		@Override
		public void close() throws Exception {
			Http.pool.push(c);
		}
		
	}
}