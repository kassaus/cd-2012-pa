package webservices;

import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class StandAlone {
    
    public static void main(final String[] args) {
        final CDWSServiceLocator serviceLocator = new CDWSServiceLocator();
        final CDWSProxy proxy = new CDWSProxy(serviceLocator.getCDWSAddress());
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd    HH:mm:ss");
        try {
            System.out.println(dateFormat.format(new Date(proxy.getTime())));
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}
