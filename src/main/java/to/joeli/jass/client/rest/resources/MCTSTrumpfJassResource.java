package to.joeli.jass.client.rest.resources;

import to.joeli.jass.client.rest.Server;
import to.joeli.jass.client.strategy.JassTheRipperJassStrategy;

import javax.ws.rs.Path;

/**
 * Root resource (exposed at "mcts-trumpf" path)
 */
@Path("mcts-trumpf")
public class MCTSTrumpfJassResource extends TrumpfJassResource {

	@Override
	protected JassTheRipperJassStrategy getJassStrategy() {
		return Server.MCTS_TRUMPF_STRATEGY;
	}
}
