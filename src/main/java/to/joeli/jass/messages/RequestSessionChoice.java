package to.joeli.jass.messages;

import to.joeli.jass.client.websocket.GameHandler;
import to.joeli.jass.messages.responses.Response;

import java.util.Optional;

public class RequestSessionChoice implements Message {
    @Override
    public Optional<Response> dispatch(GameHandler handler) {
        return Optional.of(handler.onRequestSessionChoice());
    }
}
