package com.zuehlke.jasschallenge.client.game.strategy;

import com.google.common.collect.Iterables;
import com.zuehlke.jasschallenge.client.game.*;
import com.zuehlke.jasschallenge.client.game.strategy.exceptions.MCTSException;
import com.zuehlke.jasschallenge.client.game.strategy.helpers.CardSelectionHelper;
import com.zuehlke.jasschallenge.client.game.strategy.helpers.MCTSHelper;
import com.zuehlke.jasschallenge.client.game.strategy.helpers.TrumpfSelectionHelper;
import com.zuehlke.jasschallenge.client.game.strategy.mcts.CardMove;
import com.zuehlke.jasschallenge.client.game.strategy.mcts.NeuralNetwork;
import com.zuehlke.jasschallenge.client.game.strategy.mcts.TrumpfMove;
import com.zuehlke.jasschallenge.client.game.strategy.mcts.src.Move;
import com.zuehlke.jasschallenge.game.cards.Card;
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

	private NeuralNetwork learningNetwork = new NeuralNetwork();
	private NeuralNetwork frozenNetwork = new NeuralNetwork(learningNetwork);

	private MCTSHelper mctsHelper;

	private StrengthLevel cardStrengthLevel = StrengthLevel.INSANE;
	private StrengthLevel trumpfStrengthLevel = StrengthLevel.TRUMPF;

	// TODO MCTS still does not like to shift by itself. It is forced to shift now because of the rule-based pruning
	//  --> Investigate why MCTS without pruning does not like shifting
	private TrumpfSelectionMethod trumpfSelectionMethod = TrumpfSelectionMethod.MCTS;


	// TODO consider ForkJoinPool so we can also do leaf parallelisation or tree parallelisation
	// TODO implement cheating player as a benchmark: not very easily possible because we dont know the cards -> not planned at the moment
	// TODO find a way to visualize the MCTS tree
	// TODO hilfsmethoden bockVonJederFarbe, TruempfeNochImSpiel, statistisches Modell von möglichen Karten von jedem Spieler
	// TODO add exceptions to code!!!
	// TODO add tests!
	// TODO select function mcts anschauen, wie wird leaf node bestimmt?

	private static JassTheRipperJassStrategy instance;

	public static final Logger logger = LoggerFactory.getLogger(JassTheRipperJassStrategy.class);

	private JassTheRipperJassStrategy() {
	}

	/**
	 * The JassStrategy is a singleton
	 *
	 * @return
	 */
	public static JassTheRipperJassStrategy getInstance() {
		if (JassTheRipperJassStrategy.instance == null) {
			JassTheRipperJassStrategy.instance = new JassTheRipperJassStrategy();
		}
		return JassTheRipperJassStrategy.instance;
	}

	/**
	 * IMPORTANT: Returns the first instance created -> if there was an instance created with a different parametrization before, that one is returned.
	 *
	 * @param cardStrengthLevel
	 * @return
	 */
	public static JassTheRipperJassStrategy getInstance(StrengthLevel cardStrengthLevel) {
		if (JassTheRipperJassStrategy.instance == null) {
			JassTheRipperJassStrategy.instance = new JassTheRipperJassStrategy();
			JassTheRipperJassStrategy.instance.cardStrengthLevel = cardStrengthLevel;
		}
		return JassTheRipperJassStrategy.instance;
	}

	/**
	 * IMPORTANT: Returns the first instance created -> if there was an instance created with a different parametrization before, that one is returned.
	 *
	 * @param cardStrengthLevel
	 * @param trumpfStrengthLevel
	 * @return
	 */
	public static JassTheRipperJassStrategy getInstance(StrengthLevel cardStrengthLevel, StrengthLevel trumpfStrengthLevel) {
		if (JassTheRipperJassStrategy.instance == null) {
			JassTheRipperJassStrategy.instance = new JassTheRipperJassStrategy();
			JassTheRipperJassStrategy.instance.cardStrengthLevel = cardStrengthLevel;
			JassTheRipperJassStrategy.instance.trumpfStrengthLevel = trumpfStrengthLevel;
		}
		return JassTheRipperJassStrategy.instance;
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
					Move move = mctsHelper.predictMove(availableCards, session, true, isGschobe, trumpfStrengthLevel);
					mode = ((TrumpfMove) move).getChosenTrumpf();
				} catch (MCTSException e) {
					logger.debug("{}", e);
					logger.error("Something went wrong. Had to choose random trumpf, damn it!");
				}

			logger.info("Total time for move: {}ms", System.currentTimeMillis() - startTime);
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
		final Game game = session.getCurrentGame();
		try {
			final long startTime = System.currentTimeMillis();
			printCards(availableCards);

			final Set<Card> possibleCards = CardSelectionHelper.getCardsPossibleToPlay(availableCards, game);
			Card card = CardSelectionHelper.getRandomCard(possibleCards, game);

			if (possibleCards.isEmpty())
				logger.error("We have a serious problem! No possible card to play!");

			if (possibleCards.size() == 1) {
				card = Iterables.getOnlyElement(possibleCards);
				logger.info("Only one possible card to play: {}", card);
			} else { // Start searching for a good card
				final Player currentPlayer = game.getCurrentPlayer();
				if (currentPlayer.isMctsEnabled()) {
					try {
						assert mctsHelper != null;
						Move move = mctsHelper.predictMove(availableCards, session, false, false, cardStrengthLevel);
						card = ((CardMove) move).getPlayedCard();
						logger.info("Chose Card based on MCTS, Hurra!");
					} catch (MCTSException e) {
						logger.debug("{}", e);
						logger.error("Something went wrong. Had to choose random card, damn it!");
					}
				} else { // Choose the network's prediction directly, without the mcts policy enhancement
					card = getNeuralNetwork(currentPlayer.isNetworkTrainable()).predictMove(game).getPlayedCard();
					logger.info("Chose card based only on value estimator network.");
				}
			}

			// INFO: Even if there is only one card: wait for maxThinkingTime because opponents might detect patterns otherwise
			// INFO: Commented out for machine only play. Only needed for humans
			//if (!session.getCurrentRound().isLastRound())
			//	waitUntilTimeIsUp(endingTime);

			logger.info("Total time for move: {}ms", System.currentTimeMillis() - startTime);
			logger.info("Chose card {} out of possible cards {} out of available cards {}", card, possibleCards, availableCards);
			return card;
		} catch (Exception e) {
			logger.debug("{}", e);
			logger.error("Something unexpectedly went terribly wrong! But could catch exception and chose random card now.");
			return CardSelectionHelper.getRandomCard(availableCards, game);
		}
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

	private void printCards(Set<Card> availableCards) {
		logger.info("Hi there! I am JassTheRipper, " +
				"these are my cards: {} " +
				"and this is my card strength level: {} " +
				"and this is my trumpf strength level: {}", availableCards, cardStrengthLevel, trumpfStrengthLevel);
	}

	public NeuralNetwork getNeuralNetwork(boolean trainable) {
		if (trainable)
			return this.learningNetwork;
		return this.frozenNetwork;
	}

	public void updateNetworks() {
		this.frozenNetwork = new NeuralNetwork(this.learningNetwork);
	}

	public StrengthLevel getCardStrengthLevel() {
		return cardStrengthLevel;
	}

	public void setCardStrengthLevel(StrengthLevel cardStrengthLevel) {
		this.cardStrengthLevel = cardStrengthLevel;
	}

	public StrengthLevel getTrumpfStrengthLevel() {
		return trumpfStrengthLevel;
	}

	public void setTrumpfStrengthLevel(StrengthLevel trumpfStrengthLevel) {
		this.trumpfStrengthLevel = trumpfStrengthLevel;
	}

	public TrumpfSelectionMethod getTrumpfSelectionMethod() {
		return trumpfSelectionMethod;
	}

	public void setTrumpfSelectionMethod(TrumpfSelectionMethod trumpfSelectionMethod) {
		this.trumpfSelectionMethod = trumpfSelectionMethod;
	}

	@Override
	public void onSessionFinished() {
		mctsHelper.shutDown();
	}

	@Override
	public void onSessionStarted(GameSession gameSession) {
		mctsHelper = new MCTSHelper(RunMode.TIME);
	}

	@Override
	public String toString() {
		return "JassTheRipperJassStrategy{" +
				"cardStrengthLevel=" + cardStrengthLevel +
				"trumpfStrengthLevel=" + trumpfStrengthLevel +
				'}';
	}
}
