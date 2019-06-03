package com.zuehlke.jasschallenge.client.game;

import com.zuehlke.jasschallenge.client.game.strategy.JassStrategy;
import com.zuehlke.jasschallenge.client.game.strategy.JassTheRipperJassStrategy;
import com.zuehlke.jasschallenge.client.game.strategy.StrengthLevel;
import com.zuehlke.jasschallenge.game.cards.Card;
import com.zuehlke.jasschallenge.game.mode.Mode;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static java.util.Arrays.asList;

public class GameSessionBuilder {

    private Mode startedGameMode = null;
    private JassStrategy jassStrategy = JassTheRipperJassStrategy.getInstance(StrengthLevel.FAST_TEST, StrengthLevel.FAST);

    private Set<Card> cards0 = EnumSet.of(Card.CLUB_QUEEN, Card.CLUB_ACE, Card.HEART_SIX, Card.HEART_JACK, Card.HEART_KING, Card.DIAMOND_SEVEN, Card.DIAMOND_QUEEN, Card.SPADE_TEN, Card.SPADE_KING);
    private Set<Card> cards1 = EnumSet.of(Card.CLUB_NINE, Card.CLUB_JACK, Card.HEART_EIGHT, Card.HEART_NINE, Card.DIAMOND_EIGHT, Card.DIAMOND_NINE, Card.DIAMOND_TEN, Card.SPADE_EIGHT, Card.SPADE_QUEEN);
    private Set<Card> cards2 = EnumSet.of(Card.CLUB_KING, Card.CLUB_EIGHT, Card.HEART_SEVEN, Card.HEART_QUEEN, Card.DIAMOND_JACK, Card.DIAMOND_KING, Card.SPADE_SEVEN, Card.SPADE_JACK, Card.SPADE_ACE);
    private Set<Card> cards3 = EnumSet.of(Card.CLUB_SIX, Card.CLUB_TEN, Card.CLUB_SEVEN, Card.HEART_TEN, Card.HEART_ACE, Card.DIAMOND_SIX, Card.DIAMOND_ACE, Card.SPADE_SIX, Card.SPADE_NINE);

    private Player player0 = new Player("0", "Player0", 0, cards0, jassStrategy);
    private Player player1 = new Player("1", "Player1", 1, cards1, jassStrategy);
    private Player player2 = new Player("2", "Player2", 2, cards2, jassStrategy);
    private Player player3 = new Player("3", "Player3", 3, cards3, jassStrategy);

    private List<Player> playingOrder = asList(player0, player1, player2, player3);

    private Team team0 = new Team("Team0", asList(player0, player2));
    private Team team1 = new Team("Team1", asList(player1, player3));

    private List<Team> teams = asList(team0, team1);

    private Card[] playedCards = {};

    public static GameSessionBuilder newSession() {
        return new GameSessionBuilder();
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