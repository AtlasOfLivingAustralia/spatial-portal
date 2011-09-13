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
import org.ala.layers.dto.Layer;
import org.apache.log4j.Logger;
import org.springframework.jdbc.core.simple.ParameterizedBeanPropertyRowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.stereotype.Service;

/**
 *
 * @author ajay
 */
@Service("layerDao")
public class LayerDAOImpl implements LayerDAO {

    /** log4j logger */
    private static final Logger logger = Logger.getLogger(LayerDAOImpl.class);
    private SimpleJdbcTemplate jdbcTemplate;

    @Resource(name = "dataSource")
    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new SimpleJdbcTemplate(dataSource);
    }

    @Override
    public List<Layer> getLayers() {
        //return hibernateTemplate.find("from Layer where enabled=true");
        logger.info("Getting a list of all enabled layers");
        String sql = "select * from layers where enabled=true";
        List<Layer> l = jdbcTemplate.query(sql, ParameterizedBeanPropertyRowMapper.newInstance(Layer.class));
        System.out.println("***********************************");
        System.out.println("Got " + l.size() + " layers to display");
        System.out.println("***********************************");
        return l;
    }

    @Override
    public Layer getLayerById(int id) {
        //List<Layer> layers = hibernateTemplate.find("from Layer where enabled=true and id=?", id);
        logger.info("Getting enabled layer info for id = " + id);
        String sql = "select * from layers where enabled=true and id = ?";
        List<Layer> l = jdbcTemplate.query(sql, ParameterizedBeanPropertyRowMapper.newInstance(Layer.class), id);
        if (l.size() > 0) {
            return l.get(0);
        } else {
            return null;
        }
    }

    @Override
    public Layer getLayerByName(String name) {
        //List<Layer> layers = hibernateTemplate.find("from Layer where enabled=true and name=?", name);

        logger.info("Getting enabled layer info for name = " + name);
        String sql = "select * from layers where enabled=true and name = ?";
        List<Layer> l = jdbcTemplate.query(sql, ParameterizedBeanPropertyRowMapper.newInstance(Layer.class), name);
        if (l.size() > 0) {
            return l.get(0);
        } else {
            return null;
        }
    }

    @Override
    public List<Layer> getLayersByEnvironment() {
        //return hibernateTemplate.find("from Layer where enabled=true and type='Environmental'");
        String type = "Environmental";
        logger.info("Getting a list of all enabled environmental layers");
        String sql = "select * from layers where enabled=true and type = ?";
        return jdbcTemplate.query(sql, ParameterizedBeanPropertyRowMapper.newInstance(Layer.class), type);
    }

    @Override
    public List<Layer> getLayersByContextual() {
        //return hibernateTemplate.find("from Layer where enabled=true and type='Contextual'");
        String type = "Contextual";
        logger.info("Getting a list of all enabled Contextual layers");
        String sql = "select * from layers where enabled=true and type = ?";
        return jdbcTemplate.query(sql, ParameterizedBeanPropertyRowMapper.newInstance(Layer.class), type);
    }
}
