package com.zuehlke.jasschallenge.client.game.strategy;

import com.rits.cloning.Cloner;
import com.zuehlke.jasschallenge.client.game.Game;
import com.zuehlke.jasschallenge.client.game.GameSession;
import com.zuehlke.jasschallenge.client.game.Player;
import com.zuehlke.jasschallenge.client.game.strategy.deepcopy.DeepCopy;
import com.zuehlke.jasschallenge.client.game.strategy.deepcopy.ObjectCloner;
import com.zuehlke.jasschallenge.game.Trumpf;
import com.zuehlke.jasschallenge.game.cards.Card;
import com.zuehlke.jasschallenge.game.cards.Color;
import com.zuehlke.jasschallenge.game.mode.Mode;
import org.apache.commons.lang3.SerializationUtils;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by joelniklaus on 06.05.17.
 */
public class DeepCopyTest {

	private static final int MIN_TIME = 20;
	private static final int NUMBER_OF_RUNS = 10000;

	@Test
	public void testFastestDeepCopySerializationReflection() {

		final GameSession gameSession = GameSessionBuilder.newSession()
				.withStartedGame(Mode.bottomUp())
				.createGameSession();

		Game game = gameSession.getCurrentGame();
		Player player = game.getCurrentPlayer();

		Object object = player;

		for (int i = 0; i < NUMBER_OF_RUNS; i++) {
			try {
				long startTime = System.currentTimeMillis();
				DeepCopy.copy(object);
				if (System.currentTimeMillis() - startTime > MIN_TIME)
					System.out.println("DeepCopy " + (System.currentTimeMillis() - startTime));

				startTime = System.currentTimeMillis();
				SerializationUtils.clone((Player) object);
				if (System.currentTimeMillis() - startTime > MIN_TIME)
					System.out.println("SerializationUtils " + (System.currentTimeMillis() - startTime));

				startTime = System.currentTimeMillis();
				ObjectCloner.deepCopySerialization(object);
				if (System.currentTimeMillis() - startTime > MIN_TIME)
					System.out.println("Objectcloner " + (System.currentTimeMillis() - startTime));

				startTime = System.currentTimeMillis();
				new Cloner().deepClone(object);
				if (System.currentTimeMillis() - startTime > MIN_TIME)
					System.out.println("Reflection " + (System.currentTimeMillis() - startTime));
			} catch (Exception e) {
			}
		}
	}

	@Test
	public void testCopyConstructorDeepCopy() {

		final GameSession gameSession = GameSessionBuilder.newSession()
				.withStartedGame(Mode.bottomUp())
				.createGameSession();

		Game originalGame = gameSession.getCurrentGame();

		Game newGame = new Game(originalGame);

		assertEquals(originalGame, newGame);
		assertEquals(originalGame.getAlreadyPlayedCards(), newGame.getAlreadyPlayedCards());
		assertEquals(originalGame.getCurrentPlayer(), newGame.getCurrentPlayer());
		assertEquals(originalGame.getCurrentRound(), newGame.getCurrentRound());
		assertEquals(originalGame.getMode(), newGame.getMode());
		assertEquals(originalGame.getCurrentRoundMode(), newGame.getCurrentRoundMode());
		assertEquals(originalGame.getOrder(), newGame.getOrder());
		assertEquals(originalGame.getResult(), newGame.getResult());
	}

}