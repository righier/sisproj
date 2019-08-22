import static org.junit.Assert.assertTrue;

import java.util.HashSet;

import org.junit.Test;

import utils.Async;
import utils.Async.Worker;
import utils.Barrier;

public class AsyncTest {

	@Test
	public void myTest() {
		
		final HashSet<String> ids = new HashSet<>();
		
		final int n = 100;
		
		Barrier b = new Barrier();
		for (int i = 0; i < n; i++) {
			b.add(Async.run(() -> {
				assertTrue(Thread.currentThread() instanceof Worker);
				synchronized(ids) {
					
					ids.add(Async.id());
					ids.notifyAll();
					
					while(ids.size() < n) {
						try {
							ids.wait();
						} catch (InterruptedException e) {}
					}
				}
			}));
		}
		
		
		b.await();
		
		// checks that no thread is used more than one time
		assertTrue( ids.size() == n );
		
		for (int i = 0; i < n; i++) {
			b.add(Async.run(() -> {
				
				// checks that on the second run all the threads are being reused
				assertTrue( ids.contains(Async.id()) );
			}));
		}
		
	}
}
