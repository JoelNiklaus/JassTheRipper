package com.zuehlke.jasschallenge.client.game.strategy.helpers;


import com.zuehlke.jasschallenge.client.game.strategy.mcts.src.main.Move;
import com.zuehlke.jasschallenge.game.cards.Card;

/**
 * Created by joelniklaus on 06.05.17.
 */
public class CardMove implements Move {
	private Card card;

	public CardMove(Card card) {
		this.card = card;
	}

	public Card getCard() {
		return card;
	}
}
