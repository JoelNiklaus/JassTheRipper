package to.joeli.jass.messages.type;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RemotePlayer {
    private final String id;
    private final String name;
    private final int seatId;

    public RemotePlayer(@JsonProperty(value = "id",required = true) String id,
                        @JsonProperty(value = "name",required = true) String name,
                        @JsonProperty(value = "seatId",required = true) int seatId) {
        this.id = id;
        this.name = name;
        this.seatId = seatId;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getSeatId() {
        return seatId;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RemotePlayer that = (RemotePlayer) o;

        if (seatId != that.seatId) return false;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        return name != null ? name.equals(that.name) : that.name == null;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + seatId;
        return result;
    }

    @Override
    public String toString() {
        return "RemotePlayer{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", seatId=" + seatId +
                '}';
    }
}
