package to.joeli.jass.client.game.strategy.mcts.src;

/**
 * Create a class implementing this interface and instantiate
 * it. Pass the instance to the MCTS instance using the
 * {@link MCTS#setHeuristicFunction(HeuristicFunction heuristicFunction) setHeuristicFunction} method.
 *
 * @author Ganryu
 *
 */
public interface HeuristicFunction {
	double heuristicFunction(Board board);
}
