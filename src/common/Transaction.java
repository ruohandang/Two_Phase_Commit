package common;

import java.io.Serializable;

public class Transaction implements Serializable {
    public String transactionId;
    public String operation;
    public String key;
    public String value;
    public boolean isPrepared;

    public Transaction(String transactionId, String operation, String key, String value, boolean isPrepared) {
        this.transactionId = transactionId;
        this.operation = operation;
        this.key = key;
        this.value = value;
        this.isPrepared = isPrepared;
    }
}
