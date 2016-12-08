package com.zuehlke.jasschallenge.localserver;


import com.zuehlke.jasschallenge.messages.Message;
import com.zuehlke.jasschallenge.messages.responses.Response;

class Player {

    private final ConnectionHandle connectionHandle;
    private String playerName;
    private String id;
    private int seatId;

    public Player(ConnectionHandle connectionHandle) {
        this.connectionHandle = connectionHandle;
    }

    public void notify(Message message) {
        connectionHandle.send(message);
    }

    public String getName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public <T extends Response> T ask(Message message, Class<T> responseClass) {
        return connectionHandle.ask(message, responseClass);
    }

    public ConnectionHandle getConnectionHandle() {
        return connectionHandle;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getSeatId() {
        return seatId;
    }

    public void setSeatId(int seatId) {
        this.seatId = seatId;
    }

    @Override
    public String toString() {
        return "Player{" +
                "playerName='" + playerName + '\'' +
                ", id=" + id +
                '}';
    }
}
