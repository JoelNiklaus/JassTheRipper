package com.zuehlke.jasschallenge.client.game.strategy.helpers;

import com.zuehlke.jasschallenge.client.game.Game;
import com.zuehlke.jasschallenge.client.game.GameSession;
import com.zuehlke.jasschallenge.client.game.strategy.mcts.src.main.MCTS;
import com.zuehlke.jasschallenge.client.game.strategy.mcts.src.main.Move;
import com.zuehlke.jasschallenge.game.cards.Card;
import org.apache.commons.lang3.SerializationUtils;

import java.util.Set;

/**
 * Created by joelniklaus on 05.05.17.
 */
public class MCTSHelper {

	public static Card getCard(Set<Card> availableCards, Game game) throws Exception {
		MCTS mcts = new MCTS();
		mcts.setExplorationConstant(1.4);
		mcts.setOptimisticBias(0);
		mcts.setPessimisticBias(0);
		mcts.setTimeDisplay(true);

		JassBoard jass;
		try {
			jass = JassBoard.jassFactory(availableCards, SerializationUtils.clone(game));
		} catch (Exception e) {
			System.err.println("Could not clone session or cards");
			e.printStackTrace();
			throw(e);
		}


		// TODO Nach 480 ms abbrechen
		int maxComputationTime = 480;

		Move move = mcts.runMCTS(jass, false, 400);


		return ((CardMove) move).getPlayedCard();
	}


}
