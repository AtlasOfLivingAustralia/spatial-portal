package org.ala.spatial.web;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
//import org.ala.spatial.analysis.cluster.ClusteredRecord;
import org.ala.spatial.analysis.cluster.Record;
//import org.ala.spatial.analysis.cluster.SpatialCluster;
import org.ala.spatial.analysis.cluster.SpatialCluster3;
import org.ala.spatial.analysis.index.OccurrencesIndex;
import org.ala.spatial.analysis.service.FilteringService;

import org.ala.spatial.analysis.service.OccurrencesService;
import org.ala.spatial.analysis.service.SamplingService;
import org.ala.spatial.dao.SpeciesDAO;
import org.ala.spatial.model.ValidTaxonName;
import org.ala.spatial.util.SimpleRegion;
import org.ala.spatial.util.SimpleShapeFile;
import org.ala.spatial.util.SpatialSettings;
import org.ala.spatial.util.TabulationSettings;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.ObjectMapper;
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
 *
 * @author ajayr
 */
@Controller
@RequestMapping("/species")
public class SpeciesController {

    private final int DEFAULT_PIXEL_DISTANCE = 40;
    private final int DEFAULT_MAP_ZOOM = 4;
    private SpeciesDAO speciesDao;

    @Autowired
    public void setSpeciesDao(SpeciesDAO speciesDao) {
        this.speciesDao = speciesDao;
    }

    @RequestMapping(method = RequestMethod.GET)
    public ModelAndView showPage(HttpSession session) {
        System.out.println("species page method called");

        //return new ModelAndView("species", "message", "Add method called");

        ModelMap modelMap = new ModelMap();
        //modelMap.addAttribute("dsourceList", dataDao.listByUser(user.getId()));
        modelMap.addAttribute("message", "Generating species list");
        modelMap.addAttribute("spList", speciesDao.getSpecies());
        return new ModelAndView("species", modelMap);

    }

    @RequestMapping(value = "/records", method = RequestMethod.GET)
    public ModelAndView getNames(@RequestParam String query) {
        System.out.println("Looking up list for: " + query);
        String[] spdata = query.split("#");
        ModelMap modelMap = new ModelMap();
        modelMap.addAttribute("message", "Generating species list");
        modelMap.addAttribute("spList", speciesDao.getRecordsByNameLevel(spdata[0], spdata[1]));
        return new ModelAndView("species/records", modelMap);

    }

    @RequestMapping(value = "/names", method = RequestMethod.GET)
    public
    @ResponseBody
    Map getNames2(@RequestParam String q, @RequestParam int s, @RequestParam int p) {
        System.out.println("Looking up names for: " + q);

        /*
        List lsp = speciesDao.findByName(q, s, p);
        //List lsp = speciesDao.getNames();
        Hashtable<String, Object> hout = new Hashtable();
        hout.put("totalCount", lsp.size());
        hout.put("taxanames", lsp);
        return hout;
         *
         */

        return speciesDao.findByName(q, s, p);

    }

    @RequestMapping(value = "/taxon", method = RequestMethod.GET)
    public
    @ResponseBody
    String getTaxonNamesEmpty() {
        return "";
    }

    @RequestMapping(value = "/taxon/{name}", method = RequestMethod.GET)
    public
    @ResponseBody
    String getTaxonNames(@PathVariable("name") String name) {
        StringBuffer slist = new StringBuffer();
        try {

            System.out.println("Looking up names for: " + name);

            TabulationSettings.load();

            name = URLDecoder.decode(name, "UTF-8");

            String[] aslist = OccurrencesService.filterSpecies(name, 40);
            if (aslist == null) {
                aslist = new String[1];
                aslist[0] = "";
            }

            for (String s : aslist) {
                slist.append(s).append("\n");
            }

            //System.out.println(">>>>> dumping out s.names <<<<<<<<<<");
            //System.out.println(slist);
            //System.out.println(">>>>> dumping out c.names <<<<<<<<<<");
//long t1 = System.currentTimeMillis();
/*
            List<CommonName> clist = speciesDao.getCommonNames(name);
            Iterator<CommonName> it = clist.iterator();
            String previousScientificName = "";
            while (it.hasNext()) {
            CommonName cn = it.next();
            //System.out.println("> " + cn.getCommonname() + " -- " + cn.getScientificname());

            //only add if different from previous (query is sorted by scientificName)
            if (!previousScientificName.equals(cn.getScientificname())) {
            int records = OccurrencesService.getSpeciesCount(cn.getScientificname());
            slist.append(cn.getCommonname()).append(" / Scientific name: ").append(cn.getScientificname()).append(" / found ").append(records).append("\n");
            previousScientificName = cn.getScientificname();
            }
            }*/
//long t2 = System.currentTimeMillis();

            String s = OccurrencesIndex.getCommonNames(name,aslist);
            slist.append(s);

//long t3 = System.currentTimeMillis();

//System.out.println("timings: DAO=" + (t2 - t1) + "ms; OI=" + (t3-t2) + "ms");

            //System.out.println(">>>>> done <<<<<<<<<<");
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }

        return slist.toString();

    }

    @RequestMapping(value = "/lsid/{lsid}", method = RequestMethod.GET)
    public
    @ResponseBody
    List<ValidTaxonName> getTaxonByLsid(@PathVariable("lsid") String lsid) {
        try {
            lsid = URLDecoder.decode(lsid, "UTF-8");
            lsid = lsid.replaceAll("__", ".");
            System.out.println("Starting out search for: " + lsid);
            List l = speciesDao.findById(lsid);
            System.out.println("re-returning " + l.size() + " records");

            /*
            int[] recs = OccurrencesIndex.lookup("institutionCode","WAM");
            int[] recs2 = OccurrencesIndex.lookup("collectionCode","MAMM");
            int[] recs3 = OccurrencesIndex.lookup("collectionCode","ARACH");

            int[] recs23 = recs2+recs3;
            sort(recs);
            sort(recs23);
            int[] finalRecs = removeNotInList(recs,recs23);

            double[][] pts = OccurrencesIndex.getPointsPairs();
            for (i=0;i<finalRecs.length;i++) {
            pts[finalRecs[i]][0];
            pts[finalRecs[i]][1];
            }
             */


            return l;
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(SpeciesController.class.getName()).log(Level.SEVERE, null, ex);
        }

        return null;
    }

    /**
     * Returns a relative path to a zip file of the filtered georeferenced data
     *
     * @param pid
     * @param shape
     * @param req
     * @return
     */
    @RequestMapping(value = "/lsid/{lsid}/geojson", method = RequestMethod.GET)
    public
    @ResponseBody
    String getSamplesListAsGeoJSON(@PathVariable String lsid, HttpServletRequest req) {
        TabulationSettings.load();

        long start = System.currentTimeMillis();

        try {

            lsid = URLDecoder.decode(lsid, "UTF-8");
            lsid = lsid.replaceAll("__", ".");

            String currentPath = req.getSession().getServletContext().getRealPath(File.separator);
            //String currentPath = TabulationSettings.base_output_dir;
            long currTime = System.currentTimeMillis();
            //String outputpath = currentPath + File.separator + "output" + File.separator + "sampling" + File.separator + currTime + File.separator;
            String outputpath = currentPath + File.separator + "output" + File.separator + "sampling" + File.separator;
            File fDir = new File(outputpath);
            fDir.mkdir();

            String gjsonFile = SamplingService.getLSIDAsGeoJSONIntoParts(lsid, fDir);

            long end = System.currentTimeMillis();
            System.out.println("get species by lsid geojson: " + (end - start) + "ms");


            //return "output/sampling/" + currTime + "/" + gjsonFile;
            return "output/sampling/" + gjsonFile;
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
        return "";
    }

    @RequestMapping(value = "/cluster/{species}", method = RequestMethod.GET)
    public
    @ResponseBody
    Hashtable getClusteredRecords(@PathVariable("species") String species, HttpServletRequest req) {
        try {
            SpatialSettings ssets = new SpatialSettings();

            long start = System.currentTimeMillis();

            int zoom = DEFAULT_MAP_ZOOM;
            int pdist = DEFAULT_PIXEL_DISTANCE;

            species = URLDecoder.decode(species, "UTF-8");
            species = species.replaceAll("__", ".");

            try {
                if (req.getParameter("z") != null) {
                    zoom = Integer.parseInt(req.getParameter("z"));
                }
            } catch (Exception e) {
                zoom = DEFAULT_MAP_ZOOM;
            }
            try {
                if (req.getParameter("d") != null) {
                    pdist = Integer.parseInt(req.getParameter("d"));
                }
            } catch (Exception e) {
                pdist = DEFAULT_PIXEL_DISTANCE;
            }

            System.out.println("req.sp: " + species);
            System.out.println("req.z: " + zoom);
            System.out.println("req.d: " + pdist);

            //String lsid = "urn:lsid:biodiversity.org.au:apni.taxon:295864";
            //species = lsid;

            /*SamplingService ss = new SamplingService();

            String[][] results = ss.sampleSpecies(species, null, region, null, TabulationSettings.MAX_RECORD_COUNT);
            StringBuilder sbResults = new StringBuilder();
            Vector dataPoints = new Vector();

            if (results != null) {
                System.out.println("Got " + results.length + " records for species: " + species);
            } else {
                System.out.println("Got no records for species: " + species);
            }

            int sciname_pos = -1;
            int prec_pos = -1;
            for(int i=0;i<TabulationSettings.geojson_property_names.length;i++){
                if (TabulationSettings.geojson_property_names[i].equalsIgnoreCase("s")) {
                    sciname_pos = i;
                }
                if (TabulationSettings.geojson_property_names[i].equalsIgnoreCase("u")) {
                    prec_pos = i;
                }
            }

            for (int i = 1; i < results.length; i++) {
                //System.out.println("Adding to cluster");
                // System.out.println(results[i][TabulationSettings.geojson_id] + " - " + results[i][TabulationSettings.geojson_property_fields[5]] + " - " + results[i][TabulationSettings.geojson_longitude] + ", " + results[i][TabulationSettings.geojson_latitude]);
                if (results[i][TabulationSettings.geojson_id] != null) {
                    if (!results[i][TabulationSettings.geojson_id].toLowerCase().equals("null")) {
                        dataPoints.add(new Record(results[i][TabulationSettings.geojson_id], results[i][TabulationSettings.geojson_property_fields[sciname_pos]], Double.parseDouble(results[i][TabulationSettings.geojson_longitude]), Double.parseDouble(results[i][TabulationSettings.geojson_latitude]), results[i][TabulationSettings.geojson_property_fields[prec_pos]]));
                    }
                }
            }*/
            Vector dataPoints = OccurrencesIndex.sampleSpeciesForClustering(species,null,TabulationSettings.MAX_RECORD_COUNT);

            long timePoints = System.currentTimeMillis();

            Vector allFeatures = new Vector();
            // SpatialCluster3 stuff start
            SpatialCluster3 scluster = new SpatialCluster3();
            Vector<Vector<Record>> clustered = scluster.cluster(dataPoints, pdist, zoom);
            for (int i = 0; i < clustered.size(); i++) {
                //System.out.println(i + "> " + clustered.get(i).toString());

                Vector<Record> cluster = clustered.get(i);
                Record r = cluster.get(0);

                Hashtable geometry = new Hashtable();
                geometry.put("type", "Point");
                double[] coords = {r.getLongitude(), r.getLatitude()};
                geometry.put("coordinates", coords);

                Map cFeature = new HashMap();
                cFeature.put("type", "Feature"); // feature.getType().getName().toString()
                cFeature.put("id", "occurrences." + i + 1);
                cFeature.put("properties", cluster);
                cFeature.put("geometry_name", "the_geom");
                cFeature.put("geometry", geometry);

                allFeatures.add(cFeature);

            }
            // SpatialCluster3 stuff end

            Hashtable data = new Hashtable();
            data.put("type", "FeatureCollection");
            data.put("features", allFeatures);

            /*
            System.out.println("returning allFeatures:" + allFeatures.toArray());
            System.out.println("===========================");
            ObjectMapper mapper = new ObjectMapper();
            mapper.writeValue(System.out, data);
            System.out.println("===========================");
             *
             */

            long end = System.currentTimeMillis();

            System.out.println("cluster/species: datapoints=" + (timePoints - start) + "ms total=" + (end - start) + "ms");

            return data;

        } catch (Exception e) {
            System.out.println("getClusteredRecords.error: ");
            e.printStackTrace(System.out);
        }
        System.out.println("returning null");
        return null;
    }

    @RequestMapping(value = "/cluster/{species}/area/{area}", method = RequestMethod.GET)
    public
    @ResponseBody
    Hashtable getClusteredRecords(@PathVariable("species") String species, @PathVariable("area") String area, HttpServletRequest req) {
        try {
            SpatialSettings ssets = new SpatialSettings();

            long start = System.currentTimeMillis();

            int zoom = DEFAULT_MAP_ZOOM;
            int pdist = DEFAULT_PIXEL_DISTANCE;

            species = URLDecoder.decode(species, "UTF-8");
            species = species.replaceAll("__", ".");

            try {
                if (req.getParameter("z") != null) {
                    zoom = Integer.parseInt(req.getParameter("z"));
                }
            } catch (Exception e) {
                zoom = DEFAULT_MAP_ZOOM;
            }
            try {
                if (req.getParameter("d") != null) {
                    pdist = Integer.parseInt(req.getParameter("d"));
                }
            } catch (Exception e) {
                pdist = DEFAULT_PIXEL_DISTANCE;
            }

//            String area = req.getParameter("area");
//            ArrayList<Integer> records = null;
//            SimpleRegion region = null;
//            if (area != null && area.startsWith("ENVELOPE")) {
//                records = FilteringService.getRecords(req.getParameter("area"));
//            } else {
//                region = SimpleShapeFile.parseWKT(req.getParameter("area"));
//            }

            SimpleRegion region = SimpleShapeFile.parseWKT(URLDecoder.decode(area,"UTF-8"));

            System.out.println("req.sp: " + species);
            System.out.println("req.a: " + area);
            System.out.println("req.z: " + zoom);
            System.out.println("req.d: " + pdist);

            //String lsid = "urn:lsid:biodiversity.org.au:apni.taxon:295864";
            //species = lsid;

            /*SamplingService ss = new SamplingService();

            String[][] results = ss.sampleSpecies(species, null, region, null, TabulationSettings.MAX_RECORD_COUNT);
            StringBuilder sbResults = new StringBuilder();
            Vector dataPoints = new Vector();

            if (results != null) {
                System.out.println("Got " + results.length + " records for species: " + species);
            } else {
                System.out.println("Got no records for species: " + species);
            }

            int sciname_pos = -1;
            int prec_pos = -1;
            for(int i=0;i<TabulationSettings.geojson_property_names.length;i++){
                if (TabulationSettings.geojson_property_names[i].equalsIgnoreCase("s")) {
                    sciname_pos = i;
                }
                if (TabulationSettings.geojson_property_names[i].equalsIgnoreCase("u")) {
                    prec_pos = i;
                }
            }

            for (int i = 1; i < results.length; i++) {
                //System.out.println("Adding to cluster");
                // System.out.println(results[i][TabulationSettings.geojson_id] + " - " + results[i][TabulationSettings.geojson_property_fields[5]] + " - " + results[i][TabulationSettings.geojson_longitude] + ", " + results[i][TabulationSettings.geojson_latitude]);
                if (results[i][TabulationSettings.geojson_id] != null) {
                    if (!results[i][TabulationSettings.geojson_id].toLowerCase().equals("null")) {
                        dataPoints.add(new Record(results[i][TabulationSettings.geojson_id], results[i][TabulationSettings.geojson_property_fields[sciname_pos]], Double.parseDouble(results[i][TabulationSettings.geojson_longitude]), Double.parseDouble(results[i][TabulationSettings.geojson_latitude]), results[i][TabulationSettings.geojson_property_fields[prec_pos]]));
                    }
                }
            }*/
            Vector dataPoints = OccurrencesIndex.sampleSpeciesForClustering(species,region,TabulationSettings.MAX_RECORD_COUNT);

            long timePoints = System.currentTimeMillis();

            Vector allFeatures = new Vector();
            // SpatialCluster3 stuff start
            SpatialCluster3 scluster = new SpatialCluster3();
            Vector<Vector<Record>> clustered = scluster.cluster(dataPoints, pdist, zoom);
            for (int i = 0; i < clustered.size(); i++) {
                //System.out.println(i + "> " + clustered.get(i).toString());

                Vector<Record> cluster = clustered.get(i);
                Record r = cluster.get(0);

                Hashtable geometry = new Hashtable();
                geometry.put("type", "Point");
                double[] coords = {r.getLongitude(), r.getLatitude()};
                geometry.put("coordinates", coords);

                Hashtable properties = new Hashtable();
                properties.put("cluster", cluster);
                properties.put("count", cluster.size()); 

                Map cFeature = new HashMap();
                cFeature.put("type", "Feature"); // feature.getType().getName().toString()
                cFeature.put("id", "occurrences." + i + 1);
                cFeature.put("properties", properties);
                cFeature.put("geometry_name", "the_geom");
                cFeature.put("geometry", geometry);

                allFeatures.add(cFeature);

            }
            // SpatialCluster3 stuff end

            Hashtable data = new Hashtable();
            data.put("type", "FeatureCollection");
            data.put("features", allFeatures);

            /*
            System.out.println("returning allFeatures:" + allFeatures.toArray());
            System.out.println("===========================");
            ObjectMapper mapper = new ObjectMapper();
            mapper.writeValue(System.out, data);
            System.out.println("===========================");
             * 
             */

            long end = System.currentTimeMillis();

            System.out.println("cluster/species/area: datapoints=" + (timePoints - start) + "ms total=" + (end - start) + "ms");

            return data;

        } catch (Exception e) {
            System.out.println("getClusteredRecords.error: ");
            e.printStackTrace(System.out);
        }
        System.out.println("returning null");
        return null;
    }

    /**
     * not: only supporting square areas
     *
     * @param area
     * @param req
     * @return
     */
    @RequestMapping(value = "/cluster/area/{area}", method = RequestMethod.GET)
    public
    @ResponseBody
    Hashtable getClusteredRecordsInArea(@PathVariable("area") String area, HttpServletRequest req) {
        try {
            SpatialSettings ssets = new SpatialSettings();

            long start = System.currentTimeMillis();

            int zoom = DEFAULT_MAP_ZOOM;
            int pdist = DEFAULT_PIXEL_DISTANCE;

            SimpleRegion region = SimpleShapeFile.parseWKT(URLDecoder.decode(area,"UTF-8"));

            try {
                if (req.getParameter("z") != null) {
                    zoom = Integer.parseInt(req.getParameter("z"));
                }
            } catch (Exception e) {
                zoom = DEFAULT_MAP_ZOOM;
            }
            try {
                if (req.getParameter("d") != null) {
                    pdist = Integer.parseInt(req.getParameter("d"));
                }
            } catch (Exception e) {
                pdist = DEFAULT_PIXEL_DISTANCE;
            }

            System.out.println("req.a: " + area);
            System.out.println("req.z: " + zoom);
            System.out.println("req.d: " + pdist);

            //String lsid = "urn:lsid:biodiversity.org.au:apni.taxon:295864";
            //species = lsid;

            System.out.println("area: " + area);

            /*SamplingService ss = new SamplingService();

            String[][] results = FilteringService.getSamplesCells("none", region);
            StringBuilder sbResults = new StringBuilder();
            Vector dataPoints = new Vector();

            if (results != null) {
                System.out.println("Got " + results.length + " records for area: " + area);
            } else {
                System.out.println("Got no records for area: " + area);
            }

            int sciname_pos = -1;
            int prec_pos = -1;
            for(int i=0;i<TabulationSettings.geojson_property_names.length;i++){
                if (TabulationSettings.geojson_property_names[i].equalsIgnoreCase("s")) {
                    sciname_pos = i;
                }
                if (TabulationSettings.geojson_property_names[i].equalsIgnoreCase("u")) {
                    prec_pos = i;
                }
            }

            for (int i = 1; i < results.length; i++) {
                //System.out.println("Adding to cluster");
                // System.out.println(results[i][TabulationSettings.geojson_id] + " - " + results[i][TabulationSettings.geojson_property_fields[5]] + " - " + results[i][TabulationSettings.geojson_longitude] + ", " + results[i][TabulationSettings.geojson_latitude]);
                if (results[i][TabulationSettings.geojson_id] != null) {
                    if (!results[i][TabulationSettings.geojson_id].toLowerCase().equals("null")) {
                        dataPoints.add(new Record(results[i][TabulationSettings.geojson_id], results[i][TabulationSettings.geojson_property_fields[sciname_pos]], Double.parseDouble(results[i][TabulationSettings.geojson_longitude]), Double.parseDouble(results[i][TabulationSettings.geojson_latitude]), results[i][TabulationSettings.geojson_property_fields[prec_pos]]));
                    }
                }
            }*/

            Vector dataPoints = OccurrencesIndex.sampleSpeciesForClustering(null,region,TabulationSettings.MAX_RECORD_COUNT);

            long timePoints = System.currentTimeMillis();

            Vector allFeatures = new Vector();
            // SpatialCluster3 stuff start
            SpatialCluster3 scluster = new SpatialCluster3();
            Vector<Vector<Record>> clustered = scluster.cluster(dataPoints, pdist, zoom);
            for (int i = 0; i < clustered.size(); i++) {
                //System.out.println(i + "> " + clustered.get(i).toString());

                Vector<Record> cluster = clustered.get(i);
                Record r = cluster.get(0);

                Hashtable geometry = new Hashtable();
                geometry.put("type", "Point");
                double[] coords = {r.getLongitude(), r.getLatitude()};
                geometry.put("coordinates", coords);

                Hashtable properties = new Hashtable();
                properties.put("cluster", cluster);
                properties.put("count", cluster.size());

                Map cFeature = new HashMap();
                cFeature.put("type", "Feature"); // feature.getType().getName().toString()
                cFeature.put("id", "occurrences." + i + 1);
                cFeature.put("properties", properties);
                cFeature.put("geometry_name", "the_geom");
                cFeature.put("geometry", geometry);

                allFeatures.add(cFeature);

            }
            // SpatialCluster3 stuff end

            Hashtable data = new Hashtable();
            data.put("type", "FeatureCollection");
            data.put("features", allFeatures);

            /*
            System.out.println("returning allFeatures:" + allFeatures.toArray());
            System.out.println("===========================");
            ObjectMapper mapper = new ObjectMapper();
            mapper.writeValue(System.out, data);
            System.out.println("===========================");
             *
             */


            long end = System.currentTimeMillis();

            System.out.println("cluster/area: datapoints=" + (timePoints - start) + "ms total=" + (end - start) + "ms");


            return data;

        } catch (Exception e) {
            System.out.println("getClusteredRecords.error: ");
            e.printStackTrace(System.out);
        }
        System.out.println("returning null");
        return null;
    }
}
