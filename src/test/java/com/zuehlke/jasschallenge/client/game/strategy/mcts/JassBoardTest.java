package com.zuehlke.jasschallenge.client.game.strategy.mcts;

import com.zuehlke.jasschallenge.client.game.*;
import com.zuehlke.jasschallenge.client.game.strategy.helpers.CardSelectionHelper;
import com.zuehlke.jasschallenge.client.game.strategy.mcts.src.CallLocation;
import com.zuehlke.jasschallenge.game.Trumpf;
import com.zuehlke.jasschallenge.game.cards.Card;
import com.zuehlke.jasschallenge.game.cards.Color;
import com.zuehlke.jasschallenge.game.mode.Mode;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;

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

	/**
	 * Picks a random sub set out of the given cards with the given size.
	 *
	 * @param cards
	 * @param numberOfCards
	 * @return
	 */
	static Set<Card> pickRandomSubSet(Set<Card> cards, int numberOfCards) {
		assert (numberOfCards > 0 || numberOfCards <= 9);
		List<Card> listOfCards = new LinkedList<>(cards);
		assert numberOfCards <= listOfCards.size();
		Collections.shuffle(listOfCards);
		List<Card> randomSublist = listOfCards.subList(0, numberOfCards);
		Set<Card> randomSubSet = EnumSet.copyOf(randomSublist);
		assert (cards.containsAll(randomSubSet));
		return randomSubSet;
	}

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
			JassBoard jassBoard = JassBoard.constructCardSelectionJassBoard(cards1, diamondsGame);
			jassBoard.sampleCardDeterminizationToPlayers();

			assertFalse(jassBoard.getMoves(CallLocation.playout).isEmpty());
			assertFalse(jassBoard.getMoves(CallLocation.treePolicy).isEmpty());
		}
	}

	@Test
	public void testGetMovesObeAbe() {
		for (int i = 0; i < 100; i++) {
			JassBoard jassBoard = JassBoard.constructCardSelectionJassBoard(cards1, obeAbeGame);
			jassBoard.sampleCardDeterminizationToPlayers();
			// should not get filtered
			assertEquals(9, jassBoard.getMoves(CallLocation.playout).size());
			assertEquals(9, jassBoard.getMoves(CallLocation.treePolicy).size());
		}
	}

	@Test
	public void testGetMovesNeverReturnsEmptyArrayList() {
		for (int i = 0; i < 100; i++) {
			JassBoard jassBoard = JassBoard.constructCardSelectionJassBoard(cards1, obeAbeGame);
			jassBoard.sampleCardDeterminizationToPlayers();
			// should not get filtered
			JassBoard jassBoard2 = JassBoard.constructCardSelectionJassBoard(pickRandomSubSet(allCards, 9), obeAbeGame);
			jassBoard2.sampleCardDeterminizationToPlayers();
			assertFalse(jassBoard2.getMoves(CallLocation.playout).isEmpty());
			assertFalse(jassBoard2.getMoves(CallLocation.treePolicy).isEmpty());
		}
	}

	@Test
	public void testGetImPossibleCardsForPlayerNormalNotFollowSuit() {
		obeAbeGame.makeMove(new Move(player0, Card.CLUB_SIX));
		obeAbeGame.makeMove(new Move(player1, Card.CLUB_SEVEN));
		obeAbeGame.makeMove(new Move(player2, Card.HEART_EIGHT)); // player 2 did not follow suit
		obeAbeGame.makeMove(new Move(player3, Card.CLUB_KING));
		obeAbeGame.startNextRound();

		Set<Card> impossibleCardsForPlayer = CardKnowledgeBase.getImpossibleCardsForPlayer(obeAbeGame, player2);
		assertEquals(9, impossibleCardsForPlayer.size());

		assertTrue(impossibleCardsForPlayer.contains(Card.CLUB_SIX));
		assertTrue(impossibleCardsForPlayer.contains(Card.CLUB_SEVEN));
		assertTrue(impossibleCardsForPlayer.contains(Card.CLUB_EIGHT));
		assertTrue(impossibleCardsForPlayer.contains(Card.CLUB_NINE));
		assertTrue(impossibleCardsForPlayer.contains(Card.CLUB_TEN));
		assertTrue(impossibleCardsForPlayer.contains(Card.CLUB_JACK));
		assertTrue(impossibleCardsForPlayer.contains(Card.CLUB_QUEEN));
		assertTrue(impossibleCardsForPlayer.contains(Card.CLUB_KING));
		assertTrue(impossibleCardsForPlayer.contains(Card.CLUB_ACE));
	}

	@Test
	public void testGetImPossibleCardsForPlayerTrumpfNotFollowSuit() {
		diamondsGame.makeMove(new Move(player0, Card.DIAMOND_EIGHT));
		diamondsGame.makeMove(new Move(player1, Card.DIAMOND_NINE));
		diamondsGame.makeMove(new Move(player2, Card.HEART_EIGHT)); // player 2 did not follow suit
		diamondsGame.makeMove(new Move(player3, Card.DIAMOND_QUEEN));
		diamondsGame.startNextRound();

		Set<Card> impossibleCardsForPlayer = CardKnowledgeBase.getImpossibleCardsForPlayer(diamondsGame, player2);
		assertEquals(8, impossibleCardsForPlayer.size());

		assertTrue(impossibleCardsForPlayer.contains(Card.DIAMOND_SIX));
		assertTrue(impossibleCardsForPlayer.contains(Card.DIAMOND_SEVEN));
		assertTrue(impossibleCardsForPlayer.contains(Card.DIAMOND_EIGHT));
		assertTrue(impossibleCardsForPlayer.contains(Card.DIAMOND_NINE));
		assertTrue(impossibleCardsForPlayer.contains(Card.DIAMOND_TEN));
		assertFalse(impossibleCardsForPlayer.contains(Card.DIAMOND_JACK)); // special rule for jack
		assertTrue(impossibleCardsForPlayer.contains(Card.DIAMOND_QUEEN));
		assertTrue(impossibleCardsForPlayer.contains(Card.DIAMOND_KING));
		assertTrue(impossibleCardsForPlayer.contains(Card.DIAMOND_ACE));
	}

}