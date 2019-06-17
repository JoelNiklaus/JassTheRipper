package com.zuehlke.jasschallenge.client.game.strategy.mcts.src;

public class BoardNodePair {
	private Board board;
	private Node node;

	public BoardNodePair(Board board, Node node) {
		this.board = board;
		this.node = node;
	}

	public Board getBoard() {
		return board;
	}

	public Node getNode() {
		return node;
	}

}
