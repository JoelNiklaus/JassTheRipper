package to.joeli.jass.client.strategy.mcts.src;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import to.joeli.jass.client.strategy.exceptions.MCTSException;

import java.util.*;
import java.util.concurrent.*;

// TODO evaluate which is more important: many runs or many root parallelisations

// TODO calculate how big the tree gets at each point in the game


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
	private int numPlayouts = 2; // Scored the best in experiments
	private FinalSelectionPolicy finalSelectionPolicy = FinalSelectionPolicy.ROBUST_CHILD;
	private HeuristicFunction heuristicFunction;
	private PlayoutSelectionPolicy playoutSelectionPolicy;

	private ExecutorService threadPool;
	private ArrayList<FutureTask<Node>> futures;

	private int numRuns;
	private int numDeterminizations;

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
			logger.info("Only running one determinization");
			return executeByTime(startingBoard, endingTime).getMove();
		} else {
			this.numDeterminizations = numDeterminizations;
			logger.info("Running {} determinizations", numDeterminizations);
			submitTimeTasks(startingBoard, endingTime);
			return collectResultsAndGetFinalSelectedMove();
		}
	}

	private void submitTimeTasks(Board startingBoard, long endingTime) {
		for (int i = 0; i < numDeterminizations; i++)
			futures.add((FutureTask<Node>) threadPool.submit(new MCTSTaskTime(startingBoard, endingTime)));
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
			return executeByRuns(startingBoard, runs).getMove();
		} else {
			this.numDeterminizations = numDeterminizations;
			logger.info("Running {} determinizations :)", numDeterminizations);
			submitRunsTasks(startingBoard, runs);
			return collectResultsAndGetFinalSelectedMove();
		}
	}


	private void submitRunsTasks(Board startingBoard, long runs) {
		for (int i = 0; i < numDeterminizations; i++)
			futures.add((FutureTask<Node>) threadPool.submit(new MCTSTaskRuns(startingBoard, runs)));
	}


	/**
	 * Runs the MCTS for one determinization for the specified number of runs
	 *
	 * @param startingBoard
	 * @param runs
	 * @return the final move selected
	 */
	private Node executeByRuns(Board startingBoard, long runs) {
		Node rootNode = new Node(startingBoard);
		long startTime = System.currentTimeMillis();
		for (int i = 0; i < runs; i++)
			select(startingBoard, rootNode);
		logger.debug("Ran {} runs in {}ms.", runs, System.currentTimeMillis() - startTime);
		return finalSelection(rootNode);
	}

	/**
	 * Runs the MCTS for one determinization until the time runs out
	 *
	 * @param startingBoard
	 * @param endingTime
	 * @return the final move selected
	 */
	private Node executeByTime(Board startingBoard, long endingTime) {
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
		synchronized (this) {
			numRuns += runCounter;
		}
		logger.debug("Ran {} runs in {}ms.", runCounter, System.currentTimeMillis() - startTime);
		return finalSelection(rootNode);
	}

	private Move collectResultsAndGetFinalSelectedMove() throws MCTSException {
		try {
			while (!checkDone(futures)) {
				// logger.debug("Futures not ready yet. Simulation is still running. Waiting now...");
				Thread.sleep(1);
			}

			for (FutureTask<Node> future : futures) {
				if (!future.isDone()) throw new AssertionError();
			}

			ArrayList<Node> finalNodes = new ArrayList<>();

			// Collect all final selected nodes
			for (FutureTask<Node> future : futures) {
				final Node node = future.get();
				if (node != null)
					finalNodes.add(node);
			}

			logger.info("The MCTS searched {} nodes per determinization", numRuns / numDeterminizations);
			synchronized (this) {
				numRuns = 0;
			}

			return vote(finalNodes);

		} catch (InterruptedException | ExecutionException e) {
			logger.error("{}", e);
			throw (new MCTSException("There was a problem in the MCTS."));
		} finally {
			futures.clear();
		}
	}

	/**
	 * This implements a majority vote from the different determinizations (mcts trees parallelised at the root)
	 *
	 * @param nodes
	 * @return
	 * @throws MCTSException
	 */
	private Move vote(ArrayList<Node> nodes) throws MCTSException {
		if (nodes.isEmpty())
			throw new MCTSException("There are no moves to vote from. Maybe there was not enough time to explore the tree.");

		HashMap<Move, Integer> numSelections = new HashMap<>();
		HashMap<Move, Double> summedFinalScores = new HashMap<>();

		for (Node node : nodes) {
			// Some determinizations are more reliable (more nodes searched)
			// but we choose not to weigh by the number of nodes searched because the difference is small for high strengthlevel
			logger.info("move: {}, num games: {}, score: {}", node.getMove(), node.getParent().getGames(), node.getScoreForCurrentPlayer());
			// TODO how many games do we need to be sufficiently sure of correctness of simulation


			Move move = node.getMove();

			int numSelectionsForMove = numSelections.getOrDefault(move, 0) + 1;
			numSelections.put(move, numSelectionsForMove);

			double summedFinalScoresForMove = summedFinalScores.getOrDefault(move, 0d) + node.getScoreForCurrentPlayer();
			summedFinalScores.put(move, summedFinalScoresForMove);
		}

		// TODO use average final score (AFS) to behave according to risk profile. Example move not selected often but high AFS --> choose when risk-taking high
		// TODO possibly not just choose move with most selections but bias it with AFS
		// Print statistics so we can get insights into the decision process of the algorithm
		numSelections.forEach((move, numTimesSelected) -> {
			final Double summedFinalScore = summedFinalScores.get(move);
			int averageFinalScore = (int) Math.round(summedFinalScore / numTimesSelected);
			logger.info("{} selected {} times with average final score {} -> summed final score: {}",
					String.format("%1$-3s", move), String.format("%1$2d", numTimesSelected), String.format("%1$3d", averageFinalScore), summedFinalScore);
		});



		return summedFinalScores.entrySet().stream() // move with highest possible reward but still high confidence is chosen -> more risk taking
		//return numSelections.entrySet().stream() // move which has been selected the most over all the determinizations is chosen -> more risk averse
				.max(Map.Entry.comparingByValue())
				.orElseThrow(() -> new IllegalStateException("There must be at least one move!"))
				.getKey();
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
					List<Node> bestNodes = findChildren(node, board, optimisticBias, pessimisticBias, explorationConstant);

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
	 * Playout function for MCTS
	 *
	 * @param board
	 * @return
	 */
	private double[] playout(Board board) {
		// Do not simulate the playout but estimate the score directly with a neural network
		if (board.hasScoreEstimator())
			return board.estimateScore();

		// INFO: Run multiple playouts and take average to get a more reliable outcome. If numPlayouts = 1 take the outcome directly
		double[] scoreAggregate = new double[board.getQuantityOfPlayers()];
		for (int i = 0; i < numPlayouts; i++) {
			final double[] score = runPlayout(board.duplicate(false));
			for (int j = 0; j < score.length; j++) {
				scoreAggregate[j] += score[j];
			}
		}
		if (numPlayouts > 1)
			for (int i = 0; i < scoreAggregate.length; i++)
				scoreAggregate[i] /= numPlayouts;

		return scoreAggregate;
	}

	/**
	 * Runs one playout of the board
	 *
	 * @param board
	 * @return
	 */
	private double[] runPlayout(Board board) {
		// Start playing random moves until the game is over
		while (!board.gameOver()) {
			Move move;
			if (playoutSelectionPolicy == null) {
				move = getRandomMove(board);
			} else {
				move = playoutSelectionPolicy.getBestMove(board); // NOTE: Originally it used the not duplicated oldBoard here.
			}
			board.makeMove(move);
		}
		return board.getScore();
	}

	public Move getRandomMove(Board board) {
		Move move;
		List<Move> moves = board.getMoves(CallLocation.PLAYOUT); // NOTE: Originally it used CallLocation.TREE_POLICY here
		if (moves.isEmpty()) throw new AssertionError();
		if (board.getCurrentPlayer() >= 0) {
			// make random selection normally
			move = moves.get(random.nextInt(moves.size()));
		} else {
			// This situation only occurs when a move
			// is entirely random, for example a die
			// roll. We must consider the random weights
			// of the moves.
			move = moves.get(getRandomChildNodeIndex(board));
		}
		return move;
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
	 * This is the final step of the algorithm, to pick the best move to
	 * actually make.
	 *
	 * @param node this is the node whose children are considered
	 * @return the node with the best Move the algorithm can find or null if the node is invalid
	 */
	private Node finalSelection(Node node) {
		if (!node.isValid()) // if there was no run completed
			return null;

		switch (finalSelectionPolicy) {
			case MAX_CHILD:
				return maxChild(node);
			case ROBUST_CHILD:
				return robustChild(node);
			default:
				return robustChild(node);
		}
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
			tempBest = s.getScores()[node.getPlayer()];
			tempBest += s.getOpti()[node.getPlayer()] * optimisticBias;
			tempBest += s.getPess()[node.getPlayer()] * pessimisticBias;
			bestValue = getBestValue(bestValue, tempBest, bestNodes, s);
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

	/**
	 * Sets the number of playouts that are performed. The average of the performed playouts is returned.
	 * The higher, the better the quality but the longer the time for an iteration through the tree.
	 *
	 * @param numPlayouts
	 */
	public void setNumPlayouts(int numPlayouts) {
		this.numPlayouts = numPlayouts;
	}

	/**
	 * Sets the selection policy to choose the move at the end of the tree search
	 *
	 * @param finalSelectionPolicy
	 */
	public void setFinalSelectionPolicy(FinalSelectionPolicy finalSelectionPolicy) {
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
	 * @param playoutSelectionPolicy
	 */
	public void setPlayoutSelectionPolicy(PlayoutSelectionPolicy playoutSelectionPolicy) {
		this.playoutSelectionPolicy = playoutSelectionPolicy;
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
	private boolean checkDone(ArrayList<FutureTask<Node>> tasks) {
		for (FutureTask<Node> task : tasks)
			if (!task.isDone())
				return false;

		return true;
	}

	public void setRandom(int seed) {
		this.random = new Random(seed);
	}

	protected abstract class MCTSTask implements Callable<Node> {
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
		public Node call() {
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
		public Node call() {
			return executeByRuns(board, runs);
		}
	}

}