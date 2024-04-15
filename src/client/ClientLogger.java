package client;

import java.text.SimpleDateFormat;
import java.util.Date;
/**
 * A utility class responsible for logging client activities, including timestamps,
 * request details, and server responses. This class centralizes logging logic.
 */
public class ClientLogger {
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    public static void log(String message) {
        System.out.println("[" + dateFormat.format(new Date()) + "] " + message);
    }

    public static void error(String message) {
        System.err.println("[" + dateFormat.format(new Date()) + "] ERROR: " + message);
    }
}
