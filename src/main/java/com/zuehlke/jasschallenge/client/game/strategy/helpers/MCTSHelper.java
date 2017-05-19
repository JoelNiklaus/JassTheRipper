package com.zuehlke.jasschallenge.client.game.strategy.helpers;

import com.zuehlke.jasschallenge.client.game.Game;
import com.zuehlke.jasschallenge.client.game.strategy.mcts.CardMove;
import com.zuehlke.jasschallenge.client.game.strategy.mcts.JassBoard;
import com.zuehlke.jasschallenge.client.game.strategy.mcts.src.MCTS;
import com.zuehlke.jasschallenge.game.cards.Card;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by joelniklaus on 05.05.17.
 */
public class MCTSHelper {

	private static final int NUMBER_OF_THREADS = Runtime.getRuntime().availableProcessors() * 3;

	/**
	 * Runs the MCTS and predicts a Card
	 *
	 * @param availableCards
	 * @param game
	 * @param endingTime
	 * @return
	 * @throws Exception
	 */
	public static Card getCard(Set<Card> availableCards, Game game, long endingTime, boolean parallelisation) throws Exception {
		MCTS mcts = new MCTS();
		mcts.setExplorationConstant(1.4);
		mcts.setOptimisticBias(0);
		mcts.setPessimisticBias(0);
		mcts.setTimeDisplay(true);
		//mcts.setMoveSelectionPolicy(FinalSelectionPolicy.maxChild);
		//mcts.setHeuristicFunction(new JassHeuristic());
		//mcts.setPlayoutSelection(new JassPlayoutSelection());


		if (parallelisation)
			mcts.enableRootParallelisation(NUMBER_OF_THREADS);

		return runPrediction(availableCards, game, mcts, endingTime);
	}

	private static Card runPrediction(Set<Card> availableCards, Game game, MCTS mcts, long endingTime) throws Exception {
		// Can do multithreading now -> Much faster
		// Only do this when multithreading disabled
		if (!mcts.isParallelisationEnabled()) {
			long maxComputationTime = endingTime - System.currentTimeMillis();
			int numberOfMCTSRuns = 4;

			long timePerRun = maxComputationTime / numberOfMCTSRuns;

			HashMap<Card, Integer> numberOfSelections = new HashMap<>();
			for (int i = 0; i < numberOfMCTSRuns; i++) {
				Card card = predictCard(availableCards, game, mcts, timePerRun);
				int number = 1;
				if (numberOfSelections.containsKey(card)) {
					number += numberOfSelections.get(card);
				}
				numberOfSelections.put(card, number);
			}
			Card card = numberOfSelections.entrySet().stream().sorted(Map.Entry.comparingByValue(Collections.reverseOrder())).findFirst().get().getKey();
			return card;
		}


		return predictCard(availableCards, game, mcts, endingTime);
	}


	private static Card predictCard(Set<Card> availableCards, Game game, MCTS mcts, long endingTime) throws Exception {
		final long startTime = System.currentTimeMillis();
		JassBoard jassBoard = new JassBoard(availableCards, game, true);
		final long cloningTime = (System.currentTimeMillis() - startTime);
		System.out.println("Cloning time: " + cloningTime + "ms");

		return ((CardMove) mcts.runMCTS_UCT(jassBoard, endingTime, false)).getPlayedCard();
	}

}
