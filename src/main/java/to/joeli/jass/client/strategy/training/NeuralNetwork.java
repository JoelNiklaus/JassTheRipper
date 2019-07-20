package to.joeli.jass.client.strategy.training;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import to.joeli.jass.client.strategy.helpers.ShellScriptRunner;
import to.joeli.jass.client.strategy.helpers.ZeroMQClient;

/**
 * Decision not to use DL4J anymore for the following reasons:
 * - Throws UnsupportedKerasConfigurationException: Unsupported keras layer type Softmax for special cards estimator network configuration
 * - Training is a pain in DL4J: No Tensorboard support, own visualization tool does not work, very hard to get metrics
 * - Documentation is horrible
 * --> Conclusion: Use Keras and ZeroMQ for communication
 */
public class NeuralNetwork {


	public static final Logger logger = LoggerFactory.getLogger(NeuralNetwork.class);

	protected final NetworkType networkType;
	protected final boolean trainable;


	public NeuralNetwork(NetworkType networkType, boolean trainable) {
		this.networkType = networkType;
		this.trainable = trainable;
	}


	public boolean train(TrainMode trainMode) {
		return ShellScriptRunner.runShellProcess(ShellScriptRunner.getPythonDirectory(), "python3 train.py " + trainMode.getPath() + " " + networkType.getPath());
	}

	public boolean loadWeightsOfTrainableNetwork() {
		return ZeroMQClient.loadWeights(networkType);
	}

	public boolean isTrainable() {
		return trainable;
	}
}
