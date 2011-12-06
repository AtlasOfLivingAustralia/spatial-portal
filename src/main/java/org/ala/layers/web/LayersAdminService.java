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

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import org.ala.layers.dao.LayerDAO;
import org.ala.layers.dto.Layer;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

/**
 *
 * @author ajay
 */
@Controller
public class LayersAdminService {
    /**
     * Log4j instance
     */
    protected Logger logger = Logger.getLogger(this.getClass());
    @Resource(name = "layerDao")
    private LayerDAO layerDao;

    /**
     * This method returns all layers
     *
     * @return
     */
    @RequestMapping(value = "/admin/layers", method = RequestMethod.GET)
    public ModelAndView layerObjects() {
        logger.info("Retriving all layers");
        return new ModelAndView("layers/list", "layers", layerDao.getLayersForAdmin());
    }

    /**
     * This method returns an edit view of the selected layer
     *
     * @param req
     * @return
     */
    @RequestMapping(value = "/admin/layers/edit/{id}", method = RequestMethod.GET)
    public ModelAndView showLayerEditPage(@PathVariable int id) {
        logger.info("Retriving all layers");
        return new ModelAndView("layers/edit", "layer", layerDao.getLayerByIdForAdmin(id));
    }

    @RequestMapping(value = "/admin/layers/edit/{id}", method = RequestMethod.POST)
    public String updateLayer(Layer layer) {
        //LayerInfo layer = new LayerInfo();

        System.out.println("editing existing layer object: " + layer.getName());

        layerDao.updateLayer(layer);
        return "redirect:/admin/layers";
    }

    /**
     * This method returns an add view a new layer
     *
     * @param req
     * @return
     */
    @RequestMapping(value = "/admin/layers/add", method = RequestMethod.GET)
    public String showLayerAddPage() {
        logger.info("display new layer form");
        return "layers/add";
    }

    /**
     * This method returns an add view a new layer
     *
     * @param req
     * @return
     */
    @RequestMapping(value = "/admin/layers/add", method = RequestMethod.POST)
    public String addLayer(Layer layer) {
        logger.info("adding new layer metadata for " + layer.getName());
        layerDao.addLayer(layer);
        return "redirect:/admin/layers";
    }

}
