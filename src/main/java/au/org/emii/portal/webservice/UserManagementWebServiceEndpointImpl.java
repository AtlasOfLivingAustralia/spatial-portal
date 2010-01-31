/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal.webservice;

import java.util.Map;
import org.springframework.beans.factory.annotation.Required;

/**
 *
 * @author geoff
 */
public abstract class UserManagementWebServiceEndpointImpl implements UserManagementWebServiceEndpoint {

    /**
     * URI to make requests to
     */
    protected String serviceUri = null;

    private Map<String, UserManagementWebService> services = null;

    private String defaultUserManagementWebService = null;

    /**
     * XML web service processor
     */
    protected XmlWebService xmlWebService = null;

    public XmlWebService getXmlWebService() {
        return xmlWebService;
    }

    @Required
    public void setXmlWebService(XmlWebService xmlWebService) {
        this.xmlWebService = xmlWebService;
    }


    @Override
    public Map<String, UserManagementWebService> getUserManagementServices() {
        return services;
    }

    @Required
    @Override
    public void setUserManagementServices(Map<String, UserManagementWebService> services) {
        this.services = services;
    }

    @Required
    @Override
    public void setDefaultUserManagementServiceId(String service) {
        defaultUserManagementWebService = service;
    }

    @Override
    public String getDefaultUserManagementServiceId() {
        return defaultUserManagementWebService;
    }

    @Override
    public void setDefaultUserManagementService(UserManagementWebService service) {
        services.put(defaultUserManagementWebService, service);
    }

    @Override
    public UserManagementWebService getDefaultUserManagementService() {
        return getUserManagementWebServiceById(defaultUserManagementWebService);
    }

    @Override
    public UserManagementWebService getUserManagementWebServiceById(String id) {
        return services.get(id);
    }


}
