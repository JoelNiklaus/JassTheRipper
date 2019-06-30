package com.zuehlke.jasschallenge.client.game.strategy;

import com.zuehlke.jasschallenge.client.game.GameSession;
import com.zuehlke.jasschallenge.client.game.strategy.helpers.CardSelectionHelper;
import com.zuehlke.jasschallenge.client.game.strategy.helpers.TrumpfSelectionHelper;
import com.zuehlke.jasschallenge.game.cards.Card;
import com.zuehlke.jasschallenge.game.mode.Mode;

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
