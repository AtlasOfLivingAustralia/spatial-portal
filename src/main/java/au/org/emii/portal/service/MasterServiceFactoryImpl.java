/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal.service;

import au.org.emii.portal.authentication.ForgottenPasswordService;
import au.org.emii.portal.authentication.LoginService;
import au.org.emii.portal.registration.RegistrationService;
import au.org.emii.portal.user.UserInfoService;
import java.util.Map;

/**
 *
 * @author geoff
 */
public class MasterServiceFactoryImpl implements MasterServiceFactory {

    private String portalUserAccountManagerId = null;
    private String adminConsoleAccountMangerId = null;
    private String userInfoAccountManagerId = null;
    private String forgottenPasswordAccountManagerId = null;
    private String registrationAccountManagerId = null;

    /**
     * Map the service id's from the above fields to the ServiceFactory instance
     * they can be resolved in.
     *
     * Example of map contents:
     *  "mest-account-manager-0" --> MestServiceFactory instance
     *  "portal-admin-account-manager-0" --> PropertiesServiceFactory instance
     */
    private Map<String, ServiceFactory> factories = null;


    @Override
    public LoginService createPortalUserLoginService() {
        return factories.get(portalUserAccountManagerId)
                .createPortalUserLoginService(portalUserAccountManagerId);
    }

    @Override
    public LoginService createAdminConsoleLoginService() {
        return factories.get(adminConsoleAccountMangerId)
                .createAdminConsoleLoginService(adminConsoleAccountMangerId);
    }

    @Override
    public UserInfoService createUserInfoService() {
        return factories.get(userInfoAccountManagerId)
                .createUserInfoService(userInfoAccountManagerId);
    }

    @Override
    public ForgottenPasswordService createForgottenPasswordService() {
        return factories.get(forgottenPasswordAccountManagerId)
                .createForgottenPasswordService(forgottenPasswordAccountManagerId);
    }

    @Override
    public RegistrationService createRegistrationService() {
        return factories.get(registrationAccountManagerId)
                .createRegistrationService(registrationAccountManagerId);
    }

    public String getAdminConsoleAccountMangerId() {
        return adminConsoleAccountMangerId;
    }

    public void setAdminConsoleAccountMangerId(String adminConsoleAccountMangerId) {
        this.adminConsoleAccountMangerId = adminConsoleAccountMangerId;
    }

    public Map<String, ServiceFactory> getFactories() {
        return factories;
    }

    public void setFactories(Map<String, ServiceFactory> factories) {
        this.factories = factories;
    }

    public String getForgottenPasswordAccountManagerId() {
        return forgottenPasswordAccountManagerId;
    }

    public void setForgottenPasswordAccountManagerId(String forgottenPasswordAccountManagerId) {
        this.forgottenPasswordAccountManagerId = forgottenPasswordAccountManagerId;
    }

    public String getPortalUserAccountManagerId() {
        return portalUserAccountManagerId;
    }

    public void setPortalUserAccountManagerId(String portalUserAccountManagerId) {
        this.portalUserAccountManagerId = portalUserAccountManagerId;
    }

    public String getRegistrationAccountManagerId() {
        return registrationAccountManagerId;
    }

    public void setRegistrationAccountManagerId(String registrationAccountManagerId) {
        this.registrationAccountManagerId = registrationAccountManagerId;
    }

    public String getUserInfoAccountManagerId() {
        return userInfoAccountManagerId;
    }

    public void setUserInfoAccountManagerId(String userInfoAccountManagerId) {
        this.userInfoAccountManagerId = userInfoAccountManagerId;
    }

    

}
