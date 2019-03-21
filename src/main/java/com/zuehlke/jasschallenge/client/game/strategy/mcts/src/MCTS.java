package com.zuehlke.jasschallenge.client.game.strategy.mcts.src;

import com.zuehlke.jasschallenge.client.game.strategy.exceptions.MCTSException;
import com.zuehlke.jasschallenge.client.game.strategy.mcts.CardMove;
import com.zuehlke.jasschallenge.client.game.strategy.mcts.TrumpfMove;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.List;
import java.util.concurrent.*;

/**
 * The main class responsible for the Monte Carlo Tree Search Method.
 */
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

	public static final Logger logger = LoggerFactory.getLogger(MCTS.class);

	/**
	 * Run a UCT-MCTS simulation for a a certain amount of time.
	 *
	 * @param startingBoard starting board
	 * @param endingTime    time when to stop running (in milliseconds)
	 * @param bounds        enable or disable score bounds.
	 * @return
	 */
	public Move runMCTS_UCT(Board startingBoard, long endingTime, boolean bounds) throws MCTSException {
		scoreBounds = bounds;
		Move bestMoveFound;

		//long startTime = System.currentTimeMillis();

		if (!rootParallelisation) {
			logger.info("Not parallelised :(");
			Node rootNode = new Node(startingBoard);
			runUntilTimeRunsOut(startingBoard, rootNode, endingTime);
			return finalMoveSelection(rootNode);
		} else {
			logger.info("Parallelised with {} threads :)", threads);
			//logger.info("{}ms thinking time left.", endingTime - startTime);
			for (int i = 0; i < threads; i++)
				futures.add((FutureTask<Node>) threadpool.submit(new MCTSTask(startingBoard, endingTime)));

			try {

				while (!checkDone(futures)) {
					//System.err.println("Futures not ready yet. Simulation is still running. Waiting now...");
					Thread.sleep(10);
				}

				for (FutureTask<Node> future : futures)
					assert future.isDone();

				ArrayList<Node> rootNodes = new ArrayList<>();

				// Collect all computed root nodes
				for (FutureTask<Node> future : futures)
					rootNodes.add(future.get());

				assert !rootNodes.isEmpty();

				ArrayList<Move> moves = new ArrayList<>();

				for (Node node : rootNodes) {
					if (node.isValid()) { // so, if there was at least one run
						Move move = finalMoveSelection(node);
						moves.add(move);
						//logger.info(move);
					}
				}

				assert !moves.isEmpty();

				return vote(moves);

			} catch (InterruptedException | ExecutionException e) {
				logger.debug("{}", e);
				throw (new MCTSException("There was a problem in the MCTS. Enable debug logging for more information."));
			} finally {
				//threadpool.shutdown();
				futures.clear();

				//assert threadpool.isShutdown();
				assert futures.isEmpty();
			}
		}

		//long endTime = System.currentTimeMillis();

		/*
		if (this.trackTime) {
			//logger.info("Making choice for player: " + bestMoveFound);
			logger.info("Thinking time for move: " + (endTime - startTime) + "ms");
		}
		*/
	}

	private void runNTimes(Board startingBoard, Node rootNode, int runs) {
		for (int i = 0; i < runs; i++)
			select(startingBoard.duplicate(false), rootNode);
	}

	private void runUntilTimeRunsOut(Board startingBoard, Node rootNode, long endingTime) {
		int runCounter = 0;
		while ((System.currentTimeMillis() < endingTime)) {
			// Start new path from root node
			select(startingBoard, rootNode);
			runCounter++;
		}
		if (runCounter == 0) {
			rootNode.invalidate();
		}
		logger.info("Run {} times for same random cards", runCounter);
	}

	private Move vote(ArrayList<Move> moves) {
		HashMap<Move, Integer> numberOfSelections = new HashMap<>();
		assert !moves.isEmpty();
		for (Move move : moves) {
			int number = 1;
			if (numberOfSelections.containsKey(move)) {
				number += numberOfSelections.get(move);
			}
			numberOfSelections.put(move, number);
		}
		// Print statistics so we can get insights into the decision process of the algorithm
		// TODO get rid of instanceof codesmell
		numberOfSelections.forEach((move, numTimesSelected) -> {
			String moveDisplay = "";
			if (move instanceof CardMove)
				moveDisplay = ((CardMove) move).getPlayedCard().toString();
			if (move instanceof TrumpfMove)
				moveDisplay = ((TrumpfMove) move).getChosenTrumpf().toString();
			logger.info("{} selected {} times.", moveDisplay, numTimesSelected);
		});
		Optional<Map.Entry<Move, Integer>> entryOptional = numberOfSelections.entrySet().parallelStream().sorted(Map.Entry.comparingByValue(Collections.reverseOrder())).findFirst();

		assert entryOptional.isPresent();
		return entryOptional.get().getKey();



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
		BoardNodePair boardNodePair = treePolicy(currentBoard, currentNode);


		// Run a random playout until the end of the game.
		double[] score = playout(boardNodePair.getBoard());

		// Backpropagate results of playout.
		Node node = boardNodePair.getNode();
		node.backPropagateScore(score);
		if (scoreBounds) {
			node.backPropagateBounds(score);
		}
	}

	/**
	 * Begin tree policy. Traverse down the tree and expand.
	 * Return the new node or the deepest node it could reach.
	 * Additionally, return a board matching the returned node.
	 */
	private BoardNodePair treePolicy(Board brd, Node node) {
		//long startTime = System.currentTimeMillis();

		Board board = brd.duplicate(false);

		while (!board.gameOver()) {
			if (!node.isRandomNode()) { // this is a regular node
				if (node.getUnvisitedChildren() == null) {
					node.expandNode(board);
				}

				if (!node.getUnvisitedChildren().isEmpty()) {
					Node temp = node.getUnvisitedChildren().remove(random.nextInt(node.getUnvisitedChildren().size()));
					node.getChildren().add(temp);
					board.makeMove(temp.getMove());
					return new BoardNodePair(board, temp);
				} else {
					ArrayList<Node> bestNodes = findChildren(node, board, optimisticBias, pessimisticBias,
							explorationConstant);

					if (bestNodes.isEmpty()) {
						// We have failed to find a single child to visit
						// from a non-terminal node, so we conclude that
						// all children must have been pruned, and that
						// therefore there is no reason to continue.
						return new BoardNodePair(board, node);
					}

					Node finalNode = bestNodes.get(random.nextInt(bestNodes.size()));
					node = finalNode;
					board.makeMove(finalNode.getMove());
				}
			} else { // this is a random node

				// Random nodes are special. We must guarantee that
				// every random node has a fully populated list of
				// child nodes and that the list of unvisited children
				// is empty. We start by checking if we have been to
				// this node before. If we haven't, we must initialise
				// all of this node's children properly.

				if (node.getUnvisitedChildren() == null) {
					node.expandNode(board);

					for (Node n : node.getUnvisitedChildren()) {
						node.getChildren().add(n);
					}
					node.getUnvisitedChildren().clear();
				}

				// The tree policy for random nodes is different. We
				// ignore selection heuristics and pick one node at
				// random based on the weight vector.

				Node selectedNode = node.getChildren().get(node.randomSelect(board));
				node = selectedNode;
				board.makeMove(selectedNode.getMove());
			}
		}

		//Helper.printMethodTime(startTime);

		return new BoardNodePair(board, node);
	}

	/**
	 * This is the final step of the algorithm, to pick the best move to
	 * actually make.
	 *
	 * @param node this is the node whose children are considered
	 * @return the best Move the algorithm can find
	 */
	private Move finalMoveSelection(Node node) {
		Node finalMove;

		switch (finalSelectionPolicy) {
			case maxChild:
				finalMove = maxChild(node);
				break;
			case robustChild:
				finalMove = robustChild(node);
				break;
			default:
				finalMove = robustChild(node);
				break;
		}

		return finalMove.getMove();
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

		for (Node current : node.getChildren()) {
			tempBest = current.getGames();
			bestValue = getBestValue(bestValue, tempBest, bestNodes, current);
		}

		return bestNodes.get(random.nextInt(bestNodes.size()));
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

		return bestNodes.get(random.nextInt(bestNodes.size()));
	}

	/**
	 * Playout function for MCTS
	 *
	 * @param board
	 * @return
	 */
	private double[] playout(Board board) {
		//long startTime = System.currentTimeMillis();

		List<Move> moves;
		Move move;
		Board brd = board.duplicate(false);

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
				// WHY DOES IT PROCESS THE NOT DUPLICATED BOARD HERE?
				playoutpolicy.process(board);
			}
		}

		//Helper.printMethodTime(startTime);

		return brd.getScore();
	}

	private Move getRandomMove(Board board, List<Move> moves) {
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
	 * @param explorationConstant
	 */
	public void setExplorationConstant(double explorationConstant) {
		this.explorationConstant = explorationConstant;
	}

	public void setMoveSelectionPolicy(FinalSelectionPolicy finalSelectionPolicy) {
		this.finalSelectionPolicy = finalSelectionPolicy;
	}

	public void setHeuristicFunction(HeuristicFunction heuristicFunction) {
		heuristic = heuristicFunction;
	}

	public void setPlayoutSelection(PlayoutSelection playoutSelection) {
		playoutpolicy = playoutSelection;
	}

	/**
	 * This is multiplied by the pessimistic bounds of any considered move
	 * during selection.
	 *
	 * @param pessimisticBias
	 */
	public void setPessimisticBias(double pessimisticBias) {
		this.pessimisticBias = pessimisticBias;
	}

	/**
	 * This is multiplied by the optimistic bounds of any considered move during
	 * selection.
	 *
	 * @param optimisticBias
	 */
	public void setOptimisticBias(double optimisticBias) {
		this.optimisticBias = optimisticBias;
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

	public boolean isParallelisationEnabled() {
		return rootParallelisation;
	}

	/**
	 * Shuts down the thread pool. Has to be called as soon as it is not used anymore!
	 */
	public void shutDown() {
		threadpool.shutdown();
	}

	/**
	 * Checks if the thread pool has been shut down.
	 */
	public boolean isShutDown() {
		return threadpool.isShutdown();
	}

	/**
	 * Check if all threads are done
	 *
	 * @param tasks
	 * @return
	 */
	private boolean checkDone(ArrayList<FutureTask<Node>> tasks) {
		for (FutureTask<Node> task : tasks) {
			if (!task.isDone()) {
				return false;
			}
		}

		return true;
	}


	/**
	 * This is a task for the threadpool.
	 */
	private class MCTSTask implements Callable<Node> {
		private Board board;
		private long endingTime;

		private MCTSTask(Board board, long endingTime) {
			this.endingTime = endingTime;
			this.board = board.duplicate(true);
		}

		@Override
		public Node call() {
			Node root = new Node(board);

			//logger.info("New random cards dealt");
			runUntilTimeRunsOut(board, root, endingTime);

			return root;
		}

	}

}