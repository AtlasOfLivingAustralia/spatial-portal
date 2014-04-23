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

import org.ala.layers.dto.LayerPid;
import org.apache.log4j.Logger;
import org.springframework.jdbc.core.simple.ParameterizedBeanPropertyRowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * @author ajay
 */
@Service("layerPidDao")
public class LayerPidDAOImpl implements LayerPidDAO {

    /**
     * log4j logger
     */
    private static final Logger logger = Logger.getLogger(LayerPidDAOImpl.class);
    private SimpleJdbcTemplate jdbcTemplate;

    @Resource(name = "dataSource")
    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new SimpleJdbcTemplate(dataSource);
    }

    @Override
    public List<LayerPid> getLayers() {
        //return hibernateTemplate.find("from LayerPid");
        logger.info("Getting a list of all enabled layerpids");
        String sql = "select * from layerpids";
        return jdbcTemplate.query(sql, ParameterizedBeanPropertyRowMapper.newInstance(LayerPid.class));
    }

    @Override
    public LayerPid getLayerById(String id) {
        //List<LayerPid> layers = hibernateTemplate.find("from LayerPid id=?", id);
        logger.info("Getting enabled layerpids info for id = " + id);
        String sql = "select * from layerpids where id = ?";
        List<LayerPid> l = jdbcTemplate.query(sql, ParameterizedBeanPropertyRowMapper.newInstance(LayerPid.class), id);
        if (l.size() > 0) {
            return l.get(0);
        } else {
            return null;
        }
    }

    @Override
    public LayerPid getLayerByPid(String pid) {
        //List<LayerPid> layers = hibernateTemplate.find("from LayerPid pid=?", pid);
        logger.info("Getting enabled layerpids info for pid = " + pid);
        String sql = "select * from layerpids where pid = ?";
        List<LayerPid> l = jdbcTemplate.query(sql, ParameterizedBeanPropertyRowMapper.newInstance(LayerPid.class), pid);
        if (l.size() > 0) {
            return l.get(0);
        } else {
            return null;
        }
    }

}
