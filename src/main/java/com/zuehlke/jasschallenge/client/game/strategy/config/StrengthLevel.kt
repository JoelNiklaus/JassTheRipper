package com.zuehlke.jasschallenge.client.game.strategy.config

/**
 * Describes the strength of the strategy. More configurations can be added when needed.
 * NOTE: When using a value estimator much fewer runs are needed than when using random playouts
 *
 *
 * IMPORTANT: maxThinkingTime (the maximal number of milliseconds per choose card move) has to
 * be tweaked in order not to exceed Timeout but still compute good moves
 *
 *
 * IMPORTANT: numDeterminizationsFactor is a hyperparameter that has to be tweaked
 * The MCTS creates (9-roundNumber) * numDeterminizationsFactor determinizations.
 * The higher this number, the more threads are spawned.
 *
 *
 * IMPORTANT: numRuns is a hyperparameter. Determines how many nodes should be explored in one mcts tree
 */
enum class StrengthLevel constructor(val numDeterminizationsFactor: Int, val maxThinkingTime: Long, val numRuns: Long) {
    //
    FAST_TEST(1, 50, 10),
    TEST(2, 100, 20),
    FAST(4, 200, 40),
    STRONG(6, 500, 100),
    POWERFUL(8, 1000, 200),
    EXTREME(12, 2000, 400),
    INSANE(16, 2500, 500),
    SUPERMAN(32, 5000, 1000),
    IRONMAN(32, 10000, 2000),
    TRUMPF(64, 10000, 2000),

    TEST_WEAK_TIME(1, 50, 10),
    TEST_STRONG_TIME(1, 250, 10),
    TEST_WEAK_NUM_DETERMINIZATIONS(1, 250, 10),
    TEST_STRONG_NUM_DETERMINIZATIONS(5, 250, 10);

    override fun toString(): String {
        return this.name + ": {" +
                "numDeterminizationsFactor=" + numDeterminizationsFactor +
                ", maxThinkingTime=" + maxThinkingTime +
                ", numRuns=" + numRuns +
                '}'.toString()
    }
}
