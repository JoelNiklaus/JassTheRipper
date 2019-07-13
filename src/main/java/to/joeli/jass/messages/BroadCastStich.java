package to.joeli.jass.messages;

import com.fasterxml.jackson.annotation.JsonProperty;
import to.joeli.jass.client.websocket.GameHandler;
import to.joeli.jass.messages.responses.Response;
import to.joeli.jass.messages.type.Stich;

import java.util.Optional;

public class BroadCastStich implements Message {

    private final Stich stich;

    public BroadCastStich(
            @JsonProperty(value = "data",required = true) Stich stich) {
        this.stich = stich;
    }

    @Override
    public Optional<Response> dispatch(GameHandler handler) {
        handler.onBroadCastStich(stich);
        return Optional.empty();
    }
}
