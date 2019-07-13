package to.joeli.jass.messages;

import com.fasterxml.jackson.annotation.JsonProperty;
import to.joeli.jass.client.websocket.GameHandler;
import to.joeli.jass.messages.responses.Response;

import java.util.Optional;

public class PlayerJoined implements Message {


    private final PlayerJoinedSession session;

    public PlayerJoined(@JsonProperty(value = "data",required = true) PlayerJoinedSession session) {
        this.session = session;
    }

    @Override
    public Optional<Response> dispatch(GameHandler handler) {
        handler.onPlayerJoined(session);
        return Optional.empty();
    }

}
