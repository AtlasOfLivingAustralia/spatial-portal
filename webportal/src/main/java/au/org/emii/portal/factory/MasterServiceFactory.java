/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal.factory;

import au.org.emii.portal.service.ForgottenPasswordService;
import au.org.emii.portal.service.LoginService;
import au.org.emii.portal.service.RegistrationService;
import au.org.emii.portal.service.UserInfoService;

/**
 *
 * @author geoff
 */
public interface MasterServiceFactory {
    public LoginService createPortalUserLoginService();
    public LoginService createAdminConsoleLoginService();
    public UserInfoService createUserInfoService();
    public ForgottenPasswordService createForgottenPasswordService();
    public RegistrationService createRegistrationService();

}
