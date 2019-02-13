package com.zuehlke.jasschallenge;

import com.zuehlke.jasschallenge.client.RemoteGame;
import com.zuehlke.jasschallenge.client.game.Player;
import com.zuehlke.jasschallenge.client.game.strategy.JassStrategy;
import com.zuehlke.jasschallenge.client.game.strategy.JassTheRipperJassStrategy;
import com.zuehlke.jasschallenge.client.game.strategy.RandomJassStrategy;
import com.zuehlke.jasschallenge.messages.type.SessionType;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Allows to test your bot against severals teams of Java random bots.
 * The challenge server must be running:
 * <pre>
 *     npm start
 * </pre>
 * And a tournament started.
 */
class ApplicationBotTryout {

	private static final String LOCAL_URL = "ws://localhost:3000";

	//CHALLENGE2017: Set your bot name
	private final static String BOT_NAME = "JassTheRipper";

	//CHALLENGE2017: Set your strategoy
	private final static JassStrategy MY_STRATEGY = new JassTheRipperJassStrategy();

	//CHALLENGE2017: Set the number of opponent teams with random bots
	private final static int NUMBER_OF_RANDOM_TEAMS = 1;

	private final static boolean TEST_HUMAN = true;

	public static void main(String[] args) throws Exception {

		int numThreads = NUMBER_OF_RANDOM_TEAMS * 2 + 2;
		if (TEST_HUMAN)
			numThreads = 3;
		ExecutorService executorService = Executors.newFixedThreadPool(numThreads);

		List<Future<RemoteGame>> futures = new LinkedList<>();
		if (!TEST_HUMAN) {
			for (int i = 0; i < NUMBER_OF_RANDOM_TEAMS; i++) {
				int teamId = i;
				futures.add(executorService.submit(() -> startGame(LOCAL_URL, new Player("RandomJavaBot" + teamId, new RandomJassStrategy()), SessionType.TOURNAMENT)));
				futures.add(executorService.submit(() -> startGame(LOCAL_URL, new Player("RandomJavaBot" + teamId, new RandomJassStrategy()), SessionType.TOURNAMENT)));
			}
		}
		futures.add(executorService.submit(() -> startGame(LOCAL_URL, new Player(BOT_NAME, MY_STRATEGY), SessionType.TOURNAMENT)));
		futures.add(executorService.submit(() -> startGame(LOCAL_URL, new Player(BOT_NAME, MY_STRATEGY), SessionType.TOURNAMENT)));

		if (TEST_HUMAN) {
			futures.add(executorService.submit(() -> startGame(LOCAL_URL, new Player(BOT_NAME, MY_STRATEGY), SessionType.TOURNAMENT)));
		}

		futures.forEach(ApplicationBotTryout::awaitFuture);
		executorService.shutdown();
	}

	private static <T> void awaitFuture(Future<T> future) {
		try {
			future.get();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
	}

	private static RemoteGame startGame(String targetUrl, Player myLocalPlayer, SessionType sessionType) throws Exception {

		final RemoteGame remoteGame = new RemoteGame(targetUrl, myLocalPlayer, sessionType);
		remoteGame.start();
		return remoteGame;
	}
}
