package com.zuehlke.jasschallenge;

import com.zuehlke.jasschallenge.client.RemoteGame;
import com.zuehlke.jasschallenge.client.game.Player;
import com.zuehlke.jasschallenge.client.game.strategy.RandomJassStrategy;
import com.zuehlke.jasschallenge.messages.type.SessionType;

/**
 * Starts one bot in tournament mode. Add your own strategy to compete in the Jass Challenge Tournament 2017!
 * <br><br>
 * To start from CLI use
 * <pre>
 *     gradlew run [websocketUrl]
 * </pre>
 */
public class Application {
    //CHALLENGE2017: Set your bot name
    private static final String BOT_NAME = "awesomeJavaBot";
    //CHALLENGE2017: Set your own strategy
    private static final RandomJassStrategy STRATEGY = new RandomJassStrategy();

    private static final String LOCAL_URL = "ws://127.0.0.1:3000";

    public static void main(String[] args) throws Exception {
        String websocketUrl = parseWebsocketUrlOrDefault(args);

        Player myLocalPlayer = new Player(BOT_NAME, STRATEGY);
        startGame(websocketUrl, myLocalPlayer, SessionType.TOURNAMENT);
    }


    private static String parseWebsocketUrlOrDefault(String[] args) {
        if (args.length > 0) {
            return args[0];
        }
        return LOCAL_URL;
    }

    private static void startGame(String targetUrl, Player myLocalPlayer, SessionType sessionType) throws Exception {
        RemoteGame remoteGame = new RemoteGame(targetUrl, myLocalPlayer, sessionType);
        remoteGame.start();
    }
}
