package com.zuehlke.jasschallenge.client.game.strategy.mcts.src.connectFour;

import com.zuehlke.jasschallenge.client.game.strategy.mcts.src.main.Move;

/**
 * Moves are simple to manage for Connect Four. We
 * store in each move only the row of the board
 * where this piece will be inserted. The board
 * itself is responsible for implementing
 * the function that actually performs this move.
 */
public class ConnectFourMove implements Move {
	int row;
	
	public ConnectFourMove(int row){
		this.row = row;
	}
	
	public String toString(){
		StringBuilder strb = new StringBuilder();
		strb.append(row);
		return strb.toString();
	}
}
