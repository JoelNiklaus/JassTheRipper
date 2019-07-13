package to.joeli.jass.game.mode;

import to.joeli.jass.client.game.Game;
import to.joeli.jass.game.Trumpf;
import to.joeli.jass.game.cards.Card;
import to.joeli.jass.game.cards.CardValue;
import to.joeli.jass.game.cards.Color;

import java.util.Comparator;
import java.util.Optional;
import java.util.Set;

import static java.lang.String.valueOf;

class TrumpfColorMode extends Mode {

    private final Color trumpfColor;

    public TrumpfColorMode(Color trumpfColor) {
        this.trumpfColor = trumpfColor;
    }

    @Override
    public Trumpf getTrumpfName() {
        return Trumpf.TRUMPF;
    }

    @Override
    public Color getTrumpfColor() {
        return trumpfColor;
    }

    @Override
    public int getCode() {
        return trumpfColor.getValue();
    }

    @Override
    public int calculateRoundScore(int roundNumber, Set<Card> playedCards) {
        if(roundNumber == Game.LAST_ROUND_NUMBER) {
            return GeneralRules.calculateLastRoundBonus(getFactor()) + calculateScore(playedCards);
        }
        return calculateScore(playedCards);
    }

    @Override
    public int calculateScore(Set<Card> playedCards) {
        return getFactor() * playedCards.stream()
                .mapToInt(card -> {
                    if(card.getValue() == CardValue.EIGHT) return 0;
                    if (isTrumpf(card)) {
                        return card.getValue().getTrumpfScore();
                    } else {
                        return card.getValue().getScore();
                    }
                }).sum();
    }


    @Override
    public boolean canPlayCard(Card card, Set<Card> alreadyPlayedCards, Color currentRoundColor, Set<Card> playerCards) {
        final boolean noCardsHaveBeenPlayed = alreadyPlayedCards.isEmpty();
        final boolean hasOtherCardsOfRoundColor = playerCards.stream().anyMatch(playersCard -> playersCard.getColor() == currentRoundColor);
        final boolean isHighestTrumpfInRound = isTrumpf(card) && isHighestTrumpf(card, alreadyPlayedCards);

        if(noCardsHaveBeenPlayed) return true;
        if(hasOnlyTrumpf(playerCards)) return true;
        if(isTrumpf(card) && currentRoundColor != trumpfColor) return isHighestTrumpfInRound;
        if(currentRoundColor == trumpfColor && hasOnlyJackOfTrumpf(playerCards)) return true;
        else return !hasOtherCardsOfRoundColor || card.getColor() == currentRoundColor;
    }

    @Override
    public int getFactor() {
        return 1;
    }

    @Override
    public Comparator<Card> createRankComparator() {
        return this::compareWithTrumpf;

    }

    @Override
    public String toString() {
        return valueOf(getTrumpfName()) + " - " +  valueOf(getTrumpfColor());
    }

    private boolean hasOnlyJackOfTrumpf(Set<Card> playerCards) {
        return playerCards.stream()
                .filter(cards -> cards.getColor() == trumpfColor)
                .allMatch(card -> card.getValue() == CardValue.JACK);
    }

    private boolean hasOnlyTrumpf(Set<Card> cards) {
        return cards.stream().allMatch(card -> card.getColor() == trumpfColor);
    }

    private boolean isHighestTrumpf(Card card, Set<Card> alreadyPlayedCards) {
        return highestTrumpf(alreadyPlayedCards)
                .map(card::isHigherTrumpfThan)
                .orElse(true);
    }

    private Optional<Card> highestTrumpf(Set<Card> alreadyPlayedCards) {
        return alreadyPlayedCards.stream()
                .filter(card -> card.getColor() == trumpfColor)
                .max(this::compareTrumpf);
    }

    private boolean isTrumpf(Card card) {
        return card.getColor() == trumpfColor;
    }

    private int compareWithTrumpf(Card card1, Card card2) {
        if (isTrumpf(card1) && isTrumpf(card2)) {
            return compareTrumpf(card1, card2);
        } else if (isTrumpf(card1)) {
            return 1;
        } else if (isTrumpf(card2)) {
            return -1;
        } else {
            return compareMoves(card1, card2);
        }
    }

    private int compareTrumpf(Card first, Card second) {
        return first.isHigherTrumpfThan(second) ? 1 : -1;
    }

    private int compareMoves(Card card1, Card card2) {
        return card1.isHigherThan(card2) ? 1 : -1;
    }

}
