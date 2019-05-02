package com.zuehlke.jasschallenge.client.game.strategy.helpers;

import com.zuehlke.jasschallenge.game.Trumpf;
import com.zuehlke.jasschallenge.game.cards.Card;
import com.zuehlke.jasschallenge.game.cards.Color;
import com.zuehlke.jasschallenge.game.mode.Mode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class TrumpfSelectionHelper {

	public static final Logger logger = LoggerFactory.getLogger(TrumpfSelectionHelper.class);


	// TODO: Maybe this is too high or too low? => Write tests.
	// INFO: If the rating of the highest trumpf is lower than this constant, the rule-based algorithm will decide to shift
	// --> The higher this value, the more likely shifting is.
	public static final int MAX_SHIFT_RATING_VAL = 100;

	private TrumpfSelectionHelper() {

	}

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
	 * Calculates values for each possible trumpf and chooses the one with the highest value.
	 * If no value exceeds the threshold MAX_SHIFT_RATING_VAL, the bot shifts
	 *
	 * @param availableCards
	 * @param isGschobe
	 * @return
	 */
	public static Mode predictTrumpf(Set<Card> availableCards, boolean isGschobe) {
		Mode prospectiveMode = TrumpfSelectionHelper.getRandomMode(isGschobe);
		int max = 0;
		for (Color color : Color.values()) {
			int colorTrumpRating = rateColorForTrumpf(availableCards, color);
			if (colorTrumpRating > max) {
				max = colorTrumpRating;
				prospectiveMode = Mode.from(Trumpf.TRUMPF, color);
			}
		}
		// rateObeabe and rateUndeUfe are 180 at max; 180 = can make all Stich
		double noTrumpfWeight = 0.8; // INFO: favor trumpf to topdown and bottomup because bot is better in cardplay relative to humans there
		if (isGschobe)
			noTrumpfWeight -= 0.1; // INFO: make obeae and undeufe just a little bit more unlikely
		if (noTrumpfWeight * rateObeabe(availableCards) > max) {
			prospectiveMode = Mode.topDown();
			max = rateObeabe(availableCards);
		}
		if (noTrumpfWeight * rateUndeufe(availableCards) > max) {
			prospectiveMode = Mode.bottomUp();
			max = rateUndeufe(availableCards);
		}
		logger.info("ChooseTrumpf succeeded!");
		if (max < MAX_SHIFT_RATING_VAL && !isGschobe)
			return Mode.shift();
		return prospectiveMode;
	}

	/**
	 * Calculates a value for choosing obeabe as trumpf with the given cards.
	 *
	 * @param cards
	 * @return
	 */
	public static int rateObeabe(Set<Card> cards) {
		int sum = 0;
		for (Color color : Color.values()) {
			sum += rateObeabeColor(cards, color);
		}
		return sum;
	}

	/**
	 * Calculates a value for choosing undeufe as trumpf with the given cards.
	 *
	 * @param cards
	 * @return
	 */
	public static int rateUndeufe(Set<Card> cards) {
		int sum = 0;
		for (Color color : Color.values()) {
			sum += rateUndeufeColor(cards, color);
		}
		return sum;
	}


	/**
	 * Calculates a trumpf value for a given trumpf color
	 *
	 * @param cards
	 * @param color
	 * @return
	 */
	public static int rateColorForTrumpf(Set<Card> cards, Color color) {
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
		rating *= Math.pow(1.15, cardsOfColor.size());

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

	public static int rateObeabeColor(Set<Card> cards, Color color) {
		// Get the cards in descending order
		List<Card> sortedCardOfColor = JassHelper.sortCardsOfColorDescending(cards, color);
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
		while (!sortedCardOfColor.isEmpty()) {
			// The next card to be tested
			Card nextCard = sortedCardOfColor.get(0);
			// Estimate how safe you Stich with that card
			int numberOfCardsInbetween = lastCard.getRank() - nextCard.getRank() - 1;
			higherCards += numberOfCardsInbetween;
			safety *= safetyOfStich(numberOfMyCards, higherCards, nextCard, lastCard, numberOfCardsInbetween);
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

	public static int rateUndeufeColor(Set<Card> cards, Color color) {
		// Get the cards in ascending order
		List<Card> sortedCardOfColor = JassHelper.sortCardsOfColorAscending(cards, color);
		if (sortedCardOfColor.isEmpty())
			return 0;
		// Number of cards I have that are lower than the nextCard
		int lowerCards = 0;
		// Number of the cards I have of this color
		int numberOfMyCards = sortedCardOfColor.size();
		// Estimate how safe you make a Stich with your highest card
		float safety = calculateInitialSafetyUndeUfe(sortedCardOfColor);
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
			int numberOfCardsInbetween = nextCard.getRank() - lastCard.getRank() - 1;
			safety *= safetyOfStich(numberOfMyCards, lowerCards, nextCard, lastCard, numberOfCardsInbetween);
			// How safe is the Stich? * Stichvalue
			rating += safety * 20;
			sortedCardOfColor.remove(0);
			// One card is higher than the last
			lowerCards++;
			lastCard = nextCard;
		}
		return (int) Math.ceil(rating);
	}


	private static boolean containsTwoOrMoreAces(Set<Card> cardStream) {
		List<Card> cardsSorted = cardStream.stream().sorted(Comparator.comparing(Card::getRank).reversed()).collect(Collectors.toList());
		int countAces = 0;
		while (cardsSorted.get(0).getRank() == 9) {
			countAces++;
			cardsSorted.remove(0);
		}
		return countAces >= 2;
	}

	private static float safetyOfStich(int numberOfCards, int higherCards, Card nextCard, Card lastCard, int numberOfCardsInbetween) {
		// Have the next-higher card => probability to stich is the same as with the next higher card
		if (numberOfCardsInbetween == 0)
			return 1;
			// Probability to stich is 1 - the probability, that enemy has a higher card + enough cards to discard
		else
			return 1 - ((float) 2 / 3 * enemenyHasNoMoreCards(numberOfCards, higherCards, numberOfCardsInbetween));
	}

	private static float enemenyHasNoMoreCards(int numberOfMyCards, int higherCards, int numberOfCardsBetween) {
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

	private static float factorial(int n) {
		if (n == 1 || n == 0)
			return 1;
		else if (n < 0)
			return 0;
		else
			return n * factorial(n - 1);
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
}
