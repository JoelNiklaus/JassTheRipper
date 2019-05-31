package com.zuehlke.jasschallenge.client.game.strategy.mcts;


import com.zuehlke.jasschallenge.client.game.Game;
import com.zuehlke.jasschallenge.client.game.Player;
import com.zuehlke.jasschallenge.client.game.strategy.helpers.CardSelectionHelper;
import com.zuehlke.jasschallenge.game.cards.Card;
import com.zuehlke.jasschallenge.game.mode.Mode;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.GradientNormalization;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.modelimport.keras.KerasModelImport;
import org.deeplearning4j.nn.modelimport.keras.exceptions.InvalidKerasConfigurationException;
import org.deeplearning4j.nn.modelimport.keras.exceptions.UnsupportedKerasConfigurationException;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.io.ClassPathResource;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.nd4j.linalg.util.ArrayUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;

import static java.util.Arrays.asList;

public class NeuralNetwork implements Serializable {

	public static final Logger logger = LoggerFactory.getLogger(NeuralNetwork.class);


	public static final int NUM_INPUT_ROWS = 72; // 36 + 9 + 9 + 9 + 9
	public static final int THREE_HOT_ENCODING_LENGTH = 14; // 4 + 9 + 1
	public static final int INPUT_DIM = NUM_INPUT_ROWS * THREE_HOT_ENCODING_LENGTH;
	private MultiLayerNetwork model;

	public static final String BASE_PATH = "src/main/resources/";
	public static final String VALUE_ESTIMATOR_PATH = BASE_PATH + "ValueEstimator";


	public NeuralNetwork() {
		try {
			model = ModelSerializer.restoreMultiLayerNetwork((VALUE_ESTIMATOR_PATH));
			logger.info("Loaded saved model from " + VALUE_ESTIMATOR_PATH);
		} catch (IOException e) {
			e.printStackTrace();
			logger.info("Cannot load saved neural network. Building new one now...");

			MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
					.seed(42)
					.optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
					.updater(new Adam(0.01))
					.activation(Activation.LEAKYRELU)
					.weightInit(WeightInit.XAVIER)
					.dropOut(0.5)
					.l2(0.0001)
					.gradientNormalization(GradientNormalization.RenormalizeL2PerLayer)
					.list()
					.layer(0, new DenseLayer.Builder().nIn(INPUT_DIM).nOut(128).build())
					.layer(1, new DenseLayer.Builder().nIn(128).nOut(128).build())
					.layer(2, new DenseLayer.Builder().nIn(128).nOut(128).build())
					.layer(3, new DenseLayer.Builder().nIn(128).nOut(128).build())
					.layer(4, new OutputLayer.Builder(LossFunctions.LossFunction.MEAN_ABSOLUTE_ERROR)
							.activation(Activation.SIGMOID)
							.nIn(128).nOut(1).build())
					.build();

			model = new MultiLayerNetwork(conf);
			model.init();
		}
	}

	public NeuralNetwork(NeuralNetwork neuralNetwork) {
		model = neuralNetwork.model.clone();
	}

	private static void importKerasModel() throws IOException, InvalidKerasConfigurationException, UnsupportedKerasConfigurationException {
		String simpleMlp = new ClassPathResource("simple_mlp.h5").getFile().getPath(); // At the base of the resources folder
		MultiLayerNetwork model = KerasModelImport.importKerasSequentialModelAndWeights(simpleMlp);

		INDArray input = Nd4j.create(256, 100);
		INDArray output = model.output(input);
		System.out.println(output);
	}


	public CardMove predictMove(Game game) {
		final Player player = game.getCurrentPlayer();
		Set<Card> possibleCards = CardSelectionHelper.getCardsPossibleToPlay(EnumSet.copyOf(player.getCards()), game);

		assert !possibleCards.isEmpty();

		EnumMap<Card, Double> actionValuePairs = new EnumMap<>(Card.class);
		for (Card card : possibleCards) {
			Game clonedGame = new Game(game); // TODO this might be a performance bottleneck. Change to a more efficient variant
			clonedGame.makeMove(new CardMove(player, card));

			// TODO maybe it is more efficient to do all the forward passes at the same time and not one by one
			actionValuePairs.put(card, 157 - predictValue(clonedGame)); // NOTE: 157 - value because the value is from the perspective of a player of the opponent team
		}
		Card bestCard = actionValuePairs.entrySet()
				.stream()
				.min(Map.Entry.comparingByValue(Comparator.reverseOrder()))
				.get()
				.getKey();

		return new CardMove(player, bestCard);
	}

	public double predictValue(Game game) {
		return 157 * predict(asList(getObservation(game)))[0]; // INFO: We disregard the match bonus for simplicity
	}

	/**
	 * Performs a forward pass through the network and returns the regressed values.
	 *
	 * @param observations
	 * @return
	 */
	private double[] predict(List<INDArray> observations) {
		INDArray input = buildInput(observations);
		final INDArray output = model.output(input);
		return output.toDoubleVector();
	}

	private INDArray buildInput(List<INDArray> observations) {
		INDArray input = Nd4j.create(observations.size(), observations.get(0).length());
		for (int i = 0; i < observations.size(); i++) {
			input.putRow(i, observations.get(i));
		}
		return input;
	}

	public void train(List<INDArray> observations, List<INDArray> labels) {
		DataSet dataSet = new DataSet();
		dataSet.setFeatures(buildInput(observations));
		dataSet.setLabels(buildInput(labels));

		// TODO check if this really works
		//dataSet.normalize();

		model.fit(dataSet);
	}

	public void save() {
		try {
			ModelSerializer.writeModel(model, VALUE_ESTIMATOR_PATH, true);
			logger.info("Saved model to " + VALUE_ESTIMATOR_PATH);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static INDArray getObservation(Game game) {
		return Nd4j.createFromArray(ArrayUtil.flatten(generateObservation(game)));
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
	static int[][] generateObservation(Game game) {
		int[][] observation = new int[NUM_INPUT_ROWS][THREE_HOT_ENCODING_LENGTH];

		insertCardsIntoObservation(game.getAlreadyPlayedCardsInOrder(), game.getMode(), observation, 0);

		int startIndex = 36;
		for (Player player : game.getOrder().getPlayersInCurrentPlayingOrder()) {
			insertCardsIntoObservation(new ArrayList<>(player.getCards()), game.getMode(), observation, startIndex);
			startIndex += 9;
		}

		// NOTE: adding the trumpf as another row would be an additional option
		// observation[72] = toBinary(game.getMode().getCode(), THREE_HOT_ENCODING_LENGTH);

		return observation;
	}

	private static void insertCardsIntoObservation(List<Card> cards, Mode mode, int[][] observation, int startIndex) {
		for (int i = 0; i < cards.size(); i++)
			observation[startIndex + i] = fromCardToThreeHot(cards.get(i), mode);
	}

	static int[] fromCardToThreeHot(Card card, Mode mode) {
		int[] threeHot = new int[THREE_HOT_ENCODING_LENGTH]; // first 4 for suit, second 9 for value, last 1 for trumpf

		threeHot[card.getColor().getValue()] = 1; // set suit
		threeHot[3 + card.getValue().getRank()] = 1; // set value
		threeHot[THREE_HOT_ENCODING_LENGTH - 1] = getTrumpfBit(card, mode); // set trumpf

		return threeHot;
	}

	static int getTrumpfBit(Card card, Mode mode) {
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
	static int[] toBinary(int number, int base) {
		final int[] binary = new int[base];
		for (int i = 0; i < base; i++) {
			binary[base - 1 - i] = (1 << i & number) == 0 ? 0 : 1;
		}
		return binary;
	}
}
