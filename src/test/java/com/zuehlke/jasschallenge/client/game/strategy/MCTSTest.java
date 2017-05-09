package com.zuehlke.jasschallenge.client.game.strategy;

import com.zuehlke.jasschallenge.client.game.*;
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
	public void testMCTSStart() throws Exception {
		final GameSession gameSession = GameSessionBuilder.newSession()
				.withStartedGame(Mode.bottomUp())
				.createGameSession();

		Set<Card> cards = EnumSet.of(Card.CLUB_ACE, Card.CLUB_EIGHT, Card.CLUB_JACK, Card.DIAMOND_EIGHT, Card.DIAMOND_SEVEN, Card.SPADE_EIGHT, Card.HEART_TEN, Card.SPADE_NINE, Card.SPADE_JACK);

		MCTSHelper.getCard(cards, gameSession.getCurrentGame());

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

		MCTSHelper.getCard(cards, gameSession.getCurrentGame());

	}

	@Test
	public void testMCTSDuringSecondRound() throws Exception {
		final GameSession gameSession = GameSessionBuilder.newSession()
				.withStartedGame(Mode.bottomUp())
				.createGameSession();

		PlayingOrder order = gameSession.getCurrentRound().getPlayingOrder();

		gameSession.makeMove(new Move(order.getCurrentPlayer(), Card.CLUB_NINE));
		order.moveToNextPlayer();
		Player player = order.getCurrentPlayer();
		gameSession.makeMove(new Move(order.getCurrentPlayer(), Card.CLUB_EIGHT));
		order.moveToNextPlayer();
		gameSession.makeMove(new Move(order.getCurrentPlayer(), Card.CLUB_KING));
		order.moveToNextPlayer();
		gameSession.makeMove(new Move(order.getCurrentPlayer(), Card.CLUB_ACE));
		order.moveToNextPlayer();

		gameSession.startNextRound();

		gameSession.makeMove(new Move(player, Card.HEART_ACE));


		Set<Card> cards = EnumSet.of(Card.CLUB_ACE, Card.CLUB_EIGHT, Card.CLUB_JACK, Card.DIAMOND_EIGHT, Card.DIAMOND_SEVEN, Card.SPADE_EIGHT, Card.HEART_TEN, Card.SPADE_NINE, Card.SPADE_JACK);

		MCTSHelper.getCard(cards, gameSession.getCurrentGame());

	}

}