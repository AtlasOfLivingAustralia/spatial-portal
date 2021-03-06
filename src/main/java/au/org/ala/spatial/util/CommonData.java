/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.ala.spatial.util;

import au.com.bytecode.opencsv.CSVReader;
import au.org.ala.legend.QueryField;
import au.org.ala.spatial.StringConstants;
import au.org.ala.spatial.dto.SpeciesListItemDTO;
import au.org.emii.portal.lang.LanguagePack;
import au.org.emii.portal.lang.LanguagePackImpl;
import au.org.emii.portal.util.PortalProperties;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.io.WKTReader;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;

import java.io.*;
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
    public static final String AUSTRALIA_WKT = "default.wkt";
    public static final String AUSTRALIA_NAME = "default.name";
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
    private static LsidCountsDynamic lsidCounts;
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
    private static Properties i18nProperites = null;
    private static LanguagePack languagePack = null;
    private static Map speciesListCounts;
    private static Long speciesListCountsUpdated = 0L;
    private static Map speciesListCountsKosher;
    private static Long speciesListCountsUpdatedKosher = 0L;
    private static Map<String, Map<String, List<String>>> speciesListAdditionalColumns = new HashMap<String, Map<String, List<String>>>();

    private static final String FACET_SUFFIX = "/search/grouped/facets";
    private static List<QueryField> facetQueryFieldList;
    
    private CommonData() {
        //to hide public constructor
    }

    /*
     * initialize common data from geoserver and satserver
     */
    public static void init(Properties settings) {
        //first time, load from disk cache
        boolean readFromCache = false;
        if (CommonData.settings == null) {
            readFromCache = loadFromCache();
        }
        
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

        //init language pack (but not everywhere)
        initLanguagePack();

        setupAnalysisLayerSets();

        //journalmap
        initJournalmap();

        if (!readFromCache) refreshCachedData();
    }

    private static void refreshCachedData() {

        initLayerDistances();

        //(3) for layer list json
        initLayerList();

        //(4) for species wms layers
        initSpeciesWMSLayers();

        //(5) for layer to facet name mapping
        //readLayerInfo();

        //(7) lsid counts
        //LsidCounts lc = new LsidCounts();
        //if (lc.getSize() > 0) {
        //    lsidCounts = lc;
        //}
        lsidCounts.clear();

        //species list additional columns
        speciesListAdditionalColumns = initSpeciesListAdditionalColumns();

        // load the download reasons
        initDownloadReasons();
        if (copyDownloadReasons != null) {
            downloadReasons = copyDownloadReasons;
        }

        //keep a list of biocache field names to know what is available for queries
        initBiocacheLayerList();

        //need this data if using SP's endemic method
        if (CommonData.getSettings().containsKey("endemic.sp.method")
                && CommonData.getSettings().getProperty("endemic.sp.method").equals("true")) {
            getSpeciesListCountsKosher(true);
            getSpeciesListCounts(true);
        }


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

        //(6) for common facet name and value conversions, layers need to be initialised before here
        initI18nProperies();

        writeToCache();

    }

    private static void writeToCache() {
        try {
            String path = "/data/webportal/cache/";

            new File(path).mkdirs();

            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(path + "commondata"));

            oos.writeObject(speciesListAdditionalColumns);
            oos.writeObject(downloadReasons);
            oos.writeObject(biocacheLayerList);
            oos.writeObject(speciesListCountsKosher);
            oos.writeObject(speciesListCounts);
            oos.writeObject(speciesWmsLayers);
            oos.writeObject(distancesMap);
            oos.writeObject(distances);
            oos.writeObject(layerlistJSON);
            oos.writeObject(speciesWmsLayers);
            oos.writeObject(speciesMetadataLayers);
            oos.writeObject(speciesSpcodeLayers);
            oos.writeObject(checklistspeciesWmsLayers);
            oos.writeObject(checklistspeciesMetadataLayers);
            oos.writeObject(checklistspeciesSpcodeLayers);
            oos.writeObject(checklistspeciesWmsLayersBySpcode);
            oos.writeObject(i18nProperites);
            oos.writeObject(facetQueryFieldList);

            oos.close();

        } catch (Exception e) {
            LOGGER.error("cannot write common data to cache", e);
        }
    }

    private static boolean loadFromCache() {
        try {
            String path = "/data/webportal/cache/";

            if (!new File(path + "commondata").exists()) return false;

            ObjectInputStream oos = new ObjectInputStream(new FileInputStream(path + "commondata"));

            speciesListAdditionalColumns = (Map<String, Map<String, List<String>>>) oos.readObject();
            downloadReasons = (JSONArray) oos.readObject();
            biocacheLayerList = (Set<String>) oos.readObject();
            speciesListCountsKosher = (Map<String, JSONObject>) oos.readObject();
            speciesListCounts = (Map<String, JSONObject>) oos.readObject();
            speciesWmsLayers = (Map<String, String[]>) oos.readObject();
            distancesMap = (Map<String, Map<String, Double>>) oos.readObject();
            distances = (JSONObject) oos.readObject();
            layerlistJSON = (JSONArray) oos.readObject();
            speciesWmsLayers = (Map<String, String[]>) oos.readObject();
            speciesMetadataLayers = (Map<String, String[]>) oos.readObject();
            speciesSpcodeLayers = (Map<String, String[]>) oos.readObject();
            checklistspeciesWmsLayers = (Map<String, String[]>) oos.readObject();
            checklistspeciesMetadataLayers = (Map<String, String[]>) oos.readObject();
            checklistspeciesSpcodeLayers = (Map<String, String[]>) oos.readObject();
            checklistspeciesWmsLayersBySpcode = (Map<String, String[]>) oos.readObject();
            i18nProperites = (Properties) oos.readObject();
            facetQueryFieldList = (List<QueryField>) oos.readObject();

            oos.close();

        } catch (Exception e) {
            LOGGER.error("cannot read common data from cache", e);
            return false;
        }

        return true;
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
                JSONParser jp = new JSONParser();
                copyDistances = (JSONObject) jp.parse(get.getResponseBodyAsString());


                //make map
                copyDistancesMap = new HashMap<String, Map<String, Double>>();
                for (Object okey : copyDistances.keySet()) {
                    Double d = (Double) copyDistances.get((String) okey);
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
        String layersListURL = layersServer + "/fields/search?q=";
        try {

            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(layersListURL);
            get.addRequestHeader(StringConstants.ACCEPT, StringConstants.JSON_JAVASCRIPT_ALL);

            int result = client.executeMethod(get);

            if (result == 200) {
                JSONParser jp = new JSONParser();
                copyLayerlistJSON = (JSONArray) jp.parse(get.getResponseBodyAsString());
            }

            //addFieldsToLayers(copyLayerlistJSON);
        } catch (Exception e) {
            LOGGER.error("error getting layers list: " + layersListURL, e);
            copyLayerlistJSON = null;
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

            JSONParser jp = new JSONParser();
            JSONArray ja = (JSONArray) jp.parse(slist);

            LOGGER.debug(ja.size() + " species wms distributions");

            for (int i = 0; i < ja.size(); i++) {
                JSONObject jo = (JSONObject) ja.get(i);
                if (jo.containsKey(StringConstants.LSID) && jo.containsKey(StringConstants.WMSURL)) {
                    //manage lsids with multiple wmsurls
                    String lsid = jo.get(StringConstants.LSID).toString();

                    //wms
                    String[] urls = copySpeciesWmsLayers.get(lsid);
                    if (urls != null) {
                        String[] newUrls = new String[urls.length + 1];
                        System.arraycopy(urls, 0, newUrls, 0, urls.length);
                        newUrls[newUrls.length - 1] = jo.get(StringConstants.WMSURL).toString();
                        urls = newUrls;
                    } else {
                        urls = new String[]{jo.get(StringConstants.WMSURL).toString()};
                    }
                    copySpeciesWmsLayers.put(lsid, urls);

                    //metadata
                    String m = "";
                    if (jo.containsKey(StringConstants.METADATA_U)) {
                        m = jo.get(StringConstants.METADATA_U).toString();
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
                        m = jo.get(StringConstants.SPCODE).toString();
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
                        spcode = jo.get(StringConstants.SPCODE).toString();
                    }
                    lsid = null;
                    if (jo.containsKey(StringConstants.LSID)) {
                        lsid = jo.get(StringConstants.LSID).toString();
                    }
                    String pid = null;
                    if (jo.containsKey(StringConstants.PID)) {
                        pid = jo.get(StringConstants.PID).toString();
                    }
                    String type = null;
                    if (jo.containsKey(StringConstants.TYPE)) {
                        type = jo.get(StringConstants.TYPE).toString();
                    }
                    copySpeciesWmsLayersBySpcode.put(spcode, new String[]{jo.get(StringConstants.SCIENTIFIC).toString(), jo.get(StringConstants.WMSURL).toString(), m, lsid, pid, type});
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
                ja = (JSONArray) jp.parse(slist);

                LOGGER.debug(ja.size() + " species wms checklists");

                for (int i = 0; i < ja.size(); i++) {
                    JSONObject jo = (JSONObject) ja.get(i);
                    if (jo.containsKey(StringConstants.LSID) && jo.containsKey(StringConstants.WMSURL)) {
                        //manage lsids with multiple wmsurls
                        String lsid = jo.get(StringConstants.LSID).toString();

                        //wms
                        String[] urls = copyChecklistspeciesWmsLayers.get(lsid);
                        if (urls != null) {
                            String[] newUrls = new String[urls.length + 1];
                            System.arraycopy(urls, 0, newUrls, 0, urls.length);
                            newUrls[newUrls.length - 1] = jo.get(StringConstants.WMSURL).toString();
                            urls = newUrls;
                        } else {
                            urls = new String[]{jo.get(StringConstants.WMSURL).toString()};
                        }
                        copyChecklistspeciesWmsLayers.put(lsid, urls);

                        //metadata
                        String m = "";
                        if (jo.containsKey(StringConstants.METADATA_U)) {
                            m = jo.get(StringConstants.METADATA_U).toString();
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
                            m = jo.get(StringConstants.SPCODE).toString();
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
                        String spcode = jo.get(StringConstants.SPCODE).toString();
                        copyChecklistspeciesWmsLayersBySpcode.put(spcode, new String[]{jo.get(StringConstants.SCIENTIFIC).toString(), jo.get(StringConstants.WMSURL).toString(), m});
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
        return getLayer(layer) == null ? getLayerFacetNameDefault(layer) : getLayer(layer).get(StringConstants.ID).toString();
    }

    public static String getLayerFacetNameDefault(String layer) {
        String facetName = layer;
        JSONObject f = getLayer(layer);
        if (f != null) {
            facetName = f.get(StringConstants.ID).toString();
        }
        return facetName;
    }

    public static String getFacetLayerName(String facet) {
        return getFacetLayerNameDefault(facet);
    }

    public static String getFacetLayerNameDefault(String facet) {
        return getLayer(facet) == null ? null : facet;
    }

    public static String getFacetLayerDisplayName(String facet) {
        JSONObject field = getLayer(facet);
        if (field != null) {
            return field.get(StringConstants.NAME).toString();
        }
        return getFacetLayerDisplayNameDefault(facet);
    }

    public static String getFacetLayerDisplayNameDefault(String facet) {
        JSONObject field = getLayer(facet);
        if (field != null) {
            return field.get(StringConstants.NAME).toString();
        }
        return null;
    }

    public static String getLayerDisplayName(String name) {
        JSONObject field = getLayer(name);
        if (field != null) {
            return field.get(StringConstants.NAME).toString();
        }
        return null;
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

            //append facet.{fieldId}={layer display name}
            List layers = getLayerListJSONArray();
            if (layers != null) {
                for (Object o : layers) {
                    JSONObject facet = (JSONObject) o;
                    String facetId = facet.get(StringConstants.ID).toString();
                    if (facetId != null) {
                        p.put("facet." + facetId, facet.get(StringConstants.NAME).toString());
                    }
                }
            } else {
                LOGGER.error("layers not added to cached i18n");
            }

            i18nProperites = p;
        } catch (Exception e) {
            LOGGER.error("error loading properties file URL: " + i18nURL, e);
        }

        init18n();
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
        String url = CommonData.getSettings().getProperty("logger.url") + "/service/logger/reasons";
        try {

            LOGGER.debug(url);
            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(url);

            int result = client.executeMethod(get);

            if (result == 200) {
                JSONParser jp = new JSONParser();
                copyDownloadReasons = (JSONArray) jp.parse(get.getResponseBodyAsString());
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
        JSONObject found = null;
        for (int i = 0; i < layerlistJSON.size() && found == null; i++) {
            JSONObject field = (JSONObject) layerlistJSON.get(i);
            if (field.get(StringConstants.ID).toString().equalsIgnoreCase(name) ||
                    (((JSONObject) field.get("layer")).get(StringConstants.NAME).toString().equalsIgnoreCase(name) &&
                            field.get("defaultlayer").toString().equals("true"))) {
                found = field;
            }
        }
        return found;
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
                JSONParser jp = new JSONParser();
                JSONArray ja = (JSONArray) jp.parse(get.getResponseBodyAsString());
                LOGGER.debug("size: " + ja.size());

                // Populate the biocache layer list with the names of all
                // indexed fields. The additional non-layer field names will
                // not cause a problem here.
                for (int i = 0; i < ja.size(); i++) {
                    String layer = ((JSONObject) ja.get(i)).get(StringConstants.NAME).toString();
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
        int extra = (speciesListInvasive.length() == 0 ? 0 : 1) + (speciesListThreatened.length() == 0 ? 0 : 1);
        String[] ret = new String[areaReportFacets.length + extra];
        System.arraycopy(areaReportFacets, 0, ret, extra, areaReportFacets.length);
        int pos = 0;
        if (speciesListInvasive.length() > 0) ret[pos++] = speciesListInvasive;
        if (speciesListThreatened.length() > 0) ret[pos++] = speciesListThreatened;
        return ret;
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

    public static LsidCountsDynamic getLsidCounts() {
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

    public static Map getSpeciesListCounts(boolean refresh) {
        if (speciesListCounts == null || refresh) {

            Map m = new HashMap();

            HttpClient client = new HttpClient();
            String url = biocacheServer
                    + "/occurrences/facets/download?facets=species_guid&count=true"
                    + "&q=geospatial_kosher:*";
            LOGGER.debug(url);
            GetMethod get = new GetMethod(url);

            try {
                client.executeMethod(get);
                CSVReader csv = new CSVReader(new BufferedReader(new InputStreamReader(get.getResponseBodyAsStream())));
                String[] row;
                csv.readNext(); //skip header
                while ((row = csv.readNext()) != null) {
                    try {
                        m.put(row[0], Long.parseLong(row[1]));
                    } catch (Exception e) {
                        LOGGER.error("error getting species_guid,count: " + url, e);
                    }
                }
            } catch (Exception e) {
                LOGGER.error("error getting species list from: " + url);
            }

            speciesListCountsUpdated = System.currentTimeMillis();
            speciesListCounts = m;
        }
        return speciesListCounts;
    }

    public static Map getSpeciesListCountsKosher(boolean refresh) {
        if (speciesListCountsKosher == null || refresh) {
            Map m = new HashMap();

            HttpClient client = new HttpClient();
            String url = biocacheServer
                    + "/occurrences/facets/download?facets=species_guid&count=true"
                    + "&q=geospatial_kosher:true";
            LOGGER.debug(url);
            GetMethod get = new GetMethod(url);

            try {
                client.executeMethod(get);
                CSVReader csv = new CSVReader(new BufferedReader(new InputStreamReader(get.getResponseBodyAsStream())));
                String[] row;
                csv.readNext(); //skip header
                while ((row = csv.readNext()) != null) {
                    try {
                        m.put(row[0], Long.parseLong(row[1]));
                    } catch (Exception e) {
                        LOGGER.error("error getting species_guid,count (kosher): " + url, e);
                    }
                }
            } catch (Exception e) {
                LOGGER.error("error getting species list from: " + url);
            }

            speciesListCountsUpdatedKosher = System.currentTimeMillis();
            speciesListCountsKosher = m;
        }
        return speciesListCountsKosher;
    }

    //see https://www.journalmap.org/search
    //don't refresh this on cache refresh
    static List<JSONObject> journalMapArticles = null;
    static List<JournalMapLocation> journalMapLocations = null;

    private static void initJournalmap() {
        if (journalMapArticles != null && journalMapArticles.size() > 0) {
            return;
        }

        journalMapArticles = new ArrayList<JSONObject>();
        journalMapLocations = new ArrayList<JournalMapLocation>();

        try {

            String journalmapUrl = CommonData.getSettings().getProperty("journalmap.url", null);
            String journalmapKey = CommonData.getSettings().getProperty("journalmap.api_key", null);

            //try disk cache
            File jaFile = new File("/data/webportal/journalmapArticles.json");

            if (jaFile.exists()) {
                JSONParser jp = new JSONParser();
                JSONArray ja = (JSONArray) jp.parse(FileUtils.readFileToString(jaFile));

                for (int i = 0; i < ja.size(); i++) {
                    journalMapArticles.add((JSONObject) ja.get(i));
                }
            } else if (journalmapKey != null && !journalmapKey.isEmpty()) {

                int page = 1;
                int maxpage = 0;
                List<String> publicationsIds = new ArrayList<String>();
                while (page == 1 || page <= maxpage) {
                    HttpClient client = new HttpClient();

                    String url = journalmapUrl + "api/publications.json?version=1.0&key=" + journalmapKey + "&page=" + page;
                    page = page + 1;

                    LOGGER.debug("journalmap url: " + url);

                    GetMethod get = new GetMethod(url);

                    int result = client.executeMethod(get);

                    //update maxpage
                    maxpage = Integer.parseInt(get.getResponseHeader("X-Pages").getValue());

                    //cache
                    JSONParser jp = new JSONParser();
                    JSONArray jcollection = (JSONArray) jp.parse(get.getResponseBodyAsString());
                    for (int i = 0; i < jcollection.size(); i++) {
                        if (((JSONObject) jcollection.get(i)).containsKey("id")) {
                            publicationsIds.add(((JSONObject) jcollection.get(i)).get("id").toString());
                            LOGGER.debug("found publication: " + ((JSONObject) jcollection.get(i)).get("id").toString() + ", article_count: " + ((JSONObject) jcollection.get(i)).get("articles_count").toString());
                        }
                    }
                }

                for (String publicationsId : publicationsIds) {
                    //allow for collection failure
                    try {
                        page = 1;
                        maxpage = 0;
                        while (page == 1 || page <= maxpage) {
                            HttpClient client = new HttpClient();

                            String url = journalmapUrl + "api/articles.json?version=1.0&key=" + journalmapKey + "&page=" + page + "&publication_id=" + publicationsId;
                            page = page + 1;

                            LOGGER.debug("journalmap url: " + url);

                            GetMethod get = new GetMethod(url);

                            int result = client.executeMethod(get);

                            //update maxpage
                            maxpage = Integer.parseInt(get.getResponseHeader("X-Pages").getValue());

                            //cache
                            JSONParser jp = new JSONParser();
                            JSONArray jarticles = (JSONArray) jp.parse(get.getResponseBodyAsString());
                            for (int j = 0; j < jarticles.size(); j++) {
                                JSONObject o = (JSONObject) jarticles.get(j);
                                if (o.containsKey("locations")) {
                                    journalMapArticles.add(o);
                                }
                            }
                        }
                    } catch (Exception e) {
                        LOGGER.error("journalmap - failure to get articles from publicationsId: " + publicationsId);
                    }
                }

                //save to disk cache
                FileWriter fw = new FileWriter(jaFile);
                JSONValue.writeJSONString(journalMapArticles, fw);
                fw.flush();
                fw.close();
            }
        } catch (Exception e) {
            LOGGER.error("error initialising journalmap data", e);
        }

        //construct locations list
        for (int i = 0; i < journalMapArticles.size(); i++) {
            JSONArray locations = (JSONArray) journalMapArticles.get(i).get("locations");

            for (int j = 0; j < locations.size(); j++) {
                JSONObject l = (JSONObject) locations.get(j);
                double longitude = Double.parseDouble(l.get("longitude").toString());
                double latitude = Double.parseDouble(l.get("latitude").toString());
                journalMapLocations.add(new JournalMapLocation(longitude, latitude, i));
            }
        }
    }

    static class JournalMapLocation {
        Point point;
        int idx;

        public JournalMapLocation(double longitude, double latitude, int idx) {
            Coordinate c = new Coordinate(longitude, latitude);
            GeometryFactory gf = new GeometryFactory();
            point = gf.createPoint(c);

            this.idx = idx;
        }
    }

    public static List<JSONObject> filterJournalMapArticles(String wkt) {
        List<JSONObject> list = new ArrayList<JSONObject>();
        Set<Integer> set = new HashSet<Integer>();

        try {
            WKTReader wktReader = new WKTReader();
            com.vividsolutions.jts.geom.Geometry g = wktReader.read(wkt);

            for (JournalMapLocation l : journalMapLocations) {
                //only add it once (articles can have >1 location)
                if (!set.contains(l.idx)) {
                    if (g.contains(l.point)) {
                        list.add(journalMapArticles.get(l.idx));
                        set.add(l.idx);
                    }
                }
            }

        } catch (Exception e) {
            LOGGER.error("error intersecting wkt with journal map articles", e);
        }

        return list;
    }

    public static String speciesListThreatened = "";
    public static String speciesListInvasive = "";

    private static Map<String, Map<String, List<String>>> initSpeciesListAdditionalColumns() {
        Map<String, Map<String, List<String>>> map = new HashMap<String, Map<String, List<String>>>();

        String slac = settings.getProperty("species.list.additional.columns", "");
        //append dynamic columns
        slac += dynamicSpeciesListColumns();
        String[] columns = slac.split("\\|");
        for (String line : columns) {
            String[] parts = line.split(",");
            if (parts[0].equals("Conservation")) {
                speciesListThreatened = "species_list_uid:" + StringUtils.join(Arrays.copyOfRange(parts, 1, parts.length), " OR species_list_uid:");
            }
            if (parts[0].equals("Invasive")) {
                speciesListInvasive = "species_list_uid:" + StringUtils.join(Arrays.copyOfRange(parts, 1, parts.length), " OR species_list_uid:");
            }
            if (parts.length > 1) {
                String columnTitle = parts[0];
                for (int i = 1; i < parts.length; i++) {
                    try {
                        JSONParser jp = new JSONParser();
                        InputStream is = new URL(CommonData.getSpeciesListServer() + "/ws/speciesList?druid=" + parts[i]).openStream();
                        String listName = ((JSONObject) jp.parse(IOUtils.toString(is))).get("listName").toString();
                        is.close();

                        Map<String, List<String>> m = map.get(columnTitle);
                        if (m == null) m = new HashMap<String, List<String>>();
                        ArrayList<String> sp = new ArrayList<String>();
                        //fetch species list

                        Collection<SpeciesListItemDTO> list = SpeciesListUtil.getListItems(parts[i]);
                        for (SpeciesListItemDTO item : list) {
                            if (item.getLsid() != null && !item.getLsid().isEmpty()) sp.add(item.getLsid());
                        }

                        Collections.sort(sp);
                        m.put(listName, sp);
                        map.put(columnTitle, m);
                    } catch (Exception e) {
                        LOGGER.error("error reading list: " + parts[i], e);
                    }
                }
            }
        }

        return map;
    }

    private static String dynamicSpeciesListColumns() {
        StringBuilder sb = new StringBuilder();
        try {
            JSONParser jp = new JSONParser();
            JSONObject threatened = (JSONObject) jp.parse(Util.readUrl(settings.getProperty("species_list_url", "") + "/ws/speciesList/?isThreatened=eq:true&isAuthoritative=eq:true"));
            JSONObject invasive = (JSONObject) jp.parse(Util.readUrl(settings.getProperty("species_list_url", "") + "/ws/speciesList/?isInvasive=eq:true&isAuthoritative=eq:true"));

            JSONObject[] lists = {threatened, invasive};

            for (JSONObject o : lists) {
                if (sb.length() == 0) sb.append("Conservation");
                else sb.append("|Invasive");
                JSONArray ja = (JSONArray) o.get("lists");
                for (int i = 0; i < ja.size(); i++) {
                    sb.append(",").append(((JSONObject) ja.get(i)).get("dataResourceUid"));
                }
            }

        } catch (Exception e) {
            LOGGER.error("failed to get species lists for threatened or invasive species", e);
        }
        return sb.toString();
    }

    public static List<String> getSpeciesListAdditionalColumnsHeader() {
        return new ArrayList<String>(speciesListAdditionalColumns.keySet());
    }

    public static List<String> getSpeciesListAdditionalColumns(List<String> headers, String lsid) {
        //extract lsid from names_and_lsid field
        if (lsid != null && lsid.contains("|") && lsid.split("\\|").length > 1) lsid = lsid.split("\\|")[1];
        List<String> list = new ArrayList<String>();
        for (int i = 0; i < headers.size(); i++) {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, List<String>> entry : speciesListAdditionalColumns.get(headers.get(i)).entrySet()) {
                List<String> sorted = entry.getValue();
                if (sorted != null && Collections.binarySearch(sorted, lsid) >= 0) {
                    if (sb.length() > 0) {
                        sb.append("|");
                    }
                    sb.append(entry.getKey());
                }
            }
            list.add(sb.toString());
        }

        return list;
    }

    private static void init18n() {
        try {
            Map<String, String[][]> tmpMap = new LinkedHashMap<String, String[][]>();
            List<QueryField> tmpList = new ArrayList<QueryField>();
            //get the JSON from the WS\
            JSONParser jp = new JSONParser();
            JSONArray values = (JSONArray) jp.parse(Util.readUrl(CommonData.getBiocacheServer() + FACET_SUFFIX));

            LOGGER.debug(values);
            Map<String, QueryField.FieldType> dataTypes = getDataTypes();
            for (Object v : values) {
                JSONObject value = (JSONObject) v;
                //extract the group
                String title = value.get(StringConstants.TITLE).toString();
                //now get the facets themselves
                List<Map<String, String>> facets = (List<Map<String, String>>) value.get("facets");
                String[][] facetValues = new String[facets.size()][2];
                int i = 0;
                for (Map<String, String> facet : facets) {
                    String field = facet.get("field");
                    //Only add if it is not included in the ignore list
                    if (!CommonData.ignoredFacets.contains(field)) {
                        String i18n = i18nProperites.getProperty("facet." + field, field);

                        //TODO: update biocache i18n instead of doing this
                        if ("data_provider".equals(field)) {
                            i18n = "Data Provider";
                        }

                        //use current layer names for facets
                        try {
                            String layername = CommonData.getFacetLayerName(field);
                            if (i18n == null || layername != null) {
                                i18n = CommonData.getLayerDisplayName(layername);
                            }
                        } catch (Exception e) {

                        }

                        facetValues[i][0] = field;
                        facetValues[i][1] = i18n;
                        QueryField.FieldType ftype = dataTypes.containsKey(field) ? dataTypes.get(field) : QueryField.FieldType.STRING;


                        QueryField qf = new QueryField(field, i18n, QueryField.GroupType.getGroupType(title), ftype);
                        tmpList.add(qf);
                        i++;
                    }
                }
                tmpMap.put(title, facetValues);
            }

            //add a bunch of configured extra fields from the default values
            for (String f : CommonData.customFacets) {
                String i18n = i18nProperites.getProperty("facet." + f, f);
                tmpList.add(new QueryField(f, i18n, QueryField.GroupType.CUSTOM, QueryField.FieldType.STRING));
            }

            facetQueryFieldList = tmpList;
            LOGGER.debug("Grouped Facets: " + tmpMap);
            LOGGER.debug("facet query list : " + facetQueryFieldList);
        } catch (Exception e) {
            LOGGER.error("failed to init i18n", e);
        }
    }

    public static List<QueryField> getFacetQueryFieldList() {
        return facetQueryFieldList;
    }

    /**
     * Extracts the biocache data types from the webservice so that they can be used to dynamically load the facets
     *
     * @return
     */
    private static Map<String, QueryField.FieldType> getDataTypes() throws Exception {
        Map<String, QueryField.FieldType> map = new HashMap<String, QueryField.FieldType>();
        //get the JSON from the WS
        JSONParser jp = new JSONParser();
        JSONArray values = (JSONArray) jp.parse(Util.readUrl(CommonData.getBiocacheServer() + "/index/fields"));
        for (Object mvalues : values) {
            String name = ((JSONObject) mvalues).get(StringConstants.NAME).toString();
            String dtype = "string";
            if (((JSONObject) mvalues).containsKey("dataType"))
                dtype = ((JSONObject) mvalues).get("dataType").toString();

            if ("string".equals(dtype) || "textgen".equals(dtype)) {
                map.put(name, QueryField.FieldType.STRING);
            } else if ("int".equals(dtype) || "tint".equals(dtype) || "tdate".equals(dtype)) {
                map.put(name, QueryField.FieldType.INT);
            } else if ("double".equals(dtype) || "tdouble".equals(dtype)) {
                map.put(name, QueryField.FieldType.DOUBLE);
            } else {
                map.put(name, QueryField.FieldType.STRING);
            }
        }
        return map;
    }
}
