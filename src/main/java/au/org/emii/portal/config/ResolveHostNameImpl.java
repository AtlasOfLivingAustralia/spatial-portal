/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal.config;

import java.net.InetAddress;
import java.net.UnknownHostException;
import org.apache.log4j.Logger;

/**
 * Class for resolving the hostname through java api or by extra jvm
 * argument 'hostname'
 * @author geoff
 */
public class ResolveHostNameImpl implements ResolveHostName {

    private Logger logger = Logger.getLogger(this.getClass());
    private final static String HOST_NAME_JVM_PARAM = "hostname";

    /**
     * Get the hostname - if the hostname jvm parameter has been set by launching
     * with -Dhostname=xxx, then this will be used in preference to the hostname
     * resolved through the java api
     * @return The hostname, IP address or null if no address can be determined
     */
    @Override
    public String resolveHostName() {
        String hostName = null;
        if (System.getProperties().getProperty(HOST_NAME_JVM_PARAM) != null) {
            hostName = System.getProperties().getProperty(HOST_NAME_JVM_PARAM);
        } else {
            try {
                hostName = InetAddress.getLocalHost().getCanonicalHostName();
            } catch (UnknownHostException ex) {
                try {
                    hostName = InetAddress.getLocalHost().getHostAddress();
                    logger.error(
                            "can't resolve " + hostName + " to a hostname - using " +
                            "naked IP address instead.  Please fix the broken DNS " +
                            "for this server or add -Dhostname=myhostname.com to " +
                            "your tomcat JVM arguments ");
                } catch (UnknownHostException ex1) {
                    // can't even get an IP address - use "localhost"
                    logger.error(
                            "Your DNS is very broken - can't resolve an IP address for the server the portal is " +
                            "running on.  Requests to use the automatic squid caching for map layers will likely " +
                            "fail.  A quick workaround is to add -Dhostname=myhostname.com to your tomcat JVM " +
                            "arguments, but you should still fix your broken DNS");
                }
            }
        }
        logger.info("hostname resolves to '" + hostName + "'");
        return hostName;
    }
}
