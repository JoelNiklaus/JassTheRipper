package to.joeli.jass.client.strategy.training;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import to.joeli.jass.client.game.*;
import to.joeli.jass.client.strategy.config.Config;
import to.joeli.jass.client.strategy.helpers.*;
import to.joeli.jass.client.strategy.training.data.CardsDataSet;
import to.joeli.jass.client.strategy.training.data.DataSet;
import to.joeli.jass.client.strategy.training.data.ScoreDataSet;
import to.joeli.jass.client.strategy.training.networks.NeuralNetwork;
import to.joeli.jass.game.cards.Card;
import to.joeli.jass.game.mode.Mode;

import java.io.File;
import java.util.*;

import static to.joeli.jass.client.strategy.training.data.DataSet.zeroPadded;

public class Arena {

	private static final boolean DATA_AUGMENTATION_ENABLED = true;
	private static final int NUM_EPISODES = 1000; // TEST: 1

	private static final int NUM_GAMES_TEST_SET = 20;
	private static final int NUM_GAMES_TRAIN_SET = 100;
	private static final int NUM_GAMES_VAL_SET = 10;
	// TEST: 2, Needs to be an even number because of fairTournamentMode!
	private static final int NUM_EVALUATION_GAMES = 10;

	// If the learning network scores more points than the frozen network times this factor, the frozen network gets replaced
	public static final double IMPROVEMENT_THRESHOLD_PERCENTAGE = 105;
	public static final int SEED = 42;
	public static final float TOTAL_POINTS = 157.0f; // INFO: We disregard Matchbonus for simplicity here

	private static final boolean CARDS_ESTIMATOR_USED = false;
	private static final boolean SCORE_ESTIMATOR_USED = false;


	private final double improvementThresholdPercentage;
	private final Random random;

	private double minValLossOld = Double.MAX_VALUE;
	private double testLossOld = Double.MAX_VALUE;

	private GameSession gameSession;

	private CardsDataSet cardsDataSet;
	private ScoreDataSet scoreDataSet;

	public static final Logger logger = LoggerFactory.getLogger(Arena.class);
	public static final Logger experimentLogger = LoggerFactory.getLogger("Experiment");

	public static void main(String[] args) {
		final Arena arena = new Arena(IMPROVEMENT_THRESHOLD_PERCENTAGE, SEED);

		logger.info("Training the networks with self-play\n");
		arena.trainForNumEpisodes(NUM_EPISODES);
	}

	public Arena(double improvementThresholdPercentage, int seed) {
		// CudaEnvironment.getInstance().getConfiguration().allowMultiGPU(true); // NOTE: This might have to be enabled on the server

		this.improvementThresholdPercentage = improvementThresholdPercentage;
		random = new Random(seed);

		setUp();
	}

	public Arena(GameSession gameSession) {
		this(IMPROVEMENT_THRESHOLD_PERCENTAGE, SEED);
		this.gameSession = gameSession;
	}

	private void setUp() {
		logger.info("Setting up the training process\n");
		gameSession = GameSessionBuilder.newSession().createGameSession();

		// The Datasets operate with an evicting queue. When a new element is added and the queue is full, the head is removed.
		cardsDataSet = new CardsDataSet(computeDataSetSize());
		scoreDataSet = new ScoreDataSet(computeDataSetSize());

		String path = DataSet.getEpisodePath(0);
		if (!new File(path).exists()) {
			logger.info("No dataset found. Collecting a dataset of games played using MCTS with random playouts\n");
			runMCTSWithRandomPlayout(random);
		}

		logger.info("Existing dataset found. Pre-training the neural networks\n");
		trainNetworks(0);

		logger.info("Loading the pre-trained networks into memory\n");
		Config[] configs = {
				new Config(true, CARDS_ESTIMATOR_USED, true, SCORE_ESTIMATOR_USED, true),
				new Config(true, CARDS_ESTIMATOR_USED, false, SCORE_ESTIMATOR_USED, false)
		};
		gameSession.setConfigs(configs);
		loadNetworks(0, true);
		loadNetworks(0, false);
	}

	public void trainForNumEpisodes(int numEpisodes) {
		List<Double> history = new ArrayList<>();
		history.add(0.0); // no performance for pre-training
		for (int i = 1; i < numEpisodes; i++) {
			history.add(i, runEpisode(i));
		}
		logger.info("Performance over the episodes:\n{}", history);
	}

	public void trainUntilBetterThanRandomPlayouts() {
		List<Double> history = new ArrayList<>(Collections.singletonList(0.0));
		history.add(0.0); // no performance for pre-training
		for (int i = 1; history.get(i) < 100; i++) {
			history.add(i, runEpisode(i));
		}
		logger.info("Performance over the episodes:\n{}", history);
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
	private double runEpisode(int episode) {
		logger.info("Running episode #{}\n", episode);
		experimentLogger.info("\n===========================\nEpisode #{}\n===========================", episode);

		logger.info("Collecting training examples by self play with estimator enhanced MCTS\n");
		runMCTSWithEstimators(random, episode);

		logger.info("Training the trainable networks with the collected examples\n");
		trainNetworks(episode);

		logger.info("Loading the newly trained trainable networks into memory\n");
		loadNetworks(episode, true);

		if (SCORE_ESTIMATOR_USED) {
			logger.info("Pitting the 'naked' score estimators against each other to see " +
					"if the learning network can score more than {}% of the points of the frozen network\n", improvementThresholdPercentage);
			final boolean wasImproved = runOnlyNetworks(random) > improvementThresholdPercentage;
			updateNetworks(episode, wasImproved);
		}
		if (CARDS_ESTIMATOR_USED) {
			logger.info("Checking if the minimum validation loss of the current cards estimator is less than the old one\n");
			double minValLoss = IOHelper.INSTANCE.getQuantifierFromFile("min_val_loss.txt");
			experimentLogger.info("\nMinimum Validation Loss after training network: {}", minValLoss);
			minValLossOld = minValLoss;
			double testLoss = IOHelper.INSTANCE.getQuantifierFromFile("test_loss.txt");
			experimentLogger.info("\nTest Loss after training network: {}", testLoss);
			updateNetworks(episode, testLoss < testLossOld);
			testLossOld = testLoss;
		}

		logger.info("Testing MCTS with estimators against basic MCTS with random playout\n");
		final double performance = runMCTSWithEstimatorsAgainstMCTSWithoutEstimators(random);
		experimentLogger.info("\nEstimator enhanced MCTS scored {}% of the points of regular MCTS", performance);

		logger.info("After episode #{}, estimator enhanced MCTS scored {}% of the points of regular MCTS\n", episode, performance);
		return performance;
	}

	private void updateNetworks(int episodeNumber, boolean wasImproved) {
		if (wasImproved) { // if the learning network is significantly better
			logger.info("The learning network outperformed the frozen network. Updating the frozen network\n");
			loadNetworks(episodeNumber, false);
		} else {
			logger.info("The learning network failed to outperform the frozen network. Training for another episode\n");
		}
	}

	/**
	 * Loads the exported models into memory.
	 */
	private void loadNetworks(int episodeNumber, boolean trainable) {
		if (CARDS_ESTIMATOR_USED) {
			gameSession.getPlayersInInitialPlayingOrder().forEach(player -> {
				if (player.getConfig().isCardsEstimatorTrainable() == trainable)
					player.getCardsEstimator().loadModel(episodeNumber);
			});
		}
		if (SCORE_ESTIMATOR_USED) {
			gameSession.getPlayersInInitialPlayingOrder().forEach(player -> {
				if (player.getConfig().isScoreEstimatorTrainable() == trainable)
					player.getScoreEstimator().loadModel(episodeNumber);
			});
		}
	}

	/**
	 * Trains the networks
	 */
	private void trainNetworks(int episode) {
		// NOTE: The networks of team 0 are trainable. Both players of the same team normally have the same network references
		if (CARDS_ESTIMATOR_USED)
			NeuralNetwork.train(episode, NetworkType.CARDS);
		if (SCORE_ESTIMATOR_USED)
			NeuralNetwork.train(episode, NetworkType.SCORE);
	}

	public double runMatchWithConfigs(Random random, Config[] configs) {
		return performMatch(random, TrainMode.EVALUATION, -1, configs);
	}

	private double runOnlyNetworks(Random random) {
		Config[] configs = {
				new Config(false, CARDS_ESTIMATOR_USED, true, SCORE_ESTIMATOR_USED, true),
				new Config(false, CARDS_ESTIMATOR_USED, false, SCORE_ESTIMATOR_USED, false)
		};
		return performMatch(random, TrainMode.EVALUATION, -1, configs);
	}

	private double runMCTSWithEstimatorsAgainstMCTSWithoutEstimators(Random random) {
		Config[] configs = {
				new Config(true, CARDS_ESTIMATOR_USED, true, SCORE_ESTIMATOR_USED, true),
				new Config(true, false, false, false, false)
		};
		return performMatch(random, TrainMode.EVALUATION, -1, configs);
	}

	private double runMCTSWithRandomPlayout(Random random) {
		Config[] configs = {
				new Config(true, false, false, false, false),
				new Config(true, false, false, false, false)
		};
		return performMatch(random, TrainMode.DATA_COLLECTION, 0, configs);
	}

	private double runMCTSWithEstimators(Random random, int episode) {
		Config[] configs = {
				new Config(true, CARDS_ESTIMATOR_USED, true, SCORE_ESTIMATOR_USED, true),
				new Config(true, CARDS_ESTIMATOR_USED, false, SCORE_ESTIMATOR_USED, false)
		};
		return performMatch(random, TrainMode.DATA_COLLECTION, episode, configs);
	}

	/**
	 * Performs a match which can be parametrized along multiple dimensions to test the things we want
	 *
	 * @param trainMode
	 * @param configs
	 * @return
	 */
	private double performMatch(Random random, TrainMode trainMode, int episode, Config[] configs) {
		gameSession.setConfigs(configs);

		if (trainMode == TrainMode.DATA_COLLECTION) {
			logger.info("Collecting training set\n");
			playGames(random, NUM_GAMES_TRAIN_SET, trainMode, "train/", episode);

			logger.info("Collecting validation set\n");
			playGames(random, NUM_GAMES_VAL_SET, trainMode, "val/", episode);

			logger.info("Collecting test set\n");
			playGames(random, NUM_GAMES_TEST_SET, trainMode, "test/", episode);
		}
		if (trainMode == TrainMode.EVALUATION)
			return playGames(random, NUM_EVALUATION_GAMES, trainMode, null, episode);
		return 0;
	}

	/**
	 * Simulates a number of games and returns the performance of Team A in comparison with Team B.
	 *
	 * @param random
	 * @param numGames
	 * @param trainMode
	 * @return
	 */
	private double playGames(Random random, int numGames, TrainMode trainMode, String dataSetType, int episode) {
		List<Card> orthogonalCards = null;
		List<Card> cards = Arrays.asList(Card.values());
		Collections.shuffle(cards, random);

		for (int i = 1; i <= numGames; i++) {
			logger.info("Running game #{}\n", i);

			if (trainMode.isFairTournamentModeEnabled())
				orthogonalCards = dealCards(cards, orthogonalCards);
			else {
				Collections.shuffle(cards, random);
				gameSession.dealCards(cards);
			}

			performTrumpfSelection();

			Result result = playGame(trainMode.isSavingData());

			logger.info("Result of game #{}: {}\n", i, result);

			if (trainMode.isSavingData())
				IOHelper.INSTANCE.saveData(cardsDataSet, scoreDataSet, episode, dataSetType, zeroPadded(i));
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
	 * Simulates a game being played. The gameSession can be configured in many different ways to allow different simulations.
	 *
	 * @param savingData
	 * @return
	 */
	public Result playGame(boolean savingData) {
		Game game = gameSession.getCurrentGame();

		HashMap<float[][], Player> scoreFeaturesForPlayer = new HashMap<>(); // The float[][] is the key because it is unique
		int[][] cardsTarget = null;
		List<int[][]> analogousCardsTargets = new ArrayList<>();
		if (savingData) {
			if (DATA_AUGMENTATION_ENABLED) {
				analogousCardsTargets = NeuralNetworkHelper.getAnalogousCardsTargets(game);
			} else {
				cardsTarget = NeuralNetworkHelper.getCardsTarget(game);
			}
		}

		while (!game.gameFinished()) {
			final Round round = game.getCurrentRound();
			while (!round.roundFinished()) {
				final Player player = game.getCurrentPlayer();
				final Move move = player.makeMove(gameSession);
				gameSession.makeMove(move);
				player.onMoveMade(move);

				if (savingData) {
					final Map<Card, Distribution> cardKnowledge = CardKnowledgeBase.initCardKnowledge(game, game.getCurrentPlayer().getCards());
					if (DATA_AUGMENTATION_ENABLED) {
						// INFO: Because the permutations are always in the same order we can just add the analogous features and targets. They
						cardsDataSet.addFeatures(NeuralNetworkHelper.getAnalogousCardsFeatures(game, cardKnowledge));
						cardsDataSet.addTargets(analogousCardsTargets);

						NeuralNetworkHelper.getAnalogousScoreFeatures(game).forEach(feature -> scoreFeaturesForPlayer.put(feature, player));
					} else {
						cardsDataSet.addFeature(NeuralNetworkHelper.getCardsFeatures(game, cardKnowledge));
						cardsDataSet.addTarget(cardsTarget);

						scoreFeaturesForPlayer.put(NeuralNetworkHelper.getScoreFeatures(game), player);
					}
				}
			}
			gameSession.startNextRound();
		}

		if (savingData) {
			if (scoreFeaturesForPlayer.size() != computeDataSetSize()) throw new AssertionError();
			for (Map.Entry<float[][], Player> entry : scoreFeaturesForPlayer.entrySet()) {
				scoreDataSet.addFeature(entry.getKey());
				scoreDataSet.addTarget(NeuralNetworkHelper.getScoreTarget(game, entry.getValue()));
			}
		}

		return game.getResult();
	}

	private int computeDataSetSize() {
		// 36: Number of Cards in a game
		int size = 36;
		if (DATA_AUGMENTATION_ENABLED)
			size *= 24; // 24: Number of color permutations
		return size;
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


	/**
	 * Organizes the trumpf selection part of the simulation.
	 */
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
}
