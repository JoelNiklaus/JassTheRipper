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


// TODO refactor this! it is way too big: Maybe take out helper method or divide into two classes with different responsibilities: data collection and experiments
public class Arena {

	// The bigger, the bigger the datasets are, and the longer the training takes
	// If it is 4: each experience will be used 4 times.
	// Should not be bigger than 32 because it might result in OutOfMemoryErrors
	private static final int REPLAY_MEMORY_SIZE_FACTOR = 4; // Standard: 4, 8, 16

	private static final boolean SUPERVISED_PRETRAINING_ENABLED = true;
	private static final boolean DATA_AUGMENTATION_ENABLED = true;
	private static final int NUM_EPISODES = 1; // TEST: 1
	private static final int NUM_TRAINING_GAMES = 1; // Should be an even number and a multiple of REOLAY_MEMORY_SIZE_FACTOR, TEST: 2
	private static final int NUM_TESTING_GAMES = 1;  // Should be an even number and a multiple of REOLAY_MEMORY_SIZE_FACTOR, TEST: 2
	// If the learning network scores more points than the frozen network times this factor, the frozen network gets replaced
	public static final double IMPROVEMENT_THRESHOLD_PERCENTAGE = 105;
	public static final int SEED = 42;
	public static final float TOTAL_POINTS = 157.0f; // INFO: We disregard Matchbonus for simplicity here

	private static final boolean cardsEstimatorUsed = false;
	private static final boolean scoreEstimatorUsed = true;

	private final int numTrainingGames;
	private final int numTestingGames;
	private final double improvementThresholdPercentage;
	private final Random random;

	private GameSession gameSession;

	private CardsDataSet cardsDataSet;
	private ScoreDataSet scoreDataSet;

	public static final Logger logger = LoggerFactory.getLogger(Arena.class);

	public static void main(String[] args) {
		final Arena arena = new Arena(NUM_TRAINING_GAMES, NUM_TESTING_GAMES, IMPROVEMENT_THRESHOLD_PERCENTAGE, SEED);

		logger.info("Training the networks with self-play\n");
		arena.trainForNumEpisodes(1000);
	}


	public Arena(int numTrainingGames, int numTestingGames, double improvementThresholdPercentage, int seed) {
		// CudaEnvironment.getInstance().getConfiguration().allowMultiGPU(true); // NOTE: This might have to be enabled on the server

		this.numTrainingGames = REPLAY_MEMORY_SIZE_FACTOR * numTrainingGames;
		this.numTestingGames = REPLAY_MEMORY_SIZE_FACTOR * numTestingGames;
		this.improvementThresholdPercentage = improvementThresholdPercentage;
		random = new Random(seed);

		setUp();
	}


	public Arena(GameSession gameSession) {
		this(NUM_TRAINING_GAMES, NUM_TESTING_GAMES, IMPROVEMENT_THRESHOLD_PERCENTAGE, SEED);
		this.gameSession = gameSession;
	}

	private void setUp() {
		logger.info("Setting up the training process\n");
		gameSession = GameSessionBuilder.newSession().createGameSession();

		// 36: Number of Cards in a game
		int size = 36 * REPLAY_MEMORY_SIZE_FACTOR;
		if (DATA_AUGMENTATION_ENABLED)
			size *= 24; // 24: Number of color permutations (data augmentation)
		// The Datasets operate with an evicting queue. When a new element is added and the queue is full, the head is removed.
		cardsDataSet = new CardsDataSet(size);
		scoreDataSet = new ScoreDataSet(size);

		String path = DataSet.BASE_PATH + zeroPadded(0);
		if (!new File(path).exists()) {
			logger.info("Collecting a dataset of games played with random playouts\n");
			runMCTSWithRandomPlayout(random, REPLAY_MEMORY_SIZE_FACTOR * 10);
		}

		logger.info("Pre-training the neural networks\n");
		trainNetworks(0);

		logger.info("Loading the pre_trained networks into memory\n");
		Config[] configs = {
				new Config(true, cardsEstimatorUsed, true, scoreEstimatorUsed, true),
				new Config(true, cardsEstimatorUsed, false, scoreEstimatorUsed, false)
		};
		gameSession.setConfigs(configs);
		loadNetworks(0, true);
		loadNetworks(0, false);
	}

	public double runMatchWithConfigs(Random random, int numGames, Config[] configs) {
		return performMatch(random, numGames, TrainMode.EVALUATION, -1, configs);
	}


	public void trainForNumEpisodes(int numEpisodes) {
		List<Double> history = new ArrayList<>();
		history.add(0.0); // no performance for pretraining
		for (int i = 1; i < numEpisodes; i++) {
			history.add(i, runEpisode(i));
		}
		logger.info("Performance over the episodes:\n{}", history);
	}

	public void trainUntilBetterThanRandomPlayouts() {
		List<Double> history = new ArrayList<>(Collections.singletonList(0.0));
		history.add(0.0); // no performance for pretraining
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
	private double runEpisode(int episodeNumber) {
		logger.info("Running episode #{}\n", episodeNumber);

		logger.info("Collecting training examples by self play with estimator enhanced MCTS\n");
		runMCTSWithEstimators(random, numTrainingGames, episodeNumber);

		logger.info("Training the trainable networks with the collected examples\n");
		trainNetworks(episodeNumber);

		logger.info("Loading the newly trained trainable networks into memory\n");
		loadNetworks(episodeNumber, true);

		logger.info("Pitting the 'naked' networks against each other to see " +
				"if the learning network can score more than {}% of the points of the frozen network\n", improvementThresholdPercentage);
		final double improvement = runOnlyNetworks(random, numTestingGames);
		if (improvement > improvementThresholdPercentage) { // if the learning network is significantly better
			logger.info("The learning network outperformed the frozen network. Updating the frozen network\n");
			loadNetworks(episodeNumber, false);
		} else {
			logger.info("The learning network failed to outperform the frozen network. Training for another episode\n");
		}

		logger.info("Testing MCTS with estimators against MCTS without\n");
		final double performance = runMCTSWithEstimatorsAgainstMCTSWithoutEstimators(random, numTestingGames, episodeNumber);

		logger.info("After episode #{}, estimator enhanced MCTS scored {}% of the points of regular MCTS\n", episodeNumber, performance);
		return performance;
	}

	/**
	 * Updates the networks: loads the exported models into memory.
	 */
	private void loadNetworks(int episodeNumber, boolean trainable) {
		if (cardsEstimatorUsed) {
			gameSession.getPlayersInInitialPlayingOrder().forEach(player -> {
				if (player.getConfig().isCardsEstimatorTrainable() == trainable)
					player.getCardsEstimator().loadModel(episodeNumber);
			});
		}
		if (scoreEstimatorUsed) {
			gameSession.getPlayersInInitialPlayingOrder().forEach(player -> {
				if (player.getConfig().isScoreEstimatorTrainable() == trainable)
					player.getScoreEstimator().loadModel(episodeNumber);
			});
		}
	}

	/**
	 * Trains the networks
	 */
	private void trainNetworks(int episodeNumber) {
		// NOTE: The networks of team 0 are trainable. Both players of the same team normally have the same network references
		if (cardsEstimatorUsed)
			NeuralNetwork.train(episodeNumber, NetworkType.CARDS);
		if (scoreEstimatorUsed)
			NeuralNetwork.train(episodeNumber, NetworkType.SCORE);
	}


	private double runMCTSWithRandomPlayout(Random random, int numGames) {
		Config[] configs = {
				new Config(true, cardsEstimatorUsed, false, scoreEstimatorUsed, false),
				new Config(true, cardsEstimatorUsed, false, scoreEstimatorUsed, false)
		};
		return performMatch(random, numGames, TrainMode.PRE_TRAIN, 0, configs);
	}

	private double runMCTSWithEstimators(Random random, int numGames, int episodeNumber) {
		Config[] configs = {
				new Config(true, cardsEstimatorUsed, true, scoreEstimatorUsed, true),
				new Config(true, cardsEstimatorUsed, false, scoreEstimatorUsed, false)
		};
		return performMatch(random, numGames, TrainMode.SELF_PLAY, episodeNumber, configs);
	}

	private double runOnlyNetworks(Random random, int numGames) {
		// TODO How can cards estimators be pitted against each other so that we can measure which one is better (trainable or frozen)
		//  without (or minimal) outside influence like the MCTS
		Config[] configs = {
				new Config(false, cardsEstimatorUsed, true, scoreEstimatorUsed, true),
				new Config(false, cardsEstimatorUsed, false, scoreEstimatorUsed, false)
		};
		return performMatch(random, numGames, TrainMode.NONE, -1, configs);
	}

	private double runMCTSWithEstimatorsAgainstMCTSWithoutEstimators(Random random, int numGames, int episodeNumber) {
		Config[] configs = {
				new Config(true, cardsEstimatorUsed, true, scoreEstimatorUsed, true),
				new Config(true, false, false, false, false)
		};
		return performMatch(random, numGames, TrainMode.SELF_PLAY, episodeNumber, configs);
	}

	private static String zeroPadded(int number) {
		return String.format("%04d", number);
	}

	/**
	 * Performs a match which can be parametrized along multiple dimensions to test the things we want
	 *
	 * @param numGames
	 * @param trainMode
	 * @param configs
	 * @return
	 */
	private double performMatch(Random random, int numGames, TrainMode trainMode, int episodeNumber, Config[] configs) {
		gameSession.setConfigs(configs);

		return playGames(random, numGames, trainMode, episodeNumber);
	}

	/**
	 * Simulates a number of games and returns the performance of Team A in comparison with Team B.
	 *
	 * @param random
	 * @param numGames
	 * @param trainMode
	 * @return
	 */
	private double playGames(Random random, int numGames, TrainMode trainMode, int episodeNumber) {
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

			if (trainMode.isSavingData() && i % REPLAY_MEMORY_SIZE_FACTOR == 0) {
				final String name = zeroPadded(i - REPLAY_MEMORY_SIZE_FACTOR) + "-" + zeroPadded(i);
				IOHelper.saveData(cardsDataSet, scoreDataSet, zeroPadded(episodeNumber), name);
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
			if (scoreFeaturesForPlayer.size() != 24 * 36) throw new AssertionError();
			for (Map.Entry<float[][], Player> entry : scoreFeaturesForPlayer.entrySet()) {
				scoreDataSet.addFeature(entry.getKey());
				scoreDataSet.addTarget(NeuralNetworkHelper.getScoreTarget(game, entry.getValue()));
			}
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
