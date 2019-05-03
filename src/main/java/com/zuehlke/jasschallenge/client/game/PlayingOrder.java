package com.zuehlke.jasschallenge.client.game;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class PlayingOrder implements Serializable {

	private final List<Player> playersInInitialPlayingOrder;
	private final int startingPlayerIndex;
	private int currentPlayerIndex;

	public static PlayingOrder createOrder(List<Player> playersInInitialPlayingOrder) {
		return new PlayingOrder(playersInInitialPlayingOrder, 0);
	}

	public static PlayingOrder createOrderStartingFromPlayer(List<Player> playersInPlayingOrder, Player startFrom) {
		return new PlayingOrder(playersInPlayingOrder, playersInPlayingOrder.indexOf(startFrom));
	}

	private PlayingOrder(List<Player> playersInInitialPlayingOrder, int startingPlayerIndex) {
		this.playersInInitialPlayingOrder = playersInInitialPlayingOrder;
		this.startingPlayerIndex = startingPlayerIndex;
		this.currentPlayerIndex = 0;
	}

	/**
	 * Copy constructor for deep copy
	 *
	 * @param playingOrder
	 */
	public PlayingOrder(PlayingOrder playingOrder) {
		this.playersInInitialPlayingOrder = new ArrayList<>();
		for (Player player : playingOrder.getPlayersInInitialPlayingOrder())
			this.playersInInitialPlayingOrder.add(new Player(player));
		this.startingPlayerIndex = playingOrder.getStartingPlayerIndex();
		this.currentPlayerIndex = playingOrder.getCurrentPlayerIndex();
	}

	public List<Player> getPlayersInInitialPlayingOrder() {
		return playersInInitialPlayingOrder;
	}

	public Player getCurrentPlayer() {
		return getPlayerByIndex(currentPlayerIndex);
	}


	public Player getPartnerOfPlayer(Player player) {
		return getPlayerByIndex(playersInInitialPlayingOrder.indexOf(player) + 2);
	}

	public void moveToNextPlayer() {
		currentPlayerIndex++;
	}

	private int getStartingPlayerIndex() {
		return startingPlayerIndex;
	}

	private Player getPlayerByIndex(int index) {
		return playersInInitialPlayingOrder.get(getBoundIndex(index));
	}

	private int getBoundIndex(int playerPosition) {
		return (this.startingPlayerIndex + playerPosition) % playersInInitialPlayingOrder.size();
	}

	private int getCurrentPlayerIndex() {
		return currentPlayerIndex;
	}


	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof PlayingOrder)) return false;

		PlayingOrder that = (PlayingOrder) o;

		if (startingPlayerIndex != that.startingPlayerIndex) return false;
		if (currentPlayerIndex != that.currentPlayerIndex) return false;
		return playersInInitialPlayingOrder != null ? playersInInitialPlayingOrder.equals(that.playersInInitialPlayingOrder) : that.playersInInitialPlayingOrder == null;
	}

	@Override
	public int hashCode() {
		int result = playersInInitialPlayingOrder != null ? playersInInitialPlayingOrder.hashCode() : 0;
		result = 31 * result + startingPlayerIndex;
		result = 31 * result + currentPlayerIndex;
		return result;
	}

	@Override
	public String toString() {
		return "PlayingOrder{" +
				"playersInInitialPlayingOrder=" + playersInInitialPlayingOrder +
				", startingPlayerIndex=" + startingPlayerIndex +
				", currentPlayerIndex=" + currentPlayerIndex +
				'}';
	}
}
