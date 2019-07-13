package to.joeli.jass;

import to.joeli.jass.client.RemoteGame;
import to.joeli.jass.client.game.Player;
import to.joeli.jass.client.game.strategy.JassTheRipperJassStrategy;
import to.joeli.jass.messages.type.SessionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	private static final String SERVER_URL = "wss://jass.joeli.to";

	public static final Logger logger = LoggerFactory.getLogger(Application.class);


	public static void main(String[] args) {
		// Competition
		String websocketUrl = parseWebsocketUrlOrDefault(args);
		String sessionName = parseSessionNameOrDefault(args);
		Integer chosenTeamIndex = parseChosenTeamIndexOrDefault(args);
		String advisedPlayerName = parseAdvisedPlayerNameOrDefault(args);
		String botName = parseBotNameOrDefault(args);
		logger.info("Arguments: {}", Arrays.toString(args));

		logger.info("Connecting... Server socket URL: {}", websocketUrl);

		Player player = new Player(botName, STRATEGY);

		new RemoteGame(websocketUrl, player, SessionType.SINGLE_GAME, sessionName, chosenTeamIndex, advisedPlayerName).start();
	}

	private static String parseWebsocketUrlOrDefault(String[] args) {
		if (args.length > 0) {
			return args[0];
		}
		return LOCAL_URL;
	}

	private static String parseSessionNameOrDefault(String[] args) {
		if (args.length > 1) {
			return args[1];
		}
		return "Java Client Session";
	}

	private static Integer parseChosenTeamIndexOrDefault(String[] args) {
		if (args.length > 2) {
			try {
				return Integer.parseInt(args[2]);
			} catch (NumberFormatException e) {
				return 1;
			}
		}
		return 1;
	}

	private static String parseAdvisedPlayerNameOrDefault(String[] args) {
		if (args.length > 3) {
			return !args[3].equals("null") ? args[3] : null;
		}
		return null;
	}

	private static String parseBotNameOrDefault(String[] args) {
		if (args.length > 4) {
			return !args[4].equals("null") ? args[4] : null;
		}
		return BOT_NAME;
	}
}
