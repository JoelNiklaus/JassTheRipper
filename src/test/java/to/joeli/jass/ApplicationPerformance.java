package to.joeli.jass;

import to.joeli.jass.client.RemoteGame;
import to.joeli.jass.client.game.Player;
import to.joeli.jass.client.strategy.RandomJassStrategy;
import to.joeli.jass.messages.type.SessionType;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Measures performance of java client.
 */
class ApplicationPerformance {

	public static final String LOCAL_URL = "ws://localhost:3000";

	private final static String BOT_NAME = "performanceBot";

	public static void main(String[] args) throws Exception {

		ExecutorService executorService = Executors.newFixedThreadPool(4);

		double startNano = System.nanoTime();
		List<Future<RemoteGame>> futures = new LinkedList<>();
		for (int i = 0; i < 4; i++) {
			int finalI = i;
			futures.add(executorService.submit(() -> startGame(LOCAL_URL, new Player(BOT_NAME + finalI, new RandomJassStrategy()), SessionType.SINGLE_GAME)));
		}
		futures.forEach(ApplicationPerformance::awaitFuture);
		double durationNanos = System.nanoTime() - startNano;

		executorService.shutdown();
		System.out.println("Duration in seconds " + (durationNanos / 1000000000d));
	}

	private static void startTournamentGame(String targetUrl, Player myLocalPlayer, Player myLocalPartner) throws Exception {
		ExecutorService executorService = Executors.newFixedThreadPool(2);
		executorService
				.invokeAll(Arrays.asList(() -> startGame(targetUrl, myLocalPlayer, SessionType.TOURNAMENT),
						() -> startGame(targetUrl, myLocalPartner, SessionType.TOURNAMENT)))
				.forEach(ApplicationPerformance::awaitFuture);
		executorService.shutdown();
	}

	private static <T> void awaitFuture(Future<T> future) {
		try {
			future.get();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
	}

	private static RemoteGame startGame(String targetUrl, Player myLocalPlayer, SessionType sessionType) {

		final RemoteGame remoteGame = new RemoteGame(targetUrl, myLocalPlayer, sessionType);
		remoteGame.start();
		return remoteGame;
	}
}
