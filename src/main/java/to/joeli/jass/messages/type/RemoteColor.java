package to.joeli.jass.messages.type;

import to.joeli.jass.game.cards.Color;

import java.util.Arrays;

public enum RemoteColor {
    HEARTS(Color.HEARTS),
    DIAMONDS(Color.DIAMONDS),
    SPADES(Color.SPADES),
    CLUBS(Color.CLUBS);

    private final Color mappedColor;

    RemoteColor(Color mappedColor) {
        this.mappedColor = mappedColor;
    }

    public static RemoteColor from(Color color) {
        return Arrays.stream(RemoteColor.values()).filter(c -> c.getMappedColor() == color).findFirst().get();
    }

    public Color getMappedColor() {
        return mappedColor;
    }
}
