package to.joeli.jass.client.game.strategy.training;

import to.joeli.jass.client.game.Game;
import to.joeli.jass.client.game.strategy.helpers.Distribution;
import to.joeli.jass.game.cards.Card;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class CardsEstimator extends NeuralNetwork {

	public static final int NUM_OUTPUT_COLS = 4; // Other Players

	public static final Logger logger = LoggerFactory.getLogger(CardsEstimator.class);

	public CardsEstimator(boolean trainable) {
		super("cards", trainable);
	}


	/**
	 * Predict the card distribution of the hidden cards of the other players.
	 * This should help generating determinizations of better quality.
	 *
	 * @return
	 */
	public Map<Card, Distribution> predictCardDistribution(Game game, Set<Card> availableCards) {
		game = new Game(game);
		game.getCurrentPlayer().setCards(availableCards);

		final double[] distribution = null;// TODO invoke keras prediction here
		logger.info(Arrays.toString(distribution));

		Map<Card, Distribution> cardDistribution = new HashMap<>();
		return cardDistribution;
	}
}
