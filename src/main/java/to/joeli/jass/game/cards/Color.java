package to.joeli.jass.game.cards;

public enum Color {
	DIAMONDS("D", 0),
	HEARTS("H", 1),
	SPADES("S", 2),
	CLUBS("C", 3);

	private final String sign;
	private final int value;

	Color(String sign, int value) {
		this.sign = sign;
		this.value = value;
	}

	public static Color getColor(String sign) {
		switch (sign) {
			case "D":
				return DIAMONDS;
			case "H":
				return HEARTS;
			case "S":
				return SPADES;
			case "C":
				return CLUBS;
			default:
				return null;
		}
	}

	public static Color getColor(int value) {
		switch (value) {
			case 0:
				return DIAMONDS;
			case 1:
				return HEARTS;
			case 2:
				return SPADES;
			case 3:
				return CLUBS;
			default:
				return null;
		}
	}

	public int getValue() {
		return value;
	}

	@Override
	public String toString() {
		return sign;
	}
}
