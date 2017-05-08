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

	public static Card getCard(Set<Card> availableCards, GameSession session) throws Exception {
		MCTS mcts = new MCTS();
		mcts.setExplorationConstant(1.4);
		mcts.setOptimisticBias(0);
		mcts.setPessimisticBias(0);
		mcts.setTimeDisplay(true);

		Jass jass = null;
		try {
			jass = Jass.jassFactory(availableCards, session);
		} catch (Exception e) {
			System.err.println("Could not clone session or cards");
			e.printStackTrace();
			throw(e);
		}

		Move move = mcts.runMCTS(jass, 10, false);


		return ((CardMove) move).getPlayedCard();
	}


}
