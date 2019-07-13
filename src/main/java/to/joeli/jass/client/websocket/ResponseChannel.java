package to.joeli.jass.client.websocket;

import to.joeli.jass.messages.responses.Response;

public interface ResponseChannel {

    void respond(Response response);

}
