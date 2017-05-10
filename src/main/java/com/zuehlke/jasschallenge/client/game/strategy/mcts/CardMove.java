package com.zuehlke.jasschallenge.client.game.strategy.helpers;


import com.zuehlke.jasschallenge.client.game.Player;
import com.zuehlke.jasschallenge.client.game.strategy.mcts.Move;
import com.zuehlke.jasschallenge.game.cards.Card;

/**
 * Created by joelniklaus on 06.05.17.
 */
public class CardMove extends com.zuehlke.jasschallenge.client.game.Move implements Move {

	public CardMove(Player player, Card playedCard) {
		super(player, playedCard);
	}
}
