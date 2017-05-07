package com.zuehlke.jasschallenge.client.game.strategy.helpers;

import com.zuehlke.jasschallenge.client.game.GameSession;
import com.zuehlke.jasschallenge.client.game.Player;
import com.zuehlke.jasschallenge.client.game.Round;
import com.zuehlke.jasschallenge.client.game.strategy.exceptions.InvalidTrumpfException;
import com.zuehlke.jasschallenge.game.Trumpf;
import com.zuehlke.jasschallenge.game.cards.Card;
import com.zuehlke.jasschallenge.game.cards.Color;
import com.zuehlke.jasschallenge.game.mode.Mode;
import weka.classifiers.Classifier;
import weka.core.DenseInstance;
import weka.core.Instances;
import weka.core.converters.ConverterUtils;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by joelniklaus on 05.05.17.
 */
public class JassHelper {
	public static Card getCardOfPartner(Round round) {
		if(lastPlayer(round))
			return round.getMoves().get(1).getPlayedCard();
		if(thirdPlayer(round))
			return round.getMoves().get(0).getPlayedCard();
		return null;
	}

	public static int countNumberOfCardsOfColor(Set<Card> cards, Color color) {
		return (int) getSortedCardsOfColor(cards, color).size();
	}

	public static Set<Card> getSortedCardsOfColor(Set<Card> cards, Color color) {
		return cards.stream().filter(card -> card.getColor().equals(color)).sorted().collect(Collectors.toSet());
	}

	public static boolean startingPlayer(Round round) {
		return round.numberOfPlayedCards() == 0;
	}

	public static boolean secondPlayer(Round round) {
		return round.numberOfPlayedCards() == 1;
	}

	public static boolean thirdPlayer(Round round) {
		return round.numberOfPlayedCards() == 2;
	}


	public static boolean lastPlayer(Round round) {
		return round.numberOfPlayedCards() == 3;
	}

	public static Set<Card> getPossibleCards(Set<Card> availableCards, Round round, Mode gameMode) {
		return availableCards.stream().filter(card -> gameMode.canPlayCard(card, round.getPlayedCards(), round.getRoundColor(), availableCards)).collect(Collectors.toSet());
	}

}
