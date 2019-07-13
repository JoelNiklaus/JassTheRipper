package to.joeli.jass.client.strategy.helpers;

import to.joeli.jass.client.game.Game;
import to.joeli.jass.client.game.Player;
import to.joeli.jass.client.strategy.mcts.CardMove;
import to.joeli.jass.game.cards.Card;

import java.util.*;

/**
 * IMPORTANT: At the moment this class only selects a random move.
 * But the goal would be to use rules for good play in a perfect information game scenario.
 * Using a rule based bot like for example the challenge bot would be an idea hers.
 */
public class PerfectInformationGameSolver {

	private PerfectInformationGameSolver() {
	}

	public static CardMove getMove(Game game) {
		final Player player = game.getCurrentPlayer();

		Set<Card> possibleCards = CardSelectionHelper.getCardsPossibleToPlay(EnumSet.copyOf(player.getCards()), game);

		Card card = new ArrayList<>(possibleCards).get(new Random().nextInt(possibleCards.size()));

		return new CardMove(player, card);
	}
}
