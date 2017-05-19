package com.zuehlke.jasschallenge.client.game.strategy.helpers;

import com.zuehlke.jasschallenge.client.game.Game;
import com.zuehlke.jasschallenge.client.game.Player;
import com.zuehlke.jasschallenge.client.game.Round;
import com.zuehlke.jasschallenge.client.game.strategy.JassTheRipperJassStrategy;
import com.zuehlke.jasschallenge.game.Trumpf;
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




	public static Mode predictTrumpf(Set<Card> availableCards, Mode prospectiveMode, boolean isGschobe) throws Exception {
		int max = 0;
		for (Color color : Color.values()) {
			int colorTrumpRating = rateColorForTrumpf(availableCards, color);
			if (colorTrumpRating > max) {
				max = colorTrumpRating;
				prospectiveMode = Mode.from(Trumpf.TRUMPF, color);
			}
		}
		// rateObeabe and rateUndeUfe are 180 at max; 180 = can make all Stich
		// Rate ObeAbe and UndeUfe much much lower (with 1/3) when it is gschobe
		// TODO: This is an ugly hotfix, make it nicer ;)
		float gschobeFactor = 1;
		if (isGschobe)
			gschobeFactor = 1 / 3;
		if (gschobeFactor * rateObeabe(availableCards) > max) {
			prospectiveMode = Mode.topDown();
			max = rateObeabe(availableCards);
		}
		if (gschobeFactor * rateUndeufe(availableCards) > max) {
			prospectiveMode = Mode.bottomUp();
			max = rateUndeufe(availableCards);
		}
		System.out.println("ChooseTrumpf succeeded!");
		if (max < JassTheRipperJassStrategy.MAX_SCHIFT_RATING_VAL && !isGschobe)
			return Mode.shift();
		return prospectiveMode;
	}

	public static int rateColorForTrumpf(Set<Card> cards, Color color) throws Exception {
		Set<Card> cardsOfColor = JassHelper.getCardsOfColor(cards, color);
		if (cardsOfColor.size() <= 1)
			return 0;
		List<Card> cardsOfColorL = new ArrayList<>(cardsOfColor);
		boolean[] prospectiveTrumpfCards = new boolean[9];
		for (int i = 0; i < 9; i++) {
			if (cardsOfColorL.isEmpty())
				break;
			if (cardsOfColorL.get(0).getRank() == i + 1) {
				prospectiveTrumpfCards[i] = true;
				cardsOfColorL.remove(0);
			}
		}
		int rating = 0;
		// If you have 6 or more Cards of this color, rate it insanely high!
		if (cardsOfColor.size() >= 6)
			rating += 120;
		float qualityOfTrumpfCards = 0;
		// rate plus 2 * Trumpfrank (9 for Jack, 8 for Nell, 7 for Ace, …)
		// Maximum is 90 this way
		for (Card card : cardsOfColor) {
			qualityOfTrumpfCards += 2 * card.getTrumpfRank();
		}
		rating += (int) qualityOfTrumpfCards;
		// If you have Jack, rate higher
		if (prospectiveTrumpfCards[5])
			rating += 10;
		// If you have Nell, rate higher
		if (prospectiveTrumpfCards[3])
			rating += 7;
		// If you have Ace, rate slightly higher
		if (prospectiveTrumpfCards[8])
			rating += 3;

		// If a lot of a color, it is rather good Trumpf
		for (Card card : cardsOfColor) {
			// TODO: maybe do something with * instead of + here?
			rating *= 1.15;
		}
		// If Jack and Nell and another Trumpf and 2 Aces: good Trumpf
		if ((prospectiveTrumpfCards[5] && prospectiveTrumpfCards[3]) && cardsOfColor.size() > 2 && containsTwoOrMoreAces(cards))
			if (cardsOfColor.size() > 3)
				rating += 40;
			else
				rating += 30;
		// If you have Jack + 3 cards, one of which is good => would be good Trumpf!
		if (prospectiveTrumpfCards[5] && cardsOfColor.size() > 3 && qualityOfTrumpfCards - 9 > 12)
			if (cardsOfColor.size() > 4)
				rating += 40;
			else
				rating += 30;
		// If Nell + 4 --> is good Trumpf; + 3 solid Trumpf
		if ((prospectiveTrumpfCards[3] && cardsOfColor.size() > 3))
			if (cardsOfColor.size() > 4)
				rating += 30;
			else
				rating += 20;
		// If Nell, Ass + 3 --> is good Trumpf
		if ((prospectiveTrumpfCards[5] && prospectiveTrumpfCards[8] && cardsOfColor.size() > 4))
			if (cardsOfColor.size() > 5)
				rating += 40;
			else
				rating += 30;

		// Try to consider the value of the other cards; is kind of similar to ObeAbe, but a lot less valuable…
		for (Color c : Color.values()) {
			// Does it make sense to exclude the own color?
			//if (c != color)
			rating += (rateObeabeColor(cards, c) / 4);
		}
		return rating;
	}

	private static boolean containsTwoOrMoreAces(Set<Card> cardStream) throws Exception {
		List<Card> cardsSorted = cardStream.stream().sorted(Comparator.comparing(Card::getRank).reversed()).collect(Collectors.toList());
		int countAces = 0;
		while (cardsSorted.get(0).getRank() == 9) {
			countAces++;
			cardsSorted.remove(0);
		}
		return countAces >= 2;
	}

	public static int rateObeabe(Set<Card> cards) throws Exception {
		int sum = 0;
		for (Color color : Color.values()) {
			sum += rateObeabeColor(cards, color);
		}
		return sum;
	}

	public static int rateUndeufe(Set<Card> cards) throws Exception {
		int sum = 0;
		for (Color color : Color.values()) {
			sum += rateUndeufeColor(cards, color);
		}
		return sum;
	}

	public static int rateObeabeColor(Set<Card> cards, Color color) throws Exception {
		// Get the cards in descending order
		List<Card> sortedCardOfColor = sortCardsOfColorDescending(cards, color);
		if (sortedCardOfColor.isEmpty())
			return 0;
		// Number of cards I have that are higher than the nextCard
		int higherCards = 0;
		// Number of the cards I have of this color
		int numberOfMyCards = sortedCardOfColor.size();
		// Estimate how safe you make a Stich with your highest card
		float safety = calculateInitialSafetyObeabe(sortedCardOfColor);
		// The last card rated
		Card lastCard = sortedCardOfColor.get(0);
		// 1 Stich entspricht 20 ratingPoints
		float rating = safety * 20;
		// remove the last card tested
		sortedCardOfColor.remove(lastCard);
		while (sortedCardOfColor.size() > 0) {
			// The next card to be tested
			Card nextCard = sortedCardOfColor.get(0);
			// Estimate how safe you Stich with that card
			int numberOfCardsInbetween = lastCard.getRank() - nextCard.getRank() - 1;
			higherCards += numberOfCardsInbetween;
			safety *= safetyOfStich(numberOfMyCards, higherCards, nextCard, lastCard);
			// How safe is the Stich? * Stichvalue
			rating += safety * 20;
			sortedCardOfColor.remove(0);
			// One card is higher than the last
			higherCards++;
			lastCard = nextCard;
		}
		// TODO: Take 'Opferkarten' into account; King with 7 should be much more valuable than only King!
		return (int) Math.ceil(rating);
	}

	public static List<Card> sortCardsOfColorDescending(Set<Card> cards, Color color) {
		Set<Card> cardsOfColor = JassHelper.getCardsOfColor(cards, color);
		return cardsOfColor.stream().sorted(Comparator.comparing(Card::getRank).reversed()).collect(Collectors.toList());
	}

	public static List<Card> sortCardsOfColorAscending(Set<Card> cards, Color color) {
		Set<Card> cardsOfColor = JassHelper.getCardsOfColor(cards, color);
		return cardsOfColor.stream().sorted(Comparator.comparing(Card::getRank)).collect(Collectors.toList());
	}

	public static float calculateInitialSafetyObeabe(List<Card> sortedCards) {
		Card nextCard = sortedCards.get(0);
		int rank = nextCard.getRank();
		float safety = 1;
		// Probability that my partner has all the higher cards
		for (int i = 0; i < 9 - rank; i++)
			safety /= 3;
		return safety;
	}

	public static float calculateInitialSafetyUndeUfe(List<Card> sortedCards) {
		Card nextCard = sortedCards.get(0);
		int rank = nextCard.getRank();
		float safety = 1;
		// Probability that my partner has all the lower cards
		for (int i = 0; i < rank - 1; i++)
			safety /= 3;
		return safety;
	}

	private static float safetyOfStich(int numberOfCards, int higherCards, Card nextCard, Card lastCard) throws Exception {
		int numberOfCardsBetween = lastCard.getRank() - nextCard.getRank() - 1;
		// Have the next-higher card => probability to stich is the same as with the next higher card
		if (numberOfCardsBetween == 0)
			return 1;
			// Probability to stich is 1 - the probability, that enemy has a higher card + enough cards to discard
		else
			return 1 - ((float) 2 / 3 * enemenyHasNoMoreCards(numberOfCards, higherCards, numberOfCardsBetween));
	}

	private static float enemenyHasNoMoreCards(int numberOfMyCards, int higherCards, int numberOfCardsBetween) throws Exception {
		float estimate = 1;
		int otherColorCards = 9 - numberOfMyCards - 1;
		int otherCards = 27 - 1;
		// Only rough estimate of the probability, that a player of the other team has enough cards to discard (i.e. I
		// have an Ace and King, but he has 6 and 7 so can discard those invaluable cards
		estimate *= (float) (otherColorCards) / otherCards;
		estimate *= factorial(otherColorCards - 1);
		for (int i = 0; i < higherCards; i++) {
			otherColorCards--;
			estimate *= (float) (otherColorCards) / otherCards;
		}
		// Estimate of a mathematician: if the number of ranks between the last one and this one is increased by one,
		// the probability of me making the Stich is roughly halved.
		for (int i = 0; i < numberOfCardsBetween; i++)
			estimate *= 0.45;
		if (estimate > 1)
			estimate = 0.8f;
		if (estimate > 0)
			return estimate;
		else
			return 0;
	}

	public static int rateUndeufeColor(Set<Card> cards, Color color) throws Exception {
		// Get the cards in ascending order
		List<Card> sortedCards = sortCardsOfColorAscending(cards, color);
		if (sortedCards.size() == 0)
			return 0;
		// Number of cards I have that are lower than the nextCard
		int lowerCards = 0;
		// Number of the cards I have of this color
		int numberOfMyCards = sortedCards.size();
		// Estimate how safe you make a Stich with your highest card
		float safety = calculateInitialSafetyUndeUfe(sortedCards);
		// The last card rated
		Card lastCard = sortedCards.get(0);
		// 1 Stich entspricht 20 ratingPoints
		float rating = safety * 20;
		// remove the last card tested
		sortedCards.remove(lastCard);
		while (sortedCards.size() > 0) {
			// The next card to be tested
			Card nextCard = sortedCards.get(0);
			// Estimate how safe you Stich with that card
			safety *= safetyOfStich(numberOfMyCards, lowerCards, nextCard, lastCard);
			// How safe is the Stich? * Stichvalue
			rating += safety * 20;
			sortedCards.remove(0);
			// One card is higher than the last
			lowerCards++;
			lastCard = nextCard;
		}
		return (int) Math.ceil(rating);
	}

	private static float factorial(int n) {
		if (n == 1 || n == 0)
			return 1;
		else if (n < 0)
			return 0;
		else
			return n * factorial(n - 1);
	}
}
