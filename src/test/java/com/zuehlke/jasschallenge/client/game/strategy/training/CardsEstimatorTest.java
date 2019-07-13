package com.zuehlke.jasschallenge.client.game.strategy.training;

import com.zuehlke.jasschallenge.client.game.Game;
import com.zuehlke.jasschallenge.client.game.strategy.helpers.GameSessionBuilder;
import com.zuehlke.jasschallenge.game.cards.Card;
import com.zuehlke.jasschallenge.game.cards.Color;
import com.zuehlke.jasschallenge.game.mode.Mode;
import org.junit.Test;

import java.io.File;
import java.util.EnumSet;
import java.util.Set;

public class CardsEstimatorTest {

	@Test
	public void testCardsEstimatorPrediction() {
		// TODO Unsupported Keras Configuration Exception! --> use zeromq server

		Game diamondsGame = GameSessionBuilder.newSession().withStartedGame(Mode.trump(Color.DIAMONDS)).createGameSession().getCurrentGame();
		Set<Card> availableCards = diamondsGame.getCurrentPlayer().getCards();
		// Delete the cards of the players because we want to estimate them.
		diamondsGame.getPlayers().forEach(player -> player.setCards(EnumSet.noneOf(Card.class)));


		CardsEstimator network = new CardsEstimator(true);
		if (!new File(Arena.CARDS_ESTIMATOR_KERAS_PATH).exists())
			network.train(TrainMode.PRE_TRAIN);

		System.out.println(network.predictCardDistribution(diamondsGame, availableCards));
	}

}