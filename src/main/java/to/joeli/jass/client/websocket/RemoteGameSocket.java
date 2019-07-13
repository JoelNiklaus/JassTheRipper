package to.joeli.jass.client.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import to.joeli.jass.messages.Message;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@WebSocket
public class RemoteGameSocket extends GameSocket {

    private static ObjectMapper objectMapper = new ObjectMapper();
    private final CountDownLatch closeLatch = new CountDownLatch(1);

    public RemoteGameSocket(GameHandler handler) {
        super(handler);
    }

    public boolean awaitClose(int duration, TimeUnit unit) throws InterruptedException {
        return this.closeLatch.await(duration, unit);
    }

    @OnWebSocketConnect
    public void onWebSocketConnect(Session session) {
        logger.trace("Got connect: {}", session);
        super.onConnect(new WebSocketResponseChannel(session));
    }

    @OnWebSocketClose
    public void onWebSocketClose(int statusCode, String reason) {
        super.onClose(statusCode, reason);
        closeLatch.countDown();
    }

    @OnWebSocketMessage
    public void onWebSocketMessage(String msg) throws IOException {
        logger.trace("Received message: {}", msg);
        final Message message = read(msg, Message.class);
        super.onMessage(message);
    }

    private static <T> T read(String msg, Class<T> valueType) throws IOException {
        return objectMapper.readValue(msg, valueType);
    }

}
