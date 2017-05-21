package com.zuehlke.jasschallenge.game.cards;

import java.io.Serializable;

public enum Card implements Serializable {
	HEART_SIX(Color.HEARTS, CardValue.SIX),
	HEART_SEVEN(Color.HEARTS, CardValue.SEVEN),
	HEART_EIGHT(Color.HEARTS, CardValue.EIGHT),
	HEART_NINE(Color.HEARTS, CardValue.NINE),
	HEART_TEN(Color.HEARTS, CardValue.TEN),
	HEART_JACK(Color.HEARTS, CardValue.JACK),
	HEART_QUEEN(Color.HEARTS, CardValue.QUEEN),
	HEART_KING(Color.HEARTS, CardValue.KING),
	HEART_ACE(Color.HEARTS, CardValue.ACE),

	DIAMOND_SIX(Color.DIAMONDS, CardValue.SIX),
	DIAMOND_SEVEN(Color.DIAMONDS, CardValue.SEVEN),
	DIAMOND_EIGHT(Color.DIAMONDS, CardValue.EIGHT),
	DIAMOND_NINE(Color.DIAMONDS, CardValue.NINE),
	DIAMOND_TEN(Color.DIAMONDS, CardValue.TEN),
	DIAMOND_JACK(Color.DIAMONDS, CardValue.JACK),
	DIAMOND_QUEEN(Color.DIAMONDS, CardValue.QUEEN),
	DIAMOND_KING(Color.DIAMONDS, CardValue.KING),
	DIAMOND_ACE(Color.DIAMONDS, CardValue.ACE),

	CLUB_SIX(Color.CLUBS, CardValue.SIX),
	CLUB_SEVEN(Color.CLUBS, CardValue.SEVEN),
	CLUB_EIGHT(Color.CLUBS, CardValue.EIGHT),
	CLUB_NINE(Color.CLUBS, CardValue.NINE),
	CLUB_TEN(Color.CLUBS, CardValue.TEN),
	CLUB_JACK(Color.CLUBS, CardValue.JACK),
	CLUB_QUEEN(Color.CLUBS, CardValue.QUEEN),
	CLUB_KING(Color.CLUBS, CardValue.KING),
	CLUB_ACE(Color.CLUBS, CardValue.ACE),

	SPADE_SIX(Color.SPADES, CardValue.SIX),
	SPADE_SEVEN(Color.SPADES, CardValue.SEVEN),
	SPADE_EIGHT(Color.SPADES, CardValue.EIGHT),
	SPADE_NINE(Color.SPADES, CardValue.NINE),
	SPADE_TEN(Color.SPADES, CardValue.TEN),
	SPADE_JACK(Color.SPADES, CardValue.JACK),
	SPADE_QUEEN(Color.SPADES, CardValue.QUEEN),
	SPADE_KING(Color.SPADES, CardValue.KING),
	SPADE_ACE(Color.SPADES, CardValue.ACE);

	private final CardValue value;
	private final Color color;

	Card(Color color, CardValue value) {
		this.value = value;
		this.color = color;
	}

	public boolean hasSameColor(Card other) {
		return color.equals(other.getColor());
	}

	public boolean hasDifferentColor(Card other) {
		return !hasSameColor(other);
	}

	public Color getColor() {
		return color;
	}

	public CardValue getValue() {
		return value;
	}

	public boolean isHigherThan(Card other) {
		return color == other.color && value.getRank() > other.value.getRank();
	}

	public int getRank() {
		return this.getValue().getRank();
	}

	public int getTrumpfRank() {
		return this.getValue().getTrumpfRank();
	}

	public boolean isHigherTrumpfThan(Card other) {
		return value.getTrumpfRank() > other.getValue().getTrumpfRank();
	}

	@Override
	public String toString() {
		return value.toString() + color.toString();
	}
}
