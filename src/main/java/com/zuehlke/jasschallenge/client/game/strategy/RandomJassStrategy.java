package com.zuehlke.jasschallenge.client.game.strategy;

import com.zuehlke.jasschallenge.client.game.Game;
import com.zuehlke.jasschallenge.client.game.GameSession;
import com.zuehlke.jasschallenge.client.game.Round;
import com.zuehlke.jasschallenge.client.game.strategy.helpers.JassHelper;
import com.zuehlke.jasschallenge.game.cards.Card;
import com.zuehlke.jasschallenge.game.mode.Mode;

import java.io.Serializable;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class RandomJassStrategy implements JassStrategy, Serializable {

	@Override
	public Mode chooseTrumpf(Set<Card> availableCards, GameSession session, boolean isGschobe) {
		return JassHelper.getRandomMode(isGschobe);
	}

	@Override
	public Card chooseCard(Set<Card> availableCards, GameSession session) {
		return JassHelper.getRandomCard(availableCards, session.getCurrentGame());
	}
}
