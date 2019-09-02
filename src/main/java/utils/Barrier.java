package utils;

import java.util.ArrayList;
import java.util.List;

import utils.WorkerPool.Worker;

public class Barrier {
	
	private List<Worker> workers = new ArrayList<>();
	
	public synchronized void add(Worker t) {
		workers.add(t);
	}
	
	public synchronized void await() {
		for (Worker worker: workers) {
			worker.waitAvailable();
		}

		workers.clear();
	}
	
	public synchronized boolean isDone() {
		return workers.isEmpty();
	}

}
