package com.zuehlke.jasschallenge.client.game.strategy.helpers;

import com.google.common.collect.Collections2;
import com.zuehlke.jasschallenge.client.game.Game;
import com.zuehlke.jasschallenge.client.game.Player;
import com.zuehlke.jasschallenge.client.game.strategy.mcts.NeuralNetwork;
import com.zuehlke.jasschallenge.client.game.strategy.training.Arena;
import com.zuehlke.jasschallenge.game.cards.Card;
import com.zuehlke.jasschallenge.game.cards.CardValue;
import com.zuehlke.jasschallenge.game.cards.Color;
import com.zuehlke.jasschallenge.game.mode.Mode;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.util.ArrayUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static java.util.Arrays.asList;

public class NeuralNetworkHelper {

	public static final Logger logger = LoggerFactory.getLogger(NeuralNetworkHelper.class);


	public static boolean pretrainCardsEstimator() {
		return pretrainKerasModel("cards_estimator");
	}

	public static boolean pretrainScoreEstimator() {
		return pretrainKerasModel("score_estimator");
	}

	private static boolean pretrainKerasModel(String model) {
		final String directory = System.getProperty("user.dir") + "/src/main/java/com/zuehlke/jasschallenge/client/game/strategy/training/python";
		String command = "python3 " + model + ".py";

		return ShellScriptRunner.runShellProcess(directory, command);
	}

	/**
	 * Gets the observations of all the color permutations for the given game state.
	 * This will generate 24 instead of just 1 observation for trumpfs which are not top-down or bottom-up.
	 * This can be used for data augmentation purposes.
	 * TODO an additional idea for data augmentation would be adding noise (e.g. switch 6s and 7s inside every suit
	 *
	 * @param game
	 * @return
	 */
	public static List<INDArray> getAnalogousObservations(Game game) {
		List<INDArray> observations = new ArrayList<>();
		if (game.getMode().isTrumpfMode()) {
			Collection<List<Color>> permutations = Collections2.permutations(asList(Color.values()));
			permutations.forEach(colors -> observations.add(getObservation(game, colors)));
		} else
			observations.add(getObservation(game));
		return observations;
	}

	/**
	 * Gets a single observation from a given game state
	 *
	 * @param game
	 * @return
	 */
	public static INDArray getObservation(Game game) {
		return Nd4j.createFromArray(ArrayUtil.flatten(generateObservation(game, null)));
	}

	private static INDArray getObservation(Game game, List<Color> colors) {
		return Nd4j.createFromArray(ArrayUtil.flatten(generateObservation(game, colors)));
	}

	/**
	 * Generates an observation from a game to be used by the neural network.
	 * index 00 - 35: played cards in order of appearance, NOTE: deliberately chose 36 cards and not only minimum required 31 to provide more training examples
	 * index 36 - 44: hand cards of current player
	 * index 45 - 53: hand cards of first opponent player
	 * index 54 - 62: hand cards of partner player
	 * index 63 - 71: hand cards of second opponent player
	 *
	 * @param game
	 * @return
	 */
	public static int[][] generateObservation(Game game, List<Color> colors) {
		int[][] observation = new int[NeuralNetwork.NUM_INPUT_ROWS][NeuralNetwork.THREE_HOT_ENCODING_LENGTH];

		insertCardsIntoObservation(game.getAlreadyPlayedCardsInOrder(), game.getMode(), observation, 0, colors);

		int startIndex = 36;
		for (Player player : game.getOrder().getPlayersInCurrentPlayingOrder()) {
			insertCardsIntoObservation(new ArrayList<>(player.getCards()), game.getMode(), observation, startIndex, colors);
			startIndex += 9;
		}

		// NOTE: adding the trumpf as another row would be an additional option
		// observation[72] = toBinary(game.getMode().getCode(), THREE_HOT_ENCODING_LENGTH);

		// TODO: Somehow add information if game is shifted or not -> may be important for card estimation network
		// observation[72] = toBinary(game.isShifted() ? 1 : 0, THREE_HOT_ENCODING_LENGTH);

		return observation;
	}

	/**
	 * Converts the cards into the three hot encoding format and inserts them in the observation at starting from the start index.
	 * For data augmentation purposes a permutation of colors can be supplied. This makes it possible to generate 24 observations from a single game.
	 * NOTE: This is only possible for trumpf modes. For top-down and bottom-up it is not available yet.
	 *
	 * @param cards
	 * @param mode
	 * @param observation
	 * @param startIndex
	 * @param colors      the permutation of colors to generate the observation from. Set to null to generate an observation of the actual game
	 */
	private static void insertCardsIntoObservation(List<Card> cards, Mode mode, int[][] observation, int startIndex, List<Color> colors) {
		if (mode.isTrumpfMode() && colors != null) {
			Color newTrumpfColor = colors.get(mode.getTrumpfColor().getValue());
			mode = Mode.trump(newTrumpfColor);
		}
		for (int i = 0; i < cards.size(); i++) {
			Card card = cards.get(i);
			if (mode.isTrumpfMode() && colors != null) {
				Color newCardColor = colors.get(card.getColor().getValue());
				card = Card.getCard(newCardColor, card.getValue());
			}
			observation[startIndex + i] = fromCardToThreeHot(card, mode);
		}
	}

	/**
	 * Reconstructs the information from an observation (list of three hot encoded vectors representing a card each)
	 *
	 * @param observation
	 * @return
	 */
	public static Map<String, List<Card>> reconstructObservation(int[][] observation) {
		Map<String, List<Card>> reconstruction = new HashMap<>();

		List<Card> alreadyPlayedCards = new ArrayList<>();
		for (int i = 0; i <= 35; i++) {
			final Card card = fromThreeHotToCard(observation[i]);
			if (card != null)
				alreadyPlayedCards.add(card);
		}

		reconstruction.put("AlreadyPlayedCards", alreadyPlayedCards);

		for (int j = 0; j < 4; j++) {
			List<Card> playerCards = new ArrayList<>();
			for (int i = 36 + j * 9; i <= 44 + j * 9; i++) {
				final Card card = fromThreeHotToCard(observation[i]);
				if (card != null)
					playerCards.add(card);
			}
			reconstruction.put("PlayerCards-" + j, playerCards);
		}

		return reconstruction;
	}

	/**
	 * Example:
	 * |    suit   |           value          | isTrumpf
	 * 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0            for DIAMOND_JACK and TrumpfColor CLUBS
	 * 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1            for DIAMOND_JACK and TrumpfColor DIAMONDS
	 *
	 * @param card
	 * @param mode
	 * @return
	 */
	public static int[] fromCardToThreeHot(Card card, Mode mode) {
		int[] threeHot = new int[NeuralNetwork.THREE_HOT_ENCODING_LENGTH]; // first 4 for suit, second 9 for value, last 1 for trumpf

		threeHot[card.getColor().getValue()] = 1; // set suit
		threeHot[3 + card.getValue().getRank()] = 1; // set value
		threeHot[NeuralNetwork.THREE_HOT_ENCODING_LENGTH - 1] = getTrumpfBit(card, mode); // set trumpf

		return threeHot;
	}

	public static Card fromThreeHotToCard(int[] threeHot) {
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
			return 0; // NOTE: in top down, no card has the trumpf bit set TODO ask michele if this is a good idea
		else if (mode.equals(Mode.bottomUp()))
			return 1; // NOTE: in bottom up, all cards have the trumpf bit set TODO ask michele if this is a good idea
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

	public static boolean createIfNotExists(File directory) {
		if (directory.isDirectory())
			return true;
		return directory.mkdirs();
	}

	public static boolean saveDataSet(DataSet dataSet) {
		if (createIfNotExists(new File(Arena.DATASETS_BASE_PATH))) {
			try {
				dataSet.save(new File(Arena.DATASET_PATH));

				Nd4j.writeTxt(dataSet.getFeatures(), Arena.DATASETS_BASE_PATH + "features.txt");
				Nd4j.writeTxt(dataSet.getLabels(), Arena.DATASETS_BASE_PATH + "labels.txt");

				Nd4j.writeAsNumpy(dataSet.getFeatures(), new File(Arena.DATASETS_BASE_PATH + "features.npy"));
				Nd4j.writeAsNumpy(dataSet.getLabels(), new File(Arena.DATASETS_BASE_PATH + "labels.npy"));
			} catch (IOException e) {
				e.printStackTrace();
			}
			logger.info("Saved dataset to {}", Arena.DATASET_PATH);
			return true;
		} else {
			logger.error("Could not save the file {}", Arena.DATASET_PATH);
			return false;
		}
	}

	public static DataSet loadDataSet(String filePath) {
		DataSet dataSet = new DataSet();
		dataSet.load(new File(filePath));
		return dataSet;
	}

	public static INDArray buildInput(Collection<INDArray> collection) {
		List<INDArray> list = new ArrayList<>(collection);
		INDArray input = Nd4j.create(list.size(), list.get(0).length());
		for (int i = 0; i < list.size(); i++) {
			input.putRow(i, list.get(i));
		}
		return input;
	}

	public static DataSet buildDataSet(Collection<INDArray> observations, Collection<INDArray> labels) {
		DataSet dataSet = new DataSet();
		dataSet.setFeatures(buildInput(observations));
		dataSet.setLabels(buildInput(labels));
		return dataSet;
	}
}
