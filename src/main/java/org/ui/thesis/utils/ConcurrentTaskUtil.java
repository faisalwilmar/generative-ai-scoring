package org.ui.thesis.utils;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class ConcurrentTaskUtil {

	/**
	 * Runs a collection of tasks and collects any exceptions into a thread-safe queue.
	 * The method returns a list of Future objects, allowing the caller to retrieve the
	 * return value (T) from successful tasks later if needed.
	 * @param tasks The list of Callable<T> tasks to execute.
	 * @param errorQueue The thread-safe queue to collect exceptions into.
	 * @param executor The ExecutorService managing the thread pool.
	 * @return List of Future<T> objects for further processing, if necessary.
	 */
	public static <T> List<Future<T>> runAllTasks(Collection<Callable<T>> tasks,
			ConcurrentLinkedQueue<Exception> errorQueue, ExecutorService executor) throws InterruptedException {

		// invokeAll submits all tasks and blocks until all are complete.
		List<Future<T>> futures = executor.invokeAll(tasks);

		// After all threads finish, we iterate over the Futures to explicitly
		// check for any exceptions that weren't caught internally.
		for (Future<T> future : futures) {
			try {
				// Calling future.get() will re-throw any uncaught exception
				// that occurred in the thread, wrapped in ExecutionException.
				future.get();
			}
			catch (ExecutionException e) {
				// Catch the wrapper exception and add the underlying cause to our queue
				errorQueue.add(e);
			}
			catch (Exception e) {
				// Catch any other exceptions (like InterruptedException)
				errorQueue.add(e);
			}
		}

		// Return the Futures so the caller can check the results of successful tasks
		return futures;
	}

}
