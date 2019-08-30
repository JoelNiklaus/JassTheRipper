package to.joeli.jass.client.rest

data class JassRequest(
        val version: String,
        val trump: Int,
        val dealer: Int,
        val currentPlayer: Int,
        val tss: Int,
        val tricks: List<Trick>,
        val player: List<Hand>,
        val jassTyp: String) {
    constructor() : this("", 0, 0, 0, 0, ArrayList<Trick>(), ArrayList<Hand>(), "")
}
