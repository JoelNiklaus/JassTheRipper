package to.joeli.jass.client.game;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import to.joeli.jass.client.strategy.JassStrategy;
import to.joeli.jass.client.strategy.JassTheRipperJassStrategy;
import to.joeli.jass.client.strategy.RandomJassStrategy;
import to.joeli.jass.client.strategy.config.Config;
import to.joeli.jass.client.strategy.training.networks.CardsEstimator;
import to.joeli.jass.client.strategy.training.networks.ScoreEstimator;
import to.joeli.jass.game.cards.Card;
import to.joeli.jass.game.mode.Mode;

import java.util.EnumSet;
import java.util.Set;

public class Player implements Comparable<Player> {

	private static final Logger logger = LoggerFactory.getLogger(Player.class);

	private String id;
	private final String name;
	private int seatId;
	private final Set<Card> cards;
	private JassStrategy jassStrategy;

	public Player(String id, String name, int seatId) {
		this(name);
		this.id = id;
		this.seatId = seatId;
	}

	public Player(String name) {
		this(name, new RandomJassStrategy());
	}

	public Player(String name, JassStrategy jassStrategy) {
		this.name = name;
		this.cards = EnumSet.noneOf(Card.class);
		this.jassStrategy = jassStrategy;
	}

	public Player(String id, String name, int seatId, Set<Card> cards, JassStrategy jassStrategy) {
		this.id = id;
		this.name = name;
		this.seatId = seatId;
		this.cards = cards;
		this.jassStrategy = jassStrategy;
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
		this.jassStrategy = player.getJassStrategy();
	}

	public boolean wasStartingPlayer(Round round) {
		if (round.getMoves().isEmpty())
			throw new IllegalArgumentException("The round has to contain at least one Move to find the starting player.");
		return round.getMoves().get(0).getPlayer().equals(this);
	}

	public boolean isPartner(Player other) {
		return !this.equals(other) && (getSeatId() + other.getSeatId()) % 2 == 0;
	}

	public JassStrategy getJassStrategy() {
		return jassStrategy;
	}

	public void setJassStrategy(JassStrategy jassStrategy) {
		this.jassStrategy = jassStrategy;
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

	public Set<Card> getCards() {
		return cards;
	}

	public void setCards(Set<Card> cards) {
		this.cards.clear();
		this.cards.addAll(cards);
	}

	public boolean addCard(Card card) {
		if (this.getCards().size() == 9)
			throw new UnsupportedOperationException("Cannot add a card to a player with 9 cards. No player can have more than 9 cards.");
		return this.cards.add(card);
	}

	public void setConfig(Config config) {
		((JassTheRipperJassStrategy) jassStrategy).setConfig(config);
	}

	public Config getConfig() {
		if (!(jassStrategy instanceof JassTheRipperJassStrategy)) throw new AssertionError();
		return ((JassTheRipperJassStrategy) jassStrategy).getConfig();
	}

	public ScoreEstimator getScoreEstimator() {
		if (!(jassStrategy instanceof JassTheRipperJassStrategy)) throw new AssertionError();
		return ((JassTheRipperJassStrategy) jassStrategy).getScoreEstimator();
	}

	public void setScoreEstimator(ScoreEstimator scoreEstimator) {
		if (!(jassStrategy instanceof JassTheRipperJassStrategy)) throw new AssertionError();
		((JassTheRipperJassStrategy) jassStrategy).setScoreEstimator(scoreEstimator);
	}

	public CardsEstimator getCardsEstimator() {
		if (!(jassStrategy instanceof JassTheRipperJassStrategy)) throw new AssertionError();
		return ((JassTheRipperJassStrategy) jassStrategy).getCardsEstimator();
	}

	public void setCardsEstimator(CardsEstimator cardsEstimator) {
		if (!(jassStrategy instanceof JassTheRipperJassStrategy)) throw new AssertionError();
		((JassTheRipperJassStrategy) jassStrategy).setCardsEstimator(cardsEstimator);
	}

	public Move makeMove(GameSession session) {
		if (cards.isEmpty()) throw new RuntimeException("Cannot play a card without cards in deck");
		final Card cardToPlay = chooseCardWithFallback(session);
		return new Move(this, cardToPlay);
	}

	private Card chooseCardWithFallback(GameSession session) {
		final Card cardToPlay = jassStrategy.chooseCard(cards, session);
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
		return jassStrategy.chooseTrumpf(cards, session, shifted);
	}

	public void onMoveMade(Move move) {
		cards.remove(move.getPlayedCard());
		jassStrategy.onMoveMade(move);
	}

	public void onSessionFinished() {
		jassStrategy.onSessionFinished();
	}

	public void onGameFinished() {
		jassStrategy.onGameFinished();
	}

	public void onGameStarted(GameSession session) {
		jassStrategy.onGameStarted(session);
	}

	public void onSessionStarted(GameSession session) {
		jassStrategy.onSessionStarted(session);
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
				//", jassStrategy=" + jassStrategy +
				'}';
	}

	@Override
	public int compareTo(@NotNull Player o) {
		return this.seatId - o.seatId;
	}
}
