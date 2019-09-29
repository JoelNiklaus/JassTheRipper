package to.joeli.jass.client.strategy.helpers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import to.joeli.jass.client.game.Game;
import to.joeli.jass.client.game.Player;
import to.joeli.jass.client.game.Round;
import to.joeli.jass.game.cards.Card;
import to.joeli.jass.game.cards.CardValue;
import to.joeli.jass.game.cards.Color;
import to.joeli.jass.game.mode.Mode;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This class comprises a collection of helper methods for both trumpf and card selection
 * <p>
 * Created by joelniklaus on 05.05.17.
 */
public class JassHelper {

	private static final int BRETTLI_BOUNDARY = 5; // TEN
	private static final int EIGHT = 3; // EIGHT
	private static final int NELL = 4; // NELL

	public static final Logger logger = LoggerFactory.getLogger(JassHelper.class);

	private JassHelper() {

	}

	/**
	 * Calculates the current bocks (highest cards) of each suit (excluding trumpf).
	 * Slightly faster method if only bocks are calculated.
	 *
	 * @param game
	 * @return
	 */
	public static Set<Card> getBocks(Game game) {
		EnumSet<Card> bocks = EnumSet.noneOf(Card.class);
		for (Color color : Color.values()) {
			if (!isTrumpfModeAndMatchesColor(game.getMode(), color)) {
				// Start with the highest card first
				List<CardValue> values = Arrays.asList(CardValue.values());
				if (!isBottomUp(game.getMode()))
					Collections.reverse(values);
				// And then check for every card down the value line
				for (CardValue value : values) {
					Card card = Card.getCard(color, value);
					// if it is still in the game
					if (!game.getAlreadyPlayedCards().contains(card)) {
						bocks.add(card);
						break;
					}
				}
			}
		}
		return bocks;
	}

	/**
	 * Calculates the current bocks (highest cards) of each suit (excluding trumpf).
	 * Slightly faster method if also second highest cards are needed.
	 *
	 * @param mode
	 * @param orderedRemainingCards
	 * @return
	 */
	public static Set<Card> getBocks(Mode mode, Map<Color, List<Card>> orderedRemainingCards) {
		EnumSet<Card> bocks = EnumSet.noneOf(Card.class);
		for (Color color : Color.values())
			if (!isTrumpfModeAndMatchesColor(mode, color) && !orderedRemainingCards.get(color).isEmpty())
				bocks.add(orderedRemainingCards.get(color).get(0));
		return bocks;
	}

	/**
	 * Calculates the second highest cards of each suit (excluding trumpf).
	 *
	 * @param mode
	 * @param orderedRemainingCards
	 * @return
	 */
	public static Set<Card> getRemainingCardsBelowBocks(Mode mode, Map<Color, List<Card>> orderedRemainingCards) {
		EnumSet<Card> bocks = EnumSet.noneOf(Card.class);
		for (Color color : Color.values())
			if (!isTrumpfModeAndMatchesColor(mode, color) && orderedRemainingCards.get(color).size() > 1)
				bocks.add(orderedRemainingCards.get(color).get(1));
		return bocks;
	}

	/**
	 * Returns a map of the cards still in the game ordered by the strength.
	 * Example: In the beginning of a clubs game:
	 * HA, HK, etc.
	 * DA, DK, etc.
	 * SA, SK, etc.
	 * CJ, C9, CA, CK, etc.
	 * <p>
	 * In the beginning of a bottom up game:
	 * H6, H7, etc.
	 * D6, D7, etc.
	 * etc.
	 *
	 * @param game
	 * @return
	 */
	public static Map<Color, List<Card>> getCardsStillInGameInStrengthOrder(Game game) {
		EnumMap<Color, List<Card>> orderedRemainingCards = new EnumMap<>(Color.class);
		for (Color color : Color.values()) {
			// Start with the highest card first
			List<CardValue> values = Arrays.asList(CardValue.values());
			if (!isBottomUp(game.getMode()))
				Collections.reverse(values);
			if (isTrumpfModeAndMatchesColor(game.getMode(), color))
				values = values.stream().sorted(Comparator.comparing(CardValue::getTrumpfRank).reversed()).collect(Collectors.toList());

			List<Card> cards = new ArrayList<>();
			// And then check for every card down the value line
			for (CardValue value : values) {
				Card card = Card.getCard(color, value);
				// if it is still in the game
				if (!game.getAlreadyPlayedCards().contains(card))
					cards.add(card);
				orderedRemainingCards.put(color, cards);
			}
		}
		return orderedRemainingCards;
	}

	private static boolean isTrumpfModeAndMatchesColor(Mode mode, Color color) {
		return mode.isTrumpfMode() && mode.getTrumpfColor().equals(color);
	}


	/**
	 * Determines if the card is a Brettli (a low card which has zero points)
	 *
	 * @param card
	 * @param mode
	 * @return
	 */
	public static boolean isBrettli(Card card, Mode mode) {
		int rank = card.getRank();
		if (isBottomUp(mode)) {
			return rank > BRETTLI_BOUNDARY;
		}
		if (rank < BRETTLI_BOUNDARY) {
			if (isTopDown(mode)) {
				return rank != EIGHT;
			} else return !hasTrumpfColor(card, mode);
		}
		return false;
	}

	public static boolean isNell(Card card, Mode mode) {
		int rank = card.getRank();
		return hasTrumpfColor(card, mode) && rank == NELL;
	}

	private static boolean hasTrumpfColor(Card card, Mode mode) {
		return card.getColor().equals(mode.getTrumpfColor());
	}

	private static boolean isBottomUp(Mode mode) {
		return mode.equals(Mode.bottomUp());
	}

	private static boolean isTopDown(Mode mode) {
		return mode.equals(Mode.topDown());
	}

	private static Set<Card> getBrettli(Set<Card> possibleCards, Mode mode, Color color) {
		Set<Card> cards = getCardsOfColor(possibleCards, color);
		if (isBottomUp(mode))
			cards = cards.stream().filter(card -> card.getRank() > BRETTLI_BOUNDARY).collect(Collectors.toSet());
		else {
			cards = cards.stream().filter(card -> card.getRank() < BRETTLI_BOUNDARY).collect(Collectors.toSet());
			if (isTopDown(mode))
				cards = cards.stream().filter(card -> card.getRank() != EIGHT).collect(Collectors.toSet());
		}
		return cards;
	}

	public static boolean isTrumpf(Mode mode) {
		return !isNotTrumpf(mode);
	}

	public static boolean isNotTrumpf(Mode mode) {
		return isBottomUp(mode) || isTopDown(mode);
	}


	/**
	 * Rates the given color for the Mode ObeAbe respecting the cards that have already been played (e.g. rating a
	 * King higher if the Ace has been played).
	 *
	 * @param ownCards           - cards of the player
	 * @param alreadyPlayedCards - cards already played in the round
	 * @param color              - the color being rated
	 * @return the rating
	 */
	public static int rateColorObeAbeRespectingAlreadyPlayedCards(Set<Card> ownCards, Set<Card> alreadyPlayedCards, Color color) {
		List<Card> playedCardsOfColor = sortCardsOfColorDescending(alreadyPlayedCards, color);
		// Get the cards in descending order
		List<Card> sortedCardsOfColor = sortCardsOfColorDescending(ownCards, color);
		if (sortedCardsOfColor.isEmpty())
			return 0;
		// Number of cards I have that are higher than the nextCard
		int higherCards = 0;
		// Number of the cards I have of this color
		int numberOfMyCards = sortedCardsOfColor.size();
		// Estimate how safe you make a Stich with your highest card
		float safety = calculateInitialSafetyObeabeRespectingPlayedCards(sortedCardsOfColor, playedCardsOfColor);
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
			int numberOfCardsInbetween = calculateNumberOfCardsInbetweenObeAbeRespectingPlayedCards(lastCard, nextCard, playedCardsOfColor);
			higherCards += numberOfCardsInbetween;
			safety *= safetyOfStichVerwerfen(numberOfMyCards, higherCards, nextCard, lastCard, numberOfCardsInbetween);
			// How safe is the Stich? * Stichvalue
			rating += safety * 20;
			sortedCardsOfColor.remove(0);
			// One card is higher than the last
			higherCards++;
			lastCard = nextCard;
		}
		return (int) Math.ceil(rating);
	}

	/**
	 * Rates the given color for the Mode UndeUfe respecting the cards that have already been played (e.g. rating a
	 * Seven higher if the Six has been played).
	 *
	 * @param ownCards           - cards of the player
	 * @param alreadyPlayedCards - cards already played in the round
	 * @param color              - the color being rated
	 * @return the rating
	 */
	public static int rateColorUndeUfeRespectingAlreadyPlayedCards(Set<Card> ownCards, Set<Card> alreadyPlayedCards, Color color) {
		List<Card> playedCardsOfColor = sortCardsOfColorAscending(alreadyPlayedCards, color);
		// Get the cards in descending order
		List<Card> sortedCardsOfColor = sortCardsOfColorAscending(ownCards, color);
		if (sortedCardsOfColor.isEmpty())
			return 0;
		// Number of cards I have that are higher than the nextCard
		int lowerCards = 0;
		// Number of the cards I have of this color
		int numberOfMyCards = sortedCardsOfColor.size();
		// Estimate how safe you make a Stich with your highest card
		float safety = calculateInitialSafetyUndeUfeRespectingPlayedCards(sortedCardsOfColor, playedCardsOfColor);
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
			int numberOfCardsInbetween = calculateNumberOfCardsInbetweenUndeUfeRespectingPlayedCards(lastCard, nextCard, playedCardsOfColor);
			lowerCards += numberOfCardsInbetween;
			safety *= safetyOfStichVerwerfen(numberOfMyCards, lowerCards, nextCard, lastCard, numberOfCardsInbetween);
			// How safe is the Stich? * Stichvalue
			rating += safety * 20;
			sortedCardsOfColor.remove(0);
			// One card is higher than the last
			lowerCards++;
			lastCard = nextCard;
		}
		return (int) Math.ceil(rating);
	}

	public static int calculateNumberOfCardsInbetweenObeAbeRespectingPlayedCards(Card lastCard, Card nextCard, List<Card> playedCardsOfColor) {
		int numberOfCardsInbetween = lastCard.getRank() - nextCard.getRank() - 1;
		for (Card card : playedCardsOfColor) {
			int rank = card.getRank();
			if (rank < lastCard.getRank() && rank > nextCard.getRank()) {
				numberOfCardsInbetween--;
			}
		}
		if (numberOfCardsInbetween < 0)
			return 0;
		return numberOfCardsInbetween;
	}

	public static int calculateNumberOfCardsInbetweenUndeUfeRespectingPlayedCards(Card lastCard, Card nextCard, List<Card> playedCardsOfColor) {
		int numberOfCardsInbetween = nextCard.getRank() - lastCard.getRank() - 1;
		for (Card card : playedCardsOfColor) {
			int rank = card.getRank();
			if (rank > lastCard.getRank() && rank < nextCard.getRank()) {
				numberOfCardsInbetween--;
			}
		}
		if (numberOfCardsInbetween < 0)
			return 0;
		return numberOfCardsInbetween;
	}

	/**
	 * Gets the card which the partner played in this round
	 *
	 * @param round
	 * @return
	 */
	public static Card getCardOfPartner(Round round) {
		if (CardSelectionHelper.isLastPlayer(round))
			return round.getMoves().get(1).getPlayedCard();
		if (CardSelectionHelper.isThirdPlayer(round))
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
	private static int countNumberOfCardsOfColor(Set<Card> cards, Color color) {
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
		return cards.stream().
				filter(card -> card.getColor().equals(color)).
				collect(Collectors.toSet());
	}

	/**
	 * Checks if the partner has already played a card in the current round
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
	public static boolean isPartner(Player player, Player otherPlayer) {
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

	public static boolean stichBelongsToPartner(Round round) {
		return isPartner(round.getWinner(), round.getCurrentPlayer());
	}

	/**
	 * Gets all the trumps out of the cardset
	 *
	 * @param cards
	 * @param mode
	 * @return
	 */
	public static Set<Card> getTrumpfs(Set<Card> cards, Mode mode) {
		Set<Card> trumpfs = EnumSet.noneOf(Card.class);
		for (Card card : cards)
			if (hasTrumpfColor(card, mode))
				trumpfs.add(card);
		return trumpfs;
	}

	/**
	 * Gets all the cards which are not trumps out of the card set
	 *
	 * @param cards
	 * @param mode
	 * @return
	 */
	public static Set<Card> getNonTrumpfs(Set<Card> cards, Mode mode) {
		Set<Card> nonTrumpfs = EnumSet.noneOf(Card.class);
		for (Card card : cards)
			if (!hasTrumpfColor(card, mode))
				nonTrumpfs.add(card);
		return nonTrumpfs;
	}

	/**
	 * Gets all the cards which are of a given suit out of the card set
	 *
	 * @param cards
	 * @param color
	 * @return
	 */
	public static Set<Card> getCardsOfSuit(Set<Card> cards, Color color) {
		return cards.stream()
				.filter(card -> card.getColor().equals(color))
				.collect(Collectors.toSet());
	}


	/**
	 * Gets all the cards which can be used for Schmieren out of the possible cards
	 *
	 * @param possibleCards
	 * @param mode
	 * @return
	 */
	public static Set<Card> getSchmierCards(Set<Card> possibleCards, Mode mode) {
		List<CardValue> possibleCardValues = new LinkedList<>();
		possibleCardValues.add(CardValue.TEN);
		if (isTopDown(mode))
			possibleCardValues.add(CardValue.EIGHT);
		if (isBottomUp(mode)) {
			possibleCardValues.add(CardValue.KING);
			possibleCardValues.add(CardValue.QUEEN);
			possibleCardValues.add(CardValue.JACK);
		}

		Set<Card> schmierCards = EnumSet.noneOf(Card.class);
		for (Card card : possibleCards)
			if (possibleCardValues.contains(card.getValue()))
				schmierCards.add(card);

		return schmierCards;
	}

	/**
	 * Gets all the cards which can be used for Verwerfen out of the possible cards (= Brettli)
	 *
	 * @param possibleCards
	 * @param mode
	 * @return
	 */
	public static Set<Card> getVerwerfCards(Set<Card> possibleCards, Mode mode) {
		Set<Card> verwerfCards = EnumSet.noneOf(Card.class);
		for (Card card : possibleCards)
			if (isBrettli(card, mode))
				verwerfCards.add(card);
		return verwerfCards;
	}


	public static List<Card> sortCardsOfColorDescending(Set<Card> cards, Color color) {
		Set<Card> cardsOfColor = getCardsOfColor(cards, color);
		return cardsOfColor.stream().sorted(Comparator.comparing(Card::getRank).reversed()).collect(Collectors.toList());
	}

	public static List<Card> sortCardsOfColorAscending(Set<Card> cards, Color color) {
		Set<Card> cardsOfColor = getCardsOfColor(cards, color);
		return cardsOfColor.stream().sorted(Comparator.comparing(Card::getRank)).collect(Collectors.toList());
	}

	public static float calculateInitialSafetyObeabeRespectingPlayedCards(List<Card> sortedCards, List<Card> alreadyPlayedCards) {
		if (alreadyPlayedCards.isEmpty())
			return TrumpfSelectionHelper.calculateInitialSafetyObeabe(sortedCards);
		Card nextCard = sortedCards.get(0);
		Card nextPlayedCard = alreadyPlayedCards.get(0);
		int rank = nextCard.getRank();
		float safety = 1;
		// Probability that my partner has all the higher cards
		for (int i = 0; i < 9 - rank; i++)
			if (nextPlayedCard.getRank() != 9 - i) {
				safety /= 3;
			} else {
				if (!alreadyPlayedCards.isEmpty())
					alreadyPlayedCards.remove(0);
				if (!alreadyPlayedCards.isEmpty())
					nextPlayedCard = alreadyPlayedCards.get(0);
			}
		return safety;
	}


	public static float calculateInitialSafetyUndeUfeRespectingPlayedCards(List<Card> sortedCards, List<Card> alreadyPlayedCards) {
		if (alreadyPlayedCards.isEmpty())
			return TrumpfSelectionHelper.calculateInitialSafetyUndeUfe(sortedCards);
		Card nextCard = sortedCards.get(0);
		Card nextPlayedCard = alreadyPlayedCards.get(0);
		int rank = nextCard.getRank();
		float safety = 1;
		// Probability that my partner has all the higher cards
		for (int i = 0; i < rank - 1; i++)
			if (nextPlayedCard.getRank() != i + 1) {
				safety /= 3;
			} else {
				if (!alreadyPlayedCards.isEmpty())
					alreadyPlayedCards.remove(0);
				if (!alreadyPlayedCards.isEmpty())
					nextPlayedCard = alreadyPlayedCards.get(0);
			}
		return safety;
	}

	private static float safetyOfStichVerwerfen(int numberOfCards, int higherCards, Card nextCard, Card lastCard, int numberOfCardsInbetween) {
		// Have the next-higher card => probability to stich is the same as with the next higher card
		if (numberOfCardsInbetween == 0)
			return 1;

		float safetyFactor = 1;
		// For each card inbetweeen, reduce safety to a fourth
		for (int i = 0; i < numberOfCardsInbetween; i++) {
			safetyFactor *= 0.25;
		}
		return safetyFactor;
	}

}
