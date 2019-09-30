package to.joeli.jass.client.rest.responses

import to.joeli.jass.game.cards.Card

data class CardResponse(val card: String) {
    constructor() : this("")
    constructor(card: Card) : this(card.toString())
}
