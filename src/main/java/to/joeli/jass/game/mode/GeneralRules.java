package to.joeli.jass.game.mode;

import to.joeli.jass.game.cards.Card;
import to.joeli.jass.game.cards.Color;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

class GeneralRules {

    private static final int LAST_ROUND_BONUS = 5;

    public static int calculateLastRoundBonus(int factor) {
        return factor * LAST_ROUND_BONUS;
    }

    public static boolean canPlayCard(Card card, Set<Card> alreadyPlayedCards, Color currentRoundColor, Set<Card> playerCards) {
        return alreadyPlayedCards.isEmpty()
                || card.getColor() == currentRoundColor
                || playerCards.stream().noneMatch(playersCard -> playersCard.getColor() == currentRoundColor);
    }


    public static Optional<Card> determineWinnerCard(List<Card> cards, Comparator<Card> cardRankComparator, Optional<Color> trumpfColor) {
        if (cards == null || cards.isEmpty()) {
            return Optional.empty();
        }
        final Color firstCardColor = cards.get(0).getColor();
        return cards.stream()
                .filter(allCardsWithColorOrTrumpfColor(firstCardColor, trumpfColor))
                .max(cardRankComparator);
    }



    private static Predicate<Card> allCardsWithColorOrTrumpfColor(Color firstCardColor, Optional<Color> trumpfColor) {
        return card -> card.getColor() == trumpfColor.orElse(firstCardColor) || card.getColor() == firstCardColor;
    }

}
