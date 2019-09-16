package utils;

public class WorkerPool {
	private final BlockingStack<Worker> pool;
	private int poolSize = 0;
	
	private final Object lock = new Object();

	public WorkerPool(int size) {
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
					lock.wait();
				} catch (InterruptedException e) {}
			}
		}
	}
	
	public void shutdown() {
		synchronized(lock) {
			while (poolSize > 0) {
				pool.pop().shutdown();
				poolSize--;
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
			lock.notifyAll();
		}
	}

	
	public class Worker extends Thread {
		private final WorkerPool pool;

		private boolean running = true;
		private Runnable job = null;
		
		private final Object lock = new Object();

		public Worker(WorkerPool pool) {
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


