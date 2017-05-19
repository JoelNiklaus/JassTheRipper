package com.zuehlke.jasschallenge.client.game.strategy.helpers;

import com.zuehlke.jasschallenge.client.game.Game;
import com.zuehlke.jasschallenge.client.game.Player;
import com.zuehlke.jasschallenge.client.game.Round;
import com.zuehlke.jasschallenge.game.cards.Card;
import com.zuehlke.jasschallenge.game.cards.CardValue;
import com.zuehlke.jasschallenge.game.cards.Color;
import com.zuehlke.jasschallenge.game.mode.Mode;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by joelniklaus on 05.05.17.
 */
public class JassHelper {

	/**
	 * Choose a random trump mode
	 *
	 * @param isGschobe
	 * @return
	 */
	public static Mode getRandomMode(boolean isGschobe) {
		final List<Mode> allPossibleModes = Mode.standardModes();
		if (!isGschobe) {
			allPossibleModes.add(Mode.shift());
		}
		return allPossibleModes.get(new Random().nextInt(allPossibleModes.size()));
	}

	/**
	 * Get a random card out of my available cards
	 *
	 * @param availableCards
	 * @param game
	 * @return
	 */
	public static Card getRandomCard(Set<Card> availableCards, Game game) {
		return getPossibleCards(availableCards, game).parallelStream()
				.findAny()
				.orElseThrow(() -> new RuntimeException("There should always be a card to play"));
	}

	/**
	 * Get the set of cards which are possible to play at this moment according to the game rules
	 *
	 * @param availableCards
	 * @param game
	 * @return
	 */
	public static Set<Card> getPossibleCards(Set<Card> availableCards, Game game) {
		assert (availableCards.size() > 0);
		Round round = game.getCurrentRound();
		Mode mode = round.getMode();
		// If you have a card
		Set<Card> validCards = availableCards.parallelStream().
				filter(card -> mode.canPlayCard(card, round.getPlayedCards(), round.getRoundColor(), availableCards)).
				collect(Collectors.toSet());
		if (validCards.size() > 0)
			return validCards;
		else
			return availableCards;
	}

	/**
	 * Gets the card which the partner played in this round
	 *
	 * @param round
	 * @return
	 */
	public static Card getCardOfPartner(Round round) {
		if (lastPlayer(round))
			return round.getMoves().get(1).getPlayedCard();
		if (thirdPlayer(round))
			return round.getMoves().get(0).getPlayedCard();
		return null;
	}

	/**
	 * Counts the number of cards of a certain color
	 *
	 * @param cards
	 * @param color
	 * @return
	 */
	public static int countNumberOfCardsOfColor(Set<Card> cards, Color color) {
		return getCardsOfColor(cards, color).size();
	}

	/**
	 * Checks if it is possible to play the same color
	 *
	 * @param cards
	 * @param startingCard
	 * @return
	 */
	public static boolean isAngebenPossible(Set<Card> cards, Card startingCard) {
		return countNumberOfCardsOfColor(cards, startingCard.getColor()) > 0;
	}

	/**
	 * Gets all the cards of a certain color
	 *
	 * @param cards
	 * @param color
	 * @return
	 */
	public static Set<Card> getCardsOfColor(Set<Card> cards, Color color) {
		return cards.parallelStream().
				filter(card -> card.getColor().equals(color)).
				collect(Collectors.toSet());
	}

	/**
	 * Checks if the current player is the first player in the round
	 *
	 * @param round
	 * @return
	 */
	public static boolean startingPlayer(Round round) {
		return round.numberOfPlayedCards() == 0;
	}

	/**
	 * Checks if the current player is the second player in the round
	 *
	 * @param round
	 * @return
	 */
	public static boolean secondPlayer(Round round) {
		return round.numberOfPlayedCards() == 1;
	}

	/**
	 * Checks if the current player is the third player in the round
	 *
	 * @param round
	 * @return
	 */
	public static boolean thirdPlayer(Round round) {
		return round.numberOfPlayedCards() == 2;
	}

	/**
	 * Checks if the current player is the last player in the round
	 *
	 * @param round
	 * @return
	 */
	public static boolean lastPlayer(Round round) {
		return round.numberOfPlayedCards() == 3;
	}

	/**
	 * Checks if the partner has already played a card in the round
	 *
	 * @param round
	 * @return
	 */
	public static boolean hasPartnerAlreadyPlayed(Round round) {
		return round.numberOfPlayedCards() >= 2;
	}

	/**
	 * Checks if two players are in the same team
	 *
	 * @param player
	 * @param otherPlayer
	 * @return
	 */
	public static boolean isTeamMember(Player player, Player otherPlayer) {
		return !isOpponent(otherPlayer, player);
	}

	/**
	 * Checks if two players are in opponent teams
	 *
	 * @param otherPlayer
	 * @param player
	 * @return
	 */
	public static boolean isOpponent(Player otherPlayer, Player player) {
		return (otherPlayer.getSeatId() * player.getSeatId()) % 2 != player.getSeatId() % 2;
	}

	/**
	 * Gets all the trumps out of the cardset
	 *
	 * @param cards
	 * @param mode
	 * @return
	 */
	public static Set<Card> getTrumps(Set<Card> cards, Mode mode) {
		return cards.parallelStream()
				.filter(card -> card.getColor().equals(mode.getTrumpfColor()))
				.collect(Collectors.toSet());
	}

	/**
	 * Gets all the cards which are not trumps out of the card set
	 *
	 * @param cards
	 * @param mode
	 * @return
	 */
	public static Set<Card> getNotTrumps(Set<Card> cards, Mode mode) {
		return cards.parallelStream()
				.filter(card -> !card.getColor().equals(mode.getTrumpfColor()))
				.collect(Collectors.toSet());
	}

	// TODO could be made more sophisticated

	/**
	 * Gets all the cards which can be used to schmieren out of the possible cards
	 *
	 * @param possibleCards
	 * @param cardOfPartner
	 * @param mode
	 * @return
	 */
	public static Set<Card> getSchmierCards(Set<Card> possibleCards, Card cardOfPartner, Mode mode) {
		Set<Card> cardsOfColour = getCardsOfColor(possibleCards, cardOfPartner.getColor());
		assert !cardsOfColour.isEmpty();

		List<CardValue> possibleCardValues = new LinkedList<>();
		possibleCardValues.add(CardValue.TEN);
		if (mode.equals(Mode.topDown()))
			possibleCardValues.add(CardValue.EIGHT);

		Set<Card> schmierCards = EnumSet.noneOf(Card.class);
		for (Card card : cardsOfColour)
			if (possibleCardValues.contains(card.getValue()))
				schmierCards.add(card);

		if (!schmierCards.isEmpty())
			return schmierCards;
		return cardsOfColour;
	}
}
