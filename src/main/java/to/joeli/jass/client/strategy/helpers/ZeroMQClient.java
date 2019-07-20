package to.joeli.jass.client.strategy.helpers;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import to.joeli.jass.client.strategy.training.NetworkType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;


public class ZeroMQClient {

	private static final Gson GSON = new Gson();

	private static List<Process> processes = new ArrayList<>();

	public static final Logger logger = LoggerFactory.getLogger(ZeroMQClient.class);


	private static boolean serverStarted() {
		return !processes.isEmpty();
	}

	/**
	 * Starts a neural network prediction server running in python with keras
	 */
	public static void startServer() {
		if (!serverStarted()) {
			ShellScriptRunner.startShellProcessInThread(processes, ShellScriptRunner.getPythonDirectory(), "python3 predict.py");
			try {
				TimeUnit.SECONDS.sleep(2);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			logger.info("Server started successfully");
		}
	}

	/**
	 * Stops the neural network prediction server.
	 * IMPORTANT: This should be called before the main thread exits! Otherwise the server keeps listening for connections.
	 *
	 * @return
	 */
	public static boolean stopServer() {
		if (serverStarted()) {
			final String reply = sendAndReceive(new Message(Command.SHUTDOWN, null, null));
			if (reply.equals("success")) {
				processes.remove(0);
				logger.info("Server stopped gracefully");
				return true;
			} else {
				try {
					ShellScriptRunner.killProcess(processes.get(0), 9);
					processes.remove(0);
					logger.info("Had to kill server process");
					return true;
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		logger.error("Could not kill server process");
		return false;
	}

	/**
	 * Sends a request to the neural network server to predict the cards distribution for the given features
	 *
	 * @param features
	 * @return
	 */
	public static double[][] predictCards(double[][] features) {
		final Message message = new Message(Command.PREDICT, NetworkType.CARDS, features);

		final String reply = sendAndReceive(message);

		return GSON.fromJson(reply, double[][].class);
	}

	/**
	 * Sends a request to the neural network server to predict the score for the given features
	 *
	 * @param features
	 * @return
	 */
	public static double predictScore(double[][] features) {
		final Message message = new Message(Command.PREDICT, NetworkType.SCORE, features);

		final String reply = sendAndReceive(message);

		return Double.parseDouble(reply);
	}

	/**
	 * Loads the saved weights of a model for a live predicting model in the RAM.
	 * <p>
	 * This is typically called when the trainable network has outperformed the frozen network.
	 * Therefore we want to update the frozen network to the level of the trainable network.
	 * The trainable network will be the newest model in the respective directory.
	 *
	 * @param networkType
	 */
	public static boolean loadWeights(NetworkType networkType) {
		Message message = new Message(Command.LOAD, networkType, null);

		final String reply = sendAndReceive(message);

		return reply.equals("success");
	}


	/**
	 * Sends a request to the neural network server and returns the reply
	 *
	 * @param message
	 * @return
	 */
	private static String sendAndReceive(Message message) {
		if (!serverStarted())
			throw new IllegalStateException("A message can only be sent if the server is running!");

		final String request = GSON.toJson(message);

		try (ZMQ.Socket socket = new ZContext().createSocket(SocketType.REQ)) {

			logger.info("Connecting to neural network server");

			socket.connect("tcp://localhost:5555");

			logger.info("Sending request: " + request);

			long start = System.nanoTime();
			socket.send(request.getBytes(ZMQ.CHARSET), 0);

			byte[] reply = socket.recv(0);
			logger.debug("Request took " + (System.nanoTime() - start) / 1000000.0 + "ms");


			final String result = new String(reply, ZMQ.CHARSET);
			logger.info("Received reply: " + result);
			return result;
		}

	}


	/**
	 * A message used to communicate with the neural network server
	 */
	static class Message {
		private Command command;
		private NetworkType networkType;
		private double[][] features;

		public Message(Command command, NetworkType networkType, double[][] features) {
			this.command = command;
			this.networkType = networkType;
			this.features = features;
		}
	}

	enum Command {
		PREDICT, LOAD, SHUTDOWN
	}


}
