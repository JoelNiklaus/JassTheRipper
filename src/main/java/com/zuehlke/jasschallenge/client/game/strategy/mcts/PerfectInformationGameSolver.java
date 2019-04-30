package com.zuehlke.jasschallenge.client.game.strategy.mcts;

import com.zuehlke.jasschallenge.client.game.Game;
import com.zuehlke.jasschallenge.client.game.Player;
import com.zuehlke.jasschallenge.client.game.strategy.helpers.CardSelectionHelper;
import com.zuehlke.jasschallenge.client.game.strategy.mcts.src.Move;
import com.zuehlke.jasschallenge.game.cards.Card;

import java.util.*;

public class PerfectInformationGameSolver {
	public static Move getMove(Game game) {
		final Player player = game.getCurrentPlayer();

		Set<Card> possibleCards = CardSelectionHelper.getCardsPossibleToPlay(EnumSet.copyOf(player.getCards()), game);

		Card card = new ArrayList<>(possibleCards).get(new Random().nextInt(possibleCards.size()));

		return new CardMove(player, card);
	}
}
