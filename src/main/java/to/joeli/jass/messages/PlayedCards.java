package to.joeli.jass.messages;

import com.fasterxml.jackson.annotation.JsonProperty;
import to.joeli.jass.client.websocket.GameHandler;
import to.joeli.jass.messages.responses.Response;
import to.joeli.jass.messages.type.RemoteCard;

import java.util.List;
import java.util.Optional;

public class PlayedCards implements Message {

    private final List<RemoteCard> cards;

    public PlayedCards(@JsonProperty(value = "data",required = true) List<RemoteCard> data) {
        this.cards = data;
    }

    @Override
    public Optional<Response> dispatch(GameHandler handler) {
        handler.onPlayedCards(cards);
        return Optional.empty();
    }
}
