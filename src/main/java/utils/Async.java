package utils;

public class Async {
	private static final ThreadPool pool = new ThreadPool(0);

	public static Worker run(Runnable r) {
		return pool.run(r);
	}
	
	public static void waitAllIdle() {
		pool.waitAllIdle();
	}
	
	public static String id() {
		return id(Thread.currentThread());
	}
	
	public static String id(Thread t) {
		return "" + t.getId();
	}
	
	public static void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static class ThreadPool {
		private final BlockingStack<Worker> pool;
		private int poolSize = 0;
		
		private final Object lock = new Object();

		public ThreadPool(int size) {
			pool = new BlockingStack<>(size);

			for (int i = 0; i < size; i++) {
				pool.push(spawn());
			}
		}

		public Worker run(Runnable r) {
			return get().startJob(r);
		}
		
		public void waitAllIdle() {
			synchronized(lock) {
				while(pool.size() < poolSize) {
					try {
						this.wait();
					} catch (InterruptedException e) {}
				}
			}
		}
		
		private Worker spawn() {
			synchronized(lock) {
				poolSize++;
			}
			return new Worker(this);
		}

		private Worker get() {
			while(true) {
				Worker w = pool.popOrNull();
				if (w == null) return spawn();
				if (w.isRunning()) return w;
			}
		}

		private void add(Worker worker) {
			pool.push(worker);
			synchronized(lock) {
				this.notifyAll();
			}
		}

	}


	public static class Worker extends Thread {
		private final ThreadPool pool;

		private boolean running = true;
		private Runnable job = null;
		
		private final Object lock = new Object();

		public Worker(ThreadPool pool) {
			this.pool = pool;
			start();
		}

		public void shutdown() {
			synchronized(lock) {
				running = false;
				lock.notifyAll();
			}
		}
		
		public boolean isRunning() {
			synchronized(lock) {
				return running;
			}
		}

		public boolean isBusy() {
			synchronized(lock) {
				return job != null;
			}
		}

		public void waitAvailable() {
			synchronized(lock) {
				while(job != null) {
					try {
						lock.wait();
					} catch (InterruptedException e) {}
				}
			}
		}

		public Worker startJob(Runnable job) {
			synchronized(lock) {
				if (running) {
					this.job = job;
					lock.notifyAll();
				}
			}
			return this;
		}

		@Override
		public void run() {
			while(running) {
				synchronized(lock) {
					while (job == null) {
						if (!running) {
							return;
						}

						try {
							lock.wait();
						} catch (InterruptedException e) {
							running = false;
							return;
						}
					}
				}

				job.run();

				synchronized(lock) {
					job = null;
					lock.notifyAll();
				}

				pool.add(this);
			}
		}

	}
}
