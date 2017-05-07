package com.zuehlke.jasschallenge.client.game.strategy.helpers;

import com.zuehlke.jasschallenge.client.game.Game;
import com.zuehlke.jasschallenge.client.game.GameSession;
import com.zuehlke.jasschallenge.client.game.strategy.mcts.src.main.MCTS;
import com.zuehlke.jasschallenge.client.game.strategy.mcts.src.main.Move;
import com.zuehlke.jasschallenge.game.cards.Card;

import java.util.Set;

/**
 * Created by joelniklaus on 05.05.17.
 */
public class MCTSHelper {

	public static Card getCard(Set<Card> availableCards, GameSession session) {
		MCTS mcts = new MCTS();
		mcts.setExplorationConstant(1.4);
		mcts.setOptimisticBias(0);
		mcts.setPessimisticBias(0);
		mcts.setTimeDisplay(true);

		Jass jass = new Jass(availableCards, session);

		Move move = mcts.runMCTS(jass, 10, false);
		return ((CardMove) move).getPlayedCard();
	}


}
