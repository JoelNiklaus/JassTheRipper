package to.joeli.jass.client.rest.resources;

import to.joeli.jass.client.rest.Server;
import to.joeli.jass.client.strategy.JassTheRipperJassStrategy;

import javax.ws.rs.Path;

/**
 * Root resource (exposed at "rule-based-trumpf" path)
 */
@Path("rule-based-trumpf")
public class RuleBasedTrumpfJassResource extends TrumpfJassResource {

	@Override
	protected JassTheRipperJassStrategy getJassStrategy() {
		return Server.RANDOM_PLAYOUT_STRATEGY;
	}
}
