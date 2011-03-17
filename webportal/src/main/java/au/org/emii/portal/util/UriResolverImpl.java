/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal.util;

import au.org.emii.portal.config.xmlbeans.AbstractService;
import java.util.HashMap;
import java.util.Map;

/**
 * Resolve URIs from service provider id refs in the config file
 * @author geoff
 */
public class UriResolverImpl implements UriResolver {

    /**
     * Map of mappings - id -> uri, as extracted from the config file
     */
    private Map<String,String> mapping = new HashMap<String,String>();

    /**
     * Clear all map entries
     */
    @Override
    public void clear() {
        mapping.clear();
    }

    /**
     * Remove a specific mapping
     * @param id config file id
     */
    @Override
    public void remove(String id) {
        mapping.remove(id);
    }

    /**
     * Add a new mapping
     * @param id id from config file
     * @param uri target URI
     */
    @Override
    public void put(String id, String uri) {
        mapping.put(id, uri);
    }

    /**
     * Lookup the URI that belongs to an id
     * @param id id from config file
     * @return URI corresponding or null if there is no match
     */
    @Override
    public String resolve(String id) {
        return mapping.get(id);
    }

    /**
     * Resolve the URI used in an AbstractService (discovery,baselayer,service)
     * @param service service to be inspected
     * @return the URI to use to get the map data - if a real URI has been provided
     * this will be used otherwise the uriId will be looked up in the mapping
     * list
     */
    @Override
    public String resolve(AbstractService service) {
        return (service.getUri() == null) ? resolve(service.getUriIdRef()) : service.getUri();
    }
}
