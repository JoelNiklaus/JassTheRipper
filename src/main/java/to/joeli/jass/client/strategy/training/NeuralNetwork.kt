package to.joeli.jass.client.strategy.training

import org.slf4j.LoggerFactory
import to.joeli.jass.client.strategy.helpers.ShellScriptRunner
import to.joeli.jass.client.strategy.helpers.ZeroMQClient

/**
 * Decision not to use DL4J anymore for the following reasons:
 * - Throws UnsupportedKerasConfigurationException: Unsupported keras layer type Softmax for special cards estimator network configuration
 * - Training is a pain in DL4J: No Tensorboard support, own visualization tool does not work, very hard to get metrics
 * - Documentation is horrible
 * --> Conclusion: Use Keras and ZeroMQ for fast communication between java and python
 */
open class NeuralNetwork(val networkType: NetworkType, val isTrainable: Boolean) {


    /**
     * Trains the network with a given train mode. The actual training is done in python with keras. This is why we invoke the shell script.
     */
    fun train(trainMode: TrainMode): Boolean {
        return ShellScriptRunner.runShellProcess(ShellScriptRunner.getPythonDirectory(), "python3 train.py " + trainMode.path + " " + networkType.path)
    }

    /**
     * Sends a message to the neural netork server (ZeroMQServer) to load the weights of the saved trainable network into memory for prediction
     * IMPORTANT: The ZeroMQServer must be started.
     */
    fun loadWeightsOfTrainableNetwork(): Boolean {
        return ZeroMQClient.loadWeights(networkType)
    }

    companion object {
        val logger = LoggerFactory.getLogger(NeuralNetwork::class.java)
    }
}
