package to.joeli.jass.client.game.strategy.helpers;

import to.joeli.jass.client.game.Game;
import to.joeli.jass.client.game.Player;
import to.joeli.jass.client.game.Round;
import to.joeli.jass.game.cards.Card;
import to.joeli.jass.game.cards.Color;
import to.joeli.jass.game.mode.Mode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class CardSelectionHelper {

	public static final Logger logger = LoggerFactory.getLogger(CardSelectionHelper.class);


	private CardSelectionHelper() {

	}

	/**
	 * Get a random card out of my available cards
	 *
	 * @param availableCards
	 * @param game
	 * @return
	 */
	public static Card getRandomCard(Set<Card> availableCards, Game game) {
		return getCardsPossibleToPlay(availableCards, game).stream()
				.findAny()
				.orElseThrow(() -> new RuntimeException("There should always be a card to play"));
	}

	/**
	 * TODO Maybe this can be used as a heuristicFunction function in the MCTS!
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
			Set<Card> notTrumpsOfRoundWinningCards = JassHelper.getNotTrumps(roundWinningCards, mode);
			if (!notTrumpsOfRoundWinningCards.isEmpty())
				return notTrumpsOfRoundWinningCards;

			// wenn möglich mit trumpf zu stechen und stich hat mindestens 10 punkte
			Set<Card> trumpsOfRoundWinningCards = JassHelper.getTrumps(roundWinningCards, mode);
			if (!trumpsOfRoundWinningCards.isEmpty() && round.calculateScore() > 10)
				return trumpsOfRoundWinningCards;
		}

		/**
		 * AUSTRUMPFEN
		 */
		final Set<Card> trumps = JassHelper.getTrumps(possibleCards, mode);
		if (shouldAustrumpfen(round, trumps))
			return trumps;

		/**
		 * VERWERFEN (Nachricht empfangen)
		 */
		// TODO this has to be more thoroughly thought through! What if I am really good at the color I should not play now?
		/*
		Color verworfen = detectVerwerfen(game);
		Set<Card> cardsVerworfenColor = getCardsOfColor(possibleCards, verworfen);
		if (isStartingPlayer(round) && verworfen != null
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
		if (isStartingPlayer(round) && verworfen != null
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
					if (isLastPlayer(round))
						return schmierCards;
						// TODO wenn zweitletzter spieler prüfen ob letzer spieler noch stechen kann
						// TODO für jeden Spieler Karteneinschätzung machen!!!
					else {
						assert isThirdPlayer(round);
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
							Color color = getBestVerwerfenColor(possibleCards, alreadyPlayedCards, isTopDown(mode));
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

	/**
	 * ANZIEHEN (Nachricht empfangen)
	 */
	// if my partner played anziehen in one of the previous rounds, play this color
	public static Color detectAnziehen(Game game) {
		Mode mode = game.getMode();
		if (JassHelper.isNotTrumpf(mode))
			return null; // abort if notTrumpf
		Player player = game.getCurrentPlayer();
		Player partner = game.getPartnerOfPlayer(player);
		List<Round> previousRounds = game.getPreviousRounds();
		for (Round round : previousRounds) {
			Card myCard = round.getCardOfPlayer(player);
			assert myCard != null;
			Card cardOfPartner = round.getCardOfPlayer(partner);
			assert cardOfPartner != null;
			if (JassHelper.isBrettli(cardOfPartner, mode)) {
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
		if (JassHelper.isTrumpf(mode))
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

				if (!cardOfPartner.getColor().equals(myCard.getColor()) && JassHelper.isBrettli(cardOfPartner, mode))
					return cardOfPartner.getColor();
			}
		}
		return null;
	}


	private static boolean shouldStechen(Round round, Player player) {
		// wenn letzter Spieler und Stich gehört Gegner
		return isLastPlayer(round) && JassHelper.isOpponent(round.getWinner(), player);
	}

	private static boolean shouldAnziehenStarting(Set<Card> possibleCards, Round round, Set<Card> alreadyPlayedCards, Mode mode) {
		// Wenn erster spieler und ein Trumpf und anziehen macht sinn
		return isStartingPlayer(round) && JassHelper.isTrumpf(mode)
				&& shouldAnziehen(possibleCards, alreadyPlayedCards, true);
	}

	private static boolean shouldAustrumpfen(Round round, Set<Card> trumps) {
		// Wenn erster spieler am anfang des spiels (erste beide runden) und mindestens 2 trümpfe
		return isStartingPlayer(round) && round.getRoundNumber() <= 1 && trumps.size() >= 2;
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
				rating = JassHelper.rateColorObeAbeRespectingAlreadyPlayedCards(ownCards, alreadyPlayedCards, color);
			else
				rating = JassHelper.rateColorUndeUfeRespectingAlreadyPlayedCards(ownCards, alreadyPlayedCards, color);
			if (bestRating < rating) {
				bestRating = rating;
				bestColor = color;
			}
			sum += rating;
		}
		Set<Card> cardsOfBestColor = JassHelper.getCardsOfColor(ownCards, bestColor);
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
			Set<Card> cardsOfColor = JassHelper.getCardsOfColor(ownCards, color);
			if (!cardsOfColor.isEmpty()) {
				if (obeAbe)
					rating = JassHelper.rateColorObeAbeRespectingAlreadyPlayedCards(ownCards, alreadyPlayedCards, color);
				else
					rating = JassHelper.rateColorUndeUfeRespectingAlreadyPlayedCards(ownCards, alreadyPlayedCards, color);
				if (worstRating > rating) {
					worstRating = rating;
					worstColor = color;
				}
				sum += rating;
			}
		}
		Set<Card> cardsOfWorstColor = JassHelper.getCardsOfColor(ownCards, worstColor);
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
				rating = JassHelper.rateColorObeAbeRespectingAlreadyPlayedCards(ownCards, alreadyPlayedCards, color);
			else
				rating = JassHelper.rateColorUndeUfeRespectingAlreadyPlayedCards(ownCards, alreadyPlayedCards, color);
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
	private static Color getBestVerwerfenColor(Set<Card> ownCards, Set<Card> alreadyPlayedCards, boolean obeAbe) {
		int worstRating = 500;
		int rating;
		Color worstColor = Color.CLUBS;
		for (Color color : Color.values()) {
			if (obeAbe)
				rating = JassHelper.rateColorObeAbeRespectingAlreadyPlayedCards(ownCards, alreadyPlayedCards, color);
			else
				rating = JassHelper.rateColorUndeUfeRespectingAlreadyPlayedCards(ownCards, alreadyPlayedCards, color);
			if (worstRating > rating) {
				worstRating = rating;
				worstColor = color;
			}
		}
		return worstColor;
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
	 * Get the set of cards which are possible to play at this moment according to the game rules
	 *
	 * @param availableCards
	 * @param game
	 * @return
	 */
	public static Set<Card> getCardsPossibleToPlay(Set<Card> availableCards, Game game) {
		assert !availableCards.isEmpty();
		Round round = game.getCurrentRound();
		Mode mode = round.getMode();
		// If you have a card
		Set<Card> validCards = availableCards.stream().
				filter(card -> mode.canPlayCard(card, round.getPlayedCards(), round.getRoundColor(), availableCards)).
				collect(Collectors.toSet());
		if (!validCards.isEmpty())
			return validCards;
		else
			return availableCards;
	}


	/**
	 * Checks if the player was the starting player in the past round
	 *
	 * @param player
	 * @param round
	 * @return
	 */
	private static boolean wasStartingPlayer(Player player, Round round) {
		return round.getPlayingOrder().getPlayersInInitialOrder().get(0).equals(player);
	}

	/**
	 * Checks if the player was the second player in the past round
	 *
	 * @param player
	 * @param round
	 * @return
	 */
	private static boolean wasSecondPlayer(Player player, Round round) {
		return round.getPlayingOrder().getPlayersInInitialOrder().get(1).equals(player);
	}

	/**
	 * Checks if the player was the third player in the past round
	 *
	 * @param player
	 * @param round
	 * @return
	 */
	private static boolean wasThirdPlayer(Player player, Round round) {
		return round.getPlayingOrder().getPlayersInInitialOrder().get(2).equals(player);
	}

	/**
	 * Checks if the player was the last player in the past round
	 *
	 * @param player
	 * @param round
	 * @return
	 */
	private static boolean wasLastPlayer(Player player, Round round) {
		return round.getPlayingOrder().getPlayersInInitialOrder().get(3).equals(player);
	}

	/**
	 * Checks if the current player is the first player in the current round
	 *
	 * @param round
	 * @return
	 */
	private static boolean isStartingPlayer(Round round) {
		return round.numberOfPlayedCards() == 0;
	}

	/**
	 * Checks if the current player is the second player in the current round
	 *
	 * @param round
	 * @return
	 */
	public static boolean isSecondPlayer(Round round) {
		return round.numberOfPlayedCards() == 1;
	}

	/**
	 * Checks if the current player is the third player in the current round
	 *
	 * @param round
	 * @return
	 */
	static boolean isThirdPlayer(Round round) {
		return round.numberOfPlayedCards() == 2;
	}

	/**
	 * Checks if the current player is the last player in the current round
	 *
	 * @param round
	 * @return
	 */
	static boolean isLastPlayer(Round round) {
		return round.numberOfPlayedCards() == 3;
	}


}
