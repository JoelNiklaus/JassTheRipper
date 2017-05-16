package com.zuehlke.jasschallenge.client.game.strategy;

import com.zuehlke.jasschallenge.client.game.GameSession;
import com.zuehlke.jasschallenge.client.game.Move;
import com.zuehlke.jasschallenge.client.game.PlayingOrder;
import com.zuehlke.jasschallenge.game.cards.Card;
import com.zuehlke.jasschallenge.game.cards.Color;
import com.zuehlke.jasschallenge.game.mode.Mode;
import org.junit.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Created by joelniklaus on 06.05.17.
 */
public class JassTheRipperStrategyTest {

	private Set<Card> cards1 = EnumSet.of(Card.CLUB_ACE, Card.CLUB_EIGHT, Card.CLUB_JACK, Card.DIAMOND_SIX, Card.DIAMOND_SEVEN, Card.SPADE_QUEEN, Card.HEART_TEN, Card.SPADE_NINE, Card.SPADE_KING);
	private Set<Card> cards2 = EnumSet.of(Card.HEART_ACE, Card.HEART_EIGHT, Card.HEART_JACK, Card.CLUB_SIX, Card.CLUB_SEVEN, Card.DIAMOND_QUEEN, Card.SPADE_TEN, Card.DIAMOND_NINE, Card.DIAMOND_JACK);
	private Set<Card> cards3 = EnumSet.of(Card.SPADE_ACE, Card.SPADE_EIGHT, Card.SPADE_JACK, Card.HEART_SIX, Card.HEART_SEVEN, Card.CLUB_QUEEN, Card.DIAMOND_TEN, Card.CLUB_NINE, Card.CLUB_JACK);
	private Set<Card> cards4 = EnumSet.of(Card.DIAMOND_ACE, Card.DIAMOND_EIGHT, Card.DIAMOND_JACK, Card.SPADE_SIX, Card.SPADE_SEVEN, Card.HEART_QUEEN, Card.CLUB_TEN, Card.HEART_NINE, Card.HEART_JACK);
	private Set<Card> clubs = EnumSet.of(Card.CLUB_ACE, Card.CLUB_KING, Card.CLUB_QUEEN, Card.CLUB_JACK, Card.CLUB_TEN, Card.CLUB_NINE, Card.CLUB_EIGHT, Card.CLUB_SEVEN, Card.CLUB_SIX);
	private Set<Card> veryGoodUndeUfe = EnumSet.of(Card.CLUB_SIX, Card.CLUB_EIGHT, Card.DIAMOND_SIX, Card.DIAMOND_SEVEN, Card.DIAMOND_NINE, Card.SPADE_SIX, Card.SPADE_SEVEN, Card.HEART_TEN, Card.CLUB_NINE);


	@Test
	public void testMCTSStart() throws Exception {
		final GameSession gameSession = GameSessionBuilder.newSession()
				.withStartedGame(Mode.bottomUp())
				.createGameSession();

		JassTheRipperJassStrategy strategy = new JassTheRipperJassStrategy();
		strategy.chooseCard(cards1, gameSession);
		strategy.chooseCard(cards2, gameSession);
		strategy.chooseCard(cards3, gameSession);
		strategy.chooseCard(cards4, gameSession);
	}

	// TODO spätere runden testen
	// TODO andere trümpfe testen

	@Test
    public void testRateUndeUfeBestWithGreatUndeUfeCards() {
        JassTheRipperJassStrategy jassStrategy = new JassTheRipperJassStrategy();
        int ratingObeAbe =jassStrategy.rateObeabe(veryGoodUndeUfe);
        int ratingUndeUfe =jassStrategy.rateUndeufe(veryGoodUndeUfe);
        assertTrue(ratingUndeUfe > ratingObeAbe);
        System.out.println("Undeufe: " + ratingUndeUfe + ", Obeabe: " + ratingObeAbe);
    }

    @Test
	public void testRateObeAbeWithAllClubs() {
		JassTheRipperJassStrategy jassStrategy = new JassTheRipperJassStrategy();
		// 90 is maximum amount of points
		assertEquals(90, jassStrategy.rateObeabeColor(clubs,Color.CLUBS));
		assertEquals(0, jassStrategy.rateObeabeColor(clubs, Color.DIAMONDS));
		assertEquals(0, jassStrategy.rateObeabeColor(clubs, Color.HEARTS));
		assertEquals(0, jassStrategy.rateObeabeColor(clubs, Color.SPADES));
		assertEquals(90, jassStrategy.rateObeabe(clubs));
	}

    @Test
    public void testRateUndeUfeWithAllClubs() {
        JassTheRipperJassStrategy jassStrategy = new JassTheRipperJassStrategy();
        // 90 is maximum amount of points
        assertEquals(90, jassStrategy.rateUndeufeColor(clubs,Color.CLUBS));
        assertEquals(0, jassStrategy.rateUndeufeColor(clubs, Color.DIAMONDS));
        assertEquals(0, jassStrategy.rateUndeufeColor(clubs, Color.HEARTS));
        assertEquals(0, jassStrategy.rateUndeufeColor(clubs, Color.SPADES));
        assertEquals(90, jassStrategy.rateUndeufe(clubs));
    }

    @Test
    public void testRateObeAbeWithIsBetween0And90() {
        JassTheRipperJassStrategy jassStrategy = new JassTheRipperJassStrategy();
        assertTrue(jassStrategy.rateObeabe(cards1) >= 0);
        assertTrue(jassStrategy.rateObeabe(cards2) >= 0);
        assertTrue(jassStrategy.rateObeabe(cards3) >= 0);
        assertTrue(jassStrategy.rateObeabe(cards4) >= 0);
        assertTrue(jassStrategy.rateObeabe(cards1) <= 90);
        assertTrue(jassStrategy.rateObeabe(cards2) <= 90);
        assertTrue(jassStrategy.rateObeabe(cards3) <= 90);
        assertTrue(jassStrategy.rateObeabe(cards4) <= 90);
    }

    @Test
    public void testRateUndeUfeWithIsBetween0And90() {
        JassTheRipperJassStrategy jassStrategy = new JassTheRipperJassStrategy();
        assertTrue(jassStrategy.rateUndeufe(cards1) >= 0);
        assertTrue(jassStrategy.rateUndeufe(cards2) >= 0);
        assertTrue(jassStrategy.rateUndeufe(cards3) >= 0);
        assertTrue(jassStrategy.rateUndeufe(cards4) >= 0);
        assertTrue(jassStrategy.rateUndeufe(cards1) <= 90);
        assertTrue(jassStrategy.rateUndeufe(cards2) <= 90);
        assertTrue(jassStrategy.rateUndeufe(cards3) <= 90);
        assertTrue(jassStrategy.rateUndeufe(cards4) <= 90);
    }

	@Test
	public void testCalculateInitialSafety() {
		JassTheRipperJassStrategy jassStrategy = new JassTheRipperJassStrategy();
		List<Card> sortedClubs = jassStrategy.sortCardsDescending(cards1, Color.CLUBS);
		List<Card> sortedSpades = jassStrategy.sortCardsDescending(cards1, Color.SPADES);
		assertEquals(jassStrategy.calculateInitialSafetyObeabe(sortedClubs), 1.0, 0.05);
		assertTrue(jassStrategy.calculateInitialSafetyObeabe(sortedSpades) > 0.33);
		assertTrue(jassStrategy.calculateInitialSafetyObeabe(sortedSpades) < 0.34);

	}

	@Test
    public void getCardRank() {
	    // Test that the Ace has Rank 9
	    assertEquals(9, Card.CLUB_ACE.getRank());
    }

	@Test
	public void testMCTSDuringFirstRound() throws Exception {
		final GameSession gameSession = GameSessionBuilder.newSession()
				.withStartedGame(Mode.bottomUp())
				.createGameSession();

		PlayingOrder order = gameSession.getCurrentRound().getPlayingOrder();
		gameSession.makeMove(new Move(order.getCurrentPlayer(), Card.CLUB_NINE));


		JassTheRipperJassStrategy strategy = new JassTheRipperJassStrategy();
		strategy.chooseCard(cards1, gameSession);
	}

	@Test
	public void testMCTSSeveralPredictions() throws Exception {
		final GameSession gameSession = GameSessionBuilder.newSession()
				.withStartedGame(Mode.bottomUp())
				.createGameSession();

		PlayingOrder order = gameSession.getCurrentRound().getPlayingOrder();


		JassTheRipperJassStrategy strategy = new JassTheRipperJassStrategy();
		gameSession.makeMove(new Move(order.getCurrentPlayer(), strategy.chooseCard(cards1, gameSession)));
		gameSession.makeMove(new Move(order.getCurrentPlayer(), strategy.chooseCard(cards2, gameSession)));
		gameSession.makeMove(new Move(order.getCurrentPlayer(), strategy.chooseCard(cards3, gameSession)));
		gameSession.makeMove(new Move(order.getCurrentPlayer(), strategy.chooseCard(cards4, gameSession)));


	}
}