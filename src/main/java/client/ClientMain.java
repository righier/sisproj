package client;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import beans.House;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import proto.AdminServiceGrpc.AdminServiceImplBase;
import proto.HouseProto.Empty;
import proto.HouseProto.Identifier;
import utils.Http;

public class ClientMain {

	public static void main(String args[]) {
	
		Response r;
		
		String serverAddr = "http://localhost:8000";
		String ip = "localhost";
		int port = 0;
		
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
		if (r.getStatusInfo().equals(Status.OK)) {
			System.out.println("Can't subscribe to REST server");
			System.out.println(r.getStatusInfo());
			System.exit(1);
		}
		
		Scanner in = new Scanner(System.in);
		boolean running = true;
		
		while (running) {
			
			String s = in.next();
			
			switch(s) {
			
			case "exit":
				running = false;
				break;
				
			case "list":
				r = http.get("houses");
				if (r.getStatusInfo().equals(Status.OK)) {
					
				} else {
					System.out.println(r.getStatusInfo());
				}
				break;
				
			case "house":
				String id = in.next();
				int n = in.nextInt();
				r = http.get("stats/house/"+id+"/"+n);
				if (r.getStatusInfo().equals(Status.OK)) {
					List<House> houses = r.readEntity(new GenericType<List<House>>() {});
					for (House h: houses) {
						System.out.println(h);
					}
				} else if (r.getStatusInfo().equals(Status.NOT_FOUND)){
					System.out.println("The id was not found");
				} else {
					System.out.println(r.getStatusInfo());
				}
				break;
			
			case "condo":
				
				break;
				
			case "avg":
				
				break;
				
			case "sigma":
				
				break;
			}

		}
		
		http.get("admin/stop/"+ip+"/"+port);
		
		in.close();
	}

	public static class ServiceProvider extends AdminServiceImplBase {
		
		@Override
		public void notifyHouseJoined(Identifier request, StreamObserver<Empty> responseObserver) {
			// TODO Auto-generated method stub
			super.notifyHouseJoined(request, responseObserver);
		}
		
		@Override
		public void notifyHouseLeft(Identifier request, StreamObserver<Empty> responseObserver) {
			// TODO Auto-generated method stub
			super.notifyHouseLeft(request, responseObserver);
		}
		
		@Override
		public void notifyBoostStart(Identifier request, StreamObserver<Empty> responseObserver) {
			// TODO Auto-generated method stub
			super.notifyBoostStart(request, responseObserver);
		}
		
		@Override
		public void notifyBoostStop(Identifier request, StreamObserver<Empty> responseObserver) {
			// TODO Auto-generated method stub
			super.notifyBoostStop(request, responseObserver);
		}
	}
}
