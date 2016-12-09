package com.zuehlke.jasschallenge.game.mode;

import com.zuehlke.jasschallenge.client.game.Game;
import com.zuehlke.jasschallenge.game.Trumpf;
import com.zuehlke.jasschallenge.game.cards.Card;
import com.zuehlke.jasschallenge.game.cards.Color;

import java.util.Comparator;
import java.util.Set;

import static java.lang.String.valueOf;

class TopDownMode extends Mode {
    private static final int FACTOR = 3;

    @Override
    public Trumpf getTrumpfName() {
        return Trumpf.OBEABE;
    }

    @Override
    public Color getTrumpfColor() {
        return null;
    }

    @Override
    public int calculateRoundScore(int roundNumber, Set<Card> playedCards) {
        if(roundNumber == Game.LAST_ROUND_NUMBER) {
            return GeneralRules.calculateLastRoundBonus(FACTOR) + calculateScore(playedCards);
        }
        return calculateScore(playedCards);
    }

    @Override
    public int calculateScore(Set<Card> playedCards) {
        return FACTOR * playedCards.stream()
                .mapToInt(card -> card.getValue().getScore())
                .sum();
    }

    @Override
    public boolean canPlayCard(Card card, Set<Card> alreadyPlayedCards, Color currentRoundColor, Set<Card> playerCards) {
        return GeneralRules.canPlayCard(card, alreadyPlayedCards, currentRoundColor, playerCards);
    }

    @Override
    public int getFactor() {
        return FACTOR;
    }

    @Override
    public Comparator<Card> createRankComparator() {
        return (card, card2) -> card.isHigherThan(card2) ? 1 : -1;
    }

    @Override
    public String toString() {

        return valueOf(getTrumpfName());
    }
}
