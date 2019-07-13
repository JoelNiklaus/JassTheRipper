package to.joeli.jass.client.strategy.training;

import to.joeli.jass.client.strategy.helpers.ShellScriptRunner;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Decision not to use DL4J anymore for the following reasons:
 * - Throws UnsupportedKerasConfigurationException: Unsupported keras layer type Softmax for special cards estimator network configuration
 * - Training is a pain in DL4J: No Tensorboard support, own visualization tool does not work, very hard to get metrics
 * - Documentation is horrible
 * --> Conclusion: Use Keras and ZeroMQ for communication
 */
public class NeuralNetwork {


	public static final Logger logger = LoggerFactory.getLogger(NeuralNetwork.class);

	protected final String name;
	protected final boolean trainable;
	protected final boolean pretrainingEnabled;

	public NeuralNetwork(String name, boolean trainable) {
		this.name = name;
		this.trainable = trainable;
		this.pretrainingEnabled = true; // TODO maybe at some point we also want to train from scratch. But for now this is ok
	}


	public boolean train(TrainMode trainMode) {
		return ShellScriptRunner.runShellProcess(getPythonDirectory(), "python3 train.py " + trainMode.getPath() + " " + name);
	}

	@NotNull
	private static String getPythonDirectory() {
		return System.getProperty("user.dir") + "/src/main/java/to/joeli/jass/client/strategy/training/python";
	}

	public void loadWeightsOfTrainableNetwork() {
		// TODO load the weights of the trainable network here
	}


	public boolean isTrainable() {
		return trainable;
	}

	public boolean isPretrainingEnabled() {
		return pretrainingEnabled;
	}
}
