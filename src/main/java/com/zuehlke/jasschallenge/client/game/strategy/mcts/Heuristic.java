package com.zuehlke.jasschallenge.client.game.strategy.helpers;

import com.zuehlke.jasschallenge.client.game.strategy.mcts.Board;
import com.zuehlke.jasschallenge.client.game.strategy.mcts.HeuristicFunction;

/**
 * Created by joelniklaus on 10.05.17.
 */
public class Heuristic implements HeuristicFunction {

	@Override
	public double h(Board board) {
		return 0;
	}

}