package com.zuehlke.jasschallenge.client.game.strategy.training;

import com.zuehlke.jasschallenge.client.game.Game;
import com.zuehlke.jasschallenge.client.game.Player;
import com.zuehlke.jasschallenge.client.game.strategy.helpers.GameSessionBuilder;
import com.zuehlke.jasschallenge.client.game.strategy.helpers.NeuralNetworkHelper;
import org.junit.Test;

import java.util.HashMap;

import static com.zuehlke.jasschallenge.client.game.strategy.training.Arena.IMPROVEMENT_THRESHOLD_PERCENTAGE;
import static org.junit.Assert.assertArrayEquals;

public class ArenaTest {

	private Arena arena = new Arena(2, 2, IMPROVEMENT_THRESHOLD_PERCENTAGE, Arena.SEED);

	@Test
	public void testBuildCardsLabels() {
		final Game game = GameSessionBuilder.startedClubsGame();

		final HashMap<Player, int[][]> cardsLabels = NeuralNetworkHelper.buildCardsLabels(game);
		System.out.println(cardsLabels);

		final int[][] cardsOfPlayers = cardsLabels.get(game.getCurrentPlayer());
		assertArrayEquals(new int[]{1, 0, 0, 0}, cardsOfPlayers[0]); // Current player is first
		assertArrayEquals(new int[]{0, 0, 1, 0}, cardsOfPlayers[1]);
		assertArrayEquals(new int[]{0, 1, 0, 0}, cardsOfPlayers[2]);
		assertArrayEquals(new int[]{0, 1, 0, 0}, cardsOfPlayers[3]);
		assertArrayEquals(new int[]{0, 0, 0, 1}, cardsOfPlayers[4]);

	}

	@Test
	public void train() {
		// NOTE: Do not run for normal tests. Takes way too much time.
		// arena.trainForNumEpisodes(1);
	}

	@Test
	public void trainUntilBetterThanRandomPlayouts() {
		// NOTE: Do not run for normal tests. Takes way too much time.
		// arena.trainUntilBetterThanRandomPlayouts();
	}
}
