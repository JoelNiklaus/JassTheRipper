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
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.evaluation.regression.RegressionEvaluation;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.io.ClassPathResource;
import org.nd4j.linalg.learning.config.Nadam;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.nd4j.linalg.util.ArrayUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;

import static java.util.Arrays.asList;

public class NeuralNetwork implements Serializable {
	public static final int NUM_INPUT_ROWS = 72; // 36 + 9 + 9 + 9 + 9
	public static final int THREE_HOT_ENCODING_LENGTH = 14; // 4 + 9 + 1
	public static final int INPUT_DIM = NUM_INPUT_ROWS * THREE_HOT_ENCODING_LENGTH;
	public static final int NUM_NEURONS = 128;
	public static final double LEARNING_RATE = 1e-3;
	public static final double WEIGHT_DECAY = 1e-3;
	public static final double DROPOUT = 0.5;
	public static final int SEED = 42;

	private MultiLayerNetwork model;

	public static final Logger logger = LoggerFactory.getLogger(NeuralNetwork.class);

	public NeuralNetwork() {
		MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
				.seed(SEED)
				.optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
				.updater(new Nadam(LEARNING_RATE)) // NOTE: Also try AmsGrad, Adam
				.activation(Activation.RELU)
				.weightInit(WeightInit.XAVIER)
				.dropOut(DROPOUT)
				.weightDecay(WEIGHT_DECAY)
				.gradientNormalization(GradientNormalization.RenormalizeL2PerLayer)
				.list()
				.layer(new DenseLayer.Builder().nIn(INPUT_DIM).nOut(NUM_NEURONS).build())
				.layer(new DenseLayer.Builder().nIn(NUM_NEURONS).nOut(NUM_NEURONS).build())
				.layer(new DenseLayer.Builder().nIn(NUM_NEURONS).nOut(NUM_NEURONS).build())
				.layer(new DenseLayer.Builder().nIn(NUM_NEURONS).nOut(NUM_NEURONS).build())
				.layer(new OutputLayer.Builder(LossFunctions.LossFunction.MEAN_ABSOLUTE_ERROR)
						.activation(Activation.SIGMOID)
						.nIn(NUM_NEURONS).nOut(1).build())
				.build();

		model = new MultiLayerNetwork(conf);
		model.init();
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

	public void evaluate(DataSet dataSet) {
		RegressionEvaluation evaluation = new RegressionEvaluation();
		final INDArray predictions = model.output(dataSet.getFeatures());
		System.out.println(predictions);
		evaluation.eval(dataSet.getLabels(), predictions);
		System.out.println(evaluation.stats());
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

	private static INDArray buildInput(Collection<INDArray> collection) {
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

	public void train(Collection<INDArray> observations, Collection<INDArray> labels, int numEpochs) {
		train(buildDataSet(observations, labels), numEpochs);
	}

	public void train(DataSet dataSet, int numEpochs) {
		model.setListeners(new ScoreIterationListener(1));

		dataSet.shuffle(); // NOTE: can be used to remove bias in the training set.

		// TODO check if this really works
		//dataSet.normalize();

		for (int i = 0; i < numEpochs; i++) {
			logger.info("Epoch #{}", i);
			model.fit(dataSet);
		}
	}

	private static boolean createIfNotExists(File directory) {
		if (directory.isDirectory())
			return true;
		return directory.mkdirs();
	}

	public static boolean saveDataSet(DataSet dataSet, String filePath) {
		final File file = new File(filePath);
		if (createIfNotExists(file.getParentFile())) {
			dataSet.save(file);
			logger.info("Saved dataset to {}", filePath);
			return true;
		} else {
			logger.error("Could not save the file {}", filePath);
			return false;
		}
	}

	public static DataSet loadDataSet(String filePath) {
		DataSet dataSet = new DataSet();
		dataSet.load(new File(filePath));
		return dataSet;
	}

	public boolean saveModel(String filePath) {
		try {
			if (createIfNotExists(new File(filePath).getParentFile())) {
				ModelSerializer.writeModel(model, filePath, true);
				logger.info("Saved model to {}", filePath);
				return true;
			} else {
				logger.error("Could not save the file {}", filePath);
				return false;
			}
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	public MultiLayerNetwork loadModel(String filePath) {
		try {
			model = ModelSerializer.restoreMultiLayerNetwork((filePath));
			logger.info("Loaded saved model from {}", filePath);
			return model;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
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

		// TODO: Somehow add information if game is shifted or not -> may be important for card estimation network
		// observation[72] = toBinary(game.isShifted() ? 1 : 0, THREE_HOT_ENCODING_LENGTH);

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
