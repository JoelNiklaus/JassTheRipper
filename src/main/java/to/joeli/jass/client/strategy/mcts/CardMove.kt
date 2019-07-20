package to.joeli.jass.client.strategy.mcts


import to.joeli.jass.client.game.Player
import to.joeli.jass.client.strategy.mcts.src.Move
import to.joeli.jass.game.cards.Card

/**
 * Created by joelniklaus on 06.05.17.
 */
class CardMove(player: Player, playedCard: Card) : to.joeli.jass.client.game.Move(player, playedCard), Move {

    override fun compareTo(o: Move): Int {
        val other = o as CardMove
        return playedCard.compareTo(other.playedCard)
    }

    override fun toString(): String {
        return playedCard.toString()
    }
}
