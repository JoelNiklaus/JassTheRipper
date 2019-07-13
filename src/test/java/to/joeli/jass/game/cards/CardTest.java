package to.joeli.jass.game.cards;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CardTest {

    @Test
    public void allCards_containsEveryCard() {
        assertEquals(36, Card.values().length);
    }

    @Test
    public void isHigherTrumpfThan() {

        assertTrue(Card.HEART_SEVEN.isHigherTrumpfThan(Card.HEART_SIX));
    }

    @Test
    public void isHigherThan() {

        assertTrue(Card.HEART_SEVEN.isHigherThan(Card.HEART_SIX));
    }

    @Test
    public void isHigherThan_cardsWithDifferentColor() {

        assertFalse(Card.HEART_SEVEN.isHigherThan(Card.CLUB_SIX));
    }
}
