package com.zuehlke.jasschallenge.client.game.strategy.helpers;

import com.zuehlke.jasschallenge.client.game.Game;
import com.zuehlke.jasschallenge.client.game.strategy.mcts.CardMove;
import com.zuehlke.jasschallenge.client.game.strategy.mcts.JassBoard;
import com.zuehlke.jasschallenge.client.game.strategy.mcts.src.MCTS;
import com.zuehlke.jasschallenge.game.cards.Card;

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


		final int threads = 10; // The more the merrier ;)
		//final int threads = Runtime.getRuntime().availableProcessors();
		mcts.enableRootParallelisation(threads);


		// TODO add this when multithreading disabled
		/*
		Can do multithreading now -> Much faster
		Only do this when multithreading disabled

		int maxComputationTime = 440;
		int numberOfMCTSRuns = 4;

		int timePerRun = maxComputationTime / numberOfMCTSRuns;

		HashMap<Card, Integer> numberOfSelections = new HashMap<>();
		for (int i = 0; i < numberOfMCTSRuns; i++) {
			Card card = predictCard(availableCards, game, mcts, timePerRun);
			int number = 1;
			if (numberOfSelections.containsKey(card)) {
				number += numberOfSelections.get(card);
			}
			numberOfSelections.put(card, number);
		}
		Card card = numberOfSelections.entrySet().stream().sorted(Map.Entry.comparingByValue()).findFirst().get().getKey();
		*/


		return predictCard(availableCards, game, mcts, endingTime);
	}


	private static Card predictCard(Set<Card> availableCards, Game game, MCTS mcts, long endingTime) {
		final long startTime = System.nanoTime();
		JassBoard jassBoard = new JassBoard(availableCards, game, true);
		final long cloningTime = (System.nanoTime() - startTime) / 1000000;
		System.out.println("Cloning time: " + cloningTime + "ms");

		return ((CardMove) mcts.runMCTS_UCT(jassBoard, endingTime, false)).getPlayedCard();
	}

}
