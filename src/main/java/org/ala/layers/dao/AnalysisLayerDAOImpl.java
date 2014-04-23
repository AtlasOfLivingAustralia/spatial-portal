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

import org.ala.layers.dto.AnalysisLayer;
import org.ala.layers.intersect.SimpleRegion;
import org.apache.log4j.Logger;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * @author ajay
 */
@Service("analysislayerDao")
public class AnalysisLayerDAOImpl implements AnalysisLayerDAO {

    /**
     * log4j logger
     */
    private static final Logger logger = Logger.getLogger(AnalysisLayerDAOImpl.class);
    private SimpleJdbcTemplate jdbcTemplate;
    @Resource(name = "layerIntersectDao")
    private LayerIntersectDAO layerIntersectDao;
    @Resource(name = "layerDao")
    private LayerDAO layerDao;
    @Resource(name = "fieldDao")
    private FieldDAO fieldDao;

    @Resource(name = "dataSource")
    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new SimpleJdbcTemplate(dataSource);
    }

    @Override
    public List<AnalysisLayer> getAnalysisLayersByName(String[] layers) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<AnalysisLayer> getAnalysisLayersByEnvCategory(String env_category) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<Double> getResolutions(List<AnalysisLayer> layers) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<String> getEnvCategories(List<AnalysisLayer> layers) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String cutGridFiles(List<AnalysisLayer> layers, SimpleRegion region) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
