package com.zuehlke.jasschallenge.client.game.strategy;

import com.zuehlke.jasschallenge.client.game.Game;
import com.zuehlke.jasschallenge.client.game.GameSession;
import com.zuehlke.jasschallenge.client.game.Round;
import com.zuehlke.jasschallenge.game.Trumpf;
import com.zuehlke.jasschallenge.game.cards.Card;
import com.zuehlke.jasschallenge.game.cards.Color;
import com.zuehlke.jasschallenge.game.mode.Mode;
import weka.classifiers.functions.MultilayerPerceptron;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;


// TODO Only ML methods or include Jass Knowledge?
public class JassTheRipperJassStrategy implements JassStrategy {
	private final int max_schift_rating_val = 30;

	private MultilayerPerceptron mlp = new MultilayerPerceptron();

	// TODO hilfsmethoden bockVonJederFarbe, TruempfeNochImSpiel, statistisches Modell von möglichen Karten von jedem Spieler
	// TODO alle gespielten Karten merken

	// wähle trumpf mit besten voraussetzungen -> ranking
	// bei drei sicheren stichen -> obeabe oder undeufe
	//
	// wenn nicht gut -> schieben
	@Override
	public Mode chooseTrumpf(Set<Card> availableCards, GameSession session, boolean isGschobe) {
		System.out.println("ChooseTrumpf!");
		int max = 0;
		Mode prospectiveMode = Mode.from(Trumpf.TRUMPF, Color.CLUBS);
		for (Color color : Color.values()) {
			int colorTrumpRating = rate(availableCards, color);
			if (colorTrumpRating > max) {
				max = colorTrumpRating;
				prospectiveMode = Mode.from(Trumpf.TRUMPF, color);
			}
		}
		if (rateObeabe(availableCards) > max)
			prospectiveMode = Mode.topDown();
		if (rateUndeufe(availableCards) > max)
			prospectiveMode = Mode.bottomUp();
		System.out.println("ChooseTrumpf succeeded!");
		if (max < max_schift_rating_val)
			return Mode.shift();
		return prospectiveMode;
	}

	private int rate(Set<Card> cardStream, Color color) {
		return 30;
	}

	private int rateObeabe(Set<Card> cards) {
		int sum = 0;
		for (Color color : Color.values()) {
			sum += rateObeabeColor(cards, color);
		}
		return sum;
	}

	private int rateUndeufe(Set<Card> cards) {
		int sum = 0;
		for (Color color : Color.values()) {
			sum += rateUndeufeColor(cards, color);
		}
		return sum;
	}

	/* Ass = 20, König = 12, Königin = 6, Bube = 3, Zehn = 1
	 */
	private int rateObeabeColor(Set<Card> cards, Color color) {
		Set<Card> cardsOfColor = getCardsOfColor(cards, color);
		List<Card> sortedCards = cardsOfColor.stream().sorted(Comparator.comparing(Card::getRank)).collect(Collectors.toList());
		if (sortedCards.stream().findFirst().isPresent())
			if (sortedCards.stream().findFirst().get().getValue().getRank() >= 8)
				return 3000;
		return 5;
	}

	// Sorts the wrong way round
	private int rateUndeufeColor(Set<Card> cards, Color color) {
		Set<Card> cardsOfColor = getCardsOfColor(cards, color);
		List<Card> sortedCards = cardsOfColor.stream().sorted(Comparator.comparing(Card::getRank)).collect(Collectors.toList());
		if (sortedCards.stream().findFirst().isPresent())
			if (sortedCards.stream().findFirst().get().getValue().getRank() <= 2)
				return 300;
		return 5;
	}

	private Set<Card> getCardsOfColor(Set<Card> cards, Color color) {
		return cards.stream().filter(card -> card.getColor().equals(color)).collect(Collectors.toSet());
	}


	@Override
	public Card chooseCard(Set<Card> availableCards, GameSession session) {
		final Game currentGame = session.getCurrentGame();
		final Round round = currentGame.getCurrentRound();
		final Mode gameMode = round.getMode();

		return getPossibleCards(availableCards, round, gameMode).stream()
				.findAny()
				.orElseThrow(() -> new RuntimeException("There should always be a card to play"));
	}

	private int countNumberOfCardsOfColor(Set<Card> availableCards, Color color) {
		return (int) availableCards.stream().filter(card -> card.getColor().equals(color)).count();
	}

	private Set<Card> getPossibleCards(Set<Card> availableCards, Round round, Mode gameMode) {
		return availableCards.stream().filter(card -> gameMode.canPlayCard(card, round.getPlayedCards(), round.getRoundColor(), availableCards)).collect(Collectors.toSet());
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
