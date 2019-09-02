package house2;

import java.util.Scanner;

import house2.HouseManager;
import utils.Generator;

public class CasaMain {

	public static HouseManager init(String id, String serverAddr, String ip, int port, boolean simulator) {

		HouseManager manager = new HouseManager(id, ip, port, serverAddr, simulator);
		
		return manager;
	}

	public static void main(String args[]) {

		HouseManager manager = init(Generator.randomId(), "http://localhost:8000", "localhost", 0, true);

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
