package com.zuehlke.jasschallenge.client.websocket;

import com.zuehlke.jasschallenge.client.game.Player;
import com.zuehlke.jasschallenge.messages.type.RemotePlayer;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

public class PlayerMapperTest {

    public static final String PLAYER_0_ID = "uid-0";
    public static final String PLAYER_1_ID = "uid-1";
    public static final String PLAYER_2_ID = "uid-2";

    @Test
    public void mapPlayer_returnsExistingPlayer() {
        final Player localPlayer = new Player(PLAYER_0_ID, "localPlayer", 0);

        final Player foundPlayer = new PlayerMapper(localPlayer).mapPlayer(new RemotePlayer(PLAYER_0_ID, "localPlayer", 0));

        assertThat(foundPlayer, equalTo(localPlayer));
    }

    @Test
    public void mapPlayer_returnsNewlyCreatedPlayer() {
        final Player localPlayer = new Player(PLAYER_1_ID, "localPlayer", 1);

        final Player foundPlayer = new PlayerMapper(localPlayer).mapPlayer(new RemotePlayer(PLAYER_0_ID, "unknown", 0));

        assertThat(foundPlayer, not(equalTo(localPlayer)));
    }

    @Test
    public void mapPlayer_returnsPlayer_afterAUnknownPlayerWasMapped() {
        final Player localPlayer = new Player("localPlayer");
        final PlayerMapper playerMapper = new PlayerMapper(localPlayer);
        playerMapper.mapPlayer(new RemotePlayer(PLAYER_0_ID, "will be created", 0));

        final Player foundPlayer = playerMapper.findPlayerById(PLAYER_0_ID);

        assertThat(foundPlayer.getId(), equalTo(0));
    }

    @Test
    public void findPlayerByName_returnsFoundPlayer() {
        final Player localPlayer = new Player(PLAYER_1_ID, "localPlayer", 0);

        final Player foundPlayer = new PlayerMapper(localPlayer).findPlayerById(PLAYER_1_ID);

        assertThat(foundPlayer, equalTo(localPlayer));
    }

    @Test(expected = RuntimeException.class)
    public void findPlayerByName_throwsException_ifNoSuchPlayerExists() {
        new PlayerMapper(new Player(PLAYER_1_ID, "localPlayer", 1)).findPlayerById(PLAYER_2_ID);
    }

}