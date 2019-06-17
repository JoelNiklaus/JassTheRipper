package com.zuehlke.jasschallenge.client;

import com.zuehlke.jasschallenge.client.game.Player;
import com.zuehlke.jasschallenge.client.websocket.GameHandler;
import com.zuehlke.jasschallenge.client.websocket.RemoteGameSocket;
import com.zuehlke.jasschallenge.messages.type.SessionType;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.concurrent.TimeUnit;

public class RemoteGame implements Game {

	private static final Logger logger = LoggerFactory.getLogger(RemoteGame.class);
	private static final int CLOSE_TIMEOUT_MIN = 120; // INFO: This is very important! The bot will terminate itself after that time. So do not set too low!
	private final Player player;
	private final String targetUrl;
	private final SessionType sessionType;
	private final String sessionName;
	private final Integer chosenTeamIndex;
	private final String advisedPlayerName;

	public RemoteGame(String targetUrl, Player player, SessionType sessionType) {
		this(targetUrl, player, sessionType, "Java Client Session", 1, null);
	}

	public RemoteGame(String targetUrl, Player player, SessionType sessionType, String sessionName, int chosenTeamIndex, String advisedPlayerName) {
		this.targetUrl = targetUrl;
		this.player = player;
		this.sessionType = sessionType;
		this.sessionName = sessionName;
		this.chosenTeamIndex = chosenTeamIndex;
		this.advisedPlayerName = advisedPlayerName;
	}

	@Override
	public void start() {
		WebSocketClient client;
		if (targetUrl.contains("wss")) {
			final SslContextFactory sslContextFactory = new SslContextFactory();
			sslContextFactory.setTrustAll(true);
			client = new WebSocketClient(sslContextFactory);
		} else {
			client = new WebSocketClient();
		}
		try {
			RemoteGameSocket socket = new RemoteGameSocket(new GameHandler(player, sessionType, sessionName, chosenTeamIndex, advisedPlayerName));
			client.start();

			URI uri = new URI(targetUrl);
			ClientUpgradeRequest request = new ClientUpgradeRequest();
			client.connect(socket, uri, request);
			logger.debug("Connecting to: {}", uri);
			socket.awaitClose(CLOSE_TIMEOUT_MIN, TimeUnit.MINUTES);
		} catch (Exception e) {
			logger.debug("{}", e);
		} finally {
			try {
				client.stop();
			} catch (Exception e) {
				e.printStackTrace();
				logger.error("Could not stop the websocket client.");
			}
		}
	}

}
