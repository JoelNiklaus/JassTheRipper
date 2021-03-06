package to.joeli.jass.messages;

import com.fasterxml.jackson.annotation.JsonProperty;
import to.joeli.jass.client.websocket.GameHandler;
import to.joeli.jass.messages.responses.Response;
import to.joeli.jass.messages.type.RemoteTeam;

import java.util.List;
import java.util.Optional;

public class BroadCastTeams implements Message {
    private final List<RemoteTeam> remoteTeams;

    public BroadCastTeams(@JsonProperty(value = "data", required = true) List<RemoteTeam> remoteTeams) {
        this.remoteTeams = remoteTeams;
    }

    @Override
    public Optional<Response> dispatch(GameHandler handler) {
        handler.onBroadCastTeams(remoteTeams);
        return Optional.empty();
    }
}
