/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal.factory;

import au.org.emii.portal.service.LoginServicePropertiesImpl;
import au.org.emii.portal.util.PropertiesWriter;
import au.org.emii.portal.service.ForgottenPasswordService;
import au.org.emii.portal.service.LoginService;
import au.org.emii.portal.service.RegistrationService;
import au.org.emii.portal.service.ServiceFactory;
import au.org.emii.portal.service.UserInfoService;
import java.util.Map;
import org.springframework.beans.factory.annotation.Required;

/**
 *
 * @author geoff
 */
public class PropertiesServiceFactory implements ServiceFactory {

    /**
     * Map of ids --> properties filenames
     */
    private Map<String, String> serviceMap = null;

    private PropertiesWriter propertiesWriter = null;

    @Override
    public LoginService createAdminConsoleLoginService(String id) {
        LoginServicePropertiesImpl service = new LoginServicePropertiesImpl();
        service.setPropertiesWriter(propertiesWriter);
        service.setFilename(serviceMap.get(id));

        return service;
    }

    public PropertiesWriter getPropertiesWriter() {
        return propertiesWriter;
    }

    @Required
    public void setPropertiesWriter(PropertiesWriter propertiesWriter) {
        this.propertiesWriter = propertiesWriter;
    }

    public Map<String, String> getServiceMap() {
        return serviceMap;
    }

    public void setServiceMap(Map<String, String> serviceMap) {
        this.serviceMap = serviceMap;
    }

    @Override
    public LoginService createPortalUserLoginService(String id) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public UserInfoService createUserInfoService(String id) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ForgottenPasswordService createForgottenPasswordService(String id) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public RegistrationService createRegistrationService(String id) {
        throw new UnsupportedOperationException("Not supported yet.");
    }



}
