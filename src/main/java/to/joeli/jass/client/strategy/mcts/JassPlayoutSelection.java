package to.joeli.jass.client.strategy.mcts;

import to.joeli.jass.client.strategy.mcts.src.Board;
import to.joeli.jass.client.strategy.mcts.src.Move;
import to.joeli.jass.client.strategy.mcts.src.PlayoutSelection;

/**
 * Created by joelniklaus on 10.05.17.
 */
public class JassPlayoutSelection implements PlayoutSelection {
	@Override
	public Move getBestMove(Board board) {
		return board.getBestMove();
	}
}
