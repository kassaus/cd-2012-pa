/**
 * CDWSSoapBindingImpl.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package webservices;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import backend.Interface;

public class CDWSSoapBindingImpl implements webservices.CDWS{
    public java.lang.String getTime() throws java.rmi.RemoteException {
        
        try {
            final Registry registry = LocateRegistry.getRegistry("127.0.0.1", 2005);
            final Interface stub = (Interface) registry.lookup("ServerBackEnd");
            return stub.getTime();

        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
            return null;
        }
    }
}
