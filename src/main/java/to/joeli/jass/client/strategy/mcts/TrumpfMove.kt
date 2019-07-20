package to.joeli.jass.client.strategy.mcts


import to.joeli.jass.client.game.Player
import to.joeli.jass.client.strategy.mcts.src.Move
import to.joeli.jass.game.mode.Mode

/**
 * Created by joelniklaus on 06.05.17.
 */
class TrumpfMove(val player: Player, val chosenTrumpf: Mode) : Move {

    override fun compareTo(other: Move): Int {
        return 0
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as TrumpfMove?
        return player == that!!.player && chosenTrumpf == that.chosenTrumpf
    }

    override fun hashCode(): Int {
        return chosenTrumpf.hashCode()
    }

    override fun toString(): String {
        return chosenTrumpf.toString()
    }
}
