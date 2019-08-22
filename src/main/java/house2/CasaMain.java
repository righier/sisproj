package house2;

public class CasaMain {

	public static void init(String id, String serverAddr, String ip, int port, boolean simulator) {

		HouseManager manager = new HouseManager(id, ip, port, serverAddr, simulator);
	}

}
