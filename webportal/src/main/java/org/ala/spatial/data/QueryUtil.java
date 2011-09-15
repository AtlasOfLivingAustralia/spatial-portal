/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.ala.spatial.data;

import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.menu.MapLayer;
import java.util.Hashtable;
import java.util.List;
import org.ala.spatial.util.UserData;
import org.ala.spatial.wms.RecordsLookup;

/**
 *
 * @author Adam
 */
public class QueryUtil {
    /**
     * Get a new Query by an lsid or upload id.
     *
     * @param id
     * @param mc
     * @return
     */
    public static Query get(String id, MapComposer mc) {
        Query q = null;

        //search within uploaded records
        if(mc != null) {
            Hashtable<String, UserData> htUserSpecies = (Hashtable) mc.getSession().getAttribute("userpoints");
            if(htUserSpecies != null) {
                UserData ud = htUserSpecies.get(id);

                if(ud != null) {
                    q = ud.getQuery();
                }
            }
        }

        //treat as lsid
        if(q == null) {
            q = new SolrQuery(id, null, null, null);
        }

        return q;
    }
}
