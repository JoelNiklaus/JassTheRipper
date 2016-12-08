package com.zuehlke.jasschallenge.localserver;

import com.zuehlke.jasschallenge.messages.*;
import com.zuehlke.jasschallenge.messages.type.RemotePlayer;
import com.zuehlke.jasschallenge.messages.type.RemoteTeam;
import com.zuehlke.jasschallenge.messages.type.Stich;
import com.zuehlke.jasschallenge.messages.type.TrumpfChoice;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;

class Players {

    private static final int NUMBER_OF_PLAYERS = 4;
    private final List<Player> players = new LinkedList<>();
    private PlayerOrder playerOrder;


    Player getPartnerOf(Player player) {
        int index = players.indexOf(player);
        int increment = index % 2 == 0 ? 1 : -1;
        return players.get(index + increment);
    }

    public void playerJoined(Player player) {
        if (players.size() >= NUMBER_OF_PLAYERS) {
            throw new IllegalArgumentException("Already 4 players");
        }
        this.players.add(player);
        if (players.size() == NUMBER_OF_PLAYERS) {
            initPlayerOrder();
        }

    }

    private void initPlayerOrder() {
        List<Player> orderedBySeat = players.stream().sorted(Comparator.comparingInt(Player::getSeatId)).collect(Collectors.toList());
        this.playerOrder = new PlayerOrder(orderedBySeat.get(0), orderedBySeat.get(2), orderedBySeat.get(1), orderedBySeat.get(3));
    }

    public boolean has2CompleteTeams() {
        return players.size() == NUMBER_OF_PLAYERS;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public void broadcastTeams() {

        List<RemoteTeam> remoteTeams = getRemoteTeams();
        BroadCastTeams broadCastTeams = new BroadCastTeams(remoteTeams);

        players.forEach(player -> player.notify(broadCastTeams));
    }

    private List<RemoteTeam> getRemoteTeams() {
        RemoteTeam team1 = getTeam1();
        RemoteTeam team2 = getTeam2();
        return asList(team1, team2);
    }

    public Player getNextToPlay() {
        return playerOrder.getNextToPlay();
    }

    RemoteTeam getTeam2() {
        Player player1 = players.get(2);
        Player player2 = players.get(3);
        return new RemoteTeam("Team 2 ("+ createRemotePlayerFor(player1).getName() +
                " + "+ createRemotePlayerFor(player2).getName()+")",
                asList(createRemotePlayerFor(player1), createRemotePlayerFor(player2)));
    }

    RemoteTeam getTeam1() {
        Player player1 = players.get(0);
        Player player2 = players.get(1);
        return new RemoteTeam("Team 1 ("+ createRemotePlayerFor(player1).getName() +
                " + "+ createRemotePlayerFor(player2).getName()+")",
                asList(createRemotePlayerFor(player1), createRemotePlayerFor(player2)));
    }

    RemotePlayer createRemotePlayerFor(Player player) {
        return new RemotePlayer(player.getId(), player.getName(), player.getSeatId());
    }

    public List<RemotePlayer> asRemotePlayers() {
        return players.stream().map(this::createRemotePlayerFor).collect(Collectors.toList());
    }

    public Player getPlayer1Team1() {
        return players.get(0);
    }

    public Player getPlayer2Team1() {
        return players.get(1);
    }

    public Player getPlayer1Team2() {
        return players.get(2);
    }

    public Player getPlayer2Team2() {
        return players.get(3);
    }

    public boolean hasPlayer(Player player) {
        return players.contains(player);
    }

    public void broadcastTrumpf(TrumpfChoice data) {
        players.forEach(player->player.notify(new BroadCastTrumpf(data)));
    }

    public Player getStartPlayer() {
        return players.get(0);
    }

    public RemoteTeam getTeamOf(Player stichPlayer){
        if (players.indexOf(stichPlayer) <= 1) {
            return getTeam1();
        } else {
            return getTeam2();
        }
    }

    public void newRound() {
        playerOrder.newRound();
    }

    public void broadcastStich(StichResult stichResult, List<RemoteTeam> teams) {
        Player stichPlayer = stichResult.getStichPlayer();

        // The team which made the Stich must come first
        teams.sort((team1, team2) -> team1.getPlayers().stream().map(RemotePlayer::getId).anyMatch(id -> Objects.equals(id, stichPlayer.getId())) ? -1 : 1);


        Stich stich = new Stich(stichPlayer.getName(), stichPlayer.getId(), stichPlayer.getSeatId(), stichResult.getPlayedCards(), teams);
        BroadCastStich message = new BroadCastStich(stich);
        players.forEach(player -> player.notify(message));
    }

    public void broadcastNewGame(List<RemoteTeam> teams) {
        players.forEach(player -> player.notify(new BroadCastGameFinished(teams)));
    }

    public void broadcastWinner(SessionScore sessionScore) {
        BroadCastWinnerTeam message = new BroadCastWinnerTeam(sessionScore.getWinnerTeam());
        players.forEach(player -> {
            player.notify(message);
        });
    }

    public void stichMade(Player stichPlayer) {
        playerOrder.stichMade(stichPlayer);
    }
}
