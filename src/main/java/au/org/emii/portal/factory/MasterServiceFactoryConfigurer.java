/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal.factory;

import au.org.emii.portal.service.ServiceFactory;
import java.util.Map;

/**
 *
 * @author geoff
 */
public interface MasterServiceFactoryConfigurer {

    public String getAdminConsoleAccountMangerId();

    public Map<String, ServiceFactory> getFactories();

    public String getForgottenPasswordAccountManagerId();

    public String getPortalUserAccountManagerId();

    public String getRegistrationAccountManagerId();

    public String getUserInfoAccountManagerId();

    public void setAdminConsoleAccountMangerId(String adminConsoleAccountMangerId);

    public void setFactories(Map<String, ServiceFactory> factories);

    public void setForgottenPasswordAccountManagerId(String forgottenPasswordAccountManagerId);

    public void setPortalUserAccountManagerId(String portalUserAccountManagerId);

    public void setRegistrationAccountManagerId(String registrationAccountManagerId);

    public void setUserInfoAccountManagerId(String userInfoAccountManagerId);

}
