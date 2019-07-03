package house;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import beans.House;
import simulator.Buffer;
import simulator.SmartMeterSimulator;
import utils.ClientPool;

public class CasaMain {

	public static String randomId() {
		String alpha = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
		StringBuilder builder = new StringBuilder();
		Random r = new Random();
		for (int i = 0; i < 10; i++) {
			builder.append(alpha.charAt(r.nextInt(alpha.length())));
		}
		return builder.toString();
	}

	public static void main(String args[]) {

		String id = randomId();
		String serverAddr = "http://localhost:8000/";

		ClientPool.init(serverAddr);

		HouseManager houseManager;
		try {
			houseManager = new HouseManager(id);
		} catch (IOException e1) {
			e1.printStackTrace();
			return;
		}

		House house = new House(id, "localhost", houseManager.getPort());

		Client client = ClientPool.get();
		Response response = client.target(ClientPool.getUrl())
			.path("houses/add")
			.request(MediaType.APPLICATION_JSON)
			.put(Entity.entity(house, MediaType.APPLICATION_JSON));
		ClientPool.add(client);
		
		System.out.println("I AM "+id+" status "+response.getStatus());

		int status = response.getStatus();
		if (status == 200) {
			List<House> houses = response.readEntity(new GenericType<List<House>>() {});
			Collections.sort(houses);


			for (House h: houses) {
				if (h.getId().equals(id)) continue;

				try {
					new HouseConnection(houseManager, h.getId(), h.getAddr(), h.getPort());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}


			houseManager.setReady(true);
		}

		System.out.println(id+" my port is "+house.getPort());

		SmartMeterBuffer buffer = new SmartMeterBuffer(houseManager);
		SmartMeterSimulator meter = new SmartMeterSimulator(id, (Buffer) buffer);
		meter.start();

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			meter.stopMeGently();
			System.out.println(id+" exit");
		}));

		while (true) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
