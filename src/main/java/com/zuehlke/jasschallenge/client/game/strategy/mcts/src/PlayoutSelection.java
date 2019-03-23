package com.zuehlke.jasschallenge.client.game.strategy.mcts.src;

/**
 * Use this as a replacement for the conventional
 * playout function during simulations. The idea
 * is to implement a function that takes a game
 * board, applies some random available move from
 * the current state and returns it.
 *
 * @author KGS
 */
public interface PlayoutSelection {
	void process(Board board);
}