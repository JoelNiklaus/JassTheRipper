package com.zuehlke.jasschallenge.client.game.strategy.helpers;

import com.zuehlke.jasschallenge.client.game.*;
import com.zuehlke.jasschallenge.client.game.strategy.JassTheRipperJassStrategy;
import com.zuehlke.jasschallenge.game.Trumpf;
import com.zuehlke.jasschallenge.game.cards.Card;
import com.zuehlke.jasschallenge.game.cards.CardValue;
import com.zuehlke.jasschallenge.game.cards.Color;
import com.zuehlke.jasschallenge.game.mode.Mode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by joelniklaus on 05.05.17.
 */
public class JassHelper {

	public static final int BRETTLI_BOUNDARY = 5; // TEN
	public static final int EIGHT = 3; // EIGHT
	public static final int NELL = 4; // NELL

	public static final Logger logger = LoggerFactory.getLogger(JassHelper.class);


	/**
	 * ANZIEHEN (Nachricht empfangen)
	 */
	// if my partner played anziehen in one of the previous rounds, play this color
	public static Color detectAnziehen(Game game) {
		Mode mode = game.getMode();
		if (isNotTrumpf(mode))
			return null; // abort if notTrumpf
		Player player = game.getCurrentPlayer();
		Player partner = game.getPartnerOfPlayer(player);
		List<Round> previousRounds = game.getPreviousRounds();
		for (Round round : previousRounds) {
			Card myCard = round.getCardOfPlayer(player);
			assert myCard != null;
			Card cardOfPartner = round.getCardOfPlayer(partner);
			assert cardOfPartner != null;
			if (isBrettli(cardOfPartner, mode)) {
				// ANZIEHEN STARTING
				if (wasStartingPlayer(partner, round))
					return cardOfPartner.getColor();
				// ANZIEHEN LATER
				if ((wasThirdPlayer(partner, round) || wasLastPlayer(partner, round))
						&& cardOfPartner.hasDifferentColor(myCard))
					return cardOfPartner.getColor();
			}
		}
		return null;
	}


	/**
	 * VERWERFEN (Nachricht empfangen)
	 * TODO schauen dass der schluss es nicht verfälscht -> es sollte die runde mit kleinster round number zuerst anschauen
	 */
	// if my partner played verwerfen in one of the previous rounds, do not play this color
	public static Color detectVerwerfen(Game game) {
		Mode mode = game.getMode();
		if (isTrumpf(mode))
			return null; // abort if trumpf

		Player player = game.getCurrentPlayer();
		Player partner = game.getPartnerOfPlayer(player);
		List<Round> previousRounds = game.getPreviousRounds();
		for (Round round : previousRounds) {
			if (wasStartingPlayer(player, round)
					&& round.getWinner().equals(player)) {
				Card myCard = round.getCardOfPlayer(player);
				assert myCard != null;
				Card cardOfPartner = round.getCardOfPlayer(partner);
				assert cardOfPartner != null;

				if (!cardOfPartner.getColor().equals(myCard.getColor()) && isBrettli(cardOfPartner, mode))
					return cardOfPartner.getColor();
			}
		}
		return null;
	}

	/**
	 * Determines if the card is a brettli (a low card which has zero points)
	 *
	 * @param card
	 * @param mode
	 * @return
	 */
	private static boolean isBrettli(Card card, Mode mode) {
		int rank = card.getRank();
		if (isBottomUp(mode)) {
			return rank > BRETTLI_BOUNDARY;
		}
		if (rank < BRETTLI_BOUNDARY) {
			if (isTopDown(mode)) {
				return rank != EIGHT;
			} else if (!hasTrumpfColor(card, mode))
				return true;
		}
		return false;
	}

	private static boolean isNell(Card card, Mode mode) {
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

	/**
	 * Checks if the player was the starting player in the past round
	 *
	 * @param player
	 * @param round
	 * @return
	 */
	private static boolean wasStartingPlayer(Player player, Round round) {
		return round.getPlayingOrder().getPlayersInInitialPlayingOrder().get(0).equals(player);
	}

	/**
	 * Checks if the player was the second player in the past round
	 *
	 * @param player
	 * @param round
	 * @return
	 */
	private static boolean wasSecondPlayer(Player player, Round round) {
		return round.getPlayingOrder().getPlayersInInitialPlayingOrder().get(1).equals(player);
	}

	/**
	 * Checks if the player was the third player in the past round
	 *
	 * @param player
	 * @param round
	 * @return
	 */
	private static boolean wasThirdPlayer(Player player, Round round) {
		return round.getPlayingOrder().getPlayersInInitialPlayingOrder().get(2).equals(player);
	}

	/**
	 * Checks if the player was the last player in the past round
	 *
	 * @param player
	 * @param round
	 * @return
	 */
	private static boolean wasLastPlayer(Player player, Round round) {
		return round.getPlayingOrder().getPlayersInInitialPlayingOrder().get(3).equals(player);
	}

	/**
	 * TODO Maybe this can be used as a heuristic function in the MCTS!
	 * <p>
	 * Reduces the set of the possible cards which can be played in a move to the sensible cards.
	 * This is done by expert jass knowledge. It is done here so that all the players play as intelligently as possible
	 * and therefore the simulation gets the most realistic outcome.
	 *
	 * @param possibleCards
	 * @return
	 */
	public static Set<Card> refineCardsWithJassKnowledge(Set<Card> possibleCards, Game game) {
		final Round round = game.getCurrentRound();
		final Player player = game.getCurrentPlayer();
		final Set<Card> alreadyPlayedCards = game.getAlreadyPlayedCards();
		final Mode mode = round.getMode();

		/**
		 * STECHEN (als letzter Spieler)
		 */
		if (shouldStechen(round, player)) {
			int stichValue = round.calculateScore();
			Set<Card> roundWinningCards = getRoundWinningCards(possibleCards, round);

			// wenn möglich mit nicht trumpf zu stechen
			Set<Card> notTrumpsOfRoundWinningCards = getNotTrumps(roundWinningCards, mode);
			if (!notTrumpsOfRoundWinningCards.isEmpty())
				return notTrumpsOfRoundWinningCards;

			// wenn möglich mit trumpf zu stechen und stich hat mindestens 10 punkte
			Set<Card> trumpsOfRoundWinningCards = getTrumps(roundWinningCards, mode);
			if (!trumpsOfRoundWinningCards.isEmpty() && round.calculateScore() > 10)
				return trumpsOfRoundWinningCards;
		}

		/**
		 * AUSTRUMPFEN
		 */
		final Set<Card> trumps = getTrumps(possibleCards, mode);
		if (shouldAustrumpfen(round, trumps))
			return trumps;

		/**
		 * VERWERFEN (Nachricht empfangen)
		 */
		// TODO this has to be more thoroughly thought through! What if I am really good at the color I should not play now?
		/*
		Color verworfen = detectVerwerfen(game);
		Set<Card> cardsVerworfenColor = getCardsOfColor(possibleCards, verworfen);
		if (startingPlayer(round) && verworfen != null
				&& cardsVerworfenColor.size() < possibleCards.size()) {
			possibleCards.removeAll(cardsVerworfenColor);
		}
		*/

		/**
		 * ANZIEHEN (Nachricht empfangen)
		 */
		// TODO this has to be more thoroughly thought through! What if there are no trumps left and I can just do all the obeabe stichs?
		/*
		Color angezogen = detectAnziehen(game);
		Set<Card> cardsAngezogenColor = getCardsOfColor(possibleCards, angezogen);
		if (startingPlayer(round) && verworfen != null
				&& !cardsAngezogenColor.isEmpty())
			return cardsAngezogenColor;
		*/

		/**
		 * ANZIEHEN STARTING (Nachricht senden)
		 */
		/*
		if (shouldAnziehenStarting(possibleCards, round, alreadyPlayedCards, mode)) {
			Color color = getBestAnziehenColor(possibleCards, alreadyPlayedCards, true);
			Set<Card> brettli = getBrettli(possibleCards, mode, color);
			if (!brettli.isEmpty())
				return brettli;
		}
		*/

		/*
		// wenn partner schon gespielt hat
		if (hasPartnerAlreadyPlayed(round)) {
			Card cardOfPartner = getCardOfPartner(round);
			// wenn partner den stich macht bis jetzt
			if (round.getWinningCard().equals(cardOfPartner)) {
				// wenn ich noch angeben kann
				if (isAngebenPossible(possibleCards, cardOfPartner)) {
				*/
		/**
		 * SCHMIEREN
		 */
					/*
					Set<Card> schmierCards = getSchmierCards(possibleCards, cardOfPartner, mode);
					// wenn letzter spieler einfach schmieren
					if (lastPlayer(round))
						return schmierCards;
						// TODO wenn zweitletzter spieler prüfen ob letzer spieler noch stechen kann
						// TODO für jeden Spieler Karteneinschätzung machen!!!
					else {
						assert thirdPlayer(round);
						// TODO to change
						return schmierCards;
					}
				}
				// wenn ich nicht mehr angeben kann
				else {
				*/
		/**
		 * VERWERFEN (Nachricht senden)
		 */
					/*
					if (!isTrumpf(mode)) {
						// if at least one color is good -> get best color
						if (shouldVerwerfen(possibleCards, alreadyPlayedCards, isTopDown(mode))) {
							Color color = getBestVerwerfColor(possibleCards, alreadyPlayedCards, isTopDown(mode));
							Set<Card> brettli = getBrettli(possibleCards, mode, color);
							if (!brettli.isEmpty())
								return brettli;
						}
					}
					*/
		/**
		 * ANZIEHEN LATER (Nachricht senden)
		 */
					/*
					else {
						if (shouldAnziehen(possibleCards, alreadyPlayedCards, true)) {
							Color color = getBestAnziehenColor(possibleCards, alreadyPlayedCards, true);
							Set<Card> brettli = getBrettli(possibleCards, mode, color);
							if (!brettli.isEmpty())
								return brettli;
						}
					}
					*/
					/*
				}
			}
		}
		*/

		return possibleCards;
	}

	private static boolean shouldStechen(Round round, Player player) {
		// wenn letzter Spieler und Stich gehört Gegner
		return lastPlayer(round) && isOpponent(round.getWinner(), player);
	}

	private static boolean shouldAnziehenStarting(Set<Card> possibleCards, Round round, Set<Card> alreadyPlayedCards, Mode mode) {
		// Wenn erster spieler und ein Trumpf und anziehen macht sinn
		return startingPlayer(round) && isTrumpf(mode)
				&& shouldAnziehen(possibleCards, alreadyPlayedCards, true);
	}

	private static boolean shouldAustrumpfen(Round round, Set<Card> trumps) {
		// Wenn erster spieler am anfang des spiels (erste beide runden) und mindestens 2 trümpfe
		return startingPlayer(round) && round.getRoundNumber() <= 1 && trumps.size() >= 2;
	}

	private static Set<Card> getBrettli(Set<Card> possibleCards, Mode mode, Color color) {
		Set<Card> cards = getCardsOfColor(possibleCards, color);
		if (isBottomUp(mode))
			cards = cards.parallelStream().filter(card -> card.getRank() > BRETTLI_BOUNDARY).collect(Collectors.toSet());
		else {
			cards = cards.parallelStream().filter(card -> card.getRank() < BRETTLI_BOUNDARY).collect(Collectors.toSet());
			if (isTopDown(mode))
				cards = cards.parallelStream().filter(card -> card.getRank() != EIGHT).collect(Collectors.toSet());
		}
		return cards;
	}

	private static boolean isTrumpf(Mode mode) {
		return !isNotTrumpf(mode);
	}

	private static boolean isNotTrumpf(Mode mode) {
		return isBottomUp(mode) || isTopDown(mode);
	}

	/* TODO: Hey Joel, this is the boolean helperMethod you asked for. If you want to change the return logic (e.g.
	 * return true if you can make 3 Stichs) you shouldn't have any problems, I've written down what the return
	 * statements calculated mean in a comment above them.
	 * Below: Same for verworfen, returns true if you can make less than one Stich with your worst color (almost always
	 * the case; if you want it to be if you are very unlikely to make a Stich, make it return worstRating <= 2,
	 * if you want it to be quite unlikely make it return worstRating <= 8
	 * @Note: If you want to have the best and worst Color for 'Anziehen' and 'Verwerfen', there are helperMethods for
	 * that below this method.
	 * */

	/**
	 * Returns true if the player can make ca. > 65% of the remaining Stichs or at minimum 5 Stich or at minimum
	 * ca. 3 Stichs with his best color (the one to be angezogen)
	 *
	 * @param ownCards           - the cards of the player
	 * @param alreadyPlayedCards - the cards which have already been played in the game
	 * @param obeAbe             - if Obeabe true, if Undeufe false (we only do anziehen if it is trumpf -> therefore obeabe rating is relevant
	 * @return
	 */
	private static boolean shouldAnziehen(Set<Card> ownCards, Set<Card> alreadyPlayedCards, boolean obeAbe) {
		// sum is (#Stichs the Player can make) * 19
		int sum = 0;
		// bestRating is (#Stichs the Player can make with his best color) * 19
		int bestRating = 0;
		int rating;
		Color bestColor = Color.CLUBS;
		for (Color color : Color.values()) {
			if (obeAbe)
				rating = rateColorObeAbeRespectingAlreadyPlayedCards(ownCards, alreadyPlayedCards, color);
			else
				rating = rateColorUndeUfeRespectingAlreadyPlayedCards(ownCards, alreadyPlayedCards, color);
			if (bestRating < rating) {
				bestRating = rating;
				bestColor = color;
			}
			sum += rating;
		}
		Set<Card> cardsOfBestColor = getCardsOfColor(ownCards, bestColor);
		// As a safety measure ;)
		if (ownCards.isEmpty())
			return false;
		// 65 means: 65% (~2/3) of the remaining Stichs can be made
		// 100 = 5*20 ~ 5 Stichs => sum >= 95 (-5 for float imprecision as each of the 5 Stichs may be valued with only 19)
		// means can make at minimum 5 Stichs
		if (5f * sum / ownCards.size() > 65 || sum >= 95)
			return true;
		// 60 is about three Stich (3*20); -3 for float imprecision
		if (!cardsOfBestColor.isEmpty())
			return (bestRating >= 57);
		return false;
	}

	/**
	 * Returns true if the player can <=1 Stich with the worst color (the one to be verworfen)
	 *
	 * @param ownCards           - the cards of the player
	 * @param alreadyPlayedCards - the cards which have already been played in the game
	 * @param obeAbe             - if Obeabe true, if Undeufe false
	 * @return
	 */
	private static boolean shouldVerwerfen(Set<Card> ownCards, Set<Card> alreadyPlayedCards, boolean obeAbe) {
		// sum is (#Stichs the Player can make) * 19
		int sum = 0;
		// bestRating is (#Stichs the Player can make with his best color) * 19
		int worstRating = 0;
		int rating;
		Color worstColor = Color.CLUBS;
		for (Color color : Color.values()) {
			Set<Card> cardsOfColor = getCardsOfColor(ownCards, color);
			if (!cardsOfColor.isEmpty()) {
				if (obeAbe)
					rating = rateColorObeAbeRespectingAlreadyPlayedCards(ownCards, alreadyPlayedCards, color);
				else
					rating = rateColorUndeUfeRespectingAlreadyPlayedCards(ownCards, alreadyPlayedCards, color);
				if (worstRating > rating) {
					worstRating = rating;
					worstColor = color;
				}
				sum += rating;
			}
		}
		Set<Card> cardsOfWorstColor = getCardsOfColor(ownCards, worstColor);
		// As a safety measure ;)
		if (ownCards.isEmpty())
			return false;
		// Can make less than one Stich => verwerfen
		if (!cardsOfWorstColor.isEmpty())
			return (worstRating < 20);
		return false;
	}

	/**
	 * Returns the best color to be 'angezogen'.
	 *
	 * @param ownCards           - the cards of the player
	 * @param alreadyPlayedCards - the cards which have already been played in the game
	 * @param obeAbe             - if Obeabe true, if Undeufe false
	 * @return
	 */
	private static Color getBestAnziehenColor(Set<Card> ownCards, Set<Card> alreadyPlayedCards, boolean obeAbe) {
		int bestRating = 0;
		int rating;
		Color bestColor = Color.DIAMONDS;
		for (Color color : Color.values()) {
			if (obeAbe)
				rating = rateColorObeAbeRespectingAlreadyPlayedCards(ownCards, alreadyPlayedCards, color);
			else
				rating = rateColorUndeUfeRespectingAlreadyPlayedCards(ownCards, alreadyPlayedCards, color);
			if (bestRating < rating) {
				bestRating = rating;
				bestColor = color;
			}
		}
		return bestColor;
	}

	/**
	 * Returns the best color to be 'verworfen' (so the player's worst color, actually).
	 *
	 * @param ownCards           - the cards of the player
	 * @param alreadyPlayedCards - the cards which have already been played in the game
	 * @param obeAbe             - if Obeabe true, if Undeufe false
	 * @return
	 */
	private static Color getBestVerwerfColor(Set<Card> ownCards, Set<Card> alreadyPlayedCards, boolean obeAbe) {
		int worstRating = 500;
		int rating;
		Color worstColor = Color.CLUBS;
		for (Color color : Color.values()) {
			if (obeAbe)
				rating = rateColorObeAbeRespectingAlreadyPlayedCards(ownCards, alreadyPlayedCards, color);
			else
				rating = rateColorUndeUfeRespectingAlreadyPlayedCards(ownCards, alreadyPlayedCards, color);
			if (worstRating > rating) {
				worstRating = rating;
				worstColor = color;
			}
		}
		return worstColor;
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
		while (sortedCardsOfColor.size() > 0) {
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
		while (sortedCardsOfColor.size() > 0) {
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
	 * Get all of my cards which can win the round.
	 *
	 * @param possibleCards
	 * @param round
	 * @return
	 */
	private static Set<Card> getRoundWinningCards(Set<Card> possibleCards, Round round) {
		Set<Card> remainingCards = new HashSet<>(possibleCards);
		Card winningCard = round.getWinningCard();
		Set<Card> cardsToRemove = EnumSet.noneOf(Card.class);
		for (Card card : remainingCards) {
			List<Card> cards = new LinkedList<>();
			cards.add(card);
			cards.add(winningCard);
			if (round.getMode().determineWinningCard(cards).equals(winningCard))
				cardsToRemove.add(card);
		}
		if (remainingCards.size() > cardsToRemove.size())
			remainingCards.removeAll(cardsToRemove);
		return remainingCards;
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
		assert !availableCards.isEmpty();
		Round round = game.getCurrentRound();
		Mode mode = round.getMode();
		// If you have a card
		Set<Card> validCards = availableCards.parallelStream().
				filter(card -> mode.canPlayCard(card, round.getPlayedCards(), round.getRoundColor(), availableCards)).
				collect(Collectors.toSet());
		if (!validCards.isEmpty())
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
	 * Checks if the current player is the first player in the current round
	 *
	 * @param round
	 * @return
	 */
	public static boolean startingPlayer(Round round) {
		return round.numberOfPlayedCards() == 0;
	}

	/**
	 * Checks if the current player is the second player in the current round
	 *
	 * @param round
	 * @return
	 */
	public static boolean secondPlayer(Round round) {
		return round.numberOfPlayedCards() == 1;
	}

	/**
	 * Checks if the current player is the third player in the current round
	 *
	 * @param round
	 * @return
	 */
	public static boolean thirdPlayer(Round round) {
		return round.numberOfPlayedCards() == 2;
	}

	/**
	 * Checks if the current player is the last player in the current round
	 *
	 * @param round
	 * @return
	 */
	public static boolean lastPlayer(Round round) {
		return round.numberOfPlayedCards() == 3;
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
				.filter(card -> hasTrumpfColor(card, mode))
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
				.filter(card -> !hasTrumpfColor(card, mode))
				.collect(Collectors.toSet());
	}


	/**
	 * Gets all the cards which can be used to schmieren out of the possible cards
	 * TODO could be made more sophisticated
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
		if (isTopDown(mode))
			possibleCardValues.add(CardValue.EIGHT);

		Set<Card> schmierCards = EnumSet.noneOf(Card.class);
		for (Card card : cardsOfColour)
			if (possibleCardValues.contains(card.getValue()))
				schmierCards.add(card);

		if (!schmierCards.isEmpty())
			return schmierCards;
		return cardsOfColour;
	}


	public static Mode predictTrumpf(Set<Card> availableCards, Mode prospectiveMode, boolean isGschobe) {
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
		logger.info("ChooseTrumpf succeeded!");
		if (max < JassTheRipperJassStrategy.MAX_SHIFT_RATING_VAL && !isGschobe)
			return Mode.shift();
		return prospectiveMode;
	}

	public static int rateColorForTrumpf(Set<Card> cards, Color color) {
		Set<Card> cardsOfColor = getCardsOfColor(cards, color);
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

	private static boolean containsTwoOrMoreAces(Set<Card> cardStream) {
		List<Card> cardsSorted = cardStream.stream().sorted(Comparator.comparing(Card::getRank).reversed()).collect(Collectors.toList());
		int countAces = 0;
		while (cardsSorted.get(0).getRank() == 9) {
			countAces++;
			cardsSorted.remove(0);
		}
		return countAces >= 2;
	}

	public static int rateObeabe(Set<Card> cards) {
		int sum = 0;
		for (Color color : Color.values()) {
			sum += rateObeabeColor(cards, color);
		}
		return sum;
	}

	public static int rateUndeufe(Set<Card> cards) {
		int sum = 0;
		for (Color color : Color.values()) {
			sum += rateUndeufeColor(cards, color);
		}
		return sum;
	}

	public static int rateObeabeColor(Set<Card> cards, Color color) {
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

	public static List<Card> sortCardsOfColorDescending(Set<Card> cards, Color color) {
		Set<Card> cardsOfColor = getCardsOfColor(cards, color);
		return cardsOfColor.stream().sorted(Comparator.comparing(Card::getRank).reversed()).collect(Collectors.toList());
	}

	public static List<Card> sortCardsOfColorAscending(Set<Card> cards, Color color) {
		Set<Card> cardsOfColor = getCardsOfColor(cards, color);
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

	public static float calculateInitialSafetyObeabeRespectingPlayedCards(List<Card> sortedCards, List<Card> alreadyPlayedCards) {
		if (alreadyPlayedCards.isEmpty())
			return calculateInitialSafetyObeabe(sortedCards);
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


	public static float calculateInitialSafetyUndeUfe(List<Card> sortedCards) {
		Card nextCard = sortedCards.get(0);
		int rank = nextCard.getRank();
		float safety = 1;
		// Probability that my partner has all the lower cards
		for (int i = 0; i < rank - 1; i++)
			safety /= 3;
		return safety;
	}

	public static float calculateInitialSafetyUndeUfeRespectingPlayedCards(List<Card> sortedCards, List<Card> alreadyPlayedCards) {
		if (alreadyPlayedCards.isEmpty())
			return calculateInitialSafetyUndeUfe(sortedCards);
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

	private static float safetyOfStich(int numberOfCards, int higherCards, Card nextCard, Card lastCard, int numberOfCardsInbetween) {
		// Have the next-higher card => probability to stich is the same as with the next higher card
		if (numberOfCardsInbetween == 0)
			return 1;
			// Probability to stich is 1 - the probability, that enemy has a higher card + enough cards to discard
		else
			return 1 - ((float) 2 / 3 * enemenyHasNoMoreCards(numberOfCards, higherCards, numberOfCardsInbetween));
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

	public static int rateUndeufeColor(Set<Card> cards, Color color) {
		// Get the cards in ascending order
		List<Card> sortedCards = sortCardsOfColorAscending(cards, color);
		if (sortedCards.isEmpty())
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
			int numberOfCardsInbetween = nextCard.getRank() - lastCard.getRank() - 1;
			safety *= safetyOfStich(numberOfMyCards, lowerCards, nextCard, lastCard, numberOfCardsInbetween);
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
