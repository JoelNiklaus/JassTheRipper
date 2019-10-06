package to.joeli.jass.client.strategy.benchmarks;

import org.openjdk.jmh.annotations.*;
import to.joeli.jass.client.game.Game;
import to.joeli.jass.client.strategy.helpers.GameSessionBuilder;
import to.joeli.jass.client.strategy.mcts.HeavyJassPlayoutSelectionPolicy;
import to.joeli.jass.client.strategy.mcts.JassBoard;
import to.joeli.jass.client.strategy.mcts.LightJassPlayoutSelectionPolicy;
import to.joeli.jass.client.strategy.mcts.src.MCTS;

import java.util.concurrent.TimeUnit;


/**
 * INFO: Microbenchmarks seem to be very hard to write correctly because the compiler can make many enhancements we may be not aware of!
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 1, timeUnit = TimeUnit.NANOSECONDS)
@Measurement(iterations = 2, timeUnit = TimeUnit.NANOSECONDS)
public class PlayoutBenchmark {

	private JassBoard jassBoard;
	private final MCTS mcts = new MCTS();
	private final HeavyJassPlayoutSelectionPolicy heavyJassPlayoutSelectionPolicy = new HeavyJassPlayoutSelectionPolicy();
	private final LightJassPlayoutSelectionPolicy lightJassPlayoutSelectionPolicy = new LightJassPlayoutSelectionPolicy();

	@Setup(Level.Invocation)
	public void setUp() {
		Game game = GameSessionBuilder.startedClubsGame();
		jassBoard = JassBoard.constructCardSelectionJassBoard(game.getCurrentPlayer().getCards(), game, false, false, null, null);
	}

	@Benchmark
	@Fork(1)
	public void benchmarkSimulationSpeedRandomPlayout() {
		mcts.getRandomMove(jassBoard);
	}

	@Benchmark
	@Fork(1)
	public void benchmarkSimulationSpeedHeavyPlayout() {
		heavyJassPlayoutSelectionPolicy.getBestMove(jassBoard);
	}

	@Benchmark
	@Fork(1)
	public void benchmarkSimulationSpeedLightPlayout() {
		heavyJassPlayoutSelectionPolicy.getBestMove(jassBoard);
	}
}
