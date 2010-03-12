/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal.mest.webservice;

import au.org.emii.portal.aspect.CheckNotNull;
import au.org.emii.portal.aspect.LogSetterValue;
import au.org.emii.portal.mest.MestConfiguration;

/**
 *
 * @author geoff
 */
public interface MestWebServiceParameters {

    /**
     * Change password when old password is not known (forgotten password
     * reset)
     * @return
     */
    public String changePasswordServiceUri();

    public String getAdministratorProfile();

    public String getEmailTemplatePasswordChanged();

    public String getEmailTemplateRegistration();

    public String getEmailTemplateResetPassword();

    public MestConfiguration getMestConfiguration();

    public String getNewUserProfile();

    public String getPassword();

    public String getServiceBasePath();

    public String getUsername();

    public String loginServiceUri();

    public String resetPasswordServiceUri();

    public String selfRegistrationServiceUri();

    /**
     * Getting the port used to connect to a uri - assume port 80 if nothing
     * specified.  This implementation is made static so that the mocks can
     * use it
     * @param uri
     * @return
     */
    public int servicePort(String uri);

    @LogSetterValue
    public void setAdministratorProfile(String administratorProfile);

    @LogSetterValue
    @CheckNotNull
    public void setEmailTemplatePasswordChanged(String emailTemplatePasswordChanged);

    @CheckNotNull
    @LogSetterValue
    public void setEmailTemplateRegistration(String emailTemplateRegistration);

    @CheckNotNull
    @LogSetterValue
    public void setEmailTemplateResetPassword(String emailTemplateResetPassword);

    @CheckNotNull
    @LogSetterValue
    public void setMestConfiguration(MestConfiguration mestConfiguration);

    @CheckNotNull
    @LogSetterValue
    public void setNewUserProfile(String newUserProfile);

    @CheckNotNull
    @LogSetterValue
    public void setPassword(String password);

    @CheckNotNull
    @LogSetterValue
    public void setServiceBasePath(String serviceBasePath);

    @CheckNotNull
    @LogSetterValue
    public void setUsername(String username);

    public String userInfoServiceUri();

    /**
     * Change password when old password is known
     * @return
     */
    public String changePwServiceUri();

}
