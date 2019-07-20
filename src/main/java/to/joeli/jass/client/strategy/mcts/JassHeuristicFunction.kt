package to.joeli.jass.client.strategy.mcts

import to.joeli.jass.client.strategy.mcts.src.Board
import to.joeli.jass.client.strategy.mcts.src.HeuristicFunction

/**
 * Created by joelniklaus on 10.05.17.
 */
class JassHeuristicFunction : HeuristicFunction {

    override fun heuristicFunction(board: Board): Double {
        return 0.0
    }
}
