package to.joeli.jass.client.websocket;

import to.joeli.jass.messages.Message;
import to.joeli.jass.messages.responses.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class GameSocket {

    private final GameHandler handler;
    protected ResponseChannel responseChannel;

    final Logger logger = LoggerFactory.getLogger(getClass());

    public GameSocket(GameHandler handler) {
        this.handler = handler;
    }

    public void onMessage(Message msg) {
        Optional<Response> response = dispatchMessage(msg);
        response.ifPresent(responseChannel::respond);
    }

    public Optional<Response> dispatchMessage(Message msg) {
        return msg.dispatch(handler);
    }

    void onClose(int statusCode, String reason) {
        logger.trace("Connection closed: {} - {}", statusCode, reason);
    }

    public void onConnect(ResponseChannel responseChannel) {
        this.responseChannel = responseChannel;
    }
}
