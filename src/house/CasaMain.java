package house;

import java.util.Random;

import simulator.Buffer;
import simulator.SmartMeterSimulator;

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
		String serverAddr = "http://localhost:1337/";
		System.out.println("Started Casa: "+id);
		
		SmartMeterBuffer buffer = new SmartMeterBuffer();
		SmartMeterSimulator meter = new SmartMeterSimulator(id, (Buffer)buffer);
		meter.start();
		try {
			Thread.sleep(1000000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		meter.stopMeGently();
	}
}
