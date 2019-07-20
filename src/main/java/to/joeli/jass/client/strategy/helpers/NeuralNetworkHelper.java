package to.joeli.jass.client.strategy.helpers;

import com.google.common.collect.Collections2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import to.joeli.jass.client.game.Game;
import to.joeli.jass.client.game.Move;
import to.joeli.jass.client.game.Player;
import to.joeli.jass.client.strategy.training.Arena;
import to.joeli.jass.game.cards.Card;
import to.joeli.jass.game.cards.CardValue;
import to.joeli.jass.game.cards.Color;
import to.joeli.jass.game.mode.Mode;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;

public class NeuralNetworkHelper {

	public static final Logger logger = LoggerFactory.getLogger(NeuralNetworkHelper.class);


	public static int[][] getCardsTargets(Game game) {
		return getCardsTargets(game, null);
	}

	/**
	 * For each card we have a one hot encoded vector of length 4.
	 * The one represents the player who has/had that specific card.
	 * The players are listed by seatId.
	 * The order of the cards is the natural order
	 *
	 * @param game
	 */
	public static int[][] getCardsTargets(Game game, List<Color> colors) {
		int[][] targets = new int[36][4];
		final Card[] cards = Card.values();
		for (int i = 0; i < cards.length; i++) {
			for (Player player : game.getPlayers()) {
				Set<Card> playerCards = player.getCards().stream()
						.map(card -> DataAugmentationHelper.getRespectiveCard(card, colors))
						.collect(Collectors.toSet());
				if (playerCards.contains(cards[i]))
					targets[i][player.getSeatId()] = 1;
			}
		}
		return targets;
	}

	public static double getScoreTarget(Game game, Player player) {
		// NOTE: the scoreTarget is between 0 and 157 inside the network
		return Math.min(game.getResult().getTeamScore(player), Arena.TOTAL_POINTS);
	}


	/**
	 * Gets the observations of all the color permutations for the given game state.
	 * This will generate 24 instead of just 1 observation for trumpfs which are not top-down or bottom-up.
	 * This can be used for data augmentation purposes.
	 * TODO an additional idea for data augmentation would be adding noise (e.g. switch 6s and 7s inside every suit)
	 *
	 * @param game
	 * @return
	 */
	public static List<double[][]> getAnalogousScoreFeatures(Game game) {
		List<double[][]> features = new ArrayList<>();
		if (game.getMode().isTrumpfMode()) {
			Collection<List<Color>> permutations = Collections2.permutations(asList(Color.values()));
			permutations.forEach(colors -> features.add(getScoreFeatures(game, colors)));
		} else
			features.add(getScoreFeatures(game));
		return features;
	}

	public static List<double[][]> getAnalogousCardsFeatures(Game game, Map<Card, Distribution> cardKnowledge) {
		List<double[][]> features = new ArrayList<>();
		if (game.getMode().isTrumpfMode()) {
			Collection<List<Color>> permutations = Collections2.permutations(asList(Color.values()));
			permutations.forEach(colors -> features.add(getCardsFeatures(game, cardKnowledge, colors)));
		} else
			features.add(getCardsFeatures(game, cardKnowledge));
		return features;
	}

	public static List<int[][]> getAnalogousCardsTargets(Game game) {
		List<int[][]> targets = new ArrayList<>();
		if (game.getMode().isTrumpfMode()) {
			Collection<List<Color>> permutations = Collections2.permutations(asList(Color.values()));
			permutations.forEach(colors -> targets.add(getCardsTargets(game, colors)));
		} else
			targets.add(getCardsTargets(game));
		return targets;
	}

	public static double[][] getScoreFeatures(Game game) {
		return getScoreFeatures(game, null);
	}

	public static double[][] getScoreFeatures(Game game, List<Color> colors) {
		return getFeatures(game, null, colors);
	}

	public static double[][] getCardsFeatures(Game game, Map<Card, Distribution> cardKnowledge) {
		return getCardsFeatures(game, cardKnowledge, null);
	}

	public static double[][] getCardsFeatures(Game game, Map<Card, Distribution> cardKnowledge, List<Color> colors) {
		return getFeatures(game, cardKnowledge, colors);
	}

	/**
	 * Generates an observation from a game to be used by the neural network.
	 * INPUT:
	 * 1) INFO_ROW: Shifted (2) and trumpf code (7) and current player index (4) each in one-hot encoding
	 * example: shifted (--> 01), trumpf code 3 spades (--> 0001000), current player index 2 (0010)
	 * --> shifted   trumpf code   player index
	 * --> 1 0      0 0 0 1 0 0 0    0 0 1 0
	 * 2) CARDS_HISTORY: the cards played: 36 x (14 + 4)
	 * (where 14 is a three-hot encoded: 4 suit, 9 value, 1 bit trump
	 * and 4 is the player who had the card (players in initial stable order --> player.seatId)) in order of appearance
	 * 3) CARDS_DISTRIBUTION: your own output post-processed: 36 x (14 + 4) in cards order (all hearts, all diamonds, etc.)
	 * i.e. you set to 1 the 9 cards you own and set 0s and 1s for previously played cards; and erase the rest of the suite when you don't play it, ...
	 * CARDS: Probabilities as input
	 * SCORE: If determinized MCTS enabled: sample of distribution, otherwise: probabilities too
	 * <p>
	 * OUTPUT:
	 * card_out: the card estimation: 36 x 4 (where 4 is the player) as probabilities in cards order (all hearts, all diamonds, etc.)
	 * score_out: integer [0:157]
	 */
	private static double[][] getFeatures(Game game, Map<Card, Distribution> cardKnowledge, List<Color> colors) {
		final Mode respectiveMode = DataAugmentationHelper.getRespectiveMode(game.getMode(), colors);
		double[][] features = new double[73][18];

		// INFO_ROW
		features[0] = createInfoRow(game, respectiveMode);

		// CARDS_HISTORY
		final List<Move> history = DataAugmentationHelper.getRespectiveMoves(game.getAlreadyPlayedMovesInOrder(), colors);
		final List<ProbabilityMove> historyMoves = history.stream().map(move -> new ProbabilityMove(move.getPlayer(), move.getPlayedCard())).collect(Collectors.toList());
		final List<double[]> historyEncodings = getListOfEncodings(historyMoves, respectiveMode);
		addListToArray(historyEncodings, features, 1);

		// CARDS_DISTRIBUTION
		List<ProbabilityMove> distributionMoves = new ArrayList<>();
		if (cardKnowledge != null) { // collect the cards features: this gets called when we are in a imperfect information game setting
			cardKnowledge.forEach((key, value) -> {
				Card card = DataAugmentationHelper.getRespectiveCard(key, colors);
				distributionMoves.add(new ProbabilityMove(card, value.getProbabilitiesInSeatIdOrder()));
			});
			// INFO: Here we do not have to add the history Moves because the card knowledge already contains them
		} else { // collect the score features: this gets called when we are in a perfect information game setting
			final List<Player> order = game.getOrder().getPlayersInCurrentOrder();
			for (Player player : order) {
				player.getCards().forEach(card -> distributionMoves.add(new ProbabilityMove(player, DataAugmentationHelper.getRespectiveCard(card, colors))));
			}
			distributionMoves.addAll(historyMoves);
		}
		// Sort distributionMoves by card order
		distributionMoves.sort(Comparator.comparing(ProbabilityMove::getCard));

		final List<double[]> distributionEncodings = new ArrayList<>(getListOfEncodings(distributionMoves, respectiveMode));

		assert distributionEncodings.size() == 36;
		addListToArray(distributionEncodings, features, 37);

		return features;
	}


	private static void addListToArray(List<double[]> list, double[][] array, int startIndex) {
		for (int i = 0; i < list.size(); i++) {
			array[startIndex + i] = list.get(i);
		}
	}

	static double[] createInfoRow(Game game, Mode respectiveMode) {
		double[] infoRow = new double[18];

		// Knowing whether the game was shifted or not may be important for  the cards estimation network
		final int shiftedIndex = game.isShifted() ? 1 : 0;
		infoRow[shiftedIndex] = 1;

		// The trumpf code is another piece of information that might help
		final int trumpfCodeIndex = respectiveMode.getCode();
		infoRow[2 + trumpfCodeIndex] = 1;

		// We need to know the seat id of the first player in the round
		final int initialPlayerSeatId = game.getOrder().getPlayersInInitialOrder().get(0).getSeatId();
		infoRow[2 + 7 + initialPlayerSeatId] = 1;

		// We need to know the seat id of the current player
		final int currentPlayerSeatId = game.getCurrentPlayer().getSeatId();
		infoRow[2 + 7 + 4 + currentPlayerSeatId] = 1;

		return infoRow;
	}

	/**
	 * Converts the probability moves to encodings
	 *
	 * @param moves
	 * @param mode
	 * @return
	 */
	private static List<double[]> getListOfEncodings(List<ProbabilityMove> moves, Mode mode) {
		return moves.stream()
				.map(move -> fromMoveToEncoding(move.getCard(), mode, move.getProbabilities()))
				.collect(Collectors.toList());
	}


	/**
	 * Example:
	 * >|    suit   |           value          | isTrumpf | playerIndex
	 * > 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0,          0, 0, 1, 0    for DIAMOND_JACK and TrumpfColor CLUBS and playerIndex 2
	 * > 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 1,          1, 0, 0, 0    for HEARTS_QUEEN and TrumpfColor HEARTS and playerIndex 0
	 *
	 * @param card
	 * @param mode
	 * @return
	 */
	public static double[] fromMoveToEncoding(Card card, Mode mode, int playerIndex) {
		if (playerIndex < 0 || playerIndex > 3)
			throw new IllegalArgumentException("The playerIndex has to be between 0 and 3. Check what seatId the player has.");

		double[] encoded = Arrays.copyOf(fromCardToEncoding(card, mode), 18);
		encoded[14 + playerIndex] = 1; // set player

		return encoded;
	}

	/**
	 * Example:
	 * >|    suit   |           value          | isTrumpf | probabilities
	 * > 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0,          0.2, 0.4, 0.4, 0.0    for DIAMOND_JACK and TrumpfColor CLUBS
	 * > 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 1,          1,   0.0, 0.0  0.0    for HEARTS_QUEEN and TrumpfColor HEARTS
	 *
	 * @param card
	 * @param mode
	 * @return
	 */
	private static double[] fromMoveToEncoding(Card card, Mode mode, double[] probabilities) {
		double[] encoded = Arrays.copyOf(fromCardToEncoding(card, mode), 18);
		for (int i = 0; i < probabilities.length; i++) {
			encoded[14 + i] = probabilities[i]; // copy probabilities
		}

		return encoded;
	}


	private static double[] fromCardToEncoding(Card card, Mode mode) {
		double[] encoded = new double[14]; // first 4 for suit, second 9 for value, third 1 for trumpf, last 4 for player seatid

		encoded[card.getColor().getValue()] = 1; // set suit
		encoded[3 + card.getValue().getRank()] = 1; // set value
		encoded[14 - 1] = getTrumpfBit(card, mode); // set trumpf

		return encoded;
	}

	public static Card fromEncodingToCard(double[] threeHot) {
		Color color = null;
		for (int i = 0; i < 4; i++) {
			if (threeHot[i] == 1)
				color = Color.getColor(i);
		}
		if (color == null)
			return null;

		CardValue cardValue = null;
		for (int i = 1; i < 10; i++) {
			if (threeHot[3 + i] == 1)
				cardValue = CardValue.getCardValue(i);
		}
		if (cardValue == null)
			return null;

		return Card.getCard(color, cardValue);
	}

	public static int getTrumpfBit(Card card, Mode mode) {
		if (card.getColor().equals(mode.getTrumpfColor()))
			return 1; // NOTE: in trumpfs, the cards of the respective color have the trumpf bit set
		else if (mode.equals(Mode.topDown()))
			return 0; // NOTE: in top down, no card has the trumpf bit set (Michele approved)
		else if (mode.equals(Mode.bottomUp()))
			return 1; // NOTE: in bottom up, all cards have the trumpf bit set (Michele approved)
		return 0; // NOTE: for shift do not set anything yet.
	}

	/**
	 * Adaptation from https://stackoverflow.com/questions/8151435/integer-to-binary-array
	 *
	 * @param number
	 * @param base
	 * @return
	 */
	public static int[] toBinary(int number, int base) {
		final int[] binary = new int[base];
		for (int i = 0; i < base; i++) {
			binary[base - 1 - i] = (1 << i & number) == 0 ? 0 : 1;
		}
		return binary;
	}

	public static int fromBinary(int[] binary) {
		int result = 0;
		for (int i = 0; i < binary.length; i++)
			result += binary[binary.length - 1 - i] * Math.pow(2d, i);
		return result;
	}

	/**
	 * Reconstructs the information from an observation (list of three hot encoded vectors representing a card each)
	 *
	 * @param observation
	 * @return
	 */
	public static Map<String, List<Card>> reconstructFeatures(double[][] observation) {
		Map<String, List<Card>> reconstruction = new HashMap<>();

		List<Card> alreadyPlayedCards = new ArrayList<>();
		for (int i = 1; i < 37; i++) {
			final Card card = fromEncodingToCard(observation[i]);
			if (card != null)
				alreadyPlayedCards.add(card);
		}

		reconstruction.put("AlreadyPlayedCards", alreadyPlayedCards);


		List<Card> cards = new ArrayList<>();
		for (int i = 37; i < 73; i++) {
			final Card card = fromEncodingToCard(observation[i]);
			if (card != null)
				cards.add(card);
		}
		reconstruction.put("CardsDistribution", cards);


		return reconstruction;
	}

	static class ProbabilityMove {
		private final Card card;
		private final double[] probabilities;

		ProbabilityMove(Player player, Card card) {
			this.card = card;
			this.probabilities = new double[4];
			this.probabilities[player.getSeatId()] = 1;
		}

		ProbabilityMove(Card card, double[] probabilities) {
			this.card = card;
			this.probabilities = probabilities;
		}

		Card getCard() {
			return card;
		}

		double[] getProbabilities() {
			return probabilities;
		}
	}
}
