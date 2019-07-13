package to.joeli.jass.client.strategy.helpers;

import to.joeli.jass.client.game.*;
import to.joeli.jass.game.Trumpf;
import to.joeli.jass.game.cards.Card;
import to.joeli.jass.game.cards.Color;
import to.joeli.jass.game.mode.Mode;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;

public class CardKnowledgeBaseTest {

	private Set<Card> allCards = EnumSet.copyOf(Arrays.asList(Card.values()));
	private Set<Card> cards1 = EnumSet.of(Card.CLUB_EIGHT, Card.CLUB_JACK, Card.DIAMOND_SIX, Card.DIAMOND_SEVEN, Card.SPADE_QUEEN, Card.HEART_TEN, Card.SPADE_NINE, Card.SPADE_KING);
	private Set<Card> cards2 = EnumSet.of(Card.CLUB_KING, Card.CLUB_EIGHT, Card.CLUB_JACK, Card.DIAMOND_SIX, Card.DIAMOND_SEVEN, Card.SPADE_QUEEN, Card.HEART_TEN, Card.SPADE_NINE, Card.SPADE_KING);
	private Player player0 = new Player("0", "player0", 0);
	private Player player1 = new Player("1", "player1", 1);
	private Player player2 = new Player("2", "player2", 2);
	private Player player3 = new Player("3", "player3", 3);
	private PlayingOrder order = PlayingOrder.createOrder(asList(player0, player1, player2, player3));
	private Team Team0 = new Team("Team0", asList(player0, player2));
	private Team Team1 = new Team("Team1", asList(player1, player3));
	private Game diamondsGame = Game.startGame(Mode.from(Trumpf.TRUMPF, Color.DIAMONDS), order, asList(Team0, Team1), false);
	private Game obeAbeGame = Game.startGame(Mode.topDown(), order, asList(Team0, Team1), false);

	@Before
	public void setUp() {
		assertEquals(36, allCards.size());
	}

	@Test
	public void testFullyPlayedRound() {
		obeAbeGame.makeMove(new Move(player0, Card.CLUB_SIX));
		obeAbeGame.makeMove(new Move(player1, Card.CLUB_SEVEN));
		obeAbeGame.makeMove(new Move(player2, Card.HEART_EIGHT)); // player 2 did not follow suit
		obeAbeGame.makeMove(new Move(player3, Card.CLUB_KING));
		obeAbeGame.startNextRound();

		CardKnowledgeBase.sampleCardDeterminizationToPlayers(obeAbeGame, cards1, null);

		assertEquals(8, player0.getCards().size());
		assertEquals(8, player1.getCards().size());
		assertEquals(8, player2.getCards().size());
		assertEquals(8, player3.getCards().size());

		assertFalse(player2.getCards().contains(Card.CLUB_SIX));
		assertFalse(player2.getCards().contains(Card.CLUB_SEVEN));
		assertFalse(player2.getCards().contains(Card.CLUB_EIGHT));
		assertFalse(player2.getCards().contains(Card.CLUB_NINE));
		assertFalse(player2.getCards().contains(Card.CLUB_TEN));
		assertFalse(player2.getCards().contains(Card.CLUB_JACK));
		assertFalse(player2.getCards().contains(Card.CLUB_QUEEN));
		assertFalse(player2.getCards().contains(Card.CLUB_KING));
		assertFalse(player2.getCards().contains(Card.CLUB_ACE));
	}

	@Test
	public void testPartiallyPlayedRound() {
		diamondsGame.makeMove(new Move(player0, Card.CLUB_SIX));
		diamondsGame.makeMove(new Move(player1, Card.CLUB_SEVEN));
		diamondsGame.makeMove(new Move(player2, Card.HEART_EIGHT)); // player 2 did not follow suit

		CardKnowledgeBase.sampleCardDeterminizationToPlayers(diamondsGame, cards2, null);

		assertEquals(8, player0.getCards().size());
		assertEquals(8, player1.getCards().size());
		assertEquals(8, player2.getCards().size());
		assertEquals(9, player3.getCards().size());

		assertFalse(player2.getCards().contains(Card.CLUB_SIX));
		assertFalse(player2.getCards().contains(Card.CLUB_SEVEN));
		assertFalse(player2.getCards().contains(Card.CLUB_EIGHT));
		assertFalse(player2.getCards().contains(Card.CLUB_NINE));
		assertFalse(player2.getCards().contains(Card.CLUB_TEN));
		assertFalse(player2.getCards().contains(Card.CLUB_JACK));
		assertFalse(player2.getCards().contains(Card.CLUB_QUEEN));
		assertFalse(player2.getCards().contains(Card.CLUB_KING));
		assertFalse(player2.getCards().contains(Card.CLUB_ACE));
	}

}