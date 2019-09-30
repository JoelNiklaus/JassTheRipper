package to.joeli.jass.client.rest.resources;

import to.joeli.jass.client.rest.Server;
import to.joeli.jass.client.strategy.JassTheRipperJassStrategy;

import javax.ws.rs.Path;

/**
 * Root resource (exposed at "light-playout" path)
 */
@Path("light-playout")
public class LightPlayoutJassResource extends AbstractJassResource {

	@Override
	protected JassTheRipperJassStrategy getJassStrategy() {
		return Server.LIGHT_PLAYOUT_STRATEGY;
	}
}
