package server;

import java.io.IOException;
import java.net.BindException;
import java.net.URI;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

public class ServerMain {

	public static String addr = "http://localhost:8000/";

	/**
	 * Starts Grizzly HTTP server exposing JAX-RS resources defined in this
	 * application.
	 * 
	 * @return Grizzly HTTP server.
	 */
	public static HttpServer startServer() {
		// create a resource config that scans for JAX-RS resources and providers
		// in me.erre.sisdisper.energia package
		final ResourceConfig rc = new ResourceConfig().packages("server.services");
		rc.register(AppExceptionMapper.class);
		
		// create and start a new instance of grizzly http server
		// exposing the Jersey application at BASE_URI
		return GrizzlyHttpServerFactory.createHttpServer(URI.create(addr), rc);
	}

	public static void main(String args[]) {

		String host = "localhost";
		int port = 8000;

		addr = "http://" + host + ":" + port + "/";

		try {
			System.out.println(addr);

			HttpServer server = startServer();
			server.start();
			System.out.println(addr);
			System.out.println("Hit return to stop...");
			System.in.read();
			server.shutdown();
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
