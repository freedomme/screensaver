
package edu.mit.broad.chembank.shared.mda.webservices.service;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceFeature;


/**
 * This class was generated by the JAX-WS RI.
 * JAX-WS RI 2.1.3-b02-
 * Generated source version: 2.1
 * 
 */
@WebServiceClient(name = "MoleculeWebService", targetNamespace = "http://edu.mit.broad.chembank.shared.mda.webservices.service", wsdlLocation = "http://chembank.broad.harvard.edu/webservices/MoleculeWebService?wsdl")
public class MoleculeWebService_Service
    extends Service
{

    private final static URL MOLECULEWEBSERVICE_WSDL_LOCATION;
    private final static Logger logger = Logger.getLogger(edu.mit.broad.chembank.shared.mda.webservices.service.MoleculeWebService_Service.class.getName());

    static {
        URL url = null;
        try {
            URL baseUrl;
            baseUrl = edu.mit.broad.chembank.shared.mda.webservices.service.MoleculeWebService_Service.class.getResource(".");
            url = new URL(baseUrl, "http://chembank.broad.harvard.edu/webservices/MoleculeWebService?wsdl");
        } catch (MalformedURLException e) {
            logger.warning("Failed to create URL for the wsdl Location: 'http://chembank.broad.harvard.edu/webservices/MoleculeWebService?wsdl', retrying as a local file");
            logger.warning(e.getMessage());
        }
        MOLECULEWEBSERVICE_WSDL_LOCATION = url;
    }

    public MoleculeWebService_Service(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public MoleculeWebService_Service() {
        super(MOLECULEWEBSERVICE_WSDL_LOCATION, new QName("http://edu.mit.broad.chembank.shared.mda.webservices.service", "MoleculeWebService"));
    }

    /**
     * 
     * @return
     *     returns MoleculeWebService
     */
    @WebEndpoint(name = "MoleculeWebService")
    public MoleculeWebService getMoleculeWebService() {
        return super.getPort(new QName("http://edu.mit.broad.chembank.shared.mda.webservices.service", "MoleculeWebService"), MoleculeWebService.class);
    }

    /**
     * 
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns MoleculeWebService
     */
    @WebEndpoint(name = "MoleculeWebService")
    public MoleculeWebService getMoleculeWebService(WebServiceFeature... features) {
        return super.getPort(new QName("http://edu.mit.broad.chembank.shared.mda.webservices.service", "MoleculeWebService"), MoleculeWebService.class, features);
    }

}