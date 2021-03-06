package to.joeli.jass.client.strategy.mcts.src

interface Board {

    /**
     * Returns the player ID for the player whose turn is active.
     * This method is called by the MCTS.
     *
     * @return
     */
    val currentPlayer: Int

    /**
     * Returns the number of players.
     *
     * @return
     */
    val quantityOfPlayers: Int

    /**
     * Returns a score vector.
     * [1.0, 0.0] indicates a win for player 0.
     * [0.0, 1.0] indicates a win for player 1
     * [0.5, 0.5] indicates a draw
     *
     * @return score array
     */
    val score: DoubleArray

    /**
     * Returns an array of probability weights
     * for each move possible on this board. This
     * is only relevant in board states where
     * the choice to make is a random choice.
     *
     * @return array of weights
     */
    val moveWeights: DoubleArray

    /**
     * Returns the best possible move considering the
     * information from the determinization.
     * This method should only be invoked in the playout phase!
     * Here we assume that we are in a Perfect information game
     * determinization. If we invoke this method outside the playout,
     * it would be cheating, because we are not supposed to
     * have access to the hidden information there!
     *
     * @return
     */
    fun getBestMove(playoutSelectionPolicy: PlayoutSelectionPolicy): Move

    /**
     * Create one copy of the board. It is important that the copies do
     * not store references to objects shared by other boards unless
     * those objects are immutable.
     *
     * @return
     */
    fun duplicate(newRandomCards: Boolean): Board


    /**
     * Get a list of all available moves for the current state. MCTS
     * calls this to know what actions are possible at that point.
     *
     *
     * The location parameter indicates from where in the algorithm
     * the method was called. Can be either TREE_POLICY or PLAYOUT.
     *
     * @param location
     * @return
     */
    fun getMoves(location: CallLocation): List<Move>

    /**
     * Apply the move move to the current state of the board.
     *
     * @param move
     */
    fun makeMove(move: Move)

    /**
     * Returns true if the game is over.
     *
     * @return
     */
    fun gameOver(): Boolean

    /**
     * returns true if the board uses an estimator for the score
     * (usually some kind of artificial neural network) and false otherwise
     *
     * @return
     */
    fun hasScoreEstimator(): Boolean

    /**
     * Estimates the score of the current state at the end of the game
     *
     * @return
     */
    fun estimateScore(): DoubleArray
}