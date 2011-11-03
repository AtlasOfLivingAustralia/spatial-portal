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
import javax.annotation.Resource;
import javax.sql.DataSource;
import org.ala.layers.dto.Tabulation;
import org.ala.layers.tabulation.TabulationUtil;
import org.apache.log4j.Logger;
import org.springframework.jdbc.core.simple.ParameterizedBeanPropertyRowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.stereotype.Service;

/**
 *
 * @author ajay
 */
@Service("tabulationDao")
public class TabulationDAOImpl implements TabulationDAO {

    /** log4j logger */
    private static final Logger logger = Logger.getLogger(FieldDAOImpl.class);
    private SimpleJdbcTemplate jdbcTemplate;

    @Resource(name = "dataSource")
    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new SimpleJdbcTemplate(dataSource);
    }

    @Override
    public List<Tabulation> getTabulation(String fid1, String fid2, String wkt) {
        String min, max;
        if (fid1.compareTo(fid2) < 0) {
            min = fid1;
            max = fid2;
        } else {
            min = fid2;
            max = fid1;
        }

        if(wkt == null || wkt.length() == 0) {
            String sql = "SELECT i.*, o1.name as name1, o2.name as name2 FROM "
                    + "(SELECT * FROM tabulation WHERE fid1= ? AND fid2 = ? ) i, "
                    + "(SELECT * FROM objects WHERE fid= ? ) o1, "
                    + "(SELECT * FROM objects WHERE fid= ? ) o2 "
                    + "WHERE i.pid1=o1.pid AND i.pid2=o2.pid;";

            return jdbcTemplate.query(sql, ParameterizedBeanPropertyRowMapper.newInstance(Tabulation.class), min, max, min, max);
        } else {
            String sql = "SELECT fid1, pid1, fid2, pid2, ST_AsText(newgeom) as geometry, name1, name2 FROM "
                    + "(SELECT fid1, pid1, fid2, pid2, (ST_INTERSECTION(ST_GEOMFROMTEXT( ? ,4326), i.the_geom)) as newgeom, o1.name as name1, o2.name as name2 FROM "
                        + "(SELECT * FROM tabulation WHERE fid1= ? AND fid2 = ? ) i, "
                        + "(SELECT * FROM objects WHERE fid= ? ) o1, "
                        + "(SELECT * FROM objects WHERE fid= ? ) o2 "
                        + "WHERE i.pid1=o1.pid AND i.pid2=o2.pid) a "
                    + "WHERE a.newgeom is not null AND ST_Area(a.newgeom) > 0;";

            List<Tabulation> tabulations = jdbcTemplate.query(sql, ParameterizedBeanPropertyRowMapper.newInstance(Tabulation.class), wkt, min, max, min, max);

            for(Tabulation t : tabulations) {
                try {
                    t.setArea(TabulationUtil.calculateArea(t.getGeometry()));
                } catch (Exception e) {
                    logger.error("fid1:" + fid1 + " fid2:" + fid2 + " wkt:" + wkt, e);
                }
            }

            return tabulations;
        }
    }

    @Override
    public List<Tabulation> listTabulations(){
        String sql = "SELECT fid1, fid2, f1.name as name1, f2.name as name2 "
                + "FROM tabulation, fields f1, fields f2 "
                + "WHERE f1.id = fid1 AND f2.id = fid2 AND the_geom is not null "
                + "GROUP BY fid1, fid2, name1, name2;";

        return jdbcTemplate.query(sql, ParameterizedBeanPropertyRowMapper.newInstance(Tabulation.class));
    }

    @Override
    public List<Tabulation> getTabulationSingle(String fid, String wkt) {
        if(wkt != null && wkt.length() > 0) {
            String sql = "SELECT fid as fid1, pid as pid1, name as name1,"
                    + " 'user area' as fid2, 'user area' as pid2, 'user area' as name2, "
                    + " ST_AsText(newgeom) as geometry FROM "
                    + "(SELECT fid, pid, name, (ST_INTERSECTION(ST_GEOMFROMTEXT( ? ,4326), the_geom)) as newgeom FROM "
                        + "objects WHERE fid= ? ) o "
                    + "WHERE newgeom is not null AND ST_Area(newgeom) > 0;";

            List<Tabulation> tabulations = jdbcTemplate.query(sql, ParameterizedBeanPropertyRowMapper.newInstance(Tabulation.class), wkt, fid);

            for(Tabulation t : tabulations) {
                try {
                    t.setArea(TabulationUtil.calculateArea(t.getGeometry()));
                } catch (Exception e) {
                    logger.error("fid:" + fid + " wkt:" + wkt, e);
                }
            }

            return tabulations;
        } else {
            String sql = "SELECT fid1, pid1, name as name1,"
                    + " 'world' as fid2, 'world' as pid2, 'world' as name2, "
                    + " ST_AsText(newgeom) as geometry, area_km as area FROM "
                    + "(SELECT name, fid as fid1, pid as pid1, the_geom as newgeom FROM "
                        + "objects WHERE fid= ? ) t "
                    + "WHERE newgeom is not null AND ST_Area(newgeom) > 0;";

            List<Tabulation> tabulations = jdbcTemplate.query(sql, ParameterizedBeanPropertyRowMapper.newInstance(Tabulation.class), fid, fid);

            //objects table area ok to use
//            for(Tabulation t : tabulations) {
//                try {
//                    t.setArea(TabulationUtil.calculateArea(t.getGeometry()));
//                } catch (Exception e) {
//                    logger.error("fid:" + fid, e);
//                }
//            }

            return tabulations;
        }
    }
}
