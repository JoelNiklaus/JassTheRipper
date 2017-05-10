package com.zuehlke.jasschallenge.client.game.strategy.mcts;


import com.zuehlke.jasschallenge.client.game.Player;
import com.zuehlke.jasschallenge.client.game.strategy.mcts.src.Move;
import com.zuehlke.jasschallenge.game.cards.Card;

/**
 * Created by joelniklaus on 06.05.17.
 */
public class CardMove extends com.zuehlke.jasschallenge.client.game.Move implements Move {

	public CardMove(Player player, Card playedCard) {
		super(player, playedCard);
	}

	@Override
	public int compareTo(Move o) {
		CardMove other = (CardMove) o;
		return getPlayedCard().compareTo(other.getPlayedCard());
	}
}
