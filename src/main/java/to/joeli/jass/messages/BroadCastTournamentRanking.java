package to.joeli.jass.messages;

import to.joeli.jass.client.websocket.GameHandler;
import to.joeli.jass.messages.responses.Response;

import java.util.Optional;

public class BroadCastTournamentRanking implements Message {
    @Override
    public Optional<Response> dispatch(GameHandler handler) {
        return Optional.empty();
    }
}
