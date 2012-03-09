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
import java.util.Map;
import javax.annotation.Resource;
import javax.sql.DataSource;
import org.ala.layers.dto.SearchObject;
import org.apache.log4j.Logger;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.SqlTypeValue;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.ParameterizedBeanPropertyRowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.jdbc.core.support.AbstractSqlTypeValue;
import org.springframework.stereotype.Service;

/**
 *
 * @author ajay
 */
@Service("searchDao")
public class SearchDAOImpl implements SearchDAO {

    /** log4j logger */
    private static final Logger logger = Logger.getLogger(SearchDAOImpl.class);
    private SimpleJdbcTemplate jdbcTemplate;
//    private SimpleJdbcCall procSearchObject;

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
        return jdbcTemplate.query(sql, ParameterizedBeanPropertyRowMapper.newInstance(SearchObject.class), "%"+criteria+"%",limit);

    }
}
