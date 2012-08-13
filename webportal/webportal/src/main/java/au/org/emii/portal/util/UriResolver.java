/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal.util;

import au.org.emii.portal.config.xmlbeans.AbstractService;

/**
 *
 * @author geoff
 */
public interface UriResolver {

    /**
     * Clear all map entries
     */
    public void clear();

    /**
     * Add a new mapping
     * @param id id from config file
     * @param uri target URI
     */
    public void put(String id, String uri);

    /**
     * Remove a specific mapping
     * @param id config file id
     */
    public void remove(String id);

    /**
     * Lookup the URI that belongs to an id
     * @param id id from config file
     * @return URI corresponding or null if there is no match
     */
    public String resolve(String id);

    public String resolve(AbstractService service);
}
