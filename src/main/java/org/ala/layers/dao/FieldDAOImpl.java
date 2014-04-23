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

import org.ala.layers.dto.Field;
import org.apache.log4j.Logger;
import org.springframework.jdbc.core.simple.ParameterizedBeanPropertyRowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * @author ajay
 */
@Service("fieldDao")
public class FieldDAOImpl implements FieldDAO {

    /**
     * log4j logger
     */
    private static final Logger logger = Logger.getLogger(FieldDAOImpl.class);

    private SimpleJdbcTemplate jdbcTemplate;

    @Resource(name = "layerIntersectDao")
    private LayerIntersectDAO layerIntersectDao;

    @Resource(name = "dataSource")
    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new SimpleJdbcTemplate(dataSource);
    }

    @Override
    public List<Field> getFields() {
        logger.info("Getting a list of all enabled fields");
        String sql = "select * from fields where enabled=true";
        return jdbcTemplate.query(sql, ParameterizedBeanPropertyRowMapper.newInstance(Field.class));
    }

    @Override
    public Field getFieldById(String id) {
        logger.info("Getting enabled field info for id = " + id);
        String sql = "select * from fields where enabled=true and id = ?";
        List<Field> l = jdbcTemplate.query(sql, ParameterizedBeanPropertyRowMapper.newInstance(Field.class), id);
        if (l.size() > 0) {
            return l.get(0);
        } else {
            return null;
        }
    }

    @Override
    public List<Field> getFieldsByDB() {
        if (layerIntersectDao.getConfig().getLayerIndexUrl() != null) {
            return layerIntersectDao.getConfig().getFieldsByDB();
        } else {
            //return hibernateTemplate.find("from Field where enabled=true and indb=true");
            logger.info("Getting a list of all enabled fields with indb");
            String sql = "select * from fields where enabled=TRUE and indb=TRUE";
            return jdbcTemplate.query(sql, ParameterizedBeanPropertyRowMapper.newInstance(Field.class));
        }

    }
}
