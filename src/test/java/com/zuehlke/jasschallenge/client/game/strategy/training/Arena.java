package com.zuehlke.jasschallenge.client.game.strategy.training;

import com.google.common.collect.EvictingQueue;
import com.zuehlke.jasschallenge.client.game.*;
import com.zuehlke.jasschallenge.client.game.strategy.GameSessionBuilder;
import com.zuehlke.jasschallenge.client.game.strategy.JassTheRipperJassStrategy;
import com.zuehlke.jasschallenge.client.game.strategy.mcts.NeuralNetwork;
import com.zuehlke.jasschallenge.game.cards.Card;
import com.zuehlke.jasschallenge.game.mode.Mode;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class Arena {

	// The bigger, the bigger the datasets are, and the longer the training takes
	// If it is 4: each experience will be used 4 times.
	private static final int REPLAY_MEMORY_SIZE_FACTOR = 4;

	private final int numTrainingGames;
	private final int numTestingGames;
	private final double improvementThresholdPercentage;
	private final Random random;

	private GameSession gameSession;

	private Queue<INDArray> observations;
	private Queue<INDArray> labels;

	public static final Logger logger = LoggerFactory.getLogger(Arena.class);


	public Arena(int numTrainingGames, int numTestingGames, double improvementThresholdPercentage, int seed) {
		this.numTrainingGames = numTrainingGames;
		this.numTestingGames = numTestingGames;
		this.improvementThresholdPercentage = improvementThresholdPercentage;
		random = new Random(seed);
	}

	public void train(int numEpisodes) {
		setUp();

		for (int i = 0; i < numEpisodes; i++) {
			logger.info("Running episode #{}\n", i);
			final double performance = runEpisode();
			logger.info("After episode #{}, value estimation mcts scored {}% of the points of random playouts mcts", i, performance);
		}

		tearDown();
	}

	public void trainUntilBetterThanRandomPlayouts() {
		setUp();

		double performance = 0;
		for (int i = 0; performance < 100; i++) {
			logger.info("Running episode #{}\n", i);
			performance = runEpisode();
			logger.info("After episode #{}, value estimation mcts scored {}% of the points of random playouts mcts", i, performance);
		}

		tearDown();
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
	private double runEpisode() {
		final JassTheRipperJassStrategy strategy = JassTheRipperJassStrategy.getInstance();

		logger.info("Collecting training examples by self play with MCTS policy improvement\n");
		performMatch(numTrainingGames, true,
				new boolean[]{true, true}, new boolean[]{true, true}, new boolean[]{true, false});

		logger.info("Training the network with the collected examples\n");
		strategy.getNeuralNetwork(true).train(new ArrayList<>(observations), new ArrayList<>(labels));

		logger.info("Pitting the 'naked' networks against each other to see " +
				"if the learning network can score more than {}% of the points of the frozen network\n", improvementThresholdPercentage);
		final double improvement = performMatch(numTestingGames, false,
				new boolean[]{false, false}, new boolean[]{true, true}, new boolean[]{true, false});
		if (improvement > improvementThresholdPercentage) { // if the learning network is significantly better
			strategy.updateNetworks(); // set the frozen network to a copy of the learning network.
			logger.info("The learning network outperformed the frozen network. Updated the frozen network\n");
		}

		logger.info("Testing MCTS with a value estimator against MCTS with random playouts\n");
		return performMatch(numTestingGames, true,
				new boolean[]{true, true}, new boolean[]{true, false}, new boolean[]{true, false});
	}

	/**
	 * Performs a match which can be parametrized along multiple dimensions to test the things we want
	 *
	 * @param numGames
	 * @param collectExperiences
	 * @param mctsEnabled
	 * @param valueEstimatorUsed
	 * @param networkTrainable
	 * @return
	 */
	private double performMatch(int numGames, boolean collectExperiences, boolean[] mctsEnabled, boolean[] valueEstimatorUsed, boolean[] networkTrainable) {
		// MCTS enabled for policy improvement
		gameSession.getTeams().get(0).getPlayers().forEach(player -> player.setMctsEnabled(mctsEnabled[0]));
		gameSession.getTeams().get(1).getPlayers().forEach(player -> player.setMctsEnabled(mctsEnabled[1]));

		// This is necessary to trigger the value estimation by the network inside the MCTS!
		gameSession.getTeams().get(0).getPlayers().forEach(player -> player.setValueEstimaterUsed(valueEstimatorUsed[0]));
		gameSession.getTeams().get(1).getPlayers().forEach(player -> player.setValueEstimaterUsed(valueEstimatorUsed[1]));

		// Only one team's network is trainable, the other one's is frozen
		gameSession.getTeams().get(0).getPlayers().forEach(player -> player.setNetworkTrainable(networkTrainable[0]));
		gameSession.getTeams().get(1).getPlayers().forEach(player -> player.setNetworkTrainable(networkTrainable[1]));

		return playGames(numGames, collectExperiences);
	}

	private double playGames(int numGames, boolean collectExperiences) {
		List<Card> orthogonalCards = null;
		List<Card> cards = Arrays.asList(Card.values());
		Collections.shuffle(cards, random);

		for (int i = 0; i < numGames; i++) {
			logger.info("Running game #{}\n", i);

			orthogonalCards = dealCards(cards, orthogonalCards);
			performTrumpfSelection();
			Result result = playGame(collectExperiences);

			logger.info("Result of game #{}: {}\n", i, result);
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

	private void setUp() {
		logger.info("Setting up the training process\n");
		gameSession = GameSessionBuilder.newSession().createGameSession();

		for (Player player : gameSession.getPlayersInInitialPlayingOrder()) {
			player.onSessionStarted(gameSession);
		}

		int size = numTrainingGames * REPLAY_MEMORY_SIZE_FACTOR;
		// When a new element is added and the queue is full, the head is removed.
		observations = EvictingQueue.create(size);
		labels = EvictingQueue.create(size);
	}

	private void tearDown() {
		for (Player player : gameSession.getPlayersInInitialPlayingOrder()) {
			player.onSessionFinished();
		}
		JassTheRipperJassStrategy.getInstance().getNeuralNetwork(true).save(); // Serialize and save the trained network for later evaluation
		logger.info("Successfully terminated the training process\n");
	}
}
