package com.zuehlke.jasschallenge.client.game.strategy;

import com.zuehlke.jasschallenge.client.game.GameSession;
import com.zuehlke.jasschallenge.client.game.Move;
import com.zuehlke.jasschallenge.client.game.Player;
import com.zuehlke.jasschallenge.client.game.PlayingOrder;
import com.zuehlke.jasschallenge.client.game.strategy.helpers.MCTSHelper;
import com.zuehlke.jasschallenge.game.cards.Card;
import com.zuehlke.jasschallenge.game.mode.Mode;
import org.junit.Test;

import java.util.EnumSet;
import java.util.Set;

/**
 * Created by joelniklaus on 06.05.17.
 */
public class JassTheRipperStrategyTest {

	@Test
	public void testMCTSStart() throws Exception {
		final GameSession gameSession = GameSessionBuilder.newSession()
				.withStartedGame(Mode.bottomUp())
				.createGameSession();

		Set<Card> cards = EnumSet.of(Card.CLUB_ACE, Card.CLUB_EIGHT, Card.CLUB_JACK, Card.DIAMOND_EIGHT, Card.DIAMOND_SEVEN, Card.SPADE_EIGHT, Card.HEART_TEN, Card.SPADE_NINE, Card.SPADE_JACK);

		JassTheRipperJassStrategy strategy = new JassTheRipperJassStrategy();
		strategy.chooseCard(cards, gameSession);
	}

	// TODO spätere runden testen
	// TODO andere trümpfe testen

	@Test
	public void testMCTSDuringFirstRound() throws Exception {
		final GameSession gameSession = GameSessionBuilder.newSession()
				.withStartedGame(Mode.bottomUp())
				.createGameSession();

		PlayingOrder order = gameSession.getCurrentRound().getPlayingOrder();

		gameSession.makeMove(new Move(order.getCurrentPlayer(), Card.CLUB_NINE));

		Set<Card> cards = EnumSet.of(Card.CLUB_ACE, Card.CLUB_EIGHT, Card.CLUB_JACK, Card.DIAMOND_EIGHT, Card.DIAMOND_SEVEN, Card.SPADE_EIGHT, Card.HEART_TEN, Card.SPADE_NINE, Card.SPADE_JACK);

		JassTheRipperJassStrategy strategy = new JassTheRipperJassStrategy();
		strategy.chooseCard(cards, gameSession);
	}



}