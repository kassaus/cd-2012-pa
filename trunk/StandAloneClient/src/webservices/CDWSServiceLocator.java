/**
 * CDWSServiceLocator.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package webservices;

public class CDWSServiceLocator extends org.apache.axis.client.Service implements webservices.CDWSService {

    public CDWSServiceLocator() {
    }


    public CDWSServiceLocator(org.apache.axis.EngineConfiguration config) {
        super(config);
    }

    public CDWSServiceLocator(java.lang.String wsdlLoc, javax.xml.namespace.QName sName) throws javax.xml.rpc.ServiceException {
        super(wsdlLoc, sName);
    }

    // Use to get a proxy class for CDWS
    private java.lang.String CDWS_address = "http://localhost:8080/WebService/services/CDWS";

    public java.lang.String getCDWSAddress() {
        return CDWS_address;
    }

    // The WSDD service name defaults to the port name.
    private java.lang.String CDWSWSDDServiceName = "CDWS";

    public java.lang.String getCDWSWSDDServiceName() {
        return CDWSWSDDServiceName;
    }

    public void setCDWSWSDDServiceName(java.lang.String name) {
        CDWSWSDDServiceName = name;
    }

    public webservices.CDWS getCDWS() throws javax.xml.rpc.ServiceException {
       java.net.URL endpoint;
        try {
            endpoint = new java.net.URL(CDWS_address);
        }
        catch (java.net.MalformedURLException e) {
            throw new javax.xml.rpc.ServiceException(e);
        }
        return getCDWS(endpoint);
    }

    public webservices.CDWS getCDWS(java.net.URL portAddress) throws javax.xml.rpc.ServiceException {
        try {
            webservices.CDWSSoapBindingStub _stub = new webservices.CDWSSoapBindingStub(portAddress, this);
            _stub.setPortName(getCDWSWSDDServiceName());
            return _stub;
        }
        catch (org.apache.axis.AxisFault e) {
            return null;
        }
    }

    public void setCDWSEndpointAddress(java.lang.String address) {
        CDWS_address = address;
    }

    /**
     * For the given interface, get the stub implementation.
     * If this service has no port for the given interface,
     * then ServiceException is thrown.
     */
    public java.rmi.Remote getPort(Class serviceEndpointInterface) throws javax.xml.rpc.ServiceException {
        try {
            if (webservices.CDWS.class.isAssignableFrom(serviceEndpointInterface)) {
                webservices.CDWSSoapBindingStub _stub = new webservices.CDWSSoapBindingStub(new java.net.URL(CDWS_address), this);
                _stub.setPortName(getCDWSWSDDServiceName());
                return _stub;
            }
        }
        catch (java.lang.Throwable t) {
            throw new javax.xml.rpc.ServiceException(t);
        }
        throw new javax.xml.rpc.ServiceException("There is no stub implementation for the interface:  " + (serviceEndpointInterface == null ? "null" : serviceEndpointInterface.getName()));
    }

    /**
     * For the given interface, get the stub implementation.
     * If this service has no port for the given interface,
     * then ServiceException is thrown.
     */
    public java.rmi.Remote getPort(javax.xml.namespace.QName portName, Class serviceEndpointInterface) throws javax.xml.rpc.ServiceException {
        if (portName == null) {
            return getPort(serviceEndpointInterface);
        }
        java.lang.String inputPortName = portName.getLocalPart();
        if ("CDWS".equals(inputPortName)) {
            return getCDWS();
        }
        else  {
            java.rmi.Remote _stub = getPort(serviceEndpointInterface);
            ((org.apache.axis.client.Stub) _stub).setPortName(portName);
            return _stub;
        }
    }

    public javax.xml.namespace.QName getServiceName() {
        return new javax.xml.namespace.QName("http://webservices", "CDWSService");
    }

    private java.util.HashSet ports = null;

    public java.util.Iterator getPorts() {
        if (ports == null) {
            ports = new java.util.HashSet();
            ports.add(new javax.xml.namespace.QName("http://webservices", "CDWS"));
        }
        return ports.iterator();
    }

    /**
    * Set the endpoint address for the specified port name.
    */
    public void setEndpointAddress(java.lang.String portName, java.lang.String address) throws javax.xml.rpc.ServiceException {
        
if ("CDWS".equals(portName)) {
            setCDWSEndpointAddress(address);
        }
        else 
{ // Unknown Port Name
            throw new javax.xml.rpc.ServiceException(" Cannot set Endpoint Address for Unknown Port" + portName);
        }
    }

    /**
    * Set the endpoint address for the specified port name.
    */
    public void setEndpointAddress(javax.xml.namespace.QName portName, java.lang.String address) throws javax.xml.rpc.ServiceException {
        setEndpointAddress(portName.getLocalPart(), address);
    }

}
