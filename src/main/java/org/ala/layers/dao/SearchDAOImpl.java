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
import java.util.Map.Entry;
import javax.annotation.Resource;
import javax.sql.DataSource;

import org.ala.layers.dto.GridClass;
import org.ala.layers.dto.IntersectionFile;
import org.ala.layers.dto.SearchObject;
import org.apache.log4j.Logger;
import org.springframework.jdbc.core.simple.ParameterizedBeanPropertyRowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * @author ajay
 */
@Service("searchDao")
public class SearchDAOImpl implements SearchDAO {

    /**
     * log4j logger
     */
    private static final Logger logger = Logger.getLogger(SearchDAOImpl.class);
    private SimpleJdbcTemplate jdbcTemplate;
    //    private SimpleJdbcCall procSearchObject;
    @Resource(name = "layerIntersectDao")
    private LayerIntersectDAO layerIntersectDao;

    @Resource(name = "dataSource")
    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new SimpleJdbcTemplate(dataSource);
//        this.procSearchObject = new SimpleJdbcCall(dataSource).withProcedureName("searchobjectswithlimit")
//                .useInParameterNames("q", "lim");
    }

    @Override
    public List<SearchObject> findByCriteria(final String criteria, int limit) {
        //return hibernateTemplate.find("from SearchObject where ", this)

//        int limit = 20;
//
//        SqlParameterSource in = new MapSqlParameterSource()
//                .addValue("q", criteria)
//                .addValue("lim", limit);
//
//        Map m = procSearchObject.returningResultSet("searchobjectstype", ParameterizedBeanPropertyRowMapper.newInstance(SearchObject.class)).execute(in);
//
//        return (List<SearchObject>) m.get("searchobjectstype");

        logger.info("Getting search results for query: " + criteria);
        String sql = "select pid, id, name, \"desc\" as description, fid, fieldname from searchobjects(?,?)";
        return addGridClassesToSearch(jdbcTemplate.query(sql, ParameterizedBeanPropertyRowMapper.newInstance(SearchObject.class), "%" + criteria + "%", limit), criteria, limit);

    }

    private List<SearchObject> addGridClassesToSearch(List<SearchObject> search, String criteria, int limit) {
        criteria = criteria.toLowerCase();
        int maxPos = Integer.MAX_VALUE;
        int pos;
        for (SearchObject so : search) {
            pos = so.getName().toLowerCase().indexOf(criteria);
            if (pos >= 0 && pos < maxPos) {
                maxPos = pos;
            }
        }
        for (IntersectionFile f : layerIntersectDao.getConfig().getIntersectionFiles().values()) {
            if (f.getType().equalsIgnoreCase("a") && f.getClasses() != null) {
                //search
                for (Entry<Integer, GridClass> c : f.getClasses().entrySet()) {
                    if ((pos = c.getValue().getName().toLowerCase().indexOf(criteria)) >= 0) {
                        if (pos <= maxPos) {
                            search.add(SearchObject.create(
                                    f.getLayerPid() + ":" + c.getKey(),
                                    f.getLayerPid() + ":" + c.getKey(),
                                    c.getValue().getName(),
                                    null,
                                    f.getFieldId(),
                                    f.getFieldName()));
                        }
                    }
                }
            }
        }
        return search;
    }
}
