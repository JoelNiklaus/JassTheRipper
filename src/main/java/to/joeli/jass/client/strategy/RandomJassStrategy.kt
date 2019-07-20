package to.joeli.jass.client.strategy

import to.joeli.jass.client.game.GameSession
import to.joeli.jass.client.strategy.helpers.CardSelectionHelper
import to.joeli.jass.client.strategy.helpers.TrumpfSelectionHelper
import to.joeli.jass.game.cards.Card
import to.joeli.jass.game.mode.Mode

class RandomJassStrategy : JassStrategy {

    override fun chooseTrumpf(availableCards: Set<Card>, session: GameSession, isGschobe: Boolean): Mode {
        return TrumpfSelectionHelper.getRandomMode(isGschobe)
    }

    override fun chooseCard(availableCards: Set<Card>, session: GameSession): Card {
        return CardSelectionHelper.getRandomCard(availableCards, session.currentGame)
    }
}
