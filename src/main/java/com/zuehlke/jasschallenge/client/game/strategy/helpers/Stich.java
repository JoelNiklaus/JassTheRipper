package com.zuehlke.jasschallenge.client.game.strategy.helpers;

import com.zuehlke.jasschallenge.client.game.Round;
import com.zuehlke.jasschallenge.client.game.strategy.mcts.src.connectFour.ConnectFour;
import com.zuehlke.jasschallenge.client.game.strategy.mcts.src.connectFour.ConnectFourMove;
import com.zuehlke.jasschallenge.client.game.strategy.mcts.src.main.Board;
import com.zuehlke.jasschallenge.client.game.strategy.mcts.src.main.CallLocation;
import com.zuehlke.jasschallenge.client.game.strategy.mcts.src.main.Move;
import com.zuehlke.jasschallenge.game.cards.Card;
import com.zuehlke.jasschallenge.game.mode.Mode;

import java.util.ArrayList;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by joelniklaus on 06.05.17.
 */
public class Stich implements Board {

	public int[][] board; // The actual game board data
	public int currentPlayer = 0;
	public int freeSlots[]; // The number of free slots per column
	public int totalFreeSlots = 6 * 7;
	public int winner = -1;

	public Stich(Set<Card> availableCards, Round round, Mode gameMode, Set<Card> possibleCards) {
		board = new int[7][6];
		freeSlots = new int[7];
		for (int i = 0; i < 7; i++)
			freeSlots[i] = 6;
		winner = -1;
	}

	public static Set<Card> getPossibleCards(Set<Card> availableCards, Round round, Mode gameMode) {
		return availableCards.stream().filter(card -> gameMode.canPlayCard(card, round.getPlayedCards(), round.getRoundColor(), availableCards)).collect(Collectors.toSet());
	}

	@Override
	public Board duplicate() {
		// Copy board data
		return null;
	}

	@Override
	public ArrayList<Move> getMoves(CallLocation location) {
		ArrayList<Move> moves = new ArrayList<Move>();
		for (int i = 0; i < 7; i++) {
			if (freeSlots[i] > 0) {
				ConnectFourMove cfm = new ConnectFourMove(i);
				moves.add(cfm);
			}
		}

		return moves;
	}

	/*
	 * Return true if last move won the game for that player
	 */
	private boolean thisMoveWonTheGame(int x, int y, int pl) {
		int horizontal = 1;
		int vertical = 1;
		int risingDiagonal = 1;
		int sinkingDiagonal = 1;

		horizontal += scanLine(x, y, -1, 0, pl);
		horizontal += scanLine(x, y, 1, 0, pl);
		vertical += scanLine(x, y, 0, 1, pl);
		risingDiagonal += scanLine(x, y, 1, -1, pl);
		risingDiagonal += scanLine(x, y, -1, 1, pl);
		sinkingDiagonal += scanLine(x, y, 1, 1, pl);
		sinkingDiagonal += scanLine(x, y, -1, -1, pl);

		return (horizontal >= 4 || vertical >= 4 ||
				risingDiagonal >= 4 || sinkingDiagonal >= 4);
	}

	/*
	 * Return the number of pieces extending from position x, y in the
	 * direction of xf, yf. Think of the latter as a direction vector.
	 */
	private int scanLine(int x, int y, int xf, int yf, int playerID) {
		int sum = 0;
		for (int i = 1; i < 4; i++) {
			if (x + i * xf > 6 || x + i * xf < 0)
				break;
			if (y + i * yf > 5 || y + i * yf < 0)
				break;

			if (board[x + i * xf][y + i * yf] == playerID + 1)
				sum++;
			else
				break;
		}

		return sum;
	}

	@Override
	public void makeMove(Move m) {

	}

	@Override
	public int getQuantityOfPlayers() {
		return 4;
	}

	@Override
	public int getCurrentPlayer() {
		return currentPlayer;
	}

	public void print() {
		System.out.println("--------------");
		for (int y = 0; y < 6; y++) {
			for (int x = 0; x < 7; x++) {
				if (board[x][y] == 1)
					System.out.print("()");
				else if (board[x][y] == 2)
					System.out.print("<>");
				else if (board[x][y] == 0)
					System.out.print("  ");
				else
					System.out.print("{}");
			}
			System.out.println("");
		}
	}

	@Override
	public boolean gameOver() {
		return winner >= 0;
	}

	@Override
	public double[] getScore() {
		double[] score = new double[2];
		if (winner >= 0)
			score[winner] = 1.0d;
		return score;
	}

	/*
	 * This method is not used by this game, but at least
	 * a function body is required to fulfill the Board
	 * interface contract.
	 */
	public double[] getMoveWeights() {
		return null;
	}

	@Override
	public void bPrint() {
	}
}
