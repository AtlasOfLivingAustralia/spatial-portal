/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal.mest;

import au.org.emii.portal.AbstractIdentifier;
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

    @CheckNotNull
    public void setServicePathChangePassword(String servicePathChangePassword);

    @CheckNotNull
    public void setServicePathKeyword(String servicePathKeyword);

    @CheckNotNull
    public void setServicePathLogin(String servicePathLogin);

    @CheckNotNull
    public void setServicePathResetPassword(String servicePathResetPassword);

    @CheckNotNull
    public void setServicePathSelfRegistration(String servicePathSelfRegistration);

    @CheckNotNull
    public void setServicePathUserInfo(String servicePathUserInfo);

    @CheckNotNull
    public void setTokenDuplicateUser(String tokenDuplicateUser);

    @CheckNotNull
    public void setTokenExpiredChangeKey(String tokenExpiredChangeKey);

    @CheckNotNull
    public void setTokenIncorrectLogin(String tokenIncorrectLogin);

    @CheckNotNull
    public void setTokenInvalidUser(String tokenInvalidUser);

    @CheckNotNull
    public void setVersion(String version);

}
