
import org.glassfish.grizzly.http.server.HttpServer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import house.CasaMain;
import house.HouseManager;
import server.ServerMain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MyResourceTest {

	private static final String addr = "http://localhost:8000";
	private HttpServer server;

	@Before
	public void setUp() throws Exception {

		server = ServerMain.startServer();
		System.out.println("Server started");

	}

	@After
	public void tearDown() throws Exception {
		server.stop();
	}
	
	@Test 
	public void boostTest() throws Exception {
		HouseManager m1 = CasaMain.init("a", addr, 0, false);
		HouseManager m2 = CasaMain.init("b", addr, 0, false);
		HouseManager m3 = CasaMain.init("c", addr, 0, false);
		HouseManager m4 = CasaMain.init("d", addr, 0, false);
		
		Thread.sleep(500);
		
		m1.boost();
		Thread.sleep(200);
		m2.boost();
		m3.boost();
		Thread.sleep(100);
		m4.boost();
		
		Thread.sleep(6000);
	}

//	@Test
	public void stressTest() {
		for (int i = 0; i < 50; i++) {
			new Thread(() -> {
				System.out.println("starting house n "+Thread.currentThread().getId());

				CasaMain.init(CasaMain.randomId(), addr, 0, true);
			}).start();
			
		}
			
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}
}
