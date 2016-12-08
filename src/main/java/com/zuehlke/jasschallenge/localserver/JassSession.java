package com.zuehlke.jasschallenge.localserver;

import com.zuehlke.jasschallenge.game.Trumpf;
import com.zuehlke.jasschallenge.game.cards.Card;
import com.zuehlke.jasschallenge.game.cards.Color;
import com.zuehlke.jasschallenge.messages.*;
import com.zuehlke.jasschallenge.messages.responses.ChooseTrumpf;
import com.zuehlke.jasschallenge.messages.type.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static java.util.stream.Collectors.toList;

class JassSession {

    private final Logger logger = LoggerFactory.getLogger(JassSession.class);

    private static final int SCORE_LIMIT = 2500;
    private final String sessionName;

    private final Players players = new Players();

    private SessionScore sessionScore;
    private Round currentRound;

    public JassSession(SessionType sessionType, String sessionName) {
        this.sessionName = sessionName;
    }

    public void join(Player player) {
        players.playerJoined(player);

        List<RemotePlayer> playersInSession = players.asRemotePlayers();
        PlayerJoinedSession playerJoinedSession = new PlayerJoinedSession(sessionName, players.createRemotePlayerFor(player), playersInSession);
        PlayerJoined message = new PlayerJoined(playerJoinedSession);
        player.notify(message);
        players.getPlayers().stream().filter(p -> p != player).forEach(p -> p.notify(message));
        if (players.has2CompleteTeams()) {
            players.broadcastTeams();
            startSession();
        }
    }


    private void notifyWinnerTeam() {
        players.broadcastWinner(sessionScore);
    }

    private void playNextRound() {
        // deal cards
        players.getPlayers().forEach(this::dealCards);

        // getTrumpf
        Player nextToPlay = players.getStartPlayer();
        nextToPlay.notify(new RequestTrumpf());

        while (!sessionScore.winnerExists()) {
            StichResult stichResult;
            while (currentRound.hasCardsToPlay() && !sessionScore.winnerExists()) {
                stichResult = currentRound.playStich(players);
                Player stichPlayer = stichResult.getStichPlayer();
                players.stichMade(stichPlayer);
                RemoteTeam winningTeam = players.getTeamOf(stichPlayer);

                sessionScore.appendScore(stichResult.getPoints(), winningTeam);
                addMatchScoreIfMatch(sessionScore, winningTeam);

                players.broadcastStich(stichResult, sessionScore.getTeams());
                logger.debug("Stich finished: {}", stichResult);
            }
            prepareNextRound();
        }

    }

    private void addMatchScoreIfMatch(SessionScore sessionScore, RemoteTeam winningTeam) {
        if (isMatch(sessionScore)) {
            sessionScore.appendScore(currentRound.getMode().getFactor() * 100, winningTeam);
        }
    }

    private boolean isMatch(SessionScore sessionScore) {
        return !currentRound.hasCardsToPlay() && sessionScore.getTeams().stream().filter(team -> team.getCurrentRoundPoints() == 0).findAny().isPresent();
    }

    private void prepareNextRound() {
        sessionScore.updateRoundScores();
        players.broadcastNewGame(sessionScore.getTeams());
        players.newRound();
        currentRound = new Round(players.getPlayer1Team1(), players.getPlayer1Team2() , players.getPlayer2Team1(), players.getPlayer2Team2());

        // deal cards
        players.getPlayers().stream().forEach(this::dealCards);

        // getTrumpf
        Player nextToPlay = players.getStartPlayer();
        nextToPlay.notify(new RequestTrumpf());
    }

    private void dealCards(Player player) {
        List<Card> cards = currentRound.getCards(player);
        List<RemoteCard> remoteCards = cards.stream().map(Mapping::mapToRemoteCard).collect(toList());
        player.notify(new DealCard(remoteCards));
    }

    private void startSession() {
        sessionScore = new SessionScore(players.getTeam1(), players.getTeam2(), SCORE_LIMIT);
        currentRound = new Round(players.getPlayer1Team1(), players.getPlayer1Team2() , players.getPlayer2Team1(), players.getPlayer2Team2());

        while ( ! sessionScore.winnerExists()) {
            playNextRound();
        }
        notifyWinnerTeam();
    }

    public void onTrumpfChosen(ChooseTrumpf chooseTrumpf, Player player) {
        TrumpfChoice data = chooseTrumpf.getData();
        if (Trumpf.SCHIEBE == data.getMode()) {
            currentRound.setShifted();
            Player partnerOf = players.getPartnerOf(player);
            players.broadcastTrumpf(chooseTrumpf.getData());
            ChooseTrumpf trumpf = partnerOf.ask(new RequestTrumpf(), ChooseTrumpf.class);
            onTrumpfChosen(trumpf, partnerOf);
        } else {
            Color color = data.getTrumpfColor() != null ? data.getTrumpfColor().getMappedColor() : null;
            currentRound.setTrumpf(data.getMode(), color);
            players.broadcastTrumpf(data);
        }
    }

    public boolean hasJoinedPlayer(Player player) {
        return players.hasPlayer(player);
    }
}
