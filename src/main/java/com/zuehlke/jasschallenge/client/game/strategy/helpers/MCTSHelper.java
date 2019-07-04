package com.zuehlke.jasschallenge.client.game.strategy.helpers;

import com.zuehlke.jasschallenge.client.game.GameSession;
import com.zuehlke.jasschallenge.client.game.strategy.config.MCTSConfig;
import com.zuehlke.jasschallenge.client.game.strategy.config.RunMode;
import com.zuehlke.jasschallenge.client.game.strategy.config.StrengthLevel;
import com.zuehlke.jasschallenge.client.game.strategy.exceptions.MCTSException;
import com.zuehlke.jasschallenge.client.game.strategy.mcts.JassBoard;
import com.zuehlke.jasschallenge.client.game.strategy.training.CardsEstimator;
import com.zuehlke.jasschallenge.client.game.strategy.mcts.src.Board;
import com.zuehlke.jasschallenge.client.game.strategy.mcts.src.MCTS;
import com.zuehlke.jasschallenge.client.game.strategy.mcts.src.Move;
import com.zuehlke.jasschallenge.client.game.strategy.training.ScoreEstimator;
import com.zuehlke.jasschallenge.game.cards.Card;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * Created by joelniklaus on 05.05.17.
 */
public class MCTSHelper {

	private final MCTSConfig mctsConfig;

	private static final int BUFFER_TIME_MILLIS = 10; // INFO Makes sure, that the bot really finishes before the thinking time is up.
	private static final int ROUND_MULTIPLIER = 10;

	private final MCTS mcts = new MCTS();

	public static final Logger logger = LoggerFactory.getLogger(MCTSHelper.class);

	public MCTSHelper(MCTSConfig mctsConfig) {
		this.mctsConfig = mctsConfig;

		// TODO tune parameters
		mcts.setRandom(mctsConfig.getSeed());
		mcts.setScoreBoundsUsed(mctsConfig.getScoreBoundsUsed());
		mcts.setExplorationConstant(mctsConfig.getExplorationConstant());
		mcts.setOptimisticBias(mctsConfig.getOptimisticBias());
		mcts.setPessimisticBias(mctsConfig.getPessimisticBias());
		mcts.setMoveSelectionPolicy(mctsConfig.getFinalSelectionPolicy());
		mcts.setHeuristicFunction(mctsConfig.getHeuristicFunction());
		mcts.setPlayoutSelection(mctsConfig.getPlayoutPolicy());

		// if we run by runs we want the threadPool to only have as many threads as there are cores available for maximal efficiency (no unnecessary scheduling overhead)
		if (mctsConfig.getRunMode() == RunMode.RUNS)
			mcts.enableRootParallelisation(Runtime.getRuntime().availableProcessors());
		// if we run by time we want the threadPool to have enough threads to have all determinizations running at the same time
		if (mctsConfig.getRunMode() == RunMode.TIME)
			mcts.enableRootParallelisation(ROUND_MULTIPLIER * mctsConfig.getTrumpfStrengthLevel().getNumDeterminizationsFactor()); // NOTE: It creates A LOT of threads here now!
	}

	/**
	 * Shuts down the thread pool in the mcts object. Has to be called as soon as it is not used anymore!
	 */
	public void shutDown() {
		mcts.shutDown();
	}

	/**
	 * Checks whether the thread pool in the mcts object has been shut down.
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
	 * @return
	 */
	public Move predictMove(Set<Card> availableCards, GameSession gameSession, boolean isChoosingTrumpf, boolean shifted) throws MCTSException {
		Board jassBoard;
		ScoreEstimator scoreEstimator;
		CardsEstimator cardsEstimator;
		StrengthLevel strengthLevel;
		if (isChoosingTrumpf) {
			strengthLevel = mctsConfig.getTrumpfStrengthLevel();
			scoreEstimator = gameSession.getTrumpfSelectingPlayer().getScoreEstimator();
			cardsEstimator = gameSession.getTrumpfSelectingPlayer().getCardsEstimator();
			jassBoard = JassBoard.constructTrumpfSelectionJassBoard(availableCards, gameSession, shifted, scoreEstimator, cardsEstimator);
		} else {
			strengthLevel = mctsConfig.getCardStrengthLevel();
			scoreEstimator = gameSession.getCurrentGame().getCurrentPlayer().getScoreEstimator();
			cardsEstimator = gameSession.getCurrentGame().getCurrentPlayer().getCardsEstimator();
			jassBoard = JassBoard.constructCardSelectionJassBoard(availableCards, gameSession.getCurrentGame(), scoreEstimator, cardsEstimator);
		}

		int numDeterminizations = computeNumDeterminizations(gameSession, isChoosingTrumpf, strengthLevel.getNumDeterminizationsFactor());

		long numRuns = strengthLevel.getNumRuns();
		if (scoreEstimator != null) {
			logger.info("Using a score estimator network to determine the score");
			if (mctsConfig.getRunMode() == RunMode.RUNS) {
				numRuns /= 10; // NOTE: Less runs when using network because it should be superior to random playout
				logger.info("Running only {} runs per determinization", numRuns);
			} else if (mctsConfig.getRunMode() == RunMode.TIME) {
				numDeterminizations *= 2; // NOTE: Can do more determinizations in the same time because it should be faster than random playout
				logger.info("Running even {} determinizations", numDeterminizations);
			}
		} else
			logger.info("Using a random playout to determine the score");

		if (mctsConfig.getRunMode() == RunMode.RUNS)
			return mcts.runForRuns(jassBoard, numDeterminizations, numRuns);
		else if (mctsConfig.getRunMode() == RunMode.TIME)
			return mcts.runForTime(jassBoard, numDeterminizations, System.currentTimeMillis() + strengthLevel.getMaxThinkingTime() - BUFFER_TIME_MILLIS);
		return null;
	}

	private int computeNumDeterminizations(GameSession gameSession, boolean isChoosingTrumpf, int numDeterminizationsFactor) {
		if (!isChoosingTrumpf)
			return (9 - gameSession.getCurrentRound().getRoundNumber()) * numDeterminizationsFactor;
		return ROUND_MULTIPLIER * numDeterminizationsFactor;
	}
}
