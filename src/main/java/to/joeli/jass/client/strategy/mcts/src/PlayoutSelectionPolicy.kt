package to.joeli.jass.client.strategy.mcts.src

import to.joeli.jass.client.game.Game

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
interface PlayoutSelectionPolicy {
    fun getBestMove(board: Board): Move

    fun runPlayout(game: Game): Move
}