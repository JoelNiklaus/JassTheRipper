package com.zuehlke.jasschallenge.client.game.strategy.helpers;

import com.zuehlke.jasschallenge.client.game.GameSession;
import com.zuehlke.jasschallenge.client.game.strategy.exceptions.MCTSException;
import com.zuehlke.jasschallenge.client.game.strategy.mcts.CardMove;
import com.zuehlke.jasschallenge.client.game.strategy.mcts.JassBoard;
import com.zuehlke.jasschallenge.client.game.strategy.mcts.src.Board;
import com.zuehlke.jasschallenge.client.game.strategy.mcts.src.MCTS;
import com.zuehlke.jasschallenge.client.game.strategy.mcts.src.Move;
import com.zuehlke.jasschallenge.game.cards.Card;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by joelniklaus on 05.05.17.
 */
public class MCTSHelper {

	public static final Logger logger = LoggerFactory.getLogger(MCTSHelper.class);

	private final MCTS mcts = new MCTS();

	public MCTSHelper(int numThreads) {
		// TODO tune parameters
		mcts.setExplorationConstant(1.4);
		mcts.setOptimisticBias(0);
		mcts.setPessimisticBias(0);
		mcts.setTimeDisplay(true);
		//mcts.setMoveSelectionPolicy(FinalSelectionPolicy.maxChild);
		//mcts.setHeuristicFunction(new JassHeuristic());
		//mcts.setPlayoutSelection(new JassPlayoutSelection());


		if (numThreads > 1)
			mcts.enableRootParallelisation(numThreads);
	}

	/**
	 * Shuts down the thread pool in the mcts object. Has to be called as soon as it is not used anymore!
	 */
	public void shutDown() {
		mcts.shutDown();
	}

	/**
	 * Checks whether the threadpool in the mcts object has been shut down.
	 *
	 * @return
	 */
	public boolean isShutDown() {
		return mcts.isShutDown();
	}

	/**
	 * Sets the MCTS parameters, runs it and predicts a Card
	 *
	 * @param availableCards
	 * @param gameSession
	 * @param endingTime
	 * @return
	 * @throws Exception
	 */
	public Move getMove(Set<Card> availableCards, GameSession gameSession, boolean isChoosingTrumpf, boolean shifted, long endingTime) throws MCTSException {
		if (!isChoosingTrumpf) {
			// Fast track: If Jass Knowledge only suggests one sensible option -> return this one.
			// But, we do not want to trust this too much but rather rely on the MCTS. It can be included there too.
			Set<Card> possibleCards = CardSelectionHelper.getPossibleCards(availableCards, gameSession.getCurrentGame());
			possibleCards = CardSelectionHelper.refineCardsWithJassKnowledge(possibleCards, gameSession.getCurrentGame());
			if (possibleCards.size() == 1) {
				logger.info("Based on expert Jass Knowledge there is only one sensible card available now.");
				Card card = (Card) possibleCards.toArray()[0];
				return new CardMove(gameSession.getCurrentPlayer(), card);
			}
		}

		return runPrediction(availableCards, gameSession, isChoosingTrumpf, shifted, endingTime);
	}

	/**
	 * Runs the prediction of the card. Runs differently whether or not parallelisation is enabled.
	 *
	 * @param availableCards
	 * @param gameSession
	 * @param endingTime
	 * @return
	 */
	private Move runPrediction(Set<Card> availableCards, GameSession gameSession, boolean isChoosingTrumpf, boolean shifted, long endingTime) throws MCTSException {
		// Can do multithreading now -> Much faster
		// Only do this when multithreading disabled


		if (!mcts.isParallelisationEnabled()) {
			long maxComputationTime = endingTime - System.currentTimeMillis();
			int numberOfMCTSRuns = 4;

			long timePerRun = maxComputationTime / numberOfMCTSRuns;

			HashMap<Move, Integer> numberOfSelections = new HashMap<>();
			for (int i = 0; i < numberOfMCTSRuns; i++) {
				Move move = predictMove(availableCards, gameSession, isChoosingTrumpf, shifted, System.currentTimeMillis() + timePerRun);
				int number = 1;
				if (numberOfSelections.containsKey(move)) {
					number += numberOfSelections.get(move);
				}
				numberOfSelections.put(move, number);
			}
			Move move = numberOfSelections.entrySet().stream()
					.sorted(Map.Entry.comparingByValue(Collections.reverseOrder()))
					.findFirst().get().getKey();
			return move;
		}


		return predictMove(availableCards, gameSession, isChoosingTrumpf, shifted, endingTime);
	}

	/**
	 * Chooses a card by running the mcts method.
	 *
	 * @param availableCards
	 * @param gameSession
	 * @param endingTime
	 * @return
	 */
	private Move predictMove(Set<Card> availableCards, GameSession gameSession, boolean isChoosingTrumpf, boolean shifted, long endingTime) throws MCTSException {
		Board jassBoard = new JassBoard(availableCards, gameSession, true, isChoosingTrumpf, shifted);
		return mcts.runMCTS_UCT(jassBoard, endingTime, false);
	}

}
