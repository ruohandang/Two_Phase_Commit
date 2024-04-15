package common;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IKeyValueServer extends Remote {
    public String get(String clientId, String key) throws RemoteException;
    public String put(String clientId, String key, String value) throws RemoteException;
    public String delete(String clientId, String key) throws RemoteException;
    boolean prepare(String transactionId, String operation, String key, String value) throws RemoteException;
    boolean commit(String transactionId) throws RemoteException;
    boolean abort(String transactionId) throws RemoteException;
}
