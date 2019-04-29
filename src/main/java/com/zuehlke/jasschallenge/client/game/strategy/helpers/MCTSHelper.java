package com.zuehlke.jasschallenge.client.game.strategy.helpers;

import com.zuehlke.jasschallenge.client.game.GameSession;
import com.zuehlke.jasschallenge.client.game.strategy.RunMode;
import com.zuehlke.jasschallenge.client.game.strategy.StrengthLevel;
import com.zuehlke.jasschallenge.client.game.strategy.exceptions.MCTSException;
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

	private final int numDeterminizationsFactor; // determines how many determinizations we create
	private final RunMode runMode;
	private static final int BUFFER_TIME_MILLIS = 25; // INFO Makes sure, that the bot really finishes before the thinking time is up.


	private final MCTS mcts = new MCTS();

	public static final Logger logger = LoggerFactory.getLogger(MCTSHelper.class);

	public MCTSHelper(int numDeterminizationsFactor, RunMode runMode) {
		this.numDeterminizationsFactor = numDeterminizationsFactor;
		this.runMode = runMode;

		// TODO tune parameters
		mcts.setExplorationConstant(1.4);
		mcts.setOptimisticBias(0);
		mcts.setPessimisticBias(0);
		//mcts.setMoveSelectionPolicy(FinalSelectionPolicy.maxChild);
		//mcts.setHeuristicFunction(new JassHeuristic());
		//mcts.setPlayoutSelection(new JassPlayoutSelection());

		// if we run by runs we want the threadPool to only have as many threads as there are cores available for maximal efficiency (no unnecessary scheduling overhead)
		if (runMode == RunMode.RUNS)
			mcts.enableRootParallelisation(Runtime.getRuntime().availableProcessors());
		// if we run by time we want the threadpool to have enough threads to have all determinizations running at the same time
		if (runMode == RunMode.TIME)
			mcts.enableRootParallelisation(10 * numDeterminizationsFactor);
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
	 * Chooses a card by running the MCTS method.
	 *
	 * @param availableCards
	 * @param gameSession
	 * @param strengthLevel
	 * @return
	 */
	public Move predictMove(Set<Card> availableCards, GameSession gameSession, boolean isChoosingTrumpf, boolean shifted, StrengthLevel strengthLevel) throws MCTSException {
		Board jassBoard;
		if (isChoosingTrumpf)
			jassBoard = JassBoard.constructTrumpfSelectionJassBoard(availableCards, gameSession, shifted);
		else
			jassBoard = JassBoard.constructCardSelectionJassBoard(availableCards, gameSession.getCurrentGame());
		int roundMultiplier = 10;
		if (!isChoosingTrumpf)
			roundMultiplier = (9 - gameSession.getCurrentRound().getRoundNumber());
		int numDeterminizations = roundMultiplier * numDeterminizationsFactor;
		if (runMode == RunMode.RUNS)
			return mcts.runForRuns(jassBoard, numDeterminizations, strengthLevel.getNumRuns());
		else if (runMode == RunMode.TIME)
			return mcts.runForTime(jassBoard, numDeterminizations, System.currentTimeMillis() + strengthLevel.getMaxThinkingTime() - BUFFER_TIME_MILLIS);
		return null;
	}
}
