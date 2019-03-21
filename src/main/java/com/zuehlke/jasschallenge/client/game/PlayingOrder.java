package com.zuehlke.jasschallenge.client.game;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class PlayingOrder implements Serializable {

	private final List<Player> playersInInitialPlayingOrder;
	private final int startingPlayerInt;
	private int currentPlayerInt;

	public static PlayingOrder createOrder(List<Player> playersInInitialPlayingOrder) {
		return new PlayingOrder(playersInInitialPlayingOrder, 0);
	}

	public static PlayingOrder createOrderStartingFromPlayer(List<Player> playersInPlayingOrder, Player startFrom) {
		return new PlayingOrder(playersInPlayingOrder, playersInPlayingOrder.indexOf(startFrom));
	}

	private PlayingOrder(List<Player> playersInInitialPlayingOrder, int startingPlayerInt) {
		this.playersInInitialPlayingOrder = playersInInitialPlayingOrder;
		this.startingPlayerInt = startingPlayerInt;
		this.currentPlayerInt = 0;
	}

	// TODO maybe order is confused

	/**
	 * Copy constructor for deep copy
	 *
	 * @param playingOrder
	 */
	public PlayingOrder(PlayingOrder playingOrder) {
		this.playersInInitialPlayingOrder = new ArrayList<>();
		for (Player player : playingOrder.getPlayersInInitialPlayingOrder())
			this.playersInInitialPlayingOrder.add(new Player(player));
		this.startingPlayerInt = playingOrder.getStartingPlayerInt();
		this.currentPlayerInt = playingOrder.getCurrentPlayerInt();
	}

	public List<Player> getPlayersInInitialPlayingOrder() {
		return playersInInitialPlayingOrder;
	}

	public int getStartingPlayerInt() {
		return startingPlayerInt;
	}

	public Player getCurrentPlayer() {
		return playersInInitialPlayingOrder.get(getBoundIndex(currentPlayerInt));
	}

	public void moveToNextPlayer() {
		currentPlayerInt++;
	}

	private int getBoundIndex(int playerPosition) {
		return (this.startingPlayerInt + playerPosition) % playersInInitialPlayingOrder.size();
	}

	private int getCurrentPlayerInt() {
		return currentPlayerInt;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof PlayingOrder)) return false;

		PlayingOrder that = (PlayingOrder) o;

		if (startingPlayerInt != that.startingPlayerInt) return false;
		if (currentPlayerInt != that.currentPlayerInt) return false;
		return playersInInitialPlayingOrder != null ? playersInInitialPlayingOrder.equals(that.playersInInitialPlayingOrder) : that.playersInInitialPlayingOrder == null;
	}

	@Override
	public int hashCode() {
		int result = playersInInitialPlayingOrder != null ? playersInInitialPlayingOrder.hashCode() : 0;
		result = 31 * result + startingPlayerInt;
		result = 31 * result + currentPlayerInt;
		return result;
	}

	@Override
	public String toString() {
		return "PlayingOrder{" +
				"playersInInitialPlayingOrder=" + playersInInitialPlayingOrder +
				", startingPlayerInt=" + startingPlayerInt +
				", currentPlayerInt=" + currentPlayerInt +
				'}';
	}
}
