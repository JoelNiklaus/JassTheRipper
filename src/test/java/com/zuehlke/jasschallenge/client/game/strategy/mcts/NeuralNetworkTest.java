package com.zuehlke.jasschallenge.client.game.strategy.mcts;

import com.zuehlke.jasschallenge.client.game.Game;
import com.zuehlke.jasschallenge.client.game.Player;
import com.zuehlke.jasschallenge.client.game.PlayingOrder;
import com.zuehlke.jasschallenge.client.game.Team;
import com.zuehlke.jasschallenge.client.game.strategy.mcts.src.Move;
import com.zuehlke.jasschallenge.game.Trumpf;
import com.zuehlke.jasschallenge.game.cards.Card;
import com.zuehlke.jasschallenge.game.cards.Color;
import com.zuehlke.jasschallenge.game.mode.Mode;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class NeuralNetworkTest {

	private Set<Card> allCards = EnumSet.copyOf(Arrays.asList(Card.values()));
	private Set<Card> cards0 = EnumSet.of(Card.CLUB_QUEEN, Card.CLUB_ACE, Card.HEART_SIX, Card.HEART_JACK, Card.HEART_KING, Card.DIAMOND_SEVEN, Card.DIAMOND_QUEEN, Card.SPADE_TEN, Card.SPADE_KING);
	private Set<Card> cards1 = EnumSet.of(Card.CLUB_NINE, Card.CLUB_JACK, Card.HEART_EIGHT, Card.HEART_NINE, Card.DIAMOND_EIGHT, Card.DIAMOND_NINE, Card.DIAMOND_TEN, Card.SPADE_EIGHT, Card.SPADE_QUEEN);
	private Set<Card> cards2 = EnumSet.of(Card.CLUB_KING, Card.CLUB_EIGHT, Card.HEART_SEVEN, Card.HEART_QUEEN, Card.DIAMOND_JACK, Card.DIAMOND_KING, Card.SPADE_SEVEN, Card.SPADE_JACK, Card.SPADE_ACE);
	private Set<Card> cards3 = EnumSet.of(Card.CLUB_SIX, Card.CLUB_TEN, Card.CLUB_SEVEN, Card.HEART_TEN, Card.HEART_ACE, Card.DIAMOND_SIX, Card.DIAMOND_ACE, Card.SPADE_SIX, Card.SPADE_NINE);
	private Player player0 = new Player("0", "player0", 0);
	private Player player1 = new Player("1", "player1", 1);
	private Player player2 = new Player("2", "player2", 2);
	private Player player3 = new Player("3", "player3", 3);
	private PlayingOrder order = PlayingOrder.createOrder(asList(player0, player1, player2, player3));
	private Team Team0 = new Team("Team0", asList(player0, player2));
	private Team Team1 = new Team("Team1", asList(player1, player3));
	private Game obeAbeGame = Game.startGame(Mode.topDown(), order, asList(Team0, Team1), false);
	private Game diamondsGame = Game.startGame(Mode.from(Trumpf.TRUMPF, Color.DIAMONDS), order, asList(Team0, Team1), false);

	@Before
	public void setUp() {
		player0.setCards(cards0);
		player1.setCards(cards1);
		player2.setCards(cards2);
		player3.setCards(cards3);
	}

	@Test
	public void testToBinary() {
		assertArrayEquals(new int[]{0, 0, 0, 1, 1, 0, 0}, NeuralNetwork.toBinary(12, 7));
		assertArrayEquals(new int[]{0, 0, 1, 1, 1}, NeuralNetwork.toBinary(7, 5));
	}

	@Test
	public void testGetTrumpfBit() {
		assertEquals(1, NeuralNetwork.getTrumpfBit(Card.CLUB_ACE, Mode.from(Trumpf.TRUMPF, Color.CLUBS)));
		assertEquals(0, NeuralNetwork.getTrumpfBit(Card.CLUB_ACE, Mode.from(Trumpf.TRUMPF, Color.HEARTS)));
		assertEquals(0, NeuralNetwork.getTrumpfBit(Card.CLUB_ACE, Mode.from(Trumpf.TRUMPF, Color.SPADES)));
		assertEquals(0, NeuralNetwork.getTrumpfBit(Card.CLUB_ACE, Mode.from(Trumpf.TRUMPF, Color.DIAMONDS)));

		assertEquals(0, NeuralNetwork.getTrumpfBit(Card.CLUB_ACE, Mode.shift()));
		assertEquals(0, NeuralNetwork.getTrumpfBit(Card.CLUB_ACE, Mode.topDown()));
		assertEquals(1, NeuralNetwork.getTrumpfBit(Card.CLUB_ACE, Mode.bottomUp()));
	}

	@Test
	public void testFromCardToThreeHot() {
		assertArrayEquals(new int[]{0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0}, NeuralNetwork.fromCardToThreeHot(Card.DIAMOND_JACK, Mode.from(Trumpf.TRUMPF, Color.CLUBS)));
		assertArrayEquals(new int[]{0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0}, NeuralNetwork.fromCardToThreeHot(Card.DIAMOND_JACK, Mode.from(Trumpf.TRUMPF, Color.HEARTS)));
		assertArrayEquals(new int[]{0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0}, NeuralNetwork.fromCardToThreeHot(Card.DIAMOND_JACK, Mode.from(Trumpf.TRUMPF, Color.SPADES)));
		assertArrayEquals(new int[]{0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1}, NeuralNetwork.fromCardToThreeHot(Card.DIAMOND_JACK, Mode.from(Trumpf.TRUMPF, Color.DIAMONDS)));

		assertArrayEquals(new int[]{0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0}, NeuralNetwork.fromCardToThreeHot(Card.DIAMOND_JACK, Mode.shift()));
		assertArrayEquals(new int[]{0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0}, NeuralNetwork.fromCardToThreeHot(Card.DIAMOND_JACK, Mode.topDown()));
		assertArrayEquals(new int[]{0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1}, NeuralNetwork.fromCardToThreeHot(Card.DIAMOND_JACK, Mode.bottomUp()));
	}

}