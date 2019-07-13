package to.joeli.jass.game.mode;

import com.pholser.junit.quickcheck.ForAll;
import to.joeli.jass.client.game.Game;
import to.joeli.jass.client.game.Move;
import to.joeli.jass.client.game.Player;
import to.joeli.jass.game.cards.Card;
import to.joeli.jass.game.cards.Color;
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
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeThat;

@RunWith(Theories.class)
public class TopDownModeTest {

    @Test
    public void calculateScore_eightWasPlayed() {

        final Set<Card> playedCards = EnumSet.of(CLUB_EIGHT);

        final int score = Mode.topDown().calculateScore(playedCards);

        assertThat(score, equalTo(8));
    }

    @Test
    public void calculateScore_sixIsPlayed() {

        final Set<Card> playedCards = EnumSet.of(CLUB_SIX);

        final int score = Mode.topDown().calculateScore(playedCards);

        assertThat(score, equalTo(0));
    }

    @Test
    public void calculateScore_aceIsPlayed() {

        final Set<Card> playedCards = EnumSet.of(CLUB_ACE);

        final int score = Mode.topDown().calculateScore(playedCards);

        assertThat(score, equalTo(11));
    }

    @Test
    public void calculateScore_ifNoCardsWerePlayed_returnsZero() {

        int score = Mode.topDown().calculateScore(EnumSet.noneOf(Card.class));

        assertThat(score, equalTo(0));
    }

    @Test
    public void calculateScore_someCardsArePlayed_returnsSumOfCardScores() {

        final EnumSet<Card> cards = EnumSet.of(DIAMOND_ACE, HEART_SIX, HEART_TEN);

        final int score = Mode.topDown().calculateScore(cards);

        assertThat(score, equalTo((11+10)));
    }

    @Test
    public void calculateScore_lastRoundWasPlayed() {

        final EnumSet<Card> cards = EnumSet.of(DIAMOND_ACE, HEART_SIX, HEART_TEN);

        final int score = Mode.topDown().calculateRoundScore(Game.LAST_ROUND_NUMBER, cards);

        assertThat(score, equalTo(26));
    }

    @Theory
    public void canPlayCard_whenNoCardIsPlayed_everyCardIsAllowed(
            @ForAll Card cardToPlay) {

        final Set<Card> alreadyPlayedCards = EnumSet.noneOf(Card.class);
        final Set<Card> playerCards = EnumSet.of(cardToPlay);

        final boolean canCardBePlayed = Mode.topDown().canPlayCard(cardToPlay, alreadyPlayedCards, null, playerCards);

        assertTrue(canCardBePlayed);
    }

    @Theory
    public void canPlayCard_withPlayedCards_allowsCardsOfSameColor(
            @ForAll(sampleSize = 20) Card playedCard,
            @ForAll(sampleSize = 20) Card cardToPlay) {

        assumeThat(playedCard.getColor(), equalTo(cardToPlay.getColor()));

        final Set<Card> alreadyPlayedCards = EnumSet.of(playedCard);
        final Color roundColor = playedCard.getColor();
        final Set<Card> playerCards = EnumSet.of(cardToPlay);

        final boolean canCardBePlayed = Mode.topDown().canPlayCard(cardToPlay, alreadyPlayedCards, roundColor, playerCards);

        assertTrue(canCardBePlayed);
    }

    @Theory
    public void canPlayCard_withPlayedCards_allowsNoCardsOfOtherColor(
            @ForAll(sampleSize = 20) Card playedCard,
            @ForAll(sampleSize = 20) Card cardToPlay) {

        assumeThat(playedCard, not(equalTo(HEART_JACK)));
        assumeThat(playedCard.getColor(), equalTo(HEARTS));
        assumeThat(cardToPlay.getColor(), not(equalTo(HEARTS)));

        final Set<Card> alreadyPlayedCards = EnumSet.of(playedCard);
        final Color roundColor = playedCard.getColor();
        final Set<Card> playerCards = EnumSet.of(HEART_JACK);

        final boolean canCardBePlayed = Mode.topDown().canPlayCard(cardToPlay, alreadyPlayedCards, roundColor, playerCards);

        assertFalse(canCardBePlayed);
    }

    @Test
    public void canPlayCard_playerHasNoCardOfSameColorInDeck_allowsCardOfOtherColor() {

        final EnumSet<Card> alreadyPlayedCards = EnumSet.of(CLUB_SIX);
        final EnumSet<Card> playersCards = EnumSet.of(HEART_EIGHT, HEART_NINE);

        final boolean canCardBePlayed = Mode.topDown().canPlayCard(HEART_EIGHT, alreadyPlayedCards, CLUBS, playersCards);

        assertTrue(canCardBePlayed);
    }

    @Test
    public void determineWinner_noMovesWereMade_returnsNull() {

        assertNull(Mode.topDown().determineWinningMove(emptyList()));
    }

    @Test
    public void determineWinner_movesWithAllTheSameColor_returnsThePlayerWhichPlayedTheHighestCard() {
        final Player playerA = new Player("a");
        final Player playerB = new Player("b");
        final Player playerC = new Player("c");
        final Player playerD = new Player("d");
        final List<Move> moves = asList(
                new Move(playerA, HEART_TEN),
                new Move(playerB, HEART_QUEEN),
                new Move(playerC, HEART_SEVEN),
                new Move(playerD, HEART_JACK));

        final Player winner = Mode.topDown().determineWinningMove(moves).getPlayer();

        assertThat(winner, equalTo(playerB));
    }

    @Test
    public void determineWinner_movesWithDifferentColors__returnsThePlayerWhichPlayedTheHighestCardOfRoundColor() {
        final Player playerA = new Player("a");
        final Player playerB = new Player("b");
        final Player playerC = new Player("c");
        final Player playerD = new Player("d");
        final List<Move> moves = asList(
                new Move(playerA, HEART_JACK),
                new Move(playerB, HEART_NINE),
                new Move(playerC, DIAMOND_EIGHT),
                new Move(playerD, DIAMOND_NINE));

        final Player winner = Mode.topDown().determineWinningMove(moves).getPlayer();

        assertThat(winner, equalTo(playerA));
    }

}