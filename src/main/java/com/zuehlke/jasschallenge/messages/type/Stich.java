package com.zuehlke.jasschallenge.messages.type;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class Stich {
    private final String name;
    private final String id;
    private final int seatId;
    private final List<RemoteCard> playedCards;
    private final List<RemoteTeam> teams;

    public Stich(@JsonProperty(value = "name",required = true) String name,
                 @JsonProperty(value = "id",required = true) String id,
                 @JsonProperty(value = "seatId",required = true) int seatId,
                 @JsonProperty(value = "playedCards",required = true) List<RemoteCard> playedCards,
                 @JsonProperty(value = "teams",required = true) List<RemoteTeam> teams) {
        this.name = name;
        this.id = id;
        this.seatId = seatId;
        this.playedCards = playedCards;
        this.teams = teams;
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }

    public List<RemoteCard> getPlayedCards() {
        return playedCards;
    }

    public List<RemoteTeam> getTeams() {
        return teams;
    }

    public int getSeatId() {
        return seatId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Stich stich = (Stich) o;

        if (seatId != stich.seatId) return false;
        if (name != null ? !name.equals(stich.name) : stich.name != null) return false;
        if (id != null ? !id.equals(stich.id) : stich.id != null) return false;
        if (playedCards != null ? !playedCards.equals(stich.playedCards) : stich.playedCards != null) return false;
        return teams != null ? teams.equals(stich.teams) : stich.teams == null;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (id != null ? id.hashCode() : 0);
        result = 31 * result + seatId;
        result = 31 * result + (playedCards != null ? playedCards.hashCode() : 0);
        result = 31 * result + (teams != null ? teams.hashCode() : 0);
        return result;
    }
}
