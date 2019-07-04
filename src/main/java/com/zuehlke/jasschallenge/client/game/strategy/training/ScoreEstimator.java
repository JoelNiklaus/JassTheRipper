package com.zuehlke.jasschallenge.client.game.strategy.training;

import com.zuehlke.jasschallenge.client.game.Game;
import com.zuehlke.jasschallenge.client.game.Player;
import com.zuehlke.jasschallenge.client.game.strategy.helpers.CardSelectionHelper;
import com.zuehlke.jasschallenge.client.game.strategy.helpers.NeuralNetworkHelper;
import com.zuehlke.jasschallenge.client.game.strategy.mcts.CardMove;
import com.zuehlke.jasschallenge.game.cards.Card;
import org.nd4j.evaluation.regression.RegressionEvaluation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class ScoreEstimator extends NeuralNetwork {

	public static final Logger logger = LoggerFactory.getLogger(ScoreEstimator.class);

	public ScoreEstimator() {

	}

	public ScoreEstimator(ScoreEstimator scoreEstimator) {
		super(scoreEstimator);
	}

	public void evaluate(DataSet dataSet) {
		RegressionEvaluation evaluation = new RegressionEvaluation();
		final INDArray predictions = model.output(dataSet.getFeatures());
		System.out.println(predictions);
		evaluation.eval(dataSet.getLabels(), predictions);
		System.out.println(evaluation.stats());
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
		return Arena.TOTAL_POINTS * predict(Collections.singletonList(NeuralNetworkHelper.getObservation(game)))[0];
	}


	/**
	 * Performs a forward pass through the network and returns the regressed values.
	 *
	 * @param observations
	 * @return
	 */
	private double[] predict(List<INDArray> observations) {
		INDArray input = NeuralNetworkHelper.buildInput(observations);
		final INDArray output = model.output(input);
		return output.toDoubleVector();
	}
}
