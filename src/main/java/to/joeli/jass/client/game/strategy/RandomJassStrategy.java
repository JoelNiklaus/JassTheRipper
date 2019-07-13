package to.joeli.jass.client.game.strategy;

import to.joeli.jass.client.game.GameSession;
import to.joeli.jass.client.game.strategy.helpers.CardSelectionHelper;
import to.joeli.jass.client.game.strategy.helpers.TrumpfSelectionHelper;
import to.joeli.jass.game.cards.Card;
import to.joeli.jass.game.mode.Mode;

import java.util.Set;

public class RandomJassStrategy implements JassStrategy {

	@Override
	public Mode chooseTrumpf(Set<Card> availableCards, GameSession session, boolean isGschobe) {
		return TrumpfSelectionHelper.getRandomMode(isGschobe);
	}

	@Override
	public Card chooseCard(Set<Card> availableCards, GameSession session) {
		return CardSelectionHelper.getRandomCard(availableCards, session.getCurrentGame());
	}
}
