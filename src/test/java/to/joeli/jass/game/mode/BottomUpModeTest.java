package to.joeli.jass.game.mode;

import com.pholser.junit.quickcheck.ForAll;
import to.joeli.jass.client.game.Game;
import to.joeli.jass.client.game.Move;
import to.joeli.jass.client.game.Player;
import to.joeli.jass.game.cards.Card;
import to.joeli.jass.game.cards.Color;
import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.junit.contrib.theories.Theories;
import org.junit.contrib.theories.Theory;
import org.junit.runner.RunWith;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static to.joeli.jass.game.cards.Card.*;
import static to.joeli.jass.game.cards.Color.CLUBS;
import static to.joeli.jass.game.cards.Color.HEARTS;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeThat;

@RunWith(Theories.class)
public class BottomUpModeTest {

    @Test
    public void calculateScore_withSomeCards() {

        final Set<Card> playedCards = EnumSet.of(DIAMOND_KING, DIAMOND_NINE, CLUB_SEVEN, CLUB_TEN);

        final int score = Mode.bottomUp().calculateScore(playedCards);

        assertThat(score, equalTo(14));
    }

    @Test
    public void calculateScore_sixIsPlayed() {

        final Set<Card> playedCards = EnumSet.of(CLUB_SIX);

        final int score = Mode.bottomUp().calculateScore(playedCards);

        assertThat(score, equalTo(11));
    }

    @Test
    public void calculateScore_aceIsPlayed() {

        final Set<Card> playedCards = EnumSet.of(CLUB_ACE);

        final int score = Mode.bottomUp().calculateScore(playedCards);

        assertThat(score, equalTo(0));
    }

    @Test
    public void calculateScore_eightWasPlayed() {

        final Set<Card> playedCards = EnumSet.of(CLUB_EIGHT);

        final int score = Mode.bottomUp().calculateScore(playedCards);

        assertThat(score, equalTo(8));
    }

    @Test
    public void calculateScore_lastRoundWasPlayed() {

        final Set<Card> cards = EnumSet.of(DIAMOND_ACE, HEART_SIX, HEART_TEN);

        final int score = Mode.bottomUp().calculateRoundScore(Game.LAST_ROUND_NUMBER, cards);

        assertThat(score, equalTo(26));
    }


    @Test
    public void calculateScore_() {

        final Set<Card> cards = EnumSet.of(Card.SPADE_JACK, SPADE_QUEEN, SPADE_KING, SPADE_ACE);

        final int score = Mode.bottomUp().calculateRoundScore(5, cards);

        assertThat(score, equalTo((2+3+4)));
    }

    @Theory
    public void canPlayCard_whenNoCardIsPlayed_everyCardIsAllowed(
            @ForAll Card cardToPlay) {

        final Set<Card> alreadyPlayedCards = EnumSet.noneOf(Card.class);
        final Set<Card> playerCards = EnumSet.of(cardToPlay);

        final boolean canCardBePlayed = Mode.bottomUp().canPlayCard(cardToPlay, alreadyPlayedCards, null, playerCards);

        assertTrue(canCardBePlayed);
    }

    @Theory
    public void canPlayCard_withPlayedCards_allowsCardsOfSameColor(
            @ForAll(sampleSize = 20) Card playedCard,
            @ForAll(sampleSize = 20) Card cardToPlay) {

        assumeThat(playedCard.getColor(), CoreMatchers.equalTo(cardToPlay.getColor()));

        final Set<Card> alreadyPlayedCards = EnumSet.of(playedCard);
        final Color roundColor = playedCard.getColor();
        final Set<Card> playerCards = EnumSet.of(cardToPlay);

        final boolean canCardBePlayed = Mode.bottomUp().canPlayCard(cardToPlay, alreadyPlayedCards, roundColor, playerCards);

        assertTrue(canCardBePlayed);
    }

    @Theory
    public void canPlayCard_withPlayedCards_allowsNoCardsOfOtherColor(
            @ForAll(sampleSize = 20) Card playedCard,
            @ForAll(sampleSize = 20) Card cardToPlay) {

        assumeThat(playedCard, not(CoreMatchers.equalTo(HEART_JACK)));
        assumeThat(playedCard.getColor(), CoreMatchers.equalTo(HEARTS));
        assumeThat(cardToPlay.getColor(), not(CoreMatchers.equalTo(HEARTS)));

        final Set<Card> alreadyPlayedCards = EnumSet.of(playedCard);
        final Color roundColor = playedCard.getColor();
        final Set<Card> playerCards = EnumSet.of(HEART_JACK);

        final boolean canCardBePlayed = Mode.bottomUp().canPlayCard(cardToPlay, alreadyPlayedCards, roundColor, playerCards);

        assertFalse(canCardBePlayed);
    }

    @Test
    public void canPlayCard_playerHasNoCardOfSameColorInDeck_allowsCardOfOtherColor() {

        final EnumSet<Card> alreadyPlayedCards = EnumSet.of(CLUB_SIX);
        final EnumSet<Card> playersCards = EnumSet.of(HEART_EIGHT, HEART_NINE);

        final boolean canCardBePlayed = Mode.bottomUp().canPlayCard(HEART_EIGHT, alreadyPlayedCards, CLUBS, playersCards);

        assertTrue(canCardBePlayed);
    }

    @Test
    public void determineWinner_noMovesWereMade_returnsNull() {

        assertNull(Mode.bottomUp().determineWinningMove(emptyList()));
    }

    @Test
    public void determineWinner_movesWithAllTheSameColor_returnsThePlayerWhichPlayedTheLowestCard() {
        final Player playerA = new Player("a");
        final Player playerB = new Player("b");
        final Player playerC = new Player("c");
        final Player playerD = new Player("d");
        final List<Move> moves = asList(
                new Move(playerA, HEART_TEN),
                new Move(playerB, HEART_QUEEN),
                new Move(playerC, HEART_SEVEN),
                new Move(playerD, HEART_JACK));

        final Player winner = Mode.bottomUp().determineWinningMove(moves).getPlayer();

        assertThat(winner, equalTo(playerC));
    }

    @Test
    public void determineWinner_movesWithDifferentColors__returnsThePlayerWhichPlayedTheLowestCardOfRoundColor() {
        final Player playerA = new Player("a");
        final Player playerB = new Player("b");
        final Player playerC = new Player("c");
        final Player playerD = new Player("d");
        final List<Move> moves = asList(
                new Move(playerA, HEART_JACK),
                new Move(playerB, HEART_NINE),
                new Move(playerC, DIAMOND_EIGHT),
                new Move(playerD, DIAMOND_NINE));

        final Player winner = Mode.bottomUp().determineWinningMove(moves).getPlayer();

        assertThat(winner, equalTo(playerB));
    }


}