package com.zuehlke.jasschallenge.client.game.strategy.mcts.src;

import java.util.List;

public interface Board {

	/**
	 * Create one copy of the board. It is important that the copies do
	 * not store references to objects shared by other boards unless
	 * those objects are immutable.
	 *
	 * @return
	 */
	Board duplicate(boolean newRandomCards);


	/**
	 * Get a list of all available moves for the current state. MCTS
	 * calls this to know what actions are possible at that point.
	 * <p>
	 * The location parameter indicates from where in the algorithm
	 * the method was called. Can be either treePolicy or playout.
	 *
	 * @param location
	 * @return
	 */
	List<Move> getMoves(CallLocation location);

	/**
	 * Apply the move move to the current state of the board.
	 *
	 * @param move
	 */
	void makeMove(Move move);

	/**
	 * Returns true if the game is over.
	 *
	 * @return
	 */
	boolean gameOver();

	/**
	 * Returns the player ID for the player whose turn is active.
	 * This method is called by the MCTS.
	 *
	 * @return
	 */
	int getCurrentPlayer();

	/**
	 * Returns the number of players.
	 *
	 * @return
	 */
	int getQuantityOfPlayers();

	/**
	 * Returns a score vector.
	 * [1.0, 0.0] indicates a win for player 0.
	 * [0.0, 1.0] indicates a win for player 1
	 * [0.5, 0.5] indicates a draw
	 *
	 * @return score array
	 */
	double[] getScore();

	/**
	 * Returns an array of probability weights
	 * for each move possible on this board. This
	 * is only relevant in board states where
	 * the choice to make is a random choice.
	 *
	 * @return array of weights
	 */
	double[] getMoveWeights();

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
	Move getBestMove();

	/**
	 * returns true if the board uses an estimator for the score
	 * (usually some kind of artificial neural network) and false otherwise
	 *
	 * @return
	 */
	boolean hasScoreEstimator();

	/**
	 * Estimates the score of the current state at the end of the game
	 *
	 * @return
	 */
	double[] estimateScore();
}