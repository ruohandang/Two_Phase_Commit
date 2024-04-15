package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import common.IKeyValueServer;
import common.ServerConfig;

/**
 * The driver class for the client application. It parses command-line arguments
 * (server address, port), initializes the appropriate client,
 * and handles the logic for sending requests and receiving responses based on user input
 * and predefined operations
 */
public class ClientApp {
    private final static String clientId = UUID.randomUUID().toString();
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java client.ClientApp <hostname> <port-number>");
            return;
        }
        String hostname = args[0];
        int port = Integer.parseInt(args[1]);

        try {     
            performAutomaticRequests(hostname, port);
            interactiveMode(hostname, port);
            ClientLogger.log("Client exited.");
        } catch (Exception e) {
            ClientLogger.error("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void performAutomaticRequests(String hostname, int port) throws Exception {
        String serverName = ServerConfig.ALL_SERVERS[new Random().nextInt(ServerConfig.ALL_SERVERS.length)];
        Registry registry = LocateRegistry.getRegistry(hostname, port);
        IKeyValueServer server = (IKeyValueServer) registry.lookup(serverName);

        ClientLogger.log(server.put(clientId ,"name", "HarryPotter"));
        ClientLogger.log(server.put(clientId ,"age", "11"));
        ClientLogger.log(server.put(clientId ,"friend", "Ron Wesley"));
        ClientLogger.log(server.put(clientId ,"school", "Hogwarts"));
        ClientLogger.log(server.put(clientId ,"description", "Harry had a thin face, knobbly knees, black hair and bright-green eyes. " +
                "He wore round glasses held together with a lot of Sellotape because of all the times Dudley had punched him on the nose. " +
                "The only thing Harry liked about his own appearance was a very thin scar on his forehead which was shaped like a bolt of lightning. " +
                "He had had it as long as he could remember and the first question he could ever remember asking his Aunt Petunia was how he had got it."));
        ClientLogger.log(server.delete(clientId ,"school"));
        ClientLogger.log(server.get(clientId ,"school"));
        ClientLogger.log(server.delete(clientId ,"description"));
        ClientLogger.log(server.put(clientId ,"description", "Harry is humble, brave, and loyal. Harry will do anything for his friends, " +
                "including risking his own life. He stands up for the weak (like Neville Longbottom) and is willing to take on the evil and powerful, " +
                "from snobbish classmate Draco Malfoy all the way up to He Who Must Not Be Named. O.K., “Voldemort” – there, we said it!"));
        ClientLogger.log(server.delete(clientId ,"uncle"));
        ClientLogger.log(server.put(clientId ,"dad", "James"));
        ClientLogger.log(server.delete(clientId ,"school"));
        ClientLogger.log(server.delete(clientId ,"age 11"));
        ClientLogger.log(server.put(clientId ,"age", "unknown"));
        ClientLogger.log(server.get(clientId ,"name"));
        ClientLogger.log(server.get(clientId ,"aunt"));
        ClientLogger.log(server.get(clientId ,"friend"));
        ClientLogger.log(server.put(clientId ,"friend", "Luna Lovegood"));
        ClientLogger.log(server.get(clientId ,"friend"));
        ClientLogger.log(server.put(clientId ,"friend", "Ron Wesley"));
        ClientLogger.log(server.get(clientId ,"friend"));
        ClientLogger.log(server.get(clientId ,"description"));

    }

    private static void interactiveMode(String hostname, int port) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Entering interactive mode. Type 'exit' to quit.");

        while (true) {
            System.out.print("Enter your request (usage 'PUT key value', 'GET key', 'DELETE key', 'exit' to quit): ");
            String userInput = reader.readLine();
            if ("exit".equalsIgnoreCase(userInput.trim()) || "quit".equalsIgnoreCase(userInput.trim())) {
                break;
            }

            if (userInput == null || userInput.trim().isEmpty()) {
                ClientLogger.error("Empty request");
                continue;
            }

            String[] parts = userInput.trim().split("\\s+", 3);
            String action = parts[0].toUpperCase();
            String key = parts.length > 1 ? parts[1] : null;
            String value = parts.length > 2 ? parts[2] : null;

            // Attempt the operation with retries on different replicas
            boolean success = attemptOperationWithRetries(action, key, value, hostname, port);
            if (!success) {
                ClientLogger.error("Failed to execute " + action + " after multiple retries.");
            }
        }
        System.out.println("Exiting interactive mode.");
    }

    private static boolean attemptOperationWithRetries(String action, String key, String value, String hostname, int port) {
        List<String> triedServers = new ArrayList<>();
        while (triedServers.size() < ServerConfig.ALL_SERVERS.length) {
            String serverName = selectRandomServer(triedServers);
            triedServers.add(serverName); // Mark this server as tried

            try {
                Registry registry = LocateRegistry.getRegistry(hostname, port);
                IKeyValueServer server = (IKeyValueServer) registry.lookup(serverName);
                switch (action) {
                    case "PUT":
                        if (key != null && value != null) {
                            String putResponse = server.put(clientId ,key, value);
                            ClientLogger.log(putResponse);
                        } else {
                            ClientLogger.error("Incomplete PUT request. Usage: 'PUT key value'");
                        }
                        break;
                    case "GET":
                        if (key != null) {
                            String getResponse = server.get(clientId ,key);
                            ClientLogger.log(getResponse);
                        } else {
                            ClientLogger.error("Incomplete GET request. Usage: 'GET key'");
                        }
                        break;
                    case "DELETE":
                        if (key != null) {
                            String deleteResponse = server.delete(clientId ,key);
                            ClientLogger.log(deleteResponse);
                        } else {
                            ClientLogger.error("Incomplete DELETE request. Usage: 'DELETE key'");
                        }
                        break;
                    default:
                        ClientLogger.error("Unknown action. Available actions are PUT, GET, DELETE.");
                }
                return true; // Success
            } catch (RemoteException re) {
                ClientLogger.error("Remote exception with " + serverName + ": " + re.getMessage() + ". Retrying...");
                // Continue to retry with another server
            } catch (Exception e) {
                ClientLogger.error("Error processing request with " + serverName + ": " + e.getMessage());
                return false; // Non-recoverable error
            }
        }
        return false; // Exhausted retries
    }

    private static String selectRandomServer(List<String> triedServers) {
        Random random = new Random();
        String serverName;
        do {
            serverName = ServerConfig.ALL_SERVERS[random.nextInt(ServerConfig.ALL_SERVERS.length)];
        } while (triedServers.contains(serverName));
        return serverName;
    }

}
