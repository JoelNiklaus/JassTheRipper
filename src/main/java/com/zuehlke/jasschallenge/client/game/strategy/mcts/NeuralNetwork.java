package com.zuehlke.jasschallenge.client.game.strategy.mcts;


import com.zuehlke.jasschallenge.client.game.Game;
import com.zuehlke.jasschallenge.client.game.Player;
import com.zuehlke.jasschallenge.client.game.strategy.helpers.CardSelectionHelper;
import com.zuehlke.jasschallenge.client.game.strategy.helpers.NeuralNetworkHelper;
import com.zuehlke.jasschallenge.client.game.strategy.training.Arena;
import com.zuehlke.jasschallenge.game.cards.Card;
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
import org.nd4j.linalg.learning.config.Nadam;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class NeuralNetwork {
	public static final int NUM_INPUT_ROWS = 72; // 36 + 9 + 9 + 9 + 9
	public static final int THREE_HOT_ENCODING_LENGTH = 14; // 4 + 9 + 1
	public static final int INPUT_DIM = NUM_INPUT_ROWS * THREE_HOT_ENCODING_LENGTH;
	public static final int NUM_NEURONS = 128;
	public static final double LEARNING_RATE = 1e-3;
	public static final double WEIGHT_DECAY = 1e-3;
	public static final double DROPOUT = 0.5;
	public static final int SEED = 42;

	protected MultiLayerNetwork model;

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
			Game clonedGame = new Game(game);
			clonedGame.makeMove(new CardMove(player, card));

			// TODO maybe it is more efficient to do all the forward passes at the same time and not one by one
			actionValuePairs.put(card, Arena.TOTAL_POINTS - predictScore(clonedGame)); // NOTE: 157 - value because the value is from the perspective of a player of the opponent team
		}
		Card bestCard = actionValuePairs.entrySet()
				.stream()
				.min(Map.Entry.comparingByValue(Comparator.reverseOrder()))
				.map(Map.Entry::getKey)
				.orElse(null);

		return new CardMove(player, bestCard);
	}

	/**
	 * Score Estimator: predict the final score of a determinized game (perfect information)
	 *
	 * @param game
	 * @return
	 */
	public double predictScore(Game game) {
		// INFO: We disregard the match bonus for simplicity
		return Arena.TOTAL_POINTS * predict(Collections.singletonList(NeuralNetworkHelper.getObservation(game)))[0];
	}

	/**
	 * Performs a forward pass through the network and returns the regressed values.
	 *
	 * @param observations
	 * @return
	 */
	private double[] predict(List<INDArray> observations) {
		INDArray input = NeuralNetworkHelper.buildInput(observations);
		final INDArray output = model.output(input);
		return output.toDoubleVector();
	}

	public void train(Collection<INDArray> observations, Collection<INDArray> labels, int numEpochs) {
		train(NeuralNetworkHelper.buildDataSet(observations, labels), numEpochs);
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

	public boolean save(String filePath) {
		try {
			if (NeuralNetworkHelper.createIfNotExists(new File(filePath).getParentFile())) {
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

	public boolean load(String filePath) {
		try {
			model = ModelSerializer.restoreMultiLayerNetwork((filePath));
			logger.info("Loaded saved model from {}", filePath);
			return true;
		} catch (IOException e) {
			e.printStackTrace();
		}
		logger.error("Could not load saved model from {}", filePath);
		return false;
	}

	public boolean loadKerasModel(String filePath) {
		try {
			model = KerasModelImport.importKerasSequentialModelAndWeights(filePath);
			logger.info("Loaded saved model from {}", filePath);
			return true;
		} catch (IOException e) {
			e.printStackTrace();
		} catch (UnsupportedKerasConfigurationException e) {
			e.printStackTrace();
		} catch (InvalidKerasConfigurationException e) {
			e.printStackTrace();
		}
		logger.error("Could not load saved model from {}", filePath);
		return false;
	}

	/**
	 * Cards Estimator: Predict the card distribution of the hidden cards of the other players.
	 * This should help generating determinizations of better quality.
	 *
	 * @return
	 */
	public Map<Card, Distribution<Player>> predictCardDistribution() {
		return null;
	}
}
