package utils;

public class Async {
	private static final ThreadPool pool = new ThreadPool(20);
	
	public static Worker run(Runnable r) {
		return pool.run(r);
	}

	private static class ThreadPool {
		private final BlockingStack<Worker> pool;
		
		public ThreadPool(int size) {
			pool = new BlockingStack<>(size);
			
			for (int i = 0; i < size; i++) {
				pool.push(new Worker(this));
			}
		}
		
		public Worker run(Runnable r) {
			return get().startJob(r);
		}
		
		public Worker get() {
			Worker w = pool.popOrNull();
			if (w == null) w = new Worker(this);
			return w;
		}
		
		public void add(Worker worker) {
			pool.push(worker);
		}
		
	}
	
	
	public static class Worker extends Thread {
		private final ThreadPool pool;

		private boolean running = true;
		private Runnable job = null;
		
		public Worker(ThreadPool pool) {
			this.pool = pool;
			start();
		}
		
		public void shutdown() {
			synchronized(this) {
				running = false;
			}
		}
		
		public boolean isBusy() {
			synchronized(this) {
				return job != null;
			}
		}
		
		public void waitAvailable() {
			synchronized(this) {
				while(job != null) {
					try {
						this.wait();
					} catch (InterruptedException e) {}
				}
			}
		}
		
		public Worker startJob(Runnable job) {
			synchronized(this) {
				if (running) {
					this.job = job;
					this.notify();
				}
			}
			return this;
		}
		
		@Override
		public void run() {
			while(running) {
				synchronized(this) {
					while (job == null) {
						if (!running) {
							return;
						}
						
						try {
							this.wait();
						} catch (InterruptedException e) {
							running = false;
							return;
						}
					}
				}
				
				job.run();
				
				synchronized(this) {
					job = null;
					this.notify();
				}
				
				pool.add(this);
				
			}
		}

	}
}
