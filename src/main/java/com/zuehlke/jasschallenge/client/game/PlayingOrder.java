package com.zuehlke.jasschallenge.client.game;

import java.io.Serializable;
import java.util.ArrayList;
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

	// TODO maybe order is confused
	/**
	 * Copy constructor for deep copy
	 *
	 * @param playingOrder
	 */
	public PlayingOrder(PlayingOrder playingOrder) {
		synchronized (playingOrder) {
			this.playersInInitialPlayingOrder = new ArrayList<>();
			for (Player player : playingOrder.getPlayersInInitialPlayingOrder())
				this.playersInInitialPlayingOrder.add(new Player(player));
			this.startingPlayer = playingOrder.getStartingPlayer();
			this.currentPlayer = playingOrder.getCurrentPlayerInt();
		}
	}

	public List<Player> getPlayersInInitialPlayingOrder() {
		return playersInInitialPlayingOrder;
	}

	public int getStartingPlayer() {
		return startingPlayer;
	}

	public List<Player> getPlayerInOrder() {
		return playersInInitialPlayingOrder;
	}

	public Player getCurrentPlayer() {
		return playersInInitialPlayingOrder.get(getBoundIndex(currentPlayer));
	}

	public void moveToNextPlayer() {
		currentPlayer = currentPlayer + 1;
	}

	private int getBoundIndex(int playerPosition) {
		return (this.startingPlayer + playerPosition) % playersInInitialPlayingOrder.size();
	}

	@Override
	public String toString() {
		return "PlayingOrder{" +
				"playersInInitialPlayingOrder=" + playersInInitialPlayingOrder +
				", startingPlayer=" + startingPlayer +
				", currentPlayer=" + currentPlayer +
				'}';
	}

	public int getCurrentPlayerInt() {
		return currentPlayer;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof PlayingOrder)) return false;

		PlayingOrder that = (PlayingOrder) o;

		if (startingPlayer != that.startingPlayer) return false;
		if (currentPlayer != that.currentPlayer) return false;
		return playersInInitialPlayingOrder != null ? playersInInitialPlayingOrder.equals(that.playersInInitialPlayingOrder) : that.playersInInitialPlayingOrder == null;
	}

	@Override
	public int hashCode() {
		int result = playersInInitialPlayingOrder != null ? playersInInitialPlayingOrder.hashCode() : 0;
		result = 31 * result + startingPlayer;
		result = 31 * result + currentPlayer;
		return result;
	}
}
