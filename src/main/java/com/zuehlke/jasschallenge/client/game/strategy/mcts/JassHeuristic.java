package com.zuehlke.jasschallenge.client.game.strategy.mcts;

import com.zuehlke.jasschallenge.client.game.strategy.mcts.src.Board;
import com.zuehlke.jasschallenge.client.game.strategy.mcts.src.HeuristicFunction;

/**
 * Created by joelniklaus on 10.05.17.
 */
public class JassHeuristic implements HeuristicFunction {

	@Override
	public double h(Board board) {
		return 0;
	}

}