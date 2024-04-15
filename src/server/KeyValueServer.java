package server;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import common.IKeyValueServer;
import common.ServerConfig;
import common.Transaction;


public class KeyValueServer extends UnicastRemoteObject implements IKeyValueServer {
    private KeyValueStore keyValueStore = new KeyValueStore();
    private final ConcurrentHashMap<String, Lock> locks = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, Transaction> transactions = new ConcurrentHashMap<>();
    private final Set<String> keysUnderTransaction = ConcurrentHashMap.newKeySet();
    private String centralRegistryHost;
    private int centralRegistryPort;
    private String server;

    /**
     * Constructs a new KeyValue store initialized with host, port and server id.
     */
    public KeyValueServer(String centralRegistryHost, int centralRegistryPort, String server) throws RemoteException {
        super();
        this.centralRegistryHost = centralRegistryHost;
        this.centralRegistryPort = centralRegistryPort;
        this.server = server;
    }

    
    @Override
    public String get(String clientId, String key) throws RemoteException {
        ServerLogger.log(server + " received GET request for key: " + key + " from Client ID: " + clientId);
        if (key == null || key.trim().isEmpty()) {
            Response res = new Response(false, "GET", "Key must not be null or empty.");
            ServerLogger.error(res.toString());
            return res.toString();
        }
        final long timeout = 10000; // Timeout in milliseconds, e.g., 10 seconds
        final long startTime = System.currentTimeMillis();
        while (keysUnderTransaction.contains(key)) {
            // Check for timeout to avoid waiting indefinitely
            if (System.currentTimeMillis() - startTime > timeout) {
                throw new RemoteException("Timeout waiting for transaction on key: " + key);
            }
            // Wait a bit before retrying
            try {
                Thread.sleep(100); // Wait for 100 milliseconds before retrying
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Restore the interrupted status
                throw new RemoteException("Interrupted while waiting for transaction on key: " + key);
            }
        }
        // Proceed with the get operation once the key is no longer under transaction
        String value = keyValueStore.get(key);
        Response res = (value != null)
        ? new Response(true, "GET", "Key found: [key]" + key, value)
        : new Response(false, "GET", "[key]" + key +" not found");
        ServerLogger.log(res.toString());
        return res.toString(); 
    }
    
    @Override
    public String put(String clientId, String key, String value) throws RemoteException {
        ServerLogger.log(server + " received PUT request for key: " + key + " from Client ID: " + clientId);
        if (key == null || key.trim().isEmpty() || value == null) {
            Response res = new Response(false, "PUT", "Key and value must not be null or empty.");
            ServerLogger.error(res.toString());
            return res.toString();
        }

        String transactionId = UUID.randomUUID().toString(); // Generate a unique transaction ID
        boolean allPrepared = broadcastPrepare(transactionId, "PUT", key, value);

        if (allPrepared) {
            this.commit(transactionId);
            broadcastCommit(transactionId);
            return new Response(true, "PUT", "[key]" + key + " added/updated").toString();
        } else {
            this.abort(transactionId);
            broadcastAbort(transactionId);
            return new Response(false, "PUT", "[key]" + key + " aborted").toString();
        }
    }

    @Override
    public String delete(String clientId, String key) throws RemoteException {
        ServerLogger.log(this.server + " received DELETE request for key: " + key + " from Client ID: " + clientId);
        if (key == null || key.trim().isEmpty()) {
            Response res = new Response(false, "DELETE", "Key must not be null or empty.");
            ServerLogger.error(res.toString());
            return res.toString();
        }

        String transactionId = UUID.randomUUID().toString();
        boolean allPrepared = broadcastPrepare(transactionId, "DELETE", key, null); // Value is null for delete
        if (allPrepared) {
            this.commit(transactionId);
            broadcastCommit(transactionId);

            String value = keyValueStore.get(key);
            Response res = (value != null)
            ? new Response(true, "DELETE", "[key]" + key + " deleted")
            : new Response(false, "DELETE", "[key]" + key +" not found");
            return res.toString();
        } else {
            this.abort(transactionId);
            broadcastAbort(transactionId);
            return new Response(false, "DELETE", "DELETE operation aborted").toString();
        }
    }

    private boolean broadcastPrepare(String transactionId, String operation, String key, String value) throws RemoteException {
        if (!this.prepare(transactionId, operation, key, value)) {
            return false; // If local preparation fails, no need to broadcast
        }
        ServerLogger.log(server + "/coordinator is broadcasting prepare for transactionId: " + transactionId);
        ExecutorService executor = Executors.newCachedThreadPool();
        List<Callable<Boolean>> callables = new ArrayList<>();
        for (String serverName : ServerConfig.ALL_SERVERS) {
            if (!serverName.equals(this.server)) { // Skip the coordinator itself
                Callable<Boolean> callable = () -> {
                    try {
                        Registry registry = LocateRegistry.getRegistry(centralRegistryHost, centralRegistryPort);
                        IKeyValueServer remoteStore = (IKeyValueServer) registry.lookup(serverName);
                        boolean result = remoteStore.prepare(transactionId, operation, key, value);
                        if (!result) {
                            ServerLogger.error("Prepare broadcast to " + serverName + " for transactionId: " + transactionId + " failed.");
                        }
                        return result;
                    } catch (Exception e) {
                        ServerLogger.error("Error broadcasting prepare to " + serverName + " for transactionId: " + transactionId + ": " + e.getMessage());
                        return false; // Assume failure in case of an exception
                    }
                };
                callables.add(callable);
            }
        }
        try {
            List<Future<Boolean>> futures = executor.invokeAll(callables, 5, TimeUnit.SECONDS); // 5-second timeout
            executor.shutdown();
            boolean allSuccess = futures.stream().allMatch(future -> {
                try {
                    return future.get();
                } catch (Exception e) {
                    return false;
                }
            });
            if (allSuccess) {
                ServerLogger.log(this.server + "/coordinator: Prepare broadcast completed successfully for transactionId: " + transactionId);
            } else {
                ServerLogger.error(this.server + "/coordinator: Prepare broadcast failed for transactionId: " + transactionId);
            }
            return allSuccess;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            ServerLogger.error(this.server + "/coordinator: Broadcasting prepare was interrupted for transactionId: " + transactionId);
            return false;
        }
    }

    private boolean broadcastCommit(String transactionId) {
        ServerLogger.log(this.server + "/coordinator is broadcasting commit for transactionId: " + transactionId);
        ExecutorService executor = Executors.newCachedThreadPool();
        List<Callable<Boolean>> callables = new ArrayList<>();

        for (String serverName : ServerConfig.ALL_SERVERS) {
            if (!serverName.equals(this.server)) { // Exclude the coordinator
                Callable<Boolean> callable = () -> {
                    try {
                        Registry registry = LocateRegistry.getRegistry(centralRegistryHost, centralRegistryPort);
                        IKeyValueServer remoteStore = (IKeyValueServer) registry.lookup(serverName);
                        boolean success = remoteStore.commit(transactionId);
                        if (!success) {
                            ServerLogger.error("Commit broadcast to " + serverName + " for transactionId: " + transactionId + " failed.");
                        }
                        return success;
                    } catch (Exception e) {
                        ServerLogger.error("Commit error on " + serverName + ": " + e.getMessage());
                        return false; // Assume failure on exception
                    }
                };
                callables.add(callable);
            }
        }
        try {
            List<Future<Boolean>> futures = executor.invokeAll(callables, 5, TimeUnit.SECONDS); // 5-second timeout
            executor.shutdown();
            boolean allSuccess = futures.stream().allMatch(future -> {
                try {
                    return future.get();
                } catch (Exception e) {
                    return false;
                }
            });
            if (allSuccess) {
                ServerLogger.log(this.server + "/coordinator: Commit broadcast successfully completed for transactionId: " + transactionId);
            } else {
                ServerLogger.error(this.server + "/coordinator: Commit broadcast failed for some replicas for transactionId: " + transactionId);
            }
            return allSuccess;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();        
            ServerLogger.error(this.server + "/coordinator: Broadcast commit was interrupted or failed for transactionId: " + transactionId + ": " + e.getMessage());
            return false;
        }
    }
    
    private boolean broadcastAbort(String transactionId) {
        ServerLogger.log(this.server + "/coordinator is broadcasting abort for transactionId: " + transactionId);
        ExecutorService executor = Executors.newCachedThreadPool();
        List<Callable<Boolean>> callables = new ArrayList<>();

        for (String serverName : ServerConfig.ALL_SERVERS) {
            if (!serverName.equals(this.server)) { // Exclude the coordinator
                Callable<Boolean> callable = () -> {
                    try {
                        Registry registry = LocateRegistry.getRegistry(centralRegistryHost, centralRegistryPort);
                        IKeyValueServer remoteStore = (IKeyValueServer) registry.lookup(serverName);
                        boolean success = remoteStore.abort(transactionId);
                        if (!success) {
                            ServerLogger.error("Abort broadcast to " + serverName + " for transactionId: " + transactionId + " failed.");
                        }
                        return success;
                    } catch (Exception e) {
                        ServerLogger.error("Abort error on " + serverName + ": " + e.getMessage());
                        return false; // Assume failure on exception
                    }
                };
                callables.add(callable);
            }
        }
        try {
            List<Future<Boolean>> futures = executor.invokeAll(callables, 5, TimeUnit.SECONDS); // 5-second timeout
            executor.shutdown();
            boolean allSuccess = futures.stream().allMatch(future -> {
                try {
                    return future.get();
                } catch (Exception e) {
                    return false;
                }
            });
            if (allSuccess) {
                ServerLogger.log(this.server + "/coordinator: Abort broadcast successfully completed for transactionId: " + transactionId);
            } else {
                ServerLogger.error(this.server + "/coordinator: Abort broadcast failed for some replicas for transactionId: " + transactionId);
            }
            return allSuccess;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            ServerLogger.error(this.server + "/coordinator: Broadcast abort was interrupted or failed for transactionId: " + transactionId + ": " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean prepare(String transactionId, String operation, String key, String value) throws RemoteException {
        ServerLogger.log(this.server + ": preparing transactionId: " + transactionId + " for key: " + key);
        Lock lock = locks.computeIfAbsent(key, k -> new ReentrantLock());
        lock.lock();
        try {
            Transaction existingTransaction = transactions.get(transactionId);
            // Check if the transaction already exists and is prepared
            if (existingTransaction != null && existingTransaction.isPrepared) {
                ServerLogger.log(this.server + ": TransactionId: " + transactionId + " prepared successfully.");
                return true; // Already prepared
            }

            // Check for other ongoing transactions for the same key
            if (keysUnderTransaction.contains(key)) {
                // If there is another transaction affecting this key, refuse to prepare
                return false;
            }

            Transaction newTransaction = new Transaction(transactionId, operation, key, value, true);
            transactions.put(transactionId, newTransaction); // Mark as prepared
            keysUnderTransaction.add(key); // Track the key as under transaction
            
            return true; // Indicate successful preparation
        } finally {
            lock.unlock(); // Ensure the lock is always released
        }
    }

    @Override
    public boolean commit(String transactionId) throws RemoteException {
        ServerLogger.log(this.server + ": committing transactionId: " + transactionId);
        Transaction transaction = transactions.get(transactionId);
        if (transaction != null) {
            keysUnderTransaction.remove(transaction.key); // Key is no longer under transaction
        }
        if (transaction != null && transaction.isPrepared) {
            Lock lock = locks.computeIfAbsent(transaction.key, k -> new ReentrantLock());
            lock.lock(); // Lock during commit
            try {
                switch (transaction.operation) {
                    case "PUT":
                        keyValueStore.put(transaction.key, transaction.value);
                        break;
                    case "DELETE":
                        keyValueStore.delete(transaction.key);
                        break;
                }
                ServerLogger.log(this.server + ": TransactionId: " + transactionId + " committed successfully for key " + transaction.key);
                transactions.remove(transactionId);
                return true;
            } finally {
                lock.unlock(); // Ensure the lock is always released
            }
        }
        return false;
    }

    @Override
    public boolean abort(String transactionId) throws RemoteException {
        ServerLogger.log(this.server + ": aborting transactionId: " + transactionId);
        Transaction transaction = transactions.get(transactionId);
        if (transaction != null) {
            keysUnderTransaction.remove(transaction.key); // Key is no longer under transaction
            transactions.remove(transactionId); // Cleanup the transaction
            ServerLogger.log(this.server + ": TransactionId: " + transactionId + " aborted successfully for key " + transaction.key);
            return true; // Successfully aborted
        }
        return false; // Transaction not found, implying it could not be aborted as it doesn't exist
    }
}
