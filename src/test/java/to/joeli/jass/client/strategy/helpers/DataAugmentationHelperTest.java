package to.joeli.jass.client.strategy.helpers;

import com.google.common.collect.Collections2;
import to.joeli.jass.client.game.Move;
import to.joeli.jass.client.game.Player;
import to.joeli.jass.game.cards.Card;
import to.joeli.jass.game.cards.Color;
import to.joeli.jass.game.mode.Mode;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

public class DataAugmentationHelperTest {

	@Test
	public void testGetRespectiveCard() {
		List<List<Color>> permutations = new ArrayList<>(Collections2.permutations(asList(Color.values())));
		assertEquals(Card.CLUB_ACE, DataAugmentationHelper.getRespectiveCard(Card.CLUB_ACE, permutations.get(0)));
		assertEquals(Card.SPADE_ACE, DataAugmentationHelper.getRespectiveCard(Card.CLUB_ACE, permutations.get(1)));
	}


	@Test
	public void testGetRespectiveMode() {
		List<List<Color>> permutations = new ArrayList<>(Collections2.permutations(asList(Color.values())));
		assertEquals(Mode.trump(Color.DIAMONDS), DataAugmentationHelper.getRespectiveMode(Mode.trump(Color.DIAMONDS), permutations.get(0)));
		assertEquals(Mode.trump(Color.DIAMONDS), DataAugmentationHelper.getRespectiveMode(Mode.trump(Color.DIAMONDS), permutations.get(1)));
		assertEquals(Mode.trump(Color.SPADES), DataAugmentationHelper.getRespectiveMode(Mode.trump(Color.DIAMONDS), permutations.get(2)));
		assertEquals(Mode.trump(Color.HEARTS), DataAugmentationHelper.getRespectiveMode(Mode.trump(Color.DIAMONDS), permutations.get(3)));
		assertEquals(Mode.trump(Color.HEARTS), DataAugmentationHelper.getRespectiveMode(Mode.trump(Color.DIAMONDS), permutations.get(4)));
	}

	@Test
	public void testGetRespectiveMovesListOrderDoesNotChange() {
		List<List<Color>> permutations = new ArrayList<>(Collections2.permutations(asList(Color.values())));
		List<Move> moves = new ArrayList<>();
		moves.add(new Move(new Player("Player1"), Card.DIAMOND_JACK));
		moves.add(new Move(new Player("Player2"), Card.CLUB_QUEEN));
		assertEquals(new Player("Player1"), DataAugmentationHelper.getRespectiveMoves(moves, permutations.get(0)).get(0).getPlayer());
		assertEquals(new Player("Player2"), DataAugmentationHelper.getRespectiveMoves(moves, permutations.get(0)).get(1).getPlayer());
	}
}
