package com.zuehlke.jasschallenge.client.game.strategy.training;

import com.google.common.collect.EvictingQueue;
import com.zuehlke.jasschallenge.client.game.*;
import com.zuehlke.jasschallenge.client.game.strategy.*;
import com.zuehlke.jasschallenge.client.game.strategy.helpers.GameSessionBuilder;
import com.zuehlke.jasschallenge.client.game.strategy.helpers.NeuralNetworkHelper;
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
	private static int replayMemorySizeFactor = 4;

	private static final boolean SUPERVISED_PRETRAINING_ENABLED = true;
	private static final String BASE_PATH = "src/main/resources/";
	private static final String EXPERIMENT_FOLDER = "DOES-IT-LEARN?_"
			+ NeuralNetwork.NUM_NEURONS + "-neurons" + // TODO experiment with num neurons first (128, 256, 512)
			"_3-hidden-layers" + // TODO experiment with num hidden layers later 2, 3, 4, 5,6
			"_lr=" + NeuralNetwork.LEARNING_RATE +
			"_dropout=" + NeuralNetwork.DROPOUT +
			"_weight-decay=" + NeuralNetwork.WEIGHT_DECAY +
			"_seed=" + NeuralNetwork.SEED;
	public static final String SCORE_ESTIMATOR_PATH = BASE_PATH + EXPERIMENT_FOLDER + "/ScoreEstimator.zip"; // Can be opened externally
	public static final String DATASET_PATH = BASE_PATH + EXPERIMENT_FOLDER + "/random_playout_start.dataset";
	public static final String FEATURES_PATH = BASE_PATH + EXPERIMENT_FOLDER + "/features.npy";
	public static final String LABELS_PATH = BASE_PATH + EXPERIMENT_FOLDER + "/labels.npy";
	private static final int NUM_EPISODES = 1; // TEST: 1
	private static final int NUM_TRAINING_GAMES = 2; // Should be an even number, TEST: 2
	private static final int NUM_TESTING_GAMES = 2; // Should be an even number, TEST: 2
	// If the learning network scores more points than the frozen network times this factor, the frozen network gets replaced
	public static final double IMPROVEMENT_THRESHOLD_PERCENTAGE = 105;
	public static final int SEED = 42;

	private static final int SAVE_DATASET_FREQUENCY = 1;


	private String scoreEstimatorFilePath;
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
		final Arena arena = new Arena(SCORE_ESTIMATOR_PATH, NUM_TRAINING_GAMES, NUM_TESTING_GAMES, IMPROVEMENT_THRESHOLD_PERCENTAGE, SEED);
		if ("CollectDataSet".equals(args[0]))
			arena.collectDataSetRandomPlayouts(DATASET_PATH);
		else if ("PretrainNetwork".equals(args[0]))
			arena.pretrainNetwork(DATASET_PATH);
		else
			arena.trainForNumEpisodes(NUM_EPISODES);
	}

	public Arena(String scoreEstimatorFilePath, int numTrainingGames, int numTestingGames, double improvementThresholdPercentage, int seed) {
		// CudaEnvironment.getInstance().getConfiguration().allowMultiGPU(true); // NOTE: This might have to be enabled on the server

		this.scoreEstimatorFilePath = scoreEstimatorFilePath;
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
		logger.info("Performance over the episodes:\n{}", history);

		tearDown();
	}

	public void trainUntilBetterThanRandomPlayouts() {
		setUp(false);

		List<Double> history = Arrays.asList(0.0);
		for (int i = 0; history.get(i) < 100; i++) {
			history.set(i, runEpisode(i));
		}
		logger.info("Performance over the episodes:\n{}", history);

		tearDown();
	}

	public void collectDataSetRandomPlayouts(String dataSetFilePath) {
		setUp(true);

		logger.info("Collecting a dataset of games played with random playouts\n");
		runMCTSWithRandomPlayout(random, numTrainingGames, true, dataSetFilePath, false);

		tearDown();
	}

	public void pretrainNetwork(String dataSetFilePath) {
		final NeuralNetwork scoreEstimationNetwork = gameSession.getPlayersOfTeam(0).get(0).getScoreEstimationNetwork();
		try {
			final DataSet dataSet = NeuralNetworkHelper.loadDataSet(dataSetFilePath);
			System.out.println(dataSet);
			scoreEstimationNetwork.train(dataSet, 500);
			updateAndSaveNetwork(scoreEstimationNetwork, scoreEstimatorFilePath);
			scoreEstimationNetwork.evaluate(dataSet);
		} catch (RuntimeException e) {
			logger.error("{}", e);
			logger.error("Could not find dataset to train model with. Starting with random initialization now.");
		}
	}

	private void updateAndSaveNetwork(NeuralNetwork scoreEstimationNetwork, String scoreEstimatorFilePath) {
		// Set the frozen networks of the players of team 1 to a copy of the trainable network
		gameSession.getPlayersOfTeam(1).forEach(player -> player.setScoreEstimationNetwork(new NeuralNetwork(scoreEstimationNetwork)));
		// Checkpoint so we don't lose any training progress
		scoreEstimationNetwork.save(scoreEstimatorFilePath);
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
		// NOTE: The networks of team 0 are trainable. Both players of the same team normally have the same network references
		final NeuralNetwork scoreEstimationNetwork = gameSession.getPlayersOfTeam(0).get(0).getScoreEstimationNetwork();
		scoreEstimationNetwork.train(observations, labels, 10);

		logger.info("Pitting the 'naked' networks against each other to see " +
				"if the learning network can score more than {}% of the points of the frozen network\n", improvementThresholdPercentage);
		final double improvement = runOnlyNetworks(random, numTestingGames, false, null);
		if (improvement > improvementThresholdPercentage) { // if the learning network is significantly better
			updateAndSaveNetwork(scoreEstimationNetwork, scoreEstimatorFilePath);
			logger.info("The learning network outperformed the frozen network. Updated the frozen network\n");
		}

		logger.info("Testing MCTS with a value estimator against MCTS with random playouts\n");
		final double performance = runScoreEstimatorAgainstRandomPlayout(random, numTestingGames, true, null);

		logger.info("After episode #{}, value estimation mcts scored {}% of the points of random playouts mcts", episodeNumber, performance);
		return performance;
	}

	public double runMatchWithConfigs(Random random, int numGames, Config[] configs) {
		setUp(false);
		final double performance = performMatch(random, numGames, false, null, configs, true);
		tearDown();
		return performance;
	}

	private double runMCTSWithRandomPlayout(Random random, int numGames, boolean collectExperiences, String dataSetFilePath, boolean orthogonalCardsEnabled) {
		Config[] configs = {
				new Config(true, false, false),
				new Config(true, false, false)
		};
		return performMatch(random, numGames, collectExperiences, dataSetFilePath, configs, orthogonalCardsEnabled);
	}

	private double runMCTSWithValueEstimators(Random random, int numGames, boolean collectExperiences, String dataSetFilePath) {
		Config[] configs = {
				new Config(true, true, true),
				new Config(true, true, false)
		};
		return performMatch(random, numGames, collectExperiences, dataSetFilePath, configs, true);
	}

	private double runOnlyNetworks(Random random, int numGames, boolean collectExperiences, String dataSetFilePath) {
		Config[] configs = {
				new Config(false, true, true),
				new Config(false, true, false)
		};
		return performMatch(random, numGames, collectExperiences, dataSetFilePath, configs, true);
	}

	private double runScoreEstimatorAgainstRandomPlayout(Random random, int numGames, boolean collectExperiences, String dataSetFilePath) {
		Config[] configs = {
				new Config(true, true, true),
				new Config(true, false, false)
		};
		return performMatch(random, numGames, collectExperiences, dataSetFilePath, configs, true);
	}

	/**
	 * Performs a match which can be parametrized along multiple dimensions to test the things we want
	 *
	 * @param numGames
	 * @param collectExperiences
	 * @param dataSetFilePath
	 * @param configs
	 * @param orthogonalCardsEnabled determines if two consecutive matches are played with the "same" cards or not
	 *                               if true: we get a more fair tournament
	 *                               if false: we get a more random tournament
	 * @return
	 */
	private double performMatch(Random random, int numGames, boolean collectExperiences, String dataSetFilePath, Config[] configs, boolean orthogonalCardsEnabled) {
		gameSession.getPlayersOfTeam(0).forEach(player -> player.setConfig(configs[0]));
		gameSession.getPlayersOfTeam(1).forEach(player -> player.setConfig(configs[1]));

		return playGames(random, numGames, collectExperiences, dataSetFilePath, orthogonalCardsEnabled);
	}

	private double playGames(Random random, int numGames, boolean collectExperiences, String dataSetFilePath, boolean orthogonalCardsEnabled) {
		List<Card> orthogonalCards = null;
		List<Card> cards = Arrays.asList(Card.values());
		Collections.shuffle(cards, random);

		for (int i = 0; i < numGames; i++) {
			logger.info("Running game #{}\n", i);

			if (orthogonalCardsEnabled)
				orthogonalCards = dealCards(cards, orthogonalCards);
			else {
				Collections.shuffle(cards, random);
				gameSession.dealCards(cards);
			}
			performTrumpfSelection();
			Result result = playGame(collectExperiences);

			logger.info("Result of game #{}: {}\n", i, result);

			if (dataSetFilePath != null && i % SAVE_DATASET_FREQUENCY == 0) {
				final DataSet dataSet = NeuralNetworkHelper.buildDataSet(observations, labels);
				NeuralNetworkHelper.saveDataSet(dataSet, dataSetFilePath);
			}
		}
		gameSession.updateResult(); // normally called within gameSession.startNewGame(), so we need it at the end again
		final Result result = gameSession.getResult();
		logger.info("Aggregated result of the {} games played: {}\n", numGames, result);
		double improvement = Math.round(10000.0 * result.getTeamAScore().getScore() / result.getTeamBScore().getScore()) / 100.0;
		logger.info("Team A scored {}% of the points of Team B\n", improvement);

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
				player.onMoveMade(move);

				if (collectExperiences) // NOTE: only collect high quality experiences
					NeuralNetworkHelper.getAnalogousObservations(game).forEach(observation -> observationsWithPlayer.put(observation, player));
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
			replayMemorySizeFactor = 1000000; // Enough for a lot of games...
		int size = numTrainingGames * replayMemorySizeFactor;
		// When a new element is added and the queue is full, the head is removed.
		observations = EvictingQueue.create(size);
		labels = EvictingQueue.create(size);

		// NOTE: give the training a head start by using a pre-trained network
		if (SUPERVISED_PRETRAINING_ENABLED) {
			final NeuralNetwork scoreEstimationNetwork = gameSession.getPlayersOfTeam(0).get(0).getScoreEstimationNetwork();
			if (scoreEstimationNetwork != null) scoreEstimationNetwork.load(scoreEstimatorFilePath);
		}
	}

	private void tearDown() {
		for (Player player : gameSession.getPlayersInInitialPlayingOrder()) {
			player.onSessionFinished();
		}
		final NeuralNetwork scoreEstimationNetwork = gameSession.getPlayersOfTeam(0).get(0).getScoreEstimationNetwork();
		if (scoreEstimationNetwork != null) scoreEstimationNetwork.save(scoreEstimatorFilePath);
		logger.info("Successfully terminated the training process\n");
	}
}
