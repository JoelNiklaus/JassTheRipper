package com.zuehlke.jasschallenge.client.game.strategy.helpers;

import com.zuehlke.jasschallenge.game.cards.Card;
import com.zuehlke.jasschallenge.game.cards.Color;
import org.junit.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Created by dominikbriner on 19.05.17.
 */
public class TrumpfSelectionHelperTest {

	private Set<Card> cards1 = EnumSet.of(Card.CLUB_ACE, Card.CLUB_EIGHT, Card.CLUB_JACK, Card.DIAMOND_SIX, Card.DIAMOND_SEVEN, Card.SPADE_QUEEN, Card.HEART_TEN, Card.SPADE_NINE, Card.SPADE_KING);
	private Set<Card> cards2 = EnumSet.of(Card.HEART_ACE, Card.HEART_EIGHT, Card.HEART_JACK, Card.CLUB_SIX, Card.CLUB_SEVEN, Card.DIAMOND_QUEEN, Card.SPADE_TEN, Card.DIAMOND_NINE, Card.DIAMOND_JACK);
	private Set<Card> cards3 = EnumSet.of(Card.SPADE_ACE, Card.SPADE_EIGHT, Card.SPADE_JACK, Card.HEART_SIX, Card.HEART_SEVEN, Card.CLUB_QUEEN, Card.DIAMOND_TEN, Card.CLUB_NINE, Card.CLUB_JACK);
	private Set<Card> cards4 = EnumSet.of(Card.DIAMOND_ACE, Card.DIAMOND_EIGHT, Card.DIAMOND_JACK, Card.SPADE_SIX, Card.SPADE_SEVEN, Card.HEART_QUEEN, Card.CLUB_TEN, Card.HEART_NINE, Card.HEART_JACK);
	private Set<Card> allClubs = EnumSet.of(Card.CLUB_ACE, Card.CLUB_KING, Card.CLUB_QUEEN, Card.CLUB_JACK, Card.CLUB_TEN, Card.CLUB_NINE, Card.CLUB_EIGHT, Card.CLUB_SEVEN, Card.CLUB_SIX);


	@Test
	public void testRateObeAbeWithAllClubs() {
		// 180 is maximum amount of points
		assertEquals(180, TrumpfSelectionHelper.rateObeabeColor(allClubs, Color.CLUBS));
		assertEquals(0, TrumpfSelectionHelper.rateObeabeColor(allClubs, Color.DIAMONDS));
		assertEquals(0, TrumpfSelectionHelper.rateObeabeColor(allClubs, Color.HEARTS));
		assertEquals(0, TrumpfSelectionHelper.rateObeabeColor(allClubs, Color.SPADES));
		assertEquals(180, TrumpfSelectionHelper.rateObeabe(allClubs));
	}

	@Test
	public void testRateUndeUfeWithAllClubs() {
		// 180 is maximum amount of points
		assertEquals(180, TrumpfSelectionHelper.rateUndeufeColor(allClubs, Color.CLUBS));
		assertEquals(0, TrumpfSelectionHelper.rateUndeufeColor(allClubs, Color.DIAMONDS));
		assertEquals(0, TrumpfSelectionHelper.rateUndeufeColor(allClubs, Color.HEARTS));
		assertEquals(0, TrumpfSelectionHelper.rateUndeufeColor(allClubs, Color.SPADES));
		assertEquals(180, TrumpfSelectionHelper.rateUndeufe(allClubs));
	}

	@Test
	public void testRateObeAbeWithIsBetween0And180() {
		assertTrue(TrumpfSelectionHelper.rateObeabe(cards1) >= 0);
		assertTrue(TrumpfSelectionHelper.rateObeabe(cards2) >= 0);
		assertTrue(TrumpfSelectionHelper.rateObeabe(cards3) >= 0);
		assertTrue(TrumpfSelectionHelper.rateObeabe(cards4) >= 0);
		assertTrue(TrumpfSelectionHelper.rateObeabe(cards1) <= 180);
		assertTrue(TrumpfSelectionHelper.rateObeabe(cards2) <= 180);
		assertTrue(TrumpfSelectionHelper.rateObeabe(cards3) <= 180);
		assertTrue(TrumpfSelectionHelper.rateObeabe(cards4) <= 180);
	}

	@Test
	public void testRateUndeUfeWithIsBetween0And180() {
		assertTrue(TrumpfSelectionHelper.rateUndeufe(cards1) >= 0);
		assertTrue(TrumpfSelectionHelper.rateUndeufe(cards2) >= 0);
		assertTrue(TrumpfSelectionHelper.rateUndeufe(cards3) >= 0);
		assertTrue(TrumpfSelectionHelper.rateUndeufe(cards4) >= 0);
		assertTrue(TrumpfSelectionHelper.rateUndeufe(cards1) <= 180);
		assertTrue(TrumpfSelectionHelper.rateUndeufe(cards2) <= 180);
		assertTrue(TrumpfSelectionHelper.rateUndeufe(cards3) <= 180);
		assertTrue(TrumpfSelectionHelper.rateUndeufe(cards4) <= 180);
	}

	@Test
	public void testCalculateInitialSafety() {
		List<Card> sortedClubs = JassHelper.sortCardsOfColorDescending(cards1, Color.CLUBS);
		List<Card> sortedSpades = JassHelper.sortCardsOfColorDescending(cards1, Color.SPADES);
		assertEquals(1.0, TrumpfSelectionHelper.calculateInitialSafetyObeabe(sortedClubs), 0.05);
		assertTrue(TrumpfSelectionHelper.calculateInitialSafetyObeabe(sortedSpades) > 0.33);
		assertTrue(TrumpfSelectionHelper.calculateInitialSafetyObeabe(sortedSpades) < 0.34);
	}
}