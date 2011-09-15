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
import org.ala.layers.dto.Objects;
import org.apache.log4j.Logger;
import org.springframework.jdbc.core.simple.ParameterizedBeanPropertyRowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.stereotype.Service;

/**
 *
 * @author ajay
 */
@Service("objectDao")
public class ObjectDAOImpl implements ObjectDAO {

    /** log4j logger */
    private static final Logger logger = Logger.getLogger(FieldDAOImpl.class);
    private SimpleJdbcTemplate jdbcTemplate;

    @Resource(name = "dataSource")
    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new SimpleJdbcTemplate(dataSource);
    }

    @Override
    public List<Objects> getObjects() {
        //return hibernateTemplate.find("from Objects");
        logger.info("Getting a list of all objects");
        String sql = "select pid, id, name, \"desc\" as description from objects";
        return jdbcTemplate.query(sql, ParameterizedBeanPropertyRowMapper.newInstance(Objects.class));
    }

    @Override
    public List<Objects> getObjectsById(String id) {
        //return hibernateTemplate.find("from Objects where id = ?", id);
        logger.info("Getting object info for fid = " + id);
        //String sql = "select * from objects where fid = ?";
        String sql = "select pid, id, name, \"desc\" as description from objects where fid = ?";
        return jdbcTemplate.query(sql, ParameterizedBeanPropertyRowMapper.newInstance(Objects.class), id);
    }

    @Override
    public String getObjectsGeometryById(String id, String geomtype) {
        logger.info("Getting object info for id = " + id + " and geometry as " + geomtype);
        String sql = "";
        if ("kml".equals(geomtype)) {
            sql = "SELECT ST_AsKml(the_geom) as geometry FROM objects WHERE pid=?;";
        } else if ("wkt".equals(geomtype)) {
            sql = "SELECT ST_AsText(the_geom) as geometry FROM objects WHERE pid=?;";
        } else if ("geojson".equals(geomtype)) {
            sql = "SELECT ST_AsGeoJSON(the_geom) as geometry FROM objects WHERE pid=?;";
        }
        
        List<Objects> l = jdbcTemplate.query(sql, ParameterizedBeanPropertyRowMapper.newInstance(Objects.class), id);
        if (l.size() > 0) {
            return l.get(0).getGeometry();
        } else {
            return null;
        }
    }
    
    @Override
    public Objects getObjectByPid(String pid) {
        //List<Objects> l = hibernateTemplate.find("from Objects where pid = ?", pid);
        logger.info("Getting object info for pid = " + pid);
        String sql = "select pid, id, name, \"desc\" as description from objects where pid = ?";
        List<Objects> l = jdbcTemplate.query(sql, ParameterizedBeanPropertyRowMapper.newInstance(Objects.class), pid);
        if (l.size() > 0) {
            return l.get(0);
        } else {
            return null; 
        }
    }

    @Override
    public Objects getObjectByIdAndLocation(String fid, Double lng, Double lat) {
        logger.info("Getting object info for fid = " + fid + " at loc: (" + lng + ", " + lat + ") ");
        String sql = "select pid, id, name, \"desc\" as description from objects where fid = ? and ST_Within(ST_SETSRID(ST_Point(?,?),4326), the_geom)";
        List<Objects> l = jdbcTemplate.query(sql, ParameterizedBeanPropertyRowMapper.newInstance(Objects.class), new Object[] {fid, lng, lat});
        if (l.size() > 0) {
            return l.get(0);
        } else {
            return null;
        }
    }

}
