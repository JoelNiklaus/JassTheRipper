package to.joeli.jass.client.strategy.helpers;

import to.joeli.jass.client.game.Game;
import to.joeli.jass.client.game.Player;
import to.joeli.jass.client.strategy.mcts.CardMove;
import to.joeli.jass.game.cards.Card;

import java.util.EnumSet;
import java.util.Set;

/**
 * Uses rules for good play in a perfect information game scenario to find a move.
 * Using a rule based bot like for example the challenge bot would be an idea too.
 */
public class PerfectInformationGameSolver {

	private PerfectInformationGameSolver() {
	}

	public static CardMove getMove(Game game) {
		final Player player = game.getCurrentPlayer();

		Set<Card> possibleCards = CardSelectionHelper.getCardsPossibleToPlay(EnumSet.copyOf(player.getCards()), game);

		final Set<Card> refinedCards = CardSelectionHelper.refineCardsWithJassKnowledge(possibleCards, game);

		return new CardMove(player, CardSelectionHelper.chooseRandomCard(refinedCards));
	}

}
