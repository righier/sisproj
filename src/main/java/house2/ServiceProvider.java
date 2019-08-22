package house2;

import io.grpc.stub.StreamObserver;
import proto.HouseProto.BoostRequest;
import proto.HouseProto.BoostResponse;
import proto.HouseProto.Empty;
import proto.HouseProto.Hello;
import proto.HouseProto.Identifier;
import proto.HouseProto.MasterResponse;
import proto.HouseProto.Measure;
import proto.HouseServiceGrpc.HouseServiceImplBase;

public class ServiceProvider extends HouseServiceImplBase {
	private final HouseManager manager;
	
	public ServiceProvider(HouseManager manager) {
		this.manager = manager;
	}
	
	@Override
	public void joinNetwork(Hello request, StreamObserver<Measure> response) {
		System.out.println("message Thread: "+Thread.currentThread().getId());
		response.onNext(Measure.newBuilder()
				.setId("alskdjf")
				.setValue(23.23)
				.setTimestamp(12345)
				.build());
		response.onCompleted();
	}
	
	@Override
	public void leaveNetwork(Identifier request, StreamObserver<Empty> responseObserver) {
		// TODO Auto-generated method stub
		super.leaveNetwork(request, responseObserver);
	}
	
	@Override
	public void newMaster(Empty request, StreamObserver<MasterResponse> responseObserver) {
		// TODO Auto-generated method stub
		super.newMaster(request, responseObserver);
	}
	
	@Override
	public StreamObserver<Measure> setMeasurements(StreamObserver<Empty> responseObserver) {
		// TODO Auto-generated method stub
		return super.setMeasurements(responseObserver);
	}
	
	@Override
	public void askBoost(BoostRequest request, StreamObserver<BoostResponse> responseObserver) {
		// TODO Auto-generated method stub
		super.askBoost(request, responseObserver);
	}
	
	@Override
	public void endBoost(Identifier request, StreamObserver<Empty> responseObserver) {
		// TODO Auto-generated method stub
		super.endBoost(request, responseObserver);
	}
	
}
