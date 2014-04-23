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
package org.ala.layers.web;

import java.util.List;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.ala.layers.dao.FieldDAO;
import org.ala.layers.dao.ObjectDAO;
import org.ala.layers.dto.Field;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author Adam
 */
@Controller
public class FieldsService {

    private final String WS_FIELDS = "/fields";
    private final String WS_FIELDS_DB = "/fieldsdb";
    private final String WS_FIELD_ID = "/field/{id}";
    /**
     * Log4j instance
     */
    protected Logger logger = Logger.getLogger(this.getClass());

    @Resource(name = "fieldDao")
    private FieldDAO fieldDao;

    @Resource(name = "objectDao")
    private ObjectDAO objectDao;

    /*
     * list fields table
     */
    @RequestMapping(value = WS_FIELDS, method = RequestMethod.GET)
    public
    @ResponseBody
    List<Field> listFields(HttpServletRequest req) {

//        String query = "SELECT * FROM fields WHERE enabled=TRUE;";
//        ResultSet r = DBConnection.query(query);
//        return Utils.resultSetToJSON(r);

        return fieldDao.getFields();
    }

    /*
     * list fields table with db only records
     */
    @RequestMapping(value = WS_FIELDS_DB, method = RequestMethod.GET)
    public
    @ResponseBody
    List<Field> listFieldsDBOnly(HttpServletRequest req) {

//        String query = "SELECT * FROM fields WHERE enabled=TRUE AND indb=TRUE;";
//        ResultSet r = DBConnection.query(query);
//        return Utils.resultSetToJSON(r);

        return fieldDao.getFieldsByDB();
    }

    /*
     * one fields table record
     */
    @RequestMapping(value = WS_FIELD_ID, method = RequestMethod.GET)
    public
    @ResponseBody
    Field oneField(@PathVariable("id") String id, HttpServletRequest req) {
        logger.info("calling /field/" + id);
        //test field id value
        int len = Math.min(6, id.length());
        id = id.substring(0, len);
        char prefix = id.toUpperCase().charAt(0);
        String number = id.substring(2, len);
        boolean numberOk = false;
        try {
            int i = Integer.parseInt(number);
            numberOk = true;
        } catch (Exception e) {
            logger.error("An error has occurred");
            logger.error(ExceptionUtils.getFullStackTrace(e));
        }

        if (prefix <= 'Z' && prefix >= 'A' && numberOk) {

            //Adam: not sure if this was correct
            //String query = "SELECT pid, id, name, \"desc\" FROM objects WHERE fid='" + id + "';";
//            String query = "SELECT * from fields WHERE enabled=TRUE and id = '" + id + "';";
//            logger.debug("Executing sql: " + query);
//            ResultSet r = DBConnection.query(query);
//            return Utils.resultSetToJSON(r);

            Field f = fieldDao.getFieldById(id);
            f.setObjects(objectDao.getObjectsById(id));
            return f;
        } else {
            //error
            return null;
        }
    }
}
