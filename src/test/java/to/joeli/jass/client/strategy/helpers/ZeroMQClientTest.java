package to.joeli.jass.client.strategy.helpers;

import org.junit.Test;
import to.joeli.jass.client.game.Game;

public class ZeroMQClientTest {

	@Test
	public void testNormalLifeCycleRunsWithoutProblems() throws InterruptedException {
		Game clubsGame = GameSessionBuilder.startedClubsGame();

		final double[][] scoreFeatures = NeuralNetworkHelper.getScoreFeatures(clubsGame);

		ZeroMQClient.startServer();

		ZeroMQClient.predictCards(scoreFeatures);
		ZeroMQClient.predictScore(scoreFeatures);

		ZeroMQClient.stopServer();
	}
}
