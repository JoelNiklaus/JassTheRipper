package com.zuehlke.jasschallenge.client.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zuehlke.jasschallenge.messages.responses.Response;
import org.eclipse.jetty.websocket.api.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class WebSocketResponseChannel implements ResponseChannel {

    private static ObjectMapper objectMapper = new ObjectMapper();
    private final Session session;
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public WebSocketResponseChannel(Session session) {
        this.session = session;
    }

    @Override
    public void respond(Response response) {
        final String messageString = toJson(response);
        logger.trace("Sending message: {}", messageString);
        try {
            session.getRemote().sendString(messageString);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
