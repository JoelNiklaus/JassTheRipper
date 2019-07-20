package to.joeli.jass.client.strategy.training

import org.slf4j.LoggerFactory
import to.joeli.jass.client.strategy.helpers.ShellScriptRunner
import to.joeli.jass.client.strategy.helpers.ZeroMQClient

/**
 * Decision not to use DL4J anymore for the following reasons:
 * - Throws UnsupportedKerasConfigurationException: Unsupported keras layer type Softmax for special cards estimator network configuration
 * - Training is a pain in DL4J: No Tensorboard support, own visualization tool does not work, very hard to get metrics
 * - Documentation is horrible
 * --> Conclusion: Use Keras and ZeroMQ for communication
 */
open class NeuralNetwork(protected val networkType: NetworkType, val isTrainable: Boolean) {


    fun train(trainMode: TrainMode): Boolean {
        return ShellScriptRunner.runShellProcess(ShellScriptRunner.getPythonDirectory(), "python3 train.py " + trainMode.path + " " + networkType.path)
    }

    fun loadWeightsOfTrainableNetwork(): Boolean {
        return ZeroMQClient.loadWeights(networkType)
    }

    companion object {
        val logger = LoggerFactory.getLogger(NeuralNetwork::class.java)
    }
}
