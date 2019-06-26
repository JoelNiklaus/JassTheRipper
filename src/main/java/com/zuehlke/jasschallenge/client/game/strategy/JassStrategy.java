package com.zuehlke.jasschallenge.client.game.strategy;

import com.zuehlke.jasschallenge.client.game.GameSession;
import com.zuehlke.jasschallenge.client.game.Move;
import com.zuehlke.jasschallenge.game.cards.Card;
import com.zuehlke.jasschallenge.game.mode.Mode;

import java.io.Serializable;
import java.util.Set;

public interface JassStrategy extends Serializable {
    Mode chooseTrumpf(Set<Card> availableCards, GameSession session, boolean isGschobe);
    Card chooseCard(Set<Card> availableCards, GameSession session);

    default void onSessionStarted(GameSession session) {}
    default void onGameStarted(GameSession session) {}
    default void onMoveMade(Move move, GameSession session) {}
    default void onGameFinished() {}
    default void onSessionFinished() {}
}
