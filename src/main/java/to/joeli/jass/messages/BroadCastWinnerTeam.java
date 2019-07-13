package to.joeli.jass.messages;

import com.fasterxml.jackson.annotation.JsonProperty;
import to.joeli.jass.client.websocket.GameHandler;
import to.joeli.jass.messages.responses.Response;
import to.joeli.jass.messages.type.RemoteTeam;

import java.util.Optional;

public class BroadCastWinnerTeam implements Message {
    private final RemoteTeam data;

    public BroadCastWinnerTeam(@JsonProperty(value = "data",required = true)RemoteTeam remoteTeam) {
        this.data = remoteTeam;
    }

    @Override
    public Optional<Response> dispatch(GameHandler handler) {
        handler.onBroadCastWinnerTeam(data);
        return Optional.empty();
    }
}
