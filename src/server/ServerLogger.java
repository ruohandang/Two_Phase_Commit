package server;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * A utility class for logging server-side activities, including received requests, processed responses,
 * errors, and other relevant events, each with a timestamp for tracking.
 */
public class ServerLogger {
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    public static void log(String message) {
        System.out.println("[" + dateFormat.format(new Date()) + "] " + message);
    }

    public static void error(String message) {
        System.err.println("[" + dateFormat.format(new Date()) + "] ERROR: " + message);
    }
}
