package to.joeli.jass.client.strategy.helpers

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import to.joeli.jass.client.game.GameSession
import to.joeli.jass.client.strategy.config.MCTSConfig
import to.joeli.jass.client.strategy.config.RunMode
import to.joeli.jass.client.strategy.config.StrengthLevel
import to.joeli.jass.client.strategy.exceptions.MCTSException
import to.joeli.jass.client.strategy.mcts.JassBoard
import to.joeli.jass.client.strategy.mcts.src.Board
import to.joeli.jass.client.strategy.mcts.src.MCTS
import to.joeli.jass.client.strategy.mcts.src.Move
import to.joeli.jass.client.strategy.training.networks.CardsEstimator
import to.joeli.jass.client.strategy.training.networks.ScoreEstimator
import to.joeli.jass.game.cards.Card

/**
 * Created by joelniklaus on 05.05.17.
 */
class MCTSHelper(private val mctsConfig: MCTSConfig) {

    private val mcts = MCTS()

    /**
     * Checks whether the thread pool in the mcts object has been shut down.
     *
     * @return
     */
    val isShutDown: Boolean
        get() = mcts.isShutDown

    init {
        mcts.setRandom(mctsConfig.seed)
        mcts.setScoreBoundsUsed(mctsConfig.scoreBoundsUsed)
        mcts.setExplorationConstant(mctsConfig.explorationConstant)
        mcts.setOptimisticBias(mctsConfig.optimisticBias)
        mcts.setPessimisticBias(mctsConfig.pessimisticBias)
        mcts.setNumPlayouts(mctsConfig.numPlayouts)
        mcts.setFinalSelectionPolicy(mctsConfig.finalSelectionPolicy)
        mcts.setHeuristicFunction(mctsConfig.heuristicFunction)
        mcts.setPlayoutSelectionPolicy(mctsConfig.playoutSelectionPolicy)

        // if we run by runs we want the threadPool to only have as many threads as there are cores available for maximal efficiency (no unnecessary scheduling overhead)
        if (mctsConfig.runMode === RunMode.RUNS)
            mcts.enableRootParallelisation(Runtime.getRuntime().availableProcessors())
        // if we run by time we want the threadPool to have enough threads to have all determinizations running at the same time
        if (mctsConfig.runMode === RunMode.TIME)
            mcts.enableRootParallelisation(ROUND_MULTIPLIER * mctsConfig.trumpfStrengthLevel.numDeterminizationsFactor) // NOTE: It creates A LOT of threads here now!
    }

    /**
     * Shuts down the thread pool in the mcts object. Has to be called as soon as it is not used anymore!
     */
    fun shutDown() {
        mcts.shutDown()
    }

    /**
     * Chooses a card by running the MCTS method.
     *
     * @param availableCards
     * @param gameSession
     * @return
     */
    @Throws(MCTSException::class)
    fun predictMove(availableCards: Set<Card>, gameSession: GameSession, isChoosingTrumpf: Boolean, shifted: Boolean): Move? {
        val jassBoard: Board
        val scoreEstimator: ScoreEstimator?
        val cardsEstimator: CardsEstimator?
        val strengthLevel: StrengthLevel
        if (isChoosingTrumpf) {
            strengthLevel = mctsConfig.trumpfStrengthLevel
            scoreEstimator = gameSession.trumpfSelectingPlayer.scoreEstimator
            cardsEstimator = gameSession.trumpfSelectingPlayer.cardsEstimator
            jassBoard = JassBoard.constructTrumpfSelectionJassBoard(availableCards, gameSession, shifted, mctsConfig.cheating, scoreEstimator, cardsEstimator)
        } else {
            strengthLevel = mctsConfig.cardStrengthLevel
            scoreEstimator = gameSession.currentGame.currentPlayer.scoreEstimator
            cardsEstimator = gameSession.currentGame.currentPlayer.cardsEstimator
            jassBoard = JassBoard.constructCardSelectionJassBoard(availableCards, gameSession.currentGame, mctsConfig.cheating, scoreEstimator, cardsEstimator)
        }

        var numDeterminizations = computeNumDeterminizations(gameSession, isChoosingTrumpf, strengthLevel.numDeterminizationsFactor)

        var numRuns = strengthLevel.numRuns
        if (scoreEstimator != null) {
            logger.info("Using a score estimator network to determine the score")
            if (mctsConfig.runMode === RunMode.RUNS) {
                numRuns /= 10 // NOTE: Less runs when using network because it should be superior to random playout
                logger.info("Running only {} runs per determinization", numRuns)
            } else if (mctsConfig.runMode === RunMode.TIME) {
                numDeterminizations *= 2 // NOTE: Can do more determinizations in the same time because it should be faster than random playout
                logger.info("Running even {} determinizations", numDeterminizations)
            }
        } else {
            if (mctsConfig.playoutSelectionPolicy != null)
                logger.info("Using a {} to determine the score", mctsConfig.playoutSelectionPolicy)
            else
                logger.info("Using a random playout to determine the score")
        }

        if (cardsEstimator != null) {
            logger.info("Using a cards estimator network to better estimate the hidden cards")
        }

        if (mctsConfig.runMode === RunMode.RUNS) {
            if (mctsConfig.cardStrengthLevel == StrengthLevel.HSLU_SERVER) // small hack to make it to 1000000 simulations every time
                numRuns = 100000L / numDeterminizations
            return mcts.runForRuns(jassBoard, numDeterminizations, numRuns)
        } else if (mctsConfig.runMode === RunMode.TIME)
            return mcts.runForTime(jassBoard, numDeterminizations, System.currentTimeMillis() + strengthLevel.maxThinkingTime - BUFFER_TIME_MILLIS)
        return null
    }

    private fun computeNumDeterminizations(gameSession: GameSession, isChoosingTrumpf: Boolean, numDeterminizationsFactor: Int): Int {
        return if (!isChoosingTrumpf) (9 - gameSession.currentRound!!.roundNumber) * numDeterminizationsFactor else ROUND_MULTIPLIER * numDeterminizationsFactor
    }

    companion object {

        private val BUFFER_TIME_MILLIS = 10 // INFO Makes sure, that the bot really finishes before the thinking time is up.
        private val ROUND_MULTIPLIER = 10

        val logger: Logger = LoggerFactory.getLogger(MCTSHelper::class.java)
    }
}
