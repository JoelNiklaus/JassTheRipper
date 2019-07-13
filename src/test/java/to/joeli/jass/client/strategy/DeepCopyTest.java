package to.joeli.jass.client.strategy;

import to.joeli.jass.client.game.*;
import to.joeli.jass.client.strategy.helpers.GameSessionBuilder;
import to.joeli.jass.game.cards.Card;
import to.joeli.jass.game.mode.Mode;
import org.junit.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Created by joelniklaus on 06.05.17.
 */
public class DeepCopyTest {

	private Set<Card> cards1 = EnumSet.of(Card.CLUB_ACE, Card.CLUB_EIGHT, Card.CLUB_JACK, Card.DIAMOND_SIX, Card.DIAMOND_SEVEN, Card.SPADE_QUEEN, Card.HEART_TEN, Card.SPADE_NINE, Card.SPADE_KING);

	private GameSession gameSession = GameSessionBuilder.newSession().withStartedGame(Mode.topDown()).createGameSession();

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
			System.out.println("GameSession " + elapsedTime / 1000 + "µs");

			startTime = System.nanoTime();
			new Game(gameSessionStarted.getCurrentGame());
			elapsedTime = System.nanoTime() - startTime;
			System.out.println("Game " + elapsedTime / 1000 + "µs");

			startTime = System.nanoTime();
			new Player(gameSessionStarted.getCurrentGame().getCurrentPlayer());
			elapsedTime = System.nanoTime() - startTime;
			System.out.println("Player " + elapsedTime / 1000 + "µs");

			startTime = System.nanoTime();
			new Round(gameSessionStarted.getCurrentGame().getCurrentRound());
			elapsedTime = System.nanoTime() - startTime;
			System.out.println("Round " + elapsedTime / 1000 + "µs");

			startTime = System.nanoTime();
			new PlayingOrder(gameSessionStarted.getCurrentGame().getOrder());
			elapsedTime = System.nanoTime() - startTime;
			System.out.println("PlayingOrder " + elapsedTime / 1000 + "µs");

			startTime = System.nanoTime();
			new Team(gameSessionStarted.getTeams().get(0));
			elapsedTime = System.nanoTime() - startTime;
			System.out.println("Team " + elapsedTime / 1000 + "µs");

			startTime = System.nanoTime();
			new TeamScore(gameSessionStarted.getCurrentGame().getResult().getTeamAScore());
			elapsedTime = System.nanoTime() - startTime;
			System.out.println("TeamScore " + elapsedTime / 1000 + "µs");

			startTime = System.nanoTime();
			new Result(gameSessionStarted.getCurrentGame().getResult());
			elapsedTime = System.nanoTime() - startTime;
			System.out.println("Result " + elapsedTime / 1000 + "µs");
		} catch (Exception e) {
			e.printStackTrace();
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
		for (Player player : originalGameSession.getCurrentGame().getOrder().getPlayersInInitialOrder()) {
			assertEquals(9, player.getCards().size());
		}
		for (Player player : gameSession.getCurrentGame().getOrder().getPlayersInInitialOrder()) {
			assertEquals(9, player.getCards().size());
		}
		gameSession.getCurrentGame().getOrder().getCurrentPlayer().getCards().remove(Card.CLUB_ACE);
		assertTrue(originalGameSession.getCurrentGame().getOrder().getCurrentPlayer().getCards().contains(Card.CLUB_ACE));
		assertFalse(gameSession.getCurrentGame().getOrder().getCurrentPlayer().getCards().contains(Card.CLUB_ACE));

		// NOTE: At the moment we dont have this constraint. It is very complex to implement, because of many interdependencies!
		//final Player teamPlayer = gameSession.getTeams().get(0).getPlayers().get(1);
		//final Player orderPlayer = gameSession.getGameStartingPlayingOrder().getPlayersInInitialOrder().get(2);
		//ssertSame(teamPlayer, orderPlayer); // References of the two players should be the same!
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
		for (Player player : originalGame.getOrder().getPlayersInInitialOrder()) {
			assertEquals(9, player.getCards().size());
		}
		for (Player player : game.getOrder().getPlayersInInitialOrder()) {
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
		assertEquals(originalPlayer.getJassStrategy(), player.getJassStrategy());
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