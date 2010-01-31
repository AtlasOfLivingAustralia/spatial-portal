/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal.authentication;

import au.org.emii.portal.user.PortalUserImpl;
import au.org.emii.portal.PortalSession;

/**
 * Logout service implementation (for mest)
 * @author geoff
 */
public class LogoutServiceImpl implements LogoutService {

        /**
         * Log the current user out.  Although mest provides a logout web 
         * service, this will only logout the currently logged in for the current
         * browser session.
         *
         * As there is no browser session since we are logging in server-side,
         * calling the provided web service will either logout our admin user or
         * attempt to logout the most recently logged in user.  Therefore, all
         * we do here is put a new PortalUser into the user's session so that
         * they will no longer be marked as logged in within the portal
         * 
         * @param portalSession
         */
        @Override
        public void logout(PortalSession portalSession) {
                portalSession.setPortalUser(new PortalUserImpl());
        }

}
