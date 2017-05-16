package com.zuehlke.jasschallenge.client.game.strategy;

import com.zuehlke.jasschallenge.client.game.*;
import com.zuehlke.jasschallenge.client.game.strategy.helpers.JassHelper;
import com.zuehlke.jasschallenge.client.game.strategy.helpers.MCTSHelper;
import com.zuehlke.jasschallenge.game.Trumpf;
import com.zuehlke.jasschallenge.game.cards.Card;
import com.zuehlke.jasschallenge.game.cards.Color;
import com.zuehlke.jasschallenge.game.mode.Mode;

import java.io.Serializable;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

// TODO BAD_MESSAGE überprüfen. Spielt karte statt trump. ???


public class JassTheRipperJassStrategy extends RandomJassStrategy implements JassStrategy, Serializable {

	// the maximal number of milliseconds per choose card move
	private static final int MAX_THINKING_TIME = 400;


	private final int max_schift_rating_val = 30;


	// TODO Wo sollten die Exceptions gecatcht werden???
	// TODO hilfsmethoden bockVonJederFarbe, TruempfeNochImSpiel, statistisches Modell von möglichen Karten von jedem Spieler
	// TODO alle gespielten Karten merken

	// wähle trumpf mit besten voraussetzungen -> ranking
	// bei drei sicheren stichen -> obeabe oder undeufe
	//
	// wenn nicht gut -> schieben
	@Override
	public Mode chooseTrumpf(Set<Card> availableCards, GameSession session, boolean isGschobe) {
		printCards(availableCards);

		Mode mode = JassHelper.getRandomMode(isGschobe);

		//mode = predictTrumpf(availableCards, mode);

		System.out.println("Chose Trumpf " + mode);

		return mode;
	}

	private Mode predictTrumpf(Set<Card> availableCards, Mode prospectiveMode) {
		int max = 0;
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
			prospectiveMode = Mode.shift();
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

	public int rateObeabeColor(Set<Card> cards, Color color) {
		Set<Card> cardsOfColor = JassHelper.getSortedCardsOfColor(cards, color);
		List<Card> sortedCards = cardsOfColor.stream().sorted(Comparator.comparing(Card::getRank)).collect(Collectors.toList());
		if (sortedCards.stream().findFirst().isPresent())
			if (sortedCards.stream().findFirst().get().getValue().getRank() >= 8)
				return 3000;
		return 5;
	}

	// Sorts the wrong way round
	private int rateUndeufeColor(Set<Card> cards, Color color) {
		Set<Card> cardsOfColor = JassHelper.getSortedCardsOfColor(cards, color);
		List<Card> sortedCards = cardsOfColor.stream().sorted(Comparator.comparing(Card::getRank)).collect(Collectors.toList());
		if (sortedCards.stream().findFirst().isPresent())
			if (sortedCards.stream().findFirst().get().getValue().getRank() <= 2)
				return 300;
		return 5;
	}


	@Override
	public Card chooseCard(Set<Card> availableCards, GameSession session) {
		final long startTime = System.nanoTime();
		final long endingTime = startTime + 1000000 * MAX_THINKING_TIME;
		printCards(availableCards);
		final Game game = session.getCurrentGame();
		final Set<Card> possibleCards = JassHelper.getPossibleCards(availableCards, game);

		if (possibleCards.isEmpty())
			System.err.println("We have a serious problem! No possible card to play!");

		if (possibleCards.size() == 1)
			for (Card card : possibleCards)
				return card;

		Card card = JassHelper.getRandomCard(possibleCards, game);

		try {
			final Card mctsCard = MCTSHelper.getCard(availableCards, game, endingTime);
			if (possibleCards.contains(card)) {
				System.out.println("Chose Card based on MCTS, Hurra!");
				card = mctsCard;
			} else
				System.out.println("Card chosen not in available cards. Had to choose random card, Damn it!");
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Something went wrong. Had to choose random card, Damn it!");
		}

		final long endTime = (System.nanoTime() - startTime) / 1000000;
		System.out.println("Total time for move: " + endTime + "ms");
		System.out.println("Played " + card + " out of possible Cards " + possibleCards + " out of available Cards " + availableCards+ "\n\n");
		assert card != null;
		assert possibleCards.contains(card);
		return card;
	}

	private void printCards(Set<Card> availableCards) {
		System.out.println("Hi there! I am JassTheRipper and these are my cards: " + availableCards);
	}
}
