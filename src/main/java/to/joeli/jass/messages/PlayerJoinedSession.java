package to.joeli.jass.messages;

import com.fasterxml.jackson.annotation.JsonProperty;
import to.joeli.jass.messages.type.RemotePlayer;

import java.util.List;

public class PlayerJoinedSession {

    private final String sessionName;
    private final RemotePlayer player;
    private final List<RemotePlayer> playersInSession;

    public PlayerJoinedSession(@JsonProperty(value = "sessionName", required = true) String sessionName,
                               @JsonProperty(value = "player", required = true) RemotePlayer player,
                               @JsonProperty(value = "playersInSession", required = true) List<RemotePlayer> playersInSession) {

        this.sessionName = sessionName;
        this.player = player;
        this.playersInSession = playersInSession;
    }

    public String getSessionName() {
        return sessionName;
    }

    public RemotePlayer getPlayer() {
        return player;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PlayerJoinedSession session = (PlayerJoinedSession) o;

        if (sessionName != null ? !sessionName.equals(session.sessionName) : session.sessionName != null) return false;
        if (player != null ? !player.equals(session.player) : session.player != null) return false;
        return !(playersInSession != null ? !playersInSession.equals(session.playersInSession) : session.playersInSession != null);

    }

    @Override
    public int hashCode() {
        int result = sessionName != null ? sessionName.hashCode() : 0;
        result = 31 * result + (player != null ? player.hashCode() : 0);
        result = 31 * result + (playersInSession != null ? playersInSession.hashCode() : 0);
        return result;
    }
}
