package to.joeli.jass.client.game.strategy.helpers;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;

public class ShellScriptRunner {

	public static void startShellProcessInThread(ArrayList<Process> processes, String directory, String command) {
		Thread thread = new Thread(() -> {
			ProcessBuilder builder = buildShellCommand(directory, command);
			try {
				processes.add(builder.start());
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
		thread.start();
	}

	public static boolean runShellProcess(String directory, String command) {
		ProcessBuilder builder = ShellScriptRunner.buildShellCommand(directory, command);

		try {
			Process process = builder.start();

			int exitCode = process.waitFor();
			System.out.println("\nShell process '" + command + "' finished with exit code: " + exitCode);
			return exitCode == 0;

		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return false;
	}

	public static ProcessBuilder buildShellCommand(String directory, String command) {
		ProcessBuilder builder = new ProcessBuilder();
		builder.inheritIO();
		builder.directory(new File(directory));
		builder.command(command.split(" "));
		return builder;
	}

	public static void killProcess(Process process, int signal) throws InterruptedException, IOException, IllegalStateException {
		ProcessBuilder builder = buildShellCommand("/", "kill " + signal + " " + getPidOfProcess(process));
		int exitCode = builder.start().waitFor();
		if (exitCode != 0) {
			throw new IllegalStateException("<kill " + signal + "> failed, exit code: " + exitCode);
		}
	}

	/**
	 * Retrieves the pid of a running process with reflection.
	 *
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
}
