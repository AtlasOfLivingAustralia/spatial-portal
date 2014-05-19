/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal.factory;

import java.util.Properties;

/**
 * @author geoff
 */
public interface PortalDocumentFactory {

    /**
     * Lookup the the name of the config file we should be reading from the environement
     * then validate and parse it returning a pointer to the root element.
     * <p/>
     * If an error occurs here (null returned) then the system is FUBAR
     *
     * @return PortalDocument instance if reading succeeded, null if an error was encountered
     */
    public Properties createPortalDocumentInstance();

}
