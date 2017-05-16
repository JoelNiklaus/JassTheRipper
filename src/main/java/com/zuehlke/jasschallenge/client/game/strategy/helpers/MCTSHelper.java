package com.zuehlke.jasschallenge.client.game.strategy.helpers;

import com.zuehlke.jasschallenge.client.game.Game;
import com.zuehlke.jasschallenge.client.game.strategy.mcts.CardMove;
import com.zuehlke.jasschallenge.client.game.strategy.mcts.JassBoard;
import com.zuehlke.jasschallenge.client.game.strategy.mcts.JassHeuristic;
import com.zuehlke.jasschallenge.client.game.strategy.mcts.src.FinalSelectionPolicy;
import com.zuehlke.jasschallenge.client.game.strategy.mcts.src.MCTS;
import com.zuehlke.jasschallenge.game.cards.Card;
import org.apache.commons.lang3.SerializationUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by joelniklaus on 05.05.17.
 */
public class MCTSHelper {

	/**
	 * Runs the MCTS and predicts a Card
	 *
	 * @param availableCards
	 * @param game
	 * @param endingTime
	 * @return
	 * @throws Exception
	 */
	public static Card getCard(Set<Card> availableCards, Game game, long endingTime) throws Exception {
		MCTS mcts = new MCTS();
		mcts.setExplorationConstant(1.4);
		mcts.setOptimisticBias(0);
		mcts.setPessimisticBias(0);
		mcts.setTimeDisplay(true);
		//mcts.setMoveSelectionPolicy(FinalSelectionPolicy.maxChild);
		//mcts.setHeuristicFunction(new JassHeuristic());
		//mcts.setPlayoutSelection(new JassPlayoutSelection());

		// TODO Only for debugging!
		//int threads = 1;
		int threads = Runtime.getRuntime().availableProcessors();
		mcts.enableRootParallelisation(threads);

		return predictCard(availableCards, game, mcts, endingTime);
	}


	private static Card predictCard(Set<Card> availableCards, Game game, MCTS mcts, long endingTime) {
		long startTime = System.nanoTime();
		JassBoard jassBoard = new JassBoard(availableCards, game, true);
		long cloningTime = (System.nanoTime() - startTime) / 1000000;
		System.out.println("Cloning time: " + cloningTime + "ms");

		return ((CardMove) mcts.runMCTS_UCT(jassBoard, endingTime, false)).getPlayedCard();
	}

}
