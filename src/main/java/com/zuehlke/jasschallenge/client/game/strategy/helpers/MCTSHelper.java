package com.zuehlke.jasschallenge.client.game.strategy.helpers;

import com.zuehlke.jasschallenge.client.game.Round;
import com.zuehlke.jasschallenge.client.game.strategy.exceptions.InvalidTrumpfException;
import com.zuehlke.jasschallenge.client.game.strategy.mcts.src.main.MCTS;
import com.zuehlke.jasschallenge.client.game.strategy.mcts.src.main.Move;
import com.zuehlke.jasschallenge.game.Trumpf;
import com.zuehlke.jasschallenge.game.cards.Card;
import com.zuehlke.jasschallenge.game.cards.Color;
import com.zuehlke.jasschallenge.game.mode.Mode;
import weka.classifiers.Classifier;
import weka.core.DenseInstance;
import weka.core.Instances;
import weka.core.converters.ConverterUtils;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by joelniklaus on 05.05.17.
 */
public class MCTSHelper {


	public static Card getCard(Set<Card> availableCards, Round round, Mode gameMode, Set<Card> alreadyPlayedCards) {
		MCTS player = new MCTS();
		player.setExplorationConstant(1.4);
		player.setOptimisticBias(0);
		player.setPessimisticBias(0);
		player.setTimeDisplay(true);

		Stich stich = new Stich(availableCards, round, gameMode, alreadyPlayedCards);

		Move move = player.runMCTS(stich, 10, false);
		return ((CardMove) move).getCard();
	}


}
