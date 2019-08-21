package utils;

import io.grpc.stub.StreamObserver;


public class StreamHelper {
	
	static public <T> Ignore<T> ignore() {
		return new Ignore<T>();
	}

	static private class Ignore<T> implements StreamObserver<T> {

		@Override
		public void onNext(T value) {
			
		}

		@Override
		public void onError(Throwable t) {
			t.printStackTrace();
		}

		@Override
		public void onCompleted() {

		}
		
	}
}

