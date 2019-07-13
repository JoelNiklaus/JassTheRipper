package to.joeli.jass.client.websocket;

import to.joeli.jass.client.game.*;
import to.joeli.jass.game.Trumpf;
import to.joeli.jass.game.cards.Card;
import to.joeli.jass.game.mode.Mode;
import to.joeli.jass.messages.PlayerJoinedSession;
import to.joeli.jass.messages.responses.ChooseCard;
import to.joeli.jass.messages.responses.ChoosePlayerName;
import to.joeli.jass.messages.responses.ChooseSession;
import to.joeli.jass.messages.responses.ChooseTrumpf;
import to.joeli.jass.messages.type.*;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static com.shazam.shazamcrest.MatcherAssert.assertThat;
import static com.shazam.shazamcrest.matcher.Matchers.sameBeanAs;
import static to.joeli.jass.client.LambdaMatcher.match;
import static to.joeli.jass.messages.type.RemoteColor.*;
import static to.joeli.jass.messages.type.SessionChoice.AUTOJOIN;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class GameHandlerTest {

	@Test
	public void onRequestCard_returnsTheCardPlayedFromTheLocalPlayer() {

		Player localPlayer = mock(Player.class);
		when(localPlayer.makeMove(any(GameSession.class))).thenReturn(new Move(localPlayer, Card.DIAMOND_ACE));
		when(localPlayer.getName()).thenReturn("local");
		when(localPlayer.getId()).thenReturn("uid-1");

		RemotePlayer remoteLocalPlayer = new RemotePlayer("uid-1", "local", 0);
		RemotePlayer remoteOne = new RemotePlayer("uid-2", "remote 1", 2);
		RemotePlayer remoteTwo = new RemotePlayer("uid-3", "remote 2", 1);
		RemotePlayer remoteThree = new RemotePlayer("uid-4", "remote 3", 3);
		List<RemoteTeam> remoteTeams = asList(
				new RemoteTeam("team a", asList(remoteOne, remoteThree)),
				new RemoteTeam("team b", asList(remoteLocalPlayer, remoteTwo)));

		GameHandler gameHandler = new GameHandler(localPlayer, SessionType.TOURNAMENT);
		gameHandler.onBroadCastTeams(remoteTeams);
		gameHandler.onBroadCastTrumpf(new TrumpfChoice(Trumpf.OBEABE, null));

		ChooseCard chooseCard = gameHandler.onRequestCard();

		assertThat(chooseCard.getData(), equalTo(new RemoteCard(14, DIAMONDS)));
	}

	@Test
	public void onRequestTrumpf_asksThePlayerForTrumpf() {

		final Player localPlayer = mock(Player.class);
		when(localPlayer.chooseTrumpf(any(GameSession.class), eq(false))).thenReturn(Mode.topDown());
		when(localPlayer.getName()).thenReturn("local");
		final RemotePlayer remoteLocalPlayer = new RemotePlayer("uid-2", "local", 1);
		final List<RemoteTeam> remoteTeams = asList(
				new RemoteTeam("team a", singletonList(remoteLocalPlayer)),
				new RemoteTeam("team b", singletonList(remoteLocalPlayer)));

		final GameHandler gameHandler = new GameHandler(localPlayer, SessionType.TOURNAMENT);
		gameHandler.onBroadCastTeams(remoteTeams);
		gameHandler.onBroadCastTrumpf(new TrumpfChoice(Trumpf.OBEABE, null));

		final ChooseTrumpf chooseTrumpf = gameHandler.onRequestTrumpf();

		assertThat(chooseTrumpf, equalTo(new ChooseTrumpf(Trumpf.OBEABE)));
	}

	@Test
	public void onRequestSession_returnsSessionChoice() {

		final ChooseSession chooseSession = new GameHandler(new Player("local"), SessionType.TOURNAMENT).onRequestSessionChoice();

		assertThat(chooseSession, sameBeanAs(new ChooseSession(AUTOJOIN, "Java Client Session", SessionType.TOURNAMENT)));
	}

	@Test
	public void onDealCards_setsTheCardsOnLocalPlayer() {

		final Player localPlayer = new Player("test");
		final List<RemoteCard> dealCard = asList(
				new RemoteCard(14, DIAMONDS),
				new RemoteCard(8, RemoteColor.SPADES),
				new RemoteCard(6, CLUBS));

		new GameHandler(localPlayer, SessionType.TOURNAMENT).onDealCards(dealCard);

		assertThat(localPlayer.getCards(), containsInAnyOrder(Card.DIAMOND_ACE, Card.SPADE_EIGHT, Card.CLUB_SIX));
	}

	@Test
	public void onRequestPlayerName_repliesWithNameOfLocalPlayer() {

		final Player localPlayer = new Player("test");

		final ChoosePlayerName choosePlayerName = new GameHandler(localPlayer, SessionType.TOURNAMENT).onRequestPlayerName();

		assertThat(choosePlayerName, sameBeanAs(new ChoosePlayerName("test")));
	}

	@Test
	public void onBroadCastTeam_storesTeams() {

		final Player localPlayer = new Player("uid-2", "local", 0);
		final RemotePlayer remoteLocalPlayer = new RemotePlayer("uid-2", "local", 2);
		final RemotePlayer remoteOne = new RemotePlayer("uid-0", "remote 1", 0);
		final RemotePlayer remoteTwo = new RemotePlayer("uid-1", "remote 2", 1);
		final RemotePlayer remoteThree = new RemotePlayer("uid-3", "remote 3", 3);
		final List<RemoteTeam> remoteTeams = asList(
				new RemoteTeam("team a", asList(remoteOne, remoteThree)),
				new RemoteTeam("team b", asList(remoteLocalPlayer, remoteTwo)));

		final GameHandler gameHandler = new GameHandler(localPlayer, SessionType.TOURNAMENT);
		gameHandler.onPlayerJoined(new PlayerJoinedSession("session", remoteLocalPlayer, emptyList()));
		gameHandler.onBroadCastTeams(remoteTeams);

		assertThat(gameHandler.getTeams(), contains(
				match(team -> "team a".equals(team.getTeamName()), "team a"),
				match(team -> "team b".equals(team.getTeamName()), "team b")));
		assertThat(gameHandler.getTeams(), contains(
				match(team -> team.getPlayers().size() == 2, "two members in team a"),
				match(team -> team.getPlayers().size() == 2, "two members in team b")));
		assertThat(gameHandler.getTeams().get(1).getPlayers().toArray(), hasItemInArray(equalTo(localPlayer)));
	}

	@Test
	public void onBroadcastStich_startsNewRound() {
		final Player localPlayer = new Player("uid-2", "local", 2);
		final RemotePlayer remoteLocalPlayer = new RemotePlayer("uid-2", "local", 2);
		final RemotePlayer remoteOne = new RemotePlayer("uid-0", "remote 1", 0);
		final RemotePlayer remoteTwo = new RemotePlayer("uid-1", "remote 2", 1);
		final RemotePlayer remoteThree = new RemotePlayer("uid-3", "remote 3", 3);
		final RemoteTeam remoteTeam1 = new RemoteTeam("team a", asList(remoteOne, remoteThree));
		remoteTeam1.setCurrentRoundPoints(15);
		final RemoteTeam remoteTeam2 = new RemoteTeam("team b", asList(remoteLocalPlayer, remoteTwo));
		final List<RemoteTeam> remoteTeams = asList(
				remoteTeam1,
				remoteTeam2);
		final GameHandler gameHandler = new GameHandler(localPlayer, SessionType.TOURNAMENT);

		gameHandler.onBroadCastTeams(remoteTeams);
		gameHandler.onBroadCastTrumpf(new TrumpfChoice(Trumpf.OBEABE, null));
		gameHandler.onPlayedCards(Collections.singletonList(new RemoteCard(13, CLUBS)));
		gameHandler.onPlayedCards(asList(new RemoteCard(13, CLUBS), new RemoteCard(14, CLUBS)));
		gameHandler.onBroadCastStich(new Stich("remote 2", "uid-1", 1, emptyList(), asList(remoteTeam1, remoteTeam2)));

		assertThat(gameHandler.getCurrentRound().getMoves(), empty());
		assertThat(gameHandler.getCurrentRound().getRoundNumber(), equalTo(1));
	}

	@Test
	public void onPlayedCards_afterStartingAGame_callsMakeMoveOnSession() {

		final Player localPlayer = mock(Player.class);
		when(localPlayer.getName()).thenReturn("Player0");

		Player player0 = new Player("Player0");
		Player player1 = new Player("Player1");
		Player player2 = new Player("Player2");
		Player player3 = new Player("Player3");
		List<Player> playingOrder = asList(player0, player1, player2, player3);
		Team team0 = new Team("Team0", asList(player0, player2));
		Team team1 = new Team("Team1", asList(player1, player3));
		List<Team> teams = asList(team0, team1);

		final GameSession session = spy(new GameSession(teams, playingOrder));
		session.startNewGame(Mode.bottomUp(), false);
		final GameHandler handler = new GameHandler(localPlayer, session);

		handler.onPlayedCards(Collections.singletonList(new RemoteCard(13, CLUBS)));

		verify(session).makeMove(argThat(sameBeanAs(new Move(new Player("Player0"), Card.CLUB_KING))));
	}

	@Test
	public void onPlayedCards_roundIsUpdated() {

		final Player localPlayer = new Player("uid-2", "local", 2) {
			@Override
			public Move makeMove(GameSession session) {
				return new Move(this, Card.DIAMOND_ACE);
			}
		};

		final RemotePlayer remoteLocalPlayer = new RemotePlayer("uid-2", "local", 2);
		final RemotePlayer remoteOne = new RemotePlayer("uid-0", "remote 1", 0);
		final RemotePlayer remoteTwo = new RemotePlayer("uid-1", "remote 2", 1);
		final RemotePlayer remoteThree = new RemotePlayer("uid-3", "remote 3", 3);
		final List<RemoteTeam> remoteTeams = asList(
				new RemoteTeam("team a", asList(remoteOne, remoteThree)),
				new RemoteTeam("team b", asList(remoteLocalPlayer, remoteTwo)));

		final GameHandler handler = new GameHandler(localPlayer, SessionType.TOURNAMENT);
		handler.onPlayerJoined(new PlayerJoinedSession("session", remoteLocalPlayer, emptyList()));
		handler.onBroadCastTeams(remoteTeams);
		handler.onBroadCastTrumpf(new TrumpfChoice(Trumpf.OBEABE, null));
		handler.onPlayedCards(Collections.singletonList(new RemoteCard(13, CLUBS)));
		handler.onPlayedCards(asList(new RemoteCard(13, CLUBS), new RemoteCard(10, DIAMONDS)));

		final Player playerOne = new Player("uid-0", "remote 1", 0);
		final Player playerTwo = new Player("uid-1", "remote 2", 1);
		final Player playerThree = new Player("uid-3", "remote 3", 3);
		Round expected = Round.createRound(Mode.topDown(), 0, PlayingOrder.createOrder(asList(playerOne, playerTwo, localPlayer, playerThree)));
		expected.makeMove(new Move(playerOne, Card.CLUB_KING));
		expected.makeMove(new Move(playerTwo, Card.DIAMOND_TEN));
		assertThat(handler.getCurrentRound(), sameBeanAs(expected));
	}

	@Test
	public void onPlayedCards_mapsPlayedCardToPlayer_startsWithLowestId() {
		final Player localPlayer = new Player("uid-2", "local", 2);
		final RemotePlayer remoteLocalPlayer = new RemotePlayer("uid-2", "local", 2);
		final RemotePlayer remoteOne = new RemotePlayer("uid-0", "remote 1", 0);
		final RemotePlayer remoteTwo = new RemotePlayer("uid-1", "remote 2", 1);
		final RemotePlayer remoteThree = new RemotePlayer("uid-3", "remote 3", 3);
		final List<RemoteTeam> remoteTeams = asList(
				new RemoteTeam("team a", asList(remoteOne, remoteThree)),
				new RemoteTeam("team b", asList(remoteLocalPlayer, remoteTwo)));
		final GameHandler gameHandler = new GameHandler(localPlayer, SessionType.TOURNAMENT);
		gameHandler.onBroadCastTeams(remoteTeams);
		gameHandler.onBroadCastTrumpf(new TrumpfChoice(Trumpf.OBEABE, null));

		gameHandler.onPlayedCards(Collections.singletonList(new RemoteCard(14, DIAMONDS)));

		assertThat(gameHandler.getCurrentRound().getMoves().get(0).getPlayer().getName(), equalTo(remoteOne.getName()));
	}

	@Test
	public void onPlayedCards_mapsPlayerCorrectly_startsWithTheLastWhoMadeAStich() {
		final Player localPlayer = new Player("uid-2", "local", 2);
		final RemotePlayer remoteLocalPlayer = new RemotePlayer("uid-2", "local", 2);
		final RemotePlayer remoteOne = new RemotePlayer("uid-0", "remote 1", 0);
		final RemotePlayer remoteTwo = new RemotePlayer("uid-1", "remote 2", 1);
		final RemotePlayer remoteThree = new RemotePlayer("uid-3", "remote 3", 3);
		final RemoteTeam remoteTeamA = new RemoteTeam("team a", asList(remoteOne, remoteThree));
		remoteTeamA.setCurrentRoundPoints(17);
		final RemoteTeam remoteTeamB = new RemoteTeam("team b", asList(remoteLocalPlayer, remoteTwo));
		final List<RemoteTeam> remoteTeams = asList(
				remoteTeamA,
				remoteTeamB);
		final GameHandler gameHandler = new GameHandler(localPlayer, SessionType.TOURNAMENT);

		gameHandler.onBroadCastTeams(remoteTeams);
		gameHandler.onBroadCastTrumpf(new TrumpfChoice(Trumpf.OBEABE, null));
		gameHandler.onPlayedCards(Collections.singletonList(new RemoteCard(6, DIAMONDS)));
		gameHandler.onPlayedCards(asList(new RemoteCard(6, DIAMONDS), new RemoteCard(13, CLUBS)));
		gameHandler.onPlayedCards(asList(new RemoteCard(6, DIAMONDS), new RemoteCard(13, CLUBS), new RemoteCard(14, DIAMONDS)));
		gameHandler.onPlayedCards(asList(new RemoteCard(6, DIAMONDS), new RemoteCard(13, CLUBS), new RemoteCard(14, DIAMONDS), new RemoteCard(11, DIAMONDS)));
		gameHandler.onBroadCastStich(new Stich("local", "uid-2", 2, emptyList(), asList(remoteTeamA, remoteTeamB)));
		gameHandler.onPlayedCards(Collections.singletonList(new RemoteCard(14, DIAMONDS)));
		gameHandler.onPlayedCards(asList(new RemoteCard(14, DIAMONDS), new RemoteCard(13, CLUBS)));
		gameHandler.onPlayedCards(asList(new RemoteCard(14, DIAMONDS), new RemoteCard(13, CLUBS), new RemoteCard(6, HEARTS)));
		gameHandler.onPlayedCards(asList(new RemoteCard(14, DIAMONDS), new RemoteCard(13, CLUBS), new RemoteCard(6, HEARTS), new RemoteCard(11, DIAMONDS)));

		assertThat(gameHandler.getCurrentRound().getMoves(), hasSize(4));
		assertThat(gameHandler.getCurrentRound().getMoves().get(0).getPlayer().getName(), equalTo("local"));
		assertThat(gameHandler.getCurrentRound().getMoves().get(1).getPlayer().getName(), equalTo("remote 3"));
		assertThat(gameHandler.getCurrentRound().getMoves().get(2).getPlayer().getName(), equalTo("remote 1"));
		assertThat(gameHandler.getCurrentRound().getMoves().get(3).getPlayer().getName(), equalTo("remote 2"));
	}


	@Test
	public void broadcastGschobe_does_not_start_game() {
		Player localPlayer = mock(Player.class);
		GameSession gameSession = mock(GameSession.class);
		final GameHandler gameHandler = new GameHandler(localPlayer, gameSession);

		gameHandler.onBroadCastTrumpf(new TrumpfChoice(Trumpf.SCHIEBE, null));

		verify(localPlayer, times(1)).setId(null);
		verifyNoMoreInteractions(localPlayer);
		verifyZeroInteractions(gameSession);
	}


	@Test
	public void broadcastTrumpf_does_start_game_with_shifted() {
		Player localPlayer = mock(Player.class);
		GameSession gameSession = mock(GameSession.class);
		final GameHandler gameHandler = new GameHandler(localPlayer, gameSession);

		gameHandler.onBroadCastTrumpf(new TrumpfChoice(Trumpf.SCHIEBE, null));
		gameHandler.onBroadCastTrumpf(new TrumpfChoice(Trumpf.OBEABE, null));

		verify(localPlayer, times(1)).onGameStarted(gameSession);
		verify(gameSession, times(1)).startNewGame(anyObject(), eq(true));
	}

}