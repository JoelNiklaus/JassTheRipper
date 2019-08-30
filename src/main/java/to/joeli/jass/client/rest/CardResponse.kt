package to.joeli.jass.client.rest

import to.joeli.jass.game.cards.Card

data class CardResponse(val card: String) {
    constructor() : this("")
    constructor(card: Card) : this(card.toString())
}
