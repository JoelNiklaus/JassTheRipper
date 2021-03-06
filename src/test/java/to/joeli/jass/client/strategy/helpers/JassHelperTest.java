package to.joeli.jass.client.strategy.helpers;

import org.junit.Test;
import to.joeli.jass.client.game.Game;
import to.joeli.jass.game.Trumpf;
import to.joeli.jass.game.cards.Card;
import to.joeli.jass.game.cards.Color;
import to.joeli.jass.game.mode.Mode;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by dominikbriner on 20.05.17.
 */
public class JassHelperTest {

	private Set<Card> cards1 = EnumSet.of(Card.CLUB_ACE, Card.CLUB_EIGHT, Card.CLUB_JACK, Card.DIAMOND_SIX, Card.DIAMOND_SEVEN, Card.SPADE_QUEEN, Card.HEART_TEN, Card.SPADE_NINE, Card.SPADE_KING);


	@Test
	public void testCalculateInitialSafetyRespectingPlayedCards() {
		List<Card> sortedClubs = JassHelper.sortCardsOfColorDescending(EnumSet.of(Card.CLUB_TEN, Card.CLUB_NINE, Card.SPADE_QUEEN), Color.CLUBS);
		List<Card> playedClubs = JassHelper.sortCardsOfColorDescending(EnumSet.of(Card.CLUB_KING, Card.CLUB_QUEEN, Card.SPADE_QUEEN), Color.CLUBS);
		List<Card> sortedSpades = JassHelper.sortCardsOfColorDescending(cards1, Color.SPADES);
		List<Card> playedSpades = JassHelper.sortCardsOfColorDescending(EnumSet.of(Card.SPADE_ACE, Card.SPADE_NINE, Card.CLUB_QUEEN), Color.SPADES);
		assertEquals(1.0, JassHelper.calculateInitialSafetyObeabeRespectingPlayedCards(sortedSpades, playedSpades), 0.05);
		assertEquals(1f / 9, JassHelper.calculateInitialSafetyObeabeRespectingPlayedCards(sortedClubs, playedClubs), 0.05);
		List<Card> sortedClubsUndeUfe = JassHelper.sortCardsOfColorAscending(EnumSet.of(Card.CLUB_EIGHT, Card.CLUB_NINE, Card.SPADE_QUEEN), Color.CLUBS);
		List<Card> playedClubsUndeUfe = JassHelper.sortCardsOfColorAscending(EnumSet.of(Card.CLUB_SEVEN, Card.CLUB_QUEEN, Card.SPADE_QUEEN), Color.CLUBS);
		assertEquals(1f / 3, JassHelper.calculateInitialSafetyUndeUfeRespectingPlayedCards(sortedClubsUndeUfe, playedClubsUndeUfe), 0.05);
		List<Card> sortedClubsUndeUfe1 = JassHelper.sortCardsOfColorAscending(EnumSet.of(Card.CLUB_TEN, Card.CLUB_NINE, Card.SPADE_QUEEN), Color.CLUBS);
		List<Card> playedClubsUndeUfe1 = JassHelper.sortCardsOfColorAscending(EnumSet.of(Card.CLUB_SIX, Card.CLUB_QUEEN, Card.SPADE_KING), Color.CLUBS);
		assertEquals(1f / 9, JassHelper.calculateInitialSafetyUndeUfeRespectingPlayedCards(sortedClubsUndeUfe1, playedClubsUndeUfe1), 0.05);
		List<Card> sortedClubsUndeUfe2 = JassHelper.sortCardsOfColorAscending(EnumSet.of(Card.CLUB_TEN, Card.CLUB_EIGHT, Card.SPADE_QUEEN), Color.CLUBS);
		List<Card> playedClubsUndeUfe2 = JassHelper.sortCardsOfColorAscending(EnumSet.of(Card.CLUB_QUEEN, Card.SPADE_KING), Color.CLUBS);
		assertEquals(1f / 9, JassHelper.calculateInitialSafetyUndeUfeRespectingPlayedCards(sortedClubsUndeUfe2, playedClubsUndeUfe2), 0.05);
		assertEquals(TrumpfSelectionHelper.calculateInitialSafetyUndeUfe(sortedClubsUndeUfe2), JassHelper.calculateInitialSafetyUndeUfeRespectingPlayedCards(sortedClubsUndeUfe2, playedClubsUndeUfe2), 0.05);

	}

	@Test
	public void testRateUndeUfeWithAllClubsRemaining() {
		Set<Card> playerCards = EnumSet.of(Card.CLUB_TEN, Card.CLUB_NINE, Card.SPADE_QUEEN, Card.CLUB_KING, Card.CLUB_JACK);
		Set<Card> playerCardsOfColorClubs = JassHelper.getCardsOfColor(playerCards, Color.CLUBS);
		Set<Card> playedCards = EnumSet.of(Card.CLUB_ACE, Card.CLUB_QUEEN, Card.CLUB_EIGHT, Card.CLUB_SEVEN, Card.CLUB_SIX);
		// 180 is maximum amount of points
		assertEquals(20 * playerCardsOfColorClubs.size(), JassHelper.rateColorUndeUfeRespectingAlreadyPlayedCards(playerCards, playedCards, Color.CLUBS), 1);
		assertEquals(20 * playerCardsOfColorClubs.size(), JassHelper.rateColorObeAbeRespectingAlreadyPlayedCards(playerCards, playedCards, Color.CLUBS), 1);
		assertEquals(0, JassHelper.rateColorUndeUfeRespectingAlreadyPlayedCards(playerCards, playedCards, Color.DIAMONDS), 1);
		assertEquals(0, JassHelper.rateColorUndeUfeRespectingAlreadyPlayedCards(playerCards, playedCards, Color.SPADES), 1);
		assertEquals(0, JassHelper.rateColorUndeUfeRespectingAlreadyPlayedCards(playerCards, playedCards, Color.HEARTS), 1);
		assertEquals(0, JassHelper.rateColorUndeUfeRespectingAlreadyPlayedCards(playerCards, playedCards, Color.DIAMONDS), 1);
		assertEquals(0, JassHelper.rateColorUndeUfeRespectingAlreadyPlayedCards(playerCards, playedCards, Color.SPADES), 1);
		assertEquals(0, JassHelper.rateColorUndeUfeRespectingAlreadyPlayedCards(playerCards, playedCards, Color.HEARTS), 1);
	}

	@Test
	public void testNumberOfCardsBetween() {
		Set<Card> playedCards = EnumSet.of(Card.CLUB_ACE, Card.CLUB_QUEEN, Card.CLUB_TEN, Card.CLUB_SEVEN);
		List<Card> playedClubsDesc = JassHelper.sortCardsOfColorDescending(playedCards, Color.CLUBS);
		List<Card> playedClubsAsc = JassHelper.sortCardsOfColorAscending(playedCards, Color.CLUBS);
		assertEquals(0, JassHelper.calculateNumberOfCardsInbetweenObeAbeRespectingPlayedCards(Card.CLUB_JACK, Card.CLUB_NINE, playedClubsDesc));
		assertEquals(0, JassHelper.calculateNumberOfCardsInbetweenUndeUfeRespectingPlayedCards(Card.CLUB_NINE, Card.CLUB_JACK, playedClubsAsc));

		assertEquals(1, JassHelper.calculateNumberOfCardsInbetweenObeAbeRespectingPlayedCards(Card.CLUB_JACK, Card.CLUB_EIGHT, playedClubsDesc));
		assertEquals(2, JassHelper.calculateNumberOfCardsInbetweenUndeUfeRespectingPlayedCards(Card.CLUB_SIX, Card.CLUB_JACK, playedClubsAsc));
	}

	@Test
	public void testGetTrumps() {
		Set<Card> cards = EnumSet.of(Card.CLUB_SEVEN, Card.DIAMOND_JACK, Card.DIAMOND_NINE, Card.DIAMOND_KING);
		Mode trump = Mode.from(Trumpf.TRUMPF, Color.DIAMONDS);
		Set<Card> trumps = JassHelper.getTrumpfs(cards, trump);
		Set<Card> expectedTrumps = EnumSet.of(Card.DIAMOND_JACK, Card.DIAMOND_NINE, Card.DIAMOND_KING);
		assertEquals(expectedTrumps, trumps);
	}

	@Test
	public void testGetCardRank() {
		// Test that the Ace has Rank 9
		assertEquals(9, Card.CLUB_ACE.getRank());
	}

	@Test
	public void testGetBocksFirstRound() {
		final Game game = GameSessionBuilder.startedClubsGame();
		final Set<Card> bocks = JassHelper.getBocks(game);
		assertEquals(3, bocks.size());
		assertTrue(bocks.contains(Card.HEART_ACE));
		assertTrue(bocks.contains(Card.DIAMOND_ACE));
		assertTrue(bocks.contains(Card.SPADE_ACE));
	}

	@Test
	public void testGetBocksSixthRound() {
		final Game game = GameSessionBuilder.newSession().withStartedClubsGameWithRoundsPlayed(5).createGameSession().getCurrentGame();
		final Set<Card> bocks = JassHelper.getBocks(game);
		assertEquals(3, bocks.size());
		assertTrue(bocks.contains(Card.HEART_KING));
		assertTrue(bocks.contains(Card.DIAMOND_KING));
		assertTrue(bocks.contains(Card.SPADE_KING));
	}

	@Test
	public void testGetBocksTopDown() {
		final Game game = GameSessionBuilder.newSession().withStartedGame(Mode.topDown()).createGameSession().getCurrentGame();
		final Set<Card> bocks = JassHelper.getBocks(game);
		assertEquals(4, bocks.size());
		assertTrue(bocks.contains(Card.HEART_ACE));
		assertTrue(bocks.contains(Card.DIAMOND_ACE));
		assertTrue(bocks.contains(Card.SPADE_ACE));
		assertTrue(bocks.contains(Card.CLUB_ACE));
	}

	@Test
	public void testGetBocksBottomUp() {
		final Game game = GameSessionBuilder.newSession().withStartedGame(Mode.bottomUp()).createGameSession().getCurrentGame();
		final Set<Card> bocks = JassHelper.getBocks(game);
		assertEquals(4, bocks.size());
		assertTrue(bocks.contains(Card.HEART_SIX));
		assertTrue(bocks.contains(Card.DIAMOND_SIX));
		assertTrue(bocks.contains(Card.SPADE_SIX));
		assertTrue(bocks.contains(Card.CLUB_SIX));
	}
}