/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal.service;

import au.org.emii.portal.service.ServiceStatusCodes;

/**
 *
 * @author geoff
 */
public interface RegistrationService extends ServiceStatusCodes {

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

}
