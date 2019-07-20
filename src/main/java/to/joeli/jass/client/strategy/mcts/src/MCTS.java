package to.joeli.jass.client.strategy.mcts.src;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import to.joeli.jass.client.strategy.exceptions.MCTSException;

import java.util.*;
import java.util.concurrent.*;

// TODO evaluate which is more important: many runs or many root parallelisations

// TODO calculate how big the tree gets at each point in the game

// TODO try out score bounds


/**
 * The main class responsible for the Monte Carlo Tree Search Method.
 */
public class MCTS {

	private Random random = new Random(42);
	private boolean rootParallelisationEnabled;
	private boolean scoreBoundsUsed;
	private double explorationConstant = Math.sqrt(2.0);
	private double pessimisticBias;
	private double optimisticBias;
	private FinalSelectionPolicy finalSelectionPolicy = FinalSelectionPolicy.ROBUST_CHILD;
	private HeuristicFunction heuristicFunction;
	private PlayoutSelection playoutPolicy;

	private ExecutorService threadPool;
	private ArrayList<FutureTask<Move>> futures;

	public static final Logger logger = LoggerFactory.getLogger(MCTS.class);


	/**
	 * Run a UCT-MCTS simulation for a certain amount of time.
	 *
	 * @param startingBoard starting board
	 * @param endingTime    time when to stop running (in milliseconds)
	 * @return
	 */
	public Move runForTime(Board startingBoard, int numDeterminizations, long endingTime) throws MCTSException {
		if (!rootParallelisationEnabled) {
			logger.info("Only running one determinization :(");
			return executeByTime(startingBoard, endingTime);
		} else {
			logger.info("Running {} determinizations :)", numDeterminizations);
			submitTimeTasks(startingBoard, numDeterminizations, endingTime);
			return collectResultsAndGetFinalSelectedMove();
		}
	}

	private void submitTimeTasks(Board startingBoard, int numDeterminizations, long endingTime) {
		for (int i = 0; i < numDeterminizations; i++)
			futures.add((FutureTask<Move>) threadPool.submit(new MCTSTaskTime(startingBoard, endingTime)));
	}


	/**
	 * Run a UCT-MCTS simulation for a certain number of runs.
	 *
	 * @param startingBoard starting board
	 * @param runs          the number of runs
	 * @return
	 */
	public Move runForRuns(Board startingBoard, int numDeterminizations, long runs) throws MCTSException {
		if (!rootParallelisationEnabled) {
			logger.info("Only running one determinization :(");
			return executeByRuns(startingBoard, runs);
		} else {
			logger.info("Running {} determinizations :)", numDeterminizations);
			submitRunsTasks(startingBoard, numDeterminizations, runs);
			return collectResultsAndGetFinalSelectedMove();
		}
	}


	private void submitRunsTasks(Board startingBoard, int numDeterminizations, long runs) {
		for (int i = 0; i < numDeterminizations; i++)
			futures.add((FutureTask<Move>) threadPool.submit(new MCTSTaskRuns(startingBoard, runs)));
	}


	/**
	 * Runs the MCTS for one determinization for the specified number of runs
	 *
	 * @param startingBoard
	 * @param runs
	 * @return the final move selected
	 */
	private Move executeByRuns(Board startingBoard, long runs) {
		Node rootNode = new Node(startingBoard);
		long startTime = System.currentTimeMillis();
		for (int i = 0; i < runs; i++)
			select(startingBoard, rootNode);
		logger.debug("Ran {} runs in {}ms.", runs, System.currentTimeMillis() - startTime);
		return finalMoveSelection(rootNode);
	}

	/**
	 * Runs the MCTS for one determinization until the time runs out
	 *
	 * @param startingBoard
	 * @param endingTime
	 * @return the final move selected
	 */
	private Move executeByTime(Board startingBoard, long endingTime) {
		Node rootNode = new Node(startingBoard);
		long startTime = System.currentTimeMillis();
		long runCounter = 0;
		while ((System.currentTimeMillis() < endingTime)) {
			// Start new path from root node
			select(startingBoard, rootNode);
			runCounter++;
		}
		if (runCounter == 0) {
			rootNode.invalidate();
		}
		logger.debug("Ran {} runs in {}ms.", runCounter, System.currentTimeMillis() - startTime);
		return finalMoveSelection(rootNode);
	}

	private Move collectResultsAndGetFinalSelectedMove() throws MCTSException {
		try {
			while (!checkDone(futures)) {
				// logger.debug("Futures not ready yet. Simulation is still running. Waiting now...");
				Thread.sleep(10);
			}

			for (FutureTask<Move> future : futures) {
				if (!future.isDone()) throw new AssertionError();
			}

			ArrayList<Move> moves = new ArrayList<>();

			// Collect all computed root nodes
			for (FutureTask<Move> future : futures) {
				final Move move = future.get();
				if (move != null)
					moves.add(move);
			}

			return vote(moves);

		} catch (InterruptedException | ExecutionException e) {
			logger.debug("{}", e);
			throw (new MCTSException("There was a problem in the MCTS. Enable debug logging for more information."));
		} finally {
			futures.clear();
		}
	}

	private Move vote(ArrayList<Move> moves) throws MCTSException {
		HashMap<Move, Integer> numberOfSelections = new HashMap<>();
		if (moves.isEmpty())
			throw new MCTSException("There are no moves to vote from. Maybe there was not enough time to explore the tree.");

		for (Move move : moves) {
			int number = 1;
			if (numberOfSelections.containsKey(move)) {
				number += numberOfSelections.get(move);
			}
			numberOfSelections.put(move, number);
		}
		// Print statistics so we can get insights into the decision process of the algorithm
		numberOfSelections.forEach((move, numTimesSelected) -> logger.info("{} selected {} times.", move, numTimesSelected));
		Map.Entry<Move, Integer> entryOptional = numberOfSelections.entrySet().stream()
				.min(Map.Entry.comparingByValue(Collections.reverseOrder())).orElseThrow(() -> new IllegalStateException("There must be at least one move!"));

		return entryOptional.getKey();



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
		if (scoreBoundsUsed) {
			node.backPropagateBounds(score);
		}
	}

	/**
	 * Begin tree policy. Traverse down the tree and expand.
	 * Return the new node or the deepest node it could reach.
	 * Additionally, return a board matching the returned node.
	 */
	private BoardNodePair treePolicy(Board oldBoard, Node node) {
		Board board = oldBoard.duplicate(false);

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
					List<Node> bestNodes = findChildren(node, board, optimisticBias, pessimisticBias,
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
				Node selectedNode = node.getChildren().get(getRandomChildNodeIndex(board));
				node = selectedNode;
				board.makeMove(selectedNode.getMove());
			}
		}

		return new BoardNodePair(board, node);
	}

	/**
	 * This is the final step of the algorithm, to pick the best move to
	 * actually make.
	 *
	 * @param node this is the node whose children are considered
	 * @return the best Move the algorithm can find or null if the node is invalid
	 */
	private Move finalMoveSelection(Node node) {
		if (!node.isValid()) // if there was no run completed
			return null;

		Node finalMove;
		switch (finalSelectionPolicy) {
			case MAX_CHILD:
				finalMove = maxChild(node);
				break;
			case ROBUST_CHILD:
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
	 * @param oldBoard
	 * @return
	 */
	private double[] playout(Board oldBoard) {
		// Do not simulate the playout but estimate the score directly with a neural network
		if (oldBoard.hasScoreEstimator())
			return oldBoard.estimateScore();

		List<Move> moves;
		Move move;
		Board board = oldBoard.duplicate(false);

		// Start playing random moves until the game is over
		while (!board.gameOver()) {
			if (playoutPolicy == null) {
				moves = board.getMoves(CallLocation.PLAYOUT); // NOTE: Originally it used CallLocation.TREE_POLICY here
				if (moves.isEmpty()) throw new AssertionError();
				if (board.getCurrentPlayer() >= 0) {
					// make random selection normally
					move = moves.get(random.nextInt(moves.size()));
				} else {
					// This situation only occurs when a move
					// is entirely random, for example a die
					// roll. We must consider the random weights
					// of the moves.
					move = getRandomMove(board, moves);
				}
			} else {
				move = playoutPolicy.getBestMove(board); // NOTE: Originally it used the not duplicated oldBoard here.
			}
			board.makeMove(move);
		}
		return board.getScore();
	}

	private Move getRandomMove(Board board, List<Move> moves) {
		return moves.get(getRandomChildNodeIndex(board));
	}

	/**
	 * Select a child node at random and return its index.
	 *
	 * @param board
	 * @return
	 */
	private static int getRandomChildNodeIndex(Board board) {
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
		return randomIndex;
	}

	/**
	 * Produce a list of viable nodes to visit. The actual selection is done in runMCTS
	 *
	 * @param optimisticBias
	 * @param pessimisticBias
	 * @param explorationConstant
	 * @return
	 */
	private List<Node> findChildren(Node node, Board board, double optimisticBias, double pessimisticBias,
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

				if (heuristicFunction != null) {
					tempBest += heuristicFunction.heuristicFunction(board);
				}

				bestValue = getBestValue(bestValue, tempBest, bestNodes, s);
			}
		}

		return bestNodes;
	}

	/**
	 * Determines if score bounds should be used or not.
	 *
	 * @param scoreBoundsUsed
	 */
	public void setScoreBoundsUsed(boolean scoreBoundsUsed) {
		this.scoreBoundsUsed = scoreBoundsUsed;
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

	/**
	 * Sets the heuristicFunction used to make some nodes more probable to be selected based on heuristic.
	 *
	 * @param heuristicFunction
	 */
	public void setHeuristicFunction(HeuristicFunction heuristicFunction) {
		this.heuristicFunction = heuristicFunction;
	}

	/**
	 * Sets the playoutPolicy used to replace the random rollout during the simulation phase.
	 *
	 * @param playoutSelection
	 */
	public void setPlayoutSelection(PlayoutSelection playoutSelection) {
		playoutPolicy = playoutSelection;
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

	/**
	 * Switch on multi threading.
	 * IMPORTANT: A threadPool is started here. Make sure that you terminate it using the {@link #shutDown()} method in the end (might be outside this class)!
	 **/
	public void enableRootParallelisation(int numThreads) {
		rootParallelisationEnabled = true;

		threadPool = Executors.newFixedThreadPool(numThreads);
		futures = new ArrayList<>();
	}

	public boolean isParallelisationEnabled() {
		return rootParallelisationEnabled;
	}

	/**
	 * Shuts down the thread pool. Has to be called as soon as it is not used anymore!
	 */
	public void shutDown() {
		threadPool.shutdown();
	}

	/**
	 * Checks if the thread pool has been shut down.
	 */
	public boolean isShutDown() {
		return threadPool.isShutdown();
	}

	/**
	 * Check if all threads are done
	 *
	 * @param tasks
	 * @return
	 */
	private boolean checkDone(ArrayList<FutureTask<Move>> tasks) {
		for (FutureTask<Move> task : tasks) {
			if (!task.isDone()) {
				return false;
			}
		}

		return true;
	}

	public void setRandom(int seed) {
		this.random = new Random(seed);
	}

	protected abstract class MCTSTask implements Callable<Move> {
		protected Board board;

		protected MCTSTask(Board board) {
			// Here we create a new determinization by distributing new random cards for the hidden cards of the other players
			// Starting from here we operate in a perfect information game setting!
			this.board = board.duplicate(true);
		}
	}

	/**
	 * This is a time bounded task for the threadPool.
	 */
	protected class MCTSTaskTime extends MCTSTask {
		protected long endingTime;

		protected MCTSTaskTime(Board board, long endingTime) {
			super(board);
			this.endingTime = endingTime;
		}

		@Override
		public Move call() {
			return executeByTime(board, endingTime);
		}
	}


	/**
	 * This is a runs bounded task for the threadPool.
	 */
	protected class MCTSTaskRuns extends MCTSTask {
		protected long runs;

		protected MCTSTaskRuns(Board board, long runs) {
			super(board);
			this.runs = runs;
		}

		@Override
		public Move call() {
			return executeByRuns(board, runs);
		}
	}

}