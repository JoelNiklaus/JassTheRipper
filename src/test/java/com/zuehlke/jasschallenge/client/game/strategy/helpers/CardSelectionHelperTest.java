package com.zuehlke.jasschallenge.client.game.strategy.helpers;

import com.zuehlke.jasschallenge.client.game.*;
import com.zuehlke.jasschallenge.game.Trumpf;
import com.zuehlke.jasschallenge.game.cards.Card;
import com.zuehlke.jasschallenge.game.cards.Color;
import com.zuehlke.jasschallenge.game.mode.Mode;
import org.junit.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by dominikbriner on 20.05.17.
 */
public class CardSelectionHelperTest {

	private Player firstPlayer = new Player("0", "firstPlayer", 0);

	/* Test refineCardsWithJassKnowledge
	 * Test Stechen
	 * */
	@Test
	public void testRefineCardsWithJassKnowledgeStechen() {
		GameSession gameSession = GameSessionBuilder.newSession()
				.withStartedGame(Mode.topDown())
				.createGameSession();
		Game game = gameSession.getCurrentGame();
		PlayingOrder order = game.getCurrentRound().getPlayingOrder();
		gameSession.makeMove(new Move(order.getCurrentPlayer(), Card.CLUB_TEN));
		gameSession.makeMove(new Move(order.getCurrentPlayer(), Card.CLUB_JACK));
		gameSession.makeMove(new Move(order.getCurrentPlayer(), Card.CLUB_QUEEN));

		Set<Card> cards = EnumSet.of(Card.CLUB_SEVEN, Card.CLUB_KING);
		Set<Card> refinedCards = CardSelectionHelper.refineCardsWithJassKnowledge(cards, game);
		Set<Card> expectedCards = EnumSet.of(Card.CLUB_KING);
		assertEquals(expectedCards, refinedCards);
	}

	@Test
	public void testRefineCardsStechenWithoutTrumpfIfPossible() {
		GameSession gameSession = GameSessionBuilder.newSession()
				.withStartedGame(Mode.from(Trumpf.TRUMPF, Color.DIAMONDS))
				.createGameSession();
		Game game = gameSession.getCurrentGame();
		PlayingOrder order = game.getCurrentRound().getPlayingOrder();
		gameSession.makeMove(new Move(order.getCurrentPlayer(), Card.CLUB_TEN));
		gameSession.makeMove(new Move(order.getCurrentPlayer(), Card.CLUB_JACK));
		gameSession.makeMove(new Move(order.getCurrentPlayer(), Card.CLUB_QUEEN));

		Set<Card> cards = EnumSet.of(Card.CLUB_SEVEN, Card.CLUB_KING, Card.DIAMOND_JACK);
		Set<Card> refinedCards = CardSelectionHelper.refineCardsWithJassKnowledge(cards, game);
		Set<Card> expectedCards = EnumSet.of(Card.CLUB_KING);
		assertEquals(expectedCards, refinedCards);
	}

	@Test
	public void testRefineCardsStechenWithTrumpfIfNotPossibleOtherwiseAndScoreHigherThan10() {
		GameSession gameSession = GameSessionBuilder.newSession()
				.withStartedGame(Mode.from(Trumpf.TRUMPF, Color.DIAMONDS))
				.createGameSession();
		Game game = gameSession.getCurrentGame();
		PlayingOrder order = game.getCurrentRound().getPlayingOrder();
		gameSession.makeMove(new Move(order.getCurrentPlayer(), Card.CLUB_TEN));
		gameSession.makeMove(new Move(order.getCurrentPlayer(), Card.CLUB_JACK));
		gameSession.makeMove(new Move(order.getCurrentPlayer(), Card.CLUB_QUEEN));

		Set<Card> cards = EnumSet.of(Card.CLUB_SEVEN, Card.DIAMOND_JACK, Card.DIAMOND_NINE);
		Set<Card> refinedCards = CardSelectionHelper.refineCardsWithJassKnowledge(cards, game);
		Set<Card> expectedCards = EnumSet.of(Card.DIAMOND_JACK, Card.DIAMOND_NINE);
		assertEquals(expectedCards, refinedCards);
		assertTrue(game.getCurrentRound().calculateScore() > 10);
	}

	@Test
	public void testRefineCardsNotStechenWithTrumpfIfNotPossibleOtherwiseAndScoreLowerThan10() {
		GameSession gameSession = GameSessionBuilder.newSession()
				.withStartedGame(Mode.from(Trumpf.TRUMPF, Color.DIAMONDS))
				.createGameSession();
		Game game = gameSession.getCurrentGame();
		PlayingOrder order = game.getCurrentRound().getPlayingOrder();
		gameSession.makeMove(new Move(order.getCurrentPlayer(), Card.CLUB_NINE));
		gameSession.makeMove(new Move(order.getCurrentPlayer(), Card.CLUB_JACK));
		gameSession.makeMove(new Move(order.getCurrentPlayer(), Card.CLUB_QUEEN));

		Set<Card> cards = EnumSet.of(Card.CLUB_SEVEN, Card.DIAMOND_SIX, Card.DIAMOND_SEVEN);
		Set<Card> refinedCards = CardSelectionHelper.refineCardsWithJassKnowledge(cards, game);
		assertEquals(cards, refinedCards);
		assertTrue(game.getCurrentRound().calculateScore() < 10);
	}

	/* Test refineCardsWithJassKnowledge
	 * Test Austrumpfen
	 * */

	@Test
	public void testAlsoAustrumpfenWhenNotHavingHighTrumpfs() {
		GameSession gameSession = GameSessionBuilder.newSession()
				.withStartedGame(Mode.from(Trumpf.TRUMPF, Color.DIAMONDS))
				.createGameSession();
		Game game = gameSession.getCurrentGame();

		Set<Card> cards = EnumSet.of(Card.CLUB_SEVEN, Card.DIAMOND_SIX, Card.DIAMOND_SEVEN, Card.DIAMOND_EIGHT);
		Player player = firstPlayer;
		player.setCards(cards);
		Set<Card> expectedCards = EnumSet.of(Card.DIAMOND_SIX, Card.DIAMOND_SEVEN, Card.DIAMOND_EIGHT);
		Set<Card> refinedCards = CardSelectionHelper.refineCardsWithJassKnowledge(cards, game);
		assertEquals(expectedCards, refinedCards);
	}

	@Test
	public void testNotAustrumpfenHavingHighTrumpfs() {
		GameSession gameSession = GameSessionBuilder.newSession()
				.withStartedGame(Mode.from(Trumpf.TRUMPF, Color.DIAMONDS))
				.createGameSession();
		Game game = gameSession.getCurrentGame();
		PlayingOrder order = game.getCurrentRound().getPlayingOrder();

		Set<Card> cards = EnumSet.of(Card.CLUB_SEVEN, Card.DIAMOND_JACK, Card.DIAMOND_NINE, Card.DIAMOND_KING);
		Player player = firstPlayer;
		player.setCards(cards);
		Set<Card> refinedCards = CardSelectionHelper.refineCardsWithJassKnowledge(cards, game);
		Set<Card> expectedCards = EnumSet.of(Card.DIAMOND_JACK, Card.DIAMOND_NINE, Card.DIAMOND_KING);
		assertEquals(expectedCards, refinedCards);
	}
}