/**
 * CDWSSoapBindingSkeleton.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package webservices;

public class CDWSSoapBindingSkeleton implements webservices.CDWS, org.apache.axis.wsdl.Skeleton {
    private webservices.CDWS impl;
    private static java.util.Map _myOperations = new java.util.Hashtable();
    private static java.util.Collection _myOperationsList = new java.util.ArrayList();

    /**
    * Returns List of OperationDesc objects with this name
    */
    public static java.util.List getOperationDescByName(java.lang.String methodName) {
        return (java.util.List)_myOperations.get(methodName);
    }

    /**
    * Returns Collection of OperationDescs
    */
    public static java.util.Collection getOperationDescs() {
        return _myOperationsList;
    }

    static {
        org.apache.axis.description.OperationDesc _oper;
        org.apache.axis.description.FaultDesc _fault;
        org.apache.axis.description.ParameterDesc [] _params;
        _params = new org.apache.axis.description.ParameterDesc [] {
        };
        _oper = new org.apache.axis.description.OperationDesc("getTime", _params, new javax.xml.namespace.QName("http://webservices", "getTimeReturn"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        _oper.setElementQName(new javax.xml.namespace.QName("http://webservices", "getTime"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("getTime") == null) {
            _myOperations.put("getTime", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("getTime")).add(_oper);
    }

    public CDWSSoapBindingSkeleton() {
        this.impl = new webservices.CDWSSoapBindingImpl();
    }

    public CDWSSoapBindingSkeleton(webservices.CDWS impl) {
        this.impl = impl;
    }
    public java.lang.String getTime() throws java.rmi.RemoteException
    {
        java.lang.String ret = impl.getTime();
        return ret;
    }

}
