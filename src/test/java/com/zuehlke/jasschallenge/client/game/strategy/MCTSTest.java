package com.zuehlke.jasschallenge.client.game.strategy;

import com.zuehlke.jasschallenge.client.game.Game;
import com.zuehlke.jasschallenge.client.game.GameSession;
import com.zuehlke.jasschallenge.client.game.GameSessionBuilder;
import com.zuehlke.jasschallenge.client.game.strategy.helpers.MCTSHelper;
import com.zuehlke.jasschallenge.game.Trumpf;
import com.zuehlke.jasschallenge.game.cards.Card;
import com.zuehlke.jasschallenge.game.cards.Color;
import com.zuehlke.jasschallenge.game.mode.Mode;
import org.junit.Test;

import java.util.EnumSet;
import java.util.Set;

/**
 * Created by joelniklaus on 06.05.17.
 */
public class MCTSTest {

	@Test
	public void testMCTS() {
		final GameSession gameSession = GameSessionBuilder.newSession()
				.withStartedGame(Mode.bottomUp())
				.createGameSession();

		Set<Card> cards = EnumSet.of(Card.CLUB_ACE, Card.CLUB_EIGHT, Card.CLUB_JACK, Card.DIAMOND_EIGHT, Card.DIAMOND_SEVEN, Card.SPADE_EIGHT, Card.HEART_TEN, Card.SPADE_NINE, Card.SPADE_JACK);
		Game game = gameSession.getCurrentGame();
		MCTSHelper.getCard(cards, game);

	}

}