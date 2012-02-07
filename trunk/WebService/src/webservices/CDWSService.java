/**
 * CDWSService.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package webservices;

public interface CDWSService extends javax.xml.rpc.Service {
    public java.lang.String getCDWSAddress();

    public webservices.CDWS getCDWS() throws javax.xml.rpc.ServiceException;

    public webservices.CDWS getCDWS(java.net.URL portAddress) throws javax.xml.rpc.ServiceException;
}
