package com.zuehlke.jasschallenge;

import com.zuehlke.jasschallenge.client.RemoteGame;
import com.zuehlke.jasschallenge.client.game.Player;
import com.zuehlke.jasschallenge.client.game.strategy.JassStrategy;
import com.zuehlke.jasschallenge.client.game.strategy.JassTheRipperJassStrategy;
import com.zuehlke.jasschallenge.client.game.strategy.RandomJassStrategy;
import com.zuehlke.jasschallenge.client.game.strategy.StrengthLevel;
import com.zuehlke.jasschallenge.messages.type.SessionType;

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
	private static final String SERVER_URL = "ws://jass.joeli.to/";

	//CHALLENGE2017: Set your bot name
	private final static String BOT_NAME = "JassTheRipper";

	//CHALLENGE2017: Set your strategy
	private final static JassStrategy MY_STRATEGY = new JassTheRipperJassStrategy(StrengthLevel.STRONG);

	//CHALLENGE2017: Set the number of opponent teams with random bots
	private final static int NUMBER_OF_RANDOM_TEAMS = 0;

	public static void main(String[] args) throws Exception {
		String url = LOCAL_URL;
		SessionType sessionType = SessionType.SINGLE_GAME;
		boolean TEST_HUMAN = false;


		int numThreads = NUMBER_OF_RANDOM_TEAMS * 2 + 2;
		if (TEST_HUMAN)
			numThreads = 3;
		ExecutorService executorService = Executors.newFixedThreadPool(numThreads);

		List<Future<RemoteGame>> futures = new LinkedList<>();

		if (!TEST_HUMAN) {
			for (int i = 0; i < NUMBER_OF_RANDOM_TEAMS; i++) {
				int teamId = i;
				futures.add(executorService.submit(() -> startGame(url, new Player("RandomJavaBot" + teamId, new RandomJassStrategy()), sessionType)));
				futures.add(executorService.submit(() -> startGame(url, new Player("RandomJavaBot" + teamId, new RandomJassStrategy()), sessionType)));
			}
		}
		futures.add(executorService.submit(() -> startGame(url, new Player(BOT_NAME, MY_STRATEGY), sessionType)));
		futures.add(executorService.submit(() -> startGame(url, new Player(BOT_NAME, MY_STRATEGY), sessionType)));

		if (TEST_HUMAN) {
			futures.add(executorService.submit(() -> startGame(url, new Player(BOT_NAME, MY_STRATEGY), sessionType)));
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
