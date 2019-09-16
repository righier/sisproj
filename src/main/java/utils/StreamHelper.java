package utils;

import java.util.function.Consumer;

import io.grpc.stub.StreamObserver;


public class StreamHelper {
	static public Consumer<Throwable> logError = t -> t.printStackTrace();

	static public <T> StreamObserver<T> ignore() {
		return simple(
				null, 
				t -> t.printStackTrace(), 
				null);
	}

	static public <T> StreamObserver<T> simple(Consumer<T> next, Consumer<Throwable> error, Runnable complete) {
		return new StreamObserver<T>() {
			@Override public void onNext(T value) { 
				if (next != null) next.accept(value); 
			}
			@Override public void onError(Throwable t) { 
				if (error != null) error.accept(t); 
			}
			@Override public void onCompleted() { 
				if (complete != null) complete.run(); 
			}
		};
	}


}

