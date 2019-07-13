package to.joeli.jass.client.game.strategy.training;

import to.joeli.jass.client.game.Game;
import to.joeli.jass.client.game.Player;
import to.joeli.jass.client.game.strategy.helpers.CardSelectionHelper;
import to.joeli.jass.client.game.strategy.helpers.NeuralNetworkHelper;
import to.joeli.jass.client.game.strategy.mcts.CardMove;
import to.joeli.jass.game.cards.Card;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class ScoreEstimator extends NeuralNetwork {



	public static final Logger logger = LoggerFactory.getLogger(ScoreEstimator.class);

	public ScoreEstimator(boolean trainable) {
		super("score", trainable);
	}


	public CardMove predictMove(Game game) {
		final Player player = game.getCurrentPlayer();
		Set<Card> possibleCards = CardSelectionHelper.getCardsPossibleToPlay(EnumSet.copyOf(player.getCards()), game);

		assert !possibleCards.isEmpty();

		EnumMap<Card, Double> actionValuePairs = new EnumMap<>(Card.class);
		for (Card card : possibleCards) {
			Game clonedGame = new Game(game);
			clonedGame.makeMove(new CardMove(player, card));

			// TODO maybe it is more efficient to do all the forward passes at the same time and not one by one
			actionValuePairs.put(card, Arena.TOTAL_POINTS - predictScore(clonedGame)); // NOTE: 157 - value because the value is from the perspective of a player of the opponent team
		}
		Card bestCard = actionValuePairs.entrySet()
				.stream()
				.min(Map.Entry.comparingByValue(Comparator.reverseOrder()))
				.map(Map.Entry::getKey)
				.orElse(null);

		return new CardMove(player, bestCard);
	}

	/**
	 * Predict the final score of a determinized game (perfect information)
	 *
	 * @param game
	 * @return
	 */
	public double predictScore(Game game) {
		// INFO: We disregard the match bonus for simplicity
		return predict(Collections.singletonList(NeuralNetworkHelper.getScoreFeatures(game)))[0];
	}


	/**
	 * Performs a forward pass through the network and returns the regressed values.
	 *
	 * @param observations
	 * @return
	 */
	private double[] predict(List<double[][]> observations) {
		// TODO invoke keras prediction here!
		return null;
	}
}
