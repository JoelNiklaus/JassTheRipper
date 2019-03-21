package com.zuehlke.jasschallenge.client.game.strategy;

import com.zuehlke.jasschallenge.client.game.GameSession;
import com.zuehlke.jasschallenge.client.game.Move;
import com.zuehlke.jasschallenge.client.game.PlayingOrder;
import com.zuehlke.jasschallenge.game.Trumpf;
import com.zuehlke.jasschallenge.game.cards.Card;
import com.zuehlke.jasschallenge.game.cards.Color;
import com.zuehlke.jasschallenge.game.mode.Mode;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Created by joelniklaus on 06.05.17.
 */
public class JassTheRipperStrategyTest {

	private Set<Card> allCards = EnumSet.copyOf(Arrays.asList(Card.values()));
	private Set<Card> cards1 = EnumSet.of(Card.CLUB_QUEEN, Card.CLUB_ACE, Card.HEART_SIX, Card.HEART_JACK, Card.HEART_KING, Card.DIAMOND_SEVEN, Card.DIAMOND_QUEEN, Card.SPADE_TEN, Card.SPADE_KING);
	private Set<Card> cards2 = EnumSet.of(Card.CLUB_NINE, Card.CLUB_JACK, Card.HEART_EIGHT, Card.HEART_NINE, Card.DIAMOND_EIGHT, Card.DIAMOND_NINE, Card.DIAMOND_TEN, Card.SPADE_EIGHT, Card.SPADE_QUEEN);
	private Set<Card> cards3 = EnumSet.of(Card.CLUB_KING, Card.CLUB_EIGHT, Card.HEART_SEVEN, Card.HEART_QUEEN, Card.DIAMOND_JACK, Card.DIAMOND_KING, Card.SPADE_SEVEN, Card.SPADE_JACK, Card.SPADE_ACE);
	private Set<Card> cards4 = EnumSet.of(Card.CLUB_SIX, Card.CLUB_TEN, Card.CLUB_SEVEN, Card.HEART_TEN, Card.HEART_ACE, Card.DIAMOND_SIX, Card.DIAMOND_ACE, Card.SPADE_SIX, Card.SPADE_NINE);

	@Before
	public void setUp() {
		assertEquals(9, cards1.size());
		assertEquals(9, cards2.size());
		assertEquals(9, cards3.size());
		assertEquals(9, cards4.size());
		assertEquals(36, allCards.size());
		allCards.removeAll(cards1);
		allCards.removeAll(cards2);
		allCards.removeAll(cards3);
		allCards.removeAll(cards4);
		assertEquals(0, allCards.size());
	}

	@Test
	public void testChooseTrumpf() {
		final GameSession gameSession = GameSessionBuilder.newSession()
				.createGameSession();

		JassTheRipperJassStrategy strategy = new JassTheRipperJassStrategy(StrengthLevel.FAST);
		strategy.onSessionStarted(gameSession); // Needed to initialize the mctsHelper

		Mode mode = strategy.chooseTrumpf(cards1, gameSession, false);
		assertEquals(Mode.from(Trumpf.TRUMPF, Color.HEARTS), mode);
	}

	@Test
	public void testMCTSStart() {
		final GameSession gameSession = GameSessionBuilder.newSession()
				.withStartedGame(Mode.bottomUp())
				.createGameSession();

		JassTheRipperJassStrategy strategy = new JassTheRipperJassStrategy(StrengthLevel.FAST);
		strategy.onSessionStarted(gameSession); // Needed to initialize the mctsHelper

		strategy.chooseCard(cards1, gameSession);
		strategy.chooseCard(cards2, gameSession);
		strategy.chooseCard(cards3, gameSession);
		strategy.chooseCard(cards4, gameSession);
	}

	// TODO spätere runden testen


	@Test
	public void testMCTSDuringFirstRound() {
		final GameSession gameSession = GameSessionBuilder.newSession()
				.withStartedGame(Mode.bottomUp())
				.createGameSession();

		PlayingOrder order = gameSession.getCurrentRound().getPlayingOrder();
		gameSession.makeMove(new Move(order.getCurrentPlayer(), Card.CLUB_NINE));


		JassTheRipperJassStrategy strategy = new JassTheRipperJassStrategy(StrengthLevel.FAST);
		strategy.onSessionStarted(gameSession); // Needed to initialize the mctsHelper

		strategy.chooseCard(cards1, gameSession);
	}

	@Test
	public void testMCTSSeveralPredictions() {
		final GameSession gameSession = GameSessionBuilder.newSession()
				.withStartedGame(Mode.bottomUp())
				.createGameSession();

		PlayingOrder order = gameSession.getCurrentRound().getPlayingOrder();


		JassTheRipperJassStrategy strategy = new JassTheRipperJassStrategy(StrengthLevel.FAST);
		strategy.onSessionStarted(gameSession); // Needed to initialize the mctsHelper

		gameSession.makeMove(new Move(order.getCurrentPlayer(), strategy.chooseCard(cards1, gameSession)));
		gameSession.makeMove(new Move(order.getCurrentPlayer(), strategy.chooseCard(cards2, gameSession)));
		gameSession.makeMove(new Move(order.getCurrentPlayer(), strategy.chooseCard(cards3, gameSession)));
		gameSession.makeMove(new Move(order.getCurrentPlayer(), strategy.chooseCard(cards4, gameSession)));


	}
}