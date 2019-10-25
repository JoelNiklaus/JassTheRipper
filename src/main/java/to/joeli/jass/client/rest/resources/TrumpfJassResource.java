package to.joeli.jass.client.rest.resources;

import to.joeli.jass.client.rest.Server;
import to.joeli.jass.client.rest.requests.JassRequest;
import to.joeli.jass.client.strategy.JassTheRipperJassStrategy;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public abstract class TrumpfJassResource extends AbstractJassResource {

	private String DNN_MAX_POLICY_PATH = "http://ec2-18-203-236-158.eu-west-1.compute.amazonaws.com:5001/dnn-max-policy-trump";
	// alternatively: 18.203.236.158

	@Override
	protected JassTheRipperJassStrategy getJassStrategy() {
		return Server.RANDOM_PLAYOUT_STRATEGY;
	}

	@POST
	@Path("play_card")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response playCard(JassRequest jassRequest) {
		Client client = ClientBuilder.newClient();
		WebTarget target = client.target(DNN_MAX_POLICY_PATH);

		return target.path("play_card")
				.request(MediaType.APPLICATION_JSON)
				.post(Entity.json(jassRequest));
	}
}
