/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.ala.spatial.util;

import au.org.ala.spatial.data.LsidCounts;
import au.org.emii.portal.util.LayerSelection;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.ala.layers.legend.QueryField;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * common data store
 * <p/>
 * 1. alaspatial environmental and contextual layer names (use in LayersUtil) 2.
 * alaspatial layers associations, environmental and contextual layer names (use
 * in EnvironmentalList) 3. layer list as string and JSON (layer tree, (2))
 *
 * @author Adam
 */
public class CommonData {

    private static Logger logger = Logger.getLogger(CommonData.class);

    //common data
    static public final String WORLD_WKT = "POLYGON((-179.999 -89.999,-179.999 89.999,179.999 84.999,179.999 -89.999,-179.999 -89.999))";
    //NC: 20130319 changed to using the correct direction 
    static public final String AUSTRALIA_WKT = "POLYGON((112.0 -44.0,154.0 -44.0,154.0 -9.0,112.0 -9.0,112.0 -44.0))";
    //common parameters
    static final String SAT_URL = "sat_url";
    static final String GEOSERVER_URL = "geoserver_url";
    static final String LAYERS_URL = "layers_url";
    static final String WEBPORTAL_URL = "webportal_url";
    static final String BIE_URL = "bie_url";
    static final String BIOCACHE_SERVICE_URL = "biocache_service_url";
    static final String BIOCACHE_WEBAPP_URL = "biocache_webapp_url";
    static final String SPECIES_LIST_URL = "species_list_url";
    static final String COLLECTORY_URL = "collectory_url";
    static final String DEFAULT_UPLOAD_SAMPLING = "default_upload_sampling";
    static final String MAX_Q_LENGTH = "max_q_length";
    static final String BIOCACHE_QC = "biocache_qc";
    static final String ANALYSIS_LAYER_SETS = "analysis_layer_sets";
    static final String MAX_AREA_FOR_ENDEMIC = "max_area_endemic";
    static final String EXTRA_DOWNLOAD_FIELDS = "occurrence_extra_download";
    static final String DISPLAY_POINTS_OF_INTEREST = "display_points_of_interest";
    static final String CUSTOM_FACETS = "custom_facets";
    static final String AREA_REPORT_FACETS = "area_report_facets";
    //NC 20131017 - the default facets supplied by the biocache WS that are ignored.
    static final String IGNORED_FACETS = "default_facets_ignored";
    //(2) for EnvironmentalList
    static JSONObject distances;
    static HashMap<String, HashMap<String, Double>> distances_map;
    static JSONObject copy_distances;
    static HashMap<String, HashMap<String, Double>> copy_distances_map;
    //(3) for layer list json
    static JSONArray layerlistJSON = null;
    static JSONArray copy_layerlistJSON = null;
    static String defaultFieldString = null;
    //(4) species with distribution layres
    /**
     * key = LSID value = list of WMS names
     */
    static HashMap<String, String[]> species_wms_layers = null;
    static HashMap<String, String[]> copy_species_wms_layers = null;
    static HashMap<String, String[]> species_metadata_layers = null;
    static HashMap<String, String[]> copy_species_metadata_layers = null;
    static HashMap<String, String[]> species_spcode_layers = null;
    static HashMap<String, String[]> copy_species_spcode_layers = null;
    static HashMap<String, String[]> species_wms_layers_by_spcode = null;
    static HashMap<String, String[]> copy_species_wms_layers_by_spcode = null;
    static HashMap<String, String[]> checklistspecies_wms_layers = null;
    static HashMap<String, String[]> copy_checklistspecies_wms_layers = null;
    static HashMap<String, String[]> checklistspecies_metadata_layers = null;
    static HashMap<String, String[]> copy_checklistspecies_metadata_layers = null;
    static HashMap<String, String[]> checklistspecies_spcode_layers = null;
    static HashMap<String, String[]> copy_checklistspecies_spcode_layers = null;
    static HashMap<String, String[]> checklistspecies_wms_layers_by_spcode = null;
    static HashMap<String, String[]> copy_checklistspecies_wms_layers_by_spcode = null;
    // download reasons
    static JSONArray download_reasons;
    static JSONArray copy_download_reasons;
    //Common
    static public String satServer;
    static public String geoServer;
    static public String layersServer;
    static public String webportalServer;
    static public String bieServer;
    static public String biocacheServer;
    static public String biocacheWebServer;
    static public String speciesListServer;
    static public String collectoryServer;
    static public int maxQLength;
    static public Properties settings;
    //lsid counts, for species autocomplete
    static public LsidCounts lsidCounts;
    static public String biocacheQc;
    static public ArrayList<LayerSelection> analysisLayerSets;
    static public String wkhtmltoimage_cmd;
    static public String convert_cmd;
    static public String print_output_path;
    static public String print_output_url;
    static public String[][] facetNameExceptions; //{{"cl22", "state"}, {"cl959", "places"}, {"cl20", "ibra"}, {"cl21", "imcra"}};
    static public Set<String> biocacheLayerList;
    static public int maxEndemicArea;
    static public String extraDownloadFields = "coordinateUncertaintyInMeters";
    static public boolean displayPointsOfInterest;
    static public String[] customFacets;
    static public List<String> ignoredFacets;
    static public String[] areaReportFacets;

    /*
     * initialize common data from geoserver and satserver
     */
    static public void init(Properties settings) {
        CommonData.settings = settings;

        //Common
        satServer = settings.getProperty(SAT_URL);
        geoServer = settings.getProperty(GEOSERVER_URL);
        layersServer = settings.getProperty(LAYERS_URL);
        webportalServer = settings.getProperty(WEBPORTAL_URL);
        bieServer = settings.getProperty(BIE_URL);
        biocacheServer = settings.getProperty(BIOCACHE_SERVICE_URL);
        biocacheWebServer = settings.getProperty(BIOCACHE_WEBAPP_URL);
        speciesListServer = settings.getProperty(SPECIES_LIST_URL);
        collectoryServer = settings.getProperty(COLLECTORY_URL);
        defaultFieldString = settings.getProperty(DEFAULT_UPLOAD_SAMPLING);
        maxQLength = Integer.parseInt(settings.getProperty(MAX_Q_LENGTH));
        //handle the situation where there is no config value for endemic area and use default value of 50,000km
        String maxendemic = settings.getProperty(MAX_AREA_FOR_ENDEMIC) != null ? settings.getProperty(MAX_AREA_FOR_ENDEMIC) : "50000";
        maxEndemicArea = Integer.parseInt(maxendemic);
        biocacheQc = settings.getProperty(BIOCACHE_QC);
        if (biocacheQc == null) {
            biocacheQc = "";
        }
        wkhtmltoimage_cmd = settings.getProperty("wkhtmltoimage_cmd");
        convert_cmd = settings.getProperty("convert_cmd");
        print_output_path = settings.getProperty("print_output_path");
        print_output_url = settings.getProperty("print_output_url");
        facetNameExceptions = parseFacetNameExceptions(settings.getProperty("facet_name_exceptions"));
        customFacets = settings.containsKey(CUSTOM_FACETS) ? settings.getProperty(CUSTOM_FACETS).split(",") : new String[]{};
        ignoredFacets = Arrays.asList(settings.containsKey(IGNORED_FACETS) ? settings.getProperty(IGNORED_FACETS).split(",") : new String[]{});
        areaReportFacets = settings.containsKey(AREA_REPORT_FACETS) ? settings.getProperty(AREA_REPORT_FACETS).split(",") : new String[]{};
        if (settings.containsKey(EXTRA_DOWNLOAD_FIELDS)) {
            extraDownloadFields = settings.getProperty(EXTRA_DOWNLOAD_FIELDS);
        }

        if (settings.containsKey(DISPLAY_POINTS_OF_INTEREST)) {
            displayPointsOfInterest = Boolean.parseBoolean(settings.getProperty(DISPLAY_POINTS_OF_INTEREST));
        } else {
            displayPointsOfInterest = false;
        }

        setupAnalysisLayerSets();

        initLayerDistances();

        //(3) for layer list json
        initLayerList();

        //(4) for species wms layers
        initSpeciesWMSLayers();

        //(5) for layer to facet name mapping
        readLayerInfo();

        //(6) for common facet name and value conversions
        initI18nProperies();

        //(7) lsid counts
        LsidCounts lc = new LsidCounts();
        if (lc.getSize() > 0) {
            lsidCounts = lc;
        }

        //(8) cache shape files used during coordinate uploads
        initSimpleShapeFileCache(defaultFieldString.split(","));

        // load the download reasons
        initDownloadReasons();
        if (copy_download_reasons != null) {
            download_reasons = copy_download_reasons;
        }

        //keep a list of biocache field names to know what is available for queries
        initBiocacheLayerList();

        //(2) for EnvironmentalList
        if (copy_distances != null) {
            distances = copy_distances;
        }

        if (copy_distances_map != null) {
            distances_map = copy_distances_map;
        }

        //(3) for layer list json        
        if (copy_layerlistJSON != null) {
            layerlistJSON = copy_layerlistJSON;
        }

        //(4) for species wms distributions & checklists
        if (copy_species_wms_layers != null) {
            species_wms_layers = copy_species_wms_layers;
        }
        if (copy_species_metadata_layers != null) {
            species_metadata_layers = copy_species_metadata_layers;
        }
        if (copy_species_spcode_layers != null) {
            species_spcode_layers = copy_species_spcode_layers;
        }
        if (copy_species_wms_layers_by_spcode != null) {
            species_wms_layers_by_spcode = copy_species_wms_layers_by_spcode;
        }
        if (copy_checklistspecies_wms_layers != null) {
            checklistspecies_wms_layers = copy_checklistspecies_wms_layers;
        }
        if (copy_checklistspecies_metadata_layers != null) {
            checklistspecies_metadata_layers = copy_checklistspecies_metadata_layers;
        }
        if (copy_checklistspecies_spcode_layers != null) {
            checklistspecies_spcode_layers = copy_checklistspecies_spcode_layers;
        }
        if (copy_species_wms_layers_by_spcode != null) {
            checklistspecies_wms_layers_by_spcode = copy_checklistspecies_wms_layers_by_spcode;
        }
    }

    static public JSONArray getDownloadReasons() {
        return download_reasons;
    }

    static public JSONObject getDistances() {
        return distances;
    }

    static public void initLayerDistances() {
        copy_distances = null;
        copy_distances_map = null;
        logger.debug("CommonData::initLayerDistances()");
        String url = satServer + "/ws/layerdistances";
        try {
            logger.debug(url);
            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(url);

            int result = client.executeMethod(get);

            if (result == 200) {
                copy_distances = JSONObject.fromObject(get.getResponseBodyAsString());


                //make map
                copy_distances_map = new HashMap<String, HashMap<String, Double>>();
                for (Object okey : copy_distances.keySet()) {
                    Double d = copy_distances.getDouble((String) okey);
                    String[] parts = ((String) okey).split(" ");

                    HashMap<String, Double> part = copy_distances_map.get(parts[0]);
                    if (part == null) {
                        copy_distances_map.put(parts[0], part = new HashMap<String, Double>());
                    }
                    part.put(parts[1], d);


                    part = copy_distances_map.get(parts[1]);
                    if (part == null) {
                        copy_distances_map.put(parts[1], part = new HashMap<String, Double>());
                    }
                    part.put(parts[0], d);
                }
            }
        } catch (Exception e) {
            copy_distances = null;
            copy_distances_map = null;
            logger.error("error getting layer distances", e);
        }
    }

    static public HashMap<String, HashMap<String, Double>> getDistancesMap() {
        return distances_map;
    }

    static void initLayerList() {
        copy_layerlistJSON = null;
        String layersListURL = layersServer + "/layers";
        try {

            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(layersListURL);
            get.addRequestHeader("Accept", "application/json, text/javascript, */*");

            int result = client.executeMethod(get);

            if (result == 200) {
                copy_layerlistJSON = JSONArray.fromObject(get.getResponseBodyAsString());
            }

            addFieldsToLayers(copy_layerlistJSON);
        } catch (Exception e) {
            logger.error("error getting layers list: " + layersListURL, e);
            copy_layerlistJSON = null;
        }
    }

    static void addFieldsToLayers(JSONArray joLayers) throws Exception {
        //get field id with classes
        String fieldsURL = layersServer + "/fields";
        HttpClient client = new HttpClient();
        GetMethod get = new GetMethod(fieldsURL);
        int result = client.executeMethod(get);
        if (result != 200) {
            throw new Exception("cannot retrive field list: " + fieldsURL);
        }
        String fields = get.getResponseBodyAsString();
        JSONArray ja = JSONArray.fromObject(fields);

        //attach to a new JSONArray in joLayers named "fields"
        for (int j = 0; j < joLayers.size(); j++) {
            JSONObject layer = joLayers.getJSONObject(j);
            if (layer.containsKey("id")) {
                for (int i = 0; i < ja.size(); i++) {
                    JSONObject jo = ja.getJSONObject(i);
                    if (
                            jo.containsKey("defaultlayer") && jo.getString("defaultlayer").equals("true")

                                    && jo.containsKey("spid") && jo.getString("spid").equals(layer.getString("id"))) {
                        //add to layer
                        if (!layer.containsKey("fields")) {
                            layer.put("fields", new JSONArray());
                        }
                        layer.getJSONArray("fields").add(jo);
                    }
                }
            }
        }
    }

    static public JSONArray getLayerListJSONArray() {
        return layerlistJSON;
    }

    private static void initSpeciesWMSLayers() {
        try {
            String layersListURL = layersServer + "/distributions";
            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(layersListURL);
            get.addRequestHeader("Accept", "application/json, text/javascript, */*");
            get.addRequestHeader("Accept-Encoding", "gzip");

            copy_species_wms_layers = new HashMap<String, String[]>();
            copy_species_metadata_layers = new HashMap<String, String[]>();
            copy_species_spcode_layers = new HashMap<String, String[]>();
            copy_species_wms_layers_by_spcode = new HashMap<String, String[]>();

            int result = client.executeMethod(get);
            String slist = get.getResponseBodyAsString();

            JSONArray ja = JSONArray.fromObject(slist);

            logger.debug(ja.size() + " species wms distributions");

            for (int i = 0; i < ja.size(); i++) {
                JSONObject jo = ja.getJSONObject(i);
                if (jo.containsKey("lsid") && jo.containsKey("wmsurl")) {
                    //manage lsids with multiple wmsurls
                    String lsid = jo.getString("lsid");

                    //wms
                    String[] urls = copy_species_wms_layers.get(lsid);
                    if (urls != null) {
                        String[] newUrls = new String[urls.length + 1];
                        System.arraycopy(urls, 0, newUrls, 0, urls.length);
                        newUrls[newUrls.length - 1] = jo.getString("wmsurl");
                        urls = newUrls;
                    } else {
                        urls = new String[]{jo.getString("wmsurl")};
                    }
                    copy_species_wms_layers.put(lsid, urls);

                    //metadata
                    String m = "";
                    if (jo.containsKey("metadata_u")) {
                        m = jo.getString("metadata_u");
                    }
                    String[] md = copy_species_metadata_layers.get(lsid);
                    if (md != null) {
                        String[] newMd = new String[md.length + 1];
                        System.arraycopy(md, 0, newMd, 0, md.length);
                        newMd[newMd.length - 1] = m;
                        md = newMd;
                    } else {
                        md = new String[]{m};
                    }
                    copy_species_metadata_layers.put(lsid, md);

                    //spcode
                    m = "";
                    if (jo.containsKey("spcode")) {
                        m = jo.getString("spcode");
                    }
                    md = copy_species_spcode_layers.get(lsid);
                    if (md != null) {
                        String[] newMd = new String[md.length + 1];
                        System.arraycopy(md, 0, newMd, 0, md.length);
                        newMd[newMd.length - 1] = m;
                        md = newMd;
                    } else {
                        md = new String[]{m};
                    }
                    copy_species_spcode_layers.put(lsid, md);

                    //others
                    String spcode = null;
                    if (jo.containsKey("spcode")) {
                        spcode = jo.getString("spcode");
                    }
                    lsid = null;
                    if (jo.containsKey("lsid")) {
                        lsid = jo.getString("lsid");
                    }
                    String pid = null;
                    if (jo.containsKey("pid")) {
                        pid = jo.getString("pid");
                    }
                    String type = null;
                    if (jo.containsKey("type")) {
                        type = jo.getString("type");
                    }
                    copy_species_wms_layers_by_spcode.put(spcode, new String[]{jo.getString("scientific"), jo.getString("wmsurl"), m, lsid, pid, type});
                }
            }

            //repeat to append checklists
            layersListURL = layersServer + "/checklists";
            client = new HttpClient();
            get = new GetMethod(layersListURL);
            get.addRequestHeader("Accept", "application/json, text/javascript, */*");

            result = client.executeMethod(get);

            get.addRequestHeader("Accept-Encoding", "gzip");

            result = client.executeMethod(get);

            copy_checklistspecies_wms_layers = new HashMap<String, String[]>();
            copy_checklistspecies_metadata_layers = new HashMap<String, String[]>();
            copy_checklistspecies_spcode_layers = new HashMap<String, String[]>();
            copy_checklistspecies_wms_layers_by_spcode = new HashMap<String, String[]>();

            if (result == 200) {
                slist = get.getResponseBodyAsString();
                ja = JSONArray.fromObject(slist);

                logger.debug(ja.size() + " species wms checklists");

                for (int i = 0; i < ja.size(); i++) {
                    JSONObject jo = ja.getJSONObject(i);
                    if (jo.containsKey("lsid") && jo.containsKey("wmsurl")) {
                        //manage lsids with multiple wmsurls
                        String lsid = jo.getString("lsid");

                        //wms
                        String[] urls = copy_checklistspecies_wms_layers.get(lsid);
                        if (urls != null) {
                            String[] newUrls = new String[urls.length + 1];
                            System.arraycopy(urls, 0, newUrls, 0, urls.length);
                            newUrls[newUrls.length - 1] = jo.getString("wmsurl");
                            urls = newUrls;
                        } else {
                            urls = new String[]{jo.getString("wmsurl")};
                        }
                        copy_checklistspecies_wms_layers.put(lsid, urls);

                        //metadata
                        String m = "";
                        if (jo.containsKey("metadata_u")) {
                            m = jo.getString("metadata_u");
                        }
                        String[] md = copy_checklistspecies_metadata_layers.get(lsid);
                        if (md != null) {
                            String[] newMd = new String[md.length + 1];
                            System.arraycopy(md, 0, newMd, 0, md.length);
                            newMd[newMd.length - 1] = m;
                            md = newMd;
                        } else {
                            md = new String[]{m};
                        }
                        copy_checklistspecies_metadata_layers.put(lsid, md);

                        //spcode
                        m = "";
                        if (jo.containsKey("spcode")) {
                            m = jo.getString("spcode");
                        }
                        md = copy_checklistspecies_spcode_layers.get(lsid);
                        if (md != null) {
                            String[] newMd = new String[md.length + 1];
                            System.arraycopy(md, 0, newMd, 0, md.length);
                            newMd[newMd.length - 1] = m;
                            md = newMd;
                        } else {
                            md = new String[]{m};
                        }
                        copy_checklistspecies_spcode_layers.put(lsid, md);

                        //by spcode
                        String spcode = jo.getString("spcode");
                        copy_checklistspecies_wms_layers_by_spcode.put(spcode, new String[]{jo.getString("scientific"), jo.getString("wmsurl"), m});
                    }
                }
            }
        } catch (Exception e) {
            copy_species_wms_layers = null;
            copy_species_metadata_layers = null;
            copy_species_spcode_layers = null;
            copy_species_wms_layers_by_spcode = null;

            copy_checklistspecies_wms_layers = null;
            copy_checklistspecies_metadata_layers = null;
            copy_checklistspecies_spcode_layers = null;
            copy_checklistspecies_wms_layers_by_spcode = null;


            logger.error("error getting species and distributions checklists", e);
        }
    }

    /**
     * returns array of WMS species requests
     */
    static public String[] getSpeciesDistributionWMS(String lsids) {
        if (species_wms_layers == null || lsids == null) {
            return null;
        }
        String[] lsid = lsids.split(",");
        ArrayList<String[]> wmsurls = new ArrayList<String[]>();
        int count = 0;
        for (String s : lsid) {
            String[] urls = species_wms_layers.get(s);
            if (urls != null) {
                count += urls.length;
                wmsurls.add(urls);
            }
        }
        String[] wms = null;
        if (count > 0) {
            wms = new String[count];
            int pos = 0;
            for (String[] s : wmsurls) {
                System.arraycopy(s, 0, wms, pos, s.length);
            }
        }
        return wms;
    }

    /**
     * returns array of metadata_u species requests
     */
    static public String[] getSpeciesDistributionMetadata(String lsids) {
        if (species_wms_layers == null) {
            return null;
        }
        String[] lsid = lsids.split(",");
        ArrayList<String[]> wmsurls = new ArrayList<String[]>();
        int count = 0;
        for (String s : lsid) {
            String[] urls = species_metadata_layers.get(s);
            if (urls != null) {
                count += urls.length;
                wmsurls.add(urls);
            }
        }
        String[] wms = null;
        if (count > 0) {
            wms = new String[count];
            int pos = 0;
            for (String[] s : wmsurls) {
                System.arraycopy(s, 0, wms, pos, s.length);
            }
        }
        return wms;
    }

    /**
     * returns array of spcode species requests
     */
    static public String[] getSpeciesDistributionSpcode(String lsids) {
        if (species_wms_layers == null) {
            return null;
        }
        String[] lsid = lsids.split(",");
        ArrayList<String[]> wmsurls = new ArrayList<String[]>();
        int count = 0;
        for (String s : lsid) {
            String[] urls = species_spcode_layers.get(s);
            if (urls != null) {
                count += urls.length;
                wmsurls.add(urls);
            }
        }
        String[] wms = null;
        if (count > 0) {
            wms = new String[count];
            int pos = 0;
            for (String[] s : wmsurls) {
                System.arraycopy(s, 0, wms, pos, s.length);
            }
        }
        return wms;
    }

    /**
     * returns array of WMS species requests
     */
    static public String[] getSpeciesChecklistWMS(String lsids) {
        if (checklistspecies_wms_layers == null || lsids == null) {
            return null;
        }
        String[] lsid = lsids.split(",");
        ArrayList<String[]> wmsurls = new ArrayList<String[]>();
        int count = 0;
        for (String s : lsid) {
            String[] urls = checklistspecies_wms_layers.get(s);
            if (urls != null) {
                count += urls.length;
                wmsurls.add(urls);
            }
        }
        String[] wms = null;
        if (count > 0) {
            wms = new String[count];
            int pos = 0;
            for (String[] s : wmsurls) {
                System.arraycopy(s, 0, wms, pos, s.length);
            }
        }
        return wms;
    }


    static HashMap<String, JSONObject> layerToFacet;
    static HashMap<String, JSONObject> facetToLayer;

    public static String getLayerFacetName(String layer) {
        String facetName = layer;
        JSONObject f = layerToFacet.get(layer.toLowerCase());
        if (f != null) {
            facetName = f.getString("id");
        }
        return facetName;
    }

    public static String getFacetLayerName(String facet) {
        JSONObject jo = facetToLayer.get(facet);
        if (jo != null) {
            return jo.getString("name");
        } else {
            return null;
        }
    }

    public static String getFacetShapeNameField(String facet) {
        JSONObject layer = facetToLayer.get(facet);
        if (layer != null) {
            JSONObject f = layerToFacet.get(layer.getString("name"));
            if (f != null && f.containsKey("sname")) {
                return f.getString("sname");
            }
        }

        return null;
    }

    public static String getFacetLayerDisplayName(String facet) {
        JSONObject layer = facetToLayer.get(facet);
        if (layer != null && layer.containsKey("displayname")) {
            return layer.getString("displayname");
        }
        return null;
    }

    public static String getLayerDisplayName(String name) {
        JSONObject layer = null;
        for (int i = 0; i < layerlistJSON.size(); i++) {
            layer = layerlistJSON.getJSONObject(i);
            if (layer.getString("name").equalsIgnoreCase(name) && layer.containsKey("displayname")) {
                return layer.getString("displayname");
            }
        }
        return null;
    }

    private static void readLayerInfo() {
        try {
            HashMap<String, JSONObject> ftl = new HashMap<String, JSONObject>();
            HashMap<String, JSONObject> ltf = new HashMap<String, JSONObject>();

            if (copy_layerlistJSON != null) {
                for (int i = 0; i < copy_layerlistJSON.size(); i++) {
                    JSONObject jo = copy_layerlistJSON.getJSONObject(i);

                    if (jo.containsKey("fields")) {
                        JSONArray ja = jo.getJSONArray("fields");
                        for (int j = 0; j < ja.size(); j++) {
                            JSONObject f = ja.getJSONObject(j);
                            if (f.containsKey("defaultlayer") && f.getBoolean("defaultlayer")) {
                                logger.debug("adding defaultlayer: " + jo.getString("name") + ", " + f.getString("id"));
                                String layer = jo.getString("name");
                                String facet = f.getString("id");

                                ltf.put(layer.toLowerCase(), f);
                                ftl.put(facet, jo);
                            }
                        }
                    }
                }
            }

            if (layerToFacet == null || ltf.size() > 0) {
                layerToFacet = ltf;
                facetToLayer = ftl;
            }
        } catch (Exception e) {
            logger.error("error reading layer info", e);
        }
    }

    static public ArrayList<QueryField> getDefaultUploadSamplingFields() {
        String[] fl = defaultFieldString.split(",");
        ArrayList<QueryField> fields = new ArrayList<QueryField>();
        for (int i = 0; i < fl.length; i++) {
            fields.add(new QueryField(fl[i], getFacetLayerDisplayName(fl[i]), QueryField.FieldType.AUTO));
        }

        return fields;
    }

    static Properties i18nProperites = null;

    static void initI18nProperies() {

        try {
            Properties p = new Properties();
            p.load(CommonData.class.getResourceAsStream("/messages.properties"));

            i18nProperites = p;
        } catch (Exception e) {
            logger.error("error loading /messages.properties file", e);
        }
    }

    static public String getI18nProperty(String key) {
        return i18nProperites.getProperty(key);
    }

    static public ArrayList<String> getI18nPropertiesList(String key) {
        ArrayList<String> list = new ArrayList<String>();
        String startsWith = key + ".";
        for (String k : i18nProperites.stringPropertyNames()) {
            if (k.startsWith(startsWith)) {
                list.add(k);
            }
        }
        return list;
    }

    static public void initSimpleShapeFileCache(String[] fields) {
        String[] layers = new String[fields.length];
        String[] columns = new String[fields.length];
        logger.debug("defaultFieldString: " + defaultFieldString);
        for (int i = 0; i < fields.length; i++) {
            layers[i] = getFacetLayerName(fields[i]);
            columns[i] = getFacetShapeNameField(fields[i]);
            logger.debug("field,layer,columns:" + fields[i] + "," + layers[i] + "," + columns[i]);
        }
    }

    static void setupAnalysisLayerSets() {
        ArrayList<LayerSelection> a = new ArrayList<LayerSelection>();
        try {
            if (CommonData.settings.getProperty(ANALYSIS_LAYER_SETS) != null) {
                String[] list = settings.getProperty(ANALYSIS_LAYER_SETS).split("\\|");

                for (String row : list) {
                    if (row.length() > 0) {
                        String[] cells = row.split("//");
                        if (cells.length == 2) {
                            a.add(new LayerSelection(cells[0].trim(), cells[1].trim()));
                        }
                    }
                }

            }
        } catch (Exception e) {
            logger.error("error setting up analysis layer sets", e);
        }
        analysisLayerSets = a;
    }

    public static String[] getSpeciesDistributionWMSFromSpcode(String spcode) {
        if (species_wms_layers_by_spcode == null) {
            return null;
        }

        return species_wms_layers_by_spcode.get(spcode);
    }

    public static String[] getSpeciesChecklistWMSFromSpcode(String spcode) {
        if (checklistspecies_wms_layers_by_spcode == null) {
            return null;
        }

        return checklistspecies_wms_layers_by_spcode.get(spcode);
    }

    public static int getSpeciesChecklistCountByWMS(String lookForWMS) {
        int count = 0;
        if (checklistspecies_wms_layers != null) {
            for (String[] wms : checklistspecies_wms_layers.values()) {
                for (int i = 0; i < wms.length; i++) {
                    if (wms[i].equals(lookForWMS)) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    static public void initDownloadReasons() {
        copy_download_reasons = null;
        logger.debug("CommonData::initDownloadReasons()");
        String url = "http://logger.ala.org.au/service/logger/reasons";
        try {

            logger.debug(url);
            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(url);

            int result = client.executeMethod(get);

            if (result == 200) {
                copy_download_reasons = JSONArray.fromObject(get.getResponseBodyAsString());
            }
        } catch (Exception e) {
            copy_download_reasons = null;
            logger.error("error getting reasons: " + url, e);
        }
    }

    private static String[][] parseFacetNameExceptions(String list) {
        String[] terms = list.split(",");
        String[][] output = new String[terms.length][];
        for (int i = 0; i < terms.length; i++) {
            output[i] = terms[i].split(":");
        }
        return output;
    }

    /*
     * get a layer JSONObject with the short name.
     */
    public static JSONObject getLayer(String name) {
        JSONObject layer = null;
        for (int i = 0; i < layerlistJSON.size(); i++) {
            layer = layerlistJSON.getJSONObject(i);
            if (layer.getString("name").equalsIgnoreCase(name)) {
                break;
            }
        }
        return layer;
    }

    static void initBiocacheLayerList() {
        String url = biocacheServer + "/index/fields";

        try {
            //environmental only
            logger.debug(url);
            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(url);

            int result = client.executeMethod(get);

            logger.debug("initBiocacheLayerList: " + url + " > " + result);
            if (result == 200) {
                Set<String> set = new HashSet<String>();
                JSONArray ja = JSONArray.fromObject(get.getResponseBodyAsString());
                logger.debug("size: " + ja.size());

                // Populate the biocache layer list with the names of all
                // indexed fields. The additional non-layer field names will
                // not cause a problem here.
                for (int i = 0; i < ja.size(); i++) {
                    String layer = ja.getJSONObject(i).getString("name");
                    set.add(layer);
                }
                if (ja.size() > 0) {
                    // remove the "translated" names for particular layers. E.g.
                    // "state" for cl22, "places" for cl959, "ibra" for  "cl20", "imcra"
                    for (int i = 0; i < facetNameExceptions.length; i++) {
                        if (set.contains(facetNameExceptions[i][0])) {
                            set.remove(facetNameExceptions[i][1]);
                        }
                    }
                    biocacheLayerList = set;
                }
            }
        } catch (Exception e) {
            logger.error("error getting: " + url, e);
        }
    }

    static ConcurrentHashMap<String, String> proxyAuthTickets = new ConcurrentHashMap<String, String>();

    public static void putProxyAuthTicket(String pgt_iou, String pgt) {
        proxyAuthTickets.put(pgt_iou, pgt);
    }

    public static String takeProxyAuthTicket(String pgt_iou) {
        String pgt = proxyAuthTickets.get(pgt_iou);

        proxyAuthTickets.remove(pgt_iou);

        return pgt;
    }

    public static boolean hasProxyAuthTicket(String pgt_iou) {
        return proxyAuthTickets.get(pgt_iou) != null;
    }
}
