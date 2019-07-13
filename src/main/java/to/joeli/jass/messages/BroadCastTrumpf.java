package to.joeli.jass.messages;

import com.fasterxml.jackson.annotation.JsonProperty;
import to.joeli.jass.client.websocket.GameHandler;
import to.joeli.jass.messages.responses.Response;
import to.joeli.jass.messages.type.TrumpfChoice;

import java.util.Optional;

public class BroadCastTrumpf implements Message {
    private final TrumpfChoice data;

    public BroadCastTrumpf(@JsonProperty(value = "data", required = true) TrumpfChoice trumpfChoice) {
        this.data = trumpfChoice;
    }

    @Override
    public Optional<Response> dispatch(GameHandler handler) {
        handler.onBroadCastTrumpf(data);
        return Optional.empty();
    }
}
