package com.zuehlke.jasschallenge.client.game.strategy;

import com.google.common.collect.Iterables;
import com.zuehlke.jasschallenge.client.game.*;
import com.zuehlke.jasschallenge.client.game.strategy.exceptions.MCTSException;
import com.zuehlke.jasschallenge.client.game.strategy.helpers.CardSelectionHelper;
import com.zuehlke.jasschallenge.client.game.strategy.helpers.MCTSHelper;
import com.zuehlke.jasschallenge.client.game.strategy.helpers.TrumpfSelectionHelper;
import com.zuehlke.jasschallenge.client.game.strategy.mcts.CardMove;
import com.zuehlke.jasschallenge.client.game.strategy.mcts.TrumpfMove;
import com.zuehlke.jasschallenge.client.game.strategy.mcts.src.Move;
import com.zuehlke.jasschallenge.game.cards.Card;
import com.zuehlke.jasschallenge.game.cards.Color;
import com.zuehlke.jasschallenge.game.mode.Mode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.*;


/**
 * The main class for the famous JassTheRipper Jass Strategy.
 * <p>
 * Some jass terms are introduced here for clarification:
 * Brettli: A card with value 0
 * Bock: The highest card of a color
 * Stechen: Play a card such that I win the round.
 * Anziehen: (Nur bei Trumpf) Being the starting player in the round playing a Brettli in order to show my partner that I am good at this color (have either King or Queen but not Ace)
 * Verwerfen: (Nur bei Obeabe oder Undeufe) If my partner wins the round play a Brettli of a color in order to show him/her that I am weak at that color
 * Schmieren: If my partner wins the round play a valuable card to gain many points.
 */
public class JassTheRipperJassStrategy extends RandomJassStrategy implements JassStrategy, Serializable {

//	TODO refinejass knowledge schauen austrumpfen
/*
NOTES FROM PLAY AGAINST JASS THE RIPPER 13/02/2018:

250ms bedenkzeit

Negativ:
Austrumpfen suboptimal
lässt mich lange gewähren obwohl er trümpfe hat
abstechen passiv



2500ms bedenkzeit

Negativ:
Gegner spielt auf mein könig, obwohl er noch die 7 hatte -> regel hinzufügen
Gegner trumpft aus obwohl nur noch sein Partner trümpfe hat!!!!!!
Gegner sticht 10er nicht ab als letzter spieler obwohl er noch 3 trümpfe hat!!!
Partner sticht als zweitletzter spieler gegnerisches ass mit trumpf 10 ab und wird danach überstochen!!
Partner schmiert dem Gegner, obwohl er noch ein Brettli hat!
Trumpft mit nell aus in dritter runde obwohl bauer noch im spiel ist!


Neutral:
Spielt undeufe reihe von 6-9 in spezieller reihenfolge (6 zuletzt)
Partner hat bei undeufe 6 zurückgehalten als gegner 7 gespielt hat

Positiv:
behält trumpf für letzten stich
hat einen sehr guten trumpf gut erkannt
Schmieren funktioniert
Mit 2500ms bedenkzeit austrumpfen besser
partner hat angezogen beim ausspielen
gegner antizipiert bauer des partners bei austrumpfen meines partners indem er ein ass schmiert (2. stich!)
Mehr simulationen scheinen hilfreich zu sein (spielt viel aggressiver)
Partner probiert auf match zu jassen
Gegner gehen auf die grossen punkte am schluss des spiels bei undeufe
Gegner spielt auf match (sticht brettli stich mit brettli ab, so dass ich als letzter spieler den stich nicht gratis holen kann)
Partner hat mit mir Schellen Match erfolgreich durchgezogen
*/

/*
POSSIBLE ENHANCEMENTS FROM MCTS SURVEY:
Tune parameters: exploration constant
Weighing simulation results
architecture similar to alphazero with neural net
*/



/*
Voices from the experiments:
Seems fake. Opponents are better than partner
Partnerbot hat gegner ein ass geschmiert obwohl er ein brettli hätte spielen können
Gegner hat es 10i vo mir (höchsti charte) als zweitletzte nid gno (9i hetter ge), obwohl er ass und könig gha hett
gegner hat trumpf als 3.-4. charte usgspilt obwohl niemer meh trumpf gha het (bzw het müesse ageh)

 */

	// IDEA: only one stateless jasstheripper computation container which provides an api to be called


	private Set<Color> partnerHatAngezogen = EnumSet.noneOf(Color.class);
	private Set<Color> partnerHatVerworfen = EnumSet.noneOf(Color.class);


	private MCTSHelper mctsHelper;

	private StrengthLevel strengthLevel = StrengthLevel.INSANE;

	// TODO MCTS still does not like to shift by itself. It is forced to shift now because of the rule-based pruning
	//  --> Investigate why MCTS without pruning does not like shifting
	private TrumpfSelectionMethod trumpfSelectionMethod = TrumpfSelectionMethod.MCTS;

	public static final Logger logger = LoggerFactory.getLogger(JassTheRipperJassStrategy.class);

	// TODO consider ForkJoinPool so we can also do leaf parallelisation or tree parallelisation
	// TODO implement cheating player as a benchmark: not very easily possible because we dont know the cards -> not planned at the moment
	// TODO find a way to visualize the MCTS tree
	// TODO hilfsmethoden bockVonJederFarbe, TruempfeNochImSpiel, statistisches Modell von möglichen Karten von jedem Spieler
	// TODO add exceptions to code!!!
	// TODO add tests!
	// TODO select function mcts anschauen, wie wird leaf node bestimmt?

	public JassTheRipperJassStrategy() {
	}

	public JassTheRipperJassStrategy(StrengthLevel strengthLevel) {
		this.strengthLevel = strengthLevel;
	}

	@Override
	public Mode chooseTrumpf(Set<Card> availableCards, GameSession session, boolean isGschobe) {
		try {
			final long startTime = System.currentTimeMillis();
			printCards(availableCards);

			Mode mode = TrumpfSelectionHelper.getRandomMode(isGschobe);

			if (trumpfSelectionMethod == TrumpfSelectionMethod.RULE_BASED)
				mode = TrumpfSelectionHelper.predictTrumpf(availableCards, isGschobe);

			if (trumpfSelectionMethod == TrumpfSelectionMethod.MCTS)
				try {
					assert mctsHelper != null;
					Move move = mctsHelper.predictMove(availableCards, session, true, isGschobe, StrengthLevel.TRUMPF);
					mode = ((TrumpfMove) move).getChosenTrumpf();
				} catch (MCTSException e) {
					logger.debug("{}", e);
					logger.error("Something went wrong. Had to choose random trumpf, damn it!");
				}

			final long endTime = System.currentTimeMillis() - startTime;
			logger.info("Total time for move: {}ms", endTime);
			logger.info("Chose Trumpf {}", mode);

			return mode;
		} catch (Exception e) {
			logger.debug("{}", e);
			logger.error("Something unexpectedly went terribly wrong! But could catch exception and chose random trumpf now.");
			return TrumpfSelectionHelper.getRandomMode(isGschobe);
		}
	}


	@Override
	public Card chooseCard(Set<Card> availableCards, GameSession session) {
		final long startTime = System.currentTimeMillis();
		long time = strengthLevel.getMaxThinkingTime();
		final long endingTime = startTime + time;
		printCards(availableCards);

		Card card = calculateCard(availableCards, session);

		// INFO: Even if there is only one card: wait for maxThinkingTime because opponents might detect patterns otherwise
		if (!session.getCurrentRound().isLastRound())
			waitUntilTimeIsUp(endingTime);

		final long endTime = System.currentTimeMillis() - startTime;
		logger.info("Total time for move: {}ms", endTime);
		return card;
	}

	private void waitUntilTimeIsUp(long endingTime) {
		while (System.currentTimeMillis() < endingTime) {
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				logger.debug("{}", e);
			}
		}
	}

	private Card calculateCard(Set<Card> availableCards, GameSession gameSession) {
		try {
			final Set<Card> possibleCards = CardSelectionHelper.getCardsPossibleToPlay(availableCards, gameSession.getCurrentGame());

			if (possibleCards.isEmpty())
				logger.error("We have a serious problem! No possible card to play!");

			if (possibleCards.size() == 1) {
				Card card = Iterables.getOnlyElement(possibleCards);
				logger.info("Only one possible card to play: {}", card);
				return card;
			}

			Card card = CardSelectionHelper.getRandomCard(possibleCards, gameSession.getCurrentGame());

			logger.info("Thinking now...");
			try {
				assert mctsHelper != null;
				final Card mctsCard = ((CardMove) mctsHelper.predictMove(availableCards, gameSession, false, false, strengthLevel)).getPlayedCard();
				if (possibleCards.contains(card)) {
					logger.info("Chose Card based on MCTS, Hurra!");
					card = mctsCard;
				} else
					logger.error("Card chosen not in possible cards. Had to choose random card, damn it!");
			} catch (MCTSException e) {
				logger.debug("{}", e);
				logger.error("Something went wrong. Had to choose random card, damn it!");
			}

			//final long endTime = System.currentTimeMillis() - startTime;
			//logger.info("Total time for move: {}ms", endTime);
			logger.info("Played {} out of possible Cards {} out of available Cards {}", card, possibleCards, availableCards);
			assert card != null;
			assert possibleCards.contains(card);
			return card;
		} catch (Exception e) {
			logger.error("Something unexpectedly went terribly wrong! But could catch exception and chose random card now.");
			logger.debug("{}", e);
			return CardSelectionHelper.getRandomCard(availableCards, gameSession.getCurrentGame());
		}
	}

	private void printCards(Set<Card> availableCards) {
		logger.info("Hi there! I am JassTheRipper, these are my cards: {} and this is my strength level: {}", availableCards, strengthLevel);
	}

	@Override
	public void onSessionFinished() {
		mctsHelper.shutDown();
	}

	@Override
	public void onSessionStarted(GameSession gameSession) {
		mctsHelper = new MCTSHelper(strengthLevel.getNumDeterminizationsFactor(), RunMode.RUNS);
	}

	@Override
	public String toString() {
		return "JassTheRipperJassStrategy{" +
				"strengthLevel=" + strengthLevel +
				'}';
	}
}
