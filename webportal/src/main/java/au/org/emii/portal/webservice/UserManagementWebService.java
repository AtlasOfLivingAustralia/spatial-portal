/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal.webservice;

/**
 * 
 * @author geoff
 */
public interface UserManagementWebService {
        /**
         * Get URI for the login service
         * @return
         */
        public String loginServiceUri();

        /**
         * Get URI for user info service
         * @return
         */
        public String userInfoServiceUri();

        /**
         * Get URI for self registration service
         * @return
         */
        public String selfRegistrationServiceUri();

        /**
         * URI for requesting password reset (forgotton password)
         * @return
         */
        public String resetPasswordServiceUri();

        /**
         * URI for making password change requests to, includes resetting passwords
         * @return
         */
        public String changePasswordServiceUri();

        /**
         * Get the name of the profile that will be requested for new users
         * @return
         */
        public String newUserProfile();

        /**
         * Get the name of the profile that will be trigger a user being treated as administrator
         * @return
         */
        public String administratorProfile();

        /**
         * Extract the tcp/ip port number that should be used to connect to this uri
         * @param uri uri to connect to - obtained by calling one of the *ServiceUri()
         * methods
         * @return
         */
        public int servicePort(String uri);

}
