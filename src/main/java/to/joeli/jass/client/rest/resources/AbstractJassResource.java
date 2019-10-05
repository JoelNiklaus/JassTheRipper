package to.joeli.jass.client.rest.resources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import to.joeli.jass.client.game.GameSession;
import to.joeli.jass.client.rest.requests.Hand;
import to.joeli.jass.client.rest.requests.JassRequest;
import to.joeli.jass.client.rest.requests.Trick;
import to.joeli.jass.client.rest.responses.CardResponse;
import to.joeli.jass.client.rest.responses.TrumpResponse;
import to.joeli.jass.client.strategy.JassTheRipperJassStrategy;
import to.joeli.jass.client.strategy.helpers.GameSessionBuilder;
import to.joeli.jass.game.cards.Card;
import to.joeli.jass.game.mode.Mode;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

public abstract class AbstractJassResource {

	public static final Logger logger = LoggerFactory.getLogger(AbstractJassResource.class);

	protected abstract JassTheRipperJassStrategy getJassStrategy();

	/**
	 * Method handling HTTP GET requests. The returned object will be sent
	 * to the client as "text/plain" media type.
	 *
	 * @return String that will be returned as a text/plain response.
	 */
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public String info() {
		return "The bot is available :)";
	}

	@POST
	@Path("select_trump")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response selectTrump(JassRequest jassRequest) {
		GameSession gameSession = GameSessionBuilder.newSession()
				.withHSLUInterface(jassRequest.getDealer())
				.createGameSession();
		int seatId = gameSession.getTrumpfSelectingPlayer().getSeatId();
		final boolean shifted = jassRequest.getTss() == 1;
		if (shifted)
			seatId = (seatId + 2) % 4;

		if (seatId != jassRequest.getCurrentPlayer())
			throw new AssertionError("The local current player does not match the server's current player.");

		final Mode trumpf = getJassStrategy().chooseTrumpf(getAvailableCards(jassRequest), gameSession, shifted);

		return Response
				.status(Response.Status.OK)
				.entity(new TrumpResponse(trumpf.getCode()))
				.build();
	}

	@POST
	@Path("play_card")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response playCard(JassRequest jassRequest) {
		List<Card> playedCards = new ArrayList<>();
		for (Trick trick : jassRequest.getTricks())
			playedCards.addAll(trick.getCardsTrick());
		final boolean shifted = jassRequest.getTss() == 1;
		GameSession gameSession = GameSessionBuilder.newSession()
				.withHSLUInterface(jassRequest.getDealer())
				.withStartedGame(Mode.from(jassRequest.getTrump()), shifted)
				.withCardsPlayed(playedCards)
				.createGameSession();

		if (gameSession.getCurrentPlayer().getSeatId() != jassRequest.getCurrentPlayer())
			throw new AssertionError("The local current player does not match the server's current player.");

		final Card card = getJassStrategy().chooseCard(getAvailableCards(jassRequest), gameSession);

		return Response
				.status(Response.Status.OK)
				.entity(new CardResponse(card))
				.build();
	}

	@POST
	@Path("game_info")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response gameInfo(JassRequest jassRequest) {
		logger.info("{}", jassRequest);

		return Response
				.status(Response.Status.OK)
				.build();
	}

	private EnumSet<Card> getAvailableCards(JassRequest jassRequest) {
		final Hand handOfCurrentPlayer = jassRequest.getPlayer().stream()
				.max(Comparator.comparing(hand -> hand.getHand().size()))
				.orElseThrow(() -> new RuntimeException("There has to be at least one hand."));
		return EnumSet.copyOf(handOfCurrentPlayer.getCardsHand());
	}
}
