package to.joeli.jass.client.websocket;

interface IConnection<M> {

    void send(M message);

}
