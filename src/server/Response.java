package server;
/**
 * A class to format and encapsulate responses sent back to clients.
 */
public class Response {
    private boolean success;
    private String operation;
    private String message;
    private String data; // Optional, can be used to return data for GET operations

    // Constructor for responses without data (e.g., PUT, DELETE)
    public Response(boolean success, String operation, String message) {
        this.success = success;
        this.operation = operation;
        this.message = message;
        this.data = null;
    }

    // Constructor for responses with data
    public Response(boolean success, String operation, String message, String data) {
        this.success = success;
        this.operation = operation;
        this.message = message;
        this.data = data;
    }

    // Getters
    public boolean isSuccess() {
        return success;
    }

    public String getOperation() {
        return operation;
    }

    public String getMessage() {
        return message;
    }

    public String getData() {
        return data;
    }

    // toString method to easily convert the response to a String format for transmission
    @Override
    public String toString() {
        if (data != null) {
            return String.format("%s: %s - %s [value]%s", success ? "Success" : "Failure", operation, message, data);
        } else {
            return String.format("%s: %s - %s", success ? "Success" : "Failure", operation, message);
        }
    }
}
