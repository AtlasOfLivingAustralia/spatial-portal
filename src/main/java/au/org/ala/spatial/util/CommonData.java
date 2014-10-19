/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.ala.spatial.util;

import au.org.ala.spatial.StringConstants;
import au.org.emii.portal.lang.LanguagePack;
import au.org.emii.portal.lang.LanguagePackImpl;
import au.org.emii.portal.util.PortalProperties;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.ala.layers.legend.QueryField;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;

import java.net.URL;
import java.util.*;

/**
 * common data store
 * <p/>
 * 1. alaspatial environmental and contextual layer names (use in LayersUtil) 2.
 * alaspatial layers associations, environmental and contextual layer names (use
 * in EnvironmentalList) 3. layer list as string and JSON (layer tree, (2))
 *
 * @author Adam
 */
public final class CommonData {

    //common data
    public static final String WORLD_WKT = "POLYGON((-179.999 -89.999,-179.999 89.999,179.999 84.999,179.999 -89.999,-179.999 -89.999))";
    //NC: 20130319 changed to using the correct direction
    public static final String AUSTRALIA_WKT = "POLYGON((112.0 -44.0,154.0 -44.0,154.0 -9.0,112.0 -9.0,112.0 -44.0))";
    public static final String PHYLOLIST_URL = "phylolist_url";
    //common parameters
    private static final String SAT_URL = "sat_url";
    private static final String GEOSERVER_URL = "geoserver_url";
    private static final String LAYERS_URL = "layers_url";
    private static final String WEBPORTAL_URL = "webportal_url";
    private static final String BIE_URL = "bie_url";
    private static final String BIOCACHE_SERVICE_URL = "biocache_service_url";
    private static final String BIOCACHE_WEBAPP_URL = "biocache_webapp_url";
    private static final String SPECIES_LIST_URL = "species_list_url";
    private static final String COLLECTORY_URL = "collectory_url";
    private static final String DEFAULT_UPLOAD_SAMPLING = "default_upload_sampling";
    private static final String MAX_Q_LENGTH = "max_q_length";
    private static final String BIOCACHE_QC = "biocache_qc";
    private static final String ANALYSIS_LAYER_SETS = "analysis_layer_sets";
    private static final String MAX_AREA_FOR_ENDEMIC = "max_area_endemic";
    private static final String EXTRA_DOWNLOAD_FIELDS = "occurrence_extra_download";
    private static final String DISPLAY_POINTS_OF_INTEREST = "display_points_of_interest";
    private static final String CUSTOM_FACETS = "custom_facets";
    private static final String AREA_REPORT_FACETS = "area_report_facets";
    //NC 20131017 - the default facets supplied by the biocache WS that are ignored.
    private static final String IGNORED_FACETS = "default_facets_ignored";
    private static final String I18N_URL = "i18nURL";
    private static final String I18N_IGNORE_THESE_PREFIXES = "i18nIgnoreThesePrefixes";
    private static final Logger LOGGER = Logger.getLogger(CommonData.class);
    protected static String collectoryServer;
    protected static String[] customFacets;
    protected static List<String> ignoredFacets;
    //Common
    private static String satServer;
    private static String geoServer;
    private static String layersServer;
    private static String webportalServer;
    private static String bieServer;
    private static String biocacheServer;
    private static String biocacheWebServer;
    //(4) species with distribution layres
    private static String speciesListServer;
    private static int maxQLength;
    private static Properties settings;
    //lsid counts, for species autocomplete
    private static LsidCounts lsidCounts;
    private static String biocacheQc;
    private static List<LayerSelection> analysisLayerSets;
    private static String[][] facetNameExceptions;
    private static Set<String> biocacheLayerList;
    private static int maxEndemicArea;
    private static String extraDownloadFields = "coordinateUncertaintyInMeters";
    private static boolean displayPointsOfInterest;
    private static String[] areaReportFacets;
    private static String i18nURL;
    private static List<String> i18nIgnoredPrefixes;
    //(2) for EnvironmentalList
    private static JSONObject distances;
    private static Map<String, Map<String, Double>> distancesMap;
    private static JSONObject copyDistances;
    private static Map<String, Map<String, Double>> copyDistancesMap;
    //(3) for layer list json
    private static JSONArray layerlistJSON = null;
    private static JSONArray copyLayerlistJSON = null;
    private static String defaultFieldString = null;
    /**
     * key = LSID value = list of WMS names
     */
    private static Map<String, String[]> speciesWmsLayers = null;
    private static Map<String, String[]> copySpeciesWmsLayers = null;
    private static Map<String, String[]> speciesMetadataLayers = null;
    private static Map<String, String[]> copySpeciesMetadataLayers = null;
    private static Map<String, String[]> speciesSpcodeLayers = null;
    private static Map<String, String[]> copySpeciesSpcodeLayers = null;
    private static Map<String, String[]> speciesWmsLayersBySpcode = null;
    private static Map<String, String[]> copySpeciesWmsLayersBySpcode = null;
    private static Map<String, String[]> checklistspeciesWmsLayers = null;
    private static Map<String, String[]> copyChecklistspeciesWmsLayers = null;
    private static Map<String, String[]> checklistspeciesMetadataLayers = null;
    private static Map<String, String[]> copyChecklistspeciesMetadataLayers = null;
    private static Map<String, String[]> checklistspeciesSpcodeLayers = null;
    private static Map<String, String[]> copyChecklistspeciesSpcodeLayers = null;
    private static Map<String, String[]> checklistspeciesWmsLayersBySpcode = null;
    private static Map<String, String[]> copyChecklistspeciesWmsLayersBySpcode = null;
    // download reasons
    private static JSONArray downloadReasons;
    private static JSONArray copyDownloadReasons;
    private static Map<String, JSONObject> layerToFacet;
    private static Map<String, JSONObject> facetToLayer;
    private static Properties i18nProperites = null;
    private static LanguagePack languagePack = null;

    private CommonData() {
        //to hide public constructor
    }

    /*
     * initialize common data from geoserver and satserver
     */
    public static void init(Properties settings) {
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
        biocacheQc = ((PortalProperties) settings).getProperty(BIOCACHE_QC, false);
        if (biocacheQc == null) {
            biocacheQc = "";
        }
        facetNameExceptions = parseFacetNameExceptions(settings.getProperty("facet_name_exceptions"));
        customFacets = settings.containsKey(CUSTOM_FACETS) ? settings.getProperty(CUSTOM_FACETS).split(",") : new String[]{};
        ignoredFacets = Arrays.asList(settings.containsKey(IGNORED_FACETS) ? settings.getProperty(IGNORED_FACETS).split(",") : new String[]{});
        areaReportFacets = settings.containsKey(AREA_REPORT_FACETS) ? settings.getProperty(AREA_REPORT_FACETS).split(",") : new String[]{};
        if (settings.containsKey(EXTRA_DOWNLOAD_FIELDS)) {
            extraDownloadFields = settings.getProperty(EXTRA_DOWNLOAD_FIELDS);
        }

        displayPointsOfInterest = settings.containsKey(DISPLAY_POINTS_OF_INTEREST) && Boolean.parseBoolean(settings.getProperty(DISPLAY_POINTS_OF_INTEREST));

        i18nURL = settings.getProperty(I18N_URL);
        String tmp = settings.getProperty(I18N_IGNORE_THESE_PREFIXES);
        if (tmp != null) {
            i18nIgnoredPrefixes = java.util.Arrays.asList(tmp.split(" "));
        } else {
            i18nIgnoredPrefixes = new ArrayList<String>();
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
        if (copyDownloadReasons != null) {
            downloadReasons = copyDownloadReasons;
        }

        //keep a list of biocache field names to know what is available for queries
        initBiocacheLayerList();

        //init language pack (but not everywhere)
        initLanguagePack();

        //(2) for EnvironmentalList
        if (copyDistances != null) {
            distances = copyDistances;
        }

        if (copyDistancesMap != null) {
            distancesMap = copyDistancesMap;
        }

        //(3) for layer list json
        if (copyLayerlistJSON != null) {
            layerlistJSON = copyLayerlistJSON;
        }

        //(4) for species wms distributions & checklists
        if (copySpeciesWmsLayers != null) {
            speciesWmsLayers = copySpeciesWmsLayers;
        }
        if (copySpeciesMetadataLayers != null) {
            speciesMetadataLayers = copySpeciesMetadataLayers;
        }
        if (copySpeciesSpcodeLayers != null) {
            speciesSpcodeLayers = copySpeciesSpcodeLayers;
        }
        if (copySpeciesWmsLayersBySpcode != null) {
            speciesWmsLayersBySpcode = copySpeciesWmsLayersBySpcode;
        }
        if (copyChecklistspeciesWmsLayers != null) {
            checklistspeciesWmsLayers = copyChecklistspeciesWmsLayers;
        }
        if (copyChecklistspeciesMetadataLayers != null) {
            checklistspeciesMetadataLayers = copyChecklistspeciesMetadataLayers;
        }
        if (copyChecklistspeciesSpcodeLayers != null) {
            checklistspeciesSpcodeLayers = copyChecklistspeciesSpcodeLayers;
        }
        if (copyChecklistspeciesWmsLayersBySpcode != null) {
            checklistspeciesWmsLayersBySpcode = copyChecklistspeciesWmsLayersBySpcode;
        }


    }

    public static JSONArray getDownloadReasons() {
        return downloadReasons;
    }

    public static JSONObject getDistances() {
        return distances;
    }

    public static void initLayerDistances() {
        copyDistances = null;
        copyDistancesMap = null;
        LOGGER.debug("CommonData::initLayerDistances()");
        String url = satServer + "/ws/layerdistances";
        try {
            LOGGER.debug(url);
            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(url);

            int result = client.executeMethod(get);

            if (result == 200) {
                copyDistances = JSONObject.fromObject(get.getResponseBodyAsString());


                //make map
                copyDistancesMap = new HashMap<String, Map<String, Double>>();
                for (Object okey : copyDistances.keySet()) {
                    Double d = copyDistances.getDouble((String) okey);
                    String[] parts = ((String) okey).split(" ");

                    Map<String, Double> part = copyDistancesMap.get(parts[0]);
                    if (part == null) {
                        part = new HashMap<String, Double>();
                        copyDistancesMap.put(parts[0], part);
                    }
                    part.put(parts[1], d);


                    part = copyDistancesMap.get(parts[1]);
                    if (part == null) {
                        part = new HashMap<String, Double>();
                        copyDistancesMap.put(parts[1], part);
                    }
                    part.put(parts[0], d);
                }
            }
        } catch (Exception e) {
            copyDistances = null;
            copyDistancesMap = null;
            LOGGER.error("error getting layer distances", e);
        }
    }

    public static Map<String, Map<String, Double>> getDistancesMap() {
        return distancesMap;
    }

    static void initLayerList() {
        copyLayerlistJSON = null;
        String layersListURL = layersServer + "/layers";
        try {

            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(layersListURL);
            get.addRequestHeader(StringConstants.ACCEPT, StringConstants.JSON_JAVASCRIPT_ALL);

            int result = client.executeMethod(get);

            if (result == 200) {
                copyLayerlistJSON = JSONArray.fromObject(get.getResponseBodyAsString());
            }

            addFieldsToLayers(copyLayerlistJSON);
        } catch (Exception e) {
            LOGGER.error("error getting layers list: " + layersListURL, e);
            copyLayerlistJSON = null;
        }
    }

    static void addFieldsToLayers(JSONArray joLayers) throws Exception {
        //get field id with classes
        String fieldsURL = layersServer + "/fields";
        HttpClient client = new HttpClient();
        GetMethod get = new GetMethod(fieldsURL);
        int result = client.executeMethod(get);
        if (result != 200) {
            LOGGER.error("cannot retrive field list: " + fieldsURL);
            return;
        }
        String fields = get.getResponseBodyAsString();
        JSONArray ja = JSONArray.fromObject(fields);

        //attach to a new JSONArray in joLayers named Constants.FIELDS
        for (int j = 0; j < joLayers.size(); j++) {
            JSONObject layer = joLayers.getJSONObject(j);
            if (layer.containsKey(StringConstants.ID)) {
                for (int i = 0; i < ja.size(); i++) {
                    JSONObject jo = ja.getJSONObject(i);
                    if (
                            jo.containsKey("defaultlayer") && StringConstants.TRUE.equals(jo.getString("defaultlayer"))

                                    && jo.containsKey("spid") && jo.getString("spid").equals(layer.getString(StringConstants.ID))) {
                        //add to layer
                        if (!layer.containsKey(StringConstants.FIELDS)) {
                            layer.put(StringConstants.FIELDS, new JSONArray());
                        }
                        layer.getJSONArray(StringConstants.FIELDS).add(jo);
                    }
                }
            }
        }
    }

    public static JSONArray getLayerListJSONArray() {
        return layerlistJSON;
    }

    private static void initSpeciesWMSLayers() {
        try {
            String layersListURL = layersServer + "/distributions";
            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(layersListURL);
            get.addRequestHeader(StringConstants.ACCEPT, StringConstants.JSON_JAVASCRIPT_ALL);
            get.addRequestHeader("Accept-Encoding", "gzip");

            copySpeciesWmsLayers = new HashMap<String, String[]>();
            copySpeciesMetadataLayers = new HashMap<String, String[]>();
            copySpeciesSpcodeLayers = new HashMap<String, String[]>();
            copySpeciesWmsLayersBySpcode = new HashMap<String, String[]>();

            client.executeMethod(get);
            String slist = get.getResponseBodyAsString();

            JSONArray ja = JSONArray.fromObject(slist);

            LOGGER.debug(ja.size() + " species wms distributions");

            for (int i = 0; i < ja.size(); i++) {
                JSONObject jo = ja.getJSONObject(i);
                if (jo.containsKey(StringConstants.LSID) && jo.containsKey(StringConstants.WMSURL)) {
                    //manage lsids with multiple wmsurls
                    String lsid = jo.getString(StringConstants.LSID);

                    //wms
                    String[] urls = copySpeciesWmsLayers.get(lsid);
                    if (urls != null) {
                        String[] newUrls = new String[urls.length + 1];
                        System.arraycopy(urls, 0, newUrls, 0, urls.length);
                        newUrls[newUrls.length - 1] = jo.getString(StringConstants.WMSURL);
                        urls = newUrls;
                    } else {
                        urls = new String[]{jo.getString(StringConstants.WMSURL)};
                    }
                    copySpeciesWmsLayers.put(lsid, urls);

                    //metadata
                    String m = "";
                    if (jo.containsKey(StringConstants.METADATA_U)) {
                        m = jo.getString(StringConstants.METADATA_U);
                    }
                    String[] md = copySpeciesMetadataLayers.get(lsid);
                    if (md != null) {
                        String[] newMd = new String[md.length + 1];
                        System.arraycopy(md, 0, newMd, 0, md.length);
                        newMd[newMd.length - 1] = m;
                        md = newMd;
                    } else {
                        md = new String[]{m};
                    }
                    copySpeciesMetadataLayers.put(lsid, md);

                    //spcode
                    m = "";
                    if (jo.containsKey(StringConstants.SPCODE)) {
                        m = jo.getString(StringConstants.SPCODE);
                    }
                    md = copySpeciesSpcodeLayers.get(lsid);
                    if (md != null) {
                        String[] newMd = new String[md.length + 1];
                        System.arraycopy(md, 0, newMd, 0, md.length);
                        newMd[newMd.length - 1] = m;
                        md = newMd;
                    } else {
                        md = new String[]{m};
                    }
                    copySpeciesSpcodeLayers.put(lsid, md);

                    //others
                    String spcode = null;
                    if (jo.containsKey(StringConstants.SPCODE)) {
                        spcode = jo.getString(StringConstants.SPCODE);
                    }
                    lsid = null;
                    if (jo.containsKey(StringConstants.LSID)) {
                        lsid = jo.getString(StringConstants.LSID);
                    }
                    String pid = null;
                    if (jo.containsKey(StringConstants.PID)) {
                        pid = jo.getString(StringConstants.PID);
                    }
                    String type = null;
                    if (jo.containsKey(StringConstants.TYPE)) {
                        type = jo.getString(StringConstants.TYPE);
                    }
                    copySpeciesWmsLayersBySpcode.put(spcode, new String[]{jo.getString(StringConstants.SCIENTIFIC), jo.getString(StringConstants.WMSURL), m, lsid, pid, type});
                }
            }

            //repeat to append checklists
            layersListURL = layersServer + "/checklists";
            client = new HttpClient();
            get = new GetMethod(layersListURL);
            get.addRequestHeader(StringConstants.ACCEPT, StringConstants.JSON_JAVASCRIPT_ALL);

            client.executeMethod(get);

            get.addRequestHeader("Accept-Encoding", "gzip");

            int result = client.executeMethod(get);

            copyChecklistspeciesWmsLayers = new HashMap<String, String[]>();
            copyChecklistspeciesMetadataLayers = new HashMap<String, String[]>();
            copyChecklistspeciesSpcodeLayers = new HashMap<String, String[]>();
            copyChecklistspeciesWmsLayersBySpcode = new HashMap<String, String[]>();

            if (result == 200) {
                slist = get.getResponseBodyAsString();
                ja = JSONArray.fromObject(slist);

                LOGGER.debug(ja.size() + " species wms checklists");

                for (int i = 0; i < ja.size(); i++) {
                    JSONObject jo = ja.getJSONObject(i);
                    if (jo.containsKey(StringConstants.LSID) && jo.containsKey(StringConstants.WMSURL)) {
                        //manage lsids with multiple wmsurls
                        String lsid = jo.getString(StringConstants.LSID);

                        //wms
                        String[] urls = copyChecklistspeciesWmsLayers.get(lsid);
                        if (urls != null) {
                            String[] newUrls = new String[urls.length + 1];
                            System.arraycopy(urls, 0, newUrls, 0, urls.length);
                            newUrls[newUrls.length - 1] = jo.getString(StringConstants.WMSURL);
                            urls = newUrls;
                        } else {
                            urls = new String[]{jo.getString(StringConstants.WMSURL)};
                        }
                        copyChecklistspeciesWmsLayers.put(lsid, urls);

                        //metadata
                        String m = "";
                        if (jo.containsKey(StringConstants.METADATA_U)) {
                            m = jo.getString(StringConstants.METADATA_U);
                        }
                        String[] md = copyChecklistspeciesMetadataLayers.get(lsid);
                        if (md != null) {
                            String[] newMd = new String[md.length + 1];
                            System.arraycopy(md, 0, newMd, 0, md.length);
                            newMd[newMd.length - 1] = m;
                            md = newMd;
                        } else {
                            md = new String[]{m};
                        }
                        copyChecklistspeciesMetadataLayers.put(lsid, md);

                        //spcode
                        m = "";
                        if (jo.containsKey(StringConstants.SPCODE)) {
                            m = jo.getString(StringConstants.SPCODE);
                        }
                        md = copyChecklistspeciesSpcodeLayers.get(lsid);
                        if (md != null) {
                            String[] newMd = new String[md.length + 1];
                            System.arraycopy(md, 0, newMd, 0, md.length);
                            newMd[newMd.length - 1] = m;
                            md = newMd;
                        } else {
                            md = new String[]{m};
                        }
                        copyChecklistspeciesSpcodeLayers.put(lsid, md);

                        //by spcode
                        String spcode = jo.getString(StringConstants.SPCODE);
                        copyChecklistspeciesWmsLayersBySpcode.put(spcode, new String[]{jo.getString(StringConstants.SCIENTIFIC), jo.getString(StringConstants.WMSURL), m});
                    }
                }
            }
        } catch (Exception e) {
            copySpeciesWmsLayers = null;
            copySpeciesMetadataLayers = null;
            copySpeciesSpcodeLayers = null;
            copySpeciesWmsLayersBySpcode = null;

            copyChecklistspeciesWmsLayers = null;
            copyChecklistspeciesMetadataLayers = null;
            copyChecklistspeciesSpcodeLayers = null;
            copyChecklistspeciesWmsLayersBySpcode = null;


            LOGGER.error("error getting species and distributions checklists", e);
        }
    }

    /**
     * returns array of WMS species requests
     */
    public static String[] getSpeciesDistributionWMS(String lsids) {
        if (speciesWmsLayers == null || lsids == null) {
            return new String[0];
        }
        String[] lsid = lsids.split(",");
        List<String[]> wmsurls = new ArrayList<String[]>();
        int count = 0;
        for (String s : lsid) {
            String[] urls = speciesWmsLayers.get(s);
            if (urls != null) {
                count += urls.length;
                wmsurls.add(urls);
            }
        }
        String[] wms = new String[0];
        if (count > 0) {
            wms = new String[count];
            int pos = 0;
            for (String[] s : wmsurls) {
                System.arraycopy(s, 0, wms, pos, s.length);
                pos += s.length;
            }
        }
        return wms;
    }

    /**
     * returns array of metadata_u species requests
     */
    public static String[] getSpeciesDistributionMetadata(String lsids) {
        if (speciesWmsLayers == null) {
            return new String[0];
        }
        String[] lsid = lsids.split(",");
        List<String[]> wmsurls = new ArrayList<String[]>();
        int count = 0;
        for (String s : lsid) {
            String[] urls = speciesMetadataLayers.get(s);
            if (urls != null) {
                count += urls.length;
                wmsurls.add(urls);
            }
        }
        String[] wms;
        if (count > 0) {
            wms = new String[count];
            int pos = 0;
            for (String[] s : wmsurls) {
                System.arraycopy(s, 0, wms, pos, s.length);
                pos += s.length;
            }
        } else {
            wms = new String[0];
        }
        return wms;
    }

    /**
     * returns array of spcode species requests
     */
    public static String[] getSpeciesDistributionSpcode(String lsids) {
        if (speciesWmsLayers == null) {
            return new String[0];
        }
        String[] lsid = lsids.split(",");
        List<String[]> wmsurls = new ArrayList<String[]>();
        int count = 0;
        for (String s : lsid) {
            String[] urls = speciesSpcodeLayers.get(s);
            if (urls != null) {
                count += urls.length;
                wmsurls.add(urls);
            }
        }
        String[] wms;
        if (count > 0) {
            wms = new String[count];
            int pos = 0;
            for (String[] s : wmsurls) {
                System.arraycopy(s, 0, wms, pos, s.length);
                pos += s.length;
            }
        } else {
            wms = new String[0];
        }
        return wms;
    }

    /**
     * returns array of WMS species requests
     */
    public static String[] getSpeciesChecklistWMS(String lsids) {
        if (checklistspeciesWmsLayers == null || lsids == null) {
            return new String[0];
        }
        String[] lsid = lsids.split(",");
        List<String[]> wmsurls = new ArrayList<String[]>();
        int count = 0;
        for (String s : lsid) {
            String[] urls = checklistspeciesWmsLayers.get(s);
            if (urls != null) {
                count += urls.length;
                wmsurls.add(urls);
            }
        }
        String[] wms;
        if (count > 0) {
            wms = new String[count];
            int pos = 0;
            for (String[] s : wmsurls) {
                System.arraycopy(s, 0, wms, pos, s.length);
            }
        } else {
            wms = new String[0];
        }
        return wms;
    }

    public static String getLayerFacetName(String layer) {
        String facetName = layer;
        JSONObject f = layerToFacet.get(layer.toLowerCase());
        if (f != null) {
            facetName = f.getString(StringConstants.ID);
        }
        return facetName;
    }

    public static String getFacetLayerName(String facet) {
        JSONObject jo = facetToLayer.get(facet);
        if (jo != null) {
            return jo.getString(StringConstants.NAME);
        } else {
            return null;
        }
    }

    public static String getFacetShapeNameField(String facet) {
        JSONObject layer = facetToLayer.get(facet);
        if (layer != null) {
            JSONObject f = layerToFacet.get(layer.getString(StringConstants.NAME));
            if (f != null && f.containsKey("sname")) {
                return f.getString("sname");
            }
        }

        return null;
    }

    public static String getFacetLayerDisplayName(String facet) {
        JSONObject layer = facetToLayer.get(facet);
        if (layer != null && layer.containsKey(StringConstants.DISPLAYNAME)) {
            return layer.getString(StringConstants.DISPLAYNAME);
        }
        return null;
    }

    public static String getLayerDisplayName(String name) {
        JSONObject layer;
        for (int i = 0; i < layerlistJSON.size(); i++) {
            layer = layerlistJSON.getJSONObject(i);
            if (layer.getString(StringConstants.NAME).equalsIgnoreCase(name) && layer.containsKey(StringConstants.DISPLAYNAME)) {
                return layer.getString(StringConstants.DISPLAYNAME);
            }
        }
        return null;
    }

    private static void readLayerInfo() {
        try {
            Map<String, JSONObject> ftl = new HashMap<String, JSONObject>();
            Map<String, JSONObject> ltf = new HashMap<String, JSONObject>();

            if (copyLayerlistJSON != null) {
                for (int i = 0; i < copyLayerlistJSON.size(); i++) {
                    JSONObject jo = copyLayerlistJSON.getJSONObject(i);

                    if (jo.containsKey(StringConstants.FIELDS)) {
                        JSONArray ja = jo.getJSONArray(StringConstants.FIELDS);
                        for (int j = 0; j < ja.size(); j++) {
                            JSONObject f = ja.getJSONObject(j);
                            if (f.containsKey("defaultlayer") && f.getBoolean("defaultlayer")) {
                                LOGGER.debug("adding defaultlayer: " + jo.getString(StringConstants.NAME) + ", " + f.getString(StringConstants.ID));
                                String layer = jo.getString(StringConstants.NAME);
                                String facet = f.getString(StringConstants.ID);

                                ltf.put(layer.toLowerCase(), f);
                                ftl.put(facet, jo);
                            }
                        }
                    }
                }
            }

            if (layerToFacet == null || !ltf.isEmpty()) {
                layerToFacet = ltf;
                facetToLayer = ftl;
            }
        } catch (Exception e) {
            LOGGER.error("error reading layer info", e);
        }
    }

    public static List<QueryField> getDefaultUploadSamplingFields() {
        String[] fl = defaultFieldString.split(",");
        List<QueryField> fields = new ArrayList<QueryField>();
        for (int i = 0; i < fl.length; i++) {
            fields.add(new QueryField(fl[i], getFacetLayerDisplayName(fl[i]), QueryField.FieldType.AUTO));
        }

        return fields;
    }

    static void initI18nProperies() {

        try {
            Properties p = new Properties();
            p.load(new URL(i18nURL).openStream());

            i18nProperites = p;
        } catch (Exception e) {
            LOGGER.error("error loading properties file URL: " + i18nURL, e);
        }
    }

    public static String getI18nProperty(String key) {
        return i18nProperites.getProperty(key);
    }

    public static List<String> getI18nPropertiesList(String key) {
        List<String> list = new ArrayList<String>();

        //don't look up anything if it is an ignored key
        if (!i18nIgnoredPrefixes.contains(key)) {
            String startsWith = key + ".";
            for (String k : i18nProperites.stringPropertyNames()) {
                if (k.startsWith(startsWith)) {
                    list.add(k);
                }
            }
        }
        return list;
    }

    public static void initSimpleShapeFileCache(String[] fields) {
        String[] layers = new String[fields.length];
        String[] columns = new String[fields.length];
        LOGGER.debug("defaultFieldString: " + defaultFieldString);
        for (int i = 0; i < fields.length; i++) {
            layers[i] = getFacetLayerName(fields[i]);
            columns[i] = getFacetShapeNameField(fields[i]);
            LOGGER.debug("field,layer,columns:" + fields[i] + "," + layers[i] + "," + columns[i]);
        }
    }

    static void setupAnalysisLayerSets() {
        List<LayerSelection> a = new ArrayList<LayerSelection>();
        try {
            if (CommonData.getSettings().getProperty(ANALYSIS_LAYER_SETS) != null) {
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
            LOGGER.error("error setting up analysis layer sets", e);
        }
        analysisLayerSets = a;
    }

    public static String[] getSpeciesDistributionWMSFromSpcode(String spcode) {
        if (speciesWmsLayersBySpcode == null) {
            return new String[0];
        }

        String[] ret = speciesWmsLayersBySpcode.get(spcode);

        return ret == null ? new String[0] : ret;
    }

    public static String[] getSpeciesChecklistWMSFromSpcode(String spcode) {
        if (checklistspeciesWmsLayersBySpcode == null) {
            return new String[]{null, null};
        }

        String[] ret = checklistspeciesWmsLayersBySpcode.get(spcode);

        if (ret == null) {
            LOGGER.error("failed to find species checklist for spcode=" + spcode);
            return new String[]{null, null};
        }

        return ret;
    }

    public static int getSpeciesChecklistCountByWMS(String lookForWMS) {
        int count = 0;
        if (checklistspeciesWmsLayers != null) {
            for (String[] wms : checklistspeciesWmsLayers.values()) {
                for (int i = 0; i < wms.length; i++) {
                    if (wms[i].equals(lookForWMS)) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    public static void initDownloadReasons() {
        copyDownloadReasons = null;
        LOGGER.debug("CommonData::initDownloadReasons()");
        String url = "http://logger.ala.org.au/service/logger/reasons";
        try {

            LOGGER.debug(url);
            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(url);

            int result = client.executeMethod(get);

            if (result == 200) {
                copyDownloadReasons = JSONArray.fromObject(get.getResponseBodyAsString());
            }
        } catch (Exception e) {
            copyDownloadReasons = null;
            LOGGER.error("error getting reasons: " + url, e);
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
            if (layer.getString(StringConstants.NAME).equalsIgnoreCase(name)) {
                break;
            }
        }
        return layer;
    }

    static void initBiocacheLayerList() {
        String url = biocacheServer + "/index/fields";

        try {
            //environmental only
            LOGGER.debug(url);
            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(url);

            int result = client.executeMethod(get);

            LOGGER.debug("initBiocacheLayerList: " + url + " > " + result);
            if (result == 200) {
                Set<String> set = new HashSet<String>();
                JSONArray ja = JSONArray.fromObject(get.getResponseBodyAsString());
                LOGGER.debug("size: " + ja.size());

                // Populate the biocache layer list with the names of all
                // indexed fields. The additional non-layer field names will
                // not cause a problem here.
                for (int i = 0; i < ja.size(); i++) {
                    String layer = ja.getJSONObject(i).getString(StringConstants.NAME);
                    set.add(layer);
                }
                if (!ja.isEmpty()) {
                    // remove the "translated" names for particular layers. E.g.
                    // Constants.STATE for cl22, "places" for cl959, "ibra" for  "cl20", "imcra"
                    for (int i = 0; i < facetNameExceptions.length; i++) {
                        if (set.contains(facetNameExceptions[i][0])) {
                            set.remove(facetNameExceptions[i][1]);
                        }
                    }
                    biocacheLayerList = set;
                }
            }
        } catch (Exception e) {
            LOGGER.error("error getting: " + url, e);
        }

    }

    public static String lang(String key) {
        return languagePack.getLang(key);
    }

    static void initLanguagePack() {
        languagePack = new LanguagePackImpl();
    }

    public static String getSatServer() {
        return satServer;
    }

    public static String getGeoServer() {
        return geoServer;
    }

    public static String getLayersServer() {
        return layersServer;
    }

    public static String getWebportalServer() {
        return webportalServer;
    }

    public static Properties getSettings() {
        return settings;
    }

    public static String getSpeciesListServer() {
        return speciesListServer;
    }

    public static String getBiocacheWebServer() {
        return biocacheWebServer;
    }

    public static String[] getAreaReportFacets() {
        return areaReportFacets;
    }

    public static boolean getDisplayPointsOfInterest() {
        return displayPointsOfInterest;
    }

    public static List<LayerSelection> getAnalysisLayerSets() {
        return analysisLayerSets;
    }

    public static String getBieServer() {
        return bieServer;
    }

    public static LsidCounts getLsidCounts() {
        return lsidCounts;
    }

    public static int getMaxEndemicArea() {
        return maxEndemicArea;
    }

    public static String getBiocacheServer() {
        return biocacheServer;
    }

    public static Set<String> getBiocacheLayerList() {
        return biocacheLayerList;
    }

    public static String getBiocacheQc() {
        return biocacheQc;
    }

    public static String[][] getFacetNameExceptions() {
        return facetNameExceptions;
    }

    public static int getMaxQLength() {
        return maxQLength;
    }

    public static String getExtraDownloadFields() {
        return extraDownloadFields;
    }
}
