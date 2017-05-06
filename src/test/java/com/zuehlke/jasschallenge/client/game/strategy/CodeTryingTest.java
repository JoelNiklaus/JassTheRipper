package com.zuehlke.jasschallenge.client.game.strategy;

import com.zuehlke.jasschallenge.client.game.strategy.JassStrategy;
import com.zuehlke.jasschallenge.game.Trumpf;
import com.zuehlke.jasschallenge.game.cards.Card;
import com.zuehlke.jasschallenge.game.cards.CardValue;
import com.zuehlke.jasschallenge.game.cards.Color;
import com.zuehlke.jasschallenge.game.mode.Mode;
import org.junit.Test;
import org.mockito.Matchers;

import java.util.EnumSet;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by joelniklaus on 06.05.17.
 */
public class CodeTryingTest {

	@Test
	public void testAnything() {
		System.out.println(Card.CLUB_JACK);
		System.out.println(Card.CLUB_SEVEN);
		System.out.println(Card.HEART_TEN);

		System.out.println(Mode.from(Trumpf.TRUMPF, Color.CLUBS));
		System.out.println(Mode.bottomUp());
		System.out.println(Mode.topDown());
		System.out.println(Mode.shift().toString());
	}

}