/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.data;

import au.org.emii.portal.composer.MapComposer;
import java.util.ArrayList;
import java.util.Hashtable;
import org.ala.spatial.util.SelectedArea;
import org.ala.spatial.util.UserData;

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
    public static Query get(String id, MapComposer mc, boolean forMapping) {
        Query q = null;

        //search within uploaded records
        if (mc != null) {
            Hashtable<String, UserData> htUserSpecies = (Hashtable) mc.getSession().getAttribute("userpoints");
            if (htUserSpecies != null) {
                UserData ud = htUserSpecies.get(id);

                if (ud != null) {
                    q = ud.getQuery();
                }
            }
        }

        //treat as lsid
        if (q == null) {
            q = new BiocacheQuery(id, null, null, null, forMapping);
        }

        return q;
    }

    public static Query queryFromSelectedArea(Query baseQuery, SelectedArea sa, boolean forMapping) {
        if (sa == null) {
            return baseQuery.newWkt(null, forMapping);
        }
        Query q = null;
        if (sa.getMapLayer() != null) {
            if (sa.getMapLayer().getData("facets") != null) {
                ArrayList<Facet> facets = (ArrayList<Facet>) sa.getMapLayer().getData("facets");
                if (baseQuery == null) {
                    q = new BiocacheQuery(null, null, null, facets, forMapping);
                } else {
                    q = baseQuery.newFacets(facets, forMapping);
                }
            }
        }
        if (q == null) {
            if (baseQuery == null) {
                q = new BiocacheQuery(null, sa.getWkt(), null, null, false);
            } else {
                q = baseQuery.newWkt(sa.getWkt(), forMapping);
            }
        }
        return q;
    }
}
