package to.joeli.jass.client.rest;

import com.google.gson.Gson;
import org.glassfish.grizzly.http.server.HttpServer;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import to.joeli.jass.client.rest.requests.JassRequest;
import to.joeli.jass.client.rest.responses.CardResponse;
import to.joeli.jass.client.rest.responses.TrumpResponse;
import to.joeli.jass.game.cards.Card;
import to.joeli.jass.game.cards.Color;
import to.joeli.jass.game.mode.Mode;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;

@Ignore("On Travis CI this test will not work (maybe because networking is required)")
public class RandomPlayoutJassResourceTest {

	private HttpServer server;
	private WebTarget target;

	private String path = "random-playout";

	@Before
	public void setUp() {
		// start the server
		server = Server.startServer();
		// create the client
		Client c = ClientBuilder.newClient();

		target = c.target(Server.BASE_URI);
	}

	@After
	public void tearDown() {
		server.shutdownNow();
	}

	/**
	 * Test to see that the message "The bot is available :)" is sent in the response.
	 */
	@Test
	public void testGetIt() {
		String responseMsg = target.path(path).request().get(String.class);
		assertEquals("The bot is available :)", responseMsg);
	}

	@Test
	public void testSelectTrumpf() {
		String jsonString = "{\"version\": \"V0.1\", \"dealer\": 0, \"currentPlayer\": 3, \"tricks\": [], \"jassTyp\": \"SCHIEBER_1000\"," +
				" \"player\": [{\"hand\": []}, {\"hand\": []}, {\"hand\": []}, {\"hand\": [\"D10\", \"HA\", \"HJ\", \"SQ\", \"S10\", \"S7\", \"S6\", \"CK\", \"C7\"]}]}";

		Response response = target.path(path).path("select_trump")
				.request(MediaType.APPLICATION_JSON)
				.post(getEntity(jsonString));

		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
		assertEquals(new TrumpResponse(Mode.shift()), response.readEntity(TrumpResponse.class));
		response.close();
	}

	@Test
	public void testSelectTrumpfShifted() {
		String jsonString = "{\"version\": \"V0.1\", \"dealer\": 0, \"currentPlayer\": 1, \"tss\": 1, \"tricks\": [], \"jassTyp\": \"SCHIEBER_1000\"," +
				" \"player\": [{\"hand\": []}, {\"hand\": [\"DJ\", \"D9\", \"H6\", \"SK\", \"CA\", \"CJ\", \"C9\", \"C8\", \"C6\"]}, {\"hand\": []}, {\"hand\": []}]}";

		Response response = target.path(path).path("select_trump")
				.request(MediaType.APPLICATION_JSON)
				.post(getEntity(jsonString));

		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
		assertEquals(new TrumpResponse(Mode.trump(Color.CLUBS)), response.readEntity(TrumpResponse.class));
		response.close();
	}

	@Test
	public void testPlayCard1() {
		String jsonString = "{\"version\": \"V0.1\", \"dealer\": 1, \"currentPlayer\": 1, \"trump\": 4, \"tss\": 1," +
				" \"tricks\": [{\"cards\": [\"SQ\", \"S9\", \"S10\", \"SJ\"], \"points\": 15, \"win\": 0, \"first\": 0}," +
				" {\"cards\": [\"DQ\", \"DA\", \"D8\"], \"first\": 0}], \"jassTyp\": \"SCHIEBER_1000\", " +
				"\"player\": [{\"hand\": []}, {\"hand\": [\"D6\", \"HA\", \"HK\", \"H9\", \"H7\", \"H6\", \"C8\", \"C7\"]}, {\"hand\": []}, {\"hand\": []}]}";

		Response response = target.path(path).path("play_card")
				.request(MediaType.APPLICATION_JSON)
				.post(getEntity(jsonString));

		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
		assertEquals(new CardResponse(Card.DIAMOND_SIX), response.readEntity(CardResponse.class));
		response.close();
	}

	@Test
	public void testPlayCard2() {
		String jsonString = "{\"version\": \"V0.1\", \"dealer\": 1, \"currentPlayer\": 2, \"trump\": 4, \"tss\": 1, " +
				"\"tricks\": [{\"cards\": [\"SQ\", \"S9\", \"S10\", \"SJ\"], \"points\": 15, \"win\": 0, \"first\": 0}, " +
				"{\"cards\": [\"DQ\", \"DA\", \"D8\", \"D7\"], \"points\": 22, \"win\": 3, \"first\": 0}, " +
				"{\"cards\": [\"HJ\"], \"first\": 3}], \"jassTyp\": \"SCHIEBER_1000\", " +
				"\"player\": [{\"hand\": []}, {\"hand\": []}, {\"hand\": [\"DK\", \"D9\", \"H10\", \"SA\", \"CQ\", \"C9\", \"C7\"]}, {\"hand\": []}]}";

		Response response = target.path(path).path("play_card")
				.request(MediaType.APPLICATION_JSON)
				.post(getEntity(jsonString));

		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
		assertEquals(new CardResponse(Card.HEART_TEN), response.readEntity(CardResponse.class));
		response.close();
	}

	@Test
	public void testPlayCard3() {
		String jsonString = "{\"version\": \"V0.1\", \"dealer\": 1, \"currentPlayer\": 1, \"trump\": 4, \"tss\": 1, " +
				"\"tricks\": [{\"cards\": [\"SQ\", \"S9\", \"S10\", \"SJ\"], \"points\": 15, \"win\": 0, \"first\": 0}, " +
				"{\"cards\": [\"DQ\", \"DA\", \"D8\", \"D7\"], \"points\": 22, \"win\": 3, \"first\": 0}, " +
				"{\"cards\": [\"HJ\", \"H10\", \"H6\", \"HQ\"], \"points\": 15, \"win\": 0, \"first\": 3}, " +
				"{\"cards\": [\"CJ\", \"C10\", \"C7\"], \"first\": 0}], \"jassTyp\": \"SCHIEBER_1000\", " +
				"\"player\": [{\"hand\": []}, {\"hand\": [\"D6\", \"HA\", \"HK\", \"H9\", \"H7\", \"C8\"]}, {\"hand\": []}, {\"hand\": []}]}";

		Response response = target.path(path).path("play_card")
				.request(MediaType.APPLICATION_JSON)
				.post(getEntity(jsonString));

		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
		assertEquals(new CardResponse(Card.CLUB_EIGHT), response.readEntity(CardResponse.class));
		response.close();
	}

	@Test
	public void testPlayCard4() {
		String jsonString = "{\"version\": \"V0.1\", \"dealer\": 3, \"currentPlayer\": 1, \"trump\": 5, \"tss\": 1, " +
				"\"tricks\": [{\"cards\": [\"CA\", \"CQ\", \"C10\", \"CJ\"], \"points\": 15, \"win\": 0, \"first\": 2}, " +
				"{\"cards\": [\"D7\", \"D9\", \"DA\", \"DQ\"], \"points\": 3, \"win\": 0, \"first\": 0}, " +
				"{\"cards\": [\"S8\", \"D10\", \"SQ\",\"SK\"], \"points\": 25, \"win\": 0, \"first\": 0}, " +
				"{\"cards\": [\"C9\", \"C7\", \"C8\", \"S9\"], \"points\": 8, \"win\": 3, \"first\": 0}, " +
				"{\"cards\": [\"HQ\", \"H8\", \"HJ\", \"HA\"], \"points\": 13, \"win\": 2, \"first\": 3}, " +
				"{\"cards\": [\"S10\", \"S6\", \"S7\", \"C6\"], \"points\": 32, \"win\": 1, \"first\": 2}, " +
				"{\"cards\": [\"DK\", \"D6\", \"HK\", \"SA\"], \"points\": 19, \"win\": 0, \"first\": 1}, " +
				"{\"cards\": [\"H6\", \"H10\", \"H9\", \"D8\"], \"points\": 29, \"win\": 0, \"first\": 0}, " +
				"{\"cards\": [\"DJ\", \"CK\", \"H7\"], \"first\": 0}], \"jassTyp\": \"SCHIEBER_1000\", " +
				"\"player\": [{\"hand\": []}, {\"hand\": [\"SJ\"]}, {\"hand\": []}, {\"hand\": []}]}";

		Response response = target.path(path).path("play_card")
				.request(MediaType.APPLICATION_JSON)
				.post(getEntity(jsonString));

		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
		assertEquals(new CardResponse(Card.SPADE_JACK), response.readEntity(CardResponse.class));
		response.close();
	}

	@Test
	public void testGameInfo() {
		String jsonString = "{\"version\": \"V0.1\", \"dealer\": 3, \"currentPlayer\": 1, \"trump\": 5, \"tss\": 1, " +
				"\"tricks\": [{\"cards\": [\"CA\", \"CQ\", \"C10\", \"CJ\"], \"points\": 15, \"win\": 0, \"first\": 2}, " +
				"{\"cards\": [\"D7\", \"D9\", \"DA\", \"DQ\"], \"points\": 3, \"win\": 0, \"first\": 0}, " +
				"{\"cards\": [\"S8\", \"D10\", \"SQ\",\"SK\"], \"points\": 25, \"win\": 0, \"first\": 0}, " +
				"{\"cards\": [\"C9\", \"C7\", \"C8\", \"S9\"], \"points\": 8, \"win\": 3, \"first\": 0}, " +
				"{\"cards\": [\"HQ\", \"H8\", \"HJ\", \"HA\"], \"points\": 13, \"win\": 2, \"first\": 3}, " +
				"{\"cards\": [\"S10\", \"S6\", \"S7\", \"C6\"], \"points\": 32, \"win\": 1, \"first\": 2}, " +
				"{\"cards\": [\"DK\", \"D6\", \"HK\", \"SA\"], \"points\": 19, \"win\": 0, \"first\": 1}, " +
				"{\"cards\": [\"H6\", \"H10\", \"H9\", \"D8\"], \"points\": 29, \"win\": 0, \"first\": 0}, " +
				"{\"cards\": [\"DJ\", \"CK\", \"H7\"], \"first\": 0}], \"jassTyp\": \"SCHIEBER_1000\", " +
				"\"player\": [{\"hand\": []}, {\"hand\": [\"SJ\"]}, {\"hand\": []}, {\"hand\": []}]}";

		Response response = target.path(path).path("game_info")
				.request()
				.post(getEntity(jsonString));

		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
		response.close();
	}

	@NotNull
	private Entity<JassRequest> getEntity(String jsonString) {
		JassRequest jassRequest = new Gson().fromJson(jsonString, JassRequest.class);
		return Entity.entity(jassRequest, MediaType.APPLICATION_JSON);
	}
}
