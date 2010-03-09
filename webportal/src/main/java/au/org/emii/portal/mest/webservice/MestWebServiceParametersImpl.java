/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.emii.portal.mest.webservice;

import au.org.emii.portal.value.AbstractIdentifierImpl;
import au.org.emii.portal.aspect.CheckNotNull;
import au.org.emii.portal.aspect.LogSetterValue;
import au.org.emii.portal.mest.MestConfiguration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author geoff
 */
public class MestWebServiceParametersImpl extends AbstractIdentifierImpl implements MestWebServiceParameters {

    private MestConfiguration mestConfiguration = null;
    /**
     * protocol and hostname, eg, http://bluenet.utas.edu.au.  - optional and not currently implemented
     * since this is available already for mest search catalogues
     */
    private String serviceBasePath = null;
    /**
     * Username to authenticate to mest with for privileged actions - eg getting
     * user info
     */
    private String username = null;
    /**
     * Password to authenticate to mest with for privileged actions - eg getting
     * user info
     */
    private String password = null;
    /**
     *  Profile to request for newly registered users.  Not optional for MEST/geonetwork servers
     *  (which should use 'RegisteredUser')
     */
    private String newUserProfile = null;
    /**
     * name of the user type that will trigger them becoming a local administrator for the portal.  If not
     * set, no-one from this server can become an admin user.  Example 'Administrator' to give your MEST/geonetwork
     * administrator admin rights on the portal
     */
    private String administratorProfile = null;
    /**
     * filename passed to MEST that will be used to generate the successful registration
     * email message
     */
    private String emailTemplateRegistration = null;
    /**
     * filename passed to MEST that will be used to generate the email containing the
     * changeKey after a forgotten password has been requested (forgotten password -
     * first email)
     */
    private String emailTemplateResetPassword = null;
    /**
     * filename passed to MEST that will be used to generate the password changed email
     *
     */
    private String emailTemplatePasswordChanged = null;

    private String buildUri(String path) {
        return serviceBasePath + path;
    }

    @Override
    public String loginServiceUri() {
        return buildUri(mestConfiguration.getServicePathLogin());
    }

    @Override
    public String userInfoServiceUri() {
        return buildUri(mestConfiguration.getServicePathUserInfo());
    }

    @Override
    public String selfRegistrationServiceUri() {
        return buildUri(mestConfiguration.getServicePathSelfRegistration());
    }

    @Override
    public String resetPasswordServiceUri() {
        return buildUri(mestConfiguration.getServicePathResetPassword());
    }

    @Override
    public String changePasswordServiceUri() {
        return buildUri(mestConfiguration.getServicePathChangePassword());
    }


    /**
     * Getting the port used to connect to a uri - assume port 80 if nothing
     * specified.  This implementation is made static so that the mocks can
     * use it
     * @param uri
     * @return
     */
    @Override
    public int servicePort(String uri) {
        int port;
        String regexp = "\\D*:(\\d+).*";
        Pattern p = Pattern.compile(regexp);
        Matcher m = p.matcher(uri);
        if (m.matches()) {
            port = Integer.parseInt(m.group(1));
        } else {
            // port 80 unless otherwise specified
            port = 80;
        }

        return port;
    }

    @Override
    public String getAdministratorProfile() {
        return administratorProfile;
    }

    @LogSetterValue
    @Override
    public void setAdministratorProfile(String administratorProfile) {
        this.administratorProfile = administratorProfile;
    }

    @Override
    public String getEmailTemplatePasswordChanged() {
        return emailTemplatePasswordChanged;
    }

    @LogSetterValue
    @CheckNotNull
    @Override
    public void setEmailTemplatePasswordChanged(String emailTemplatePasswordChanged) {
        this.emailTemplatePasswordChanged = emailTemplatePasswordChanged;
    }

    @Override
    public String getEmailTemplateRegistration() {
        return emailTemplateRegistration;
    }

    @CheckNotNull
    @LogSetterValue
    @Override
    public void setEmailTemplateRegistration(String emailTemplateRegistration) {
        this.emailTemplateRegistration = emailTemplateRegistration;
    }

    @Override
    public String getEmailTemplateResetPassword() {
        return emailTemplateResetPassword;
    }

    @CheckNotNull
    @LogSetterValue
    @Override
    public void setEmailTemplateResetPassword(String emailTemplateResetPassword) {
        this.emailTemplateResetPassword = emailTemplateResetPassword;
    }

    @Override
    public MestConfiguration getMestConfiguration() {
        return mestConfiguration;
    }

    @CheckNotNull
    @LogSetterValue
    @Override
    public void setMestConfiguration(MestConfiguration mestConfiguration) {
        this.mestConfiguration = mestConfiguration;
    }

    @Override
    public String getNewUserProfile() {
        return newUserProfile;
    }

    @CheckNotNull
    @LogSetterValue
    @Override
    public void setNewUserProfile(String newUserProfile) {
        this.newUserProfile = newUserProfile;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @CheckNotNull
    @LogSetterValue
    @Override
    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String getServiceBasePath() {
        return serviceBasePath;
    }

    @CheckNotNull
    @LogSetterValue
    @Override
    public void setServiceBasePath(String serviceBasePath) {
        this.serviceBasePath = serviceBasePath;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @CheckNotNull
    @LogSetterValue
    @Override
    public void setUsername(String username) {
        this.username = username;
    }
}
