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
import org.ala.layers.dao.LayerDAO;
import org.ala.layers.dto.Layer;
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
public class LayersService {
    /**
     * Log4j instance
     */
    protected Logger logger = Logger.getLogger(this.getClass());
     
    private LayerDAO layerDao;

    @Autowired
    public void setLayersDao(LayerDAO layerDao) {
        System.out.println("setting layer dao");
        this.layerDao = layerDao;
    }

    /**This method returns all layers
     * 
     * @param req
     * @return 
     */
    @RequestMapping(value = "/layers", method = RequestMethod.GET)
    public ModelMap layerObjects(HttpServletRequest req) {
        ModelMap modelMap = new ModelMap();
        List<Layer> layers = layerDao.getLayers();
        modelMap.addAttribute("layers", layers);
        return modelMap;
    }
    
    /**This method returns a single layer, provided an id
     * 
     * @param uuid
     * @param req
     * @return 
     */
    @RequestMapping(value = "/layer/{id}", method = RequestMethod.GET)
    public ModelMap layerObject(@PathVariable("id") int id, HttpServletRequest req) {
        ModelMap modelMap = new ModelMap();
        Layer layers = layerDao.getLayerById(id);
        modelMap.addAttribute("layer", layers);
        return modelMap;
    }
    
    @RequestMapping(value = "/layers/grids", method = RequestMethod.GET)
    public ModelMap gridsLayerObjects(HttpServletRequest req) {
//        String query = "SELECT * FROM layers WHERE enabled='TRUE' and type='Environmental';";
//        ResultSet r = DBConnection.query(query);
//        return Utils.resultSetToJSON(r);
        ModelMap modelMap = new ModelMap();
        List<Layer> layers = layerDao.getLayersByEnvironment();
        modelMap.addAttribute("layers", layers);
        return modelMap;
    }
        
    @RequestMapping(value = "/layers/shapes", method = RequestMethod.GET)
    public ModelMap shapesLayerObjects(HttpServletRequest req) {
//        String query = "SELECT * FROM layers WHERE enabled='TRUE' and type='Contextual';";
//        ResultSet r = DBConnection.query(query);
//        return Utils.resultSetToJSON(r);
        ModelMap modelMap = new ModelMap();
        List<Layer> layers = layerDao.getLayersByContextual();
        modelMap.addAttribute("layers", layers);
        return modelMap;

    }
    
}
