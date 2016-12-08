package com.zuehlke.jasschallenge.localserver;

import com.zuehlke.jasschallenge.client.websocket.GameSocket;
import com.zuehlke.jasschallenge.messages.RequestPlayerName;
import com.zuehlke.jasschallenge.messages.RequestSessionChoice;
import com.zuehlke.jasschallenge.messages.responses.ChoosePlayerName;
import com.zuehlke.jasschallenge.messages.responses.ChooseSession;
import com.zuehlke.jasschallenge.messages.responses.ChooseTrumpf;
import com.zuehlke.jasschallenge.messages.responses.Response;
import com.zuehlke.jasschallenge.messages.type.ChooseSessionData;
import com.zuehlke.jasschallenge.messages.type.SessionType;

import java.util.HashMap;
import java.util.Map;

public class LocalServer {

    private final Map<String, JassSession> sessions = new HashMap<>();

    public void connect(GameSocket gameSocket) {
        ConnectionHandle connectionHandle = new ConnectionHandle(gameSocket);
        Player player = new Player(connectionHandle);
        gameSocket.onConnect(new PlayerToLocalServerResponseChannel(player, this));
        player.notify(new RequestPlayerName());
    }

    public void onResponse(Response response, Player player) {
        if (response instanceof ChoosePlayerName) {
            onChoosePlayerName((ChoosePlayerName) response, player);
        } else if (response instanceof ChooseSession) {
            onSessionChosen((ChooseSession) response, player);
        } else if (response instanceof ChooseTrumpf) {
            ChooseTrumpf chooseTrumpf = (ChooseTrumpf) response;
            getSessionForPlayer(player).onTrumpfChosen(chooseTrumpf, player);
        }
    }

    private JassSession getSessionForPlayer(Player player) {
        return this.sessions.values().stream().filter(session -> session.hasJoinedPlayer(player)).findFirst().get();
    }

    private void onSessionChosen(ChooseSession response, Player player) {
        ChooseSessionData data = response.getData();
        JassSession session = getOrCreateSession(data.getSessionName(), data.getSessionType());
        session.join(player);
    }

    private JassSession getOrCreateSession(String sessionName, SessionType sessionType) {
        JassSession jassSession = sessions.computeIfAbsent(sessionName, k -> new JassSession(sessionType, sessionName));
        return jassSession;
    }

    private void onChoosePlayerName(ChoosePlayerName response, Player player) {
        player.setPlayerName(response.getData());
        player.notify(new RequestSessionChoice());
    }
}
