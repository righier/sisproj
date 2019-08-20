package client;

import java.util.Scanner;

public class ClientMain {

	public static void main(String args[]) {
		
		Scanner in = new Scanner(System.in);
		boolean running = true;
		
		while (running) {
			
			String s = in.next();
			
			switch(s) {
			
			case "exit":
				running = false;
				break;
			case "list":
				
				break;
			case "house":
				
				break;
			
			case "condo":
				
				break;
				
			case "avg":
				
				break;
			}
		}
		
	}

}
