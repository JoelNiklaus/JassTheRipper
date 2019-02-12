package com.zuehlke.jasschallenge.client.game.strategy;

import com.zuehlke.jasschallenge.client.game.strategy.JassStrategy;
import com.zuehlke.jasschallenge.game.Trumpf;
import com.zuehlke.jasschallenge.game.cards.Card;
import com.zuehlke.jasschallenge.game.cards.CardValue;
import com.zuehlke.jasschallenge.game.cards.Color;
import com.zuehlke.jasschallenge.game.mode.Mode;
import org.junit.Test;
import org.mockito.Matchers;

import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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