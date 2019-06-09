package com.zuehlke.jasschallenge.client.game.strategy.training;

import com.google.common.collect.EvictingQueue;
import com.zuehlke.jasschallenge.client.game.*;
import com.zuehlke.jasschallenge.client.game.strategy.JassTheRipperJassStrategy;
import com.zuehlke.jasschallenge.client.game.strategy.StrengthLevel;
import com.zuehlke.jasschallenge.client.game.strategy.TrumpfSelectionMethod;
import com.zuehlke.jasschallenge.client.game.strategy.helpers.GameSessionBuilder;
import com.zuehlke.jasschallenge.client.game.strategy.mcts.NeuralNetwork;
import com.zuehlke.jasschallenge.game.cards.Card;
import com.zuehlke.jasschallenge.game.mode.Mode;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.factory.Nd4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class Arena {

	// The bigger, the bigger the datasets are, and the longer the training takes
	// If it is 4: each experience will be used 4 times.
	private static int REPLAY_MEMORY_SIZE_FACTOR = 4;
	private static final boolean SUPERVISED_PRETRAINING_ENABLED = true;
	private static final String BASE_PATH = "src/main/resources/";
	private static final String EXPERIMENT_FOLDER = "DOES-IT-LEARN?_"
			+ NeuralNetwork.NUM_NEURONS + "-neurons" + // TODO experiment with num neurons first (128, 256, 512)
			"_3-hidden-layers" + // TODO experiment with num hidden layers later 2, 3, 4, 5,6
			"_lr=" + NeuralNetwork.LEARNING_RATE +
			"_dropout=" + NeuralNetwork.DROPOUT +
			"_weight-decay=" + NeuralNetwork.WEIGHT_DECAY +
			"_seed=" + NeuralNetwork.SEED;
	public static final String VALUE_ESTIMATOR_PATH = BASE_PATH + EXPERIMENT_FOLDER + "/ValueEstimator.zip"; // Can be opened externally
	public static final String DATASET_PATH = BASE_PATH + EXPERIMENT_FOLDER + "/random_playout_start.dataset";// TODO what are the typical file extensions?
	private static final int NUM_EPISODES = 100; // TEST: 1
	private static final int NUM_TRAINING_GAMES = 100; // Should be an even number, TEST: 2
	private static final int NUM_TESTING_GAMES = 100; // Should be an even number, TEST: 2
	// If the learning network scores more points than the frozen network times this factor, the frozen network gets replaced
	public static final double IMPROVEMENT_THRESHOLD_PERCENTAGE = 105;
	public static final int SEED = 42;

	private final int SAVE_DATASET_FREQUENCY = 10;


	private String valueEstimatorFilePath;
	private final int numTrainingGames;
	private final int numTestingGames;
	private final double improvementThresholdPercentage;
	private final Random random;

	private GameSession gameSession;

	private Queue<INDArray> observations;
	private Queue<INDArray> labels;

	public static final Logger logger = LoggerFactory.getLogger(Arena.class);

	/**
	 * @param args 0: "CollectDataSet" or "PretrainNetwork" or null
	 */
	public static void main(String[] args) {
		final Arena arena = new Arena(VALUE_ESTIMATOR_PATH, NUM_TRAINING_GAMES, NUM_TESTING_GAMES, IMPROVEMENT_THRESHOLD_PERCENTAGE, SEED);
		if ("CollectDataSet".equals(args[0]))
			arena.collectDataSetRandomPlayouts(DATASET_PATH);
		else if ("PretrainNetwork".equals(args[0]))
			arena.pretrainNetwork(DATASET_PATH);
		else
			arena.trainForNumEpisodes(NUM_EPISODES);
	}

	public Arena(String valueEstimatorFilePath, int numTrainingGames, int numTestingGames, double improvementThresholdPercentage, int seed) {
		// CudaEnvironment.getInstance().getConfiguration().allowMultiGPU(true); // NOTE: This might have to be enabled on the server

		this.valueEstimatorFilePath = valueEstimatorFilePath;
		this.numTrainingGames = numTrainingGames;
		this.numTestingGames = numTestingGames;
		this.improvementThresholdPercentage = improvementThresholdPercentage;
		random = new Random(seed);
	}

	public void trainForNumEpisodes(int numEpisodes) {
		setUp(false);

		List<Double> history = Arrays.asList(0.0);
		for (int i = 0; i < numEpisodes; i++) {
			history.set(i, runEpisode(i));
		}
		logger.info("Performance over the episodes:\n{}", history.toString());

		tearDown();
	}

	public void trainUntilBetterThanRandomPlayouts() {
		setUp(false);

		List<Double> history = Arrays.asList(0.0);
		for (int i = 0; history.get(i) < 100; i++) {
			history.set(i, runEpisode(i));
		}
		logger.info("Performance over the episodes:\n{}", history.toString());

		tearDown();
	}

	public void collectDataSetRandomPlayouts(String dataSetFilePath) {
		setUp(true);

		logger.info("Collecting a dataset of games played with random playouts\n");
		runMCTSWithRandomPlayout(random, numTrainingGames, true, dataSetFilePath);

		tearDown();
	}

	public void pretrainNetwork(String dataSetFilePath) {
		final JassTheRipperJassStrategy strategy = JassTheRipperJassStrategy.getInstance();
		final NeuralNetwork network = strategy.getNeuralNetwork(true);
		try {
			final DataSet dataSet = NeuralNetwork.loadDataSet(dataSetFilePath);
			network.train(dataSet, 500);
			strategy.updateNetworks();
			saveNetwork();
		} catch (RuntimeException e) {
			e.printStackTrace();
			logger.error("Could not find dataset to train model with. Starting with random initialization now.");
		}
	}


	/**
	 * Runs an episode with the following parts:
	 * - Self play with value estimation and mcts policy improvement to collect experiences into the replay buffer
	 * - Trains the network with the recorded games from the replay buffer
	 * - Pits the networks against each other without the mcts policy improvement to see which one performs better
	 * - If the learning network can outperform the frozen network by an improvementThresholdPercentage, the frozen one is updated
	 * - Tests the performance of mcts with value estimation by playing against mcts with random playouts
	 *
	 * @return the performance of mcts with value estimation against mcts with random playouts
	 */
	private double runEpisode(int episodeNumber) {
		logger.info("Running episode #{}\n", episodeNumber);

		logger.info("Collecting training examples by self play with MCTS policy improvement\n");
		runMCTSWithValueEstimators(random, numTrainingGames, true, null);


		logger.info("Training the network with the collected examples\n");
		JassTheRipperJassStrategy.getInstance().getNeuralNetwork(true).train(observations, labels, 10);

		logger.info("Pitting the 'naked' networks against each other to see " +
				"if the learning network can score more than {}% of the points of the frozen network\n", improvementThresholdPercentage);
		final double improvement = runOnlyNetworks(random, numTestingGames, false, null);
		if (improvement > improvementThresholdPercentage) { // if the learning network is significantly better
			JassTheRipperJassStrategy.getInstance().updateNetworks(); // set the frozen network to a copy of the learning network.
			saveNetwork(); // NOTE: Checkpoint so we don't lose any training progress
			logger.info("The learning network outperformed the frozen network. Updated the frozen network\n");
		}

		logger.info("Testing MCTS with a value estimator against MCTS with random playouts\n");
		final double performance = runValueEstimatorAgainstRandomPlayout(random, numTestingGames, true, null);

		logger.info("After episode #{}, value estimation mcts scored {}% of the points of random playouts mcts", episodeNumber, performance);
		return performance;
	}

	public double runMCTSWithRandomPlayoutDifferentStrengthLevels(Random random, int numGames, StrengthLevel[] cardStrengthLevels, StrengthLevel[] trumpfStrengthLevels) {
		setUp(false);

		gameSession.getTeams().get(0).getPlayers().forEach(player -> player.setCardStrengthLevel(cardStrengthLevels[0]));
		gameSession.getTeams().get(1).getPlayers().forEach(player -> player.setCardStrengthLevel(cardStrengthLevels[1]));

		gameSession.getTeams().get(0).getPlayers().forEach(player -> player.setTrumpfStrengthLevel(trumpfStrengthLevels[0]));
		gameSession.getTeams().get(1).getPlayers().forEach(player -> player.setTrumpfStrengthLevel(trumpfStrengthLevels[1]));

		final double performance = runMCTSWithRandomPlayout(random, numGames, false, null);

		tearDown();
		return performance;
	}

	public double runMCTSWithRandomPlayoutDifferentTrumpfSelectionMethods(Random random, int numGames, TrumpfSelectionMethod[] trumpfSelectionMethods) {
		setUp(false);

		gameSession.getTeams().get(0).getPlayers().forEach(player -> player.setTrumpfSelectionMethod(trumpfSelectionMethods[0]));
		gameSession.getTeams().get(1).getPlayers().forEach(player -> player.setTrumpfSelectionMethod(trumpfSelectionMethods[1]));

		final double performance = runMCTSWithRandomPlayout(random, numGames, false, null);

		tearDown();
		return performance;
	}


	private double runMCTSWithRandomPlayout(Random random, int numGames, boolean collectExperiences, String dataSetFilePath) {
		return performMatch(random, numGames, collectExperiences, dataSetFilePath,
				new boolean[]{true, true}, new boolean[]{false, false}, new boolean[]{false, false});
	}

	private double runMCTSWithValueEstimators(Random random, int numGames, boolean collectExperiences, String dataSetFilePath) {
		return performMatch(random, numGames, collectExperiences, dataSetFilePath,
				new boolean[]{true, true}, new boolean[]{true, true}, new boolean[]{true, false});
	}

	private double runOnlyNetworks(Random random, int numGames, boolean collectExperiences, String dataSetFilePath) {
		return performMatch(random, numGames, collectExperiences, dataSetFilePath,
				new boolean[]{false, false}, new boolean[]{true, true}, new boolean[]{true, false});
	}

	private double runValueEstimatorAgainstRandomPlayout(Random random, int numGames, boolean collectExperiences, String dataSetFilePath) {
		return performMatch(random, numGames, collectExperiences, dataSetFilePath,
				new boolean[]{true, true}, new boolean[]{true, false}, new boolean[]{true, false});
	}

	/**
	 * Performs a match which can be parametrized along multiple dimensions to test the things we want
	 *
	 * @param numGames
	 * @param collectExperiences
	 * @param dataSetFilePath
	 * @param mctsEnabled
	 * @param valueEstimatorUsed
	 * @param networkTrainable
	 * @return
	 */
	private double performMatch(Random random, int numGames, boolean collectExperiences, String dataSetFilePath, boolean[] mctsEnabled, boolean[] valueEstimatorUsed, boolean[] networkTrainable) {
		// MCTS enabled for policy improvement
		gameSession.getTeams().get(0).getPlayers().forEach(player -> player.setMctsEnabled(mctsEnabled[0]));
		gameSession.getTeams().get(1).getPlayers().forEach(player -> player.setMctsEnabled(mctsEnabled[1]));

		// This is necessary to trigger the value estimation by the network inside the MCTS!
		gameSession.getTeams().get(0).getPlayers().forEach(player -> player.setValueEstimaterUsed(valueEstimatorUsed[0]));
		gameSession.getTeams().get(1).getPlayers().forEach(player -> player.setValueEstimaterUsed(valueEstimatorUsed[1]));

		// Only one team's network is trainable, the other one's is frozen
		gameSession.getTeams().get(0).getPlayers().forEach(player -> player.setNetworkTrainable(networkTrainable[0]));
		gameSession.getTeams().get(1).getPlayers().forEach(player -> player.setNetworkTrainable(networkTrainable[1]));

		return playGames(random, numGames, collectExperiences, dataSetFilePath);
	}

	private double playGames(Random random, int numGames, boolean collectExperiences, String dataSetFilePath) {
		List<Card> orthogonalCards = null;
		List<Card> cards = Arrays.asList(Card.values());
		Collections.shuffle(cards, random);

		for (int i = 0; i < numGames; i++) {
			logger.info("Running game #{}\n", i);

			orthogonalCards = dealCards(cards, orthogonalCards);
			performTrumpfSelection();
			Result result = playGame(collectExperiences);

			logger.info("Result of game #{}: {}\n", i, result);

			if (dataSetFilePath != null && i % SAVE_DATASET_FREQUENCY == 0) {
				final DataSet dataSet = NeuralNetwork.buildDataSet(observations, labels);
				NeuralNetwork.saveDataSet(dataSet, dataSetFilePath);
			}
		}
		gameSession.updateResult(); // normally called within gameSession.startNewGame(), so we need it at the end again
		final Result result = gameSession.getResult();
		logger.info("Aggregated result of the {} games played: {}\n", numGames, result);
		double improvement = Math.round(10000.0 * result.getTeamAScore().getScore() / result.getTeamBScore().getScore()) / 100.0;
		logger.info("Team A scored " + improvement + "% of the points of Team B\n");

		logger.info("Resetting the result so we can get a fresh start afterwards\n");
		gameSession.resetResult();

		return improvement;
	}

	/**
	 * Plays a game and appends the made observations with the final point difference to the provided parameters (observations and labels)
	 */
	private Result playGame(boolean collectExperiences) {
		HashMap<INDArray, Player> observationsWithPlayer = new HashMap<>();
		Game game = gameSession.getCurrentGame();
		while (!game.gameFinished()) {
			final Round round = game.getCurrentRound();
			while (!round.roundFinished()) {
				final Player player = game.getCurrentPlayer();
				final Move move = player.makeMove(gameSession);
				gameSession.makeMove(move);
				player.onMoveMade(move, gameSession);

				if (collectExperiences) // NOTE: only collect high quality experiences
					observationsWithPlayer.put(NeuralNetwork.getObservation(game), player);
			}
			gameSession.startNextRound();
		}

		if (collectExperiences)
			for (Map.Entry<INDArray, Player> entry : observationsWithPlayer.entrySet()) {
				observations.add(entry.getKey());
				double[] label = {game.getResult().getTeamScore(entry.getValue()) / 157.0}; // NOTE: the label is between 0 and 1 inside the network
				labels.add(Nd4j.createFromArray(label));
			}

		return game.getResult();
	}

	/**
	 * Deals the cards to the players based on a random seed.
	 * "Orthogonal" cards (List is rotated by 9: team 1 gets cards of team 2 and vice versa) are returned for use in the next game (fairness!)
	 *
	 * @param normalCards
	 * @param orthogonalCards
	 * @return
	 */
	private List<Card> dealCards(List<Card> normalCards, List<Card> orthogonalCards) {
		if (orthogonalCards == null) {
			logger.info("Dealing the 'normal' cards: {}\n", normalCards);
			gameSession.dealCards(normalCards);

			// And prepare orthogonal cards
			orthogonalCards = new ArrayList<>(normalCards);
			Collections.rotate(orthogonalCards, 9); // rotate list so that the opponents now have the cards we had before and vice versa --> this ensures fair testing!
		} else { // if we have orthogonal cards
			logger.info("Dealing the 'orthogonal' cards: {}\n", orthogonalCards);
			gameSession.dealCards(orthogonalCards);

			// And prepare normal cards again
			orthogonalCards = null;
			Collections.shuffle(normalCards, random);
		}
		return orthogonalCards;
	}


	private void performTrumpfSelection() {
		boolean shifted = false;
		Player currentPlayer = gameSession.getTrumpfSelectingPlayer();
		Mode mode = currentPlayer.chooseTrumpf(gameSession, shifted);

		if (mode.equals(Mode.shift())) {
			shifted = true;
			final Player partner = gameSession.getPartnerOfPlayer(currentPlayer);
			mode = partner.chooseTrumpf(gameSession, shifted);
		}
		gameSession.startNewGame(mode, shifted);
	}

	private void setUp(boolean collectingDataSet) {
		logger.info("Setting up the training process\n");
		gameSession = GameSessionBuilder.newSession().createGameSession();

		for (Player player : gameSession.getPlayersInInitialPlayingOrder()) {
			player.onSessionStarted(gameSession);
		}

		if (collectingDataSet)
			REPLAY_MEMORY_SIZE_FACTOR = 1000000; // Enough for a lot of games...
		int size = numTrainingGames * REPLAY_MEMORY_SIZE_FACTOR;
		// When a new element is added and the queue is full, the head is removed.
		observations = EvictingQueue.create(size);
		labels = EvictingQueue.create(size);

		// NOTE: give the training a head start by using a pretrained network
		if (SUPERVISED_PRETRAINING_ENABLED)
			loadNetwork();
	}

	private void tearDown() {
		for (Player player : gameSession.getPlayersInInitialPlayingOrder()) {
			player.onSessionFinished();
		}
		saveNetwork();
		logger.info("Successfully terminated the training process\n");
	}

	/**
	 * Serialize and save the trained network for later evaluation
	 */
	private void saveNetwork() {
		JassTheRipperJassStrategy.getInstance().getNeuralNetwork(true).saveModel(valueEstimatorFilePath);
	}

	/**
	 * Load a saved network so we can do with less training
	 */
	private void loadNetwork() {
		JassTheRipperJassStrategy.getInstance().getNeuralNetwork(true).loadModel(valueEstimatorFilePath);
	}

}
