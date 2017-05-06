package com.zuehlke.jasschallenge.client.game.strategy.mcts.src.ticTacToe;

import com.zuehlke.jasschallenge.client.game.strategy.mcts.src.main.Move;

public class TicTacToeMove implements Move {
	int x;
	int y;
	
	TicTacToeMove(int x, int y){
		this.x = x;
		this.y = y;
	}
}
