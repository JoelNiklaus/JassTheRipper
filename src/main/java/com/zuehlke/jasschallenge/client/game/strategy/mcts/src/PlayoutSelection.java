package com.zuehlke.jasschallenge.client.game.strategy.mcts.src;

/**
 * Use this as a replacement for the conventional
 * playout function during simulations. The idea
 * is to implement a function that takes a game
 * board and adds for example domain knowledge
 * to enhance the quality of the playout
 * in comparison to the random playout.
 *
 * @author joelniklaus
 */
public interface PlayoutSelection {
	Move getBestMove(Board board);
}