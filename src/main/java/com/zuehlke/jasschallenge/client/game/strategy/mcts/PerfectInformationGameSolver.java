package com.zuehlke.jasschallenge.client.game.strategy.mcts;

import com.zuehlke.jasschallenge.client.game.Game;
import com.zuehlke.jasschallenge.client.game.Player;
import com.zuehlke.jasschallenge.client.game.strategy.helpers.CardSelectionHelper;
import com.zuehlke.jasschallenge.game.cards.Card;

import java.util.*;

/**
 * At the moment this class only selects a random move.
 * But the goal would be to use rules for good play in a perfect information game scenario.
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
