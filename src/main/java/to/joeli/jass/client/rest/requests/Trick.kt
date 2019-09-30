package to.joeli.jass.client.rest.requests

import to.joeli.jass.game.cards.Card
import java.util.function.Consumer

data class Trick(
        val cards: List<String>,
        val points: Int,
        val win: Int,
        val first: Int) {
    constructor() : this(ArrayList<String>(), 0, 0, 0)

    fun getCardsTrick(): List<Card> {
        val newCards = ArrayList<Card>()
        cards.forEach(Consumer { cardString -> newCards.add(Card.getCard(cardString)); })
        return newCards
    }
}
