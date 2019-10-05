package to.joeli.jass.client.strategy.training;

import org.junit.Ignore;
import org.junit.Test;

import static to.joeli.jass.client.strategy.training.Arena.IMPROVEMENT_THRESHOLD_PERCENTAGE;

@Ignore("Do not run for normal tests. Takes way too much time")
public class ArenaTest {

	private Arena arena = new Arena(IMPROVEMENT_THRESHOLD_PERCENTAGE, Arena.SEED);

	@Test
	public void train() {
		arena.trainForNumEpisodes(1);
	}

	@Test
	public void trainUntilBetterThanRandomPlayouts() {
		arena.trainUntilBetterThanRandomPlayouts();
	}
}
