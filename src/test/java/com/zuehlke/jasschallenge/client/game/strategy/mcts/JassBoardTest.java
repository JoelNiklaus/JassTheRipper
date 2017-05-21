package com.zuehlke.jasschallenge.client.game.strategy.mcts;

import com.zuehlke.jasschallenge.client.game.Game;
import com.zuehlke.jasschallenge.client.game.Player;
import com.zuehlke.jasschallenge.client.game.PlayingOrder;
import com.zuehlke.jasschallenge.client.game.Team;
import com.zuehlke.jasschallenge.client.game.strategy.helpers.JassHelper;
import com.zuehlke.jasschallenge.client.game.strategy.mcts.src.CallLocation;
import com.zuehlke.jasschallenge.game.Trumpf;
import com.zuehlke.jasschallenge.game.cards.Card;
import com.zuehlke.jasschallenge.game.cards.Color;
import com.zuehlke.jasschallenge.game.mode.Mode;
import org.junit.Before;
import org.junit.Test;

import java.util.EnumSet;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;

/**
 * Created by dominikbriner on 12.05.17.
 */
public class JassBoardTest {

    private Set<Card> allCards = EnumSet.of(Card.CLUB_ACE, Card.CLUB_KING, Card.CLUB_QUEEN, Card.CLUB_JACK, Card.CLUB_TEN, Card.CLUB_NINE, Card.CLUB_EIGHT, Card.CLUB_SEVEN, Card.CLUB_SIX,
            Card.DIAMOND_ACE, Card.DIAMOND_KING, Card.DIAMOND_QUEEN, Card.DIAMOND_JACK, Card.DIAMOND_TEN, Card.DIAMOND_NINE, Card.DIAMOND_EIGHT, Card.DIAMOND_SEVEN, Card.DIAMOND_SIX,
            Card.SPADE_ACE, Card.SPADE_KING, Card.SPADE_QUEEN, Card.SPADE_JACK, Card.SPADE_TEN, Card.SPADE_NINE, Card.SPADE_EIGHT, Card.SPADE_SEVEN, Card.SPADE_SIX,
            Card.HEART_ACE, Card.HEART_KING, Card.HEART_QUEEN, Card.HEART_JACK, Card.HEART_TEN, Card.HEART_NINE, Card.HEART_EIGHT, Card.HEART_SEVEN, Card.HEART_SIX);
    private Set<Card> cards1 = EnumSet.of(Card.CLUB_ACE, Card.CLUB_EIGHT, Card.CLUB_JACK, Card.DIAMOND_SIX, Card.DIAMOND_SEVEN, Card.SPADE_QUEEN, Card.HEART_TEN, Card.SPADE_NINE, Card.SPADE_KING);
    private Player player0 = new Player("0", "player0", 0);
    private Player player1 = new Player("1", "player1", 1);
    private Player player2 = new Player("2", "player2", 2);
    private Player player3 = new Player("3", "player3", 3);
    private PlayingOrder order = PlayingOrder.createOrder(asList(player0, player1, player2, player3));
    private Team Team0 = new Team("Team0", asList(player0, player2));
    private Team Team1 = new Team("Team1", asList(player1, player3));
    private Game diamondsGame = Game.startGame(Mode.from(Trumpf.TRUMPF, Color.DIAMONDS), order, asList(Team0, Team1), false);
    private Game obeAbeGame = Game.startGame(Mode.topDown(), order, asList(Team0, Team1), false);

    @Before
    public void setUp() {
        assertEquals(allCards.size(), 36);
    }

    @Test
    public void refineMovesWithJassKnowledgeWhenNotFiltering() throws Exception {
        JassBoard jassBoard = new JassBoard(allCards, diamondsGame, true);
        Set<Card> possibleCards = EnumSet.of(Card.CLUB_ACE, Card.CLUB_KING, Card.CLUB_QUEEN, Card.CLUB_JACK, Card.CLUB_TEN, Card.CLUB_NINE, Card.CLUB_EIGHT, Card.CLUB_SEVEN, Card.CLUB_SIX);
        assertEquals(JassHelper.refineCardsWithJassKnowledge(possibleCards, diamondsGame), possibleCards);
    }

    @Test
    public void refineMovesWithJassKnowledgeNeverRemovesAllCards() throws Exception {
        JassBoard jassBoard = new JassBoard(allCards, diamondsGame, true);
        Set<Card> possibleCards = EnumSet.of(Card.CLUB_ACE, Card.CLUB_KING, Card.CLUB_QUEEN, Card.CLUB_JACK, Card.CLUB_TEN, Card.CLUB_NINE, Card.CLUB_EIGHT, Card.CLUB_SEVEN, Card.CLUB_SIX);
        assertEquals(JassHelper.refineCardsWithJassKnowledge(possibleCards, diamondsGame), possibleCards);
    }

    @Test
    public void getMoves() throws Exception {
        for (int i = 0; i < 100; i++) {
            JassBoard jassBoard = new JassBoard(cards1, diamondsGame, true);
            assertTrue(jassBoard.getMoves(CallLocation.playout).size() > 0);
            assertTrue(jassBoard.getMoves(CallLocation.treePolicy).size() > 0);
        }
    }

    @Test
    public void getMovesObeAbe() throws Exception {
        for (int i = 0; i < 100; i++) {
            JassBoard jassBoard = new JassBoard(cards1, obeAbeGame, false);
            // should not get filtered
            assertEquals(jassBoard.getMoves(CallLocation.playout).size(), 9);
            assertEquals(jassBoard.getMoves(CallLocation.treePolicy).size(), 9);
        }
    }

    @Test
    public void getMovesNeverReturnsEmptyArrayList() throws Exception {
        for (int i = 0; i < 100; i++) {
            JassBoard jassBoard = new JassBoard(cards1, obeAbeGame, false);
            // should not get filtered
            JassBoard jassBoard2 = new JassBoard(JassHelper.testPickRandomSubSet(allCards, 9), obeAbeGame, false);
            assertTrue(jassBoard2.getMoves(CallLocation.playout).size() > 0);
            assertTrue(jassBoard2.getMoves(CallLocation.treePolicy).size() > 0);
        }
    }

}