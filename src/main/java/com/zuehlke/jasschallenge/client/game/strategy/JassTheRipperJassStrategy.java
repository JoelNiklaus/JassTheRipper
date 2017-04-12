package com.zuehlke.jasschallenge.client.game.strategy;

import com.zuehlke.jasschallenge.client.game.Game;
import com.zuehlke.jasschallenge.client.game.GameSession;
import com.zuehlke.jasschallenge.client.game.Round;
import com.zuehlke.jasschallenge.game.cards.Card;
import com.zuehlke.jasschallenge.game.mode.Mode;
import weka.classifiers.functions.MultilayerPerceptron;

import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Stream;


// TODO Only ML methods or include Jass Knowledge?
public class JassTheRipperJassStrategy implements JassStrategy {

	private MultilayerPerceptron mlp = new MultilayerPerceptron();

	@Override
	public Mode chooseTrumpf(Set<Card> availableCards, GameSession session, boolean isGschobe) {
		final List<Mode> allPossibleModes = Mode.standardModes();
		if (!isGschobe) {
			allPossibleModes.add(Mode.shift());
		}
		return allPossibleModes.get(new Random().nextInt(allPossibleModes.size()));
	}

	@Override
	public Card chooseCard(Set<Card> availableCards, GameSession session) {
		final Game currentGame = session.getCurrentGame();
		final Round round = currentGame.getCurrentRound();
		final Mode gameMode = round.getMode();

		return getPossibleCards(availableCards, round, gameMode)
				.findAny()
				.orElseThrow(() -> new RuntimeException("There should always be a card to play"));
	}

	private Stream<Card> getPossibleCards(Set<Card> availableCards, Round round, Mode gameMode) {
		return availableCards.stream().filter(card -> gameMode.canPlayCard(card, round.getPlayedCards(), round.getRoundColor(), availableCards));
	}

	private void mitTrumpfAbstechen() {

	}

	private void mitNichtTrumpfStechen() {

	}

	private void verwerfen() {
		
	}
}
