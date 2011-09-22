package org.ala.spatial.web.services;

import au.com.bytecode.opencsv.CSVWriter;
import java.io.BufferedReader;
import java.io.FileReader;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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

    @RequestMapping(value = {LAYERS_BASE, LAYERS_BASE + "/"}, method = RequestMethod.GET)
    public Map<String, Object> showPublicIndexPage(@RequestParam(value = "q", required = false) String q) {
//        ModelMap modelMap = new ModelMap();
//        //modelMap.addAttribute("message", "Displaying all layers");
//        modelMap.addAttribute("layerList", layersDao.getLayers());
//        return new ModelAndView("layers/public_list", modelMap);
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
            //msg = count + " search results for " + q;
            //modelMap.addAttribute("mode", "search");
        } else {
            l = layersDao.getLayers();
            //msg = "Displaying all layers";
            //modelMap.addAttribute("mode", "list");
        }

        if (l.size() > 0) {
            Iterator<LayerInfo> it = l.iterator();
            while (it.hasNext()) {
                LayerInfo lyr = it.next();
                lyr.setDisplaypath("http://spatial.ala.org.au/geoserver/wms/reflect?layers=ALA:" + lyr.getName() + "&width=300");
            }
        }

        //modelMap.addAttribute("message", msg);
        modelMap.addAttribute("GetCapabilities", "http://spatial.ala.org.au/geoserver/wms?request=getCapabilities");
        modelMap.addAttribute("layerList", l);
        //return new ModelAndView("layers/public_list", modelMap);
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("layerList", l);
        m.put("GetCapabilities", "http://spatial.ala.org.au/geoserver/wms?request=getCapabilities");
        return m;

    }

    @RequestMapping(value = LAYERS_BASE + ".csv", method = RequestMethod.GET)
    public void downloadLayerList(@RequestParam(value = "q", required = false) String q, HttpServletResponse res) {
        try {
            //List<LayerInfo> layers = layersDao.getLayers();
            List<LayerInfo> layers = null;
            if (q != null) {
                System.out.print("retriving layer list by criteria.q: " + q);
                layers = layersDao.getLayersByCriteria(q);
                int count = 0;
                if (layers != null) {
                    count = layers.size();
                }
                //msg = count + " search results for " + q;
                //modelMap.addAttribute("mode", "search");
            } else {
                layers = layersDao.getLayers();
                //msg = "Displaying all layers";
                //modelMap.addAttribute("mode", "list");
            }


            String header = "";
            header += "UID, ";
            header += "Short name, ";
            header += "Name, ";
            header += "Description, ";
            header += "Metadata contact organization, ";
            header += "Metadata contact organization website, ";
            header += "Organisation role, ";
            header += "Metadata date, ";
            header += "Reference date, ";
            header += "Licence level, ";
            header += "Licence info, ";
            header += "Licence notes, ";
            header += "Type, ";
            header += "Classification 1, ";
            header += "Classification 2, ";
            header += "Units, ";
            header += "Data language, ";
            header += "Scope, ";
            header += "Notes, ";
            header += "More information, ";
            header += "Keywords";

            res.setContentType("text/csv; charset=UTF-8");
            res.setHeader("Content-Disposition", "inline;filename=ALA_Spatial_Layers.csv");
            CSVWriter cw = new CSVWriter(res.getWriter());
            cw.writeNext("Please provide feedback on the 'keywords' columns to data_management@ala.org.au".split("\n"));
            cw.writeNext(header.split(","));

            Iterator<LayerInfo> it = layers.iterator();
            List<String[]> mylist = new Vector<String[]>();
            while (it.hasNext()) {
                LayerInfo lyr = it.next();
                System.out.println("Writing layer info for: " + lyr.getUid() + " - " + lyr.getDisplayname());
                mylist.add(lyr.toArray());
            }
            cw.writeAll(mylist);
            cw.close();
        } catch (Exception e) {
            System.out.println("Error writing layers.csv");
            e.printStackTrace(System.out);
        }

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

        // change the metadata separator.
        String mdp = layer.getMetadatapath();
        if (mdp != null) {
            layer.setMetadatapath(mdp.replaceAll(", ", "|"));
        }

        ModelMap m = new ModelMap();
        m.addAttribute("layer", layer);

        return new ModelAndView("layers/view", m);
    }

    /**
     * Action call to get a layer info based on it's UID
     *
     * @param uid
     * @return ModelAndView view
     */
    @RequestMapping(value = LAYERS_BASE + "/more/{uid}", method = RequestMethod.GET)
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

        LayerInfo layer = null;
        if (isUid) {
            System.out.print("retriving layer list by id: " + uid);
            layer = layersDao.getLayerById(uid);
        } else {
            System.out.print("retriving layer list by name: " + uid);
            List<LayerInfo> layers = layersDao.getLayersByName(uid);
            if (layers != null) {
                if (layers.size() > 0) {
                    layer = layers.get(0);
                }
            }
        }

        // change the metadata separator.
        String mdp = layer.getMetadatapath();
        if (mdp != null) {
            layer.setMetadatapath(mdp.replaceAll(", ", "|"));
        }

        ModelMap m = new ModelMap();
        m.addAttribute("layer", layer);

        return new ModelAndView("layers/moreinfo", m);
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
    List<LayerInfo> getLayerCitation(HttpServletRequest req) {
        System.out.println("retriving layer citation: ");
        try {
            BufferedReader in = new BufferedReader(req.getReader());
            String uids;
            while ((uids = in.readLine()) != null) {
                System.out.println(uids);
            }
            in.close();
            uids = uids.replaceAll("[", "");
            uids = uids.replaceAll("]", "");

            String[] uidList = uids.split(",");

            List<LayerInfo> layers = new ArrayList<LayerInfo>();
            for (String uid : uidList) {
                layers.add(layersDao.getLayerById(uid));
            }

            return layers;

        } catch (Exception e) {
            System.out.println("Error reading in post content");
            e.printStackTrace(System.out);
        }

        return null;
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
