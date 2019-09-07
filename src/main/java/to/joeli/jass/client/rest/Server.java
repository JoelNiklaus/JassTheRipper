package to.joeli.jass.client.rest;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import to.joeli.jass.client.strategy.JassTheRipperJassStrategy;
import to.joeli.jass.client.strategy.config.Config;
import to.joeli.jass.client.strategy.config.MCTSConfig;
import to.joeli.jass.client.strategy.config.StrengthLevel;

import java.io.IOException;
import java.net.URI;

/**
 * Main class.
 */
public class Server {

	private static MCTSConfig mctsConfig = new MCTSConfig(StrengthLevel.HSLU_SERVER);
	private static Config config = new Config(mctsConfig);
	static JassTheRipperJassStrategy jassStrategy = new JassTheRipperJassStrategy(config);

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
		// in com.example package
		final ResourceConfig rc = new ResourceConfig().packages("to.joeli.jass.client.rest");

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
	public static void main(String[] args) throws IOException {
		final HttpServer server = startServer();
		logger.info("Jersey app started with WADL available at {}sapplication.wadl", BASE_URI);
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			server.shutdownNow();
			jassStrategy.shutDown();
			logger.info("Server shut down gracefully.");
		}));
	}
}

