package com.zuehlke.jasschallenge.client.game.strategy.training;

import com.google.common.collect.EvictingQueue;
import com.zuehlke.jasschallenge.client.game.*;
import com.zuehlke.jasschallenge.client.game.strategy.config.Config;
import com.zuehlke.jasschallenge.client.game.strategy.helpers.GameSessionBuilder;
import com.zuehlke.jasschallenge.client.game.strategy.helpers.NeuralNetworkHelper;
import com.zuehlke.jasschallenge.game.cards.Card;
import com.zuehlke.jasschallenge.game.mode.Mode;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.factory.Nd4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;


// TODO refactor this! it is way too big: Maybe take out helper method or divide into two classes with different responsibilities: data collection and experiments
public class Arena {

	// The bigger, the bigger the datasets are, and the longer the training takes
	// If it is 4: each experience will be used 4 times.
	// Should not be bigger than 32 because it might result in OutOfMemoryErrors
	private static final int REPLAY_MEMORY_SIZE_FACTOR = 4; // Standard: 4, 8, 16

	private static final boolean SUPERVISED_PRETRAINING_ENABLED = true;
	private static final String BASE_PATH = "src/main/resources/";
	private static final String EXPERIMENT_FOLDER = "DOES-IT-LEARN?_"
			+ NeuralNetwork.NUM_NEURONS + "-neurons" + // TODO experiment with num neurons first (128, 256, 512)
			"_3-hidden-layers" + // TODO experiment with num hidden layers later 2, 3, 4, 5, 6
			"_lr=" + NeuralNetwork.LEARNING_RATE +
			"_dropout=" + NeuralNetwork.DROPOUT +
			"_weight-decay=" + NeuralNetwork.WEIGHT_DECAY +
			"_seed=" + NeuralNetwork.SEED;
	public static final String DATASETS_BASE_PATH = BASE_PATH + "datasets/";
	public static final String MODELS_BASE_PATH = BASE_PATH + "models/";
	public static final String SCORE_ESTIMATOR_KERAS_PATH = MODELS_BASE_PATH + "score_estimator.hdf5";
	public static final String SCORE_ESTIMATOR_DL4J_PATH = MODELS_BASE_PATH + "/ScoreEstimator.zip"; // Can be opened externally
	public static final String CARDS_ESTIMATOR_KERAS_PATH = MODELS_BASE_PATH + "cards_estimator.hdf5";
	public static final String CARDS_ESTIMATOR_DL4J_PATH = MODELS_BASE_PATH + "/CardsEstimator.zip"; // Can be opened externally
	public static final String DATASET_PATH = DATASETS_BASE_PATH + "random_playout.dataset";
	private static final int NUM_EPISODES = 1; // TEST: 1
	private static final int NUM_TRAINING_GAMES = 2; // Should be an even number, TEST: 2
	private static final int NUM_TESTING_GAMES = 2; // Should be an even number, TEST: 2
	// If the learning network scores more points than the frozen network times this factor, the frozen network gets replaced
	public static final double IMPROVEMENT_THRESHOLD_PERCENTAGE = 105;
	public static final int SEED = 42;
	public static final double TOTAL_POINTS = 157.0; // TODO 257 or 157 better here?

	private final int numTrainingGames;
	private final int numTestingGames;
	private final double improvementThresholdPercentage;
	private final Random random;

	private GameSession gameSession;

	// The input for the neural networks, a representation of the game
	private Queue<INDArray> scoreFeatures;
	// The input for the neural networks, a representation of the game, without the cards of the other players
	private Queue<INDArray> cardsFeatures;
	// The labels for the score estimator, the score at the end of the game
	private Queue<INDArray> scoreLabels;
	// The labels for the cards estimator, the actual cards the other players had
	private Queue<INDArray> cardsLabels;

	public static final Logger logger = LoggerFactory.getLogger(Arena.class);

	public static void main(String[] args) {
		final Arena arena = new Arena(NUM_TRAINING_GAMES, NUM_TESTING_GAMES, IMPROVEMENT_THRESHOLD_PERCENTAGE, SEED);

		logger.info("Collecting a dataset of games played with random playouts\n");
		arena.collectDataSetRandomPlayouts(REPLAY_MEMORY_SIZE_FACTOR * 10);

		logger.info("Pre-training a score estimator network\n");
		NeuralNetworkHelper.preTrainScoreEstimator();

		logger.info("Pre-training a cards estimator network\n");
		NeuralNetworkHelper.preTrainCardsEstimator();

		//logger.info("Training the score estimator network with self-play\n");
		//arena.trainForNumEpisodes(1000);
	}

	public Arena(int numTrainingGames, int numTestingGames, double improvementThresholdPercentage, int seed) {
		// CudaEnvironment.getInstance().getConfiguration().allowMultiGPU(true); // NOTE: This might have to be enabled on the server

		this.numTrainingGames = numTrainingGames;
		this.numTestingGames = numTestingGames;
		this.improvementThresholdPercentage = improvementThresholdPercentage;
		random = new Random(seed);
	}

	public void trainForNumEpisodes(int numEpisodes) {
		setUp();

		List<Double> history = new ArrayList<>();
		for (int i = 0; i < numEpisodes; i++) {
			history.add(i, runEpisode(i));
		}
		logger.info("Performance over the episodes:\n{}", history);

		tearDown();
	}

	public void trainUntilBetterThanRandomPlayouts() {
		setUp();

		List<Double> history = new ArrayList<>(Collections.singletonList(0.0));
		for (int i = 0; history.get(i) < 100; i++) {
			history.add(i, runEpisode(i));
		}
		logger.info("Performance over the episodes:\n{}", history);

		tearDown();
	}

	public void collectDataSetRandomPlayouts(int numGames) {
		setUp();

		runMCTSWithRandomPlayout(random, numGames, false);

		tearDown();
	}

	public void pretrainNetwork() {
		final ScoreEstimator scoreEstimator = gameSession.getPlayersOfTeam(0).get(0).getScoreEstimator();
		try {
			final DataSet dataSet = NeuralNetworkHelper.loadDataSet(DATASET_PATH);
			System.out.println(dataSet);
			scoreEstimator.train(dataSet, 500);
			updateAndSaveScoreEstimator(scoreEstimator);
			scoreEstimator.evaluate(dataSet);
		} catch (RuntimeException e) {
			logger.error("{}", e);
			logger.error("Could not find dataset to train model with. Starting with random initialization now.");
		}
	}

	private void updateAndSaveScoreEstimator(ScoreEstimator scoreEstimator) {
		// Set the frozen networks of the players of team 1 to a copy of the trainable network
		gameSession.getPlayersOfTeam(1).forEach(player -> player.setScoreEstimator(new ScoreEstimator(scoreEstimator)));
		// Checkpoint so we don't lose any training progress
		scoreEstimator.save(Arena.SCORE_ESTIMATOR_DL4J_PATH);
	}

	private void updateAndSaveCardsEstimator(CardsEstimator cardsEstimator) {
		// Set the frozen networks of the players of team 1 to a copy of the trainable network
		gameSession.getPlayersOfTeam(1).forEach(player -> player.setCardsEstimator(new CardsEstimator(cardsEstimator)));
		// Checkpoint so we don't lose any training progress
		cardsEstimator.save(Arena.CARDS_ESTIMATOR_DL4J_PATH);
	}


	/**
	 * Runs an episode with the following parts:
	 * - Self play with score estimation and mcts policy improvement to collect experiences into the replay buffer
	 * - Trains the network with the recorded games from the replay buffer
	 * - Pits the networks against each other without the mcts policy improvement to see which one performs better
	 * - If the learning network can outperform the frozen network by an improvementThresholdPercentage, the frozen one is updated
	 * - Tests the performance of mcts with score estimation by playing against mcts with random playouts
	 *
	 * @return the performance of mcts with score estimation against mcts with random playouts
	 */
	private double runEpisode(int episodeNumber) {
		logger.info("Running episode #{}\n", episodeNumber);

		logger.info("Collecting training examples by self play with MCTS policy improvement\n");
		runMCTSWithScoreEstimators(random, numTrainingGames);

		logger.info("Training the networks with the collected examples\n");
		// NOTE: The networks of team 0 are trainable. Both players of the same team normally have the same network references
		final ScoreEstimator scoreEstimator = gameSession.getPlayersOfTeam(0).get(0).getScoreEstimator();
		scoreEstimator.train(scoreFeatures, scoreLabels, 10);
		final CardsEstimator cardsEstimator = gameSession.getPlayersOfTeam(0).get(0).getCardsEstimator();
		cardsEstimator.train(cardsFeatures, cardsLabels, 10);

		logger.info("Pitting the 'naked' networks against each other to see " +
				"if the learning network can score more than {}% of the points of the frozen network\n", improvementThresholdPercentage);
		final double improvement = runOnlyNetworks(random, numTestingGames);
		if (improvement > improvementThresholdPercentage) { // if the learning network is significantly better
			updateAndSaveScoreEstimator(scoreEstimator);
			logger.info("The learning network outperformed the frozen network. Updated the frozen network\n");
		}

		logger.info("Testing MCTS with a score estimator against MCTS with random playouts\n");
		final double performance = runScoreEstimatorAgainstRandomPlayout(random, numTestingGames);

		logger.info("After episode #{}, score estimation MCTS scored {}% of the points of random playout MCTS", episodeNumber, performance);
		return performance;
	}

	public double runMatchWithConfigs(Random random, int numGames, Config[] configs) {
		setUp();
		final double performance = performMatch(random, numGames, false, false, true, configs);
		tearDown();
		return performance;
	}

	private double runMCTSWithRandomPlayout(Random random, int numGames, boolean orthogonalCardsEnabled) {
		Config[] configs = {
				new Config(true, false, false),
				new Config(true, false, false)
		};
		return performMatch(random, numGames, true, true, orthogonalCardsEnabled, configs);
	}

	private double runMCTSWithScoreEstimators(Random random, int numGames) {
		Config[] configs = {
				new Config(true, true, true),
				new Config(true, true, false)
		};
		return performMatch(random, numGames, true, false, true, configs);
	}

	private double runOnlyNetworks(Random random, int numGames) {
		Config[] configs = {
				new Config(false, true, true),
				new Config(false, true, false)
		};
		return performMatch(random, numGames, false, false, true, configs);
	}

	private double runScoreEstimatorAgainstRandomPlayout(Random random, int numGames) {
		Config[] configs = {
				new Config(true, true, true),
				new Config(true, false, false)
		};
		return performMatch(random, numGames, true, false, true, configs);
	}

	/**
	 * Performs a match which can be parametrized along multiple dimensions to test the things we want
	 *
	 * @param numGames
	 * @param collectExperiences
	 * @param saveData
	 * @param orthogonalCardsEnabled determines if two consecutive matches are played with the "same" cards or not
	 *                               if true: we get a more fair tournament
	 *                               if false: we get a more random tournament
	 * @param configs
	 * @return
	 */
	private double performMatch(Random random, int numGames, boolean collectExperiences, boolean saveData, boolean orthogonalCardsEnabled, Config[] configs) {
		gameSession.getPlayersOfTeam(0).forEach(player -> player.setConfig(configs[0]));
		gameSession.getPlayersOfTeam(1).forEach(player -> player.setConfig(configs[1]));

		return playGames(random, numGames, collectExperiences, saveData, orthogonalCardsEnabled);
	}

	private double playGames(Random random, int numGames, boolean collectExperiences, boolean saveData, boolean orthogonalCardsEnabled) {
		List<Card> orthogonalCards = null;
		List<Card> cards = Arrays.asList(Card.values());
		Collections.shuffle(cards, random);

		for (int i = 1; i <= numGames; i++) {
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

			if (saveData && i % REPLAY_MEMORY_SIZE_FACTOR == 0) {
				final String extension = zeroPadded(i - REPLAY_MEMORY_SIZE_FACTOR) + "-" + zeroPadded(i);
				NeuralNetworkHelper.saveData(scoreFeatures, cardsFeatures, scoreLabels, cardsLabels, extension);
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

	private static String zeroPadded(int number) {
		return String.format("%04d", number);
	}

	/**
	 * Plays a game and appends the made scoreFeatures with the final point difference to the provided parameters (scoreFeatures and scoreLabels)
	 */
	private Result playGame(boolean collectExperiences) {
		Game game = gameSession.getCurrentGame();

		HashMap<Player, INDArray> cardsLabelsForPlayer = null;
		if (collectExperiences)
			cardsLabelsForPlayer = buildCardsLabels(game);

		HashMap<INDArray, Player> observationForPlayer = new HashMap<>(); // The INDArray is the key because it is unique
		while (!game.gameFinished()) {
			final Round round = game.getCurrentRound();
			while (!round.roundFinished()) {
				final Player player = game.getCurrentPlayer();
				final Move move = player.makeMove(gameSession);
				gameSession.makeMove(move);
				player.onMoveMade(move);

				if (collectExperiences)
					NeuralNetworkHelper.getAnalogousObservations(game).forEach(observation -> observationForPlayer.put(observation, player));
			}
			gameSession.startNextRound();
		}

		assert observationForPlayer.size() == 24;
		if (collectExperiences)
			for (Map.Entry<INDArray, Player> entry : observationForPlayer.entrySet()) {
				scoreFeatures.add(entry.getKey());
				cardsFeatures.add(NeuralNetworkHelper.getCardsObservation(entry.getKey()));
				// NOTE: the scoreLabel is between 0 and 1 inside the network
				double scoreLabel = game.getResult().getTeamScore(entry.getValue()) / TOTAL_POINTS;
				scoreLabels.add(Nd4j.createFromArray(scoreLabel));
				cardsLabels.add(cardsLabelsForPlayer.get(entry.getValue())); // get the cardsLabel of the corresponding player
			}

		return game.getResult();
	}

	/**
	 * For each card we have a one hot encoded vector of length 3.
	 * The one represents the player who has that specific card.
	 * The players are listed in the playing sequence starting from the next player (in the playing order) to the current player.
	 *
	 * @param game
	 */
	static HashMap<Player, INDArray> buildCardsLabels(Game game) {
		final int numPlayers = 4;
		HashMap<Player, INDArray> cardsLabelsForPlayer = new HashMap<>();

		final Card[] cards = Card.values();
		final List<Player> playingOrder = game.getOrder().getPlayersInInitialPlayingOrder();

		for (int start = 0; start < 4; start++) { // for each of the four player's perspective
			INDArray labels = Nd4j.create(36, numPlayers); // 36 cards, 4 players
			for (int i = 0; i < cards.length; i++) { // for every card
				int[] players = new int[numPlayers];
				for (int p = 0; p < numPlayers; p++) { // check for all the players
					if (playingOrder.get((start + p) % numPlayers).getCards().contains(cards[i])) { // if the player has the card
						players[p] = 1; // set the card
						break; // and no need to evaluate the rest of the players because they cannot have the card
					}
				}
				labels.putRow(i, Nd4j.createFromArray(players));
			}
			cardsLabelsForPlayer.put(playingOrder.get(start), labels);
		}
		return cardsLabelsForPlayer;
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
		Mode mode = currentPlayer.chooseTrumpf(gameSession, false);

		if (mode.equals(Mode.shift())) {
			shifted = true;
			final Player partner = gameSession.getPartnerOfPlayer(currentPlayer);
			mode = partner.chooseTrumpf(gameSession, true);
		}
		gameSession.startNewGame(mode, shifted);
	}

	private void setUp() {
		logger.info("Setting up the training process\n");
		gameSession = GameSessionBuilder.newSession().createGameSession();

		for (Player player : gameSession.getPlayersInInitialPlayingOrder()) {
			player.onSessionStarted(gameSession);
		}

		// 36: Number of Cards in a game, 24: Number of color permutations (data augmentation)
		int size = 36 * 24 * REPLAY_MEMORY_SIZE_FACTOR;
		// When a new element is added and the queue is full, the head is removed.
		scoreFeatures = EvictingQueue.create(size);
		cardsFeatures = EvictingQueue.create(size);
		scoreLabels = EvictingQueue.create(size);
		cardsLabels = EvictingQueue.create(size);

		// NOTE: give the training a head start by using a pre-trained network
		if (SUPERVISED_PRETRAINING_ENABLED) {
			final ScoreEstimator scoreEstimator = gameSession.getPlayersOfTeam(0).get(0).getScoreEstimator();
			if (scoreEstimator != null) {
				scoreEstimator.loadKerasModel(SCORE_ESTIMATOR_KERAS_PATH);
				// scoreEstimator.load(SCORE_ESTIMATOR_DL4J_PATH);
				logger.info("Successfully loaded pre-trained score estimator network.");
			}
			final CardsEstimator cardsEstimator = gameSession.getPlayersOfTeam(0).get(0).getCardsEstimator();
			if (cardsEstimator != null) {
				cardsEstimator.loadKerasModel(SCORE_ESTIMATOR_KERAS_PATH);
				// cardsEstimator.load(CARDS_ESTIMATOR_DL4J_PATH);
				logger.info("Successfully loaded pre-trained cards estimator network.");
			}
		}
	}

	private void tearDown() {
		for (Player player : gameSession.getPlayersInInitialPlayingOrder()) {
			player.onSessionFinished();
		}
		final NeuralNetwork scoreEstimator = gameSession.getPlayersOfTeam(0).get(0).getScoreEstimator();
		if (scoreEstimator != null) scoreEstimator.save(SCORE_ESTIMATOR_DL4J_PATH);
		logger.info("Successfully terminated the training process\n");
	}
}
