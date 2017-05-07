package com.zuehlke.jasschallenge.client.game.strategy;

import com.zuehlke.jasschallenge.client.game.Game;
import com.zuehlke.jasschallenge.client.game.GameSession;
import com.zuehlke.jasschallenge.client.game.Round;
import com.zuehlke.jasschallenge.game.cards.Card;
import com.zuehlke.jasschallenge.game.mode.Mode;

import java.io.Serializable;
import java.util.*;

public class RandomJassStrategy implements JassStrategy, Serializable {
    @Override
    public Mode chooseTrumpf(Set<Card> availableCards, GameSession session, boolean isGschobe) {
        final List<Mode> allPossibleModes = Mode.standardModes();
        if (!isGschobe) {
            allPossibleModes.add(Mode.shift());
        }
        return allPossibleModes.get(new Random().nextInt(allPossibleModes.size()));
    }

    @Override
    public Card chooseCard(Set<Card> availableCards, GameSession session) {
        final Game currentGame = session.getCurrentGame();
        final Round round = currentGame.getCurrentRound();
        final Mode gameMode = round.getMode();

        return availableCards.stream()
                    .filter(card -> gameMode.canPlayCard(card, round.getPlayedCards(), round.getRoundColor(), availableCards))
                    .findAny()
                    .orElseThrow(() -> new RuntimeException("There should always be a card to play"));
    }
}
