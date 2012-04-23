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
import org.ala.layers.dto.Distribution;

/**
 * DAO for the Field object
 * 
 * @author ajay
 */
public interface DistributionDAO {

    public List<Distribution> queryDistributions(String wkt, double min_depth, double max_depth, Integer geomIdx,
                                                 String lsids,
                                                 String type);

    public List<Distribution> queryDistributions(String wkt, double min_depth, double max_depth,
                                                 Boolean pelagic, Boolean coastal, Boolean estuarine,Boolean desmersal,String groupName,
                                                 Integer geomIdx, String lsids,
                                                 String[] families, String[] familyLsids, String[] genera, String[] generaLsids,
                                                 String type);

    public List<Distribution> queryDistributionsByRadius(float longitude, float latitude, float radiusInMetres, double min_depth, double max_depth,
                                                 Boolean pelagic, Boolean coastal, Boolean estuarine, Boolean desmersal, String groupName,
                                                 Integer geomIdx, String lsids,
                                                 String[] families, String[] familyLsids, String[] genera, String[] generaLsids,
                                                 String type);

    public Distribution getDistributionBySpcode(long spcode, String type);

    public List<Distribution> getDistributionByLSID(String[] lsids);
}
