package client;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import beans.House;
import beans.Measurement;
import beans.ValueWrapper;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import picocli.CommandLine;
import picocli.CommandLine.Option;
import proto.AdminServiceGrpc.AdminServiceImplBase;
import proto.HouseProto.Empty;
import proto.HouseProto.Identifier;
import utils.Http;

public class ClientMain implements Runnable {
	
	@Option(names = {"-ip"}, description = "The ip address for receiving push notifications")
	private String ip = "localhost";
	
	@Option(names = {"-port"}, description = "The port for receiving push notifications")
	private int port = 0;

	@Option(names = {"-server"}, description = "The uri of the REST server")
	private String serverAddr = "http://localhost:8000";

	public static void main(String args[]) {
		new CommandLine(new ClientMain()).execute(args);
	}
		
	public void run() {
	
		String id;
		int n;
		Response r;
		
		Server listener = ServerBuilder.forPort(port).addService(new ServiceProvider()).build();
		try {
			listener.start();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		port = listener.getPort();
		
		Http http = new Http(serverAddr);
		
		r = http.get("admin/watch/"+ip+"/"+port);
		if (r == null || !r.getStatusInfo().equals(Status.OK)) {
			System.out.println("Can't subscribe to REST server");
			System.out.println(r.getStatusInfo());
			System.exit(1);
		}
		
		Scanner in = new Scanner(System.in);
		boolean running = true;
		
		while (running) {
			System.out.print("Type a command:");
			
			String s = in.next();
			
			switch(s) {
			
			case "exit":
				running = false;
				break;
				
			case "list":
				r = http.get("houses");
				if (r.getStatusInfo().equals(Status.OK)) {
					List<House> houses = r.readEntity(new GenericType<List<House>>() {});
					for (House h: houses) {
						System.out.println(h);
					}
				} else {
					System.out.println(r.getStatusInfo());
				}
				break;
				
			case "house":
				id = in.next();
				n = in.nextInt();
				r = http.get("stats/house/"+id+"/"+n);
				if (r.getStatusInfo().equals(Status.OK)) {
					List<Measurement> measures = r.readEntity(new GenericType<List<Measurement>>() {});
					for (Measurement m: measures) {
						System.out.println(m);
					}
				} else if (r.getStatusInfo().equals(Status.NOT_FOUND)){
					System.out.println("The id was not found");
				} else {
					System.out.println(r.getStatusInfo());
				}
				break;
			
			case "condo":
				n = in.nextInt();
				r = http.get("stats/condo/"+n);
				if (r.getStatusInfo().equals(Status.OK)) {
					List<Measurement> measures = r.readEntity(new GenericType<List<Measurement>>() {});
					for (Measurement m: measures) {
						System.out.println(m);
					}
				} else if (r.getStatusInfo().equals(Status.NOT_FOUND)){
					System.out.println("The id was not found");
				} else {
					System.out.println(r.getStatusInfo());
				}				
				break;
				
			case "avg":
				id = in.next();
				n = in.nextInt();
				r = http.get("stats/avg/"+id+"/"+n);
				if (r.getStatusInfo().equals(Status.OK)) {
					ValueWrapper v = r.readEntity(new GenericType<ValueWrapper>() {});
					System.out.println(v.getValue());
				} else if (r.getStatusInfo().equals(Status.NOT_FOUND)){
					System.out.println("The id was not found");
				} else {
					System.out.println(r.getStatusInfo());
				}				
				break;
				
			case "sigma":
				id = in.next();
				n = in.nextInt();
				r = http.get("stats/sigma/"+id+"/"+n);
				if (r.getStatusInfo().equals(Status.OK)) {
					ValueWrapper v = r.readEntity(new GenericType<ValueWrapper>() {});
					System.out.println(v.getValue());
				} else if (r.getStatusInfo().equals(Status.NOT_FOUND)){
					System.out.println("The id was not found");
				} else {
					System.out.println(r.getStatusInfo());
				}				
				break;
				
			case "condoavg":
				n = in.nextInt();
				r = http.get("stats/condoavg/"+n);
				if (r.getStatusInfo().equals(Status.OK)) {
					ValueWrapper v = r.readEntity(new GenericType<ValueWrapper>() {});
					System.out.println(v.getValue());
				} else if (r.getStatusInfo().equals(Status.NOT_FOUND)){
					System.out.println("The id was not found");
				} else {
					System.out.println(r.getStatusInfo());
				}				
				break;
				
			case "condosigma":
				n = in.nextInt();
				r = http.get("stats/condosigma/"+n);
				if (r.getStatusInfo().equals(Status.OK)) {
					ValueWrapper v = r.readEntity(new GenericType<ValueWrapper>() {});
					System.out.println(v.getValue());
				} else if (r.getStatusInfo().equals(Status.NOT_FOUND)){
					System.out.println("The id was not found");
				} else {
					System.out.println(r.getStatusInfo());
				}				
				break;
			}

		}
		
		http.get("admin/stop/"+ip+"/"+port);
		listener.shutdown();
		try {
			listener.awaitTermination();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		in.close();
	}

	public static class ServiceProvider extends AdminServiceImplBase {
		
		@Override
		public void notifyHouseJoined(Identifier request, StreamObserver<Empty> r) {
			System.out.println(request.getId()+" joined the network");
			r.onNext(Empty.getDefaultInstance());
			r.onCompleted();
		}
		
		@Override
		public void notifyHouseLeft(Identifier request, StreamObserver<Empty> r) {
			System.out.println(request.getId()+" left the network");
			r.onNext(Empty.getDefaultInstance());
			r.onCompleted();
		}
		
		@Override
		public void notifyBoostStart(Identifier request, StreamObserver<Empty> r) {
			System.out.println(request.getId()+" started boosting");
			r.onNext(Empty.getDefaultInstance());
			r.onCompleted();
		}
		
		@Override
		public void notifyBoostStop(Identifier request, StreamObserver<Empty> r) {
			System.out.println(request.getId()+" ended boosting");
			r.onNext(Empty.getDefaultInstance());
			r.onCompleted();
		}
	}
}
