/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal.util;

/**
 *
 * @author geoff
 */
public interface ResolveHostName {

    /**
     * Get the hostname - if the hostname jvm parameter has been set by launching
     * with -Dhostname=xxx, then this will be used in preference to the hostname
     * resolved through the java api
     * @return The hostname, IP address or null if no address can be determined
     */
    String resolveHostName();

}
