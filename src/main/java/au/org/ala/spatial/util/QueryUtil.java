/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.ala.spatial.util;

import au.org.ala.spatial.dto.UserDataDTO;
import au.org.emii.portal.menu.SelectedArea;
import org.ala.layers.legend.Facet;

import java.util.List;
import java.util.Map;

/**
 * @author Adam
 */
public final class QueryUtil {

    private QueryUtil() {
        //to hide public constructor
    }

    /**
     * Get a new Query by an lsid or upload id.
     *
     * @param id
     * @return
     */
    public static Query get(String id, Map<String, UserDataDTO> htUserSpecies, boolean forMapping, boolean[] geospatialKosher) {
        Query q = null;

        //search within uploaded records
        if (htUserSpecies != null) {
            UserDataDTO ud = htUserSpecies.get(id);

            if (ud != null) {
                q = ud.getQuery();
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
     * @param forMapping
     * @param geospatialKosher
     * @return
     */
    public static Query get(String field, String value, boolean forMapping, boolean[] geospatialKosher) {
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
        if (sa.getFacets() != null) {
            List<Facet> facets = sa.getFacets();
            if (baseQuery == null) {
                q = new BiocacheQuery(null, null, extraParams, facets, forMapping, geospatialKosher);
            } else {
                q = baseQuery.newFacets(facets, forMapping);
            }
        }
        if (q == null) {
            if (baseQuery == null) {
                q = new BiocacheQuery(null, sa.getReducedWkt(), extraParams, null, false, geospatialKosher);
            } else {
                q = baseQuery.newWkt(sa.getWkt(), forMapping);
            }
        }
        return q;
    }
}
