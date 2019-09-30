package to.joeli.jass.client.rest.resources;

import to.joeli.jass.client.rest.Server;
import to.joeli.jass.client.strategy.JassTheRipperJassStrategy;

import javax.ws.rs.Path;

/**
 * Root resource (exposed at "heavy-playout" path)
 */
@Path("heavy-playout")
public class HeavyPlayoutJassResource extends AbstractJassResource {

	@Override
	protected JassTheRipperJassStrategy getJassStrategy() {
		return Server.HEAVY_PLAYOUT_STRATEGY;
	}
}
