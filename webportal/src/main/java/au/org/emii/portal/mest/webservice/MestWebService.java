/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal.mest.webservice;

import au.org.emii.portal.webservice.XmlWebService;
import org.apache.log4j.Logger;

/**
 * Handy base class providing parameters and jersey stub
 * @author geoff
 */
public class MestWebService {
    
    /**
     * Logger instance
     */
    protected Logger logger = Logger.getLogger(getClass());

    /**
     * Mest web service parameters
     */
    protected MestWebServiceParameters parameters = null;

    /**
     * XML web service processor
     */
    protected XmlWebService xmlWebService = null;

    public XmlWebService getXmlWebService() {
        return xmlWebService;
    }

    public void setXmlWebService(XmlWebService xmlWebService) {
        this.xmlWebService = xmlWebService;
    }

    public MestWebServiceParameters getParameters() {
        return parameters;
    }

    public void setParameters(MestWebServiceParameters parameters) {
        this.parameters = parameters;
    }

    


    

}
