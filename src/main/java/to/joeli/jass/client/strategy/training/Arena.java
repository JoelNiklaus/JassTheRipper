package to.joeli.jass.client.strategy.training;

import com.google.common.collect.EvictingQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import to.joeli.jass.client.game.*;
import to.joeli.jass.client.strategy.config.Config;
import to.joeli.jass.client.strategy.helpers.*;
import to.joeli.jass.game.cards.Card;
import to.joeli.jass.game.mode.Mode;

import java.util.*;


// TODO refactor this! it is way too big: Maybe take out helper method or divide into two classes with different responsibilities: data collection and experiments
public class Arena {

	// The bigger, the bigger the datasets are, and the longer the training takes
	// If it is 4: each experience will be used 4 times.
	// Should not be bigger than 32 because it might result in OutOfMemoryErrors
	private static final int REPLAY_MEMORY_SIZE_FACTOR = 4; // Standard: 4, 8, 16

	private static final boolean SUPERVISED_PRETRAINING_ENABLED = true;
	private static final boolean DATA_AUGMENTATION_ENABLED = true;
	public static final String BASE_PATH = "src/main/resources/";
	public static final String DATASETS_BASE_PATH = BASE_PATH + "datasets/";
	public static final String MODELS_BASE_PATH = BASE_PATH + "models/";
	public static final String SCORE_ESTIMATOR_KERAS_PATH = MODELS_BASE_PATH + "score_estimator.hdf5";
	public static final String CARDS_ESTIMATOR_KERAS_PATH = MODELS_BASE_PATH + "cards_estimator.hdf5";
	private static final int NUM_EPISODES = 1; // TEST: 1
	private static final int NUM_TRAINING_GAMES = 2; // Should be an even number, TEST: 2
	private static final int NUM_TESTING_GAMES = 2; // Should be an even number, TEST: 2
	// If the learning network scores more points than the frozen network times this factor, the frozen network gets replaced
	public static final double IMPROVEMENT_THRESHOLD_PERCENTAGE = 105;
	public static final int SEED = 42;
	public static final double TOTAL_POINTS = 157.0; // INFO: We disregard Matchbonus for simplicity here

	private final int numTrainingGames;
	private final int numTestingGames;
	private final double improvementThresholdPercentage;
	private final Random random;

	private GameSession gameSession;

	// The input for the neural networks, a representation of the game
	private Queue<double[][]> cardsFeatures;
	private Queue<double[][]> scoreFeatures;
	// The labels for the score estimator, the score at the end of the game
	private Queue<Double> scoreTargets;
	// The labels for the cards estimator, the actual cards the other players had
	private Queue<int[][]> cardsTargets;

	public static final Logger logger = LoggerFactory.getLogger(Arena.class);

	public static void main(String[] args) {
		final Arena arena = new Arena(NUM_TRAINING_GAMES, NUM_TESTING_GAMES, IMPROVEMENT_THRESHOLD_PERCENTAGE, SEED);

		logger.info("Collecting a dataset of games played with random playouts\n");
		arena.collectDataSetRandomPlayouts(REPLAY_MEMORY_SIZE_FACTOR * 10);

		logger.info("Pre-training the neural networks\n");
		arena.preTrainNetworks();

		//logger.info("Training the networks with self-play\n");
		//arena.trainForNumEpisodes(1000);
	}


	public Arena(int numTrainingGames, int numTestingGames, double improvementThresholdPercentage, int seed) {
		// CudaEnvironment.getInstance().getConfiguration().allowMultiGPU(true); // NOTE: This might have to be enabled on the server

		this.numTrainingGames = numTrainingGames;
		this.numTestingGames = numTestingGames;
		this.improvementThresholdPercentage = improvementThresholdPercentage;
		random = new Random(seed);
	}


	public Arena(GameSession gameSession) {
		this(NUM_TRAINING_GAMES, NUM_TESTING_GAMES, IMPROVEMENT_THRESHOLD_PERCENTAGE, SEED);
		this.gameSession = gameSession;
	}

	private void preTrainNetworks() {
		setUp();

		Config[] configs = {
				new Config(true, true, true, true, true),
				new Config(true, false, false)
		};
		gameSession.setConfigs(configs);

		final Player cardsEstimatorPlayer = gameSession.getFirstPlayerWithUsedCardsEstimator(true);
		cardsEstimatorPlayer.getCardsEstimator().train(TrainMode.PRE_TRAIN);
		final Player scoreEstimatorPlayer = gameSession.getFirstPlayerWithUsedScoreEstimator(true);
		scoreEstimatorPlayer.getScoreEstimator().train(TrainMode.PRE_TRAIN);
	}

	public void trainForNumEpisodes(int numEpisodes) {
		setUp();

		List<Double> history = new ArrayList<>();
		for (int i = 0; i < numEpisodes; i++) {
			history.add(i, runEpisode(i));
		}
		logger.info("Performance over the episodes:\n{}", history);
	}

	public void trainUntilBetterThanRandomPlayouts() {
		setUp();

		List<Double> history = new ArrayList<>(Collections.singletonList(0.0));
		for (int i = 0; history.get(i) < 100; i++) {
			history.add(i, runEpisode(i));
		}
		logger.info("Performance over the episodes:\n{}", history);
	}

	public void collectDataSetRandomPlayouts(int numGames) {
		setUp();

		runMCTSWithRandomPlayout(random, numGames);
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
		Player cardsEstimatorPlayer = gameSession.getFirstPlayerWithUsedCardsEstimator(true);
		Player scoreEstimatorPlayer = gameSession.getFirstPlayerWithUsedScoreEstimator(true);
		cardsEstimatorPlayer.getCardsEstimator().train(TrainMode.SELF_PLAY);
		scoreEstimatorPlayer.getScoreEstimator().train(TrainMode.SELF_PLAY);

		logger.info("Pitting the 'naked' networks against each other to see " +
				"if the learning network can score more than {}% of the points of the frozen network\n", improvementThresholdPercentage);
		final double improvement = runOnlyNetworks(random, numTestingGames);
		if (improvement > improvementThresholdPercentage) { // if the learning network is significantly better
			cardsEstimatorPlayer = gameSession.getFirstPlayerWithUsedCardsEstimator(false);
			scoreEstimatorPlayer = gameSession.getFirstPlayerWithUsedScoreEstimator(false);
			cardsEstimatorPlayer.getCardsEstimator().loadWeightsOfTrainableNetwork();
			scoreEstimatorPlayer.getScoreEstimator().loadWeightsOfTrainableNetwork();
			logger.info("The learning network outperformed the frozen network. Updated the frozen network\n");
		}

		logger.info("Testing MCTS with a score estimator against MCTS with random playouts\n");
		final double performance = runMCTSWithScoreEstimatorAgainstMCTSWithRandomPlayout(random, numTestingGames);

		logger.info("After episode #{}, score estimation MCTS scored {}% of the points of random playout MCTS", episodeNumber, performance);
		return performance;
	}

	public double runMatchWithConfigs(Random random, int numGames, Config[] configs) {
		setUp();
		final double performance = performMatch(random, numGames, TrainMode.EVALUATION, configs);
		return performance;
	}

	private double runMCTSWithRandomPlayout(Random random, int numGames) {
		Config[] configs = {
				new Config(true, false, false),
				new Config(true, false, false)
		};
		return performMatch(random, numGames, TrainMode.PRE_TRAIN, configs);
	}

	private double runMCTSWithScoreEstimators(Random random, int numGames) {
		Config[] configs = {
				new Config(true, true, true),
				new Config(true, true, false)
		};
		return performMatch(random, numGames, TrainMode.SELF_PLAY, configs);
	}

	private double runOnlyNetworks(Random random, int numGames) {
		Config[] configs = {
				new Config(false, true, true),
				new Config(false, true, false)
		};
		return performMatch(random, numGames, TrainMode.NONE, configs);
	}

	private double runMCTSWithScoreEstimatorAgainstMCTSWithRandomPlayout(Random random, int numGames) {
		Config[] configs = {
				new Config(true, true, true),
				new Config(true, false, false)
		};
		return performMatch(random, numGames, TrainMode.SELF_PLAY, configs);
	}

	/**
	 * Performs a match which can be parametrized along multiple dimensions to test the things we want
	 *
	 * @param numGames
	 * @param trainMode
	 * @param configs
	 * @return
	 */
	private double performMatch(Random random, int numGames, TrainMode trainMode, Config[] configs) {
		gameSession.setConfigs(configs);

		return playGames(random, numGames, trainMode);
	}

	private double playGames(Random random, int numGames, TrainMode trainMode) {
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
				IOHelper.saveData(new DataSet(cardsFeatures, scoreFeatures, cardsTargets, scoreTargets), trainMode, name);
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

	public Result playGame(boolean savingData) {
		Game game = gameSession.getCurrentGame();

		HashMap<double[][], Player> scoreFeaturesForPlayer = new HashMap<>(); // The double[][] is the key because it is unique
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
						cardsFeatures.addAll(NeuralNetworkHelper.getAnalogousCardsFeatures(game, cardKnowledge));
						cardsTargets.addAll(analogousCardsTargets);

						NeuralNetworkHelper.getAnalogousScoreFeatures(game).forEach(feature -> scoreFeaturesForPlayer.put(feature, player));
					} else {
						cardsFeatures.add(NeuralNetworkHelper.getCardsFeatures(game, cardKnowledge));
						cardsTargets.add(cardsTarget);

						scoreFeaturesForPlayer.put(NeuralNetworkHelper.getScoreFeatures(game), player);
					}
				}
			}
			gameSession.startNextRound();
		}

		if (savingData) {
			if (scoreFeaturesForPlayer.size() != 24 * 36) throw new AssertionError();
			for (Map.Entry<double[][], Player> entry : scoreFeaturesForPlayer.entrySet()) {
				scoreFeatures.add(entry.getKey());
				scoreTargets.add(NeuralNetworkHelper.getScoreTarget(game, entry.getValue()));
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

		// 36: Number of Cards in a game
		int size = 36 * REPLAY_MEMORY_SIZE_FACTOR;
		if (DATA_AUGMENTATION_ENABLED)
			size *= 24; // 24: Number of color permutations (data augmentation)
		// When a new element is added and the queue is full, the head is removed.
		cardsFeatures = EvictingQueue.create(size);
		scoreFeatures = EvictingQueue.create(size);
		cardsTargets = EvictingQueue.create(size);
		scoreTargets = EvictingQueue.create(size);
	}
}
