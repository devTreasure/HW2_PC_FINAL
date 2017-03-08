package cs455.scaling.server;

import java.util.ArrayList;

public class MyThreadPool {

	private final int nThreads;
	private final PoolWorker[] threads;
	private final ArrayList taskList;

	public MyThreadPool( int nThreads) {
		this.nThreads = nThreads;
		taskList = new ArrayList();
		threads = new PoolWorker[nThreads];

		for (int i = 0; i < nThreads; i++) {
			threads[i] = new PoolWorker();
			threads[i].start();
		}
		System.out.println("Started " + nThreads + " worker threads");
	}

	public void execute(Runnable task) {
		synchronized (taskList) {
			taskList.add(task);
			taskList.notify();
		}
	}

	private class PoolWorker extends Thread {

		public void run() {
			Runnable task;

			while (true) {
				synchronized (taskList) {
					while (taskList.isEmpty()) {
						try {
							taskList.wait();
						} catch (InterruptedException e) {
							System.out.println("An error occurred while queue is waiting: " + e.getMessage());
						}
					}
					task = (Runnable) taskList.remove(0);
				}

				// If we don't catch RuntimeException,
				// the pool could leak threads
				try {
					task.run();
				} catch (RuntimeException e) {
					System.out.println("Thread pool is interrupted due to an issue: " + e.getMessage());
				}
			}
		}
	}
}