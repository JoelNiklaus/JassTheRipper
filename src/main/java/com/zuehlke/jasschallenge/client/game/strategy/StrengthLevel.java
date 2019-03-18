package com.zuehlke.jasschallenge.client.game.strategy;

/**
 * Describes the strength of the strategy. More configurations can be added when needed.
 */
public enum StrengthLevel {
	FAST_TEST(3, 50),
	TEST(3, 100),
	FAST(5, 200),
	STRONG(7, 500),
	POWERFUL(7, 1000),
	EXTREME(11, 2000),
	INSANE(13, 2500),
	SUPERMAN(17, 5000),
	;

	// IMPORTANT: This value has to be tweaked in order not to exceed Timeout but still compute a good move
	// If we make to many then the thread overhead is too much. On the other hand not enough cannot guarantee a good prediction
	// 4 times the available processors seems too much (copy of game state takes too long)
	// If the Machine only has one core, we still need more than 2 determinizations, therefore fixed number.
	// Prime number so that ties are rare!
	// Possible options: 2 * Runtime.getRuntime().availableProcessors(), 7, 13
	// NOTE that a number higher than 1 means that the MCTS is run parallelised! If does not work properly, try setting it to 1
	private final int numThreads;

	// IMPORTANT: This value has to be tweaked in order not to exceed Timeout but still compute good move
	// the maximal number of milliseconds per choose card move
	private final int maxThinkingTime;

	StrengthLevel(int numThreads, int maxThinkingTime) {
		this.numThreads = numThreads;
		this.maxThinkingTime = maxThinkingTime;
	}

	public int getNumThreads() {
		return this.numThreads;
	}

	public int getMaxThinkingTime() {
		return this.maxThinkingTime;
	}
}
