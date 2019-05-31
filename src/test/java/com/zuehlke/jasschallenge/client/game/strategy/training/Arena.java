package com.zuehlke.jasschallenge.client.game.strategy.training;

import com.google.common.collect.EvictingQueue;
import com.zuehlke.jasschallenge.client.game.*;
import com.zuehlke.jasschallenge.client.game.strategy.GameSessionBuilder;
import com.zuehlke.jasschallenge.client.game.strategy.JassTheRipperJassStrategy;
import com.zuehlke.jasschallenge.client.game.strategy.StrengthLevel;
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
	private final double improvementThresholdFactor;
	private final Random random;

	private GameSession gameSession;

	private Queue<INDArray> observations;
	private Queue<INDArray> labels;

	public static final Logger logger = LoggerFactory.getLogger(Arena.class);

	public Arena(int numTrainingGames, int numTestingGames, double improvementThresholdFactor, int seed) {
		this.numTrainingGames = numTrainingGames;
		this.numTestingGames = numTestingGames;
		this.improvementThresholdFactor = improvementThresholdFactor;
		random = new Random(seed);
	}

	public void train(int numEpisodes) {
		setUp();

		// Only one team's network is trainable, the other one's is frozen
		// This is necessary to trigger the value estimation by the network inside the MCTS!
		gameSession.getTeams().get(0).getPlayers().forEach(player -> player.setNetworkTrainable(true));

		for (int i = 0; i < numEpisodes; i++) {
			logger.info("Running Episode #{}\n", i);
			runEpisode();
		}

		tearDown();
	}

	private void runEpisode() {
		final JassTheRipperJassStrategy strategy = JassTheRipperJassStrategy.getInstance(StrengthLevel.FAST_TEST, StrengthLevel.FAST);

		logger.info("Collecting training examples by self play with MCTS policy improvement\n");
		strategy.setMctsEnabled(true); // MCTS enabled for policy improvement
		playGames(numTrainingGames);

		logger.info("Training the network with the collected examples\n");
		strategy.getNeuralNetwork(true).train(new ArrayList<>(observations), new ArrayList<>(labels));

		logger.info("Pitting the 'naked' networks against each other to see if the frozen one can already be beaten by a high enough margin ({})\n", improvementThresholdFactor);
		strategy.setMctsEnabled(false); // MCTS disabled to see the raw network performance
		final double improvement = playGames(numTestingGames);
		if (improvement > improvementThresholdFactor) { // if the learning network is significantly better
			strategy.updateNetworks(); // set the frozen network to a copy of the learning network.
			logger.info("The learning network outperformed the frozen network. Updated the frozen network.");
		}
	}

	private double playGames(int numGames) {
		List<Card> orthogonalCards = null;
		List<Card> cards = Arrays.asList(Card.values());
		Collections.shuffle(cards, random);

		for (int i = 0; i < numGames; i++) {
			logger.info("Running game #{}\n", i);

			orthogonalCards = dealCards(cards, orthogonalCards);
			performTrumpfSelection();
			Result result = playGame();

			logger.info("Result of game #{}: {}\n", i, result);
		}
		gameSession.updateResult(); // normally called within gameSession.startNewGame(), so we need it at the end again
		final Result result = gameSession.getResult();
		logger.info("Aggregated result of the {} games played: {}\n", numGames, result);
		double improvement = Math.round(100.0 * result.getTeamAScore().getScore() / result.getTeamBScore().getScore()) / 100.0;
		logger.info("The learning network performed " + improvement + " in comparison with the frozen network.");

		logger.info("Resetting the result so we can get a fresh start afterwards");
		gameSession.resetResult();

		return improvement;
	}

	/**
	 * Plays a game and appends the made observations with the final point difference to the provided parameters (observations and labels)
	 */
	private Result playGame() {
		HashMap<INDArray, Player> observationsWithPlayer = new HashMap<>();
		Game game = gameSession.getCurrentGame();
		while (!game.gameFinished()) {
			final Round round = game.getCurrentRound();
			while (!round.roundFinished()) {
				final Player player = game.getCurrentPlayer();
				final Move move = player.makeMove(gameSession);
				gameSession.makeMove(move);
				player.onMoveMade(move, gameSession);

				if (JassTheRipperJassStrategy.getInstance().isMctsEnabled()) // NOTE: only collect high quality experiences
					observationsWithPlayer.put(NeuralNetwork.getObservation(game), player);
			}
			gameSession.startNextRound();
		}

		if (JassTheRipperJassStrategy.getInstance().isMctsEnabled())
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
			logger.info("Dealing the 'normal' cards: {}", normalCards);
			gameSession.dealCards(normalCards);

			// And prepare orthogonal cards
			orthogonalCards = new ArrayList<>(normalCards);
			Collections.rotate(orthogonalCards, 9); // rotate list so that the opponents now have the cards we had before and vice versa --> this ensures fair testing!
		} else { // if we have orthogonal cards
			logger.info("Dealing the 'orthogonal' cards: {}", orthogonalCards);
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
		logger.info("Setting up the training process.");
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
		logger.info("Successfully terminated the training process.");
	}
}
