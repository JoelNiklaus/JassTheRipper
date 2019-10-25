package to.joeli.jass.client.rest;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import to.joeli.jass.client.strategy.JassTheRipperJassStrategy;
import to.joeli.jass.client.strategy.config.*;
import to.joeli.jass.client.strategy.mcts.HeavyJassPlayoutSelectionPolicy;
import to.joeli.jass.client.strategy.mcts.LightJassPlayoutSelectionPolicy;
import to.joeli.jass.client.strategy.mcts.src.PlayoutSelectionPolicy;

import java.io.IOException;
import java.net.URI;

/**
 * Main class.
 */
public class Server {

	private static final StrengthLevel strengthLevel = StrengthLevel.HSLU_SERVER;

	public static final JassTheRipperJassStrategy RANDOM_PLAYOUT_STRATEGY = new JassTheRipperJassStrategy(
			new Config(
					new MCTSConfig(strengthLevel, (PlayoutSelectionPolicy) null)
			));

	public static final JassTheRipperJassStrategy LIGHT_PLAYOUT_STRATEGY = new JassTheRipperJassStrategy(
			new Config(
					new MCTSConfig(strengthLevel, new LightJassPlayoutSelectionPolicy())
			));

	public static final JassTheRipperJassStrategy HEAVY_PLAYOUT_STRATEGY = new JassTheRipperJassStrategy(
			new Config(
					new MCTSConfig(strengthLevel, new HeavyJassPlayoutSelectionPolicy())
			));

	public static final JassTheRipperJassStrategy RUNS_100000_STRATEGY = new JassTheRipperJassStrategy(
			new Config(
					new MCTSConfig(strengthLevel, RunMode.RUNS, 1)
			));

	public static final JassTheRipperJassStrategy MCTS_TRUMPF_STRATEGY = new JassTheRipperJassStrategy(
			new Config(
					new MCTSConfig(strengthLevel, strengthLevel), TrumpfSelectionMethod.MCTS
			));

	// Base URI the Grizzly HTTP server will listen on
	public static final String BASE_URI = "http://0.0.0.0/";

	public static final Logger logger = LoggerFactory.getLogger(Server.class);


	/**
	 * Starts Grizzly HTTP server exposing JAX-RS resources defined in this application.
	 *
	 * @return Grizzly HTTP server.
	 */
	public static HttpServer startServer() {
		// create a resource config that scans for JAX-RS resources and providers
		// in to.joeli.jass.client.rest.resources package
		final ResourceConfig rc = new ResourceConfig().packages("to.joeli.jass.client.rest.resources");

		// create and start a new instance of grizzly http server
		// exposing the Jersey application at BASE_URI
		return GrizzlyHttpServerFactory.createHttpServer(URI.create(BASE_URI), rc);
	}

	/**
	 * Main method.
	 *
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) {
		final HttpServer server = startServer();
		logger.info("Jersey app started with WADL available at {}sapplication.wadl", BASE_URI);
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			server.shutdownNow();
			RANDOM_PLAYOUT_STRATEGY.shutDown();
			LIGHT_PLAYOUT_STRATEGY.shutDown();
			HEAVY_PLAYOUT_STRATEGY.shutDown();
			logger.info("Server shut down gracefully.");
		}));
	}
}

