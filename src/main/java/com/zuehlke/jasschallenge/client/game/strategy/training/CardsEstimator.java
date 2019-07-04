package com.zuehlke.jasschallenge.client.game.strategy.training;

import com.zuehlke.jasschallenge.client.game.Player;
import com.zuehlke.jasschallenge.client.game.strategy.helpers.Distribution;
import com.zuehlke.jasschallenge.game.cards.Card;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class CardsEstimator extends NeuralNetwork {

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
	public Map<Card, Distribution<Player>> predictCardDistribution() {
		return null;
	}
}
