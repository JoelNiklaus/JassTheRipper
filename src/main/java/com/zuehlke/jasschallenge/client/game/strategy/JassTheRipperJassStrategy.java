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

	// TODO hilfsmethoden bockVonJederFarbe, TruempfeNochImSpiel, statistisches Modell von möglichen Karten von jedem Spieler

	// wähle trumpf mit besten voraussetzungen -> ranking
	// bei drei sicheren stichen -> obeabe oder undeufe
	//
	// wenn nicht gut -> schieben
	@Override
	public Mode chooseTrumpf(Set<Card> availableCards, GameSession session, boolean isGschobe) {
		final List<Mode> allPossibleModes = Mode.standardModes();
		if (!isGschobe) {
			allPossibleModes.add(Mode.shift());
		}
		System.out.println("ChooseTrumpf!");
		return Mode.topDown();
		//return allPossibleModes.get(new Random().nextInt(allPossibleModes.size()));
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

	// wenn letzter spieler und nicht möglich nicht mit trumpf zu stechen, dann stechen
	private void mitTrumpfAbstechen() {

	}

	// Wenn letzter Spieler und möglich mit nicht trumpf zu stechen, dann stechen.
	private void mitNichtTrumpfStechen() {

	}

	// Wenn obeabe oder undeufe: Bei Ausspielen von Partner tiefe Karte (tiefer als 10) von Gegenfarbe verwerfen wenn bei Farbe gut.
	private void gegenFarbeVerwerfen() {

	}

}
