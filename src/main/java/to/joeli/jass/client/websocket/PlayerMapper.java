package to.joeli.jass.client.websocket;

import to.joeli.jass.client.game.Player;
import to.joeli.jass.messages.type.RemotePlayer;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

class PlayerMapper {

    private final Set<Player> allPlayers = new HashSet<>();

    public PlayerMapper(Player localPlayer) {
        allPlayers.add(localPlayer);
    }

    public Player mapPlayer(RemotePlayer remotePlayer) {
        Player player = tryToFindPlayerById(remotePlayer.getId()).orElse(new Player(remotePlayer.getId(), remotePlayer.getName(), remotePlayer.getSeatId()));
        allPlayers.add(player);
        return player;
    }

    public Player findPlayerById(String id) {
        return tryToFindPlayerById(id)
                .orElseThrow(() -> new RuntimeException("No Player with id " + id + " found"));
    }

    private Optional<Player> tryToFindPlayerById(String id) {
        return allPlayers.stream()
                .filter(player -> Objects.equals(player.getId(), id))
                .findFirst();
    }
}
