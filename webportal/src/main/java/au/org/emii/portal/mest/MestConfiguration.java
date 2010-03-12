/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal.mest;

import au.org.emii.portal.value.AbstractIdentifier;
import au.org.emii.portal.aspect.CheckNotNull;

/**
 *
 * @author geoff
 */
public interface MestConfiguration extends AbstractIdentifier {

    public String getServicePathChangePassword();

    public String getServicePathKeyword();

    public String getServicePathLogin();

    public String getServicePathResetPassword();

    public String getServicePathSelfRegistration();

    public String getServicePathUserInfo();

    public String getTokenDuplicateUser();

    public String getTokenExpiredChangeKey();

    public String getTokenIncorrectLogin();

    public String getTokenInvalidUser();

    public String getVersion();

    public void setServicePathChangePassword(String servicePathChangePassword);

    public void setServicePathKeyword(String servicePathKeyword);

    public void setServicePathLogin(String servicePathLogin);

    public void setServicePathResetPassword(String servicePathResetPassword);

    public void setServicePathSelfRegistration(String servicePathSelfRegistration);

    public void setServicePathUserInfo(String servicePathUserInfo);

    public void setTokenDuplicateUser(String tokenDuplicateUser);

    public void setTokenExpiredChangeKey(String tokenExpiredChangeKey);

    public void setTokenIncorrectLogin(String tokenIncorrectLogin);

    public void setTokenInvalidUser(String tokenInvalidUser);

    public void setVersion(String version);

    public void setServicePathChangePw(String uri);

    public String getServicePathChangePw();

}
