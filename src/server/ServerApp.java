package server;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import common.IKeyValueServer;

/**
 * The driver class for the server application that initializes and starts RMI.
 */
public class ServerApp {
    public static void main(String[] args) {
        if (args.length < 3) {
            System.err.println("Usage: java ServerApp <Central Registry Host> <RMI Registry Port> <Server ID>");
            System.exit(1);
        }
        String centralRegistryHost = args[0];
        int portNumber = Integer.parseInt(args[1]);
        String serverId = args[2];
        String serverName = "KeyValueService" + serverId;

        try {
            IKeyValueServer keyValueServer = new KeyValueServer(centralRegistryHost, portNumber, serverName);

            // sharing one central registry host
            Registry registry = LocateRegistry.getRegistry(centralRegistryHost, portNumber);
            registry.rebind(serverName, keyValueServer);

            ServerLogger.log("Server instance identified by '" + serverName + "' has been successfully registered with the RMI registry on " + centralRegistryHost + ":" + portNumber);
            addShutdownHook(keyValueServer, serverName);
        } catch (Exception e) {
            ServerLogger.error("Server exception: " + e.toString());
            e.printStackTrace();
        }
    }

        private static void addShutdownHook(IKeyValueServer keyValueServer, String serverName) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                System.out.println("Server " + serverName + " is shutting down...");
                // Unexport the remote object
                if (keyValueServer != null) {
                    UnicastRemoteObject.unexportObject(keyValueServer, true);
                    System.out.println("Server " + serverName + " unexported successfully.");
                }
            } catch (Exception e) {
                System.err.println("Error during server shutdown: " + e.getMessage());
                e.printStackTrace();
            }
        }));
    }
}
