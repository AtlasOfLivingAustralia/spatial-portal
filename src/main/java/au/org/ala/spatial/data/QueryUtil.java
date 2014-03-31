/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.ala.spatial.data;

import au.org.ala.spatial.util.SelectedArea;
import au.org.ala.spatial.util.UserData;
import au.org.emii.portal.composer.MapComposer;
import org.ala.layers.legend.Facet;

import java.util.ArrayList;
import java.util.Hashtable;

/**
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
    public static Query get(String id, MapComposer mc, boolean forMapping, boolean[] geospatialKosher) {
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
            q = new BiocacheQuery(id, null, null, null, forMapping, geospatialKosher);
        }

        return q;
    }

    /**
     * Creates a new query based on the supplied field and values
     *
     * @param field
     * @param value
     * @param mc
     * @param forMapping
     * @param geospatialKosher
     * @return
     */
    public static Query get(String field, String value, MapComposer mc, boolean forMapping, boolean[] geospatialKosher) {
        return new BiocacheQuery(null, null, field + ":\"" + value + "\"", null, forMapping, geospatialKosher);
    }

    public static Query queryFromSelectedArea(Query baseQuery, SelectedArea sa, boolean forMapping, boolean[] geospatialKosher) {
        return queryFromSelectedArea(baseQuery, sa, null, forMapping, geospatialKosher);
    }

    public static Query queryFromSelectedArea(Query baseQuery, SelectedArea sa, String extraParams, boolean forMapping, boolean[] geospatialKosher) {
        if (sa == null) {
            return baseQuery.newWkt(null, forMapping);
        }
        Query q = null;
        if (sa.getMapLayer() != null && (baseQuery == null || !(baseQuery instanceof UserDataQuery))) {
            if (sa.getMapLayer().getFacets() != null) {
                ArrayList<Facet> facets = sa.getMapLayer().getFacets();
                if (baseQuery == null) {
                    q = new BiocacheQuery(null, null, extraParams, facets, forMapping, geospatialKosher);
                } else {
                    q = baseQuery.newFacets(facets, forMapping);
                }
            }
        }
        if (q == null) {
            if (baseQuery == null) {
                q = new BiocacheQuery(null, sa.getWkt(), extraParams, null, false, geospatialKosher);
            } else {
                q = baseQuery.newWkt(sa.getWkt(), forMapping);
            }
        }
        return q;
    }
}
