package server;

import java.io.IOException;
import java.net.BindException;

import com.sun.jersey.api.container.httpserver.HttpServerFactory;
import com.sun.net.httpserver.HttpServer;

public class ServerMain {

	public static void main(String args[]) {

		String host = "localhost";
		int port = 8000;
		String addr = "http://" + host + ":" + port + "/";

		try {
			HttpServer server = HttpServerFactory.create(addr);
			server.start();
			System.out.println(addr);
			System.out.println("Hit return to stop...");
			System.in.read();
			server.stop(0);
			System.out.println("Server stopped");
		} catch (IllegalArgumentException e) {
			System.err.println("Invalid hostname");
			e.printStackTrace();
		} catch (BindException e) {
			System.err.println("Port already in use");
		} catch (IOException e) {

			e.printStackTrace();
		}
	}

}
