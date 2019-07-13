package to.joeli.jass.messages;

import com.fasterxml.jackson.annotation.JsonProperty;
import to.joeli.jass.client.websocket.GameHandler;
import to.joeli.jass.messages.responses.Response;
import to.joeli.jass.messages.type.RemoteCard;

import java.util.Optional;

public class RejectCard implements Message {
    private final RemoteCard data;

    public RejectCard(@JsonProperty(value = "data",required = true) RemoteCard remoteCard) {
        this.data = remoteCard;
    }

    @Override
    public Optional<Response> dispatch(GameHandler handler) {
        handler.onRejectCard(data);
        return Optional.empty();
    }

}
