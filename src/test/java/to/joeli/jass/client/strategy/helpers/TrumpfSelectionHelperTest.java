package to.joeli.jass.client.strategy.helpers;

import org.junit.Test;
import to.joeli.jass.client.game.GameSession;
import to.joeli.jass.client.strategy.JassTheRipperJassStrategy;
import to.joeli.jass.game.Trumpf;
import to.joeli.jass.game.cards.Card;
import to.joeli.jass.game.cards.Color;
import to.joeli.jass.game.mode.Mode;

import java.util.Arrays;
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

	private GameSession gameSession = GameSessionBuilder.newSession().createGameSession();
	private JassTheRipperJassStrategy jassStrategy = JassTheRipperJassStrategy.getTestInstance();
	private int shiftValue = TrumpfSelectionHelper.MAX_SHIFT_RATING_VAL;

	private Set<Card> allClubs = EnumSet.of(Card.CLUB_ACE, Card.CLUB_KING, Card.CLUB_QUEEN, Card.CLUB_JACK, Card.CLUB_TEN, Card.CLUB_NINE, Card.CLUB_EIGHT, Card.CLUB_SEVEN, Card.CLUB_SIX);
	private Set<Card> veryGoodUndeUfe = EnumSet.of(Card.CLUB_SIX, Card.CLUB_EIGHT, Card.CLUB_NINE, Card.DIAMOND_SIX, Card.DIAMOND_SEVEN, Card.DIAMOND_NINE, Card.SPADE_SIX, Card.SPADE_SEVEN, Card.HEART_TEN);
	private Set<Card> veryGoodDiamondTrumpf = EnumSet.of(Card.DIAMOND_JACK, Card.DIAMOND_NINE, Card.DIAMOND_ACE, Card.DIAMOND_TEN, Card.CLUB_ACE, Card.CLUB_QUEEN, Card.HEART_KING, Card.HEART_TEN, Card.HEART_NINE);
	private Set<Card> shiftOrBottomUp = EnumSet.of(Card.CLUB_SIX, Card.CLUB_EIGHT, Card.CLUB_NINE, Card.DIAMOND_SEVEN, Card.DIAMOND_EIGHT, Card.HEART_JACK, Card.HEART_SEVEN, Card.SPADE_EIGHT, Card.SPADE_NINE);
	private Set<Card> definitelyShift = EnumSet.of(Card.CLUB_SEVEN, Card.CLUB_EIGHT, Card.CLUB_QUEEN, Card.DIAMOND_EIGHT, Card.DIAMOND_QUEEN, Card.HEART_EIGHT, Card.HEART_QUEEN, Card.SPADE_EIGHT, Card.SPADE_TEN);
	private Set<Card> maybeShift = EnumSet.of(Card.CLUB_SEVEN, Card.CLUB_EIGHT, Card.CLUB_KING, Card.DIAMOND_NINE, Card.DIAMOND_KING, Card.HEART_SIX, Card.HEART_JACK, Card.SPADE_EIGHT, Card.SPADE_KING);
	private Set<Card> topDownNotQuiteGoodEnough = EnumSet.of(Card.CLUB_ACE, Card.CLUB_JACK, Card.CLUB_NINE, Card.DIAMOND_ACE, Card.DIAMOND_TEN, Card.HEART_ACE, Card.HEART_JACK, Card.SPADE_KING, Card.SPADE_EIGHT);
	private Set<Card> bottomUpNotQuiteGoodEnough = EnumSet.of(Card.CLUB_SIX, Card.CLUB_NINE, Card.CLUB_JACK, Card.DIAMOND_SIX, Card.DIAMOND_TEN, Card.HEART_SIX, Card.HEART_NINE, Card.SPADE_SEVEN, Card.SPADE_QUEEN);
	private Set<Card> clubsVersusDiamonds = EnumSet.of(Card.CLUB_EIGHT, Card.CLUB_NINE, Card.CLUB_ACE, Card.DIAMOND_EIGHT, Card.DIAMOND_NINE, Card.DIAMOND_KING, Card.HEART_NINE, Card.SPADE_SEVEN, Card.SPADE_QUEEN);


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
		assertNotEquals(Mode.shift(), jassStrategy.chooseTrumpf(cards, gameSession, true));
	}

	@Test
	public void testDoesNotChooseTopDown() {
		Set<Card> cards = topDownNotQuiteGoodEnough;
		int diamonds = TrumpfSelectionHelper.rateColorForTrumpf(cards, Color.DIAMONDS);
		int hearts = TrumpfSelectionHelper.rateColorForTrumpf(cards, Color.HEARTS);
		int spades = TrumpfSelectionHelper.rateColorForTrumpf(cards, Color.SPADES);
		int clubs = TrumpfSelectionHelper.rateColorForTrumpf(cards, Color.CLUBS);
		int obeAbe = TrumpfSelectionHelper.rateObeabe(cards);
		int undeUfe = TrumpfSelectionHelper.rateUndeufe(cards);
		System.out.println("Diamonds: " + diamonds + ", Hearts: " + hearts + ", Spades: " + spades + ", Clubs: " + clubs);
		System.out.println("Obeabe: " + obeAbe + ", Undeufe: " + undeUfe);
		assertNotEquals(Mode.topDown(), jassStrategy.chooseTrumpf(cards, gameSession, true));
		assertEquals(Mode.trump(Color.CLUBS), jassStrategy.chooseTrumpf(cards, gameSession, true));
	}

	@Test
	public void testUndeUfeRatingEqualsObeAbeRatingForEquivalentCards() {
		int obeAbe = TrumpfSelectionHelper.rateObeabe(topDownNotQuiteGoodEnough);
		int undeUfe = TrumpfSelectionHelper.rateUndeufe(bottomUpNotQuiteGoodEnough);
		System.out.println("Obeabe: " + obeAbe + ", Undeufe: " + undeUfe);
		assertEquals(undeUfe, obeAbe);
	}

	@Test
	public void testClubsVersusDiamonds() {
		Set<Card> cards = clubsVersusDiamonds;
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
		assertEquals(Mode.trump(Color.DIAMONDS), jassStrategy.chooseTrumpf(cards, gameSession, true));
	}
}