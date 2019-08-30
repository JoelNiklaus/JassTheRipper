package to.joeli.jass.client.strategy.helpers;

import to.joeli.jass.client.game.*;
import to.joeli.jass.client.strategy.JassTheRipperJassStrategy;
import to.joeli.jass.game.cards.Card;
import to.joeli.jass.game.cards.Color;
import to.joeli.jass.game.mode.Mode;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static to.joeli.jass.game.cards.Card.*;

public class GameSessionBuilder {

	private Mode startedGameMode = null;
	private boolean shifted;

	public static final List<Set<Card>> shiftCards = asList(
			EnumSet.of(CLUB_QUEEN, CLUB_ACE, HEART_SIX, HEART_JACK, HEART_KING, DIAMOND_SEVEN, DIAMOND_QUEEN, SPADE_TEN, SPADE_KING),
			EnumSet.of(CLUB_NINE, CLUB_JACK, HEART_EIGHT, HEART_NINE, DIAMOND_EIGHT, DIAMOND_NINE, DIAMOND_TEN, SPADE_EIGHT, SPADE_QUEEN),
			EnumSet.of(CLUB_KING, CLUB_EIGHT, HEART_SEVEN, HEART_QUEEN, DIAMOND_JACK, DIAMOND_KING, SPADE_SEVEN, SPADE_JACK, SPADE_ACE),
			EnumSet.of(CLUB_SIX, CLUB_SEVEN, CLUB_TEN, HEART_TEN, HEART_ACE, DIAMOND_SIX, DIAMOND_ACE, SPADE_SIX, SPADE_NINE));

	public static final List<Set<Card>> topDiamondsCards = asList(
			EnumSet.of(DIAMOND_SIX, DIAMOND_EIGHT, DIAMOND_NINE, DIAMOND_JACK, DIAMOND_QUEEN, DIAMOND_ACE, HEART_ACE, SPADE_SEVEN, SPADE_KING),
			EnumSet.of(DIAMOND_SEVEN, CLUB_TEN, CLUB_QUEEN, CLUB_ACE, HEART_SIX, HEART_JACK, HEART_KING, SPADE_SIX, SPADE_TEN),
			EnumSet.of(DIAMOND_TEN, CLUB_NINE, CLUB_JACK, HEART_EIGHT, HEART_NINE, HEART_TEN, SPADE_EIGHT, SPADE_NINE, SPADE_QUEEN),
			EnumSet.of(DIAMOND_KING, CLUB_SIX, CLUB_SEVEN, CLUB_KING, CLUB_EIGHT, HEART_SEVEN, HEART_QUEEN, SPADE_JACK, SPADE_ACE));

	private List<Player> playingOrder = new ArrayList<>();

	private List<Team> teams = new ArrayList<>();

	private List<Card> playedCards = new ArrayList<>();

	public GameSessionBuilder(List<Set<Card>> cards) {
		for (int i = 0; i < 4; i++)
			playingOrder.add(new Player("" + i, "Player" + i, i, EnumSet.copyOf(cards.get(i)), JassTheRipperJassStrategy.getTestInstance()));

		teams.add(new Team("Team0", asList(playingOrder.get(0), playingOrder.get(2))));
		teams.add(new Team("Team1", asList(playingOrder.get(1), playingOrder.get(3))));
	}

	/**
	 * Convenience method to quickly get a started session
	 *
	 * @param mode
	 * @return
	 */
	public static GameSession startedSession(Mode mode) {
		return newSession().withStartedGame(mode).createGameSession();
	}

	/**
	 * Convenience method to quickly get a started game
	 *
	 * @param mode
	 * @return
	 */
	public static Game startedGame(Mode mode) {
		return startedSession(mode).getCurrentGame();
	}

	/**
	 * Convenience method to quickly get a started trumpf game
	 *
	 * @return
	 */
	public static Game startedClubsGame() {
		return startedGame(Mode.trump(Color.CLUBS));
	}

	public static GameSessionBuilder newSession(List<Set<Card>> cards) {
		return new GameSessionBuilder(cards);
	}

	public static GameSessionBuilder newSession() {
		return new GameSessionBuilder(shiftCards);
	}

	public GameSession createGameSession() {
		final GameSession gameSession = new GameSession(teams, playingOrder);
		if (startedGameMode != null) {
			gameSession.startNewGame(startedGameMode, shifted);

			for (Card card : playedCards) {
				final Player player = gameSession.getCurrentRound().getPlayingOrder().getCurrentPlayer();
				final Move move = new Move(player, card);
				gameSession.makeMove(move);
				player.onMoveMade(move);
				if (gameSession.getCurrentRound().roundFinished())
					gameSession.startNextRound();
			}
		}
		return gameSession;
	}

	public GameSessionBuilder withStartedGame(Mode mode) {
		startedGameMode = mode;
		return this;
	}

	public GameSessionBuilder withStartedGame(Mode mode, boolean shifted) {
		startedGameMode = mode;
		this.shifted = shifted;
		return this;
	}

	public GameSessionBuilder withCardsPlayed(Card... cards) {
		return withCardsPlayed(Arrays.stream(cards).collect(Collectors.toList()));
	}

	public GameSessionBuilder withCardsPlayed(List<Card> cards) {
		playedCards.addAll(cards);
		return this;
	}

	public GameSessionBuilder withStartedClubsGameWithRoundsPlayed(int roundsPlayed) {
		if (roundsPlayed > 0)
			playedCards.addAll(Arrays.asList(CLUB_QUEEN, CLUB_NINE, CLUB_EIGHT, CLUB_SIX));
		if (roundsPlayed > 1)
			playedCards.addAll(Arrays.asList(CLUB_JACK, CLUB_KING, CLUB_SEVEN, CLUB_ACE));
		if (roundsPlayed > 2)
			playedCards.addAll(Arrays.asList(HEART_NINE, HEART_QUEEN, HEART_ACE, HEART_SIX));
		if (roundsPlayed > 3)
			playedCards.addAll(Arrays.asList(DIAMOND_ACE, DIAMOND_SEVEN, DIAMOND_TEN, DIAMOND_JACK));
		if (roundsPlayed > 4)
			playedCards.addAll(Arrays.asList(SPADE_SIX, SPADE_TEN, SPADE_QUEEN, SPADE_ACE));
		if (roundsPlayed > 5)
			playedCards.addAll(Arrays.asList(DIAMOND_KING, DIAMOND_SIX, DIAMOND_QUEEN, DIAMOND_EIGHT));
		if (roundsPlayed > 6)
			playedCards.addAll(Arrays.asList(SPADE_SEVEN, SPADE_NINE, SPADE_KING, SPADE_EIGHT));
		if (roundsPlayed > 7)
			playedCards.addAll(Arrays.asList(HEART_KING, HEART_EIGHT, HEART_SEVEN, HEART_TEN));
		if (roundsPlayed > 8)
			playedCards.addAll(Arrays.asList(HEART_JACK, DIAMOND_NINE, SPADE_JACK, CLUB_TEN));
		return withStartedGame(Mode.trump(Color.CLUBS));
	}

}