package to.joeli.jass.client.rest.requests

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class JassRequest(
        val version: String,
        val trump: Int,
        val dealer: Int,
        val currentPlayer: Int,
        val tss: Int,
        val tricks: List<Trick>,
        val player: List<Hand>,
        val jassTyp: String,
        val gameId: String, // Not used
        val seatId: Int // Not used
) {
    constructor() : this("", 0, 0, 0, 0, ArrayList<Trick>(), ArrayList<Hand>(), "", "", 0)
}
