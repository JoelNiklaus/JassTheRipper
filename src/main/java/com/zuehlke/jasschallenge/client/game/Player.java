package com.zuehlke.jasschallenge.client.game;

import com.zuehlke.jasschallenge.client.game.strategy.*;
import com.zuehlke.jasschallenge.game.cards.Card;
import com.zuehlke.jasschallenge.game.mode.Mode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.EnumSet;
import java.util.Set;

public class Player implements Serializable {

	private static final Logger logger = LoggerFactory.getLogger(Player.class);

	private String id;
	private final String name;
	private int seatId;
	private final Set<Card> cards;
	private final JassStrategy currentJassStrategy;

	private boolean mctsEnabled = true; // disable this for pitting only the networks against each other
	private boolean valueEstimaterUsed; // This is used in Self Play Training
	private boolean networkTrainable; // This is used in Self Play Training
	private StrengthLevel cardStrengthLevel; // This is used in benchmarks
	private StrengthLevel trumpfStrengthLevel; // This is used in benchmarks
	private TrumpfSelectionMethod trumpfSelectionMethod; // This is used in benchmarks

	public Player(String id, String name, int seatId) {
		this(name);
		this.id = id;
		this.seatId = seatId;
	}

	public Player(String name) {
		this(name, new RandomJassStrategy());
	}

	public Player(String name, JassStrategy strategy) {
		this.name = name;
		this.cards = EnumSet.noneOf(Card.class);
		this.currentJassStrategy = strategy;
	}

	public Player(String id, String name, int seatId, Set<Card> cards, JassStrategy currentJassStrategy) {
		this.id = id;
		this.name = name;
		this.seatId = seatId;
		this.cards = cards;
		this.currentJassStrategy = currentJassStrategy;
	}

	/**
	 * Copy constructor for deep copy
	 *
	 * @param player
	 */
	public Player(Player player) {
		this.id = player.getId();
		this.name = player.getName();
		this.seatId = player.getSeatId();
		this.cards = EnumSet.copyOf(player.getCards());
		this.currentJassStrategy = player.getCurrentJassStrategy();

		this.mctsEnabled = player.isMctsEnabled();
		this.valueEstimaterUsed = player.isValueEstimaterUsed();
		this.networkTrainable = player.isNetworkTrainable();
		this.cardStrengthLevel = player.getCardStrengthLevel();
		this.trumpfStrengthLevel = player.getTrumpfStrengthLevel();
		this.trumpfSelectionMethod = player.getTrumpfSelectionMethod();
	}

	public boolean wasStartingPlayer(Round round) {
		assert !round.getMoves().isEmpty();
		return round.getMoves().get(0).getPlayer().equals(this);
	}

	public boolean isPartner(Player other) {
		return !this.equals(other) && (getSeatId() + other.getSeatId()) % 2 == 0;
	}

	public JassStrategy getCurrentJassStrategy() {
		return currentJassStrategy;
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

	public String getName() {
		return name;
	}

	public boolean isMctsEnabled() {
		return mctsEnabled;
	}

	public void setMctsEnabled(boolean mctsEnabled) {
		this.mctsEnabled = mctsEnabled;
	}

	public boolean isValueEstimaterUsed() {
		return valueEstimaterUsed;
	}

	public void setValueEstimaterUsed(boolean valueEstimaterUsed) {
		this.valueEstimaterUsed = valueEstimaterUsed;
	}

	public boolean isNetworkTrainable() {
		return networkTrainable;
	}

	public void setNetworkTrainable(boolean networkTrainable) {
		this.networkTrainable = networkTrainable;
	}

	public StrengthLevel getCardStrengthLevel() {
		return cardStrengthLevel;
	}

	public void setCardStrengthLevel(StrengthLevel cardStrengthLevel) {
		this.cardStrengthLevel = cardStrengthLevel;
	}

	public StrengthLevel getTrumpfStrengthLevel() {
		return trumpfStrengthLevel;
	}

	public void setTrumpfStrengthLevel(StrengthLevel trumpfStrengthLevel) {
		this.trumpfStrengthLevel = trumpfStrengthLevel;
	}

	public TrumpfSelectionMethod getTrumpfSelectionMethod() {
		return trumpfSelectionMethod;
	}

	public void setTrumpfSelectionMethod(TrumpfSelectionMethod trumpfSelectionMethod) {
		this.trumpfSelectionMethod = trumpfSelectionMethod;
	}

	public Set<Card> getCards() {
		return cards;
	}

	public void setCards(Set<Card> cards) {
		this.cards.clear();
		this.cards.addAll(cards);
	}

	public Move makeMove(GameSession session) {
		if (cards.size() == 0) throw new RuntimeException("Cannot play a card without cards in deck");
		final Card cardToPlay = chooseCardWithFallback(session);
		return new Move(this, cardToPlay);
	}

	private Card chooseCardWithFallback(GameSession session) {
		// NOTE: This is used in benchmarks (to see if a higher strength level is really worth it)
		// It is a bit of a hack but only used for tests
		if (currentJassStrategy instanceof JassTheRipperJassStrategy) {
			if (cardStrengthLevel != null)
				((JassTheRipperJassStrategy) currentJassStrategy).setCardStrengthLevel(cardStrengthLevel);
			if (trumpfStrengthLevel != null)
				((JassTheRipperJassStrategy) currentJassStrategy).setTrumpfStrengthLevel(trumpfStrengthLevel);
		}

		final Card cardToPlay = currentJassStrategy.chooseCard(cards, session);
		final boolean cardIsInvalid = !session.getCurrentRound().getMode().canPlayCard(
				cardToPlay,
				session.getCurrentRound().getPlayedCards(),
				session.getCurrentRound().getRoundColor(),
				cards);
		if (cardIsInvalid) {
			logger.error("Your strategy tried to play an invalid card. Playing random card instead!");
			return new RandomJassStrategy().chooseCard(cards, session);
		}
		return cardToPlay;
	}

	public Mode chooseTrumpf(GameSession session, boolean shifted) {
		// NOTE: This is used in benchmarks (to see if the MCTS trumpf selection is really better)
		// It is a bit of a hack but only used for tests
		if (currentJassStrategy instanceof JassTheRipperJassStrategy) {
			if (trumpfSelectionMethod != null) {
				((JassTheRipperJassStrategy) currentJassStrategy).setTrumpfSelectionMethod(trumpfSelectionMethod);
			}
		}
		return currentJassStrategy.chooseTrumpf(cards, session, shifted);
	}

	public void onMoveMade(Move move, GameSession session) {
		cards.remove(move.getPlayedCard());
		currentJassStrategy.onMoveMade(move, session);
	}

	public void onSessionFinished() {
		currentJassStrategy.onSessionFinished();
	}

	public void onGameFinished() {
		currentJassStrategy.onGameFinished();
	}

	public void onGameStarted(GameSession session) {
		currentJassStrategy.onGameStarted(session);
	}

	public void onSessionStarted(GameSession session) {
		currentJassStrategy.onSessionStarted(session);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		Player player = (Player) o;

		if (id != null ? !id.equals(player.id) : player.id != null) return false;
		return name != null ? name.equals(player.name) : player.name == null;
	}

	@Override
	public int hashCode() {
		int result = id != null ? id.hashCode() : 0;
		result = 31 * result + (name != null ? name.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return "Player{" +
				"name='" + name + "'" +
				", id=" + id +
				", seatId=" + seatId +
				", cards=" + cards +
				//", currentJassStrategy=" + currentJassStrategy +
				'}';
	}
}
