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

	private final BlockingStack<Client> pool;
	private final String url;

	public Http(String url) {
		this(url, defaultPoolSize);
	}
	
	public Http(String url, int poolSize) {
		this.url = url;
		this.pool = new BlockingStack<>(poolSize);
		
		for (int i = 0; i < poolSize; i++) {
			this.pool.push(ClientBuilder.newClient());
		}
	}
	
	public String getUrl() {
		return url;
	}
	
	public <T> Response post(String path, T payload) {
		try (Managed c = new Managed(this)) {
			return c.init().path(path).request(json).post(Entity.entity(payload, json));
		} catch (Exception e) {
			return null;
		}
	}
	
	public <T> Response put(String path, T payload) {
		try (Managed c = new Managed(this)) {
			return c.init().path(path).request(json).put(Entity.entity(payload, json));
		} catch (Exception e) {
			return null;
		}
	}

	public Response delete(String path) {
		try (Managed c = new Managed(this)) {
			return c.init().path(path).request(json).delete();
		} catch (Exception e) {
			return null;
		}
	}

	public <T> Response get(String path) {
		try (Managed c = new Managed(this)) {
			return c.init().path(path).request(json).get();
		} catch (Exception e) {
			return null;
		}
	}
	
	public <T> Response head(String path) {
		try (Managed c = new Managed(this)) {
			return c.init().path(path).request(json).head();
		} catch (Exception e) {
			return null;
		}
	}
	
	private class Managed implements AutoCloseable {
		private final Http http;
		private final Client c;
		
		public Managed(Http http) {
			this.http = http;
			c = http.pool.pop();
		}
		
		public WebTarget init() {
			return c.target(http.url);
		}

		@Override
		public void close() throws Exception {
			http.pool.push(c);
		}
		
	}
}