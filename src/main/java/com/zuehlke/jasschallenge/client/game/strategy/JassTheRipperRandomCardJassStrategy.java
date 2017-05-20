package com.zuehlke.jasschallenge.client.game.strategy;

import com.zuehlke.jasschallenge.client.game.GameSession;
import com.zuehlke.jasschallenge.client.game.strategy.helpers.JassHelper;
import com.zuehlke.jasschallenge.game.cards.Card;
import com.zuehlke.jasschallenge.game.mode.Mode;

import java.io.Serializable;
import java.util.Set;

public class JassTheRipperRandomCardJassStrategy extends JassTheRipperJassStrategy implements Serializable {

	@Override
	public Card chooseCard(Set<Card> availableCards, GameSession session) {
		return JassHelper.getRandomCard(availableCards, session.getCurrentGame());
	}

}
