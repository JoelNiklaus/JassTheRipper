package com.zuehlke.jasschallenge.client.game.strategy.mcts.src;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class MCTS {
	private final Random random = new Random();

	private boolean scoreBounds;
	private double explorationConstant = Math.sqrt(2.0);
	private double pessimisticBias = 0.0;
	private double optimisticBias = 0.0;
	private boolean rootParallelisation;
	private boolean trackTime; // display thinking time used
	private FinalSelectionPolicy finalSelectionPolicy = FinalSelectionPolicy.robustChild;
	private HeuristicFunction heuristic;
	private PlayoutSelection playoutpolicy;

	private int threads;
	private ExecutorService threadpool;
	private ArrayList<FutureTask<Node>> futures;

	/**
	 * Run a UCT-MCTS simulation for a a certain amount of time.
	 *
	 * @param startingBoard starting board
	 * @param endingTime    time when to stop running (in nanoseconds)
	 * @param bounds        enable or disable score bounds.
	 * @return
	 */
	public Move runMCTS_UCT(Board startingBoard, long endingTime, boolean bounds) {
		scoreBounds = bounds;
		Move bestMoveFound = null;

		long startTime = System.nanoTime();

		if (!rootParallelisation) {
			System.out.println("not parallelised :(");
			Node rootNode = new Node(startingBoard);
			runUntilTimeRunsOut(startingBoard, rootNode, endingTime);
			bestMoveFound = finalMoveSelection(rootNode);

		} else {
			System.out.println("parallelised with " + threads + " threads :)");
			for (int i = 0; i < threads; i++)
				futures.add((FutureTask<Node>) threadpool.submit(new MCTSTask(startingBoard, endingTime)));

			try {

				while (!checkDone(futures))
					Thread.sleep(10);

				ArrayList<Node> rootNodes = new ArrayList<>();

				// Collect all computed root nodes
				for (FutureTask<Node> future : futures) {
					// Just abort the computation if time is up TODO does this really abort it??????
					try {
						Node node = future.get(400, TimeUnit.MILLISECONDS);
						rootNodes.add(node);
					} catch (TimeoutException e) {
						System.out.println("Timeout! Had to abort computation of thread!");
						future.cancel(true);
					}
				}

				ArrayList<Move> moves = new ArrayList<>();

				for (Node node : rootNodes) {
					if (node.isValid()) { // so, if there was at least one run
						Move move = robustChild(node).getMove(); // Select robust child
						System.out.println(move);
						moves.add(move);
					}
				}

				bestMoveFound = vote(moves);

			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}

			threadpool.shutdownNow();
			futures.clear();

			assert threadpool.isShutdown();
			assert futures.isEmpty();
		}

		long endTime = System.nanoTime();

		if (this.trackTime)

		{
			System.out.println("Making choice for player: " + bestMoveFound);
			System.out.println("Thinking time for move: " + (endTime - startTime) / 1000000 + "ms");
		}

		return bestMoveFound;
	}

	private void runNTimes(Board startingBoard, Node rootNode, int runs) {
		for (int i = 0; i < runs; i++)
			select(startingBoard.duplicate(), rootNode);
	}

	private void runUntilTimeRunsOut(Board startingBoard, Node rootNode, long endingTime) {
		int runCounter = 0;
		while ((System.nanoTime() < endingTime)) {
			// Start new path from root node
			select(startingBoard, rootNode);
			runCounter++;
		}
		if (runCounter == 0) {
			rootNode.invalidate();
		}
		System.out.println("Run " + runCounter + " times for same random cards");
	}

	private Move vote(ArrayList<Move> moves) {
		HashMap<Move, Integer> numberOfSelections = new HashMap<>();
		assert !moves.isEmpty();
		Move votedMove = moves.get(0);
		for (Move move : moves) {
			int number = 1;
			if (numberOfSelections.containsKey(move)) {
				number += numberOfSelections.get(move);
			}
			numberOfSelections.put(move, number);
		}
		Optional<Map.Entry<Move, Integer>> entryOptional = numberOfSelections.entrySet().parallelStream().sorted(Map.Entry.comparingByValue(Collections.reverseOrder())).findFirst();
		if (entryOptional.isPresent())
			votedMove = entryOptional.get().getKey();

		return votedMove;

		/*
		Collections.sort(moves);
		ArrayList<Integer> counts = new ArrayList<Integer>();
		ArrayList<Move> cmoves = new ArrayList<Move>();

		Move omove = moves.get(0);
		int count = 0;
		for (Move m : moves) {
			if (omove.compareTo(m) == 0) {
				count++;
			} else {
				cmoves.add(omove);
				counts.add(count);
				omove = m;
				count = 1;
			}
		}

		int mostvotes = 0;
		ArrayList<Move> mostVotedMove = new ArrayList<Move>();
		for (int i = 0; i < counts.size(); i++) {
			if (mostvotes < counts.get(i)) {
				mostvotes = counts.get(i);
				mostVotedMove.clear();
				mostVotedMove.add(cmoves.get(i));
			} else if (mostvotes == counts.get(i)) {
				mostVotedMove.add(cmoves.get(i));
			}
		}

		return mostVotedMove.get(random.nextInt(mostVotedMove.size()));
		*/
	}

	/**
	 * This represents the select stage, or default policy, of the algorithm.
	 * Traverse down to the bottom of the tree using the selection strategy
	 * until you find an unexpanded child node. Expand it. Run a random playout.
	 * Backpropagate results of the playout.
	 *
	 * @param currentNode  Node from which to start selection
	 * @param currentBoard Board state to work from.
	 */
	private void select(Board currentBoard, Node currentNode) {
		// Begin tree policy. Traverse down the tree and expand. Return
		// the new node or the deepest node it could reach. Return too
		// a board matching the returned node.
		BoardNodePair data = treePolicy(currentBoard, currentNode);

		// Run a random playout until the end of the game.
		double[] score = playout(data.getNode(), data.getBoard());

		// Backpropagate results of playout.
		Node n = data.getNode();
		n.backPropagateScore(score);
		if (scoreBounds) {
			n.backPropagateBounds(score);
		}
	}

	/**
	 *
	 */
	private BoardNodePair treePolicy(Board brd, Node node) {
		Board b = brd.duplicate();

		while (!b.gameOver()) {
			if (node.getPlayer() >= 0) { // this is a regular node
				if (node.getUnvisitedChildren() == null) {
					node.expandNode(b);
				}

				if (!node.getUnvisitedChildren().isEmpty()) {
					Node temp = node.getUnvisitedChildren().remove(random.nextInt(node.getUnvisitedChildren().size()));
					node.getChildren().add(temp);
					b.makeMove(temp.getMove());
					return new BoardNodePair(b, temp);
				} else {
					ArrayList<Node> bestNodes = findChildren(node, b, optimisticBias, pessimisticBias,
							explorationConstant);

					if (bestNodes.size() == 0) {
						// We have failed to find a single child to visit
						// from a non-terminal node, so we conclude that
						// all children must have been pruned, and that
						// therefore there is no reason to continue.
						return new BoardNodePair(b, node);
					}

					Node finalNode = bestNodes.get(random.nextInt(bestNodes.size()));
					node = finalNode;
					b.makeMove(finalNode.getMove());
				}
			} else { // this is a random node

				// Random nodes are special. We must guarantee that
				// every random node has a fully populated list of
				// child nodes and that the list of unvisited children
				// is empty. We start by checking if we have been to
				// this node before. If we haven't, we must initialise
				// all of this node's children properly.

				if (node.getUnvisitedChildren() == null) {
					node.expandNode(b);

					for (Node n : node.getUnvisitedChildren()) {
						node.getChildren().add(n);
					}
					node.getUnvisitedChildren().clear();
				}

				// The tree policy for random nodes is different. We
				// ignore selection heuristics and pick one node at
				// random based on the weight vector.

				Node selectedNode = node.getChildren().get(node.randomSelect(b));
				node = selectedNode;
				b.makeMove(selectedNode.getMove());
			}
		}

		return new BoardNodePair(b, node);
	}

	/**
	 * This is the final step of the algorithm, to pick the best move to
	 * actually make.
	 *
	 * @param node this is the node whose children are considered
	 * @return the best Move the algorithm can find
	 */
	private Move finalMoveSelection(Node node) {
		Node r;

		switch (finalSelectionPolicy) {
			case maxChild:
				r = maxChild(node);
				break;
			case robustChild:
				r = robustChild(node);
				break;
			default:
				r = robustChild(node);
				break;
		}

		return r.getMove();
	}

	/**
	 * Select the most visited child node
	 *
	 * @param node
	 * @return
	 */
	private Node robustChild(Node node) {
		double bestValue = Double.NEGATIVE_INFINITY;
		double tempBest;
		ArrayList<Node> bestNodes = new ArrayList<>();

		for (Node s : node.getChildren()) {
			tempBest = s.getGames();
			bestValue = getBestValue(bestValue, tempBest, bestNodes, s);
		}

		Node finalNode = bestNodes.get(random.nextInt(bestNodes.size()));

		return finalNode;
	}

	private double getBestValue(double bestValue, double tempBest, ArrayList<Node> bestNodes, Node node) {
		if (tempBest > bestValue) {
			bestNodes.clear();
			bestNodes.add(node);
			bestValue = tempBest;
		} else if (tempBest == bestValue) {
			bestNodes.add(node);
		}
		return bestValue;
	}

	/**
	 * Select the child node with the highest score
	 *
	 * @param node
	 * @return
	 */
	private Node maxChild(Node node) {
		double bestValue = Double.NEGATIVE_INFINITY;
		double tempBest;
		ArrayList<Node> bestNodes = new ArrayList<>();

		for (Node s : node.getChildren()) {
			tempBest = s.getScore()[node.getPlayer()];
			tempBest += s.getOpti()[node.getPlayer()] * optimisticBias;
			tempBest += s.getPess()[node.getPlayer()] * pessimisticBias;
			bestValue = getBestValue(bestValue, tempBest, bestNodes, s);
		}

		Node finalNode = bestNodes.get(random.nextInt(bestNodes.size()));

		return finalNode;
	}

	/**
	 * Playout function for MCTS
	 *
	 * @param state
	 * @return
	 */
	private double[] playout(Node state, Board board) {
		ArrayList<Move> moves;
		Move move;
		Board brd = board.duplicate();

		// Start playing random moves until the game is over
		while (!brd.gameOver()) {
			if (playoutpolicy == null) {
				moves = brd.getMoves(CallLocation.treePolicy);
				if (brd.getCurrentPlayer() >= 0) {
					// make random selection normally
					move = moves.get(random.nextInt(moves.size()));
				} else {

					// This situation only occurs when a move
					// is entirely random, for example a die
					// roll. We must consider the random weights
					// of the moves.

					move = getRandomMove(brd, moves);
				}

				brd.makeMove(move);
			} else {
				playoutpolicy.process(board);
			}
		}

		return brd.getScore();
	}

	private Move getRandomMove(Board board, ArrayList<Move> moves) {
		double[] weights = board.getMoveWeights();

		double totalWeight = 0.0d;
		for (double weight : weights) {
			totalWeight += weight;
		}

		int randomIndex = -1;
		double random = Math.random() * totalWeight;
		for (int i = 0; i < weights.length; ++i) {
			random -= weights[i];
			if (random <= 0.0d) {
				randomIndex = i;
				break;
			}
		}

		return moves.get(randomIndex);
	}

	/**
	 * Produce a list of viable nodes to visit. The actual selection is done in
	 * runMCTS
	 *
	 * @param optimisticBias
	 * @param pessimisticBias
	 * @param explorationConstant
	 * @return
	 */
	public ArrayList<Node> findChildren(Node node, Board board, double optimisticBias, double pessimisticBias,
										double explorationConstant) {
		double bestValue = Double.NEGATIVE_INFINITY;
		ArrayList<Node> bestNodes = new ArrayList<>();
		for (Node s : node.getChildren()) {
			// Pruned is only ever true if a branch has been pruned
			// from the tree and that can only happen if bounds
			// propagation mode is enabled.
			if (!s.isPruned()) {
				double tempBest = s.upperConfidenceBound(explorationConstant) + optimisticBias * s.getOpti()[node.getPlayer()]
						+ pessimisticBias * s.getPess()[node.getPlayer()];

				if (heuristic != null) {
					tempBest += heuristic.h(board);
				}

				bestValue = getBestValue(bestValue, tempBest, bestNodes, s);
			}
		}

		return bestNodes;
	}

	/**
	 * Sets the exploration constant for the algorithm. You will need to find
	 * the optimal value through testing. This can have a big impact on
	 * performance. Default value is sqrt(2)
	 *
	 * @param exp
	 */
	public void setExplorationConstant(double exp) {
		explorationConstant = exp;
	}

	public void setMoveSelectionPolicy(FinalSelectionPolicy policy) {
		finalSelectionPolicy = policy;
	}

	public void setHeuristicFunction(HeuristicFunction h) {
		heuristic = h;
	}

	public void setPlayoutSelection(PlayoutSelection p) {
		playoutpolicy = p;
	}

	/**
	 * This is multiplied by the pessimistic bounds of any considered move
	 * during selection.
	 *
	 * @param b
	 */
	public void setPessimisticBias(double b) {
		pessimisticBias = b;
	}

	/**
	 * This is multiplied by the optimistic bounds of any considered move during
	 * selection.
	 *
	 * @param b
	 */
	public void setOptimisticBias(double b) {
		optimisticBias = b;
	}

	public void setTimeDisplay(boolean displayTime) {
		this.trackTime = displayTime;
	}

	/**
	 * Switch on multi threading. The argument indicates
	 * how many threads you want in the thread pool.
	 *
	 * @param threads
	 */
	public void enableRootParallelisation(int threads) {
		rootParallelisation = true;
		this.threads = threads;

		threadpool = Executors.newFixedThreadPool(threads);
		futures = new ArrayList<>();
	}

	// Check if all threads are done
	private boolean checkDone(ArrayList<FutureTask<Node>> tasks) {
		for (FutureTask<Node> task : tasks) {
			if (!task.isDone()) {
				return false;
			}
		}

		return true;
	}

	/*
	 * This is a task for the threadpool.
	 */
	private class MCTSTask implements Callable<Node> {
		private Board board;
		private long endingTime;

		private MCTSTask(Board board, long endingTime) {
			this.endingTime = endingTime;
			this.board = board.duplicateWithNewRandomCards();
		}

		@Override
		public Node call() throws Exception {
			Node root = new Node(board);

			//System.out.println("New random cards dealt");
			runUntilTimeRunsOut(board, root, endingTime);

			return root;
		}

	}

}