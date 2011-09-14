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

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import org.ala.layers.dao.FieldDAO;
import org.ala.layers.dao.LayerDAO;
import org.ala.layers.dao.ObjectDAO;
import org.ala.layers.dto.Layer;
import org.ala.layers.dto.Objects;
import org.ala.layers.util.Grid;
import org.apache.log4j.Logger;
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

    @Resource(name="fieldDao")
    private FieldDAO fieldDao;
    @Resource(name="layerDao")
    private LayerDAO layerDao;
    @Resource(name="objectDao")
    private ObjectDAO objectDao;

    /*
     * return intersection of a point on layers(s)
     */
    // @ResponseBody String
    @RequestMapping(value = WS_INTERSECT_SINGLE, method = RequestMethod.GET)
    public @ResponseBody Object single(@PathVariable("ids") String ids, @PathVariable("lat") Double lat, @PathVariable("lng") Double lng, HttpServletRequest req) {
//        return Intersect.Intersect(ids, lat, lng);

        Vector out = new Vector();

        for(String id : ids.split(",")) {

            Layer layer = layerDao.getLayerByName(id);

            double [][] p = {{lng, lat}};

            if(layer != null) {
                if(layer.isShape()) {
                    Objects o = objectDao.getObjectByIdAndLocation("cl"+layer.getId(), lng, lat);
                    System.out.println("********* Got values");
                    out.add(o);
                } else if (layer.isGrid()) {
                    Grid g = new Grid(DATA_FILES_PATH + layer.getPathorig());
                    float [] v = g.getValues(p);
                    //s = "{\"value\":" + v[0] + ",\"layername\":\"" + layer.getDisplayname() + "\"}";
                    Map m = new HashMap();
                    m.put("value", v[0]);
                    m.put("layername", layer.getDisplayname());
                    
                    out.add(m);
                }
            } else {
                String gid = null;
                String filename = null;
                String name = null;

                if (id.startsWith("species_")) {
                    //maxent layer
                    gid = id.substring(8);
                    filename = ALASPATIAL_OUTPUT_PATH + "/maxent/" + gid + "/" + gid;
                    name = "Prediction";
                } else if (id.startsWith("aloc_")) {
                    //aloc layer
                    gid = id.substring(8);
                    filename = ALASPATIAL_OUTPUT_PATH + "/aloc/" + gid + "/" + gid;
                    name = "Classification";
                }

                if (filename != null) {
                    Grid grid = new Grid(filename);

                    if(grid != null && (new File(filename + ".grd").exists()) ) {
                        float [] v = grid.getValues(p);
                        if(v != null) {
                            Map m = new HashMap();
                            if(Float.isNaN(v[0])) {
                                //s = "{\"value\":\"no data\",\"layername\":\"" + name + " (" + gid + ")\"}";
                                m.put("value", "no data");
                                m.put("layername", name + "("+gid+")");
                            } else {
                                //s = "{\"value\":" + v[0] + ",\"layername\":\"" + name + " (" + gid + ")\"}";
                                m.put("value", v[0]);
                                m.put("layername", name + "(" + gid + ")");
                            }
                            out.add(m);
                        }
                    }
                }
            }
        }

        return out;
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
    public @ResponseBody String test(@PathVariable("ids") String ids, @PathVariable("lat") Double lat, @PathVariable("lng") Double lng, HttpServletRequest req) {

        System.out.println("====================================================");
        System.out.println("Got test request");
        //Objects o = objectDao.getObjectById(ids, lng, lat);
        System.out.println("====================================================");


        ModelMap test = new ModelMap();
        test.addAttribute("test", "my test");

        return "test";
    }

}
