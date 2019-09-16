package house;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

import beans.House;
import simulator.Buffer;
import simulator.SmartMeterSimulator;
import utils.Http;

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

	public static HouseManager init(String id, String addr, int port, boolean simulator) {

		Http http = new Http(addr);

		HouseManager houseManager;
		try {
			houseManager = new HouseManager(id, port, http);
		} catch (IOException e1) {
			e1.printStackTrace();
			return null;
		}

		House house = new House(id, "localhost", houseManager.getPort());

		Response response = http.post("houses/add", house);

		int status = response.getStatus();
		if (status == 200) {
			List<House> houses = response.readEntity(new GenericType<List<House>>() {});
			Collections.sort(houses, Collections.reverseOrder());

			// Quando entro nella rete voglio presentarmi con tutti
			// In ordine decrescente, così l'ultimo con cui parlo è il capo
			for (House h: houses) {
				System.out.println("HELLO TO "+h.getId());
				if (h.getId().equals(id)) continue;

				try {
					new HouseConnection(houseManager, h.getId(), h.getAddr(), h.getPort());
				} catch (IOException e) {
					e.printStackTrace();
				}

			}

			houseManager.setReady(true);
			System.out.println("READY");
		}

		SmartMeterBuffer buffer = new SmartMeterBuffer(houseManager);
		SmartMeterSimulator meter = new SmartMeterSimulator(id, (Buffer) buffer);

		if (simulator) {
			meter.start();
		}

		houseManager.setSimulator(meter);

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			meter.stopMeGently();
			System.out.println(id+" exit");
			System.exit(0);
		}));

		return houseManager;
	}

	public static void main(String args[]) {

		HouseManager manager = init(CasaMain.randomId(), "http://localhost:8000", 0, false);

		boolean running = true;
		Scanner in = new Scanner(System.in);
		while (running) {
			System.out.print("Type a command:");
			String s = in.next();

			switch(s) {

			case "exit":
				running = false;
				manager.stop();
				break;

			case "boost":
				manager.boost();
				break;

			}
		}
		in.close();
	}
}
