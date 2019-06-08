package com.zuehlke.jasschallenge.client.game.strategy;

import org.junit.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * Created by joelniklaus on 06.05.17.
 */
public class ExecutorServiceTest {

	@Test
	public void testExecutorServiceStartAndShutdownTime() {
		long startTime = System.nanoTime();
		ExecutorService threadpool = Executors.newFixedThreadPool(50);
		System.out.println(System.nanoTime() - startTime);

		startTime = System.nanoTime();
		threadpool.shutdown();
		if (threadpool.isTerminated())
			System.out.println(System.nanoTime() - startTime);
	}
}