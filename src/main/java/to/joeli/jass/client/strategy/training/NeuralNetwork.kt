package to.joeli.jass.client.strategy.training

import org.slf4j.LoggerFactory
import org.tensorflow.SavedModelBundle
import org.tensorflow.Tensor
import to.joeli.jass.client.strategy.helpers.ShellScriptRunner


/**
 * Decision not to use DL4J anymore for the following reasons:
 * - Throws UnsupportedKerasConfigurationException: Unsupported keras layer type Softmax for special cards estimator network configuration
 * - Training is a pain in DL4J: No Tensorboard support, own visualization tool does not work, very hard to get metrics
 * - Documentation is horrible
 * --> Conclusion: Use Keras and ZeroMQ for fast communication between java and python
 */
open class NeuralNetwork(private val networkType: NetworkType, var isTrainable: Boolean) {

    var savedModelBundle: SavedModelBundle? = null


    fun loadModel(episodeNumber: String) {
        val path = "${DataSet.BASE_PATH}$episodeNumber/${networkType.path}models/export/"
        savedModelBundle = SavedModelBundle.load(path, "tag")
    }

    @Synchronized
    fun predict(features: Array<DoubleArray>): Any {
        if (savedModelBundle == null)
            throw IllegalStateException("There is no neural network loaded! Cannot make any predictions!")

        // TODO generate floats directly in NeuralNetworkHelper
        val floats = Array(features.size) { FloatArray(features[0].size) }
        for (i in features.indices) {
            for (j in 0 until features[0].size) {
                floats[i][j] = features[i][j].toFloat()
            }
        }

        val input = Array(1) { Array(floats.size) { FloatArray(floats[0].size) } }
        input[0] = floats

        return savedModelBundle!!.session().runner()
                .feed("input", Tensor.create(input))
                .fetch(networkType.output)
                .run()[0]
    }

    /**
     * Sends a message to the neural network server (ZeroMQServer) to load the weights of the saved trainable network into memory for prediction
     * IMPORTANT: The ZeroMQServer must be started.
     */
    fun loadWeightsOfTrainableNetwork() {
        //loadModel(TrainMode.SELF_PLAY)
        //return ZeroMQClient.loadWeights(networkType)
    }

    companion object {
        /**
         * Trains the network with a given train mode. The actual training is done in python with keras. This is why we invoke the shell script.
         */
        @JvmStatic
        fun train(episodeNumber: String, networkType: NetworkType): Boolean {
            return ShellScriptRunner.runShellProcess(ShellScriptRunner.getPythonDirectory(), "python3 train.py $episodeNumber ${networkType.path}")

        }

        val logger = LoggerFactory.getLogger(NeuralNetwork::class.java)
    }
}
