/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal.util;

import java.util.Properties;

/**
 *
 * @author geoff
 */
public interface PropertiesWriter {

    boolean write(String filename, Properties props, String portalUsername);

}
