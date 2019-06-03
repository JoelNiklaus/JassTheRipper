package com.zuehlke.jasschallenge.client.game.strategy.helpers;

import com.zuehlke.jasschallenge.client.game.*;
import com.zuehlke.jasschallenge.client.game.strategy.RunMode;
import com.zuehlke.jasschallenge.client.game.strategy.StrengthLevel;
import com.zuehlke.jasschallenge.client.game.strategy.exceptions.MCTSException;
import com.zuehlke.jasschallenge.game.cards.Card;
import com.zuehlke.jasschallenge.game.mode.Mode;
import org.junit.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by joelniklaus on 06.05.17.
 */
public class MCTSHelperTest {

	private static final StrengthLevel STRENGTH_LEVEL = StrengthLevel.FAST;


	@Test
	public void testExecutorServiceShutsDownCorrectly() throws MCTSException {
		final GameSession gameSession = GameSessionBuilder.newSession()
				.withStartedGame(Mode.bottomUp())
				.createGameSession();

		Set<Card> cards = EnumSet.of(Card.CLUB_ACE, Card.CLUB_EIGHT, Card.CLUB_JACK, Card.DIAMOND_EIGHT, Card.DIAMOND_SEVEN, Card.SPADE_EIGHT, Card.HEART_TEN, Card.SPADE_NINE, Card.SPADE_JACK);

		MCTSHelper mctsHelper = new MCTSHelper(STRENGTH_LEVEL.getNumDeterminizationsFactor(), RunMode.RUNS);
		mctsHelper.predictMove(cards, gameSession, false, false, STRENGTH_LEVEL);

		assertFalse(mctsHelper.isShutDown());

		mctsHelper.shutDown();

		assertTrue(mctsHelper.isShutDown());

	}

	@Test
	public void testMCTSTrumpf() throws MCTSException {
		final GameSession gameSession = GameSessionBuilder.newSession()
				.createGameSession();

		Set<Card> cards = EnumSet.of(Card.CLUB_ACE, Card.CLUB_EIGHT, Card.CLUB_JACK, Card.DIAMOND_EIGHT, Card.DIAMOND_SEVEN, Card.SPADE_EIGHT, Card.HEART_TEN, Card.SPADE_NINE, Card.SPADE_JACK);

		MCTSHelper mctsHelper = new MCTSHelper(STRENGTH_LEVEL.getNumDeterminizationsFactor(), RunMode.RUNS);
		mctsHelper.predictMove(cards, gameSession, true, false, STRENGTH_LEVEL);
	}

	@Test
	public void testMCTSStart() throws MCTSException {
		final GameSession gameSession = GameSessionBuilder.newSession()
				.withStartedGame(Mode.bottomUp())
				.createGameSession();

		Set<Card> cards = EnumSet.of(Card.CLUB_ACE, Card.CLUB_EIGHT, Card.CLUB_JACK, Card.DIAMOND_EIGHT, Card.DIAMOND_SEVEN, Card.SPADE_EIGHT, Card.HEART_TEN, Card.SPADE_NINE, Card.SPADE_JACK);

		MCTSHelper mctsHelper = new MCTSHelper(STRENGTH_LEVEL.getNumDeterminizationsFactor(), RunMode.RUNS);
		mctsHelper.predictMove(cards, gameSession, false, false, STRENGTH_LEVEL);
	}

	// TODO spätere runden testen
	// TODO andere trümpfe testen

	@Test
	public void testMCTSDuringFirstRound() throws MCTSException {
		final GameSession gameSession = GameSessionBuilder.newSession()
				.withStartedGame(Mode.bottomUp())
				.createGameSession();

		PlayingOrder order = gameSession.getCurrentRound().getPlayingOrder();

		gameSession.makeMove(new Move(order.getCurrentPlayer(), Card.CLUB_NINE));

		Set<Card> cards = EnumSet.of(Card.CLUB_ACE, Card.CLUB_EIGHT, Card.CLUB_JACK, Card.DIAMOND_EIGHT, Card.DIAMOND_SEVEN, Card.SPADE_EIGHT, Card.HEART_TEN, Card.SPADE_NINE, Card.SPADE_JACK);

		MCTSHelper mctsHelper = new MCTSHelper(STRENGTH_LEVEL.getNumDeterminizationsFactor(), RunMode.RUNS);
		mctsHelper.predictMove(cards, gameSession, false, false, STRENGTH_LEVEL);

	}

	@Test
	public void testMCTSDuringSecondRound() throws MCTSException {
		final GameSession gameSession = GameSessionBuilder.newSession()
				.withStartedGame(Mode.bottomUp())
				.createGameSession();

		PlayingOrder order = gameSession.getCurrentRound().getPlayingOrder();

		gameSession.makeMove(new Move(order.getCurrentPlayer(), Card.CLUB_NINE));
		order.moveToNextPlayer();
		Player player = order.getCurrentPlayer();
		gameSession.makeMove(new Move(order.getCurrentPlayer(), Card.CLUB_SEVEN));
		order.moveToNextPlayer();
		gameSession.makeMove(new Move(order.getCurrentPlayer(), Card.CLUB_KING));
		order.moveToNextPlayer();
		gameSession.makeMove(new Move(order.getCurrentPlayer(), Card.CLUB_ACE));
		order.moveToNextPlayer();

		gameSession.startNextRound();

		gameSession.makeMove(new Move(player, Card.HEART_ACE));


		Set<Card> cards = EnumSet.of(Card.CLUB_QUEEN, Card.CLUB_EIGHT, Card.CLUB_JACK, Card.DIAMOND_EIGHT, Card.DIAMOND_SEVEN, Card.SPADE_EIGHT, Card.HEART_TEN, Card.SPADE_NINE);

		MCTSHelper mctsHelper = new MCTSHelper(STRENGTH_LEVEL.getNumDeterminizationsFactor(), RunMode.RUNS);
		mctsHelper.predictMove(cards, gameSession, false, false, STRENGTH_LEVEL);

	}

}