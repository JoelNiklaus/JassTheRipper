package to.joeli.jass.messages;

import com.fasterxml.jackson.annotation.JsonProperty;
import to.joeli.jass.client.websocket.GameHandler;
import to.joeli.jass.messages.responses.Response;
import to.joeli.jass.messages.type.RemoteCard;

import java.util.List;
import java.util.Optional;

public class DealCard implements Message {
    private final List<RemoteCard> cards;

    public DealCard(@JsonProperty(value = "data",required = true) List<RemoteCard> cards) {
        this.cards = cards;
    }

    @Override
    public Optional<Response> dispatch(GameHandler handler) {
        handler.onDealCards(cards);
        return Optional.empty();
    }

    @Override
    public String toString() {
        return "DealCard{" +
                "cards=" + cards +
                '}';
    }
}
