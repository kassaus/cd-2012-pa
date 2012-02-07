package webservices;

public class CDWSProxy implements webservices.CDWS {
  private String _endpoint = null;
  private webservices.CDWS cDWS = null;
  
  public CDWSProxy() {
    _initCDWSProxy();
  }
  
  public CDWSProxy(String endpoint) {
    _endpoint = endpoint;
    _initCDWSProxy();
  }
  
  private void _initCDWSProxy() {
    try {
      cDWS = (new webservices.CDWSServiceLocator()).getCDWS();
      if (cDWS != null) {
        if (_endpoint != null)
          ((javax.xml.rpc.Stub)cDWS)._setProperty("javax.xml.rpc.service.endpoint.address", _endpoint);
        else
          _endpoint = (String)((javax.xml.rpc.Stub)cDWS)._getProperty("javax.xml.rpc.service.endpoint.address");
      }
      
    }
    catch (javax.xml.rpc.ServiceException serviceException) {}
  }
  
  public String getEndpoint() {
    return _endpoint;
  }
  
  public void setEndpoint(String endpoint) {
    _endpoint = endpoint;
    if (cDWS != null)
      ((javax.xml.rpc.Stub)cDWS)._setProperty("javax.xml.rpc.service.endpoint.address", _endpoint);
    
  }
  
  public webservices.CDWS getCDWS() {
    if (cDWS == null)
      _initCDWSProxy();
    return cDWS;
  }
  
  public java.lang.String getTime() throws java.rmi.RemoteException{
    if (cDWS == null)
      _initCDWSProxy();
    return cDWS.getTime();
  }
  
  
}