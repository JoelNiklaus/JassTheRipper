package com.zuehlke.jasschallenge.client.game.strategy.mcts;

import com.zuehlke.jasschallenge.client.game.Game;
import com.zuehlke.jasschallenge.client.game.Player;
import com.zuehlke.jasschallenge.client.game.PlayingOrder;
import com.zuehlke.jasschallenge.client.game.Team;
import com.zuehlke.jasschallenge.client.game.strategy.helpers.CardSelectionHelper;
import com.zuehlke.jasschallenge.client.game.strategy.mcts.src.CallLocation;
import com.zuehlke.jasschallenge.game.Trumpf;
import com.zuehlke.jasschallenge.game.cards.Card;
import com.zuehlke.jasschallenge.game.cards.Color;
import com.zuehlke.jasschallenge.game.mode.Mode;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;

/**
 * Created by dominikbriner on 12.05.17.
 */
public class JassBoardTest {

	private Set<Card> allCards = EnumSet.copyOf(Arrays.asList(Card.values()));
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
		assertEquals(36, allCards.size());
	}

	@Test
	public void testRefineMovesWithJassKnowledgeWhenNotFiltering() {
		Set<Card> possibleCards = EnumSet.of(Card.CLUB_ACE, Card.CLUB_KING, Card.CLUB_QUEEN, Card.CLUB_JACK, Card.CLUB_TEN, Card.CLUB_NINE, Card.CLUB_EIGHT, Card.CLUB_SEVEN, Card.CLUB_SIX);
		assertEquals(CardSelectionHelper.refineCardsWithJassKnowledge(possibleCards, diamondsGame), possibleCards);
	}

	@Test
	public void testRefineMovesWithJassKnowledgeNeverRemovesAllCards() {
		Set<Card> possibleCards = EnumSet.of(Card.CLUB_ACE, Card.CLUB_KING, Card.CLUB_QUEEN, Card.CLUB_JACK, Card.CLUB_TEN, Card.CLUB_NINE, Card.CLUB_EIGHT, Card.CLUB_SEVEN, Card.CLUB_SIX);
		assertEquals(CardSelectionHelper.refineCardsWithJassKnowledge(possibleCards, diamondsGame), possibleCards);
	}

	@Test
	public void testGetMoves() {
		for (int i = 0; i < 100; i++) {
			JassBoard jassBoard = new JassBoard(cards1, diamondsGame, true);
			assertFalse(jassBoard.getMoves(CallLocation.playout).isEmpty());
			assertFalse(jassBoard.getMoves(CallLocation.treePolicy).isEmpty());
		}
	}

	@Test
	public void testGetMovesObeAbe() {
		for (int i = 0; i < 100; i++) {
			JassBoard jassBoard = new JassBoard(cards1, obeAbeGame, true);
			// should not get filtered
			assertEquals(9, jassBoard.getMoves(CallLocation.playout).size());
			assertEquals(9, jassBoard.getMoves(CallLocation.treePolicy).size());
		}
	}

	@Test
	public void testGetMovesNeverReturnsEmptyArrayList() {
		for (int i = 0; i < 100; i++) {
			JassBoard jassBoard = new JassBoard(cards1, obeAbeGame, true);
			// should not get filtered
			JassBoard jassBoard2 = new JassBoard(JassBoard.pickRandomSubSet(allCards, 9), obeAbeGame, true);
			assertFalse(jassBoard2.getMoves(CallLocation.playout).isEmpty());
			assertFalse(jassBoard2.getMoves(CallLocation.treePolicy).isEmpty());
		}
	}

}