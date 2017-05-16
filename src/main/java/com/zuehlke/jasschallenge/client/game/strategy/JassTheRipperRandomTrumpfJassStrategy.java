package com.zuehlke.jasschallenge.client.game.strategy;

import com.zuehlke.jasschallenge.client.game.GameSession;
import com.zuehlke.jasschallenge.client.game.strategy.helpers.JassHelper;
import com.zuehlke.jasschallenge.game.cards.Card;
import com.zuehlke.jasschallenge.game.mode.Mode;

import java.io.Serializable;
import java.util.Set;

public class JassTheRipperRandomTrumpfJassStrategy extends JassTheRipperJassStrategy implements Serializable {

	@Override
	public Mode chooseTrumpf(Set<Card> availableCards, GameSession session, boolean isGschobe) {
		return JassHelper.getRandomMode(isGschobe);
	}

}
