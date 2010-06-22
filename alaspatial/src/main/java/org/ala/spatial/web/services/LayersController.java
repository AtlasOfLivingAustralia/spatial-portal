/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.web.services;

import java.util.List;
import org.ala.spatial.dao.LayersDAO;
import org.ala.spatial.model.LayerInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

/**
 * Controller class to serve available layer info
 * 
 * @author ajay
 */
@Controller
@RequestMapping("/ws/layers")
public class LayersController {

    private LayersDAO layersDao;

    @Autowired
    public void setLayersDao(LayersDAO layersDao) {
        System.out.println("setting layers dao");
        this.layersDao = layersDao;
    }

    @RequestMapping(value = "/index", method = RequestMethod.GET)
    public ModelAndView showIndexPage() {
        ModelMap modelMap = new ModelMap();
        modelMap.addAttribute("message", "Displaying all layers");
        modelMap.addAttribute("layerList", layersDao.getLayers());
        return new ModelAndView("layers/list", modelMap);
        //return "layers/list";
    }

    @RequestMapping(value = "/index", method = RequestMethod.POST)
    public ModelAndView searchLayers(@RequestParam String q) {
        ModelMap modelMap = new ModelMap();

        List l = null;
        String msg = "";

        if (q != null) {
            l = layersDao.getLayersByCriteria(q);
            int count = 0;
            if (l != null) {
                count = l.size();
            }
            msg = count + " search results for " + q;
            modelMap.addAttribute("mode", "search");
        } else {
            l = layersDao.getLayers();
            msg = "Displaying all layers";
            modelMap.addAttribute("mode", "list");
        }

        modelMap.addAttribute("message", msg);
        modelMap.addAttribute("layerList", l);
        return new ModelAndView("layers/list", modelMap);
        //return "layers/list";
    }

    @RequestMapping(value = "/add", method = RequestMethod.GET)
    public String showAddPage() {
        return "layers/add";
    }

    @RequestMapping(value = "/add", method = RequestMethod.POST)
    public String add(LayerInfo layer) {
        //LayerInfo layer = new LayerInfo();

        System.out.println("adding new layer object: " + layer.getName());

        layersDao.addLayer(layer);
        return "layers/index";
    }

    @RequestMapping(value = "/edit", method = RequestMethod.GET)
    public String showEditPage() {
        return "layers/edit";
    }

    @RequestMapping(value = "/edit", method = RequestMethod.POST)
    public String edit(LayerInfo layer) {
        //LayerInfo layer = new LayerInfo();

        System.out.println("editing existing layer object: " + layer.getName());

        layersDao.addLayer(layer);
        return "layers/index";
    }

    /**
     * Action call to get a list of all layers
     * 
     * @return List layers
     */
    @RequestMapping(value = "/list", method = RequestMethod.GET)
    public
    @ResponseBody
    List<LayerInfo> getLayers() {
        return layersDao.getLayers();
    }

    /**
     * action call to get list of layers given a name (part or full)
     *
     * @param name
     * @return List layers
     */
    @RequestMapping(value = "/name/{name}", method = RequestMethod.GET)
    public
    @ResponseBody
    List<LayerInfo> getLayersByName(@PathVariable String name) {
        System.out.print("retriving layer list by name: " + name);
        return layersDao.getLayersByName(name);
    }

    /**
     * Action call to get a layer info based on it's ID
     * 
     * @param id
     * @return LayerInfo layer
     */
    @RequestMapping(value = "/id/{id}", method = RequestMethod.GET)
    public
    @ResponseBody
    LayerInfo getLayerById(@PathVariable long id) {
        System.out.print("retriving layer list by id: " + id);
        return layersDao.getLayerById(id);
    }

    /**
     * 
     * @param keywords
     * @return
     */
    @RequestMapping(value = "/search/{keywords}", method = RequestMethod.GET)
    public 
    @ResponseBody
    List<LayerInfo> searchLayersByCriteria(@PathVariable String keywords) {
        System.out.print("retriving layer list by criteria: " + keywords);
        return layersDao.getLayersByCriteria(keywords);
    }
}
