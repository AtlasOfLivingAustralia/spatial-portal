package org.ala.spatial.web;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.ala.spatial.analysis.cluster.ClusterLookup;
import org.ala.spatial.analysis.cluster.Record;
import org.ala.spatial.analysis.cluster.SpatialCluster3;
import org.ala.spatial.analysis.index.BoundingBoxes;
import org.ala.spatial.analysis.index.OccurrenceRecordNumbers;
import org.ala.spatial.analysis.index.OccurrencesCollection;
import org.ala.spatial.analysis.index.OccurrencesFilter;
import org.ala.spatial.analysis.index.OccurrencesSpeciesList;
import org.ala.spatial.analysis.index.SpeciesColourOption;
import org.ala.spatial.analysis.service.FilteringService;
import org.ala.spatial.analysis.service.SamplingLoadedPointsService;
import org.ala.spatial.analysis.service.SamplingService;
import org.ala.spatial.analysis.service.ShapeLookup;
import org.ala.spatial.dao.SpeciesDAO;
import org.ala.spatial.model.ValidTaxonName;
import org.ala.spatial.util.AndRegion;
import org.ala.spatial.util.SimpleRegion;
import org.ala.spatial.util.SimpleShapeFile;
import org.ala.spatial.util.SpatialSettings;
import org.ala.spatial.util.TabulationSettings;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
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

    private final int DEFAULT_PIXEL_DISTANCE = 50;
    private final int DEFAULT_MIN_RADIUS = 8;
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
    public //@ResponseBody
            void getTaxonNames(@PathVariable("name") String name, HttpServletRequest req, HttpServletResponse response) {
        StringBuffer slist = new StringBuffer();
        try {

            System.out.println("Looking up names for: " + name);

            name = URLDecoder.decode(name, "UTF-8");

            String[] aslist = OccurrencesCollection.findSpecies(name, 40);
            if (aslist == null) {
                aslist = new String[1];
                aslist[0] = "";
            }

            for (String s : aslist) {
                slist.append(s).append("\n");
            }

            String s = OccurrencesCollection.getCommonNames(name, aslist, 40 - ((aslist == null) ? 0 : aslist.length));
            slist.append(s);

        } catch (Exception e) {
            e.printStackTrace(System.out);
        }

        response.setContentType("text/plain;charset=UTF-8");
        ServletOutputStream o;
        try {
            o = response.getOutputStream();
            o.write(slist.toString().getBytes("UTF-8"));
            o.flush();
            o.close();
        } catch (IOException ex) {
            Logger.getLogger(SpeciesController.class.getName()).log(Level.SEVERE, null, ex);
        }

        //return slist.toString();

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

            String currentPath = TabulationSettings.base_output_dir;

            String outputpath = currentPath + File.separator + "output" + File.separator + "sampling" + File.separator;
            File fDir = new File(outputpath);
            fDir.mkdir();

            String gjsonFile = SamplingService.getLSIDAsGeoJSONIntoParts(lsid, fDir);

            long end = System.currentTimeMillis();
            System.out.println("get species by lsid geojson: " + (end - start) + "ms");

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
            //SpatialSettings ssets = new SpatialSettings();

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

            Vector dataPoints = OccurrencesCollection.getRecords(new OccurrencesFilter(species, TabulationSettings.MAX_RECORD_COUNT_CLUSTER));

            long timePoints = System.currentTimeMillis();

            Vector allFeatures = new Vector();
            // SpatialCluster3 stuff start
            SpatialCluster3 scluster = new SpatialCluster3();
            Vector<Vector<Record>> clustered = scluster.cluster(dataPoints, pdist, zoom, DEFAULT_MIN_RADIUS);
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

    @RequestMapping(value = "/cluster/{species}/area/{area}/id/{id}/now", method = RequestMethod.GET)
    public
    @ResponseBody
    Hashtable getClusteredRecords(@PathVariable("species") String species, @PathVariable("area") String area, @PathVariable("id") String id, HttpServletRequest req) {
        try {
            //SpatialSettings ssets = new SpatialSettings();

            int zoom = DEFAULT_MAP_ZOOM;
            int pdist = DEFAULT_PIXEL_DISTANCE;
            int min_radius = DEFAULT_MIN_RADIUS;

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
            try {
                if (req.getParameter("m") != null) {
                    min_radius = Integer.parseInt(req.getParameter("m"));
                }
            } catch (Exception e) {
                min_radius = DEFAULT_MIN_RADIUS;
            }

            SimpleRegion region = SimpleShapeFile.parseWKT(URLDecoder.decode(area, "UTF-8"));

            System.out.println("req.sp: " + species);
            System.out.println("req.a: " + area);
            System.out.println("req.z: " + zoom);
            System.out.println("req.d: " + pdist);

            Vector dataPoints = OccurrencesCollection.getRecords(new OccurrencesFilter(species, region, null, TabulationSettings.MAX_RECORD_COUNT_CLUSTER));

            long timePoints = System.currentTimeMillis();

            Vector allFeatures = new Vector();
            // SpatialCluster3 stuff start
            SpatialCluster3 scluster = new SpatialCluster3();

            Vector<Vector<Record>> clustered = scluster.cluster(dataPoints, pdist, zoom, min_radius);

            for (int i = 0; clustered != null && i < clustered.size(); i++) {
                Vector<Record> cluster = clustered.get(i);

                Hashtable geometry = new Hashtable();
                geometry.put("type", "Point");
                double[] coords = scluster.getCentroid(i);
                geometry.put("coordinates", coords);

                Hashtable properties = new Hashtable();
                //properties.put("cluster", cluster);
                properties.put("count", cluster.size());
                properties.put("radius", scluster.getRadius(i));
                properties.put("u", scluster.getUncertainty(i));
                properties.put("gid", id);
                properties.put("cid", i);

                double density = scluster.getDensity(i);

                properties.put("density", density);

                Map cFeature = new HashMap();
                cFeature.put("type", "Feature"); // feature.getType().getName().toString()
                //cFeature.put("id", "occurrences." + i + 1);
                cFeature.put("properties", properties);
                cFeature.put("geometry_name", "the_geom");
                cFeature.put("geometry", geometry);

                allFeatures.add(cFeature);

            }
            // SpatialCluster3 stuff end

            Hashtable data = new Hashtable();
            data.put("type", "FeatureCollection");
            data.put("features", allFeatures);

            long end = System.currentTimeMillis();

            ClusterLookup.addCluster(id, clustered);

            return data;

        } catch (Exception e) {
            System.out.println("getClusteredRecords.error: ");
            e.printStackTrace(System.out);
        }
        System.out.println("returning null");
        return null;
    }

    @RequestMapping(value = "/cluster/id/{id}/cluster/{cluster}/idx/{idx}", method = RequestMethod.GET)
    public
    @ResponseBody
    String getOccurrenceId(@PathVariable("id") String id, @PathVariable("cluster") int cluster, @PathVariable("idx") int idx, HttpServletRequest req) {
        try {
            SpatialSettings ssets = new SpatialSettings();

            return String.valueOf(ClusterLookup.getClusterId(id, cluster, idx));

        } catch (Exception e) {
            System.out.println("getOccurrenceId.error: ");
            e.printStackTrace(System.out);
        }
        System.out.println("returning null");
        return "";
    }

    @RequestMapping(value = "/cluster/lsid/{lsid}/bb", method = RequestMethod.GET)
    public
    @ResponseBody
    String getBoundingBox(@PathVariable("lsid") String lsid, HttpServletRequest req) {
        try {
            //SpatialSettings ssets = new SpatialSettings();

            lsid = URLDecoder.decode(lsid, "UTF-8");
            lsid = lsid.replaceAll("__", ".");

            return BoundingBoxes.getLsidBoundingBox(lsid);
        } catch (Exception e) {
            System.out.println("getOccurrenceId.error: ");
            e.printStackTrace(System.out);
        }
        System.out.println("returning null");
        return null;
    }

    @RequestMapping(value = "/lsid/{lsid}/count", method = RequestMethod.GET)
    public
    @ResponseBody
    String getLsidCount(@PathVariable("lsid") String lsid, HttpServletRequest req) {
        try {
            //SpatialSettings ssets = new SpatialSettings();

            lsid = URLDecoder.decode(lsid, "UTF-8");
            lsid = lsid.replaceAll("__", ".");

            ArrayList<OccurrenceRecordNumbers> orn = OccurrencesCollection.getRecordNumbers(new OccurrencesFilter(lsid, TabulationSettings.MAX_RECORD_COUNT_CLUSTER));
            int count = 0;
            if (orn != null) {
                for (OccurrenceRecordNumbers o : orn) {
                    count += o.getRecords().length;
                }
            }
            return String.valueOf(count);
        } catch (Exception e) {
            System.out.println("getOccurrenceId.error: ");
            e.printStackTrace(System.out);
        }
        System.out.println("returning null");
        return "0";
    }

    /**
     * Check if the species is sensitive
     * 
     * @param lsid
     * @return
     */
    @RequestMapping(value = "/lsid/{lsid}/sensitivity", method = RequestMethod.GET)
    public
    @ResponseBody
    String getSpeciesSensitivity(@PathVariable("lsid") String lsid) {
        try {
            lsid = URLDecoder.decode(lsid, "UTF-8");
            lsid = lsid.replaceAll("__", ".");

            return "" + SamplingService.isSensitiveRecord(lsid, null, null);
        } catch (Exception e) {
            System.out.println("species sensitivity.error: ");
            e.printStackTrace(System.out);
        }
        return "-1";
    }

    /**
     * not: only supporting square areas
     *
     * @param area
     * @param req
     * @return
     */
    @RequestMapping(value = "/cluster/area/{area}/id/{id}/now", method = RequestMethod.GET)
    public
    @ResponseBody
    Hashtable getClusteredRecordsInArea(@PathVariable("area") String area, @PathVariable("id") String id, HttpServletRequest req) {
        try {
            //SpatialSettings ssets = new SpatialSettings();

            int zoom = DEFAULT_MAP_ZOOM;
            int pdist = DEFAULT_PIXEL_DISTANCE;
            int min_radius = DEFAULT_MIN_RADIUS;

            SimpleRegion region = null;
            ArrayList<OccurrenceRecordNumbers> records = null;
            area = URLDecoder.decode(area, "UTF-8");
            if (area != null && area.startsWith("ENVELOPE")) {
                records = FilteringService.getRecords(area);
            } else {
                region = SimpleShapeFile.parseWKT(area);
            }

            SimpleRegion regionViewport = null;

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
            try {
                if (req.getParameter("a") != null) {
                    regionViewport = SimpleShapeFile.parseWKT(URLDecoder.decode(req.getParameter("a"), "UTF-8"));
                    System.out.println("a: " + req.getParameter("a"));
                }
            } catch (Exception e) {
            }
            try {
                if (req.getParameter("m") != null) {
                    min_radius = Integer.parseInt(req.getParameter("m"));
                }
            } catch (Exception e) {
                min_radius = DEFAULT_MIN_RADIUS;
            }

            System.out.println("req.a: " + area);
            System.out.println("req.z: " + zoom);
            System.out.println("req.d: " + pdist);

            System.out.println("area: " + area);

            if (regionViewport != null) {
                AndRegion ar = new AndRegion();
                ArrayList<SimpleRegion> asr = new ArrayList<SimpleRegion>(2);
                asr.add(region);
                asr.add(regionViewport);
                ar.setSimpleRegions(asr);

                region = ar;
            }
            Vector dataPoints = OccurrencesCollection.getRecords(new OccurrencesFilter(null, region, records, TabulationSettings.MAX_RECORD_COUNT_CLUSTER));

            long timePoints = System.currentTimeMillis();

            Vector allFeatures = new Vector();
            // SpatialCluster3 stuff start
            SpatialCluster3 scluster = new SpatialCluster3();
            Vector<Vector<Record>> clustered = scluster.cluster(dataPoints, pdist, zoom, min_radius);

            long timeClustering = System.currentTimeMillis();

            for (int i = 0; clustered != null && i < clustered.size(); i++) {
                Vector<Record> cluster = clustered.get(i);

                Hashtable geometry = new Hashtable();
                geometry.put("type", "Point");
                double[] coords = scluster.getCentroid(i);
                geometry.put("coordinates", coords);

                Hashtable properties = new Hashtable();
                //properties.put("cluster", cluster);
                properties.put("count", cluster.size());
                properties.put("radius", scluster.getRadius(i));
                properties.put("u", scluster.getUncertainty(i));
                properties.put("gid", id);
                properties.put("cid", i);

                double density = scluster.getDensity(i);

                properties.put("density", density);

                Map cFeature = new HashMap();
                cFeature.put("type", "Feature"); // feature.getType().getName().toString()
                //cFeature.put("id", "occurrences." + i + 1);
                cFeature.put("properties", properties);
                cFeature.put("geometry_name", "the_geom");
                cFeature.put("geometry", geometry);

                allFeatures.add(cFeature);

            }
            // SpatialCluster3 stuff end

            Hashtable data = new Hashtable();
            data.put("type", "FeatureCollection");
            data.put("features", allFeatures);

            ClusterLookup.addCluster(id, clustered);

            return data;

        } catch (Exception e) {
            System.out.println("getClusteredRecords.error: ");
            e.printStackTrace(System.out);
        }
        System.out.println("returning null");
        return null;
    }

    @RequestMapping(value = "/shape/register", method = RequestMethod.POST)
    public
    @ResponseBody
    String registerShape(HttpServletRequest req) {
        try {
            String shape = req.getParameter("area");
            if (shape.length() > 200) {
                System.out.println("registering shape: " + shape.substring(0, 200) + "...");
            } else {
                System.out.println("registering shape: " + shape);
            }
            //shape = URLDecoder.decode(shape, "UTF-8");
            SimpleRegion region = SimpleShapeFile.parseWKT(shape);

            String id = String.valueOf(System.currentTimeMillis());

            ShapeLookup.addShape(id, region);

            System.out.println("successfully registered shape: " + id);

            return id;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @RequestMapping(value = "/shape/lookup", method = RequestMethod.POST)
    public
    @ResponseBody
    String lookupShape(HttpServletRequest req) {
        try {
            String table = req.getParameter("table");
            String value = req.getParameter("value").replace("_", " ");
            System.out.println("lookup shape: " + table + " " + value);

            //prevents duplication of this table + value in the shapes list
            String id = table + "_" + value; //String.valueOf(System.currentTimeMillis());

            boolean ret = ShapeLookup.addShape(id, table, value);

            System.out.println("successfully registered shape: " + id);

            if (ret) {
                return id;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * not: only supporting square areas
     *
     * @param area
     * @param req
     * @return
     */
    @RequestMapping(value = "/info/now", method = RequestMethod.GET)
    public
    @ResponseBody
    String getSpeciesInfoInArea(HttpServletRequest req) {
        try {

            String area = req.getParameter("area");
            if (area == null) {
                area = "none";
            } else {
                area = URLDecoder.decode(area, "UTF-8");
            }

            String activearea = req.getParameter("aa");
            if (activearea == null) {
                activearea = "none";
            } else {
                activearea = URLDecoder.decode(activearea, "UTF-8");
            }

            String[] lsids = req.getParameterValues("lsid");

            if (lsids == null) {
                return "";
            }

            System.out.println("[[[]]] getSpeciesInfoInArea: " + area + "\nlsids:" + lsids + "\nactivearea:" + activearea);

            SimpleRegion region = null;
            ArrayList<OccurrenceRecordNumbers> records = null;
            area = URLDecoder.decode(area, "UTF-8");
            if (area != null && area.startsWith("ENVELOPE")) {
                records = FilteringService.getRecords(area);
            } else {
                region = SimpleShapeFile.parseWKT(area);
            }

            SimpleRegion aaregion = null;
            if (activearea != null) {
                if (activearea.startsWith("ENVELOPE")) {
                    ArrayList<OccurrenceRecordNumbers> rec = FilteringService.getRecords(activearea);
                    if (records == null || records.size() == 0) {
                        records = rec;
                    } else if (rec != null && rec.size() > 0) {
                        //join
                        for (int i = 0; i < records.size(); i++) {
                            for (int j = 0; j < rec.size(); j++) {
                                if (records.get(i).getName().equals(rec.get(j).getName())) {
                                    int[] recA = rec.get(j).getRecords();
                                    int[] recordsA = records.get(i).getRecords();

                                    int[] newrec = new int[recA.length + recordsA.length];
                                    System.arraycopy(recordsA, 0, newrec, 0, recordsA.length);
                                    System.arraycopy(recA, 0, newrec, recordsA.length, recA.length);

                                    //write back
                                    records.get(i).setRecords(newrec);
                                }
                            }
                        }
                    }
                } else {
                    aaregion = SimpleShapeFile.parseWKT(activearea);
                }
            }
            if (aaregion != null) {
                if (region == null) {
                    region = aaregion;
                } else {
                    AndRegion ar = new AndRegion();
                    ArrayList<SimpleRegion> asr = new ArrayList<SimpleRegion>(2);
                    asr.add(region);
                    asr.add(aaregion);
                    ar.setSimpleRegions(asr);

                    region = ar;
                }
            }

            String spcount = "";
            Vector dataPoints = new Vector();
            for (int i = 0; i < lsids.length; i++) {
                String lsid = lsids[i];
                System.out.println("Looking for species info: " + lsid);
                if (lsid.equalsIgnoreCase("aa")) {
                    Vector dp = OccurrencesCollection.getRecords(new OccurrencesFilter(null, region, records, TabulationSettings.MAX_RECORD_COUNT_CLUSTER));
                    if (dp.size() > 0) {
                        dataPoints.addAll(dp);
                    }
                } else {
                    dataPoints.addAll(OccurrencesCollection.getRecords(new OccurrencesFilter(lsid, region, records, TabulationSettings.MAX_RECORD_COUNT_CLUSTER)));
                }
            }

            StringBuffer sbInfo = new StringBuffer();
            for (int i = 0; i < dataPoints.size(); i++) {
                Record r = (Record) dataPoints.get(i);
                if (i > 0) {
                    sbInfo.append(",");
                }
                sbInfo.append(r.getId());
            }

            System.out.println("species info at location: " + spcount);
            return sbInfo.toString();
        } catch (Exception e) {
            System.out.println("error getting species info");
            e.printStackTrace();
        }

        return "";
    }

    @RequestMapping(value = "/lsid/register", method = RequestMethod.POST)
    public
    @ResponseBody
    String lsidRecords(HttpServletRequest req) {
        try {
            String id = String.valueOf(System.currentTimeMillis());

            String[] lsids = req.getParameter("lsids").split(",");

            for (int i = 0; i < lsids.length; i++) {
                lsids[i] = lsids[i].replaceAll("__", ".");
            }

            int count = OccurrencesCollection.registerLSID(id, lsids);

            System.out.println("successfully registered records in lsids: " + count);

            return id;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @RequestMapping(value = "/area/register", method = RequestMethod.POST)
    public
    @ResponseBody
    String areaRecords(HttpServletRequest req) {
        try {
            String id = String.valueOf(System.currentTimeMillis());

            String areaParam = req.getParameter("area");

            SimpleRegion region = null;
            ;
            areaParam = URLDecoder.decode(areaParam, "UTF-8");

            int count = 0;
            if (areaParam != null && areaParam.startsWith("ENVELOPE")) {
                ArrayList<OccurrenceRecordNumbers> records = FilteringService.getRecords(areaParam);
                count = OccurrencesCollection.registerRecords(id, records);
            } else {
                region = SimpleShapeFile.parseWKT(areaParam);
                count = OccurrencesCollection.registerArea(id, region);
            }

            System.out.println("successfully registered records in area: " + count);

            return id;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @RequestMapping(value = "/highlight/register", method = RequestMethod.GET)
    public
    @ResponseBody
    String highlightRecords(HttpServletRequest req) {
        try {
            String id = String.valueOf(System.currentTimeMillis());

            String lsid = req.getParameter("lsid").replaceAll("__", ".");
            String pid = req.getParameter("pid");
            String include = req.getParameter("include");

            pid = URLDecoder.decode(pid, "UTF-8");

            int count = 0;
            if (pid != null) {
                count = OccurrencesCollection.registerHighlight(lsid, id, pid, (include != null));
            }

            System.out.println("successfully registered records in highlight: " + count);

            return id;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @RequestMapping(value = "/colouroptions", method = RequestMethod.GET)
    public
    @ResponseBody
    String speciesColourOptions(HttpServletRequest req) {
        try {
            String id = String.valueOf(System.currentTimeMillis());

            String lsid = req.getParameter("lsid").replaceAll("__", ".");

            String list = SpeciesColourOption.getColourOptions(lsid);

            return list;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @RequestMapping(value = "/colourlegend", method = RequestMethod.GET)
    public
    @ResponseBody
    String speciesColourLegend(HttpServletRequest req) {
        try {
            String lsid = URLDecoder.decode(req.getParameter("lsid"), "UTF-8").replaceAll("__", ".");
            String colourMode = URLDecoder.decode(req.getParameter("colourmode"), "UTF-8");

            if(!SamplingLoadedPointsService.isLoadedPointsLSID(lsid)) {
                try {
                    String pid = SpeciesColourOption.getColourLegend(lsid, colourMode);
                    return pid;
                } catch (Exception e) {

                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @RequestMapping(value = "/metadata/{lsid:.+}", method = RequestMethod.GET)
    public ModelAndView speciesMetadata(@PathVariable("lsid") String lsid) {
        ModelMap modelMap = null;
        try {

            String surl = "http://biocache.ala.org.au/occurrences/searchByTaxon.json?q=" + lsid;

            System.out.println("Checking for species metadata at: " + surl);


            HttpClient client = new HttpClient();
            GetMethod post = new GetMethod(surl);
            post.addRequestHeader("Accept", "application/json, text/javascript, */*");

            int result = client.executeMethod(post);
            System.out.println("result: " + result);

            if (result == 200) {
                System.out.println("Loading metadata from biocache");
                modelMap = loadBiocacheSpeciesMetadata(lsid, post.getResponseBodyAsString());
            } else {
                System.out.println("Loading metadata from disk");
                modelMap = loadActiveAreaSpeciesMetadata(lsid);
            }

        } catch (Exception e) {
            System.out.println("error generating species metadata");
            e.printStackTrace(System.out);
        }

        return new ModelAndView("species/metadata", modelMap);
    }

    private ModelMap loadBiocacheSpeciesMetadata(String lsid, String slist) {
        ModelMap modelMap = null;
        String[] classificationList = {"kingdom", "phylum", "class", "order", "family", "genus", "species", "subspecies"};

        try {
            modelMap = new ModelMap();
            modelMap.addAttribute("lsid", lsid);
            modelMap.addAttribute("metadatatype", "biocache");


            ObjectMapper mapper = new ObjectMapper();
            JsonFactory factory = mapper.getJsonFactory();
            JsonParser jp = factory.createJsonParser(slist);
            JsonNode root = mapper.readTree(jp);

            JsonNode joOcc = root.get("searchResult").get("occurrences").get(0);
            if (joOcc == null) {
                System.out.println("not available in biocache, checking on local");
                return loadActiveAreaSpeciesMetadata(lsid);
            }

            // 1. add the Taxonomic hierarchy
            Map classification = new TreeMap();
            String[] entityQuery = root.get("entityQuery").getTextValue().split(":");

            modelMap.addAttribute("speciesname", entityQuery[1].trim());
            modelMap.addAttribute("speciesrank", entityQuery[0].trim());

            String spClass = entityQuery[0].trim();
            Vector cList = new Vector();
            for (String c : classificationList) {
                if (c.equals(spClass)) {
                    break;
                }
                classification.put(c, joOcc.get(c.replace("ss", "zz")).getTextValue() + ";" + joOcc.get(c + "Lsid").getTextValue());

                cList.add(c + ";" + joOcc.get(c.replace("ss", "zz")).getTextValue() + ";" + joOcc.get(c + "Lsid").getTextValue());
            }
            cList.add(spClass + ";" + entityQuery[1].trim() + ";" + lsid);
            classification.put(entityQuery[0].trim(), lsid);
            modelMap.addAttribute("classificationList", classificationList);
            modelMap.addAttribute("classification", classification);
            modelMap.addAttribute("cList", cList);


            // 2. add the Number of species
            // add the following snippet IF you need a list of species as well
//            String legend = SpeciesColourOption.getColourLegend(lsid, "species", false);
//            String[] lines = legend.split("\n");
//            modelMap.addAttribute("species_count", "" + (lines.length - 1));
            int geoOccCount = 0;
            int geoSpCount = 0;
            ArrayList<OccurrencesSpeciesList> osla = OccurrencesCollection.getSpeciesList(new OccurrencesFilter(lsid, Integer.MAX_VALUE));
            if (osla != null) {
                if (osla.size() > 0) {
                    geoOccCount = osla.get(0).getOccurrencesCount();
                    geoSpCount = osla.get(0).getSpeciesCount();
                }
            }
            modelMap.addAttribute("geoOccCount", geoOccCount);
            modelMap.addAttribute("geoSpCount", geoSpCount);


            // 3. add the Number of occurrences
            modelMap.addAttribute("occ_count", root.get("searchResult").get("totalRecords").getValueAsText());

            // 4. add the Institutions
//            Map institutions = new TreeMap();
//            JsonNode facetResults = root.get("searchResult").get("facetResults");
//            for (int i = 0; i < facetResults.size(); i++) {
//                JsonNode jofr = facetResults.get(i);
//                if (jofr.get("fieldName").getTextValue().equalsIgnoreCase("data_resource")) {
//                    JsonNode jadr = jofr.get("fieldResult");
//                    for (int j = 0; j < jadr.size(); j++) {
//                        JsonNode jodr = jadr.get(j);
//                        institutions.put(jodr.get("label").getTextValue(), jodr.get("count").getValueAsText());
//                    }
//                }
//            }
//            modelMap.addAttribute("institutions", institutions);

            Map institutions = new TreeMap();
            String legend = SpeciesColourOption.getColourLegend(lsid, "in", false);
            String[] lines = legend.split("\n");
            for (String l : lines) {
                String[] in = l.split(",");
                if (in.length < 5) continue;
                institutions.put((("".equals(in[0])) ? "Unknown" : in[0]), in[4]);
            }
            modelMap.addAttribute("institutions", institutions);

        } catch (Exception e) {
            System.out.println("Error loading species metadata from biocache");
            e.printStackTrace(System.out);
        }

        return modelMap;
    }

    @RequestMapping(value = "/metadata/aa/{lsid:.+}", method = RequestMethod.GET)
    public ModelAndView activeAreaSpeciesMetadata(@PathVariable("lsid") String lsid) {
        ModelMap modelMap = null;
        try {
            modelMap = loadActiveAreaSpeciesMetadata(lsid);
        } catch (Exception e) {
            System.out.println("error generating species metadata");
            e.printStackTrace(System.out);
        }

        return new ModelAndView("species/metadata", modelMap);
    }

    private ModelMap loadActiveAreaSpeciesMetadata(String lsid) {
        ModelMap modelMap = null;

        try {
            modelMap = new ModelMap();
            modelMap.addAttribute("lsid", lsid);
            modelMap.addAttribute("speciesname", "Occurrences in Active area");
            modelMap.addAttribute("metadatatype", "activearea");

            // use the following snippet IF you need the list of species as well
            // occ_count
//            System.out.println("Getting occ count...");
//            ArrayList<OccurrenceRecordNumbers> orn = OccurrencesCollection.getRecordNumbers(new OccurrencesFilter(lsid, TabulationSettings.MAX_RECORD_COUNT_CLUSTER));
//            int count = 0;
//            if (orn != null) {
//                for (OccurrenceRecordNumbers o : orn) {
//                    count += o.getRecords().length;
//                }
//            }
//            modelMap.addAttribute("occ_count", count);
//            System.out.println("occ_count: " + count);
//
//            System.out.println("Getting sp count...");
//            String legend = SpeciesColourOption.getColourLegend(lsid, "taxon_name", false);
//            String[] lines = legend.split("\n");
//            modelMap.addAttribute("species_count", "" + (lines.length - 1));
//            System.out.println("sp_count: " + (lines.length - 1));


            int geoOccCount = 0;
            int geoSpCount = 0;
            ArrayList<OccurrencesSpeciesList> osla = OccurrencesCollection.getSpeciesList(new OccurrencesFilter(lsid, Integer.MAX_VALUE));
            if (osla != null) {
                if (osla.size() > 0) {
                    geoOccCount = osla.get(0).getOccurrencesCount();
                    geoSpCount = osla.get(0).getSpeciesCount(); 
                }
            }
            modelMap.addAttribute("geoOccCount", geoOccCount);
            modelMap.addAttribute("geoSpCount", geoSpCount);

            Map institutions = new TreeMap();
            String legend = SpeciesColourOption.getColourLegend(lsid, "in", false);
            String[] lines = legend.split("\n");
            for (String l : lines) {
                String[] in = l.split(",");
                if (in.length < 5) continue;
                institutions.put((("".equals(in[0])) ? "Unknown" : in[0]), in[4]);
            }
            modelMap.addAttribute("institutions", institutions);

        } catch (Exception e) {
            System.out.println("Unable to generate metadata for species in active area");
            e.printStackTrace(System.out);
        }

        return modelMap;
    }
}
