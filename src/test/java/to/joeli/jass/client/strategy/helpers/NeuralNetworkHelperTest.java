package to.joeli.jass.client.strategy.helpers;

import org.junit.Test;
import org.mockito.internal.util.reflection.Whitebox;
import to.joeli.jass.client.game.Game;
import to.joeli.jass.client.game.GameSession;
import to.joeli.jass.client.game.Move;
import to.joeli.jass.client.game.Player;
import to.joeli.jass.game.Trumpf;
import to.joeli.jass.game.cards.Card;
import to.joeli.jass.game.cards.Color;
import to.joeli.jass.game.mode.Mode;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class NeuralNetworkHelperTest {

	private static final float DELTA = 0.001f;


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
		assertArrayEquals(new float[]{0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0}, NeuralNetworkHelper.fromMoveToEncoding(Card.DIAMOND_JACK, Mode.trump(Color.CLUBS), 0), DELTA);
		assertArrayEquals(new float[]{0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0}, NeuralNetworkHelper.fromMoveToEncoding(Card.DIAMOND_JACK, Mode.trump(Color.HEARTS), 1), DELTA);
		assertArrayEquals(new float[]{0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0}, NeuralNetworkHelper.fromMoveToEncoding(Card.DIAMOND_JACK, Mode.trump(Color.SPADES), 2), DELTA);
		assertArrayEquals(new float[]{0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1}, NeuralNetworkHelper.fromMoveToEncoding(Card.DIAMOND_JACK, Mode.trump(Color.DIAMONDS), 3), DELTA);

		assertArrayEquals(new float[]{0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0}, NeuralNetworkHelper.fromMoveToEncoding(Card.DIAMOND_JACK, Mode.shift(), 0), DELTA);
		assertArrayEquals(new float[]{0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0}, NeuralNetworkHelper.fromMoveToEncoding(Card.DIAMOND_JACK, Mode.topDown(), 1), DELTA);
		assertArrayEquals(new float[]{0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 1, 0}, NeuralNetworkHelper.fromMoveToEncoding(Card.DIAMOND_JACK, Mode.bottomUp(), 2), DELTA);
	}

	@Test
	public void testFromEncodingToCard() {
		assertEquals(Card.DIAMOND_JACK, NeuralNetworkHelper.fromEncodingToCard(new float[]{0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0}));
		assertEquals(Card.CLUB_EIGHT, NeuralNetworkHelper.fromEncodingToCard(new float[]{0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0}));

		assertNull(NeuralNetworkHelper.fromEncodingToCard(new float[]{0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0}));
		assertNull(NeuralNetworkHelper.fromEncodingToCard(new float[]{0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}));
	}

	@Test
	public void testInfoRow() {
		Game clubsGame = GameSessionBuilder.startedClubsGame();
		final float[] infoRow = NeuralNetworkHelper.createInfoRow(clubsGame, clubsGame.getMode());
		System.out.println(Arrays.toString(infoRow));
		assertArrayEquals(new float[]{1, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0}, infoRow, DELTA);
	}

	@Test
	public void testFeaturesAreEmptyForAlreadyPlayedCards() {
		Game clubsGame = GameSessionBuilder.startedClubsGame();
		final float[][] features = NeuralNetworkHelper.getScoreFeatures(clubsGame);
		System.out.println(Arrays.deepToString(features));
		for (int i = 1; i < 37; i++) {
			assertArrayEquals(new float[18], features[i], DELTA);
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
		final float[][] features = NeuralNetworkHelper.getScoreFeatures(clubsGame);
		System.out.println(Arrays.deepToString(features));
		assertArrayEquals(new float[]{0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 1, 0, 0, 0}, features[1], DELTA);

	}

	@Test
	public void testFeatureReconstruction() {
		Game clubsGame = GameSessionBuilder.startedClubsGame();
		final Player player = clubsGame.getCurrentPlayer();
		final Move move = new Move(player, Card.CLUB_QUEEN);
		player.onMoveMade(move);
		clubsGame.makeMove(move);
		System.out.println(Arrays.deepToString(NeuralNetworkHelper.getScoreFeatures(clubsGame)));

		final Map<String, List<Card>> reconstruction = NeuralNetworkHelper.reconstructFeatures(NeuralNetworkHelper.getScoreFeatures(clubsGame));

		assertEquals(1, reconstruction.get("AlreadyPlayedCards").size());
		assertEquals(Card.CLUB_QUEEN, reconstruction.get("AlreadyPlayedCards").get(0));

		assertEquals(36, reconstruction.get("CardsDistribution").size());
	}

	@Test
	public void testAnalogousScoreFeaturesForTrumpfHas24Items() {
		final Game game = GameSessionBuilder.startedClubsGame();
		assertEquals(24, NeuralNetworkHelper.getAnalogousScoreFeatures(game).size());
	}

	@Test
	public void testAnalogousScoreFeaturesForNoTrumpfHas1Item() {
		final Game game = GameSessionBuilder.newSession().withStartedGame(Mode.bottomUp()).createGameSession().getCurrentGame();
		assertEquals(1, NeuralNetworkHelper.getAnalogousScoreFeatures(game).size());
	}

	@Test
	public void testAnalogousCardsFeatures() {
		final Game game = GameSessionBuilder.startedClubsGame();
		final Map<Card, Distribution> cardKnowledge = CardKnowledgeBase.initCardKnowledge(game, game.getCurrentPlayer().getCards());
		final List<float[][]> analogousCardsFeatures = NeuralNetworkHelper.getAnalogousCardsFeatures(game, cardKnowledge);

		assertEquals(24, analogousCardsFeatures.size());
		assertArrayEquals(NeuralNetworkHelper.getCardsFeatures(game, cardKnowledge), analogousCardsFeatures.get(0));
		assertNotEquals(NeuralNetworkHelper.getCardsFeatures(game, cardKnowledge), analogousCardsFeatures.get(1));

	}

	@Test
	public void testScoreFeaturesDimension() {
		final Game game = GameSessionBuilder.startedClubsGame();
		final float[][] scoreFeatures = NeuralNetworkHelper.getScoreFeatures(game);
		assertEquals(73, scoreFeatures.length);
		assertEquals(18, scoreFeatures[0].length);
	}

	@Test
	public void testScoreFeaturesHasShiftedBit() {
		final Game game = GameSessionBuilder.startedClubsGame();
		Whitebox.setInternalState(game, "shifted", true);
		final float[][] scoreFeatures = NeuralNetworkHelper.getScoreFeatures(game);
		assertEquals(0, scoreFeatures[0][0], DELTA);
		assertEquals(1, scoreFeatures[0][1], DELTA);
	}

	@Test
	public void testGetCardsTarget() {
		final Game game = GameSessionBuilder.startedClubsGame();

		final int[][] cardsTargets = NeuralNetworkHelper.getCardsTarget(game);
		System.out.println(Arrays.deepToString(cardsTargets));

		assertArrayEquals(new int[]{1, 0, 0, 0}, cardsTargets[0]); // First player has HEART_SIX
		assertArrayEquals(new int[]{0, 0, 1, 0}, cardsTargets[1]); // Third player has HEART_SEVEN
		assertArrayEquals(new int[]{0, 1, 0, 0}, cardsTargets[2]); // Second player has HEART_EIGHT
	}

	@Test
	public void testGetScoreTarget() {
		final GameSession gameSession = GameSessionBuilder.newSession().withStartedClubsGameWithRoundsPlayed(9).createGameSession();
		final Game game = gameSession.getCurrentGame();

		final Player playerTeam1 = gameSession.getTeams().get(0).getPlayers().get(0);
		final Player playerTeam2 = gameSession.getTeams().get(1).getPlayers().get(0);

		final float scoreTargetTeam1 = NeuralNetworkHelper.getScoreTarget(game, playerTeam1);
		final float scoreTargetTeam2 = NeuralNetworkHelper.getScoreTarget(game, playerTeam2);

		assertEquals(game.getResult().getTeamScore(playerTeam1), scoreTargetTeam1, DELTA);
		assertEquals(game.getResult().getTeamScore(playerTeam2), scoreTargetTeam2, DELTA);
	}


}
