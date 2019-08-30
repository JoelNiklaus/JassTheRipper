package to.joeli.jass.game.cards;

public enum CardValue {

	SIX(1, 1, 0, 0, 11),
	SEVEN(2, 0),
	EIGHT(3, 8),
	NINE(4, 12, 0, 14, 0),
	TEN(5, 10),
	JACK(6, 13, 2, 20, 2),
	QUEEN(7, 3),
	KING(8, 4),
	ACE(9, 9, 11, 11, 0);

	private static final String[] NAMES = {"6", "7", "8", "9", "10", "J", "Q", "K", "A"};

	private final int rank;
	private final int trumpfRank;
	private final int score;
	private final int trumpfScore;
	private final int bottomUpScore;

	CardValue(int rank, int score) {
		this(rank, rank, score, score, score);
	}

	CardValue(int rank, int trumpfRank, int score, int trumpfScore, int bottomUpScore) {
		this.rank = rank;
		this.trumpfRank = trumpfRank;
		this.score = score;
		this.trumpfScore = trumpfScore;
		this.bottomUpScore = bottomUpScore;
	}

	public static CardValue getCardValue(String name) {
		switch (name) {
			case "6":
				return SIX;
			case "7":
				return SEVEN;
			case "8":
				return EIGHT;
			case "9":
				return NINE;
			case "10":
				return TEN;
			case "J":
				return JACK;
			case "Q":
				return QUEEN;
			case "K":
				return KING;
			case "A":
				return ACE;
			default:
				return null;
		}
	}

	public static CardValue getCardValue(int rank) {
		switch (rank) {
			case 1:
				return SIX;
			case 2:
				return SEVEN;
			case 3:
				return EIGHT;
			case 4:
				return NINE;
			case 5:
				return TEN;
			case 6:
				return JACK;
			case 7:
				return QUEEN;
			case 8:
				return KING;
			case 9:
				return ACE;
			default:
				return null;
		}
	}

	public int getScore() {
		return score;
	}

	public int getRank() {
		return rank;
	}

	public int getTrumpfRank() {
		return trumpfRank;
	}

	public int getTrumpfScore() {
		return trumpfScore;
	}

	public int getBottomUpScore() {
		return bottomUpScore;
	}

	@Override
	public String toString() {
		return NAMES[ordinal()];
	}
}
