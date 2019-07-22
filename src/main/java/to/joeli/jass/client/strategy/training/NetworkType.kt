package to.joeli.jass.client.strategy.training

/**
 * The network type sets the path in the python environment
 * The output is used in the model export to find the output operation
 */
enum class NetworkType constructor(val path: String, val output: String) {
    CARDS("cards/", "cards/truediv"),
    SCORE("score/", "score/BiasAdd"),
}
