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

import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;
import javax.annotation.Resource;
import javax.sql.DataSource;

import org.ala.layers.dto.Field;
import org.ala.layers.dto.GridClass;
import org.ala.layers.dto.IntersectionFile;
import org.ala.layers.dto.Tabulation;
import org.ala.layers.tabulation.TabulationUtil;
import org.ala.layers.util.SpatialUtil;
import org.apache.log4j.Logger;
import org.springframework.jdbc.core.simple.ParameterizedBeanPropertyRowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * @author ajay
 */
@Service("tabulationDao")
public class TabulationDAOImpl implements TabulationDAO {

    /**
     * log4j logger
     */
    private static final Logger logger = Logger.getLogger(TabulationDAOImpl.class);
    private SimpleJdbcTemplate jdbcTemplate;

    @Resource(name = "layerIntersectDao")
    private LayerIntersectDAO layerIntersectDao;

    @Resource(name = "fieldDao")
    private FieldDAO fieldDao;

    @Resource(name = "dataSource")
    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new SimpleJdbcTemplate(dataSource);
    }

    @Override
    public List<Tabulation> getTabulation(String fid1, String fid2, String wkt) {
        List<Tabulation> tabulations = null;

        String min, max;
        if (fid1.compareTo(fid2) < 0) {
            min = fid1;
            max = fid2;
        } else {
            min = fid2;
            max = fid1;
        }

        if (wkt == null || wkt.length() == 0) {
            /*  after "tabulation" table is updated with column "occurrences" and column "species"
             * String sql = "SELECT i.pid1, i.pid2, i.fid1, i.fid2, i.area, i.occurrences, i.species, o1.name as name1, o2.name as name2 FROM "
                    + "(SELECT * FROM tabulation WHERE fid1= ? AND fid2 = ? ) i, "
                    + "(SELECT * FROM objects WHERE fid= ? ) o1, "
                    + "(SELECT * FROM objects WHERE fid= ? ) o2 "
                    + "WHERE i.pid1=o1.pid AND i.pid2=o2.pid ;";
                    * 
                    */
            /* before "tabulation" table is updated with column "occurrences", to just make sure column "area" is all good */
            String sql = "SELECT i.pid1, i.pid2, i.fid1, i.fid2, i.area, o1.name as name1, o2.name as name2, i.occurrences, i.species FROM "
                    + "(SELECT pid1, pid2, fid1, fid2, area, occurrences, species FROM tabulation WHERE fid1= ? AND fid2 = ? ) i, "
                    + "(select t1.pid1 as pid, name from tabulation t1 left join objects o3 on t1.fid1=o3.fid and t1.pid1=o3.pid where t1.fid1= ? group by t1.pid1, name) o1, "
                    //+ "(SELECT pid, name FROM objects WHERE fid= ? ) o1, "
                    + "(select t2.pid2 as pid, name from tabulation t2 left join objects o4 on t2.fid2=o4.fid and t2.pid2=o4.pid where t2.fid2= ? group by t2.pid2, name) o2 "
                    //+ "(SELECT pid, name FROM objects WHERE fid= ? ) o2 "
                    + "WHERE i.pid1=o1.pid AND i.pid2=o2.pid ;";

            tabulations = jdbcTemplate.query(sql, ParameterizedBeanPropertyRowMapper.newInstance(Tabulation.class), min, max, min, max);

        } else {
            String sql = "SELECT fid1, pid1, fid2, pid2, ST_AsText(newgeom) as geometry, name1, name2, occurrences, species FROM "
                    + "(SELECT fid1, pid1, fid2, pid2, (ST_INTERSECTION(ST_GEOMFROMTEXT( ? ,4326), i.the_geom)) as newgeom, o1.name as name1, o2.name as name2, i.occurrences, i.species FROM "
                    + "(SELECT * FROM tabulation WHERE fid1= ? AND fid2 = ? ) i, "
                    + "(select t1.pid1 as pid, name from tabulation t1 left join objects o3 on t1.fid1=o3.fid and t1.pid1=o3.pid where t1.fid1= ? group by t1.pid1, name) o1, "
                    //+ "(SELECT pid, name FROM objects WHERE fid= ? ) o1, "
                    + "(select t2.pid2 as pid, name from tabulation t2 left join objects o4 on t2.fid2=o4.fid and t2.pid2=o4.pid where t2.fid2= ? group by t2.pid2, name) o2 "
                    //+ "(SELECT pid, name FROM objects WHERE fid= ? ) o2 "
                    + "WHERE i.pid1=o1.pid AND i.pid2=o2.pid) a "
                    + "WHERE a.newgeom is not null AND ST_Area(a.newgeom) > 0;";

            tabulations = jdbcTemplate.query(sql, ParameterizedBeanPropertyRowMapper.newInstance(Tabulation.class), wkt, min, max, min, max);

            for (Tabulation t : tabulations) {
                try {
                    t.setArea(SpatialUtil.calculateArea(t.getGeometry()));
                    t.setOccurrences(TabulationUtil.calculateOccurrences(layerIntersectDao.getConfig().getOccurrenceSpeciesRecordsFilename(), t.getGeometry()));
                    t.setSpecies(TabulationUtil.calculateSpecies(layerIntersectDao.getConfig().getOccurrenceSpeciesRecordsFilename(), t.getGeometry()));
                } catch (Exception e) {
                    logger.error("fid1:" + fid1 + " fid2:" + fid2 + " wkt:" + wkt, e);
                }
            }
        }

        //fill in 'name' for 'grids as classes'/fields.type='a'/pids with ':'
        IntersectionFile f = layerIntersectDao.getConfig().getIntersectionFile(min);
        if (f.getType().equals("a")) {
            for (Tabulation t : tabulations) {
                t.setName1(f.getClasses().get(Integer.parseInt(t.getPid1().split(":")[1])).getName());
            }
        }
        f = layerIntersectDao.getConfig().getIntersectionFile(max);
        if (f.getType().equals("a")) {
            for (Tabulation t : tabulations) {
                t.setName2(f.getClasses().get(Integer.parseInt(t.getPid2().split(":")[1])).getName());
            }
        }

        return tabulations;
    }

    @Override
    public List<Tabulation> listTabulations() {
        String incompleteTabulations = "select fid1, fid2 from tabulation where area is null and the_geom is not null group by fid1, fid2";
        String sql = "SELECT fid1, fid2, f1.name as name1, f2.name as name2 "
                + " FROM (select t1.* from "
                + "(select fid1, fid2, sum(area) a from tabulation group by fid1, fid2) t1 left join "
                + " (" + incompleteTabulations + ") i on t1.fid1=i.fid1 and t1.fid2=i.fid2 where i.fid1 is null"
                + ") t"
                + ", fields f1, fields f2 "
                + " WHERE f1.id = fid1 AND f2.id = fid2 AND a > 0 "
                + " AND f1.intersect=true AND f2.intersect=true "
                + " GROUP BY fid1, fid2, name1, name2;";

        return jdbcTemplate.query(sql, ParameterizedBeanPropertyRowMapper.newInstance(Tabulation.class));
    }

    @Override
    public List<Tabulation> getTabulationSingle(String fid, String wkt) {
        if (wkt != null && wkt.length() > 0) {
            String sql = "SELECT fid as fid1, pid as pid1, name as name1,"
                    + " 'user area' as fid2, 'user area' as pid2, 'user area' as name2, "
                    + " ST_AsText(newgeom) as geometry FROM "
                    + "(SELECT fid, pid, name, (ST_INTERSECTION(ST_GEOMFROMTEXT( ? ,4326), the_geom)) as newgeom FROM "
                    + "objects WHERE fid= ? ) o "
                    + "WHERE newgeom is not null AND ST_Area(newgeom) > 0;";

            List<Tabulation> tabulations = jdbcTemplate.query(sql, ParameterizedBeanPropertyRowMapper.newInstance(Tabulation.class), wkt, fid);

            for (Tabulation t : tabulations) {
                try {
                    t.setArea(SpatialUtil.calculateArea(t.getGeometry()));
                } catch (Exception e) {
                    logger.error("fid:" + fid + " wkt:" + wkt, e);
                }
            }

            return tabulations;
        } else {
            String sql = "SELECT fid1, pid1, name as name1,"
                    + " 'world' as fid2, 'world' as pid2, 'world' as name2, "
                    + " ST_AsText(newgeom) as geometry, area_km as area FROM "
                    + "(SELECT name, fid as fid1, pid as pid1, the_geom as newgeom, area_km FROM "
                    + "objects WHERE fid= ? ) t "
                    + "WHERE newgeom is not null AND ST_Area(newgeom) > 0;";

            List<Tabulation> tabulations = jdbcTemplate.query(sql, ParameterizedBeanPropertyRowMapper.newInstance(Tabulation.class), fid);

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
