package house2;

import beans.Measurement;
import io.grpc.stub.StreamObserver;
import proto.HouseProto.BoostRequest;
import proto.HouseProto.BoostResponse;
import proto.HouseProto.Empty;
import proto.HouseProto.Hello;
import proto.HouseProto.Identifier;
import proto.HouseProto.MasterResponse;
import proto.HouseProto.Measure;
import proto.HouseServiceGrpc.HouseServiceImplBase;
import utils.StreamHelper;

public class ServiceProvider extends HouseServiceImplBase {
	private final HouseManager manager;

	public ServiceProvider(HouseManager manager) {
		this.manager = manager;
	}

	@Override
	public void joinNetwork(Hello request, StreamObserver<Measure> response) {
		if (!manager.isStopping()) {
			manager.addHouse(new HouseChannel(request.getId(), request.getAddress(), request.getPort()));
			for (Measurement m: manager.getMeasurements()) {
				response.onNext(m.toProtobuf());
			}
		}
		response.onCompleted();
	}

	@Override
	public void leaveNetwork(Identifier request, StreamObserver<Empty> response) {
		manager.removeHouse(request.getId());
		response.onCompleted();
	}

	@Override
	public void newMaster(Empty request, StreamObserver<MasterResponse> response) {
		boolean accept = !manager.isStopping();
		manager.setMaster(accept);
		response.onNext(MasterResponse.newBuilder().setAccept(accept).build());
		response.onCompleted();
	}

	@Override
	public StreamObserver<Measure> setMeasurements(StreamObserver<Empty> response) {
		return StreamHelper.simple(manager::update, StreamHelper.logError, () -> response.onCompleted());
	}

	@Override
	public void askBoost(BoostRequest request, StreamObserver<BoostResponse> response) {
		// TODO Auto-generated method stub
		super.askBoost(request, response);
	}

	@Override
	public void endBoost(Identifier request, StreamObserver<Empty> response) {
		// TODO Auto-generated method stub
		super.endBoost(request, response);
	}

}
