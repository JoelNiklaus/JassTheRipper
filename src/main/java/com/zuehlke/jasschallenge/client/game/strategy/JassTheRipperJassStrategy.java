package com.zuehlke.jasschallenge.client.game.strategy;

import com.zuehlke.jasschallenge.client.game.*;
import com.zuehlke.jasschallenge.client.game.strategy.helpers.JassHelper;
import com.zuehlke.jasschallenge.client.game.strategy.helpers.MCTSHelper;
import com.zuehlke.jasschallenge.game.Trumpf;
import com.zuehlke.jasschallenge.game.cards.Card;
import com.zuehlke.jasschallenge.game.cards.Color;
import com.zuehlke.jasschallenge.game.mode.Mode;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;


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
Negativ:
Austrumpfen suboptimal (250ms bedenkzeit)
lässt mich lange gewähren obwohl er trümpfe hat (250ms bedenkzeit)
abstechen passiv (250ms bedenkzeit)
Gegner spielt auf mein könig, obwohl er noch die 7 hatte (2500ms) -> regel hinzufügen
Gegner trumpft aus obwohl nur noch sein Partner trümpfe hat!!!(2500ms)
Gegner sticht 10er nicht ab als letzter spieler obwohl er noch 3 trümpfe hat!!!(2500ms)
Partner sticht als zweitletzter spieler gegnerisches ass mit trumpf 10 ab und wird danach überstochen!! (2500ms)
Partner schmiert dem Gegner, obwohl er noch ein Brettli hat! (2500ms)


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

 */

	// IDEA: only one stateless jasstheripper computation container which provides an api to be called

	// TODO consider ForkJoinPool so we can also do leaf parallelisation or tree parallelisation

	// TODO make Strategy the owner of the threadpool so that it only has to be started once and not for every time we select a card! can save around 5ms on each card choosing

	// TODO implement cheating player as a benchmark: not very easily possible because we dont know the cards

	// TODO Focus on trumpf selection because card play is very good already! RL überlegen

	// TODO Greenkeeper

	// TODO Todos in gitlab issues importieren

	private Set<Color> partnerHatAngezogen = EnumSet.noneOf(Color.class);
	private Set<Color> partnerHatVerworfen = EnumSet.noneOf(Color.class);


	// IMPORTANT: If does not work properly, try setting this to false
	private static final boolean PARALLELISATION_ENABLED = true;

	// IMPORTANT: This value has to be tweaked in order not to exceed Timeout but still compute a good move
	// If we make to many then the thread overhead is too much. On the other hand not enough cannot guarantee a good prediction
	// 4 times the available processors seems too much (copy of game state takes too long)
	// If the Machine only has one core, we still need more than 2 determinizations, therefore fixed number.
	// Prime number so that ties are rare!
	// Possible options: 2 * Runtime.getRuntime().availableProcessors(), 7, 13
	public static final int NUMBER_OF_THREADS = 13;

	// IMPORTANT: This value has to be tweaked in order not to exceed Timeout but still compute good move
	// the maximal number of milliseconds per choose card move
	private static final int MAX_THINKING_TIME = 2500;

	// TODO: Maybe this is too high or too low? => Write tests.
	public static final int MAX_SHIFT_RATING_VAL = 75;


	// TODO Wo sollten die Exceptions gecatcht werden???
	// TODO hilfsmethoden bockVonJederFarbe, TruempfeNochImSpiel, statistisches Modell von möglichen Karten von jedem Spieler

	// wähle trumpf mit besten voraussetzungen -> ranking
	// bei drei sicheren stichen -> obeabe oder undeufe
	//
	// wenn nicht gut -> schieben
	@Override
	public Mode chooseTrumpf(Set<Card> availableCards, GameSession session, boolean isGschobe) {
		try {
			final long startTime = System.currentTimeMillis();
			printCards(availableCards);

			Mode mode = JassHelper.getRandomMode(isGschobe);

			mode = JassHelper.predictTrumpf(availableCards, mode, isGschobe);

			final long endTime = System.currentTimeMillis() - startTime;
			System.out.println("Total time for move: " + endTime + "ms");
			System.out.println("Chose Trumpf " + mode);

			return mode;
		} catch (Exception e) {
			System.out.println("Something unexpectedly went terribly wrong! But could catch exception and choose random trumpf now.");
			e.printStackTrace();
			return JassHelper.getRandomMode(isGschobe);
		}
	}


	@Override
	public Card chooseCard(Set<Card> availableCards, GameSession session) {
		try {
			final long startTime = System.currentTimeMillis();
			long time = MAX_THINKING_TIME;
			if (session.isFirstMove()) {
				time -= 50;
				// Reset for new round
				partnerHatAngezogen = EnumSet.noneOf(Color.class);
				partnerHatVerworfen = EnumSet.noneOf(Color.class);
			}
			final long endingTime = startTime + time;
			printCards(availableCards);
			final Game game = session.getCurrentGame();

			final Set<Card> possibleCards = JassHelper.getPossibleCards(availableCards, game);

			if (possibleCards.isEmpty())
				System.err.println("We have a serious problem! No possible card to play!");

			if (possibleCards.size() == 1)
				for (Card card : possibleCards) {
					System.out.println("Only one possible card to play: " + card + "\n\n");
					// INFO: Even if there is only one card: wait for MAX_THINKING_TIME because ottherwise, opponents may detect pattern and conclude that the bot only has one card left
					Thread.sleep(MAX_THINKING_TIME);
					return card;
				}

			Card card = JassHelper.getRandomCard(possibleCards, game);

			System.out.println("Thinking now...");
			try {
				final Card mctsCard = MCTSHelper.getCard(availableCards, game, endingTime, PARALLELISATION_ENABLED);
				if (possibleCards.contains(card)) {
					System.out.println("Chose Card based on MCTS, Hurra!");
					card = mctsCard;
				} else
					System.out.println("Card chosen not in possible cards. Had to choose random card, damn it!");
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("Something went wrong. Had to choose random card, damn it!");
			}

			final long endTime = System.currentTimeMillis() - startTime;
			System.out.println("Total time for move: " + endTime + "ms");
			System.out.println("Played " + card + " out of possible Cards " + possibleCards + " out of available Cards " + availableCards + "\n\n");
			assert card != null;
			assert possibleCards.contains(card);
			return card;
		} catch (Exception e) {
			System.out.println("Something unexpectedly went terribly wrong! But could catch exception and play random card now.");
			e.printStackTrace();
			return JassHelper.getRandomCard(availableCards, session.getCurrentGame());
		}
	}

	private void printCards(Set<Card> availableCards) {
		System.out.println("Hi there! I am JassTheRipper and these are my cards: " + availableCards);
	}
}
