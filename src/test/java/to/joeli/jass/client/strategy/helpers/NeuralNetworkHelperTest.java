package to.joeli.jass.client.strategy.helpers;

import to.joeli.jass.client.game.Game;
import to.joeli.jass.client.game.Move;
import to.joeli.jass.client.game.Player;
import to.joeli.jass.game.Trumpf;
import to.joeli.jass.game.cards.Card;
import to.joeli.jass.game.cards.Color;
import to.joeli.jass.game.mode.Mode;
import org.junit.Test;
import org.mockito.internal.util.reflection.Whitebox;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class NeuralNetworkHelperTest {

	private static final double DELTA = 0.001;


	@Test
	public void testToBinary() {
		assertArrayEquals(new int[]{0, 0, 0, 1, 1, 0, 0}, NeuralNetworkHelper.toBinary(12, 7));
		assertArrayEquals(new int[]{0, 0, 1, 1, 1}, NeuralNetworkHelper.toBinary(7, 5));
	}

	@Test
	public void testFromBinary() {
		assertEquals(12, NeuralNetworkHelper.fromBinary(new int[]{0, 0, 0, 1, 1, 0, 0}));
		assertEquals(7, NeuralNetworkHelper.fromBinary(new int[]{0, 0, 1, 1, 1}));
	}

	@Test
	public void testGetTrumpfBit() {
		assertEquals(1, NeuralNetworkHelper.getTrumpfBit(Card.CLUB_ACE, Mode.from(Trumpf.TRUMPF, Color.CLUBS)));
		assertEquals(0, NeuralNetworkHelper.getTrumpfBit(Card.CLUB_ACE, Mode.from(Trumpf.TRUMPF, Color.HEARTS)));
		assertEquals(0, NeuralNetworkHelper.getTrumpfBit(Card.CLUB_ACE, Mode.from(Trumpf.TRUMPF, Color.SPADES)));
		assertEquals(0, NeuralNetworkHelper.getTrumpfBit(Card.CLUB_ACE, Mode.from(Trumpf.TRUMPF, Color.DIAMONDS)));

		assertEquals(0, NeuralNetworkHelper.getTrumpfBit(Card.CLUB_ACE, Mode.shift()));
		assertEquals(0, NeuralNetworkHelper.getTrumpfBit(Card.CLUB_ACE, Mode.topDown()));
		assertEquals(1, NeuralNetworkHelper.getTrumpfBit(Card.CLUB_ACE, Mode.bottomUp()));
	}

	@Test
	public void testFromCardToEncoding() {
		assertArrayEquals(new double[]{0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0}, NeuralNetworkHelper.fromMoveToEncoding(Card.DIAMOND_JACK, Mode.trump(Color.CLUBS), 0), DELTA);
		assertArrayEquals(new double[]{0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0}, NeuralNetworkHelper.fromMoveToEncoding(Card.DIAMOND_JACK, Mode.trump(Color.HEARTS), 1), DELTA);
		assertArrayEquals(new double[]{0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0}, NeuralNetworkHelper.fromMoveToEncoding(Card.DIAMOND_JACK, Mode.trump(Color.SPADES), 2), DELTA);
		assertArrayEquals(new double[]{0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1}, NeuralNetworkHelper.fromMoveToEncoding(Card.DIAMOND_JACK, Mode.trump(Color.DIAMONDS), 3), DELTA);

		assertArrayEquals(new double[]{0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0}, NeuralNetworkHelper.fromMoveToEncoding(Card.DIAMOND_JACK, Mode.shift(), 0), DELTA);
		assertArrayEquals(new double[]{0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0}, NeuralNetworkHelper.fromMoveToEncoding(Card.DIAMOND_JACK, Mode.topDown(), 1), DELTA);
		assertArrayEquals(new double[]{0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 1, 0}, NeuralNetworkHelper.fromMoveToEncoding(Card.DIAMOND_JACK, Mode.bottomUp(), 2), DELTA);
	}

	@Test
	public void testFromEncodingToCard() {
		assertEquals(Card.DIAMOND_JACK, NeuralNetworkHelper.fromEncodingToCard(new double[]{0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0}));
		assertEquals(Card.CLUB_EIGHT, NeuralNetworkHelper.fromEncodingToCard(new double[]{0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0}));

		assertNull(NeuralNetworkHelper.fromEncodingToCard(new double[]{0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0}));
		assertNull(NeuralNetworkHelper.fromEncodingToCard(new double[]{0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}));
	}

	@Test
	public void testInfoRow() {
		Game clubsGame = GameSessionBuilder.startedClubsGame();
		final double[] infoRow = NeuralNetworkHelper.createInfoRow(clubsGame, clubsGame.getMode());
		System.out.println(Arrays.toString(infoRow));
		assertArrayEquals(new double[]{1, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0}, infoRow, DELTA);
	}

	@Test
	public void testFeaturesAreEmptyForAlreadyPlayedCards() {
		Game clubsGame = GameSessionBuilder.startedClubsGame();
		final double[][] features = NeuralNetworkHelper.getScoreFeatures(clubsGame);
		System.out.println(Arrays.deepToString(features));
		for (int i = 1; i < 37; i++) {
			assertArrayEquals(new double[18], features[i], DELTA);
		}
		assertEquals(73, features.length);
	}

	@Test
	public void testStartedFeaturesPlayedCardsShowFirstMove() {
		Game clubsGame = GameSessionBuilder.startedClubsGame();
		final Player player = clubsGame.getCurrentPlayer();
		final Move move = new Move(player, Card.CLUB_QUEEN);
		player.onMoveMade(move);
		clubsGame.makeMove(move);
		final double[][] features = NeuralNetworkHelper.getScoreFeatures(clubsGame);
		System.out.println(Arrays.deepToString(features));
		assertArrayEquals(new double[]{0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 1, 0, 0, 0}, features[1], DELTA);

	}

	@Test
	public void testObservationReconstruction() {
		Game clubsGame = GameSessionBuilder.startedClubsGame();
		final Player player = clubsGame.getCurrentPlayer();
		final Move move = new Move(player, Card.CLUB_QUEEN);
		player.onMoveMade(move);
		clubsGame.makeMove(move);
		System.out.println(Arrays.deepToString(NeuralNetworkHelper.getScoreFeatures(clubsGame)));

		final Map<String, List<Card>> reconstruction = NeuralNetworkHelper.reconstructObservation(NeuralNetworkHelper.getScoreFeatures(clubsGame));

		assertEquals(1, reconstruction.get("AlreadyPlayedCards").size());
		assertEquals(Card.CLUB_QUEEN, reconstruction.get("AlreadyPlayedCards").get(0));

		assertEquals(36, reconstruction.get("CardsDistribution").size());
	}

	@Test
	public void testAnalogousObservationsForTrumpfHas24Items() {
		final Game game = GameSessionBuilder.newSession().withStartedGame(Mode.trump(Color.CLUBS)).createGameSession().getCurrentGame();
		assertEquals(24, NeuralNetworkHelper.getAnalogousScoreFeatures(game).size());
	}

	@Test
	public void testAnalogousObservationsForNoTrumpfHas1Item() {
		final Game game = GameSessionBuilder.newSession().withStartedGame(Mode.bottomUp()).createGameSession().getCurrentGame();
		assertEquals(1, NeuralNetworkHelper.getAnalogousScoreFeatures(game).size());
	}

	@Test
	public void testObservationIs1022Long() {
		final Game game = GameSessionBuilder.startedClubsGame();
		final double[][] observation = NeuralNetworkHelper.getScoreFeatures(game);
		assertEquals(73, observation.length);
		assertEquals(18, observation[0].length);
	}

	@Test
	public void testObservationHasShiftedBit() {
		final Game game = GameSessionBuilder.startedClubsGame();
		Whitebox.setInternalState(game, "shifted", true);
		final double[][] observation = NeuralNetworkHelper.getScoreFeatures(game);
		assertEquals(0, observation[0][0], DELTA);
		assertEquals(1, observation[0][1], DELTA);
	}

}
