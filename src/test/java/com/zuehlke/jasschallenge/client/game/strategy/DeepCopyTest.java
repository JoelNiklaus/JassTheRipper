package com.zuehlke.jasschallenge.client.game.strategy;

import com.rits.cloning.Cloner;
import com.zuehlke.jasschallenge.client.game.*;
import com.zuehlke.jasschallenge.client.game.strategy.deepcopy.DeepCopy;
import com.zuehlke.jasschallenge.client.game.strategy.deepcopy.ObjectCloner;
import com.zuehlke.jasschallenge.game.cards.Card;
import com.zuehlke.jasschallenge.game.mode.Mode;
import org.apache.commons.lang3.SerializationUtils;
import org.junit.Before;
import org.junit.Test;

import java.util.EnumSet;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;

/**
 * Created by joelniklaus on 06.05.17.
 */
public class DeepCopyTest {

	private static final int MAX_TIME = 500 * 1000;
	private static final int NUMBER_OF_RUNS = 1000;

	private Set<Card> cards1 = EnumSet.of(Card.CLUB_ACE, Card.CLUB_EIGHT, Card.CLUB_JACK, Card.DIAMOND_SIX, Card.DIAMOND_SEVEN, Card.SPADE_QUEEN, Card.HEART_TEN, Card.SPADE_NINE, Card.SPADE_KING);
	private Set<Card> cards2 = EnumSet.of(Card.HEART_ACE, Card.HEART_EIGHT, Card.HEART_JACK, Card.CLUB_SIX, Card.CLUB_SEVEN, Card.DIAMOND_QUEEN, Card.SPADE_TEN, Card.DIAMOND_NINE, Card.DIAMOND_JACK);
	private Set<Card> cards3 = EnumSet.of(Card.SPADE_ACE, Card.SPADE_EIGHT, Card.SPADE_JACK, Card.HEART_SIX, Card.HEART_SEVEN, Card.CLUB_QUEEN, Card.DIAMOND_TEN, Card.CLUB_NINE, Card.CLUB_JACK);
	private Set<Card> cards4 = EnumSet.of(Card.DIAMOND_ACE, Card.DIAMOND_EIGHT, Card.DIAMOND_JACK, Card.SPADE_SIX, Card.SPADE_SEVEN, Card.HEART_QUEEN, Card.CLUB_TEN, Card.HEART_NINE, Card.HEART_JACK);
	private Player firstPlayer = new Player("0", "firstPlayer", 0);
	private Player secondPlayer = new Player("1", "secondPlayer", 1);
	private Player thirdPlayer = new Player("2", "thirdPlayer", 2);
	private Player lastPlayer = new Player("3", "lastPlayer", 3);
	private PlayingOrder order = PlayingOrder.createOrder(asList(firstPlayer, secondPlayer, thirdPlayer, lastPlayer));
	private GameSession gameSession;

	@Before
	public void setUp() {
		firstPlayer.setCards(cards1);
		secondPlayer.setCards(cards2);
		thirdPlayer.setCards(cards3);
		lastPlayer.setCards(cards4);

		gameSession = com.zuehlke.jasschallenge.client.game.GameSessionBuilder.newSession()
				.withPlayersInPlayingOrder(order.getPlayersInInitialPlayingOrder())
				.withStartedGame(Mode.topDown())
				.createGameSession();
	}

	@Test
	public void testFastestCopyMechanism() {
		final GameSession gameSession = GameSessionBuilder.newSession()
				.withStartedGame(Mode.bottomUp())
				.createGameSession();

		try {
			long startTime = System.nanoTime();
			SerializationUtils.clone(gameSession);
			long elapsedTime = System.nanoTime() - startTime;
			System.out.println("SerializationUtils " + elapsedTime + "ns");

			startTime = System.nanoTime();
			ObjectCloner.deepCopySerialization(gameSession);
			elapsedTime = System.nanoTime() - startTime;
			System.out.println("ObjectCloner " + elapsedTime + "ns");

			startTime = System.nanoTime();
			new Cloner().deepClone(gameSession);
			elapsedTime = System.nanoTime() - startTime;
			System.out.println("Reflection " + elapsedTime + "ns");

			startTime = System.nanoTime();
			DeepCopy.copy(gameSession);
			elapsedTime = System.nanoTime() - startTime;
			System.out.println("Serialization " + elapsedTime + "ns");

			startTime = System.nanoTime();
			new GameSession(gameSession);
			elapsedTime = System.nanoTime() - startTime;
			System.out.println("CopyConstructor " + elapsedTime + "ns");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testWhereTimeIsSpentCopying() {
		final GameSession gameSessionStarted = GameSessionBuilder.newSession()
				.withStartedGame(Mode.bottomUp())
				.createGameSession();

		final GameSession gameSession = GameSessionBuilder.newSession()
				.createGameSession();

		try {
			long startTime = System.nanoTime();
			new GameSession(gameSession);
			long elapsedTime = System.nanoTime() - startTime;
			System.out.println("GameSession " + elapsedTime + "ns");

			startTime = System.nanoTime();
			new Game(gameSessionStarted.getCurrentGame());
			elapsedTime = System.nanoTime() - startTime;
			System.out.println("Game " + elapsedTime + "ns");

			startTime = System.nanoTime();
			new Player(gameSessionStarted.getCurrentGame().getCurrentPlayer());
			elapsedTime = System.nanoTime() - startTime;
			System.out.println("Player " + elapsedTime + "ns");

			startTime = System.nanoTime();
			new Round(gameSessionStarted.getCurrentGame().getCurrentRound());
			elapsedTime = System.nanoTime() - startTime;
			System.out.println("Round " + elapsedTime + "ns");

			startTime = System.nanoTime();
			new PlayingOrder(gameSessionStarted.getCurrentGame().getOrder());
			elapsedTime = System.nanoTime() - startTime;
			System.out.println("PlayingOrder " + elapsedTime + "ns");

			startTime = System.nanoTime();
			new Result(gameSessionStarted.getCurrentGame().getResult());
			elapsedTime = System.nanoTime() - startTime;
			System.out.println("Result " + elapsedTime + "ns");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testMostRobustCopyMechanism() {
		for (int i = 0; i < NUMBER_OF_RUNS; i++) {
			try {
				long startTime = System.nanoTime();
				SerializationUtils.clone(gameSession);
				long elapsedTime = System.nanoTime() - startTime;
				if (elapsedTime > MAX_TIME)
					System.out.println("SerializationUtils " + elapsedTime + "ns");

				startTime = System.nanoTime();
				ObjectCloner.deepCopySerialization(gameSession);
				elapsedTime = System.nanoTime() - startTime;
				if (elapsedTime > MAX_TIME)
					System.out.println("ObjectCloner " + elapsedTime + "ns");

				startTime = System.nanoTime();
				new Cloner().deepClone(gameSession);
				elapsedTime = System.nanoTime() - startTime;
				if (elapsedTime > MAX_TIME)
					System.out.println("Reflection " + elapsedTime + "ns");

				startTime = System.nanoTime();
				DeepCopy.copy(gameSession);
				elapsedTime = System.nanoTime() - startTime;
				if (elapsedTime > MAX_TIME)
					System.out.println("Serialization " + elapsedTime + "ns");

				startTime = System.nanoTime();
				new GameSession(gameSession);
				elapsedTime = System.nanoTime() - startTime;
				if (elapsedTime > MAX_TIME)
					System.out.println("CopyConstructor " + elapsedTime + "ns");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Test
	public void testCopyGameSession() {
		GameSession originalGameSession = new GameSession(gameSession);

		assertEquals(originalGameSession, gameSession);
		assertNotSame(originalGameSession, gameSession);

		// check player
		originalGameSession.getCurrentGame().getCurrentPlayer().setId("oldId");
		gameSession.getCurrentGame().getCurrentPlayer().setId("newId");
		assertEquals("oldId", originalGameSession.getCurrentGame().getCurrentPlayer().getId());
		assertEquals("newId", gameSession.getCurrentGame().getCurrentPlayer().getId());
		assertNotEquals(originalGameSession.getCurrentGame().getCurrentPlayer(), gameSession.getCurrentGame().getCurrentPlayer());

		assertEquals(9, originalGameSession.getCurrentGame().getCurrentPlayer().getCards().size());
		assertEquals(9, gameSession.getCurrentGame().getCurrentPlayer().getCards().size());

		// check round
		Move move = new Move(new Player("1", "test", 1), Card.CLUB_ACE);
		gameSession.getCurrentGame().getCurrentRound().getMoves().add(move);
		assertTrue(gameSession.getCurrentGame().getCurrentRound().getMoves().contains(move));
		assertFalse(originalGameSession.getCurrentGame().getCurrentRound().getMoves().contains(move));

		// check playing order
		for (Player player : originalGameSession.getCurrentGame().getOrder().getPlayersInInitialPlayingOrder()) {
			assertEquals(9, player.getCards().size());
		}
		for (Player player : gameSession.getCurrentGame().getOrder().getPlayersInInitialPlayingOrder()) {
			assertEquals(9, player.getCards().size());
		}
		gameSession.getCurrentGame().getOrder().getCurrentPlayer().getCards().remove(Card.CLUB_ACE);
		assertTrue(originalGameSession.getCurrentGame().getOrder().getCurrentPlayer().getCards().contains(Card.CLUB_ACE));
		assertFalse(gameSession.getCurrentGame().getOrder().getCurrentPlayer().getCards().contains(Card.CLUB_ACE));
	}

	@Test
	public void testCopyGame() {
		Game originalGame = gameSession.getCurrentGame();
		originalGame.getCurrentPlayer().setId("oldId");
		Game game = new Game(originalGame);

		assertEquals(originalGame, game);
		assertNotSame(originalGame, game);

		// check player
		game.getCurrentPlayer().setId("newId");
		assertEquals("oldId", originalGame.getCurrentPlayer().getId());
		assertEquals("newId", game.getCurrentPlayer().getId());
		assertNotEquals(originalGame.getCurrentPlayer(), game.getCurrentPlayer());

		assertEquals(9, originalGame.getCurrentPlayer().getCards().size());
		assertEquals(9, game.getCurrentPlayer().getCards().size());

		// check round
		Move move = new Move(new Player("1", "test", 1), Card.CLUB_ACE);
		game.getCurrentRound().getMoves().add(move);
		assertTrue(game.getCurrentRound().getMoves().contains(move));
		assertFalse(originalGame.getCurrentRound().getMoves().contains(move));

		// check playing order
		for (Player player : originalGame.getOrder().getPlayersInInitialPlayingOrder()) {
			assertEquals(9, player.getCards().size());
		}
		for (Player player : game.getOrder().getPlayersInInitialPlayingOrder()) {
			assertEquals(9, player.getCards().size());
		}
		game.getOrder().getCurrentPlayer().getCards().remove(Card.CLUB_ACE);
		assertTrue(originalGame.getOrder().getCurrentPlayer().getCards().contains(Card.CLUB_ACE));
		assertFalse(game.getOrder().getCurrentPlayer().getCards().contains(Card.CLUB_ACE));
	}


	@Test
	public void testCopyPlayer() {
		Player originalPlayer = new Player("1", "test", 1);
		originalPlayer.setCards(cards1);
		Player player = new Player(originalPlayer);

		assertEquals(originalPlayer, player);
		assertNotSame(originalPlayer, player);

		player.setId("2");
		assertNotEquals(originalPlayer.getId(), player.getId());

		assertEquals(9, originalPlayer.getCards().size());
		assertEquals(9, player.getCards().size());
		assertNotSame(originalPlayer.getCards(), player.getCards());

		player.getCards().remove(Card.CLUB_ACE);
		assertTrue(originalPlayer.getCards().contains(Card.CLUB_ACE));
		assertFalse(player.getCards().contains(Card.CLUB_ACE));

		assertEquals(originalPlayer.getName(), player.getName());
		assertEquals(originalPlayer.getCurrentJassStrategy(), player.getCurrentJassStrategy());
	}

	@Test
	public void testCopyResult() {
		Game game = gameSession.getCurrentGame();

		Result originalResult = game.getResult();
		Result result = new Result(originalResult);

		assertEquals(originalResult, result);
		assertNotSame(originalResult, result);
	}


	@Test
	public void testCopyPlayingOrder() {
		Game game = gameSession.getCurrentGame();

		PlayingOrder originalPlayingOrder = game.getOrder();
		originalPlayingOrder.getCurrentPlayer().setId("oldId");
		PlayingOrder playingOrder = new PlayingOrder(originalPlayingOrder);

		assertEquals(originalPlayingOrder, playingOrder);
		assertNotSame(originalPlayingOrder, playingOrder);

		playingOrder.getCurrentPlayer().setId("newId");

		assertEquals("oldId", originalPlayingOrder.getCurrentPlayer().getId());
		assertEquals("newId", playingOrder.getCurrentPlayer().getId());
		assertEquals(9, originalPlayingOrder.getCurrentPlayer().getCards().size());
		assertEquals(9, playingOrder.getCurrentPlayer().getCards().size());
	}

	@Test
	public void testCopyMove() {
		Move originalMove = new Move(new Player("1", "test", 1), Card.CLUB_ACE);
		Move move = new Move(originalMove);

		assertEquals(originalMove, move);
		assertNotSame(originalMove, move);
	}

	@Test
	public void testCopyRound() {
		Game game = gameSession.getCurrentGame();

		Round originalRound = game.getCurrentRound();
		Round round = new Round(originalRound);

		assertEquals(originalRound, round);
		assertNotSame(originalRound, round);

		assertEquals(originalRound.getCurrentPlayer(), round.getCurrentPlayer());
		assertTrue(round.getMoves().isEmpty());
		assertTrue(originalRound.getMoves().isEmpty());

		round.makeMove(new Move(game.getCurrentPlayer(), Card.CLUB_ACE));

		assertFalse(round.getMoves().isEmpty());
		assertTrue(originalRound.getMoves().isEmpty());

		assertNotEquals(originalRound.getCurrentPlayer(), round.getCurrentPlayer());
	}


}