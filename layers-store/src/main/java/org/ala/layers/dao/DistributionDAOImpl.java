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

import java.util.*;
import javax.annotation.Resource;
import javax.sql.DataSource;
import org.ala.layers.dto.Distribution;
import org.apache.log4j.Logger;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.ParameterizedBeanPropertyRowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.stereotype.Service;

/**
 *
 * @author ajay
 */
@Service("distributionDao")
public class DistributionDAOImpl implements DistributionDAO {

    /** log4j logger */
    private static final Logger logger = Logger.getLogger(DistributionDAOImpl.class);
    private SimpleJdbcTemplate jdbcTemplate;

    @Resource(name = "dataSource")
    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new SimpleJdbcTemplate(dataSource);
    }

    @Override
    public List<Distribution> queryDistributions(String wkt, double min_depth, double max_depth, Integer geomIdx, String lsids, String type) {
        logger.info("Getting distributions list");

        ArrayList<Object> params = new ArrayList<Object>();
        StringBuilder where = new StringBuilder();

        if (geomIdx != null && geomIdx >= 0) {
            where.append(" geom_idx = ? ");
            params.add(geomIdx);
        }

        if (lsids != null && lsids.length() > 0) {
            if (where.length() > 0) {
                where.append(" AND ");
            }
            where.append(" ? LIKE '% '||lsid||' %'  ");
            params.add(" " + lsids.replace(",", " ") + " ");
        }

        if (min_depth != -1 && max_depth != -1) {
            if (where.length() > 0) {
                where.append(" AND ");
            }
            where.append("min_depth<= ? AND max_depth>= ? ");
            params.add(new Double(max_depth));
            params.add(new Double(min_depth));
        } else if (min_depth != -1) {
            if (where.length() > 0) {
                where.append(" AND ");
            }
            where.append("max_depth>= ? ");
            params.add(new Double(min_depth));
        } else if (max_depth != -1) {
            if (where.length() > 0) {
                where.append(" AND ");
            }
            where.append("min_depth<= ? ");
            params.add(new Double(max_depth));
        }

        if (wkt != null && wkt.length() > 0) {
            if (where.length() > 0) {
                where.append(" AND ");
            }
            where.append("ST_INTERSECTS(the_geom, ST_GEOMFROMTEXT( ? ))");
            params.add(wkt);
        }

        String sql = "select gid,spcode,scientific,authority_,common_nam,\"family\",genus_name,specific_n,min_depth,max_depth,pelagic_fl,metadata_u,wmsurl,lsid,type,area_name,pid,checklist_name,area_km, notes, geom_idx from distributions ";
        if (where.length() > 0) {
            sql += " WHERE " + where.toString() + " AND type = ? ";
        } else {
            sql += " WHERE type= ? ";
        }

        List<Distribution> distributions = null;

        if (params.size() == 0) {
            distributions = jdbcTemplate.query(sql, ParameterizedBeanPropertyRowMapper.newInstance(Distribution.class), type);
        } else if (params.size() == 1) {
            distributions = jdbcTemplate.query(sql, ParameterizedBeanPropertyRowMapper.newInstance(Distribution.class), params.get(0), type);
        } else if (params.size() == 2) {
            distributions = jdbcTemplate.query(sql, ParameterizedBeanPropertyRowMapper.newInstance(Distribution.class), params.get(0), params.get(1), type);
        } else if (params.size() == 3) {
            distributions = jdbcTemplate.query(sql, ParameterizedBeanPropertyRowMapper.newInstance(Distribution.class), params.get(0), params.get(1), params.get(2), type);
        } else if (params.size() == 4) {
            distributions = jdbcTemplate.query(sql, ParameterizedBeanPropertyRowMapper.newInstance(Distribution.class), params.get(0), params.get(1), params.get(2), params.get(3), type);
        } else if (params.size() == 5) {
            distributions = jdbcTemplate.query(sql, ParameterizedBeanPropertyRowMapper.newInstance(Distribution.class), params.get(0), params.get(1), params.get(2), params.get(3), params.get(4), type);
        }

        return distributions;
    }

    @Override
    public Distribution getDistributionBySpcode(long spcode, String type) {
        String sql = "select gid,spcode,scientific,authority_,common_nam,\"family\",genus_name,specific_n,min_depth,max_depth,pelagic_fl,metadata_u,wmsurl,lsid,type,area_name,pid, ST_AsText(the_geom) as geometry, checklist_name, area_km, notes, geom_idx from distributions where spcode= ? and type= ? ";
        List<Distribution> d = jdbcTemplate.query(sql, ParameterizedBeanPropertyRowMapper.newInstance(Distribution.class), (double) spcode, type);
        if (d.size() > 0) {
            return d.get(0);
        }
        return null;
    }

    /**
     * Query by radius
     * @return set of species with distributions intersecting the radius
     */
    public List<Distribution> queryDistributionsByRadius(float longitude, float latitude, float radiusInMetres, double min_depth, double max_depth, Integer geomIdx, String lsids, String type){
        logger.info("Getting distributions list with a radius");

        Map<String, Object> params = new HashMap<String,Object>();
        params.put("longitude", longitude);
        params.put("latitude", latitude);
        params.put("radius", radiusInMetres);
        params.put("type", type);

        String sql = "select gid,spcode,scientific,authority_,common_nam,\"family\",genus_name,specific_n,min_depth,max_depth,pelagic_fl,metadata_u,wmsurl,lsid,type,area_name,pid,checklist_name,area_km, notes, geom_idx from distributions where ST_DWithin(the_geom, ST_MakePoint(:longitude,:latitude),:radius) AND type = :type ";

        //add additional criteria
        StringBuilder whereClause = new StringBuilder();
        constructWhereClause(min_depth, max_depth, geomIdx, lsids, params, whereClause);
        if(whereClause.length()>0){
            sql += "AND " + whereClause.toString();
        }
        return jdbcTemplate.query(sql, ParameterizedBeanPropertyRowMapper.newInstance(Distribution.class), params);
    }

    @Override
    public List<Distribution> getDistributionByLSID(String[] lsids) {
        String sql = "select gid,spcode,scientific,authority_,common_nam,\"family\",genus_name,specific_n,min_depth,max_depth,pelagic_fl,metadata_u,wmsurl,lsid,type,area_name,pid,checklist_name,area_km, notes, geom_idx from distributions where lsid IN (:lsids)";
        Map<String, Object> params = new HashMap<String,Object>();
        params.put("lsids", Arrays.asList(lsids));
        params.put("type", Distribution.EXPERT_DISTRIBUTION);
        return jdbcTemplate.query(sql, ParameterizedBeanPropertyRowMapper.newInstance(Distribution.class), params);
    }

    /**
     *
     * @param min_depth
     * @param max_depth
     * @param geomIdx
     * @param lsids
     * @param params
     * @param where
     */
    private void constructWhereClause(double min_depth, double max_depth, Integer geomIdx, String lsids, Map<String,Object> params, StringBuilder where) {
        if (geomIdx != null && geomIdx >= 0) {
            where.append(" geom_idx = :geom_idx ");
            params.put("geom_idx", geomIdx);
        }

        if (lsids != null && lsids.length() > 0) {
            if (where.length() > 0) {
                where.append(" AND ");
            }
            where.append(":lsids LIKE '% '||lsid||' %'  ");
            params.put("lsids", " " + lsids.replace(",", " ") + " ") ;
        }

        if (min_depth != -1 && max_depth != -1) {
            if (where.length() > 0) {
                where.append(" AND ");
            }
            where.append("min_depth <= :min_depth AND max_depth >= :max_depth ");
            params.put("max_depth", new Double(max_depth));
            params.put("min_depth", new Double(min_depth));
        } else if (min_depth != -1) {
            if (where.length() > 0) {
                where.append(" AND ");
            }
            where.append("max_depth >= :max_depth ");
            params.put("max_depth", new Double(min_depth));
        } else if (max_depth != -1) {
            if (where.length() > 0) {
                where.append(" AND ");
            }
            where.append("min_depth<= :min_depth ");
            params.put("min_depth", new Double(max_depth));
        }
    }
}
