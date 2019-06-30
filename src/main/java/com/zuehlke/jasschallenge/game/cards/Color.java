package com.zuehlke.jasschallenge.game.cards;

public enum Color {
	HEARTS("(H)", 0),
	DIAMONDS("(D)", 1),
	CLUBS("(C)", 2),
	SPADES("(S)", 3);


	private final String sign;
	private final int value;

	Color(String sign, int value) {
		this.sign = sign;
		this.value = value;
	}

	public static Color getColor(int value) {
		if (value < 0 || value > 3)
			return null;
		switch (value) {
			case 0:
				return HEARTS;
			case 1:
				return DIAMONDS;
			case 2:
				return CLUBS;
			case 3:
				return SPADES;
		}
		return null;
	}

	public int getValue() {
		return value;
	}

	@Override
	public String toString() {
		return sign;
	}
}
