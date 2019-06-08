package com.zuehlke.jasschallenge.client.game.strategy;

import com.zuehlke.jasschallenge.client.game.GameSession;
import com.zuehlke.jasschallenge.client.game.strategy.helpers.GameSessionBuilder;
import com.zuehlke.jasschallenge.client.game.strategy.helpers.TrumpfSelectionHelper;
import com.zuehlke.jasschallenge.game.Trumpf;
import com.zuehlke.jasschallenge.game.cards.Card;
import com.zuehlke.jasschallenge.game.cards.Color;
import com.zuehlke.jasschallenge.game.mode.Mode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Created by dominikbriner on 19.05.17.
 */
public class ChooseTrumpfTest {

	// TODO andere trümpfe testen

	private GameSession gameSession = GameSessionBuilder.newSession().createGameSession();
	private JassTheRipperJassStrategy jassStrategy = JassTheRipperJassStrategy.getInstance(StrengthLevel.TEST);
	private int shiftValue = TrumpfSelectionHelper.MAX_SHIFT_RATING_VAL;

	private Set<Card> allClubs = EnumSet.of(Card.CLUB_ACE, Card.CLUB_KING, Card.CLUB_QUEEN, Card.CLUB_JACK, Card.CLUB_TEN, Card.CLUB_NINE, Card.CLUB_EIGHT, Card.CLUB_SEVEN, Card.CLUB_SIX);
	private Set<Card> veryGoodUndeUfe = EnumSet.of(Card.CLUB_SIX, Card.CLUB_EIGHT, Card.DIAMOND_SIX, Card.DIAMOND_SEVEN, Card.DIAMOND_NINE, Card.SPADE_SIX, Card.SPADE_SEVEN, Card.HEART_TEN, Card.CLUB_NINE);
	private Set<Card> veryGoodDiamondTrumpf = EnumSet.of(Card.DIAMOND_JACK, Card.DIAMOND_NINE, Card.DIAMOND_ACE, Card.DIAMOND_TEN, Card.CLUB_ACE, Card.CLUB_QUEEN, Card.HEART_KING, Card.HEART_TEN, Card.HEART_NINE);
	private Set<Card> shiftOrBottomUp = EnumSet.of(Card.CLUB_SIX, Card.CLUB_EIGHT, Card.CLUB_NINE, Card.DIAMOND_SEVEN, Card.DIAMOND_EIGHT, Card.HEART_JACK, Card.HEART_SEVEN, Card.SPADE_EIGHT, Card.SPADE_NINE);
	private Set<Card> definitelyShift = EnumSet.of(Card.CLUB_SEVEN, Card.CLUB_EIGHT, Card.CLUB_QUEEN, Card.DIAMOND_EIGHT, Card.DIAMOND_QUEEN, Card.HEART_EIGHT, Card.HEART_QUEEN, Card.SPADE_EIGHT, Card.SPADE_TEN);
	private Set<Card> maybeShift = EnumSet.of(Card.CLUB_SEVEN, Card.CLUB_EIGHT, Card.CLUB_KING, Card.DIAMOND_NINE, Card.DIAMOND_KING, Card.HEART_SIX, Card.HEART_JACK, Card.SPADE_EIGHT, Card.SPADE_KING);

	@Before
	public void setUp() {
		// IMPORTANT: Because the strategy is a singleton, this might affect later tests. Resetting at the end of this test
		jassStrategy.setTrumpfSelectionMethod(TrumpfSelectionMethod.RULE_BASED);
	}

	@Test
	public void testChooseTrumpfDiamondWithGreatDiamondCards() {
		Set<Card> cards = veryGoodDiamondTrumpf;
		int diamonds = TrumpfSelectionHelper.rateColorForTrumpf(cards, Color.DIAMONDS);
		int hearts = TrumpfSelectionHelper.rateColorForTrumpf(cards, Color.HEARTS);
		int spades = TrumpfSelectionHelper.rateColorForTrumpf(cards, Color.SPADES);
		int clubs = TrumpfSelectionHelper.rateColorForTrumpf(cards, Color.CLUBS);
		int obeAbe = TrumpfSelectionHelper.rateObeabe(cards);
		int undeUfe = TrumpfSelectionHelper.rateUndeufe(cards);
		System.out.println("Diamonds: " + diamonds + ", Hearts: " + hearts + ", Spades: " + spades + ", Clubs: " + clubs);
		System.out.println("Obeabe: " + obeAbe + ", Undeufe: " + undeUfe);
		assertTrue(diamonds > hearts);
		assertTrue(diamonds > spades);
		assertTrue(diamonds > clubs);
		assertTrue(diamonds > obeAbe);
		assertTrue(diamonds > undeUfe);
		jassStrategy.onSessionStarted(gameSession);
		assertEquals(Mode.from(Trumpf.TRUMPF, Color.DIAMONDS), jassStrategy.chooseTrumpf(cards, gameSession, false));
	}

	@Test
	public void testRateUndeUfeBestWithGreatUndeUfeCards() {
		// NOTE: Only run when all trumpfs are enabled
		if (TrumpfSelectionHelper.ALL_TRUMPFS) {
			Set<Card> cards = veryGoodUndeUfe;
			int diamonds = TrumpfSelectionHelper.rateColorForTrumpf(cards, Color.DIAMONDS);
			int hearts = TrumpfSelectionHelper.rateColorForTrumpf(cards, Color.HEARTS);
			int spades = TrumpfSelectionHelper.rateColorForTrumpf(cards, Color.SPADES);
			int clubs = TrumpfSelectionHelper.rateColorForTrumpf(cards, Color.CLUBS);
			int obeAbe = TrumpfSelectionHelper.rateObeabe(cards);
			int undeUfe = TrumpfSelectionHelper.rateUndeufe(cards);
			System.out.println("Diamonds: " + diamonds + ", Hearts: " + hearts + ", Spades: " + spades + ", Clubs: " + clubs);
			System.out.println("Obeabe: " + obeAbe + ", Undeufe: " + undeUfe);
			assertTrue(undeUfe > obeAbe);
			assertTrue(undeUfe > hearts);
			assertTrue(undeUfe > spades);
			assertTrue(undeUfe > clubs);
			assertTrue(undeUfe > diamonds);
			jassStrategy.onSessionStarted(gameSession);
			assertEquals(Mode.bottomUp(), jassStrategy.chooseTrumpf(cards, gameSession, false));
		}
	}

	@Test
	public void testShiftOrBottomUp() {
		// NOTE: Only run when all trumpfs are enabled
		if (TrumpfSelectionHelper.ALL_TRUMPFS) {
			Set<Card> cards = shiftOrBottomUp;
			int diamonds = TrumpfSelectionHelper.rateColorForTrumpf(cards, Color.DIAMONDS);
			int hearts = TrumpfSelectionHelper.rateColorForTrumpf(cards, Color.HEARTS);
			int spades = TrumpfSelectionHelper.rateColorForTrumpf(cards, Color.SPADES);
			int clubs = TrumpfSelectionHelper.rateColorForTrumpf(cards, Color.CLUBS);
			int obeAbe = TrumpfSelectionHelper.rateObeabe(cards);
			int undeUfe = TrumpfSelectionHelper.rateUndeufe(cards);
			System.out.println("Diamonds: " + diamonds + ", Hearts: " + hearts + ", Spades: " + spades + ", Clubs: " + clubs);
			System.out.println("Obeabe: " + obeAbe + ", Undeufe: " + undeUfe);
			assertTrue(undeUfe < shiftValue);
			assertTrue(obeAbe < shiftValue);
			assertTrue(diamonds < shiftValue);
			assertTrue(hearts < shiftValue);
			assertTrue(spades < shiftValue);
			assertTrue(clubs < shiftValue);
			List<Mode> possibleTrumpfs = Arrays.asList(Mode.bottomUp(), Mode.shift());
			jassStrategy.onSessionStarted(gameSession);
			assertTrue(possibleTrumpfs.contains(jassStrategy.chooseTrumpf(cards, gameSession, false)));
		}
	}

	@Test
	public void testDefinitelyShift() {
		Set<Card> cards = definitelyShift;
		int diamonds = TrumpfSelectionHelper.rateColorForTrumpf(cards, Color.DIAMONDS);
		int hearts = TrumpfSelectionHelper.rateColorForTrumpf(cards, Color.HEARTS);
		int spades = TrumpfSelectionHelper.rateColorForTrumpf(cards, Color.SPADES);
		int clubs = TrumpfSelectionHelper.rateColorForTrumpf(cards, Color.CLUBS);
		int obeAbe = TrumpfSelectionHelper.rateObeabe(cards);
		int undeUfe = TrumpfSelectionHelper.rateUndeufe(cards);
		System.out.println("Diamonds: " + diamonds + ", Hearts: " + hearts + ", Spades: " + spades + ", Clubs: " + clubs);
		System.out.println("Obeabe: " + obeAbe + ", Undeufe: " + undeUfe);
		assertTrue(undeUfe < shiftValue);
		assertTrue(obeAbe < shiftValue);
		assertTrue(diamonds < shiftValue);
		assertTrue(hearts < shiftValue);
		assertTrue(spades < shiftValue);
		assertTrue(clubs < shiftValue);
		jassStrategy.onSessionStarted(gameSession);
		assertEquals(Mode.shift(), jassStrategy.chooseTrumpf(cards, gameSession, false));
	}

	@Test
	public void testMaybeShift() {
		Set<Card> cards = maybeShift;
		int diamonds = TrumpfSelectionHelper.rateColorForTrumpf(cards, Color.DIAMONDS);
		int hearts = TrumpfSelectionHelper.rateColorForTrumpf(cards, Color.HEARTS);
		int spades = TrumpfSelectionHelper.rateColorForTrumpf(cards, Color.SPADES);
		int clubs = TrumpfSelectionHelper.rateColorForTrumpf(cards, Color.CLUBS);
		int obeAbe = TrumpfSelectionHelper.rateObeabe(cards);
		int undeUfe = TrumpfSelectionHelper.rateUndeufe(cards);
		System.out.println("Diamonds: " + diamonds + ", Hearts: " + hearts + ", Spades: " + spades + ", Clubs: " + clubs);
		System.out.println("Obeabe: " + obeAbe + ", Undeufe: " + undeUfe);
		assertTrue(undeUfe < shiftValue);
		assertTrue(obeAbe < shiftValue);
		assertTrue(diamonds < shiftValue);
		assertTrue(hearts < shiftValue);
		assertTrue(spades < shiftValue);
		assertTrue(clubs < shiftValue);
		jassStrategy.onSessionStarted(gameSession);
		assertEquals(Mode.shift(), jassStrategy.chooseTrumpf(cards, gameSession, false));
	}

	@Test
	public void testAllClubs() {
		Set<Card> cards = allClubs;
		int diamonds = TrumpfSelectionHelper.rateColorForTrumpf(cards, Color.DIAMONDS);
		int hearts = TrumpfSelectionHelper.rateColorForTrumpf(cards, Color.HEARTS);
		int spades = TrumpfSelectionHelper.rateColorForTrumpf(cards, Color.SPADES);
		int clubs = TrumpfSelectionHelper.rateColorForTrumpf(cards, Color.CLUBS);
		int obeAbe = TrumpfSelectionHelper.rateObeabe(cards);
		int undeUfe = TrumpfSelectionHelper.rateUndeufe(cards);
		System.out.println("Diamonds: " + diamonds + ", Hearts: " + hearts + ", Spades: " + spades + ", Clubs: " + clubs);
		System.out.println("Obeabe: " + obeAbe + ", Undeufe: " + undeUfe);
		assertTrue(undeUfe == obeAbe);
		assertTrue(obeAbe > diamonds);
		assertTrue(obeAbe < clubs);
		assertTrue(obeAbe > hearts);
		assertTrue(obeAbe > spades);
		List<Mode> possibleTrumpfs = Arrays.asList(Mode.bottomUp(), Mode.topDown(), Mode.from(Trumpf.TRUMPF, Color.CLUBS));
		jassStrategy.onSessionStarted(gameSession);
		assertTrue(possibleTrumpfs.contains(jassStrategy.chooseTrumpf(cards, gameSession, false)));
	}

	@Test
	public void testNotChoosesShiftAfterShift() {
		Set<Card> cards = definitelyShift;
		int diamonds = TrumpfSelectionHelper.rateColorForTrumpf(cards, Color.DIAMONDS);
		int hearts = TrumpfSelectionHelper.rateColorForTrumpf(cards, Color.HEARTS);
		int spades = TrumpfSelectionHelper.rateColorForTrumpf(cards, Color.SPADES);
		int clubs = TrumpfSelectionHelper.rateColorForTrumpf(cards, Color.CLUBS);
		int obeAbe = TrumpfSelectionHelper.rateObeabe(cards);
		int undeUfe = TrumpfSelectionHelper.rateUndeufe(cards);
		System.out.println("Diamonds: " + diamonds + ", Hearts: " + hearts + ", Spades: " + spades + ", Clubs: " + clubs);
		System.out.println("Obeabe: " + obeAbe + ", Undeufe: " + undeUfe);
		assertTrue(undeUfe < shiftValue);
		assertTrue(obeAbe < shiftValue);
		assertTrue(diamonds < shiftValue);
		assertTrue(hearts < shiftValue);
		assertTrue(spades < shiftValue);
		assertTrue(clubs < shiftValue);
		jassStrategy.onSessionStarted(gameSession);
		assertNotEquals(Mode.shift(), jassStrategy.chooseTrumpf(cards, gameSession, true));
	}

	@After
	public void tearDown() {
		// Reset to standard behaviour
		jassStrategy.setTrumpfSelectionMethod(TrumpfSelectionMethod.MCTS);
	}
}