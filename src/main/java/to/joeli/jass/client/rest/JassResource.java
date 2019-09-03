package to.joeli.jass.client.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import to.joeli.jass.client.game.GameSession;
import to.joeli.jass.client.strategy.JassStrategy;
import to.joeli.jass.client.strategy.JassTheRipperJassStrategy;
import to.joeli.jass.client.strategy.config.Config;
import to.joeli.jass.client.strategy.config.MCTSConfig;
import to.joeli.jass.client.strategy.config.StrengthLevel;
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

/**
 * Root resource (exposed at "jass" path)
 */
@Path("jass")
public class JassResource {

	private MCTSConfig mctsConfig = new MCTSConfig(StrengthLevel.IRONMAN);
	private Config config = new Config(mctsConfig);
	private JassStrategy jassStrategy = new JassTheRipperJassStrategy(config);

	public static final Logger logger = LoggerFactory.getLogger(JassResource.class);


	/**
	 * Method handling HTTP GET requests. The returned object will be sent
	 * to the client as "text/plain" media type.
	 *
	 * @return String that will be returned as a text/plain response.
	 */
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public String getIt() {
		return "Got it!";
	}

	@POST
	@Path("select_trump")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response selectTrump(JassRequest jassRequest) {
		final EnumSet<Card> availableCards = getAvailableCards(jassRequest);
		final boolean shifted = jassRequest.getTss() == 1;
		GameSession gameSession = GameSessionBuilder.newSession()
				.withHSLUInterface(jassRequest.getDealer())
				.createGameSession();
		int seatId = gameSession.getTrumpfSelectingPlayer().getSeatId();
		if (shifted)
			seatId = (seatId + 2) % 4;
		if (seatId != jassRequest.getCurrentPlayer())
			throw new AssertionError("The local current player does not match the server's current player.");
		final Mode trumpf = jassStrategy.chooseTrumpf(availableCards, gameSession, shifted);

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
		jassRequest.getTricks().forEach(trick -> playedCards.addAll(trick.getCardsTrick()));
		GameSession gameSession = GameSessionBuilder.newSession()
				.withHSLUInterface(jassRequest.getDealer())
				.withStartedGame(Mode.from(jassRequest.getTrump()), jassRequest.getTss() == 1)
				.withCardsPlayed(playedCards)
				.createGameSession();
		if (gameSession.getCurrentPlayer().getSeatId() != jassRequest.getCurrentPlayer())
			throw new AssertionError("The local current player does not match the server's current player.");
		final Card card = jassStrategy.chooseCard(getAvailableCards(jassRequest), gameSession);

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
