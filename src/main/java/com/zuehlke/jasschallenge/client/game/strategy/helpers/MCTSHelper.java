package com.zuehlke.jasschallenge.client.game.strategy.helpers;

import com.zuehlke.jasschallenge.client.game.Game;
import com.zuehlke.jasschallenge.client.game.GameSession;
import com.zuehlke.jasschallenge.client.game.strategy.mcts.src.main.MCTS;
import com.zuehlke.jasschallenge.client.game.strategy.mcts.src.main.Move;
import com.zuehlke.jasschallenge.game.cards.Card;
import org.apache.commons.lang3.SerializationUtils;
import org.javatuples.Pair;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by joelniklaus on 05.05.17.
 */
public class MCTSHelper {

	public static Card getCard(Set<Card> availableCards, Game game) throws Exception {
		MCTS mcts = new MCTS();
		mcts.setExplorationConstant(1.4);
		mcts.setOptimisticBias(0);
		mcts.setPessimisticBias(0);
		mcts.setTimeDisplay(true);


		int maxComputationTime = 440;
		int numberOfMCTSRuns = 4;

		int timePerRun = maxComputationTime / numberOfMCTSRuns;

		HashMap<Card, Integer> numberOfSelections = new HashMap<>();
		for (int i = 0; i < numberOfMCTSRuns; i++) {
			Card card = predictCard(availableCards, game, mcts, timePerRun);
			int number = 1;
			if (numberOfSelections.containsKey(card)) {
				number += numberOfSelections.get(card);
			}
			numberOfSelections.put(card, number);
		}
		Card card = numberOfSelections.entrySet().stream().sorted(Map.Entry.<Card, Integer>comparingByValue()).findFirst().get().getKey();
		return card;
	}

	private static Card predictCard(Set<Card> availableCards, Game game, MCTS mcts, int time) throws Exception {
		long startTime = System.nanoTime();
		JassBoard jass;
		try {
			jass = JassBoard.jassFactory(availableCards, SerializationUtils.clone(game));
		} catch (Exception e) {
			System.err.println("Could not clone session or cards");
			e.printStackTrace();
			throw (e);
		}
		long cloningTime = (System.nanoTime() - startTime) / 1000000;
		System.out.println("Cloning time: " + cloningTime + "ms");
		return ((CardMove) mcts.runMCTS(jass, false, time)).getPlayedCard();
	}

}
