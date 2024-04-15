This application is a sophisticated key-value store system designed to explore and implement the principles of distributed systems using Java's Remote Method Invocation (RMI) framework. It significantly extends the capabilities of a basic key-value store by introducing replication across multiple servers to enhance data availability, consistency, and fault tolerance. At its core, the application utilizes a two-phase commit (2PC) protocol to ensure that all data modifications (PUT and DELETE operations) are consistently replicated across all server instances, maintaining the integrity and consistency of the distributed store.

Features and Functionality

- Distributed Key-Value Store: Allows for the storage, retrieval, and deletion of key-value pairs in a distributed manner, ensuring data is synchronized across all server replicas.
- Two-Phase Commit Protocol: Implements 2PC to maintain data consistency across replicas, with the server receiving a client request acting as the coordinator to manage transactions.
- Automatic Data Population: Features an automatic request mode where a client pre-populates the key-value store with a predefined set of data, selecting a random server for initial data seeding.
- Interactive Client Operations: Supports an interactive mode allowing users to manually perform PUT, GET, and DELETE operations. Each request randomly chooses a server to execute the operation, demonstrating the system's ability to handle requests across different servers seamlessly.
- Fault Tolerance and High Availability: Designed to be resilient to server failures, ensuring that the key-value store remains accessible and consistent even when individual server instances encounter issues.

How It Works

- Server Coordination: Upon receiving a request from the client, the chosen server instance takes on the role of a coordinator for that particular transaction. It orchestrates the two-phase commit process across all replicas, first securing agreement from all servers to proceed (prepare phase) and then committing the transaction across the board (commit phase) to ensure atomicity and consistency.
- Client Operations:
  - Automatic Request Mode: The client randomly selects a server to pre-populate the key-value store with a predefined dataset, simulating an initial load or batch update scenario.
  - Interactive Mode: Users can interactively perform CRUD operations on the key-value store. Each operation is directed towards a randomly selected server, showcasing the distributed system's ability to handle operations across different nodes transparently.

## How to run RMI program locally

### Step 1: in `src` folder, compile the code:
```
javac server/*.java client/*.java
```
### Step 2: run servers (this will start 5 servers - replicas of key value store), to use default host (localhost) and port (1099):
```
./server.sh
```
To specify a custom host but use the default port (1099):
```
./server.sh custom_host
```

To specify both a custom host and a custom port:
```
./server.sh custom_host custom_port
```


### Step 3: Open a new terminal 
If you run clients without any arguments to connect to the default settings (localhost on port 1099):
```
./client.sh
```
If your RMI server running on a custom host but use the default port (1099), provide the hostname as the first argument:
```
./client.sh custom_host
```

To connect to an RMI server on a specific host and port, provide both as arguments:
```
./client.sh custom_host custom_port
```

NOTE: You can run multiple clients concurrently.

### Step 4: Quit app

- For RMI servers, just press ```enter``` in server terminal

- For Client, input ```exit``` or ```quit``` in the client ternimal

### If you have no permission to run shell file, please run:
```
chmod +x server.sh
chmod +x client.sh
```