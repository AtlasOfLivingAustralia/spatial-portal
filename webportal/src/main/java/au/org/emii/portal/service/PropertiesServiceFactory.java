/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal.service;

import au.org.emii.portal.authentication.ForgottenPasswordService;
import au.org.emii.portal.authentication.LoginService;
import au.org.emii.portal.authentication.LoginServicePropertiesImpl;
import au.org.emii.portal.config.PropertiesWriter;
import au.org.emii.portal.registration.RegistrationService;
import au.org.emii.portal.user.UserInfoService;
import java.util.Map;

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
