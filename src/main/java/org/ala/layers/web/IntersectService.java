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

import javax.servlet.http.HttpServletRequest;
import org.ala.layers.dao.FieldDAO;
import org.ala.layers.dao.LayerDAO;
import org.ala.layers.dao.ObjectDAO;
import org.ala.layers.dto.Layer;
import org.ala.layers.dto.Objects;
import org.ala.layers.util.Intersect;
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
 * @author ajay
 */
@Controller
public class IntersectService {

    private final String WS_INTERSECT_SINGLE = "/intersect/{ids}/{lat}/{lng}";
    private final String WS_INTERSECT_BATCH = "/intersect/batch";

    final static String ALASPATIAL_OUTPUT_PATH = "/data/ala/runtime/output";
    final static String DATA_FILES_PATH = "/data/ala/data/envlayers/WorldClimCurrent/10minutes/";

    /**
     * Log4j instance
     */
    protected Logger logger = Logger.getLogger(this.getClass());

    private FieldDAO fieldDao;
    private LayerDAO layerDao;
    private ObjectDAO objectDao;

    @Autowired
    public void setFieldsDao(FieldDAO fieldDao) {
        System.out.println("setting field dao");
        this.fieldDao = fieldDao;
    }

    @Autowired
    public void setLayersDao(LayerDAO layerDao) {
        System.out.println("setting layer dao");
        this.layerDao = layerDao;
    }

    @Autowired
    public void setObjectsDao(ObjectDAO objectDao) {
        System.out.println("setting object dao");
        this.objectDao = objectDao;
    }

    /*
     * return intersection of a point on layers(s)
     */
    // @ResponseBody String
    @RequestMapping(value = WS_INTERSECT_SINGLE, method = RequestMethod.GET)
    public ModelMap single(@PathVariable("ids") String names, @PathVariable("lat") Double lat, @PathVariable("lng") Double lng, HttpServletRequest req) {
//        return Intersect.Intersect(ids, lat, lng);

        StringBuilder sb = new StringBuilder();

        for(String name : names.split(",")) {

            String s = "";

            Layer layer = layerDao.getLayerByName(name);

            double [][] p = {{lng, lat}};

            if(layer != null) {
                if(layer.isShape()) {
                    Objects o = objectDao.getObjectByIdAndLocation("cl"+layer.getId(), lng, lat);
                    System.out.println("********* Got values");
                }
            } else {

            }
        }

        return null;
    }

    /**
     * Adam: Is this used anywhere yet?
     * @param id
     * @return 
     */
    private String cleanObjectId(String id) {
        return id.replaceAll("[^a-zA-Z0-9]:", "");
    }

    @RequestMapping(value = "/intersect/test/{ids}/{lat}/{lng}", method = RequestMethod.GET)
    public String test(@PathVariable("ids") String ids, @PathVariable("lat") Double lat, @PathVariable("lng") Double lng, HttpServletRequest req) {

        System.out.println("====================================================");
        System.out.println("Got test request");
        //Objects o = objectDao.getObjectById(ids, lng, lat);
        System.out.println("====================================================");


        ModelMap test = new ModelMap();
        test.addAttribute("test", "my test");

        return "test";
    }

}
