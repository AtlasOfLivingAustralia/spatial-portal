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
public interface ServiceFactory {
    public LoginService createPortalUserLoginService(String id);
    public LoginService createAdminConsoleLoginService(String id);
    public UserInfoService createUserInfoService(String id);
    public ForgottenPasswordService createForgottenPasswordService(String id);
    public RegistrationService createRegistrationService(String id);
}
