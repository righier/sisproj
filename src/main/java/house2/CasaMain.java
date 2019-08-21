package house2;

import java.util.Random;

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

	public static void init(String id, String addr, String ip, int port, boolean simulator) {

		ClientPool.init(addr);

		HouseManager manager = new HouseManager(id, ip, port, simulator);
	}

}
