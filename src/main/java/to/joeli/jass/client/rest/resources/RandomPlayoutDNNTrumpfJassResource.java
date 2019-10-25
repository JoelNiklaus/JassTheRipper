package to.joeli.jass.client.rest.resources;

import to.joeli.jass.client.rest.Server;
import to.joeli.jass.client.rest.requests.JassRequest;
import to.joeli.jass.client.strategy.JassTheRipperJassStrategy;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Root resource (exposed at "random-playout-dnn-trumpf" path)
 */
@Path("random-playout-dnn-trumpf")
public class RandomPlayoutDNNTrumpfJassResource extends AbstractJassResource {

	@Override
	protected JassTheRipperJassStrategy getJassStrategy() {
		return Server.RANDOM_PLAYOUT_STRATEGY;
	}

	@POST
	@Path("select_trump")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response selectTrump(JassRequest jassRequest) {
		return forwardRequest(jassRequest, "select_trump");
	}
}
