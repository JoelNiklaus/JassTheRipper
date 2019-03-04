package com.zuehlke.jasschallenge.client.websocket;

import com.zuehlke.jasschallenge.messages.*;
import com.zuehlke.jasschallenge.messages.responses.ChooseCard;
import com.zuehlke.jasschallenge.messages.responses.ChoosePlayerName;
import com.zuehlke.jasschallenge.messages.responses.ChooseSession;
import com.zuehlke.jasschallenge.messages.responses.ChooseTrumpf;
import com.zuehlke.jasschallenge.messages.type.*;
import com.zuehlke.jasschallenge.game.Trumpf;
import org.eclipse.jetty.websocket.api.Session;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static com.zuehlke.jasschallenge.messages.type.RemoteColor.*;
import static com.zuehlke.jasschallenge.messages.type.SessionChoice.AUTOJOIN;
import static com.zuehlke.jasschallenge.game.Trumpf.OBEABE;
import static java.util.Arrays.asList;
import static org.mockito.Mockito.*;

public class RemoteGameSocketTest {

    @Test
    public void onMessage_repliesWithChoosePlayerName_onRequestPlayerName() throws IOException {

        final Session session = mock(Session.class, RETURNS_DEEP_STUBS);
        final GameHandler handler = mock(GameHandler.class);
        when(handler.onRequestPlayerName()).thenReturn(new ChoosePlayerName("name"));
        RemoteGameSocket remoteGameSocket = new RemoteGameSocket(handler);
        remoteGameSocket.onConnect(new WebSocketResponseChannel(session));

        remoteGameSocket.onWebSocketMessage("{\"type\":\"REQUEST_PLAYER_NAME\"}");

        verify(session.getRemote()).sendString("{\"type\":\"CHOOSE_PLAYER_NAME\",\"data\":\"name\"}");
        verify(handler).onRequestPlayerName();
        verifyNoMoreInteractions(handler);
    }

    @Test
    public void onMessage_notifiesTheHandler_onDealCards() throws IOException {

        final Session session = mock(Session.class, RETURNS_DEEP_STUBS);

        final GameHandler handler = mock(GameHandler.class);
        RemoteGameSocket remoteGameSocket = new RemoteGameSocket(handler);
        remoteGameSocket.onConnect(new WebSocketResponseChannel(session));

        remoteGameSocket.onWebSocketMessage("{\"type\":\"DEAL_CARDS\",\"data\":[{\"number\":14,\"color\":\"DIAMONDS\"}," +
                "{\"number\":8,\"color\":\"SPADES\"}," +
                "{\"number\":6,\"color\":\"CLUBS\"}]}");

        verify(handler).onDealCards(eq(asList(new RemoteCard(14, DIAMONDS), new RemoteCard(8, SPADES), new RemoteCard(6, CLUBS))));
        verifyNoMoreInteractions(handler);
    }

    @Test
    public void onMessage_repliesWitchSessionType_onChooseSession() throws IOException {

        final Session session = mock(Session.class, RETURNS_DEEP_STUBS);
        final GameHandler handler = mock(GameHandler.class);
        when(handler.onRequestSessionChoice()).thenReturn(new ChooseSession(AUTOJOIN));
        RemoteGameSocket remoteGameSocket = new RemoteGameSocket(handler);
        remoteGameSocket.onConnect(new WebSocketResponseChannel(session));

        remoteGameSocket.onWebSocketMessage("{\"type\":\"REQUEST_SESSION_CHOICE\"}");

        verify(session.getRemote()).sendString("{\"type\":\"CHOOSE_SESSION\"," +
                "\"data\":{\"sessionChoice\":\"AUTOJOIN\",\"sessionName\":\"Java Client Session\"," +
                "\"sessionType\":\"TOURNAMENT\",\"asSpectator\":false,\"chosenTeamIndex\":1}}");
        verify(handler).onRequestSessionChoice();
        verifyNoMoreInteractions(handler);
    }

    @Test
    public void onMessage_sendsTrupf() throws IOException {

        final Session session = mock(Session.class, RETURNS_DEEP_STUBS);
        final GameHandler handler = mock(GameHandler.class);
        when(handler.onRequestTrumpf()).thenReturn(new ChooseTrumpf(OBEABE));
        RemoteGameSocket remoteGameSocket = new RemoteGameSocket(handler);
        remoteGameSocket.onConnect(new WebSocketResponseChannel(session));

        remoteGameSocket.onWebSocketMessage("{\"type\":\"REQUEST_TRUMPF\"}");

        verify(session.getRemote()).sendString("{\"type\":\"CHOOSE_TRUMPF\",\"data\":{\"mode\":\"OBEABE\",\"trumpfColor\":null}}");
        verify(handler).onRequestTrumpf();
        verifyNoMoreInteractions(handler);
    }

    @Test
    public void onMessage_callsOnPlayedCards_onPlayedCardsMessage() throws IOException {

        final Session session = mock(Session.class, RETURNS_DEEP_STUBS);
        final GameHandler handler = mock(GameHandler.class);
        RemoteGameSocket remoteGameSocket = new RemoteGameSocket(handler);
        remoteGameSocket.onConnect(new WebSocketResponseChannel(session));

        remoteGameSocket.onWebSocketMessage("{\"type\":\"PLAYED_CARDS\",\"data\":[{\"number\":13,\"color\":\"CLUBS\"}," +
                "{\"number\":10,\"color\":\"DIAMONDS\"}]}");

        verify(handler).onPlayedCards(eq(asList(new RemoteCard(13, CLUBS), new RemoteCard(10, DIAMONDS))));
        verifyNoMoreInteractions(handler);
    }

    @Test
    public void onMessage_sendsCardToPlay_onRequestCard() throws IOException {

        final Session session = mock(Session.class, RETURNS_DEEP_STUBS);
        final GameHandler handler = mock(GameHandler.class);
        when(handler.onRequestCard()).thenReturn(new ChooseCard(new RemoteCard(14, DIAMONDS)));
        RemoteGameSocket remoteGameSocket = new RemoteGameSocket(handler);
        remoteGameSocket.onConnect(new WebSocketResponseChannel(session));

        remoteGameSocket.onWebSocketMessage("{\"type\":\"REQUEST_CARD\"}");

        verify(session.getRemote()).sendString("{\"type\":\"CHOOSE_CARD\",\"data\":{\"number\":14,\"color\":\"DIAMONDS\"}}");
        verify(handler).onRequestCard();
        verifyNoMoreInteractions(handler);
    }

    @Test
    public void onMessage_canHandleBroadCastStich() throws IOException {

        final Session session = mock(Session.class, RETURNS_DEEP_STUBS);
        final GameHandler handler = mock(GameHandler.class);
        RemoteGameSocket remoteGameSocket = new RemoteGameSocket(handler);
        remoteGameSocket.onConnect(new WebSocketResponseChannel(session));

        remoteGameSocket.onWebSocketMessage("{\"type\":\"BROADCAST_STICH\",\"data\":{\"name\":\"1437909005411\",\"id\":\"uid-2\",\"seatId\": 0,\"playedCards\":[{\"number\":13,\"color\":\"DIAMONDS\"},{\"number\":13,\"color\":\"CLUBS\"},{\"number\":12,\"color\":\"CLUBS\"},{\"number\":12,\"color\":\"SPADES\"}],\"teams\":[{\"name\":\"Team 1\",\"points\":2148,\"currentRoundPoints\":348},{\"name\":\"Team 2\",\"points\":2547,\"currentRoundPoints\":108}]}}");

        final List<RemoteCard> playedCards = asList(new RemoteCard(13, DIAMONDS), new RemoteCard(13, CLUBS), new RemoteCard(12, CLUBS), new RemoteCard(12, SPADES));
        final List<RemoteTeam> remoteTeams = asList(new RemoteTeam("Team 1", 2148, 348), new RemoteTeam("Team 2", 2547, 108));
        final Stich stich = new Stich("1437909005411", "uid-2", 0, playedCards, remoteTeams);
        verify(handler).onBroadCastStich(stich);
        verifyNoMoreInteractions(handler);
    }

    @Test
    public void onMessage_canHandleSessionJoined() throws IOException {
        final Session session = mock(Session.class, RETURNS_DEEP_STUBS);
        final GameHandler handler = mock(GameHandler.class);
        when(handler.onRequestCard()).thenReturn(new ChooseCard(new RemoteCard(14, DIAMONDS)));
        RemoteGameSocket remoteGameSocket = new RemoteGameSocket(handler);
        remoteGameSocket.onConnect(new WebSocketResponseChannel(session));

        remoteGameSocket.onWebSocketMessage("{\"type\":\"BROADCAST_SESSION_JOINED\",\"data\":{\"sessionName\":\"32a340ae-5fcf-4086-84a8-9dc871a960cb\",\"player\":{\"id\":\"uid-0\",\"name\":\"1439103645725\", \"seatId\": 0},\"playersInSession\":[{\"id\":\"uid-0\",\"name\":\"1439103645725\", \"seatId\": 0}]}}");

        verify(handler).onPlayerJoined(eq(new PlayerJoinedSession("32a340ae-5fcf-4086-84a8-9dc871a960cb", new RemotePlayer("uid-0", "1439103645725", 0), Collections.singletonList(new RemotePlayer("uid-0", "1439103645725", 0)))));
        verifyNoMoreInteractions(handler);
    }

    @Test
    public void onMessage_canHandleBroadcastTeams() throws IOException {
        final Session session = mock(Session.class, RETURNS_DEEP_STUBS);
        final GameHandler handler = mock(GameHandler.class);
        RemoteGameSocket remoteGameSocket = new RemoteGameSocket(handler);
        remoteGameSocket.onConnect(new WebSocketResponseChannel(session));

        remoteGameSocket.onWebSocketMessage("{\"type\":\"BROADCAST_TEAMS\",\"data\":[{\"name\":\"Team 1\",\"players\":[{\"name\":\"1437917428074\",\"id\":\"0\", \"seatId\":0},{\"name\":\"1437917436253\",\"id\":\"2\", \"seatId\":2}]},{\"name\":\"Team 2\",\"players\":[{\"name\":\"1437917434340\",\"id\":\"1\", \"seatId\":1},{\"name\":\"1437917437853\",\"id\":\"3\", \"seatId\": 3}]}]}");

        verify(handler).onBroadCastTeams(eq(asList(
                new RemoteTeam("Team 1", asList(new RemotePlayer("0", "1437917428074", 0), new RemotePlayer("2", "1437917436253", 2))),
                new RemoteTeam("Team 2", asList(new RemotePlayer("1", "1437917434340", 1), new RemotePlayer("3", "1437917437853", 3))))));
        verifyNoMoreInteractions(handler);
    }

    @Test
    public void onMessage_canHandleBroadcastTrumpf() throws IOException {
        final Session session = mock(Session.class, RETURNS_DEEP_STUBS);
        final GameHandler handler = mock(GameHandler.class);
        RemoteGameSocket remoteGameSocket = new RemoteGameSocket(handler);
        remoteGameSocket.onConnect(new WebSocketResponseChannel(session));

        remoteGameSocket.onWebSocketMessage("{\"type\":\"BROADCAST_TRUMPF\",\"data\":{\"mode\":\"OBEABE\",\"trumpfColor\":null}}");

        verify(handler).onBroadCastTrumpf(eq(new TrumpfChoice(Trumpf.OBEABE, null)));
        verifyNoMoreInteractions(handler);
    }

    @Test
    public void onMessage_canHandleBroadcastGameFinished() throws IOException {
        final Session session = mock(Session.class, RETURNS_DEEP_STUBS);
        final GameHandler handler = mock(GameHandler.class);
        RemoteGameSocket remoteGameSocket = new RemoteGameSocket(handler);
        remoteGameSocket.onConnect(new WebSocketResponseChannel(session));

        remoteGameSocket.onWebSocketMessage("{\"type\":\"BROADCAST_GAME_FINISHED\",\"data\":[{\"name\":\"Team 2\",\"points\":1029,\"currentRoundPoints\":318},{\"name\":\"Team 1\",\"points\":2568,\"currentRoundPoints\":153}]}");

        verify(handler).onBroadGameFinished(eq(asList(new RemoteTeam("Team 2", 1029, 318), new RemoteTeam("Team 1", 2568, 153))));
        verifyNoMoreInteractions(handler);
    }

    @Test
    public void onMessage_canHandleBroadcastWinnerTeam() throws IOException {
        final Session session = mock(Session.class, RETURNS_DEEP_STUBS);
        final GameHandler handler = mock(GameHandler.class);
        RemoteGameSocket remoteGameSocket = new RemoteGameSocket(handler);
        remoteGameSocket.onConnect(new WebSocketResponseChannel(session));

        remoteGameSocket.onWebSocketMessage("{\"type\":\"BROADCAST_WINNER_TEAM\",\"data\":{\"name\":\"Team 1\",\"points\":2568,\"currentRoundPoints\":0}}");

        verify(handler).onBroadCastWinnerTeam(eq(new RemoteTeam("Team 1", 2568, 0)));
        verifyNoMoreInteractions(handler);
    }

    @Test
    public void onMessage_canHandleRejectCard() throws IOException {
        final Session session = mock(Session.class, RETURNS_DEEP_STUBS);
        final GameHandler handler = mock(GameHandler.class);
        RemoteGameSocket remoteGameSocket = new RemoteGameSocket(handler);
        remoteGameSocket.onConnect(new WebSocketResponseChannel(session));

        remoteGameSocket.onWebSocketMessage("{\"type\":\"REJECT_CARD\",\"data\":{\"number\":13,\"color\":\"DIAMONDS\"}}");

        verify(handler).onRejectCard(eq(new RemoteCard(13, DIAMONDS)));
        verifyNoMoreInteractions(handler);
    }

}