package utils;

import java.util.ArrayList;

public class BlockingStack<T> {
	
	private final ArrayList<T> list;
	
	public BlockingStack() {
		list = new ArrayList<>();
	}
	
	public BlockingStack(int capacity) {
		list = new ArrayList<>(capacity);
	}
	
	public synchronized void push(T e) {
		list.add(e);
		this.notify();
	}
	
	public synchronized T pop() {
		while(list.isEmpty()) {
			try {
				this.wait();
			} catch (InterruptedException e1) {}
		}
		
		return list.remove(list.size() - 1);
	}
	
	public synchronized T popOrNull() {
		if (list.isEmpty()) {
			return null;
		}
		
		return list.remove(list.size() - 1);
	}

}
