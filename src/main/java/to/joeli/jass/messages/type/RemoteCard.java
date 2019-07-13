package to.joeli.jass.messages.type;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RemoteCard {

    private final int number;
    private final RemoteColor color;

    public RemoteCard(@JsonProperty(value = "number",required = true) int number,
                      @JsonProperty(value = "color",required = true) RemoteColor color) {
        this.number = number;
        this.color = color;
    }

    public int getNumber() {
        return number;
    }

    public RemoteColor getColor() {
        return color;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RemoteCard that = (RemoteCard) o;

        if (number != that.number) return false;
        return color == that.color;

    }

    @Override
    public int hashCode() {
        int result = number;
        result = 31 * result + (color != null ? color.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "RemoteCard{" +
                "number=" + number +
                ", color=" + color +
                '}';
    }
}
