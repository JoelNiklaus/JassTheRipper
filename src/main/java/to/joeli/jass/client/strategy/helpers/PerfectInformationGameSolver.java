package to.joeli.jass.client.strategy.helpers;

import to.joeli.jass.client.game.Game;
import to.joeli.jass.client.game.Player;
import to.joeli.jass.client.game.Round;
import to.joeli.jass.client.strategy.mcts.CardMove;
import to.joeli.jass.game.cards.Card;
import to.joeli.jass.game.cards.Color;
import to.joeli.jass.game.mode.Mode;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Uses rules for good play in a perfect information game scenario to find a move.
 * Using a rule based bot like for example the challenge bot would be an idea too.
 */
public class PerfectInformationGameSolver {

	private PerfectInformationGameSolver() {
	}

	/**
	 * Computationally light playout. Makes it possible to run more but is of lesser quality.
	 *
	 * @param game
	 * @return
	 */
	public static CardMove runLightPlayout(Game game) {
		final Player player = game.getCurrentPlayer();
		final Set<Card> possibleCards = CardSelectionHelper.getCardsPossibleToPlay(EnumSet.copyOf(player.getCards()), game);

		final Set<Card> refinedCards = CardSelectionHelper.refineCardsWithJassKnowledge(possibleCards, game);
		final Card card = CardSelectionHelper.chooseRandomCard(refinedCards);
		return new CardMove(player, card);
	}

	/**
	 * Computationally heavy playout. Is of better quality but takes more time to execute.
	 *
	 * @param game
	 * @return
	 */
	public static CardMove runHeavyPlayout(Game game) {
		final Round round = game.getCurrentRound();
		final Mode mode = game.getMode();
		final Player player = game.getCurrentPlayer();
		final Set<Card> possibleCards = CardSelectionHelper.getCardsPossibleToPlay(EnumSet.copyOf(player.getCards()), game);

		final Set<Card> advisableCards = EnumSet.noneOf(Card.class);
		switch (round.numberOfPlayedCards()) {
			case 0: // We are the leading player
				actAsLeadingPlayer(game, round, mode, possibleCards, advisableCards);
				break;
			/*case 1: // We are the second player
				boolean stichBelongsToOpponents = !CardSelectionHelper.partnerCanDefinitelyWinStich(round);  // the partner cannot win the stich
				actAsFollowingPlayer(round, mode, possibleCards, advisableCards, stichBelongsToOpponents);
				break;
			case 2: // We are the third player
				stichBelongsToOpponents = JassHelper.isOpponent(round.getWinner(), player) // The stich already belongs to the opponents
						|| CardSelectionHelper.opponentCanWinStich(round);
				actAsFollowingPlayer(round, mode, possibleCards, advisableCards, stichBelongsToOpponents);
				break;*/
			case 3: // We are the last player
				boolean stichBelongsToOpponents = JassHelper.isOpponent(round.getWinner(), player);
				actAsFollowingPlayer(round, mode, possibleCards, advisableCards, stichBelongsToOpponents);
				break;
		}

		if (advisableCards.isEmpty()) // in case there is no good option
			advisableCards.addAll(possibleCards); // we have to choose from the possible cards
		Card card = CardSelectionHelper.chooseRandomCard(advisableCards);
		return new CardMove(player, card);
	}

	/**
	 * Describes a set of rules to follow when we are not the first player in the round.
	 *
	 * @param round
	 * @param mode
	 * @param possibleCards
	 * @param advisableCards
	 * @param stichBelongsToOpponents
	 */
	private static void actAsFollowingPlayer(Round round, Mode mode, Set<Card> possibleCards, Set<Card> advisableCards, boolean stichBelongsToOpponents) {
		if (stichBelongsToOpponents) { // The stich belongs to the opponents
			// STECHEN
			final Set<Card> roundWinningCards = CardSelectionHelper.getRoundWinningCards(possibleCards, round);

			final Set<Card> notTrumpsOfRoundWinningCards = JassHelper.getNonTrumpfs(roundWinningCards, mode);
			if (!notTrumpsOfRoundWinningCards.isEmpty())
				// wenn möglich mit nicht trumpf zu stechen
				advisableCards.addAll(notTrumpsOfRoundWinningCards);

			else {
				// wenn möglich mit trumpf zu stechen und stich hat mindestens 10 punkte
				final Set<Card> trumpsOfRoundWinningCards = JassHelper.getTrumpfs(roundWinningCards, mode);
				if (!trumpsOfRoundWinningCards.isEmpty() && round.calculateScore() > 10)
					advisableCards.addAll(trumpsOfRoundWinningCards);
			}
		} else { // The stich belongs to my partner
			// SCHMIEREN
			final Set<Card> schmierCards = JassHelper.getSchmierCards(possibleCards, mode);
			if (!schmierCards.isEmpty())
				advisableCards.addAll(schmierCards);
		}
		if (advisableCards.isEmpty()) { // if we did not find any good cards yet
			// VERWERFEN
			final Set<Card> verwerfCards = JassHelper.getVerwerfCards(possibleCards, mode);
			if (!verwerfCards.isEmpty())
				advisableCards.addAll(verwerfCards);
		}
	}

	/**
	 * Describes a set of rules to follow when we are the leading player in the round.
	 *
	 * @param game
	 * @param round
	 * @param mode
	 * @param possibleCards
	 * @param advisableCards
	 */
	private static void actAsLeadingPlayer(Game game, Round round, Mode mode, Set<Card> possibleCards, Set<Card> advisableCards) {
		final List<Player> players = round.getPlayingOrder().getPlayersInCurrentOrder();
		final Player firstOpponent = players.get(1);
		final Player partner = players.get(2);
		final Player secondOpponent = players.get(3);
		// AUSTRUMPFEN
		if (mode.isTrumpfMode()
				&& !JassHelper.getTrumpfs(firstOpponent.getCards(), mode).isEmpty()
				&& !JassHelper.getTrumpfs(secondOpponent.getCards(), mode).isEmpty())
			advisableCards.addAll(JassHelper.getTrumpfs(possibleCards, mode));

		// BOCK SPIELEN
		// TODO This method is a performance bottleneck!
		final Map<Color, List<Card>> orderedRemainingCards = JassHelper.getCardsStillInGameInStrengthOrder(game);
		final Set<Card> bocks = JassHelper.getBocks(game.getMode(), orderedRemainingCards);
		for (Card bock : bocks)
			if (possibleCards.contains(bock))
				advisableCards.add(bock);

		// PARTNER BEDIENEN
		for (Card bock : bocks)
			if (partner.getCards().contains(bock))
				advisableCards.addAll(JassHelper.getCardsOfSuit(possibleCards, bock.getColor()));

		// ANZIEHEN
		final Set<Card> secondHighestCards = JassHelper.getRemainingCardsBelowBocks(mode, orderedRemainingCards);
		for (Card card : secondHighestCards) {
			if (possibleCards.contains(card)) { // we have a second highest card
				final Set<Card> cardsOfSuit = JassHelper.getCardsOfSuit(possibleCards, card.getColor());
				if (cardsOfSuit.size() > 1) { // and we also have one below
					cardsOfSuit.remove(card);
					advisableCards.addAll(cardsOfSuit); // add all the cards below
				}
			}
		}
	}

}
