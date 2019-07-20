package to.joeli.jass.client.strategy.training

/**
 * The network type sets the path in the python environment
 */
enum class NetworkType constructor(val path: String) {
    CARDS("cards/"),
    SCORE("score/"),
}
