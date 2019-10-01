package to.joeli.jass.client.strategy.mcts

import to.joeli.jass.client.game.Game
import to.joeli.jass.client.strategy.helpers.PerfectInformationGameSolver
import to.joeli.jass.client.strategy.mcts.src.Board
import to.joeli.jass.client.strategy.mcts.src.Move
import to.joeli.jass.client.strategy.mcts.src.PlayoutSelectionPolicy

/**
 * Created by joelniklaus on 10.05.17.
 */
class HeavyJassPlayoutSelectionPolicy : PlayoutSelectionPolicy {
    override fun getBestMove(board: Board): Move {
        return board.getBestMove(this)
    }

    override fun runPlayout(game: Game): CardMove {
        return PerfectInformationGameSolver.runHeavyPlayout(game)
    }

    override fun toString(): String {
        return "heavy rule based playout"
    }
}
