package com.zuehlke.jasschallenge.client.game.strategy;

import com.zuehlke.jasschallenge.client.game.GameSession;
import com.zuehlke.jasschallenge.game.cards.Card;
import com.zuehlke.jasschallenge.game.mode.Mode;

import java.io.Serializable;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class AlwaysShiftRandomJassStrategy extends RandomJassStrategy implements Serializable {

    @Override
    public Mode chooseTrumpf(Set<Card> availableCards, GameSession session, boolean isGschobe) {
        if (isGschobe) {
            List<Mode> allPossibleModes = Mode.standardModes();
            return allPossibleModes.get(new Random().nextInt(allPossibleModes.size()));
        }
        return Mode.shift();
    }
}
