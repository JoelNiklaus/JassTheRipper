package com.zuehlke.jasschallenge.client.game.strategy.helpers;

/**
 * Created by joelniklaus on 20.05.17.
 */
public class Helper {

	public static int BORDER_TIME = 10;

	public static void printMethodTime(long startTime) {
		final long time = (System.currentTimeMillis() - startTime);
		if (time > BORDER_TIME) {
			StackTraceElement stackTraceElement = Thread.currentThread().getStackTrace()[2];
			System.out.println(stackTraceElement.getMethodName() + " in " + stackTraceElement.getClassName() + ": " + time + "ms");
		}
	}

	public static float factorial(int n) {
		if (n == 1 || n == 0)
			return 1;
		else if (n < 0)
			return 0;
		else
			return n * factorial(n - 1);
	}
}
