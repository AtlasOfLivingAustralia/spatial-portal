
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.emii.portal.mest;

import au.org.emii.portal.value.AbstractIdentifierImpl;
import au.org.emii.portal.aspect.CheckNotNull;
import au.org.emii.portal.aspect.LogSetterValue;

/**
 *
 * @author geoff
 */
public class MestConfigurationImpl extends AbstractIdentifierImpl implements MestConfiguration {

    /**
     * Version number
     */
    private String version = null;

    /**
     * Path to keyword web service - normally /geonetwork/srv/en/portal.search.keywords
     */
    private String servicePathKeyword = null;

    /**
     * path to the login service - normally /geonetwork/srv/en/xml.user.login for MEST/geonetwork
     */
    private String servicePathLogin = null;

    /**
     * path to the self registration service - normally /geonetwork/srv/en/xml.self.register for MEST/geonetwork
     */
    private String servicePathSelfRegistration = null;

    /**
     * path to the user info service - normally /geonetwork/srv/en/xml.user.get for MEST/geonetwork
     */
    private String servicePathUserInfo = null;

    /**
     * path to reset password service - normally /geonetwork/srv/en/xml.password.emaillink
     */
    private String servicePathResetPassword = null;

    /**
     * path to change password service when password is not known (forgotten
     * password system) - normally /geonetwork/srv/en/xml.password.change
     */
    private String servicePathChangePassword = null;

    /**
     * Path to change password service when password is known - normally
     * /geonetwork/srv/en/xml.user.pwupdate
     */
    private String servicePathChangePw = null;

    /**
    Error token sent by MEST when a duplicate user has been detected.  Normally
    errorEmailAddressAlreadyRegistered
     */
    private String tokenDuplicateUser = null;

    /**
    Error token sent by MEST when incorrect username/password detected.  Normally
    UserLoginEx
     */
    private String tokenIncorrectLogin = null;

    /**
    Error token sent from MEST when the changekey has expired.  Normally
    "Change key invalid" (without the quotes)
     */
    private String tokenExpiredChangeKey = null;

    /**
    Error token sent from MEST to indicate user does not exist.  Normally UserNotFoundEx
     */
    private String tokenInvalidUser = null;

    @Override
    public String getServicePathChangePassword() {
        return servicePathChangePassword;
    }

    @LogSetterValue
    @CheckNotNull
    @Override
    public void setServicePathChangePassword(String servicePathChangePassword) {
        this.servicePathChangePassword = servicePathChangePassword;
    }

    @Override
    public String getServicePathKeyword() {
        return servicePathKeyword;
    }

    @LogSetterValue
    @CheckNotNull
    @Override
    public void setServicePathKeyword(String servicePathKeyword) {
        this.servicePathKeyword = servicePathKeyword;
    }

    @Override
    public String getServicePathLogin() {
        return servicePathLogin;
    }

    @LogSetterValue
    @CheckNotNull
    @Override
    public void setServicePathLogin(String servicePathLogin) {
        this.servicePathLogin = servicePathLogin;
    }

    @Override
    public String getServicePathResetPassword() {
        return servicePathResetPassword;
    }

    @LogSetterValue
    @CheckNotNull
    @Override
    public void setServicePathResetPassword(String servicePathResetPassword) {
        this.servicePathResetPassword = servicePathResetPassword;
    }

    @Override
    public String getServicePathSelfRegistration() {
        return servicePathSelfRegistration;
    }

    @LogSetterValue
    @CheckNotNull
    @Override
    public void setServicePathSelfRegistration(String servicePathSelfRegistration) {
        this.servicePathSelfRegistration = servicePathSelfRegistration;
    }

    @Override
    public String getServicePathUserInfo() {
        return servicePathUserInfo;
    }

    @CheckNotNull
    @Override
    @LogSetterValue
    public void setServicePathUserInfo(String servicePathUserInfo) {
        this.servicePathUserInfo = servicePathUserInfo;
    }

    @Override
    public String getTokenDuplicateUser() {
        return tokenDuplicateUser;
    }

    @CheckNotNull
    @Override
    @LogSetterValue
    public void setTokenDuplicateUser(String tokenDuplicateUser) {
        this.tokenDuplicateUser = tokenDuplicateUser;
    }

    @Override
    public String getTokenExpiredChangeKey() {
        return tokenExpiredChangeKey;
    }

    @CheckNotNull
    @Override
    @LogSetterValue
    public void setTokenExpiredChangeKey(String tokenExpiredChangeKey) {
        this.tokenExpiredChangeKey = tokenExpiredChangeKey;
    }

    @Override
    public String getTokenIncorrectLogin() {
        return tokenIncorrectLogin;
    }

    @CheckNotNull
    @Override
    @LogSetterValue
    public void setTokenIncorrectLogin(String tokenIncorrectLogin) {
        this.tokenIncorrectLogin = tokenIncorrectLogin;
    }

    @Override
    public String getTokenInvalidUser() {
        return tokenInvalidUser;
    }

    @CheckNotNull
    @Override
    @LogSetterValue
    public void setTokenInvalidUser(String tokenInvalidUser) {
        this.tokenInvalidUser = tokenInvalidUser;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @CheckNotNull
    @Override
    @LogSetterValue
    public void setVersion(String version) {
        this.version = version;
    }

    @CheckNotNull
    @Override
    @LogSetterValue
    public void setServicePathChangePw(String uri) {
        this.servicePathChangePw = uri;
    }

    @Override
    public String getServicePathChangePw() {
        return servicePathChangePw;
    }


}
