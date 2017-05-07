package com.zuehlke.jasschallenge.client.game;

import java.io.Serializable;
import java.util.List;

public class PlayingOrder implements Serializable {

    private final List<Player> playersInInitialPlayingOrder;
    private final int startingPlayer;
    private int currentPlayer;

    public static PlayingOrder createOrder(List<Player> playersInInitialPlayingOrder) {
        return new PlayingOrder(playersInInitialPlayingOrder, 0);
    }

    public static PlayingOrder createOrderStartingFromPlayer(List<Player> playersInPlayingOrder, Player startFrom) {
        return new PlayingOrder(playersInPlayingOrder, playersInPlayingOrder.indexOf(startFrom));
    }

    private PlayingOrder(List<Player> playersInInitialPlayingOrder, int startingPlayer) {
        this.playersInInitialPlayingOrder = playersInInitialPlayingOrder;
        this.startingPlayer = startingPlayer;
        this.currentPlayer = 0;
    }

    public List<Player> getPlayerInOrder() { return playersInInitialPlayingOrder; }

    public Player getCurrentPlayer() {
        return playersInInitialPlayingOrder.get(getBoundIndex(currentPlayer));
    }

    public void moveToNextPlayer() {
        currentPlayer = currentPlayer + 1;
    }

    private int getBoundIndex(int playerPosition) {
        return (this.startingPlayer + playerPosition) % playersInInitialPlayingOrder.size();
    }
}
