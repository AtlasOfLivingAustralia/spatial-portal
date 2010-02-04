/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal.authentication;

import au.org.emii.portal.PortalSession;

/**
 *
 * @author geoff
 */
public interface LogoutService {
        /**
         * Log the user belonging to the passed in portal session
         * out of the web portal
         * @param portalSession
         */
        public void logout(PortalSession portalSession);
}
