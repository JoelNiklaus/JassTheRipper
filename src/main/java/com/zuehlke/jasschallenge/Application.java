package com.zuehlke.jasschallenge;

import com.zuehlke.jasschallenge.client.RemoteGame;
import com.zuehlke.jasschallenge.client.game.Player;
import com.zuehlke.jasschallenge.client.game.strategy.JassTheRipperJassStrategy;
import com.zuehlke.jasschallenge.client.game.strategy.JassTheRipperRandomCardJassStrategy;
import com.zuehlke.jasschallenge.client.game.strategy.JassTheRipperRandomTrumpfJassStrategy;
import com.zuehlke.jasschallenge.client.game.strategy.RandomJassStrategy;
import com.zuehlke.jasschallenge.messages.type.SessionType;

import java.util.Arrays;

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
	private static final String BOT_NAME = "JassTheRipper";
	//CHALLENGE2017: Set your own strategy
	private static final JassTheRipperJassStrategy STRATEGY = new JassTheRipperJassStrategy();

	private static final String LOCAL_URL = "ws://127.0.0.1:3000";

	public static void main(String[] args) throws Exception {
		// Competition
		String websocketUrl = parseWebsocketUrlOrDefault(args);

		System.out.println("Connecting... Server socket URL: " + websocketUrl);

		Player player = new Player(BOT_NAME, STRATEGY);
		startGame(websocketUrl, player, SessionType.TOURNAMENT);


		// Testing
		Player jassTheRipper = new Player("JassTheRipper", new JassTheRipperJassStrategy());
		Player jassTheRipperRandomTrumpf = new Player("JassTheRipperRandomTrumpf", new JassTheRipperRandomTrumpfJassStrategy());
		Player jassTheRipperRandomCard = new Player("JassTheRipperRandomCard", new JassTheRipperRandomCardJassStrategy());
		Player randomJasser = new Player("RandomJasser", new RandomJassStrategy());

		// Change here to run different bot
		//startGame(websocketUrl, jassTheRipper, SessionType.SINGLE_GAME);


		//startGame(websocketUrl, randomJasser, SessionType.TOURNAMENT);
		//startGame(websocketUrl, jassTheRipper, SessionType.TOURNAMENT);
		startGame(websocketUrl, jassTheRipperRandomTrumpf, SessionType.TOURNAMENT);
		//startGame(websocketUrl, jassTheRipperRandomCard, SessionType.TOURNAMENT);

	}

	private static String parseWebsocketUrlOrDefault(String[] args) {
		if (args.length > 0) {
			System.out.println("Arguments: " + Arrays.toString(args));
			return args[0];
		}
		return LOCAL_URL;
	}

	private static void startGame(String targetUrl, Player myLocalPlayer, SessionType sessionType) throws Exception {
		RemoteGame remoteGame = new RemoteGame(targetUrl, myLocalPlayer, sessionType);
		remoteGame.start();
	}
}
