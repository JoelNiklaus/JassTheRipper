package com.zuehlke.jasschallenge.client.game.strategy;

import com.zuehlke.jasschallenge.client.game.Game;
import com.zuehlke.jasschallenge.client.game.GameSession;
import com.zuehlke.jasschallenge.client.game.strategy.deepcopy.DeepCopy;
import com.zuehlke.jasschallenge.client.game.strategy.deepcopy.ObjectCloner;
import com.zuehlke.jasschallenge.game.Trumpf;
import com.zuehlke.jasschallenge.game.cards.Card;
import com.zuehlke.jasschallenge.game.cards.Color;
import com.zuehlke.jasschallenge.game.mode.Mode;
import org.apache.commons.lang3.SerializationUtils;
import org.junit.Test;

/**
 * Created by joelniklaus on 06.05.17.
 */
public class DeepCopyTest {


	@Test
	public void testFastestDeepCopy() {

		final GameSession gameSession = GameSessionBuilder.newSession()
				.withStartedGame(Mode.bottomUp())
				.createGameSession();

		Game game = gameSession.getCurrentGame();

		for (int i = 0; i < 1000; i++) {
			try {
				long startTime = System.currentTimeMillis();
				DeepCopy.copy(game);
				if (System.currentTimeMillis() - startTime > 50)
					System.out.println("DeepCopy " + (System.currentTimeMillis() - startTime));

				startTime = System.currentTimeMillis();
				SerializationUtils.clone(game);
				if (System.currentTimeMillis() - startTime > 50)
					System.out.println("SerializationUtils " + (System.currentTimeMillis() - startTime));

				startTime = System.currentTimeMillis();
				ObjectCloner.deepCopySerialization(game);
				if (System.currentTimeMillis() - startTime > 50)
					System.out.println("Objectcloner " + (System.currentTimeMillis() - startTime));
			} catch (Exception e) {
			}
		}
	}

}