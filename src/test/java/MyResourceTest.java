
import org.glassfish.grizzly.http.server.HttpServer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import house.CasaMain;
import server.ServerMain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MyResourceTest {

	private HttpServer server;

	@Before
	public void setUp() throws Exception {
		// start the server
		server = ServerMain.startServer();
		System.out.println("Server started");


//		 c.configuration().enable(new org.glassfish.jersey.media.json.JsonJaxbFeature());
	}

	@After
	public void tearDown() throws Exception {
		server.stop();
	}

	/**
	 * Test to see that the message "Got it!" is sent in the response.
	 */
	@Test
	public void testGetIt() {
		for (int i = 0; i < 10; i++) {
			new Thread(() -> {
				System.out.println("starting house n "+Thread.currentThread().getId());
				CasaMain.main(null);
			}).start();
			
		}
		
		while(true) {
		try {
			synchronized(this) {
				wait();
			}
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		}

	}
}
