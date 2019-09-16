import static org.junit.Assert.assertTrue;

import java.util.HashSet;

import org.junit.Test;

import utils.Async;
import utils.WorkerPool;
import utils.WorkerPool.Worker;

public class AsyncTest {

	@Test
	public void myTest() {
		
		final HashSet<String> ids = new HashSet<>();
		
		final int n = 100;
		
		WorkerPool async = new WorkerPool(20);
		
		for (int i = 0; i < n; i++) {
			async.run(() -> {
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
			});
		}
		
		async.waitAllIdle();
		
		// checks that no thread is used more than one time
		assertTrue( ids.size() == n );
		
		for (int i = 0; i < n; i++) {
			async.run(() -> {
				
				// checks that on the second run all the threads are being reused
				assertTrue( ids.contains(Async.id()) );
			});
		}
		
		async.waitAllIdle();
		
	}
}
