package to.joeli.jass.client.strategy.benchmarks;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import to.joeli.jass.client.RemoteGame;
import to.joeli.jass.client.game.Player;
import to.joeli.jass.client.strategy.JassStrategy;
import to.joeli.jass.client.strategy.helpers.ShellScriptRunner;
import to.joeli.jass.messages.type.SessionType;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * To run these benchmarks, make sure to have the <a href="https://github.com/JoelNiklaus/jass-server) ">jass-server</a>
 * installed in a sibling directory of the root directory of this JassTheRipper Installation!
 * (rootdir/JassTheRipper and rootdir/jass-server)
 * This Benchmark uses sudo commands. Make sure you add this line to the file opened with sudo visudo
 * <pre>
 *      user                    python executable
 *  --> joelito ALL = NOPASSWD: /usr/local/bin/python3
 * </pre>
 */
public class BenchmarkRunner {

	private static final String LOCAL_URL = "ws://localhost:3000";

	private static final String RESULT_DIRECTORY = "benchmarks";

	/**
	 * Evaluates the result of the last modified file in the benchmarks directory of the jass server.
	 *
	 * @return
	 * @throws JSONException
	 */
	public static int evaluateResult() throws JSONException {
		String experimentsDirecory = "../jass-server/" + RESULT_DIRECTORY;
		File lastModifiedFile = lastFileModified(experimentsDirecory);

		String experimentResult = null;
		try {
			experimentResult = Files.asCharSource(lastModifiedFile, Charsets.UTF_8).read();
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			JSONArray array = new JSONArray(experimentResult);

			JSONObject broadcastGameFinished = array.getJSONObject(array.length() - 2);
			JSONArray gameResult = broadcastGameFinished.getJSONArray("data");
			int[] teamPoints = getTeamPoints(gameResult);
			int difference = teamPoints[0] - teamPoints[1];

			JSONObject broadcastTeams = array.getJSONObject(0);
			JSONArray teamsJsonArray = broadcastTeams.getJSONArray("data");


			System.out.println("Team 1 ("
					+ getPlayerOfTeam(teamsJsonArray, 0, 0) + " & " + getPlayerOfTeam(teamsJsonArray, 0, 1) +
					") (" + teamPoints[0] + " points) scored " + difference + " points more than Team 2 ("
					+ getPlayerOfTeam(teamsJsonArray, 1, 0) + " & " + getPlayerOfTeam(teamsJsonArray, 1, 1) +
					") (" + teamPoints[1] + " points).");

			return difference;

		} catch (JSONException e) {
			System.err.println("The read JSON file is in a corrupt state. The experiment probably did not finish well. Please rerun it!");
			throw e;
		}
	}

	/**
	 * Runs a tournament against two challenge bots and logs the results in a folder inside the jass server to be analyzed.
	 */
	public static void runBenchmark(JassStrategy jassStrategy, String botName, int tournamentRounds, int maxPoints, int seed, boolean challengeBotFirst) {
		final ArrayList<Process> processes = new ArrayList<>();

		int numThreads = 2;
		ExecutorService executorService = Executors.newFixedThreadPool(numThreads);
		List<Future<RemoteGame>> futures = new LinkedList<>();

		WebDriver driver = null;

		try {
			// jass-server must be in the same parent directory as the JassTheRipperProject

			String command = "env TOURNAMENT_LOGGING_DIR=" + RESULT_DIRECTORY
					+ " TOURNAMENT_ROUNDS=" + tournamentRounds
					+ " MAX_POINTS=" + maxPoints
					+ " DECK_SHUFFLE_SEED=" + seed
					+ " npm run start:tournament";
			ShellScriptRunner.INSTANCE.startShellProcessInThread(processes, "../jass-server", command);

			// INFO: if the bots fail to connect, it may be because the server has not yet started. Increase the time sleeping
			waitForStartup("Wait for jass server to start...", 7500);

			driver = new ChromeDriver();
			driver.get("localhost:3000");
			setUpTournament(driver);

			// NOTE: Because we have a random seed, we have to play each team in each position once to ensure a fair overall benchmark
			if (challengeBotFirst) {
				startChallengeBots(processes);
				startJassTheRipperBots(jassStrategy, botName, numThreads, executorService, futures);
			} else {
				startJassTheRipperBots(jassStrategy, botName, numThreads, executorService, futures);
				startChallengeBots(processes);
			}

			// INFO: if the tournament cannot start, it may be because the bots have not yet started. Increase the time sleeping
			startTournament(driver);

			futures.forEach(BenchmarkRunner::awaitFuture);
		} catch (Exception e) {

		} finally {
			executorService.shutdown();
			System.out.println("JassTheRipper bots successfully terminated.");

			try {
				ShellScriptRunner.INSTANCE.killProcess(processes.get(0), 15);
				System.out.println("Jass Server successfully terminated.");

				// Challenge bots will terminate automatically when the Jass Server is shutdown
				// But if they do not:
				if (!processes.isEmpty()) {
					// Try to kill them
					ShellScriptRunner.INSTANCE.killProcess(processes.get(1), 9);
					System.out.println("Challenge Bots successfully terminated.");
				}
			} catch (InterruptedException | IllegalStateException | IOException e) {
				e.printStackTrace();
			}

			if (driver != null) {
				driver.quit();
			}
		}
	}

	/**
	 * Note that bachelor-thesis-project must be in the same parent directory as the JassTheRipperProject
	 * Make sure that the necessary packages are installed (cd into respective folder and run 'pip3 install .')
	 *
	 * @param processes
	 */
	private static void startChallengeBots(ArrayList<Process> processes) {
		ShellScriptRunner.INSTANCE.startShellProcessInThread(processes, "../bachelor-thesis-project/src/remote_play", "sudo python3 play_as_challenge_bot.py");
		waitForStartup("Wait for the Challenge bots to start...", 1000);
	}

	private static void startJassTheRipperBots(JassStrategy jassStrategy, String botName, int numThreads, ExecutorService executorService, List<Future<RemoteGame>> futures) {
		for (int i = 0; i < numThreads; i++) {
			futures.add(executorService.submit(() -> startGame(LOCAL_URL, new Player(botName, jassStrategy), SessionType.TOURNAMENT)));
		}
		waitForStartup("Wait for the JassTheRipper bots to start...", 1000);
	}


	private static String getPlayerOfTeam(JSONArray teamsJsonArray, int teamId, int playerId) throws JSONException {
		return teamsJsonArray.getJSONObject(teamId).getJSONArray("players").getJSONObject(playerId).getString("name");
	}

	private static int[] getTeamPoints(JSONArray gameResult) throws JSONException {
		int[] teamPoints = new int[2];
		for (int i = 0; i < 2; i++) {
			JSONObject team = gameResult.getJSONObject(i);
			if (team.getString("name").equals("Team 1"))
				teamPoints[0] = team.getInt("points");
			if (team.getString("name").equals("Team 2"))
				teamPoints[1] = team.getInt("points");

		}
		return teamPoints;
	}

	/**
	 * Returns the file which last has been modified in the specified directory.
	 *
	 * @param dir
	 * @return
	 */
	private static File lastFileModified(String dir) {
		File dirFile = new File(dir);
		File[] files = dirFile.listFiles(File::isFile);
		long lastMod = Long.MIN_VALUE;
		File choice = null;
		for (File file : files) {
			if (file.lastModified() > lastMod) {
				choice = file;
				lastMod = file.lastModified();
			}
		}
		return choice;
	}

	private static void waitForStartup(String message, int sleepingTime) {
		try {
			System.out.println(message);
			Thread.sleep(sleepingTime);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private static void startTournament(WebDriver driver) {
		// Find the start tournament button by its name
		WebElement startTournament = driver.findElement(By.name("startTournament"));
		startTournament.click();
	}

	private static void setUpTournament(WebDriver driver) {
		// Find the text input requestPlayerName by its id and tag
		WebElement requestPlayerName = driver.findElement(By.id("requestPlayerName")).findElement(By.tagName("input"));
		requestPlayerName.sendKeys("TestPlayer");
		requestPlayerName.sendKeys(Keys.ENTER);

		// Find the text input requestPlayerName by its name
		WebElement createNewTournament = driver.findElement(By.name("createNewTournament"));
		createNewTournament.sendKeys("TestTournament");
		createNewTournament.sendKeys(Keys.ENTER);
	}


	private static <T> void awaitFuture(Future<T> future) {
		try {
			future.get();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
	}

	private static RemoteGame startGame(String targetUrl, Player myLocalPlayer, SessionType sessionType) {
		final RemoteGame remoteGame = new RemoteGame(targetUrl, myLocalPlayer, sessionType);
		remoteGame.start();
		return remoteGame;
	}
}
