package com.zuehlke.jasschallenge.localserver;

import org.junit.Test;

import static org.junit.Assert.*;

public class PlayersTest {


    @Test
    public void testGetPartnerOf() {
        Players players = new Players();
        Player player1Team1 = new Player(null);
        players.playerJoined(player1Team1);
        Player player2Team1 = new Player(null);
        players.playerJoined(player2Team1);
        Player player1Team2 = new Player(null);
        players.playerJoined(player1Team2);
        Player player2Team2 = new Player(null);
        players.playerJoined(player2Team2);

        assertEquals(player2Team1, players.getPartnerOf(player1Team1));
        assertEquals(player1Team1, players.getPartnerOf(player2Team1));
        assertEquals(player2Team2, players.getPartnerOf(player1Team2));
        assertEquals(player1Team2, players.getPartnerOf(player2Team2));
    }
}