package utils;

public class Async {

	public static String id() {
		return id(Thread.currentThread());
	}
	
	public static String id(Thread t) {
		return "" + t.getId();
	}
	
	public static void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}


}
