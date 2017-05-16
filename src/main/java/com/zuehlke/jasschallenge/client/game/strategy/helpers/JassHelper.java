package com.zuehlke.jasschallenge.client.game.strategy.helpers;

import com.zuehlke.jasschallenge.client.game.Game;
import com.zuehlke.jasschallenge.client.game.Player;
import com.zuehlke.jasschallenge.client.game.Round;
import com.zuehlke.jasschallenge.game.cards.Card;
import com.zuehlke.jasschallenge.game.cards.Color;
import com.zuehlke.jasschallenge.game.mode.Mode;

import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by joelniklaus on 05.05.17.
 */
public class JassHelper {

	public static Mode getRandomMode(boolean isGschobe) {
		final List<Mode> allPossibleModes = Mode.standardModes();
		if (!isGschobe) {
			allPossibleModes.add(Mode.shift());
		}
		return allPossibleModes.get(new Random().nextInt(allPossibleModes.size()));
	}

	public static Card getRandomCard(Set<Card> availableCards, Game game) {
		return getPossibleCards(availableCards, game).stream()
				.findAny()
				.orElseThrow(() -> new RuntimeException("There should always be a card to play"));
	}

	public static Set<Card> getPossibleCards(Set<Card> availableCards, Game game) {
		assert(availableCards.size() > 0);
		Round round = game.getCurrentRound();
		Mode mode = round.getMode();
		// If you have a card
		Set<Card> validCards = availableCards.stream().
				filter(card -> mode.canPlayCard(card, round.getPlayedCards(), round.getRoundColor(), availableCards)).
				collect(Collectors.toSet());
		if (validCards.size() > 0)
			return validCards;
		else
			return availableCards;
	}


	public static Card getCardOfPartner(Round round) {
		if (lastPlayer(round))
			return round.getMoves().get(1).getPlayedCard();
		if (thirdPlayer(round))
			return round.getMoves().get(0).getPlayedCard();
		return null;
	}

	public static int countNumberOfCardsOfColor(Set<Card> cards, Color color) {
		return getSortedCardsOfColor(cards, color).size();
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


	public static boolean isTeamMember(Player otherPlayer, Player player) {
		return !isOpponent(otherPlayer, player);
	}

	public static boolean isOpponent(Player otherPlayer, Player player) {
		return (otherPlayer.getSeatId() * player.getSeatId()) % 2 != player.getSeatId() % 2;
	}
}
