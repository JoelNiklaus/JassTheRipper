package to.joeli.jass.client.game;

import to.joeli.jass.client.game.strategy.helpers.GameSessionBuilder;
import to.joeli.jass.game.cards.Card;
import to.joeli.jass.game.mode.Mode;
import to.joeli.jass.client.game.strategy.JassStrategy;
import org.junit.Test;
import org.mockito.Matchers;

import java.util.EnumSet;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PlayerTest {

    @Test
    public void chooseCard_strategyReturnsInvalidCard() {
        final GameSession gameSession = GameSessionBuilder
                .newSession()
                .withStartedGame(Mode.bottomUp())
                .withCardsPlayed(Card.CLUB_EIGHT)
                .createGameSession();

        final JassStrategy invalidPlayingStrategy = mock(JassStrategy.class);
        when(invalidPlayingStrategy.chooseCard(Matchers.<Set<Card>>any(), any(GameSession.class))).thenReturn(Card.HEART_EIGHT);
        Player player = new Player("test", invalidPlayingStrategy);
        player.setCards(EnumSet.of(Card.HEART_EIGHT, Card.CLUB_ACE));

        final Card card = player.makeMove(gameSession).getPlayedCard();

        assertThat(card, equalTo(Card.CLUB_ACE));
    }

}