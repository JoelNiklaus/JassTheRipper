package com.zuehlke.jasschallenge.client.game.strategy;

import com.zuehlke.jasschallenge.client.game.GameSession;
import com.zuehlke.jasschallenge.client.game.strategy.helpers.JassHelper;
import com.zuehlke.jasschallenge.game.Trumpf;
import com.zuehlke.jasschallenge.game.cards.Card;
import com.zuehlke.jasschallenge.game.cards.Color;
import com.zuehlke.jasschallenge.game.mode.Mode;
import org.junit.Test;

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
    private JassTheRipperJassStrategy jassStrategy = new JassTheRipperJassStrategy(StrengthLevel.FAST);
    private int shiftValue = jassStrategy.MAX_SHIFT_RATING_VAL;

    private Set<Card> cards1 = EnumSet.of(Card.CLUB_ACE, Card.CLUB_EIGHT, Card.CLUB_JACK, Card.DIAMOND_SIX, Card.DIAMOND_SEVEN, Card.SPADE_QUEEN, Card.HEART_TEN, Card.SPADE_NINE, Card.SPADE_KING);
    private Set<Card> cards2 = EnumSet.of(Card.HEART_ACE, Card.HEART_EIGHT, Card.HEART_JACK, Card.CLUB_SIX, Card.CLUB_SEVEN, Card.DIAMOND_QUEEN, Card.SPADE_TEN, Card.DIAMOND_NINE, Card.DIAMOND_JACK);
    private Set<Card> cards3 = EnumSet.of(Card.SPADE_ACE, Card.SPADE_EIGHT, Card.SPADE_JACK, Card.HEART_SIX, Card.HEART_SEVEN, Card.CLUB_QUEEN, Card.DIAMOND_TEN, Card.CLUB_NINE, Card.CLUB_JACK);
    private Set<Card> cards4 = EnumSet.of(Card.DIAMOND_ACE, Card.DIAMOND_EIGHT, Card.DIAMOND_JACK, Card.SPADE_SIX, Card.SPADE_SEVEN, Card.HEART_QUEEN, Card.CLUB_TEN, Card.HEART_NINE, Card.HEART_JACK);
    private Set<Card> allClubs = EnumSet.of(Card.CLUB_ACE, Card.CLUB_KING, Card.CLUB_QUEEN, Card.CLUB_JACK, Card.CLUB_TEN, Card.CLUB_NINE, Card.CLUB_EIGHT, Card.CLUB_SEVEN, Card.CLUB_SIX);
    private Set<Card> veryGoodUndeUfe = EnumSet.of(Card.CLUB_SIX, Card.CLUB_EIGHT, Card.DIAMOND_SIX, Card.DIAMOND_SEVEN, Card.DIAMOND_NINE, Card.SPADE_SIX, Card.SPADE_SEVEN, Card.HEART_TEN, Card.CLUB_NINE);
    private Set<Card> veryGoodDiamondTrumpf = EnumSet.of(Card.DIAMOND_JACK, Card.DIAMOND_NINE, Card.DIAMOND_ACE, Card.DIAMOND_TEN, Card.CLUB_ACE, Card.CLUB_QUEEN, Card.HEART_KING, Card.HEART_JACK, Card.HEART_NINE);
    private Set<Card> definitelyShift = EnumSet.of(Card.CLUB_SIX, Card.CLUB_EIGHT, Card.CLUB_NINE, Card.DIAMOND_SEVEN, Card.DIAMOND_EIGHT, Card.HEART_JACK, Card.HEART_SEVEN, Card.SPADE_EIGHT, Card.SPADE_NINE);


    @Test
    public void chooseTrumpfDiamondWithGreatDiamondCards() throws Exception {
        Set<Card> cards = veryGoodDiamondTrumpf;
        int diamonds = JassHelper.rateColorForTrumpf(cards, Color.DIAMONDS);
        int hearts = JassHelper.rateColorForTrumpf(cards, Color.HEARTS);
        int spades = JassHelper.rateColorForTrumpf(cards, Color.SPADES);
        int clubs = JassHelper.rateColorForTrumpf(cards, Color.CLUBS);
        int obeAbe = JassHelper.rateObeabe(cards);
        int undeUfe = JassHelper.rateUndeufe(cards);
        System.out.println("Diamonds: " + diamonds + ", Hearts: " + hearts + ", Spades: " + spades + ", Clubs: " + clubs);
        System.out.println("Obeabe: " + obeAbe +", Undeufe: " + undeUfe);
        assertTrue(diamonds > hearts);
        assertTrue(diamonds > spades);
        assertTrue(diamonds > clubs);
        assertTrue(diamonds > obeAbe);
        assertTrue(diamonds > undeUfe);
        assertEquals(jassStrategy.chooseTrumpf(cards, gameSession, false), Mode.from(Trumpf.TRUMPF, Color.DIAMONDS));
    }

    @Test
    public void testRateUndeUfeBestWithGreatUndeUfeCards() throws Exception {
        Set<Card> cards = veryGoodUndeUfe;
        int diamonds = JassHelper.rateColorForTrumpf(cards, Color.DIAMONDS);
        int hearts = JassHelper.rateColorForTrumpf(cards, Color.HEARTS);
        int spades = JassHelper.rateColorForTrumpf(cards, Color.SPADES);
        int clubs = JassHelper.rateColorForTrumpf(cards, Color.CLUBS);
        int obeAbe = JassHelper.rateObeabe(cards);
        int undeUfe = JassHelper.rateUndeufe(cards);
        System.out.println("Diamonds: " + diamonds + ", Hearts: " + hearts + ", Spades: " + spades + ", Clubs: " + clubs);
        System.out.println("Obeabe: " + obeAbe +", Undeufe: " + undeUfe);
        assertTrue(undeUfe > obeAbe);
        assertTrue(undeUfe > hearts);
        assertTrue(undeUfe > spades);
        assertTrue(undeUfe > clubs);
        assertTrue(undeUfe > diamonds);
        assertEquals(jassStrategy.chooseTrumpf(cards, gameSession, false), Mode.bottomUp());
    }

    @Test
    public void testDefinitelyShift() throws Exception {
        Set<Card> cards = definitelyShift;
        int diamonds = JassHelper.rateColorForTrumpf(cards, Color.DIAMONDS);
        int hearts = JassHelper.rateColorForTrumpf(cards, Color.HEARTS);
        int spades = JassHelper.rateColorForTrumpf(cards, Color.SPADES);
        int clubs = JassHelper.rateColorForTrumpf(cards, Color.CLUBS);
        int obeAbe = JassHelper.rateObeabe(cards);
        int undeUfe = JassHelper.rateUndeufe(cards);
        System.out.println("Diamonds: " + diamonds + ", Hearts: " + hearts + ", Spades: " + spades + ", Clubs: " + clubs);
        System.out.println("Obeabe: " + obeAbe +", Undeufe: " + undeUfe);
        assertTrue(undeUfe < shiftValue);
        assertTrue(obeAbe < shiftValue);
        assertTrue(diamonds < shiftValue);
        assertTrue(hearts < shiftValue);
        assertTrue(spades < shiftValue);
        assertTrue(clubs < shiftValue);
        assertEquals(jassStrategy.chooseTrumpf(cards, gameSession, false), Mode.shift());
    }

    @Test
    public void testAllClubs() throws Exception {
        Set<Card> cards = allClubs;
        int diamonds = JassHelper.rateColorForTrumpf(cards, Color.DIAMONDS);
        int hearts = JassHelper.rateColorForTrumpf(cards, Color.HEARTS);
        int spades = JassHelper.rateColorForTrumpf(cards, Color.SPADES);
        int clubs = JassHelper.rateColorForTrumpf(cards, Color.CLUBS);
        int obeAbe = JassHelper.rateObeabe(cards);
        int undeUfe = JassHelper.rateUndeufe(cards);
        System.out.println("Diamonds: " + diamonds + ", Hearts: " + hearts + ", Spades: " + spades + ", Clubs: " + clubs);
        System.out.println("Obeabe: " + obeAbe +", Undeufe: " + undeUfe);
        assertTrue(undeUfe == obeAbe);
        assertTrue(obeAbe > diamonds);
        assertTrue(obeAbe < clubs);
        assertTrue(obeAbe > hearts);
        assertTrue(obeAbe > spades);
        assertEquals(jassStrategy.chooseTrumpf(cards, gameSession, false), Mode.from(Trumpf.TRUMPF, Color.CLUBS));
    }

    @Test
    public void testRateObeAbeWithAllClubs() throws Exception {
        // 180 is maximum amount of points
        assertEquals(180, JassHelper.rateObeabeColor(allClubs,Color.CLUBS));
        assertEquals(0, JassHelper.rateObeabeColor(allClubs, Color.DIAMONDS));
        assertEquals(0, JassHelper.rateObeabeColor(allClubs, Color.HEARTS));
        assertEquals(0, JassHelper.rateObeabeColor(allClubs, Color.SPADES));
        assertEquals(180, JassHelper.rateObeabe(allClubs));
    }

    @Test
    public void testRateUndeUfeWithAllClubs() throws Exception {
        // 180 is maximum amount of points
        assertEquals(180, JassHelper.rateUndeufeColor(allClubs,Color.CLUBS));
        assertEquals(0, JassHelper.rateUndeufeColor(allClubs, Color.DIAMONDS));
        assertEquals(0, JassHelper.rateUndeufeColor(allClubs, Color.HEARTS));
        assertEquals(0, JassHelper.rateUndeufeColor(allClubs, Color.SPADES));
        assertEquals(180, JassHelper.rateUndeufe(allClubs));
    }

    @Test
    public void testRateObeAbeWithIsBetween0And180() throws Exception {
        assertTrue(JassHelper.rateObeabe(cards1) >= 0);
        assertTrue(JassHelper.rateObeabe(cards2) >= 0);
        assertTrue(JassHelper.rateObeabe(cards3) >= 0);
        assertTrue(JassHelper.rateObeabe(cards4) >= 0);
        assertTrue(JassHelper.rateObeabe(cards1) <= 180);
        assertTrue(JassHelper.rateObeabe(cards2) <= 180);
        assertTrue(JassHelper.rateObeabe(cards3) <= 180);
        assertTrue(JassHelper.rateObeabe(cards4) <= 180);
    }

    @Test
    public void testRateUndeUfeWithIsBetween0And180() throws Exception {
        assertTrue(JassHelper.rateUndeufe(cards1) >= 0);
        assertTrue(JassHelper.rateUndeufe(cards2) >= 0);
        assertTrue(JassHelper.rateUndeufe(cards3) >= 0);
        assertTrue(JassHelper.rateUndeufe(cards4) >= 0);
        assertTrue(JassHelper.rateUndeufe(cards1) <= 180);
        assertTrue(JassHelper.rateUndeufe(cards2) <= 180);
        assertTrue(JassHelper.rateUndeufe(cards3) <= 180);
        assertTrue(JassHelper.rateUndeufe(cards4) <= 180);
    }

    @Test
    public void testCalculateInitialSafety() {
        List<Card> sortedClubs = JassHelper.sortCardsOfColorDescending(cards1, Color.CLUBS);
        List<Card> sortedSpades = JassHelper.sortCardsOfColorDescending(cards1, Color.SPADES);
        assertEquals(JassHelper.calculateInitialSafetyObeabe(sortedClubs), 1.0, 0.05);
        assertTrue(JassHelper.calculateInitialSafetyObeabe(sortedSpades) > 0.33);
        assertTrue(JassHelper.calculateInitialSafetyObeabe(sortedSpades) < 0.34);

    }

    @Test
    public void testCalculateInitialSafetyRespectingPlayedCards() {
        List<Card> sortedClubs = JassHelper.sortCardsOfColorDescending(EnumSet.of(Card.CLUB_TEN, Card.CLUB_NINE, Card.SPADE_QUEEN), Color.CLUBS);
        List<Card> playedClubs = JassHelper.sortCardsOfColorDescending(EnumSet.of(Card.CLUB_KING, Card.CLUB_QUEEN, Card.SPADE_QUEEN), Color.CLUBS);
        List<Card> sortedSpades = JassHelper.sortCardsOfColorDescending(cards1, Color.SPADES);
        List<Card> playedSpades = JassHelper.sortCardsOfColorDescending(EnumSet.of(Card.SPADE_ACE, Card.SPADE_NINE, Card.CLUB_QUEEN), Color.SPADES);
        assertEquals(JassHelper.calculateInitialSafetyObeabeRespectingPlayedCards(sortedSpades, playedSpades), 1.0, 0.05);
        assertEquals(JassHelper.calculateInitialSafetyObeabeRespectingPlayedCards(sortedClubs, playedClubs), 1f/9, 0.05);
        List<Card> sortedClubsUndeUfe = JassHelper.sortCardsOfColorAscending(EnumSet.of(Card.CLUB_EIGHT, Card.CLUB_NINE, Card.SPADE_QUEEN), Color.CLUBS);
        List<Card> playedClubsUndeUfe = JassHelper.sortCardsOfColorAscending(EnumSet.of(Card.CLUB_SEVEN, Card.CLUB_QUEEN, Card.SPADE_QUEEN), Color.CLUBS);
        assertEquals(JassHelper.calculateInitialSafetyUndeUfeRespectingPlayedCards(sortedClubsUndeUfe, playedClubsUndeUfe), 1f/3, 0.05);
        List<Card> sortedClubsUndeUfe1 = JassHelper.sortCardsOfColorAscending(EnumSet.of(Card.CLUB_TEN, Card.CLUB_NINE, Card.SPADE_QUEEN), Color.CLUBS);
        List<Card> playedClubsUndeUfe1 = JassHelper.sortCardsOfColorAscending(EnumSet.of(Card.CLUB_SIX, Card.CLUB_QUEEN, Card.SPADE_KING), Color.CLUBS);
        assertEquals(JassHelper.calculateInitialSafetyUndeUfeRespectingPlayedCards(sortedClubsUndeUfe1, playedClubsUndeUfe1), 1f/9, 0.05);
        List<Card> sortedClubsUndeUfe2 = JassHelper.sortCardsOfColorAscending(EnumSet.of(Card.CLUB_TEN, Card.CLUB_EIGHT, Card.SPADE_QUEEN), Color.CLUBS);
        List<Card> playedClubsUndeUfe2 = JassHelper.sortCardsOfColorAscending(EnumSet.of(Card.CLUB_QUEEN, Card.SPADE_KING), Color.CLUBS);
        assertEquals(JassHelper.calculateInitialSafetyUndeUfeRespectingPlayedCards(sortedClubsUndeUfe2, playedClubsUndeUfe2), 1f/9, 0.05);
        assertEquals(JassHelper.calculateInitialSafetyUndeUfeRespectingPlayedCards(sortedClubsUndeUfe2, playedClubsUndeUfe2), JassHelper.calculateInitialSafetyUndeUfe(sortedClubsUndeUfe2), 0.05);

    }

    @Test
    public void testRateUndeUfeWithAllClubsRemaining() throws Exception {
        Set<Card> playerCards = EnumSet.of(Card.CLUB_TEN, Card.CLUB_NINE, Card.SPADE_QUEEN, Card.CLUB_KING, Card.CLUB_JACK);
        Set<Card> playerCardsOfColorClubs = JassHelper.getCardsOfColor(playerCards, Color.CLUBS);
        Set<Card> playedCards = EnumSet.of(Card.CLUB_ACE, Card.CLUB_QUEEN, Card.CLUB_EIGHT, Card.CLUB_SEVEN, Card.CLUB_SIX);
        // 180 is maximum amount of points
        assertEquals(20 * playerCardsOfColorClubs.size(), JassHelper.rateColorUndeUfeRespectingAlreadyPlayedCards(playerCards, playedCards, Color.CLUBS), 1);
        assertEquals(20 * playerCardsOfColorClubs.size(), JassHelper.rateColorObeAbeRespectingAlreadyPlayedCards(playerCards, playedCards, Color.CLUBS), 1);
        assertEquals(0, JassHelper.rateColorUndeUfeRespectingAlreadyPlayedCards(playerCards, playedCards, Color.DIAMONDS), 1);
        assertEquals(0, JassHelper.rateColorUndeUfeRespectingAlreadyPlayedCards(playerCards, playedCards, Color.SPADES), 1);
        assertEquals(0, JassHelper.rateColorUndeUfeRespectingAlreadyPlayedCards(playerCards, playedCards, Color.HEARTS), 1);
        assertEquals(0, JassHelper.rateColorUndeUfeRespectingAlreadyPlayedCards(playerCards, playedCards, Color.DIAMONDS), 1);
        assertEquals(0, JassHelper.rateColorUndeUfeRespectingAlreadyPlayedCards(playerCards, playedCards, Color.SPADES), 1);
        assertEquals(0, JassHelper.rateColorUndeUfeRespectingAlreadyPlayedCards(playerCards, playedCards, Color.HEARTS), 1);
    }

    @Test
    public void testNumberOfCardsBetween() throws Exception {
        Set<Card> playedCards = EnumSet.of(Card.CLUB_ACE, Card.CLUB_QUEEN, Card.CLUB_TEN, Card.CLUB_SEVEN);
        List<Card> playedClubsDesc = JassHelper.sortCardsOfColorDescending(playedCards, Color.CLUBS);
        List<Card> playedClubsAsc = JassHelper.sortCardsOfColorAscending(playedCards, Color.CLUBS);
        assertEquals(0, JassHelper.calculateNumberOfCardsInbetweenObeAbeRespectingPlayedCards(Card.CLUB_JACK, Card.CLUB_NINE, playedClubsDesc));
        assertEquals(0, JassHelper.calculateNumberOfCardsInbetweenUndeUfeRespectingPlayedCards(Card.CLUB_NINE, Card.CLUB_JACK, playedClubsAsc));

        assertEquals(1, JassHelper.calculateNumberOfCardsInbetweenObeAbeRespectingPlayedCards(Card.CLUB_JACK, Card.CLUB_EIGHT, playedClubsDesc));
        assertEquals(2, JassHelper.calculateNumberOfCardsInbetweenUndeUfeRespectingPlayedCards(Card.CLUB_SIX, Card.CLUB_JACK, playedClubsAsc));
    }

    @Test
    public void getCardRank() {
        // Test that the Ace has Rank 9
        assertEquals(9, Card.CLUB_ACE.getRank());
    }

}