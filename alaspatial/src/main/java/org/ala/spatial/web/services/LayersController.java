package org.ala.spatial.web.services;

import java.io.BufferedReader;
import java.io.FileReader;
import java.net.URLDecoder;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.ala.spatial.dao.LayersDAO;
import org.ala.spatial.model.LayerInfo;
import org.ala.spatial.util.TabulationSettings;
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
//@RequestMapping("/ws/layers")
public class LayersController {

    private final String LAYERS_BASE_WS = "/ws/layers";
    private final String LAYERS_BASE = "/layers";
    private final String LAYERS_INDEX = "/ws/layers/index";
    private final String LAYERS_ADD = "/ws/layers/add";
    private final String LAYERS_EDIT = "/ws/layers/edit";
    private final String LAYERS_LIST = "/ws/layers/list";
    private final String LAYERS_ASSOCIATIONS = "/layers/analysis/inter_layer_association.csv";
    private final String LAYERS_ASSOCIATIONS_RAWNAMES = "/layers/analysis/inter_layer_association_rawnames.csv";
    private LayersDAO layersDao;

    @Autowired
    public void setLayersDao(LayersDAO layersDao) {
        System.out.println("setting layers dao");
        this.layersDao = layersDao;
    }

    @RequestMapping(value = LAYERS_INDEX, method = RequestMethod.GET)
    public ModelAndView showIndexPage() {
        ModelMap modelMap = new ModelMap();
        modelMap.addAttribute("message", "Displaying all layers");
        modelMap.addAttribute("layerList", layersDao.getLayers());
        return new ModelAndView("layers/list", modelMap);
        //return "layers/list";
    }

    @RequestMapping(value = LAYERS_INDEX, method = RequestMethod.POST)
    public ModelAndView searchLayers(@RequestParam String q) {
        ModelMap modelMap = new ModelMap();

        List l = null;
        String msg = "";

        if (q != null) {
            System.out.print("retriving layer list by criteria.q: " + q);
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

    @RequestMapping(value = LAYERS_ADD, method = RequestMethod.GET)
    public String showAddPage() {
        return "layers/add";
    }

    @RequestMapping(value = LAYERS_ADD, method = RequestMethod.POST)
    public String add(LayerInfo layer) {
        //LayerInfo layer = new LayerInfo();

        System.out.println("adding new layer object: " + layer.getName());

        layersDao.addLayer(layer);
        return "layers/index";
    }

    @RequestMapping(value = LAYERS_EDIT, method = RequestMethod.GET)
    public String showEditPage() {
        return "layers/edit";
    }

    @RequestMapping(value = LAYERS_EDIT, method = RequestMethod.POST)
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
    @RequestMapping(value = LAYERS_LIST, method = RequestMethod.GET)
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
    @RequestMapping(value = LAYERS_BASE + "/name/{name}", method = RequestMethod.GET)
    public
    @ResponseBody
    List<LayerInfo> getLayersByName(@PathVariable String name) {
        System.out.print("retriving layer list by name: " + name);
        return layersDao.getLayersByName(name);
    }

    /**
     * Action call to get a layer info based on it's UID
     * 
     * @param uid
     * @return ModelAndView view
     */
    @RequestMapping(value = LAYERS_BASE + "/{uid}", method = RequestMethod.GET)
    public ModelAndView getLayerByUId(@PathVariable String uid) {
        System.out.print("retriving layer list by id: " + uid);
        LayerInfo layer = layersDao.getLayerById(uid);

        ModelMap m = new ModelMap();
        m.addAttribute("layer", layer);

        return new ModelAndView("layers/view", m);
    }

    /**
     * Action call to get a layer info based on it's ID
     *
     * @param id
     * @return LayerInfo layer
     */
    @RequestMapping(value = LAYERS_BASE + "/id/{uid}", method = RequestMethod.GET)
    public
    @ResponseBody
    LayerInfo getLayerById(@PathVariable String uid) {
        System.out.print("retriving layer list by id: " + uid);
        return layersDao.getLayerById(uid);
    }

    /**
     * Action call to get layer citations based on the UID/s
     *
     * @param id
     * @return LayerInfo layer
     */
    @RequestMapping(value = LAYERS_BASE + "/citation", method = RequestMethod.POST)
    public
    @ResponseBody
    LayerInfo getLayerCitation(HttpServletRequest req) {
        System.out.println("retriving layer citation: ");
        try {
            BufferedReader in = new BufferedReader(req.getReader());
            String uidList;
            while ((uidList = in.readLine()) != null) {
                System.out.println(uidList);
            }
            in.close();
        } catch (Exception e) {
            System.out.println("Error reading in post content");
            e.printStackTrace(System.out);
        }

        return layersDao.getLayerById("890");
    }

    /**
     * 
     * @param keywords
     * @return
     */
    @RequestMapping(value = LAYERS_BASE_WS + "/search/{keywords}", method = RequestMethod.GET)
    public
    @ResponseBody
    List<LayerInfo> searchLayersByCriteria(@PathVariable String keywords) {
        try {
            keywords = URLDecoder.decode(keywords, "UTF-8");
        } catch (Exception e) {
            System.out.println("Error in searchLayersByCriteria: " + keywords);
            e.printStackTrace(System.out);
        }
        System.out.print("retriving layer list by criteria: " + keywords);
        return layersDao.getLayersByCriteria(keywords);
    }

    @RequestMapping(value = LAYERS_ASSOCIATIONS, method = RequestMethod.GET)
    public
    @ResponseBody
    String getLayerAssociations(HttpServletRequest req) {
        //layer associations file named "layerDistances.csv" under index directory
        StringBuffer sb = new StringBuffer();
        try {
            BufferedReader br = new BufferedReader(
                    new FileReader(TabulationSettings.index_path
                    + "layerDistances.csv"));

            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    @RequestMapping(value = LAYERS_ASSOCIATIONS_RAWNAMES, method = RequestMethod.GET)
    public
    @ResponseBody
    String getLayerAssociationsRawNames(HttpServletRequest req) {
        //layer associations file named "layerDistances.csv" under index directory
        StringBuffer sb = new StringBuffer();
        try {
            BufferedReader br = new BufferedReader(
                    new FileReader(TabulationSettings.index_path
                    + "layerDistancesRawNames.csv"));

            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sb.toString();
    }
}
