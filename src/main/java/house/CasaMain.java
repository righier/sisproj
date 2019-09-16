package house;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

import beans.House;
import picocli.CommandLine;
import picocli.CommandLine.Option;
import simulator.Buffer;
import simulator.SmartMeterSimulator;
import utils.Generator;
import utils.Http;

public class CasaMain implements Runnable {

	@Option(names = {"-id"}, description = "The string id of this node")
	private String id = Generator.randomId();
	
	@Option(names = {"-ip"}, description = "The ip address of this node")
	private String ip = "localhost";
	
	@Option(names = {"-port"}, description = "The port of this node")
	private int port = 0;

	@Option(names = {"-server"}, description = "The uri of the REST server")
	private String serverAddr = "http://localhost:8000";

	@Option(names = {"-simulator"}, description = "The uri of the REST server")
	private boolean enableSimulator = true;

	public static void main(String args[]) {	
		new CommandLine(new CasaMain()).execute();
	}

	public HouseManager init(String id, String ip, String addr, int port, boolean simulator) {

		System.out.println(addr);
		Http http = new Http(addr);

		HouseManager houseManager;
		try {
			houseManager = new HouseManager(id, port, http);
		} catch (IOException e1) {
			e1.printStackTrace();
			return null;
		}

		House house = new House(id, ip, houseManager.getPort());

		Response response = http.post("houses/add", house);

		if (response == null) {
			System.exit(1);
		}

		int status = response.getStatus();
		if (status == 200) {
			List<House> houses = response.readEntity(new GenericType<List<House>>() {});
			Collections.sort(houses, Collections.reverseOrder());

			// Quando entro nella rete voglio presentarmi con tutti
			// In ordine decrescente, così l'ultimo con cui parlo è il capo
			for (House h: houses) {
				if (h.getId().equals(id)) continue;

				try {
					new HouseConnection(houseManager, h.getId(), h.getAddr(), h.getPort());
				} catch (IOException e) {
					e.printStackTrace();
				}

			}

			houseManager.setReady(true);
		} else {
			System.exit(1);
		}

		SmartMeterBuffer buffer = new SmartMeterBuffer(houseManager);
		SmartMeterSimulator meter = new SmartMeterSimulator(id, (Buffer) buffer);
		
		if (simulator) {
			meter.start();
		}
		
		houseManager.setSimultor(meter);
		
		return houseManager;
	}

	public void run() {

		HouseManager manager = init(id, ip, serverAddr, port, enableSimulator);

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
