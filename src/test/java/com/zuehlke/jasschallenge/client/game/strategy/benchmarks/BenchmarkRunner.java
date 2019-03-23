package com.zuehlke.jasschallenge.client.game.strategy.benchmarks;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.zuehlke.jasschallenge.client.RemoteGame;
import com.zuehlke.jasschallenge.client.game.Player;
import com.zuehlke.jasschallenge.client.game.strategy.JassStrategy;
import com.zuehlke.jasschallenge.client.game.strategy.JassTheRipperJassStrategy;
import com.zuehlke.jasschallenge.client.game.strategy.StrengthLevel;
import com.zuehlke.jasschallenge.messages.type.SessionType;
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
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class BenchmarkRunner {

	private static final String LOCAL_URL = "ws://localhost:3000";

	private final static String BOT_NAME = "JassTheRipper";

	private final static JassStrategy MY_STRATEGY = new JassTheRipperJassStrategy(StrengthLevel.FAST_TEST);

	/**
	 * Evaluates the result of the last modified file in the experiments directory of the jass server.
	 * @return
	 * @throws JSONException
	 */
	public static int evaluateResult() throws JSONException {
		String experimentsDirecory = "../jass-server/experiments/";
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
	public static void runBenchmark(int seed) {
		final ArrayList<Process> processes = new ArrayList<>();

		int numThreads = 2;
		ExecutorService executorService = Executors.newFixedThreadPool(numThreads);

		try {
			// jass-server must be in the same parent directory as the JassTheRipperProject
			startShellProcess(processes, "../jass-server", "env DECK_SHUFFLE_SEED=" + seed + " npm run start:tournament:1");

			// INFO: if the bots fail to connect, it may be because the server has not yet started. Increase the time sleeping
			waitForStartup("Wait for jass server to start...", 7500);

			WebDriver driver = new ChromeDriver();
			driver.get("localhost:3000");
			setUpTournament(driver);

			List<Future<RemoteGame>> futures = new LinkedList<>();

			for (int i = 0; i < numThreads; i++) {
				futures.add(executorService.submit(() -> startGame(LOCAL_URL, new Player(BOT_NAME, MY_STRATEGY), SessionType.TOURNAMENT)));
			}
			waitForStartup("Wait for the JassTheRipper bots to start...", 1000);

			// bachelor-thesis-project must be in the same parent directory as the JassTheRipperProject
			startShellProcess(processes, "../bachelor-thesis-project/src/remote_play", "sudo python3 play_as_challenge_bot.py");
			waitForStartup("Wait for the Challenge bots to start...", 1000);

			// INFO: if the tournament cannot start, it may be because the bots have not yet started. Increase the time sleeping
			startTournament(driver);

			futures.forEach(BenchmarkRunner::awaitFuture);
		} catch (Exception e) {

		} finally {
			executorService.shutdown();
			for (Process process : processes) {
				try {
					killProcess(process);
				} catch (InterruptedException | NoSuchFieldException | IllegalAccessException | IOException e) {
					e.printStackTrace();
				}
			}
		}
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
		File fl = new File(dir);
		File[] files = fl.listFiles(file -> file.isFile());
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


	private static void startShellProcess(ArrayList<Process> processes, String directory, String command) {
		Thread thread = new Thread(() -> {
			ProcessBuilder builder = new ProcessBuilder();
			builder.inheritIO();
			builder.directory(new File(directory));
			builder.command(command.split(" "));

			try {
				processes.add(builder.start());
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
		thread.start();
	}

	private static void killProcess(Process process) throws InterruptedException, IOException, IllegalAccessException, NoSuchFieldException {
		int exitCode = new ProcessBuilder("sudo", "kill", "-9", getPidOfProcess(process) + "").start().waitFor();
		if (exitCode != 0) {
			throw new IllegalStateException("<kill -9> failed, exit code: " + exitCode);
		}
	}

	/**
	 * Retrieves the pid of a running process with reflection.
	 * @param process
	 * @return
	 */
	private static synchronized long getPidOfProcess(Process process) {
		long pid = -1;

		try {
			if (process.getClass().getName().equals("java.lang.UNIXProcess")) {
				Field f = process.getClass().getDeclaredField("pid");
				f.setAccessible(true);
				pid = f.getLong(process);
				f.setAccessible(false);
			}
		} catch (Exception e) {
			e.printStackTrace();
			pid = -1;
		}
		System.out.println("PID of " + process + " is " + pid);
		return pid;
	}

	private static <T> void awaitFuture(Future<T> future) {
		try {
			future.get();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
	}

	private static RemoteGame startGame(String targetUrl, Player myLocalPlayer, SessionType sessionType) throws Exception {
		final RemoteGame remoteGame = new RemoteGame(targetUrl, myLocalPlayer, sessionType);
		remoteGame.start();
		return remoteGame;
	}
}
