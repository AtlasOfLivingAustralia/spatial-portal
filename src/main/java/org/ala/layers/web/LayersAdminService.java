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

import au.com.bytecode.opencsv.CSVWriter;

import java.security.Principal;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ala.layers.dao.LayerDAO;
import org.ala.layers.dto.Layer;
import org.apache.log4j.Logger;
import org.jasig.cas.client.authentication.AttributePrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

/**
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

    /**
     * This method returns all layers in an html view
     *
     * @return
     */
    @RequestMapping(value = "/layers/view", method = RequestMethod.GET)
    public ModelAndView viewLayers(HttpServletRequest req) {
        logger.info("Retriving all layers");
//        
//        System.out.println("**************************************");
//        System.out.println("      Authentication details ");
//        System.out.println("**************************************");
//        String useremail = "guest@ala.org.au";
//        if (req.getUserPrincipal() != null) {
//            Principal principal = req.getUserPrincipal();
//            if (principal instanceof AttributePrincipal) {
//                AttributePrincipal ap = (AttributePrincipal) principal;
//                System.out.println("ap: " + ap.getAttributes().toString());
//                useremail = (String) ap.getAttributes().get("email");
//            } else {
//                System.out.println("principal.class: " + principal.getClass().getCanonicalName());
//                useremail = principal.getName();
//            }
//        } else {
//            System.out.println("req.getUserPrincipal is NULL");
//        }
//        System.out.println("**************************************");
//        
//        
        return new ModelAndView("layers/index", "layers", layerDao.getLayers());
//        String useremail = AuthenticationUtil.getUserEmail(req);
//        ModelAndView mv = new ModelAndView("layers/index");
//        mv.addObject("layers", layerDao.getLayers());
//        mv.addObject("useremail", useremail);
//        mv.addObject("isAdmin",AuthenticationUtil.isUserAdmin(req));
//        
//        return mv;
    }

    /**
     * Action call to get a layer info based on it's UID or short name
     * or display name
     *
     * @param uid
     * @return ModelAndView view
     */
    @RequestMapping(value = "/layers/view/more/{uid}", method = RequestMethod.GET)
    public ModelAndView getLayerInfoByUId(@PathVariable String uid) {

        // test if the "uid" is a number or text
        // if number, then it's a uid
        // if text, then it'll be the layer (table) name

        boolean isUid = false;
        try {
            Integer.parseInt(uid);
            isUid = true;
        } catch (NumberFormatException nfe) {
        }

        Layer layer = null;
        if (isUid) {
            System.out.println("uid is an ID");
            layer = layerDao.getLayerById(Integer.parseInt(uid));
        } else {
            layer = layerDao.getLayerByName(uid);

            if (layer == null) {
                System.out.println("uid is an DisplayName");
                layer = layerDao.getLayerByDisplayName(uid);
            } else {
                System.out.println("uid is an Name");
            }
        }

        // change the metadata separator.
        if (layer != null) {
            String mdp = layer.getMetadatapath();
            if (mdp != null) {
                layer.setMetadatapath(mdp.replaceAll(", ", "|"));
            }
        } else {
            System.out.println("no layer :(");
        }

        return new ModelAndView("layers/moreinfo", "layer", layer);
    }

    @RequestMapping(value = "/layers.csv", method = RequestMethod.GET)
    public void downloadLayerList(@RequestParam(value = "q", required = false) String q, HttpServletResponse res) {
        try {
            //List<LayerInfo> layers = layersDao.getLayers();
            List<Layer> layers = null;
            if (q != null) {
                System.out.print("retriving layer list by criteria.q: " + q);
                layers = layerDao.getLayers();
                int count = 0;
                if (layers != null) {
                    count = layers.size();
                }
                //msg = count + " search results for " + q;
                //modelMap.addAttribute("mode", "search");
            } else {
                layers = layerDao.getLayers();
                //msg = "Displaying all layers";
                //modelMap.addAttribute("mode", "list");
            }


            String header = "";
            header += "UID,";
            header += "Short name,";
            header += "Name,";
            header += "Description,";
            header += "Data provider,";
            header += "Provider website,";
            header += "Provider role,";
            header += "Metadata date,";
            header += "Reference date,";
            header += "Licence level,";
            header += "Licence info,";
            header += "Licence notes,";
            header += "Type,";
            header += "Classification 1,";
            header += "Classification 2,";
            header += "Units,";
            header += "Notes,";
            header += "More information,";
            header += "Keywords";

            res.setContentType("text/csv; charset=UTF-8");
            res.setHeader("Content-Disposition", "inline;filename=ALA_Spatial_Layers.csv");
            CSVWriter cw = new CSVWriter(res.getWriter());
            cw.writeNext("Please provide feedback on the 'keywords' columns to data_management@ala.org.au".split("\n"));
            cw.writeNext(header.split(","));

            Iterator<Layer> it = layers.iterator();
            List<String[]> mylist = new Vector<String[]>();
            while (it.hasNext()) {
                Layer lyr = it.next();
                //System.out.println("Writing layer info for: " + lyr.getUid() + " - " + lyr.getDisplayname());
                mylist.add(lyr.toArray());
            }
            cw.writeAll(mylist);
            cw.close();
        } catch (Exception e) {
            System.out.println("Error writing layers.csv");
            e.printStackTrace(System.out);
        }

    }

}
