package to.joeli.jass.client.rest.requests

import to.joeli.jass.game.cards.Card
import java.util.function.Consumer

data class Hand(val hand: List<String>) {
    constructor() : this(ArrayList<String>())

    fun getCardsHand(): List<Card> {
        val cards = ArrayList<Card>()
        hand.forEach(Consumer { cardString -> cards.add(Card.getCard(cardString)); })
        return cards
    }
}
