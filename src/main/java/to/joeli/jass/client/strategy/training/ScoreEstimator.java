package to.joeli.jass.client.strategy.training;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tensorflow.Tensor;
import to.joeli.jass.client.game.Game;
import to.joeli.jass.client.game.Player;
import to.joeli.jass.client.strategy.helpers.CardSelectionHelper;
import to.joeli.jass.client.strategy.helpers.NeuralNetworkHelper;
import to.joeli.jass.client.strategy.mcts.CardMove;
import to.joeli.jass.game.cards.Card;

import java.util.*;

public class ScoreEstimator extends NeuralNetwork {


	public static final Logger logger = LoggerFactory.getLogger(ScoreEstimator.class);

	public ScoreEstimator(boolean trainable) {
		super(NetworkType.SCORE, trainable);
	}


	/**
	 * Predicts a move based on the neural network predictions for states after a possible card is played
	 *
	 * @param game
	 * @return
	 */
	public CardMove predictMove(Game game) {
		Set<Card> possibleCards = CardSelectionHelper.getCardsPossibleToPlay(EnumSet.copyOf(game.getCurrentPlayer().getCards()), game);

		if (possibleCards.isEmpty()) throw new AssertionError();

		EnumMap<Card, Double> actionValuePairs = new EnumMap<>(Card.class);
		for (Card card : possibleCards) {
			Game clonedGame = new Game(game);
			final Player player = clonedGame.getCurrentPlayer();
			final CardMove move = new CardMove(player, card);
			clonedGame.makeMove(move);
			player.onMoveMade(move);

			// TODO maybe it is more efficient to do all the forward passes at the same time and not one by one
			// NOTE: 157 - value because the value is from the perspective of a player of the opponent team
			actionValuePairs.put(card, Arena.TOTAL_POINTS - predictScore(clonedGame));
		}
		Card bestCard = actionValuePairs.entrySet()
				.stream()
				.min(Map.Entry.comparingByValue(Comparator.reverseOrder()))
				.map(Map.Entry::getKey)
				.orElse(null);

		return new CardMove(game.getCurrentPlayer(), bestCard);
	}

	/**
	 * Predict the final score of a determinized game (perfect information)
	 *
	 * @param game
	 * @return
	 */
	public double predictScore(Game game) {
		final Tensor result = (Tensor) predict(NeuralNetworkHelper.getScoreFeatures(game));
		float[][] res = new float[1][1];
		result.copyTo(res);
		return (double) res[0][0];
		//return ZeroMQClient.predictScore(NeuralNetworkHelper.getScoreFeatures(game));
	}

}
