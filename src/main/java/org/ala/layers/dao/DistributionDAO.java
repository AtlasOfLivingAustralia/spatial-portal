/**************************************************************************
 *  Copyright (C) 2010 Atlas of Living Australia
 *  All Rights Reserved.
 *
 *  The contents of this file are subject to the Mozilla Public
 *  License Version 1.1 (the "License"); you may not use this file
 *  except in compliance with the License. You may obtain a copy of
 *  the License at http://www.mozilla.org/MPL/
 *
 *  Software distributed under the License is distributed on an "AS
 *  IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 *  implied. See the License for the specific language governing
 *  rights and limitations under the License.
 ***************************************************************************/

package org.ala.layers.dao;

import java.util.List;
import java.util.Map;

import org.ala.layers.dto.Distribution;
import org.ala.layers.dto.Facet;

/**
 * DAO for the Field object
 *
 * @author ajay
 */
public interface DistributionDAO {

    public List<Distribution> queryDistributions(String wkt, double min_depth, double max_depth, Integer geomIdx,
                                                 String lsids,
                                                 String type,
                                                 String[] dataResources
    );

    public List<Distribution> queryDistributions(String wkt, double min_depth, double max_depth,
                                                 Boolean pelagic, Boolean coastal, Boolean estuarine, Boolean desmersal,
                                                 String groupName, Integer geomIdx, String lsids,
                                                 String[] families, String[] familyLsids, String[] genera, String[] generaLsids,
                                                 String type, String[] dataResources);

    public List<Facet> queryDistributionsFamilyCounts(String wkt, double min_depth, double max_depth,
                                                      Boolean pelagic, Boolean coastal, Boolean estuarine, Boolean desmersal, String groupName,
                                                      Integer geomIdx, String lsids,
                                                      String[] families, String[] familyLsids, String[] genera, String[] generaLsids,
                                                      String type, String[] dataResources);

    public List<Distribution> queryDistributionsByRadius(float longitude, float latitude, float radiusInMetres, double min_depth, double max_depth,
                                                         Boolean pelagic, Boolean coastal, Boolean estuarine, Boolean desmersal, String groupName,
                                                         Integer geomIdx, String lsids,
                                                         String[] families, String[] familyLsids, String[] genera, String[] generaLsids,
                                                         String type, String[] dataResources);

    public List<Facet> queryDistributionsByRadiusFamilyCounts(float longitude, float latitude, float radiusInMetres, double min_depth,
                                                              double max_depth, Boolean pelagic, Boolean coastal,
                                                              Boolean estuarine, Boolean desmersal, String groupName, Integer geomIdx, String lsids,
                                                              String[] families, String[] familyLsids, String[] genera,
                                                              String[] generaLsids, String type, String[] dataResources);

    /**
     * Find a distributions by SPCode
     *
     * @param spcode
     * @return
     */
    public Distribution getDistributionBySpcode(long spcode, String type);

    /**
     * Find a distributions by LSIDs
     *
     * @param lsids
     * @return
     */
    public List<Distribution> getDistributionByLSID(String[] lsids);

    /**
     * Find a distribution by name or LSID
     *
     * @param lsidOrName
     * @return
     */
    public Distribution findDistributionByLSIDOrName(String lsidOrName);

    /**
     * Identify points which fall outside an expert distribution
     *
     * @param lsid   the lsid associated with the species whose expert distribution
     *               we are interested in.
     * @param points Map containing point information. Keys are point ids
     *               (typically the uuids of the associated occurence records),
     *               values are maps containing the point's decimal latitude (with
     *               key "decimalLatitude") and decimal longitude (with key
     *               "decimalLongitude").
     * @return A map containing the distance outside the expert distribution for
     * each point which falls outside the area defined by the
     * distribution. Keys are point ids, values are the distances
     */
    public Map<String, Double> identifyOutlierPointsForDistribution(String lsid, Map<String, Map<String, Double>> points);


    /**
     * Returns the number of vertices that make up the distribution for the supplied lsid
     *
     * @param lsid
     * @return
     */
    public int getNumberOfVertices(String lsid);
}
