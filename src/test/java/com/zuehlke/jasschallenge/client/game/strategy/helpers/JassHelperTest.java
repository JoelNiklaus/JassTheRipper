package com.zuehlke.jasschallenge.client.game.strategy.helpers;

import com.zuehlke.jasschallenge.client.game.*;
import com.zuehlke.jasschallenge.client.game.strategy.mcts.CardMove;
import com.zuehlke.jasschallenge.game.Trumpf;
import com.zuehlke.jasschallenge.game.cards.Card;
import com.zuehlke.jasschallenge.game.cards.Color;
import com.zuehlke.jasschallenge.game.mode.Mode;
import org.junit.Test;

import java.util.*;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;

/**
 * Created by dominikbriner on 20.05.17.
 */
public class JassHelperTest {

	private Player firstPlayer = new Player("0", "firstPlayer", 0);
	private Player secondPlayer = new Player("1", "secondPlayer", 1);
	private Player thirdPlayer = new Player("2", "thirdPlayer", 2);
	private Player lastPlayer = new Player("3", "lastPlayer", 3);
	private PlayingOrder order = PlayingOrder.createOrder(asList(firstPlayer, secondPlayer, thirdPlayer, lastPlayer));



	/* Test refineCardsWithJassKnowledge
	 * Test Stechen
	 * */
	@Test
	public void testRefineCardsWithJassKnowledgeStechen() throws Exception {
		GameSession gameSession = GameSessionBuilder.newSession()
				.withPlayersInPlayingOrder(order.getPlayersInInitialPlayingOrder())
				.withStartedGame(Mode.topDown())
				.createGameSession();
		Game game = gameSession.getCurrentGame();
		PlayingOrder order = game.getCurrentRound().getPlayingOrder();
		gameSession.makeMove(new Move(order.getCurrentPlayer(), Card.CLUB_TEN));
		gameSession.makeMove(new Move(order.getCurrentPlayer(), Card.CLUB_JACK));
		gameSession.makeMove(new Move(order.getCurrentPlayer(), Card.CLUB_QUEEN));

		Set<Card> cards = EnumSet.of(Card.CLUB_SEVEN, Card.CLUB_KING);
		Set<Card> refinedCards = JassHelper.refineCardsWithJassKnowledge(cards, game);
		Set<Card> expectedCards = EnumSet.of(Card.CLUB_KING);
		assertEquals(expectedCards, refinedCards);
	}

	@Test
	public void testRefineCardsStechenWithoutTrumpfIfPossible() throws Exception {
		GameSession gameSession = GameSessionBuilder.newSession()
				.withPlayersInPlayingOrder(order.getPlayersInInitialPlayingOrder())
				.withStartedGame(Mode.from(Trumpf.TRUMPF, Color.DIAMONDS))
				.createGameSession();
		Game game = gameSession.getCurrentGame();
		PlayingOrder order = game.getCurrentRound().getPlayingOrder();
		gameSession.makeMove(new Move(order.getCurrentPlayer(), Card.CLUB_TEN));
		gameSession.makeMove(new Move(order.getCurrentPlayer(), Card.CLUB_JACK));
		gameSession.makeMove(new Move(order.getCurrentPlayer(), Card.CLUB_QUEEN));

		Set<Card> cards = EnumSet.of(Card.CLUB_SEVEN, Card.CLUB_KING, Card.DIAMOND_JACK);
		Set<Card> refinedCards = JassHelper.refineCardsWithJassKnowledge(cards, game);
		Set<Card> expectedCards = EnumSet.of(Card.CLUB_KING);
		assertEquals(expectedCards, refinedCards);
	}

	@Test
	public void testRefineCardsStechenWithTrumpfIfNotPossibleOtherwiseAndScoreHigherThan10() throws Exception {
		GameSession gameSession = GameSessionBuilder.newSession()
				.withPlayersInPlayingOrder(order.getPlayersInInitialPlayingOrder())
				.withStartedGame(Mode.from(Trumpf.TRUMPF, Color.DIAMONDS))
				.createGameSession();
		Game game = gameSession.getCurrentGame();
		PlayingOrder order = game.getCurrentRound().getPlayingOrder();
		gameSession.makeMove(new Move(order.getCurrentPlayer(), Card.CLUB_TEN));
		gameSession.makeMove(new Move(order.getCurrentPlayer(), Card.CLUB_JACK));
		gameSession.makeMove(new Move(order.getCurrentPlayer(), Card.CLUB_QUEEN));

		Set<Card> cards = EnumSet.of(Card.CLUB_SEVEN, Card.DIAMOND_JACK, Card.DIAMOND_NINE);
		Set<Card> refinedCards = JassHelper.refineCardsWithJassKnowledge(cards, game);
		Set<Card> expectedCards = EnumSet.of(Card.DIAMOND_JACK, Card.DIAMOND_NINE);
		assertEquals(expectedCards, refinedCards);
		assertTrue(game.getCurrentRound().calculateScore() > 10);
	}

	@Test
	public void testRefineCardsNotStechenWithTrumpfIfNotPossibleOtherwiseAndScoreLowerThan10() throws Exception {
		GameSession gameSession = GameSessionBuilder.newSession()
				.withPlayersInPlayingOrder(order.getPlayersInInitialPlayingOrder())
				.withStartedGame(Mode.from(Trumpf.TRUMPF, Color.DIAMONDS))
				.createGameSession();
		Game game = gameSession.getCurrentGame();
		PlayingOrder order = game.getCurrentRound().getPlayingOrder();
		gameSession.makeMove(new Move(order.getCurrentPlayer(), Card.CLUB_NINE));
		gameSession.makeMove(new Move(order.getCurrentPlayer(), Card.CLUB_JACK));
		gameSession.makeMove(new Move(order.getCurrentPlayer(), Card.CLUB_QUEEN));

		Set<Card> cards = EnumSet.of(Card.CLUB_SEVEN, Card.DIAMOND_SIX, Card.DIAMOND_SEVEN);
		Set<Card> refinedCards = JassHelper.refineCardsWithJassKnowledge(cards, game);
		assertEquals(cards, refinedCards);
		assertTrue(game.getCurrentRound().calculateScore() < 10);
	}

    /* Test refineCardsWithJassKnowledge
	 * Test Austrumpfen
     * */

	@Test
	public void testAlsoAustrumpfenWhenNotHavingHighTrumpfs() throws Exception {
		GameSession gameSession = GameSessionBuilder.newSession()
				.withPlayersInPlayingOrder(order.getPlayersInInitialPlayingOrder())
				.withStartedGame(Mode.from(Trumpf.TRUMPF, Color.DIAMONDS))
				.createGameSession();
		Game game = gameSession.getCurrentGame();
		PlayingOrder order = game.getCurrentRound().getPlayingOrder();


		Set<Card> cards = EnumSet.of(Card.CLUB_SEVEN, Card.DIAMOND_SIX, Card.DIAMOND_SEVEN, Card.DIAMOND_EIGHT);
		Player player = firstPlayer;
		player.setCards(cards);
		Set<Card> expectedCards = EnumSet.of(Card.DIAMOND_SIX, Card.DIAMOND_SEVEN, Card.DIAMOND_EIGHT);
		Set<Card> refinedCards = JassHelper.refineCardsWithJassKnowledge(cards, game);
		assertEquals(expectedCards, refinedCards);
	}

	@Test
	public void testNotAustrumpfenHavingHighTrumpfs() throws Exception {
		GameSession gameSession = GameSessionBuilder.newSession()
				.withPlayersInPlayingOrder(order.getPlayersInInitialPlayingOrder())
				.withStartedGame(Mode.from(Trumpf.TRUMPF, Color.DIAMONDS))
				.createGameSession();
		Game game = gameSession.getCurrentGame();
		PlayingOrder order = game.getCurrentRound().getPlayingOrder();

		Set<Card> cards = EnumSet.of(Card.CLUB_SEVEN, Card.DIAMOND_JACK, Card.DIAMOND_NINE, Card.DIAMOND_KING);
		Player player = firstPlayer;
		player.setCards(cards);
		Set<Card> refinedCards = JassHelper.refineCardsWithJassKnowledge(cards, game);
		Set<Card> expectedCards = EnumSet.of(Card.DIAMOND_JACK, Card.DIAMOND_NINE, Card.DIAMOND_KING);
		assertEquals(expectedCards, refinedCards);
	}


    /*
     * Test other Methods
     * */

	@Test
	public void testGetTrumps() throws Exception {
		Set<Card> cards = EnumSet.of(Card.CLUB_SEVEN, Card.DIAMOND_JACK, Card.DIAMOND_NINE, Card.DIAMOND_KING);
		Mode trump = Mode.from(Trumpf.TRUMPF, Color.DIAMONDS);
		Set<Card> trumps = JassHelper.getTrumps(cards, trump);
		Set<Card> expectedTrumps = EnumSet.of(Card.DIAMOND_JACK, Card.DIAMOND_NINE, Card.DIAMOND_KING);
		assertEquals(expectedTrumps, trumps);
	}


}