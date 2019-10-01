package to.joeli.jass.client.strategy.mcts.src;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Node {
	private double[] scores;
	private double games;
	private Move move;
	private List<Node> unvisitedChildren;
	private List<Node> children;
	private Set<Integer> rVisited;
	private Node parent;
	private int player;
	private double[] pess;
	private double[] opti;
	private boolean pruned;
	private boolean valid = true;

	/**
	 * This creates the root node
	 *
	 * @param board
	 */
	public Node(Board board) {
		children = new ArrayList<>();
		player = board.getCurrentPlayer();
		scores = new double[board.getQuantityOfPlayers()];
		pess = new double[board.getQuantityOfPlayers()];
		opti = new double[board.getQuantityOfPlayers()];
		for (int i = 0; i < board.getQuantityOfPlayers(); i++)
			opti[i] = 1;
	}

	/**
	 * This creates non-root nodes
	 *
	 * @param board
	 * @param move
	 * @param parent
	 */
	public Node(Board board, Move move, Node parent) {
		children = new ArrayList<>();
		this.parent = parent;
		this.move = move;
		Board tempBoard = board.duplicate(false);
		tempBoard.makeMove(move);
		player = tempBoard.getCurrentPlayer();
		scores = new double[board.getQuantityOfPlayers()];
		pess = new double[board.getQuantityOfPlayers()];
		opti = new double[board.getQuantityOfPlayers()];
		for (int i = 0; i < board.getQuantityOfPlayers(); i++)
			opti[i] = 1;
	}

	/**
	 * Return the upper confidence bound of this state
	 *
	 * @param c typically sqrt(2). Increase to emphasize exploration. Decrease
	 *          to increase exploitation
	 * @return
	 */
	public double upperConfidenceBound(double c) {
		return scores[parent.player] / games + c * Math.sqrt(Math.log(parent.games + 1) / games);
	}

	/**
	 * Update the tree with the new score.
	 *
	 * @param score
	 */
	public void backPropagateScore(double[] score) {
		this.games++;
		for (int i = 0; i < score.length; i++)
			this.scores[i] += score[i];

		if (parent != null)
			parent.backPropagateScore(score);
	}

	/**
	 * Expand this node by populating its list of
	 * unvisited child nodes.
	 *
	 * @param currentBoard
	 */
	public void expandNode(Board currentBoard) {
		List<Move> legalMoves = currentBoard.getMoves(CallLocation.TREE_POLICY);
		unvisitedChildren = new ArrayList<>();
		for (Move legalMove : legalMoves) {
			Node tempState = new Node(currentBoard, legalMove, this);
			unvisitedChildren.add(tempState);
		}
	}

	/**
	 * Set the bounds in the given node and propagate the values
	 * back up the tree. When bounds are first created they are
	 * both equivalent to a player's score.
	 *
	 * @param score
	 */
	public void backPropagateBounds(double[] score) {
		for (int i = 0; i < score.length; i++) {
			opti[i] = score[i];
			pess[i] = score[i];
		}

		if (parent != null)
			parent.backPropagateBoundsHelper();
	}

	private void backPropagateBoundsHelper() {
		for (int i = 0; i < opti.length; i++) {
			if (i == player) {
				opti[i] = 0;
				pess[i] = 0;
			} else {
				opti[i] = 1;
				pess[i] = 1;
			}
		}

		for (int i = 0; i < opti.length; i++) {
			for (Node c : children) {
				if (i == player) {
					if (opti[i] < c.opti[i])
						opti[i] = c.opti[i];
					if (pess[i] < c.pess[i])
						pess[i] = c.pess[i];
				} else {
					if (opti[i] > c.opti[i])
						opti[i] = c.opti[i];
					if (pess[i] > c.pess[i])
						pess[i] = c.pess[i];
				}
			}
		}

		// This compares against a dummy node with bounds 1 0
		// if not all children have been explored
		if (!unvisitedChildren.isEmpty()) {
			for (int i = 0; i < opti.length; i++) {
				if (i == player) {
					opti[i] = 1;
				} else {
					pess[i] = 0;
				}
			}
		}

		// TODO: This causes redundant pruning. Fix it
		pruneBranches();
		if (parent != null)
			parent.backPropagateBoundsHelper();
	}

	private void pruneBranches() {
		for (Node s : children) {
			if (pess[player] >= s.opti[player]) {
				s.pruned = true;
			}
		}

		if (parent != null)
			parent.pruneBranches();
	}

	public boolean isRandomNode() {
		return player < 0;
	}

	public double[] getScores() {
		return scores;
	}

	public void setScores(double[] scores) {
		this.scores = scores;
	}

	public double getGames() {
		return games;
	}

	public void setGames(double games) {
		this.games = games;
	}

	public Move getMove() {
		return move;
	}

	public void setMove(Move move) {
		this.move = move;
	}

	public List<Node> getUnvisitedChildren() {
		return unvisitedChildren;
	}

	public void setUnvisitedChildren(List<Node> unvisitedChildren) {
		this.unvisitedChildren = unvisitedChildren;
	}

	public List<Node> getChildren() {
		return children;
	}

	public void setChildren(List<Node> children) {
		this.children = children;
	}

	public Set<Integer> getRVisited() {
		return rVisited;
	}

	public void setRVisited(Set<Integer> rVisited) {
		this.rVisited = rVisited;
	}

	public Node getParent() {
		return parent;
	}

	public void setParent(Node parent) {
		this.parent = parent;
	}

	public int getPlayer() {
		return player;
	}

	public void setPlayer(int player) {
		this.player = player;
	}

	public double[] getPess() {
		return pess;
	}

	public void setPess(double[] pess) {
		this.pess = pess;
	}

	public double[] getOpti() {
		return opti;
	}

	public void setOpti(double[] opti) {
		this.opti = opti;
	}

	public boolean isPruned() {
		return pruned;
	}

	public void setPruned(boolean pruned) {
		this.pruned = pruned;
	}

	public boolean isValid() {
		return valid;
	}

	public void setValid(boolean valid) {
		this.valid = valid;
	}

	public void invalidate() {
		this.valid = false;
	}

	public double getScoreForCurrentPlayer() {
		if (parent.parent != null)
			throw new IllegalStateException("Please only call this method on direct children of the root node!");
		return scores[parent.player] / games;
	}

	@Override
	public String toString() {
		return "Node{" +
				"move=" + move +
				'}';
	}
}