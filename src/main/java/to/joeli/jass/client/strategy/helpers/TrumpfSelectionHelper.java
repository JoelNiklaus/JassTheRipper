package to.joeli.jass.client.strategy.helpers;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import to.joeli.jass.game.Trumpf;
import to.joeli.jass.game.cards.Card;
import to.joeli.jass.game.cards.Color;
import to.joeli.jass.game.mode.Mode;

import java.util.*;
import java.util.stream.Collectors;

public class TrumpfSelectionHelper {

	public static final boolean ALL_TRUMPFS = true;

	public static final Logger logger = LoggerFactory.getLogger(TrumpfSelectionHelper.class);

	// INFO: If the rating of the highest trumpf is lower than this constant, the rule-based algorithm will decide to shift
	// --> The higher this value, the more likely shifting is. 150 --> too high, 100 --> too low
	public static final int MAX_SHIFT_RATING_VAL = 125;
	public static final int TOP_TRUMPF_THRESHOLD = MAX_SHIFT_RATING_VAL; // NOTE: Set to a lower value when confidence in MCTS Trumpf Selection increases
	public static final int TOP_NUM_TRUMPFS = 3; // NOTE: Set to a higher value when confidence in MCTS Trumpf Selection increases

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
		final LinkedHashMap<Mode, Integer> trumpfRatings = rateModes(availableCards, isGschobe);
		logger.info("Sorted Trumpf Ratings: {}", trumpfRatings);
		return trumpfRatings.entrySet().iterator().next().getKey();
	}

	/**
	 * Can be used for pruning.
	 *
	 * @param availableCards
	 * @param isGschobe
	 * @return
	 */
	public static List<Mode> getTopTrumpfChoices(Set<Card> availableCards, boolean isGschobe) {
		final LinkedHashMap<Mode, Integer> trumpfRatings = rateModes(availableCards, isGschobe);
		//return getTrumpfsAboveThreshold(trumpfRatings);
		return getTopNumTrumpfs(trumpfRatings);
	}

	/**
	 * Returns the TOP_NUM_TRUMPFS rated trumpfs
	 *
	 * @param trumpfRatings
	 * @return
	 */
	@NotNull
	private static List<Mode> getTopNumTrumpfs(LinkedHashMap<Mode, Integer> trumpfRatings) {
		return trumpfRatings.entrySet().stream()
				.limit(TOP_NUM_TRUMPFS)
				.map(Map.Entry::getKey)
				.collect(Collectors.toList());
	}

	/**
	 * Returns the trumpf choices whose ratings exceed a certain threshold (hyperparameter)
	 *
	 * @param trumpfRatings
	 * @return
	 */
	@NotNull
	private static List<Mode> getTrumpfsAboveThreshold(LinkedHashMap<Mode, Integer> trumpfRatings) {
		return trumpfRatings.entrySet().stream()
				.filter(e -> e.getValue() >= TOP_TRUMPF_THRESHOLD)
				.map(Map.Entry::getKey)
				.collect(Collectors.toList());
	}

	/**
	 * Calculates the values for each mode and sorts them in descending order:
	 * The first entry contains the highest rating and its corresponding mode.
	 * The last entry contains the lowest rating and its corresponding mode.
	 *
	 * @param availableCards
	 * @param isGschobe
	 * @return
	 */
	private static LinkedHashMap<Mode, Integer> rateModes(Set<Card> availableCards, boolean isGschobe) {
		HashMap<Mode, Integer> trumpfRatings = new HashMap<>();

		for (Color color : Color.values())
			trumpfRatings.put(Mode.from(Trumpf.TRUMPF, color), rateColorForTrumpf(availableCards, color));

		//  IMPORTANT: This filters out obeabe and undeufe for easier training of the neural network
		if (ALL_TRUMPFS) {
			// rateObeabe and rateUndeUfe are 180 at max; 180 = can make all Stich
			float noTrumpfWeight = 0.9f; // INFO: slightly favor trumpf to topdown and bottomup because bot is probably better in cardplay relative to humans there
			if (isGschobe)
				noTrumpfWeight -= 0.15f; // INFO: make obeabe and undeufe just a little bit more unlikely because when shifted there is more risk involved
			trumpfRatings.put(Mode.topDown(), Math.round(noTrumpfWeight * rateObeabe(availableCards)));
			trumpfRatings.put(Mode.bottomUp(), Math.round(noTrumpfWeight * rateUndeufe(availableCards)));
		}

		if (!isGschobe)
			trumpfRatings.put(Mode.shift(), MAX_SHIFT_RATING_VAL);

		LinkedHashMap<Mode, Integer> sortedTrumpfRatings = new LinkedHashMap<>();
		trumpfRatings.entrySet().stream()
				.sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
				.forEachOrdered(x -> sortedTrumpfRatings.put(x.getKey(), x.getValue()));
		logger.debug("Rule-based TrumpfRatings: {}", sortedTrumpfRatings);
		return sortedTrumpfRatings;
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
		List<Card> cardsOfColorTemp = new ArrayList<>(cardsOfColor);
		boolean[] prospectiveTrumpfCards = new boolean[9];
		for (int i = 0; i < 9; i++) {
			if (cardsOfColorTemp.isEmpty())
				break;
			if (cardsOfColorTemp.get(0).getRank() == i + 1) {
				prospectiveTrumpfCards[i] = true;
				cardsOfColorTemp.remove(0);
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

		// Try to consider the value of the "Handkarten" (cards of the other colors); is kind of similar to ObeAbe, but a lot less valuable…
		for (Color c : Color.values()) {
			if (c != color)
				rating += (rateObeabeColor(cards, c) / 3);
		}
		return rating;
	}

	public static int rateObeabeColor(Set<Card> cards, Color color) {
		// Get the cards in descending order
		List<Card> sortedCardsOfColor = JassHelper.sortCardsOfColorDescending(cards, color);
		if (sortedCardsOfColor.isEmpty())
			return 0;
		// Number of cards I have that are higher than the nextCard
		int higherCards = 0;
		// Number of the cards I have of this color
		int numberOfMyCards = sortedCardsOfColor.size();
		// Estimate how safe you make a Stich with your highest card
		float safety = calculateInitialSafetyObeabe(sortedCardsOfColor);
		// The last card rated
		Card lastCard = sortedCardsOfColor.get(0);
		// 1 Stich entspricht 20 ratingPoints
		float rating = safety * 20;
		// remove the last card tested
		sortedCardsOfColor.remove(lastCard);
		while (!sortedCardsOfColor.isEmpty()) {
			// The next card to be tested
			Card nextCard = sortedCardsOfColor.get(0);
			// Estimate how safe you Stich with that card
			int numberOfCardsBetween = lastCard.getRank() - nextCard.getRank() - 1;
			higherCards += numberOfCardsBetween;
			safety *= safetyOfStich(numberOfMyCards, higherCards, nextCard, lastCard, numberOfCardsBetween);
			// How safe is the Stich? * Stichvalue
			rating += safety * 20;
			sortedCardsOfColor.remove(0);
			// One card is higher than the last
			higherCards++;
			lastCard = nextCard;
		}
		// TODO: Take 'Opferkarten' into account; King with 7 should be much more valuable than only King!
		return (int) Math.ceil(rating);
	}

	public static int rateUndeufeColor(Set<Card> cards, Color color) {
		// Get the cards in ascending order
		List<Card> sortedCardsOfColor = JassHelper.sortCardsOfColorAscending(cards, color);
		if (sortedCardsOfColor.isEmpty())
			return 0;
		// Number of cards I have that are lower than the nextCard
		int lowerCards = 0;
		// Number of the cards I have of this color
		int numberOfMyCards = sortedCardsOfColor.size();
		// Estimate how safe you make a Stich with your highest card
		float safety = calculateInitialSafetyUndeUfe(sortedCardsOfColor);
		// The last card rated
		Card lastCard = sortedCardsOfColor.get(0);
		// 1 Stich entspricht 20 ratingPoints
		float rating = safety * 20;
		// remove the last card tested
		sortedCardsOfColor.remove(lastCard);
		while (!sortedCardsOfColor.isEmpty()) {
			// The next card to be tested
			Card nextCard = sortedCardsOfColor.get(0);
			// Estimate how safe you Stich with that card
			int numberOfCardsBetween = nextCard.getRank() - lastCard.getRank() - 1;
			lowerCards += numberOfCardsBetween;
			safety *= safetyOfStich(numberOfMyCards, lowerCards, nextCard, lastCard, numberOfCardsBetween);
			// How safe is the Stich? * Stichvalue
			rating += safety * 20;
			sortedCardsOfColor.remove(0);
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

	private static float safetyOfStich(int numberOfCards, int higherCards, Card nextCard, Card lastCard, int numberOfCardsBetween) {
		// Have the next-higher card => probability to stich is the same as with the next higher card
		if (numberOfCardsBetween == 0)
			return 1;
			// Probability to stich is 1 - the probability, that enemy has a higher card + enough cards to discard
		else
			return 1 - ((float) 2 / 3 * enemenyHasNoMoreCards(numberOfCards, higherCards, numberOfCardsBetween));
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
