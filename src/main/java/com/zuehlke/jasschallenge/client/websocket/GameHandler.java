package com.zuehlke.jasschallenge.client.websocket;

import com.zuehlke.jasschallenge.client.game.*;
import com.zuehlke.jasschallenge.game.Trumpf;
import com.zuehlke.jasschallenge.game.cards.Color;
import com.zuehlke.jasschallenge.game.mode.Mode;
import com.zuehlke.jasschallenge.messages.Mapping;
import com.zuehlke.jasschallenge.messages.PlayerJoinedSession;
import com.zuehlke.jasschallenge.messages.responses.ChooseCard;
import com.zuehlke.jasschallenge.messages.responses.ChoosePlayerName;
import com.zuehlke.jasschallenge.messages.responses.ChooseSession;
import com.zuehlke.jasschallenge.messages.responses.ChooseTrumpf;
import com.zuehlke.jasschallenge.messages.type.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;

import static com.zuehlke.jasschallenge.messages.type.SessionChoice.AUTOJOIN;
import static java.util.stream.Collectors.toList;

public class GameHandler {
    private final Player localPlayer;
    private final SessionType sessionType;
    private GameSession gameSession;
    private PlayerMapper playerMapper;
    private boolean shifted = false;


    private final static Logger logger = LoggerFactory.getLogger(GameHandler.class);

    public GameHandler(Player localPlayer, SessionType sessionType) {
        this.localPlayer = localPlayer;
        resetPlayerMapper(localPlayer);
        this.sessionType = sessionType;
    }

    GameHandler(Player localPlayer, GameSession gameSession) {
        this(localPlayer, SessionType.TOURNAMENT);
        this.gameSession = gameSession;
    }

    Round getCurrentRound() {
        return gameSession.getCurrentRound();
    }


    List<Team> getTeams() {
        return gameSession.getTeams();
    }

    public ChooseSession onRequestSessionChoice() {
        return new ChooseSession(AUTOJOIN, "Java Client session", sessionType);
    }

    public ChoosePlayerName onRequestPlayerName() {
        return new ChoosePlayerName(localPlayer.getName());
    }

    public void onDealCards(List<RemoteCard> dealCard) {
        localPlayer.setCards(Mapping.mapAllToCards(dealCard));
    }

    public void onPlayerJoined(PlayerJoinedSession joinedPlayer) {
        if(localPlayer.getId() == null) {
            localPlayer.setId(joinedPlayer.getPlayer().getId());
            localPlayer.setSeatId(joinedPlayer.getPlayer().getSeatId());
        }
    }

    public void onBroadCastTeams(List<RemoteTeam> remoteTeams) {
        final List<Team> teams = mapTeams(remoteTeams);
        final List<Player> playersInPlayingOrder = getPlayersInPlayingOrder(remoteTeams);
        gameSession = new GameSession(teams, playersInPlayingOrder);
        localPlayer.onSessionStarted(gameSession);
    }

    private void resetPlayerMapper(Player localPlayer) {
        localPlayer.setId(null);
        this.playerMapper = new PlayerMapper(localPlayer);
    }

    public ChooseTrumpf onRequestTrumpf() {
        final Mode mode = localPlayer.chooseTrumpf(gameSession, shifted);
        return new ChooseTrumpf(mode.getTrumpfName(), Mapping.mapColor(mode.getTrumpfColor()));
    }

    public void onBroadCastTrumpf(TrumpfChoice trumpfChoice) {
        RemoteColor trumpfColor = trumpfChoice.getTrumpfColor();
        Color mappedColor = trumpfColor == null ? null : trumpfColor.getMappedColor();
        final Mode nextGameMode = Mode.from(trumpfChoice.getMode(), mappedColor);

        if (trumpfChoice.getMode() != Trumpf.SCHIEBE) {
            logger.info("Game started: {}", nextGameMode);
            gameSession.startNewGame(nextGameMode, shifted);
            localPlayer.onGameStarted(gameSession);
            shifted = false;
        } else {
            shifted = true;
        }
    }

    public ChooseCard onRequestCard() {
        checkEquals(getCurrentRound().getPlayingOrder().getCurrentPlayer(), localPlayer, "Order differed between remote and local state");

        final Move move = localPlayer.makeMove(gameSession);
        final RemoteCard cardToPlay = Mapping.mapToRemoteCard(move.getPlayedCard());

        return new ChooseCard(cardToPlay);
    }

    public void onPlayedCards(List<RemoteCard> playedCards) {

        final int playerPosition = playedCards.size() - 1;
        final RemoteCard remoteCard = playedCards.get(playerPosition);

        final Player player = getCurrentRound().getPlayingOrder().getCurrentPlayer();

        final Move move = new Move(player, Mapping.mapToCard(remoteCard));
        gameSession.makeMove(move);
        localPlayer.onMoveMade(move, gameSession);
    }

    public void onBroadCastStich(Stich stich) {
        final Player winner = playerMapper.findPlayerById(stich.getId());
        checkEquals(winner, getCurrentRound().getWinner(), "Local winner differs from remote");

        gameSession.startNextRound();

        checkEquals(stich.getTeams().get(0).getCurrentRoundPoints(),
                gameSession.getCurrentGame().getResult().getTeamScore(winner),
                "Local score differs from remote");
    }

    public void onBroadGameFinished(List<RemoteTeam> remoteTeams) {
        logger.info("Game finished: {} ({}) -- {} ({})",
                remoteTeams.get(0).getName(), remoteTeams.get(0).getCurrentRoundPoints(),
                remoteTeams.get(1).getName(), remoteTeams.get(1).getCurrentRoundPoints());
        logger.info("Current scores: {} ({}) -- {} ({})",
                remoteTeams.get(0).getName(), remoteTeams.get(0).getPoints(),
                remoteTeams.get(1).getName(), remoteTeams.get(1).getPoints());
        localPlayer.onGameFinished();
    }

    public void onBroadCastWinnerTeam(RemoteTeam winnerTeam) {
        logger.info("Session finished. Winner: {} ({})",
                winnerTeam.getName(),
                winnerTeam.getPoints());
        localPlayer.onSessionFinished();
        resetPlayerMapper(localPlayer);
    }

    public void onRejectCard(RemoteCard rejectCard) {
        //throw new RuntimeException("Card was rejected");
    }

    private List<Team> mapTeams(List<RemoteTeam> remoteTeams) {
        return remoteTeams.stream()
                .map(this::toTeam)
                .collect(toList());
    }

    private List<Player> getPlayersInPlayingOrder(List<RemoteTeam> remoteTeams) {
        return remoteTeams.stream()
                .flatMap(remoteTeam -> remoteTeam.getPlayers().stream())
                .sorted(Comparator.comparingInt(RemotePlayer::getSeatId))
                .map(player -> playerMapper.findPlayerById(player.getId()))
                .collect(toList());
    }

    private Team toTeam(RemoteTeam remoteTeam) {
        final List<Player> players = remoteTeam.getPlayers().stream().map(playerMapper::mapPlayer).collect(toList());
        return new Team(remoteTeam.getName(), players);
    }

    private static void checkEquals(Object a, Object b, String errorMessage) {
        if(!a.equals(b)) {
            logger.warn("Expected {} to be equal to {}: {}", a, b, errorMessage);
            throw new RuntimeException(errorMessage);
        }
    }

}
