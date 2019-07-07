package com.zuehlke.jasschallenge.client.game.strategy.training;

import com.zuehlke.jasschallenge.client.game.Game;
import com.zuehlke.jasschallenge.client.game.Player;
import com.zuehlke.jasschallenge.client.game.strategy.helpers.Distribution;
import com.zuehlke.jasschallenge.client.game.strategy.helpers.NeuralNetworkHelper;
import com.zuehlke.jasschallenge.game.cards.Card;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class CardsEstimator extends NeuralNetwork {

	public static final int NUM_INPUT_ROWS = 1 + 36 + 9;
	public static final int INPUT_DIM = NUM_INPUT_ROWS * THREE_HOT_ENCODING_LENGTH;
	public static final int NUM_OUTPUT_ROWS = 36; // Cards
	public static final int NUM_OUTPUT_COLS = 4; // Other Players
	public static final int OUTPUT_DIM = NUM_OUTPUT_ROWS * NUM_OUTPUT_COLS * THREE_HOT_ENCODING_LENGTH;

	public static final Logger logger = LoggerFactory.getLogger(CardsEstimator.class);

	public CardsEstimator() {

	}

	public CardsEstimator(CardsEstimator cardsEstimator) {
		super(cardsEstimator);
	}

	/**
	 * Predict the card distribution of the hidden cards of the other players.
	 * This should help generating determinizations of better quality.
	 *
	 * @return
	 */
	public Map<Card, Distribution<Player>> predictCardDistribution(Game game, Set<Card> availableCards) {
		game = new Game(game);
		game.getCurrentPlayer().setCards(availableCards);

		final INDArray observation = NeuralNetworkHelper.getObservation(game);
		INDArray input = NeuralNetworkHelper.buildInput(Collections.singletonList(observation));
		final INDArray output = model.output(input);
		final double[] distribution = output.toDoubleVector();
		logger.info(Arrays.toString(distribution));

		Map<Card, Distribution<Player>> cardDistribution = new HashMap<>();
		return cardDistribution;
	}
}
