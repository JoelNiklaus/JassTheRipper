package to.joeli.jass.client.game;

import to.joeli.jass.client.game.strategy.helpers.GameSessionBuilder;
import to.joeli.jass.game.cards.Card;
import to.joeli.jass.game.mode.Mode;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class GameSessionTest {
	@Test
	public void newGameSession_withoutStartedGame() {

		final GameSession gameSession = GameSessionBuilder.newSession().createGameSession();

		assertThat(gameSession.getCurrentRound(), is(nullValue()));
	}

	@Test
	public void startNewGame_whenNoGameWasStarted_firstRoundIsStarted() {

		final GameSession gameSession = GameSessionBuilder.newSession().createGameSession();

		gameSession.startNewGame(Mode.topDown(), false);

		assertThat(gameSession.getCurrentRound(), is(not(nullValue())));
	}

	@Test
	public void startNewGame_aGameWasPlayed() {

		final GameSession gameSession = GameSessionBuilder.newSession()
				.withStartedGame(Mode.bottomUp())
				.createGameSession();
		gameSession.makeMove(new Move(gameSession.getPlayersInInitialPlayingOrder().get(0), Card.CLUB_TEN));
		gameSession.startNextRound();
		gameSession.startNewGame(Mode.topDown(), false);

		assertThat(gameSession.getResult().getTeamScore(gameSession.getPlayersInInitialPlayingOrder().get(0)), equalTo(10));
	}

	@Test
	public void startNextRound_aRoundIsAlreadyPlayed_resultPointsAreUpdated() {

		final GameSession gameSession = GameSessionBuilder.newSession().createGameSession();
		final Player playerA = gameSession.getPlayersInInitialPlayingOrder().get(0);
		final Player playerB = gameSession.getPlayersInInitialPlayingOrder().get(1);
		gameSession.startNewGame(Mode.bottomUp(), false);
		gameSession.makeMove(new Move(playerA, Card.CLUB_TEN));
		gameSession.makeMove(new Move(playerB, Card.CLUB_SIX));

		gameSession.startNextRound();

		assertThat(gameSession.getCurrentGame().getResult().getTeamScore(playerA), equalTo(0));
		assertThat(gameSession.getCurrentGame().getResult().getTeamScore(playerB), equalTo(21));
	}

	@Test
	public void startNextRound_multipleRoundsArePlayed_resultsAreCombined() {

		final GameSession gameSession = GameSessionBuilder.newSession().createGameSession();
		final Player player = gameSession.getPlayersInInitialPlayingOrder().get(0);
		gameSession.startNewGame(Mode.topDown(), false);
		gameSession.makeMove(new Move(player, Card.CLUB_TEN));
		gameSession.startNextRound();
		gameSession.makeMove(new Move(player, Card.HEART_ACE));

		gameSession.startNextRound();

		assertThat(gameSession.getCurrentGame().getResult().getTeamScore(player), equalTo(21));
	}

	@Test
	public void startNextRound_multipleRoundsArePlayed_matchBonusIsAdded() {

		final GameSession gameSession = GameSessionBuilder.newSession().createGameSession();
		final Player player = gameSession.getPlayersInInitialPlayingOrder().get(0);
		gameSession.startNewGame(Mode.topDown(), false);
		for (int i = 0; i <= Game.LAST_ROUND_NUMBER; i++) {
			gameSession.makeMove(new Move(player, Card.CLUB_TEN));
			gameSession.startNextRound();
		}
		gameSession.startNextRound();

		assertThat(gameSession.getCurrentGame().getResult().getTeamScore(player), equalTo((10) * 9 + 5 + 100));
	}

	@Test
	public void startNextRound_afterAPlayedRound_roundNumberIsIncreased() {

		final GameSession gameSession = GameSessionBuilder.newSession().createGameSession();

		gameSession.startNewGame(Mode.topDown(), false);
		Round secondRound = gameSession.startNextRound();

		assertThat(secondRound.getRoundNumber(), is(1));
	}

	@Test
	public void makeMove_inANewGame_storesMoveOnRound() {

		final GameSession gameSession = GameSessionBuilder.newSession()
				.withStartedGame(Mode.topDown())
				.createGameSession();

		gameSession.makeMove(new Move(gameSession.getPlayersInInitialPlayingOrder().get(0), Card.CLUB_ACE));

		assertThat(gameSession.getCurrentRound().getMoves().size(), is(1));
	}

	@Test
	public void makeMove_inANewGame_advancesToNextPlayer() {
		final GameSession gameSession = GameSessionBuilder.newSession()
				.withStartedGame(Mode.topDown())
				.createGameSession();

		gameSession.makeMove(new Move(gameSession.getPlayersInInitialPlayingOrder().get(0), Card.CLUB_ACE));

		assertThat(gameSession.getCurrentRound().getPlayingOrder().getCurrentPlayer(), is(gameSession.getPlayersInInitialPlayingOrder().get(1)));
	}

}