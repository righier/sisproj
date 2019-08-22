import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

import org.glassfish.grizzly.http.server.HttpServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import beans.House;
import server.ServerMain;
import utils.Generator;
import utils.Http;

public class ApiTest {
	private static final String addr = "http://localhost:8000";
	private HttpServer server;
	private Http client;
	

	@Before
	public void setUp() throws Exception {

		server = ServerMain.startServer();
		System.out.println("Server started");

		client = new Http(addr, 10);
	}
	
	@After
	public void tearDown() throws Exception {
		server.shutdown();
	}
	
	@Test
	public void testHouseService() {
		
		int n = 10;
		
		HashSet<String> ids = new HashSet<>();
		
		for (int i = 0; i < 10; i++) {
			String id;
			do {
				id = Generator.randomId();
			} while(ids.contains(id));
			
			ids.add(id);
			
			Response response = client.put("houses/add", new House(id, "localhost", 1234));
			
			assertTrue(response.getStatus() == 200);
			
			List<House> houses = response.readEntity(new GenericType<List<House>>() {});
			Set<String> saved = houses.stream().map(e -> e.getId()).collect(Collectors.toSet());
			
			assertTrue(ids.equals(saved));
		}
		
		
		
		
	}
	
}
