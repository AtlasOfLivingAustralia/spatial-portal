/**************************************************************************
 *  Copyright (C) 2012 Atlas of Living Australia
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.sql.DataSource;

import org.ala.layers.dto.Distribution;
import org.ala.layers.dto.Facet;
import org.ala.layers.intersect.IntersectConfig;
import org.apache.log4j.Logger;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.simple.ParameterizedBeanPropertyRowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author ajay
 */
@Service("distributionDao")
public class DistributionDAOImpl implements DistributionDAO {

    /**
     * log4j logger
     */
    private static final Logger logger = Logger.getLogger(DistributionDAOImpl.class);
    private SimpleJdbcTemplate jdbcTemplate;
    private String viewName = "distributions";
    private final String SELECT_CLAUSE = "select gid,spcode,scientific,authority_,common_nam,\"family\",genus_name,specific_n,min_depth,"
            + "max_depth,pelagic_fl,coastal_fl,desmersal_fl,estuarine_fl,family_lsid,genus_lsid,caab_species_number,"
            + "caab_family_number,group_name,metadata_u,wmsurl,lsid,type,area_name,pid,checklist_name,area_km,notes,"
            + "geom_idx,image_quality,data_resource_uid";

    public Distribution findDistributionByLSIDOrName(String lsidOrName) {
        String sql = SELECT_CLAUSE + " from " + viewName + " WHERE " +
                "lsid=:lsid OR caab_species_number=:caab_species_number " +
                "OR scientific like :scientificName OR scientific like :scientificNameWithSubgenus limit 1";
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("lsid", lsidOrName);
        params.put("scientificName", lsidOrName);
        params.put("scientificNameWithSubgenus", removeSubGenus(lsidOrName));
        params.put("caab_species_number", lsidOrName);
        List<Distribution> ds = updateWMSUrl(jdbcTemplate.query(sql, ParameterizedBeanPropertyRowMapper.newInstance(Distribution.class), params));
        if (!ds.isEmpty())
            return ds.get(0);
        else
            return null;
    }

    private String removeSubGenus(String str) {
        if (str != null && str.contains("(") && str.contains(")"))
            return str.replaceAll(" \\([A-Z][a-z]{1,}\\) ", " ");
        return str;
    }

    @Override
    public List<Distribution> queryDistributions(String wkt, double min_depth, double max_depth, Integer geomIdx, String lsids, String type, String[] dataResources) {
        return queryDistributions(wkt, min_depth, max_depth, null, null, null, null, null, geomIdx, lsids, null, null, null, null, type, dataResources);
    }

    @Override
    public List<Distribution> queryDistributions(String wkt, double min_depth, double max_depth, Boolean pelagic,
                                                 Boolean coastal, Boolean estuarine, Boolean desmersal, String groupName,
                                                 Integer geomIdx, String lsids, String[] families, String[] familyLsids, String[] genera,
                                                 String[] generaLsids, String type, String[] dataResources) {
        logger.info("Getting distributions list");

        StringBuilder whereClause = new StringBuilder();
        Map<String, Object> params = new HashMap<String, Object>();
        constructWhereClause(min_depth, max_depth, pelagic, coastal, estuarine, desmersal, groupName, geomIdx, lsids,
                families, familyLsids, genera, generaLsids, type, dataResources, params, whereClause);
        if (wkt != null && wkt.length() > 0) {
            if (whereClause.length() > 0) {
                whereClause.append(" AND ");
            }
            whereClause.append("ST_INTERSECTS(the_geom, ST_GEOMFROMTEXT( :wkt , 4326))");
            params.put("wkt", wkt);
        }

        String sql = SELECT_CLAUSE + " from " + viewName;
        if (whereClause.length() > 0) {
            sql += " WHERE " + whereClause.toString();
        }

        return updateWMSUrl(jdbcTemplate.query(sql, ParameterizedBeanPropertyRowMapper.newInstance(Distribution.class), params));
    }

    @Override
    public List<Facet> queryDistributionsFamilyCounts(String wkt, double min_depth, double max_depth, Boolean pelagic, Boolean coastal, Boolean estuarine, Boolean desmersal, String groupName,
                                                      Integer geomIdx, String lsids, String[] families, String[] familyLsids, String[] genera, String[] generaLsids, String type, String[] dataResources) {
        logger.info("Getting distributions list - family counts");

        StringBuilder whereClause = new StringBuilder();
        Map<String, Object> params = new HashMap<String, Object>();
        constructWhereClause(min_depth, max_depth, pelagic, coastal, estuarine, desmersal, groupName, geomIdx, lsids,
                families, familyLsids, genera, generaLsids, type, dataResources, params, whereClause);
        if (wkt != null && wkt.length() > 0) {
            if (whereClause.length() > 0) {
                whereClause.append(" AND ");
            }
            whereClause.append("ST_INTERSECTS(the_geom, ST_GEOMFROMTEXT( :wkt , 4326))");
            params.put("wkt", wkt);
        }

        String sql = "Select family as name, count(*) as count from " + viewName;
        if (whereClause.length() > 0) {
            sql += " WHERE " + whereClause.toString();
        }
        sql = sql + " group by family";

        return jdbcTemplate.query(sql, ParameterizedBeanPropertyRowMapper.newInstance(Facet.class), params);
    }


    @Override
    public Distribution getDistributionBySpcode(long spcode, String type) {
        String sql = SELECT_CLAUSE + ", ST_AsText(the_geom) AS geometry, ST_AsText(bounding_box) as bounding_box FROM " + viewName + " WHERE spcode= ? AND type= ?";
        List<Distribution> d = updateWMSUrl(jdbcTemplate.query(sql, ParameterizedBeanPropertyRowMapper.newInstance(Distribution.class), (double) spcode, type));
        if (d.size() > 0) {
            return d.get(0);
        }
        return null;
    }

    public List<Distribution> queryDistributionsByRadius(float longitude, float latitude, float radiusInMetres,
                                                         double min_depth, double max_depth, Integer geomIdx, String lsids, String[] families,
                                                         String[] familyLsids, String[] genera, String[] generaLsids, String type, String[] dataResources) {
        return queryDistributionsByRadius(longitude, latitude, radiusInMetres, min_depth, max_depth, null, null, null, null, null,
                geomIdx, lsids, families, familyLsids, genera, generaLsids, type, dataResources);
    }

    /**
     * Query by radius
     *
     * @return set of species with distributions intersecting the radius
     */
    public List<Distribution> queryDistributionsByRadius(float longitude, float latitude, float radiusInMetres, double min_depth, double max_depth, Boolean pelagic, Boolean coastal,
                                                         Boolean estuarine, Boolean desmersal, String groupName, Integer geomIdx, String lsids,
                                                         String[] families, String[] familyLsids, String[] genera, String[] generaLsids, String type, String[] dataResources) {
        logger.info("Getting distributions list with a radius - " + radiusInMetres + "m");

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("radius", convertMetresToDecimalDegrees(radiusInMetres));
        params.put("type", type);
        String pointGeom = "POINT(" + longitude + " " + latitude + ")";

        //String sql = SELECT_CLAUSE + " from " + viewName + " where ST_Distance_Sphere(the_geom, ST_GeomFromText('" + pointGeom + "', 4326)) <= :radius";

        //ST_intersects(points_geom, ST_buffer(search_point_geom, Distance))


        String sql = SELECT_CLAUSE + " from " + viewName + " where ST_DWithin(the_geom, ST_GeomFromText('" + pointGeom + "', 4326), :radius)";
        // add additional criteria
        StringBuilder whereClause = new StringBuilder();

        constructWhereClause(min_depth, max_depth, pelagic, coastal, estuarine, desmersal, groupName, geomIdx, lsids,
                families, familyLsids, genera, generaLsids, type, dataResources, params, whereClause);

        if (whereClause.length() > 0) {
            sql += " AND " + whereClause.toString();
        }
        return updateWMSUrl(jdbcTemplate.query(sql, ParameterizedBeanPropertyRowMapper.newInstance(Distribution.class), params));
    }

    /**
     * Query by radius
     *
     * @return set of species with distributions intersecting the radius
     */
    public List<Facet> queryDistributionsByRadiusFamilyCounts(float longitude, float latitude, float radiusInMetres, double min_depth, double max_depth, Boolean pelagic, Boolean coastal,
                                                              Boolean estuarine, Boolean desmersal, String groupName, Integer geomIdx, String lsids, String[] families, String[] familyLsids,
                                                              String[] genera, String[] generaLsids, String type, String[] dataResources) {
        logger.info("Getting distributions list with a radius");

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("radius", convertMetresToDecimalDegrees(radiusInMetres));
        params.put("type", type);
        String pointGeom = "POINT(" + longitude + " " + latitude + ")";

        //String sql = "Select family as name, count(*) as count from " + viewName + " where ST_Distance_Sphere(the_geom, ST_GeomFromText('" + pointGeom + "', 4326)) <= :radius";
        //String sql = "Select family as name, count(*) as count from " + viewName + " where ST_Distance_Sphere(the_geom, ST_GeomFromText('" + pointGeom + "', 4326)) <= :radius";
        //select count(*) from distributions where ST_DWithin(the_geom, ST_GeomFromText('POINT(150.2177 -35.7316)', 4326), 10000);
        String sql = "Select family as name, count(*) as count from " + viewName + " where ST_DWithin(the_geom, ST_GeomFromText('" + pointGeom + "', 4326), :radius)";

        // add additional criteria
        StringBuilder whereClause = new StringBuilder();

        constructWhereClause(min_depth, max_depth, pelagic, coastal, estuarine, desmersal, groupName, geomIdx, lsids,
                families, familyLsids, genera, generaLsids, type, dataResources, params, whereClause);

        if (whereClause.length() > 0) {
            sql += " AND " + whereClause.toString();
        }

        sql = sql + " group by family";

        return jdbcTemplate.query(sql, ParameterizedBeanPropertyRowMapper.newInstance(Facet.class), params);
    }


    @Override
    public List<Distribution> getDistributionByLSID(String[] lsids) {
        String sql = SELECT_CLAUSE + ", ST_AsText(the_geom) AS geometry, ST_AsText(bounding_box) as bounding_box FROM " + viewName + "  WHERE lsid IN (:lsids)";
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("lsids", Arrays.asList(lsids));
        params.put("type", Distribution.EXPERT_DISTRIBUTION);
        return updateWMSUrl(jdbcTemplate.query(sql, ParameterizedBeanPropertyRowMapper.newInstance(Distribution.class), params));
    }

    /**
     * WARNING: This conversion isnt accurate..
     *
     * @return
     */
    public Double convertMetresToDecimalDegrees(Float metres) {
        //0.01 degrees is approximately 1110 metres
        //0.00001 1.11 m
        return (metres / 1.11) * 0.00001;
    }

    /**
     * @param min_depth
     * @param max_depth
     * @param geomIdx
     * @param lsids
     * @param params
     * @param where
     */
    private void constructWhereClause(double min_depth, double max_depth, Boolean pelagic, Boolean coastal,
                                      Boolean estuarine, Boolean desmersal, String groupName, Integer geomIdx,
                                      String lsids, String[] families, String[] familyLsids, String[] genera,
                                      String[] generaLsids, String type, String[] dataResources, Map<String, Object> params, StringBuilder where) {
        if (geomIdx != null && geomIdx >= 0) {
            where.append(" geom_idx = :geom_idx ");
            params.put("geom_idx", geomIdx);
        }

        if (lsids != null && lsids.length() > 0) {
            if (where.length() > 0) {
                where.append(" AND ");
            }
            where.append(":lsids LIKE '% '||lsid||' %'  ");
            params.put("lsids", " " + lsids.replace(",", " ") + " ");
        }

        if (dataResources != null && dataResources.length > 0) {
            if (where.length() > 0) {
                where.append(" AND ");
            }
            where.append("data_resource_uid IN (:dataResources) ");
            params.put("dataResources", Arrays.asList(dataResources));
        }

        if (min_depth != -1 && max_depth != -1) {
            if (where.length() > 0) {
                where.append(" AND ");
            }
            where.append("min_depth <= :max_depth AND max_depth >= :min_depth ");
            params.put("max_depth", new Double(max_depth));
            params.put("min_depth", new Double(min_depth));
        } else if (min_depth != -1) {
            if (where.length() > 0) {
                where.append(" AND ");
            }
            where.append("max_depth >= :min_depth ");
            params.put("min_depth", new Double(min_depth));
        } else if (max_depth != -1) {
            if (where.length() > 0) {
                where.append(" AND ");
            }
            where.append("min_depth <= :max_depth ");
            params.put("max_depth", new Double(max_depth));
        }

        if (pelagic != null) {
            if (where.length() > 0) {
                where.append(" AND ");
            }
            if (pelagic) {
                where.append("pelagic_fl > 0 ");
            } else {
                where.append("pelagic_fl = 0 ");
            }
        }

        if (coastal != null) {
            if (where.length() > 0) {
                where.append(" AND ");
            }
            where.append("coastal_fl = :coastal ");
            params.put("coastal", coastal ? 1 : 0);
        }

        if (estuarine != null) {
            if (where.length() > 0) {
                where.append(" AND ");
            }
            where.append("estuarine_fl = :estuarine ");
            params.put("estuarine", estuarine ? 1 : 0);
        }

        if (desmersal != null) {
            if (where.length() > 0) {
                where.append(" AND ");
            }
            where.append("desmersal_fl = :desmersal ");
            params.put("desmersal", desmersal ? 1 : 0);
        }

        if (type != null) {
            if (where.length() > 0) {
                where.append(" AND ");
            }
            where.append("type = :distribution_type ");
            params.put("distribution_type", type);
        }

        if (groupName != null) {
            if (where.length() > 0) {
                where.append(" AND ");
            }
            where.append("group_name = :groupName ");
            params.put("groupName", groupName);
        }

        if (families != null) {
            if (where.length() > 0) {
                where.append(" AND ");
            }
            where.append("family IN (:families) ");
            params.put("families", Arrays.asList(families));
        }

        if (familyLsids != null) {
            if (where.length() > 0) {
                where.append(" AND ");
            }
            where.append("family_lsid IN (:familyLsids) ");
            params.put("familyLsids", Arrays.asList(familyLsids));
        }

        if (genera != null) {
            if (where.length() > 0) {
                where.append(" AND ");
            }
            where.append("genus_name IN (:genera) ");
            params.put("genera", Arrays.asList(genera));
        }

        if (generaLsids != null) {
            if (where.length() > 0) {
                where.append(" AND ");
            }
            where.append("genus_lsid IN (:generaLsids) ");
            params.put("generaLsids", Arrays.asList(generaLsids));
        }
    }

    private List<Distribution> updateWMSUrl(List<Distribution> distributions) {
        if (distributions != null) {
            for (Distribution distribution : distributions) {
                if (distribution.getWmsurl() != null) {
                    if (!distribution.getWmsurl().startsWith("/")) {
                        distribution.setWmsurl(distribution.getWmsurl().replace(IntersectConfig.GEOSERVER_URL_PLACEHOLDER, IntersectConfig.getGeoserverUrl()));
                    } else {
                        distribution.setWmsurl(IntersectConfig.getGeoserverUrl() + distribution.getWmsurl());
                    }
                }
            }
        }
        return distributions;
    }

    @Override
    public int getNumberOfVertices(String lsid) {
        return jdbcTemplate.queryForInt("SELECT npoints(ds.the_geom) from distributionshapes ds join distributiondata dd on dd.geom_idx = ds.id where dd.lsid=? ", lsid);
    }

    @Override
    @Transactional(readOnly = false, propagation = Propagation.REQUIRES_NEW)
    public Map<String, Double> identifyOutlierPointsForDistribution(String lsid, Map<String, Map<String, Double>> points) {
        Map<String, Double> outlierDistances = new HashMap<String, Double>();
        String firstId = points.keySet().iterator().next();
        logger.debug("Starting to identifyOutlierPointsForDistribution " + lsid + " " + firstId);
        // get the id of the shape in the distributionshapes table associated
        // with the lsid
        try {
            final int expertDistributionShapeId = jdbcTemplate.queryForInt("SELECT geom_idx from distributiondata WHERE lsid = ?", lsid);
            logger.debug("Finished getting the geomid for " + firstId);

            // if no points were supplied (empty map) then just return an empty
            // result map
            if (points.isEmpty()) {
                return outlierDistances;
            }

            // Create temporary table for the point information
            jdbcTemplate.update("CREATE TEMPORARY TABLE temp_exp_dist_outliers (id text PRIMARY KEY, point geography) ON COMMIT DROP");


            logger.debug("Finished creating the temporary table  for " + firstId);

            // Insert all the points into the temporary table, along with the
            // uuids
            // for the points
            for (String uuid : points.keySet()) {
                Map<String, Double> pointDetails = points.get(uuid);
                if (pointDetails != null) {
                    Double latitude = pointDetails.get("decimalLatitude");
                    Double longitude = pointDetails.get("decimalLongitude");

                    if (latitude != null && longitude != null) {
                        String wkt = "POINT(" + longitude + " " + latitude + ")";
                        jdbcTemplate.update("INSERT INTO temp_exp_dist_outliers VALUES (?, ST_GeographyFromText(?))", uuid, wkt);
                    }
                }
            }
            logger.debug("Finished inserting the points into the temp table for " + firstId + " " + jdbcTemplate.queryForInt("select count(*) from temp_exp_dist_outliers", (java.util.Map<String, String>) null));

            // return the distance of all points that are located outside the
            // expert distribution, and also are inside the bounding box for the
            // expert distribution - the bounds of the area for which the
            // expert distribution was generated.
            List<Map<String, Object>> outlierDistancesQueryResult = jdbcTemplate
                    .queryForList(
                            "SELECT id, ST_DISTANCE(point, (SELECT Geography(the_geom) from distributionshapes where id = ?)) as distance from temp_exp_dist_outliers where (SELECT bounding_box FROM distributiondata where geom_idx = ?) IS NULL OR ST_Intersects(point, Geography((SELECT bounding_box FROM distributiondata where geom_idx = ?)))",
                            expertDistributionShapeId, expertDistributionShapeId, expertDistributionShapeId);

            logger.debug("Finished running query to obtain outliers for " + firstId);

            for (Map<String, Object> queryResultRow : outlierDistancesQueryResult) {
                String uuid = (String) queryResultRow.get("id");
                Double distance = (Double) queryResultRow.get("distance");
                // Zero distance implies that the point is inside the
                // distribution
                if (distance > 0) {
                    outlierDistances.put(uuid, distance);
                }
            }
            logger.debug("Finished populating the map for " + firstId);
        } catch (EmptyResultDataAccessException ex) {
            throw new IllegalArgumentException("No expert distribution associated with lsid " + lsid, ex);
        }

        return outlierDistances;
    }

    @Resource(name = "dataSource")
    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new SimpleJdbcTemplate(dataSource);
    }

    public void setViewName(String viewName) {
        this.viewName = viewName;
    }
}
