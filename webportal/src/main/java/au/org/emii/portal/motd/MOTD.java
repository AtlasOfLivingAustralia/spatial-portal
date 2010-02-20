/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal.motd;

/**
 *
 * @author geoff
 */
public interface MOTD {

    /**
     * Return a value from the motd language pack
     *
     * @param key
     * @return
     */
    String getMotd(String key);

    /**
     * Check if the motd has been enabled
     *
     * @return
     */
    boolean isMotdEnabled();

    /**
     * Save the passed in parameters to the MOTD file
     * @param enabled whether this message is enabled or not
     * @param title title to display for motd
     * @param message message to display for motd
     * @return true if save was successful otherwise false
     */
    boolean updateMotd(boolean enabled, String title, String message, String portalUsername);

}
