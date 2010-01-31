/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal.registration;

import au.org.emii.portal.webservice.UserManagementWebServiceEndpoint;
import au.org.emii.portal.webservice.UserManagementWebService;

/**
 *
 * @author geoff
 */
public interface RegistrationService extends UserManagementWebServiceEndpoint {

        // constants indicating status of registration command

        /**
         * Unknown error processing registration or registration not processed
         */
        public final static int REGISTRATION_UNKNOWN_ERROR      = -1;

        /**
         * Registration processed ok - user has been added
         */
        public final static int REGISTRATION_OK                 = 0;

        /**
         * Registration failed - some fields are missing
         */
        public final static int REGISTRATION_FAIL_INCOMPLETE    = 1;

        /**
         * Registration failed - user already exists (duplicate email address)
         */
        public final static int REGISTRATION_FAIL_DUPLICATE     = 2;

        /**
         * Register a new user, using the default UserManagmentWebService
         * @param lastName user's last name
         * @param firstName user's first name
         * @param email user's email address - will be used as a username
         * @param address user's single line postal address - optional, send null if not available
         * @param state user's state - optional, send null if not available
         * @param zip user's zip code - optional, send null if not available
         * @param country user's country - optional, send null if not available
         * @param organisation user's organisation - optional, send null if not available
         * @param kind user's affiliation - indicates government, ngo, etc.
         * @return True if registration was successful otherwise false
         */
        public int register(    String lastName,
                                String firstName,
                                String email,
                                String address,
                                String state,
                                String zip,
                                String country,
                                String organisation,
                                String affiliation);

        /**
         * Register a new user.
         * @param service key in map of UserManagementWebService instance to use
         * @param lastName user's last name
         * @param firstName user's first name
         * @param email user's email address - will be used as a username
         * @param address user's single line postal address - optional, send null if not available
         * @param state user's state - optional, send null if not available
         * @param zip user's zip code - optional, send null if not available
         * @param country user's country - optional, send null if not available
         * @param organisation user's organisation - optional, send null if not available
         * @param kind user's affiliation - indicates government, ngo, etc.
         * @return True if registration was successful otherwise false
         */
        public int register(    String service,
                                String lastName,
                                String firstName,
                                String email,
                                String address,
                                String state,
                                String zip,
                                String country,
                                String organisation,
                                String affiliation);
}
