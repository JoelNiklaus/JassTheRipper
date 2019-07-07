package com.zuehlke.jasschallenge.client.game.strategy.training;

import com.zuehlke.jasschallenge.client.game.strategy.helpers.NeuralNetworkHelper;
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

import static com.zuehlke.jasschallenge.client.game.strategy.training.ScoreEstimator.INPUT_DIM;

/**
 * Decision not to use DL4J anymore for the following reasons:
 * - Throws UnsupportedKerasConfigurationException: Unsupported keras layer type Softmax for special cards estimator network configuration
 * - Training is a pain in DL4J: No Tensorboard support, own visualization tool does not work, very hard to get metrics
 * - Documentation is horrible
 * --> Conclusion: Use Keras and ZeroMQ for communication
 */
public class NeuralNetwork {

	public static final int THREE_HOT_ENCODING_LENGTH = 4 + 9 + 1;

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
}
