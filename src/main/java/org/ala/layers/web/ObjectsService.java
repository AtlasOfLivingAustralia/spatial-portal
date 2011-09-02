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

import java.sql.ResultSet;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.ala.layers.dao.ObjectDAO;
import org.ala.layers.dto.Objects;
import org.ala.layers.util.DBConnection;
import org.ala.layers.util.Utils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 *
 * @author jac24n
 */
@Controller
public class ObjectsService {
    /**
     * Log4j instance
     */
    protected Logger logger = Logger.getLogger(this.getClass());

    private ObjectDAO objectDao;

    @Autowired
    public void setObjectsDao(ObjectDAO objectDao) {
        System.out.println("setting layer dao");
        this.objectDao = objectDao;
    }

     /**This method returns all objects associated with a field
     * 
     * @param id
     * @param req
     * @return 
     */
    @RequestMapping(value = "/objects/{id}", method = RequestMethod.GET)
    public ModelMap fieldObjects(@PathVariable("id") String id, HttpServletRequest req) {
//        String query = "SELECT pid, id, name, \"desc\" FROM objects WHERE fid='" + id + "';";
//        ResultSet r = DBConnection.query(query);
//        return Utils.resultSetToJSON(r);
        ModelMap modelMap = new ModelMap();
        List<Objects> objects = objectDao.getObjectsById(id);
        modelMap.addAttribute("objects", objects);
        return modelMap;
    }
    
    /**This method returns a single object, provided a UUID
     * 
     * @param uuid
     * @param req
     * @return 
     */
    @RequestMapping(value = "/object/{pid}", method = RequestMethod.GET)
    public ModelMap fieldObject(@PathVariable("pid") String pid, HttpServletRequest req) {
//        String query = "SELECT pid, id, name, \"desc\" FROM objects WHERE pid='" + pid + "';";
//        ResultSet r = DBConnection.query(query);
//        return Utils.resultSetToJSON(r);
        ModelMap modelMap = new ModelMap();
        Objects objects = objectDao.getObjectByPid(pid); 
        modelMap.addAttribute("objects", objects);
        return modelMap;
    }    
}
