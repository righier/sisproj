package utils;

public class Async {
	
	public static Thread run(Runnable r) {
		Thread t = new Thread(r);
		t.start();
		return t;
	}

}
