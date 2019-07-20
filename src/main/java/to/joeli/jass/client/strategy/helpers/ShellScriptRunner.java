package to.joeli.jass.client.strategy.helpers;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;

public class ShellScriptRunner {

	public static final Logger logger = LoggerFactory.getLogger(ShellScriptRunner.class);


	/**
	 * This can be used to start independent services we want to use. The started process is saved into the passed list.
	 * This list can then be used to access running processes and kill them if needed.
	 *
	 * @param processes
	 * @param directory
	 * @param command
	 */
	public static void startShellProcessInThread(List<Process> processes, String directory, String command) {
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

	/**
	 * Runs a shell process in the calling thread and blocks until it is finished.
	 *
	 * @param directory
	 * @param command
	 * @return
	 */
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

	/**
	 * Uses the ProcessBuilder to assemble a shell command out of the directory and command strings
	 *
	 * @param directory
	 * @param command
	 * @return
	 */
	private static ProcessBuilder buildShellCommand(String directory, String command) {
		ProcessBuilder builder = new ProcessBuilder();
		builder.inheritIO();
		builder.directory(new File(directory));
		builder.command(command.split(" "));
		return builder;
	}

	/**
	 * Can be used to shutdown a process which cannot be stopped gracefully
	 *
	 * @param process
	 * @param signal
	 * @throws InterruptedException
	 * @throws IOException
	 * @throws IllegalStateException
	 */
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

	/**
	 * Gets the directory where the deep learning with keras is done
	 * @return
	 */
	@NotNull
	public static String getPythonDirectory() {
		return System.getProperty("user.dir") + "/src/main/java/to/joeli/jass/client/strategy/training/python";
	}
}
