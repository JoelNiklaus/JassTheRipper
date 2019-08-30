package to.joeli.jass.client.strategy.mcts.src

/**
 * Create a class implementing this interface and instantiate
 * it. Pass the instance to the MCTS instance using the
 * [setHeuristicFunction][MCTS.setHeuristicFunction] method.
 * Add domain knowledge to make the node selection process better.
 *
 * @author Ganryu
 */
interface HeuristicFunction {
    fun heuristicFunction(board: Board): Double
}
