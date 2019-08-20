package house2;

import io.grpc.stub.StreamObserver;
import proto.HouseProto.Empty;
import proto.HouseProto.Hello;
import proto.HouseProto.Identifier;
import proto.HouseProto.Measure;
import proto.HouseServiceGrpc.HouseServiceImplBase;

public class ServiceProvider extends HouseServiceImplBase {
	
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
	public void newMaster(Identifier request, StreamObserver<Empty> responseObserver) {
		// TODO Auto-generated method stub
		super.newMaster(request, responseObserver);
	}
	
	@Override
	public StreamObserver<Measure> setMeasurements(StreamObserver<Empty> responseObserver) {
		// TODO Auto-generated method stub
		return super.setMeasurements(responseObserver);
	}
	
}
