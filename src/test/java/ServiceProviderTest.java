
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import house2.HouseManager;
import house2.ServiceProvider;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import proto.HouseProto.Hello;
import proto.HouseProto.Measure;
import proto.HouseServiceGrpc;
import proto.HouseServiceGrpc.HouseServiceStub;

public class ServiceProviderTest {
	HouseManager manager;

	@Before
	public void setUp() throws Exception {
		manager = new HouseManager("asdf", "localhost", 0, false);
		System.out.println("Server started");
	}


	@After
	public void tearDown() throws Exception {
		manager.stop();
	}

	@Test
	public void testThread() throws Exception {

		ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 8080)
		.usePlaintext()
		.build();
		

		HouseServiceStub stub = HouseServiceGrpc.newStub(channel);

		System.out.println("main thread: "+Thread.currentThread().getId());

		for (int i = 0; i < 10; i++) {

			stub.joinNetwork(Hello.newBuilder()
				.setId("my id")
				.setAddress("my address")
				.setPort(2384)
				.build(), new StreamObserver<Measure>() {

					@Override
					public void onNext(Measure value) {
						System.out.println(value);
					}

					@Override
					public void onError(Throwable t) {

					}

					@Override
					public void onCompleted() {

					}

				});

		}

		Thread.sleep(2000);
	}

}
