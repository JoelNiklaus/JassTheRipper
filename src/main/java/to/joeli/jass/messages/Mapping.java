package to.joeli.jass.messages;

import to.joeli.jass.game.cards.Card;
import to.joeli.jass.game.cards.Color;
import to.joeli.jass.messages.type.RemoteCard;
import to.joeli.jass.messages.type.RemoteColor;

import java.util.List;
import java.util.Set;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toSet;

public class Mapping {
    public static Card mapToCard(RemoteCard remoteCard) {
        return stream(Card.values())
                .filter(card -> card.getColor() == remoteCard.getColor().getMappedColor())
                .filter(card -> card.getValue().getRank() == remoteCard.getNumber() - 5)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Unable to map card"));
    }

    public static Set<Card> mapAllToCards(List<RemoteCard> remoteCards) {
        return remoteCards.stream().map(Mapping::mapToCard).collect(toSet());
    }

    public static RemoteCard mapToRemoteCard(Card card) {
        final RemoteColor remoteColor = mapColor(card.getColor());
        return new RemoteCard(card.getValue().getRank() + 5, remoteColor);
    }

    public static RemoteColor mapColor(Color localColor) {
        if(localColor == null) return null;

        return stream(RemoteColor.values())
                .filter(color -> color.getMappedColor() == localColor)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Could not map color"));
    }
}
