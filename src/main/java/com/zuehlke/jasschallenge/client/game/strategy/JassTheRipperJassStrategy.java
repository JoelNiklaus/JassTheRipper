package com.zuehlke.jasschallenge.client.game.strategy;

import com.zuehlke.jasschallenge.client.game.Game;
import com.zuehlke.jasschallenge.client.game.GameSession;
import com.zuehlke.jasschallenge.client.game.Round;
import com.zuehlke.jasschallenge.game.Trumpf;
import com.zuehlke.jasschallenge.game.cards.Card;
import com.zuehlke.jasschallenge.game.cards.Color;
import com.zuehlke.jasschallenge.game.mode.Mode;
import weka.classifiers.functions.MultilayerPerceptron;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;


// TODO Only ML methods or include Jass Knowledge?
public class JassTheRipperJassStrategy implements JassStrategy {
    private final int max_schift_rating_val = 30;

	private MultilayerPerceptron mlp = new MultilayerPerceptron();

	// TODO hilfsmethoden bockVonJederFarbe, TruempfeNochImSpiel, statistisches Modell von möglichen Karten von jedem Spieler
	// TODO alle gespielten Karten merken

	// wähle trumpf mit besten voraussetzungen -> ranking
	// bei drei sicheren stichen -> obeabe oder undeufe
	//
	// wenn nicht gut -> schieben
	@Override
	public Mode chooseTrumpf(Set<Card> availableCards, GameSession session, boolean isGschobe) {
        System.out.println("ChooseTrumpf!");
        Stream<Card> cardStream = availableCards.stream();
		int max = 0;
        Mode prospectiveMode = Mode.from(Trumpf.TRUMPF, Color.CLUBS);
        for (Color color : Color.values()) {
            int colorTrumpRating = rate(cardStream, color);
            if (colorTrumpRating > max) {
                max = colorTrumpRating;
                prospectiveMode = Mode.from(Trumpf.TRUMPF, color);
            }
        }
        if (rateObeabe(cardStream) > max)
            prospectiveMode = Mode.topDown();
        if (rateUndeufe(cardStream) > max)
            prospectiveMode = Mode.bottomUp();
        if (max < max_schift_rating_val)
            return Mode.shift();
        return prospectiveMode;
	}

    private int rate(Stream<Card> cardStream, Color color) {
	    return 30;
    }

    private int rateObeabe(Stream<Card> cardStream) {
	    int sum = 0;
        for (Color color : Color.values()) {
            sum += rateObeabeColor(cardStream, color);
        }
	    return sum;
    }

    private int rateUndeufe(Stream<Card> cardStream) {
        int sum = 0;
        for (Color color : Color.values()) {
            sum += rateUndeufeColor(cardStream, color);
        }
        return sum;
    }

    /* Ass = 20, König = 12, Königin = 6, Bube = 3, Zehn = 1
     */
    private int rateObeabeColor(Stream<Card> cardStream, Color color) {
        Stream<Card> currentStream = getCardsOfColor(cardStream, color);
        currentStream.sorted(Comparator.comparing(Card::getRank));
        if (currentStream.findFirst().isPresent())
            if (currentStream.findFirst().get().getValue().getRank() >= 8)
                return 3000;
        return 5;
    }

    // Sorts the wrong way round
    private int rateUndeufeColor(Stream<Card> cardStream, Color color) {
        Stream<Card> currentStream = getCardsOfColor(cardStream, color);
        currentStream.sorted(Comparator.comparing(Card::getRank));
        if (currentStream.findFirst().isPresent())
            if (currentStream.findFirst().get().getValue().getRank() <= 2)
                return 300;
        return 5;
    }

    private Stream<Card> getCardsOfColor(Stream<Card> cardStream, Color color) {
        return cardStream.filter(card -> card.getColor().equals(color));
    }


    @Override
	public Card chooseCard(Set<Card> availableCards, GameSession session) {
		final Game currentGame = session.getCurrentGame();
		final Round round = currentGame.getCurrentRound();
		final Mode gameMode = round.getMode();

		return getPossibleCards(availableCards, round, gameMode)
				.findAny()
				.orElseThrow(() -> new RuntimeException("There should always be a card to play"));
	}

	private int countNumberOfCardsOfColor(Set<Card> availableCards, Color color) {
	    return (int) availableCards.stream().filter(card -> card.getColor().equals(color)).count();
    }

	private Stream<Card> getPossibleCards(Set<Card> availableCards, Round round, Mode gameMode) {
		return availableCards.stream().filter(card -> gameMode.canPlayCard(card, round.getPlayedCards(), round.getRoundColor(), availableCards));
	}

	// wenn letzter spieler und nicht möglich nicht mit trumpf zu stechen, dann stechen
	private void mitTrumpfAbstechen() {

	}

	// Wenn letzter Spieler und möglich mit nicht trumpf zu stechen, dann stechen.
	private void mitNichtTrumpfStechen() {

	}

	// Wenn obeabe oder undeufe: Bei Ausspielen von Partner tiefe Karte (tiefer als 10) von Gegenfarbe verwerfen wenn bei Farbe gut.
	private void gegenFarbeVerwerfen() {

	}

}
