package backend;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Interface extends Remote {

    String getTime() throws RemoteException;
}