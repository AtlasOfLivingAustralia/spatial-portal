/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal.service;

import au.org.emii.portal.authentication.ForgottenPasswordService;
import au.org.emii.portal.authentication.LoginService;
import au.org.emii.portal.registration.RegistrationService;
import au.org.emii.portal.user.UserInfoService;

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
