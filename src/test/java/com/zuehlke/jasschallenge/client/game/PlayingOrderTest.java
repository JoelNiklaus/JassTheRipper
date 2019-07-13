package com.zuehlke.jasschallenge.client.game;

import org.junit.Test;

import java.util.List;

import static java.util.Arrays.asList;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.*;

public class PlayingOrderTest {
    @Test
    public void moveToNextPlayer_goesThroughAllPlayers() {
        final Player playerA = new Player("a");
        final Player playerB = new Player("b");
        final Player playerC = new Player("c");
        final Player playerD = new Player("d");
        final List<Player> players = asList(playerA, playerB, playerC, playerD);
        final PlayingOrder order = PlayingOrder.createOrder(players);

        assertThat(order.getCurrentPlayer(), equalTo(playerA));
        order.moveToNextPlayer();
        assertThat(order.getCurrentPlayer(), equalTo(playerB));
        order.moveToNextPlayer();
        assertThat(order.getCurrentPlayer(), equalTo(playerC));
        order.moveToNextPlayer();
        assertThat(order.getCurrentPlayer(), equalTo(playerD));
    }

    @Test
    public void moveToNextPlayer_startsWithCorrectPlayer_ifOffsetWasSpecified() {
        final Player playerA = new Player("a");
        final Player playerB = new Player("b");
        final Player playerC = new Player("c");
        final Player playerD = new Player("d");
        final List<Player> players = asList(playerA, playerB, playerC, playerD);
        final PlayingOrder order = PlayingOrder.createOrderStartingFromPlayer(players, playerC);

        assertThat(order.getCurrentPlayer(), equalTo(playerC));
        order.moveToNextPlayer();
        assertThat(order.getCurrentPlayer(), equalTo(playerD));
        order.moveToNextPlayer();
        assertThat(order.getCurrentPlayer(), equalTo(playerA));
        order.moveToNextPlayer();
        assertThat(order.getCurrentPlayer(), equalTo(playerB));
    }

    @Test
    public void testGetPlayersInCurrentPlayingOrder() {
        final Player playerA = new Player("a");
        final Player playerB = new Player("b");
        final Player playerC = new Player("c");
        final Player playerD = new Player("d");
        final List<Player> players = asList(playerA, playerB, playerC, playerD);
        final PlayingOrder order = PlayingOrder.createOrderStartingFromPlayer(players, playerC);

        assertThat(order.getCurrentPlayer(), equalTo(playerC));
        order.moveToNextPlayer();
        assertThat(order.getCurrentPlayer(), equalTo(order.getPlayersInCurrentOrder().get(0)));
        order.moveToNextPlayer();
        assertThat(order.getCurrentPlayer(), equalTo(playerA));
        order.moveToNextPlayer();
        assertThat(order.getCurrentPlayer(), equalTo(playerB));
    }
}