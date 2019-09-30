package to.joeli.jass.client.rest.resources;

import to.joeli.jass.client.rest.Server;
import to.joeli.jass.client.strategy.JassTheRipperJassStrategy;

import javax.ws.rs.Path;

/**
 * Root resource (exposed at "runs-100000" path)
 */
@Path("runs-100000")
public class Runs100000JassResource extends AbstractJassResource {

	@Override
	protected JassTheRipperJassStrategy getJassStrategy() {
		return Server.RUNS_100000_STRATEGY;
	}
}
