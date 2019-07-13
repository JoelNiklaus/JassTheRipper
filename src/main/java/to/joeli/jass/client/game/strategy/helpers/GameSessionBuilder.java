package to.joeli.jass.client.game.strategy.helpers;

import to.joeli.jass.client.game.*;
import to.joeli.jass.client.game.strategy.*;
import to.joeli.jass.game.cards.Card;
import to.joeli.jass.game.cards.Color;
import to.joeli.jass.game.mode.Mode;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static to.joeli.jass.game.cards.Card.*;
import static java.util.Arrays.asList;

public class GameSessionBuilder {

	private Mode startedGameMode = null;

	public static final List<Set<Card>> shiftCards = asList(
			EnumSet.of(CLUB_QUEEN, CLUB_ACE, HEART_SIX, HEART_JACK, HEART_KING, DIAMOND_SEVEN, DIAMOND_QUEEN, SPADE_TEN, SPADE_KING),
			EnumSet.of(CLUB_NINE, CLUB_JACK, HEART_EIGHT, HEART_NINE, DIAMOND_EIGHT, DIAMOND_NINE, DIAMOND_TEN, SPADE_EIGHT, SPADE_QUEEN),
			EnumSet.of(CLUB_KING, CLUB_EIGHT, HEART_SEVEN, HEART_QUEEN, DIAMOND_JACK, DIAMOND_KING, SPADE_SEVEN, SPADE_JACK, SPADE_ACE),
			EnumSet.of(CLUB_SIX, CLUB_TEN, CLUB_SEVEN, HEART_TEN, HEART_ACE, DIAMOND_SIX, DIAMOND_ACE, SPADE_SIX, SPADE_NINE));

	public static final List<Set<Card>> topDiamondsCards = asList(
			EnumSet.of(DIAMOND_SIX, DIAMOND_EIGHT, DIAMOND_NINE, DIAMOND_JACK, DIAMOND_QUEEN, DIAMOND_ACE, HEART_ACE, SPADE_SEVEN, SPADE_KING),
			EnumSet.of(DIAMOND_SEVEN, CLUB_TEN, CLUB_QUEEN, CLUB_ACE, HEART_SIX, HEART_JACK, HEART_KING, SPADE_SIX, SPADE_TEN),
			EnumSet.of(DIAMOND_TEN, CLUB_NINE, CLUB_JACK, HEART_EIGHT, HEART_NINE, HEART_TEN, SPADE_EIGHT, SPADE_NINE, SPADE_QUEEN),
			EnumSet.of(DIAMOND_KING, CLUB_SIX, CLUB_SEVEN, CLUB_KING, CLUB_EIGHT, HEART_SEVEN, HEART_QUEEN, SPADE_JACK, SPADE_ACE));

	private List<Player> playingOrder = new ArrayList<>();

	private List<Team> teams = new ArrayList<>();

	private Card[] playedCards = {};

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
			gameSession.startNewGame(startedGameMode, false);

			for (Card card : playedCards) {
				gameSession.makeMove(new Move(gameSession.getCurrentRound().getPlayingOrder().getCurrentPlayer(), card));
				gameSession.getCurrentRound().getPlayingOrder().moveToNextPlayer();
			}
		}
		return gameSession;
	}

	public GameSessionBuilder withStartedGame(Mode mode) {
		startedGameMode = mode;
		return this;
	}

	/**
	 * Make sure not to pass more than 3 played cards!
	 *
	 * @param cards
	 * @return
	 */
	public GameSessionBuilder withCardsPlayed(Card... cards) {
		if (cards.length > 3)
			throw new IllegalArgumentException("Please do not pass more than 3 cards to this method.");
		playedCards = cards;
		return this;
	}

}