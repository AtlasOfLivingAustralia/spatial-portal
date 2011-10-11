/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.ala.spatial.data.LsidCounts;
import org.ala.spatial.data.QueryField;
import org.ala.spatial.sampling.SimpleShapeFileCache;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.lang.StringUtils;

/**
 * common data store
 *
 * 1. alaspatial environmental and contextual layer names (use in LayersUtil)
 * 2. alaspatial layers associations, environmental and contextual layer names (use in EnvironmentalList)
 * 3. layer list as string and JSON (layer tree, (2))
 *
 *
 * @author Adam
 */
public class CommonData {

    //common data
    static public final String WORLD_WKT = "POLYGON((-179.999 -84.999,-179.999 84.999,179.999 84.999,179.999 -84.999,-179.999 -84.999))";
    static public final String AUSTRALIA_WKT = "POLYGON((112.0 -44.0,112.0 -9.0,154.0 -9.0,154.0 -44.0,112.0 -44.0))";
    //common parameters
    static final String SAT_URL = "sat_url";
    static final String GEOSERVER_URL = "geoserver_url";
    static final String LAYERS_URL = "layers_url";
    static final String WEBPORTAL_URL = "webportal_url";
    static final String BIE_URL = "bie_url";
    static final String BIOCACHE_SERVICE_URL = "biocache_service_url";
    static final String BIOCACHE_WEBAPP_URL = "biocache_webapp_url";
    static final String DEFAULT_UPLOAD_SAMPLING = "default_upload_sampling";

    //(1) for LayersUtil
    static String[] environmentalLayerNames = null;
    static String[] contextualLayerNames = null;
    static String[] copy_environmentalLayerNames = null;
    static String[] copy_contextualLayerNames = null;
    //(2) for EnvironmentalList
    static ArrayList<ListEntry> listEntriesAll;
    static String[] layerNamesAll;
    static ArrayList<ListEntry> listEntriesEnv;
    static String[] layerNamesEnv;
    static float[][] distances;
    static ArrayList<ListEntry> copy_listEntriesAll;
    static String[] copy_layerNamesAll;
    static ArrayList<ListEntry> copy_listEntriesEnv;
    static String[] copy_layerNamesEnv;
    static float[][] copy_distances;
    //(3) for layer list json
    static String layerlist = null;
    static JSONArray layerlistJSON = null;
    static HashMap<JSONObject, List> contextualClasses = null;
    static private ArrayList empty = new ArrayList();
    static String copy_layerlist = null;
    static JSONArray copy_layerlistJSON = null;
    static HashMap<JSONObject, List> copy_contextualClasses = null;
    static String defaultFieldString = null;
    public static SimpleShapeFileCache ssfCache;
    //(4) species with distribution layres
    /**
     * key = LSID
     * value = list of WMS names
     */
    static HashMap<String, String[]> species_wms_layers = null;
    static HashMap<String, String[]> copy_species_wms_layers = null;
    static HashMap<String, String[]> species_metadata_layers = null;
    static HashMap<String, String[]> copy_species_metadata_layers = null;
    //Common
    static public String satServer;
    static public String geoServer;
    static public String layersServer;
    static public String webportalServer;
    static public String bieServer;
    static public String biocacheServer;
    static public String biocacheWebServer;
    static public Map<String, String> settings;
    //lsid counts, for species autocomplete
    static public LsidCounts lsidCounts;

    /*
     * initialize common data from geoserver and satserver
     */
    static public void init(Map<String, String> settings) {
        //Common
        satServer = settings.get(SAT_URL);
        geoServer = settings.get(GEOSERVER_URL);
        layersServer = settings.get(LAYERS_URL);
        webportalServer = settings.get(WEBPORTAL_URL);
        bieServer = settings.get(BIE_URL);
        biocacheServer = settings.get(BIOCACHE_SERVICE_URL);
        biocacheWebServer = settings.get(BIOCACHE_WEBAPP_URL);
        defaultFieldString = settings.get(DEFAULT_UPLOAD_SAMPLING);
        CommonData.settings = settings;

        //(1) for LayersUtil
        initEnvironmentalLayers();
        initContextualLayers();

        //(3) for layer list json
        initLayerList();
        initContextualClasses();

        //(2) for EnvironmentalList - uses layer list json, so run after
        initEnvironmentalOnlyList();
        initEnvironmentalAllList();

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

        //(1) for LayersUtil
        if (copy_environmentalLayerNames != null) {
            environmentalLayerNames = copy_environmentalLayerNames;
        }
        if (copy_contextualLayerNames != null) {
            contextualLayerNames = copy_contextualLayerNames;
        }

        //(2) for EnvironmentalList
        if (copy_listEntriesAll != null) {
            listEntriesAll = copy_listEntriesAll;
        }
        if (copy_layerNamesAll != null) {
            layerNamesAll = copy_layerNamesAll;
        }
        if (copy_listEntriesEnv != null) {
            listEntriesEnv = copy_listEntriesEnv;
        }
        if (copy_layerNamesEnv != null) {
            layerNamesEnv = copy_layerNamesEnv;
        }
        if (copy_distances != null) {
            distances = copy_distances;
        }

        //(3) for layer list json
        if (copy_layerlist != null) {
            layerlist = copy_layerlist;
        }
        if (copy_layerlistJSON != null) {
            layerlistJSON = copy_layerlistJSON;
        }
        if (copy_contextualClasses != null) {
            contextualClasses = copy_contextualClasses;
        }

        //(4) for species wms distributions
        if (copy_species_wms_layers != null) {
            species_wms_layers = copy_species_wms_layers;
        }
        if (copy_species_metadata_layers != null) {
            species_metadata_layers = copy_species_metadata_layers;
        }
    }

    /**
     * gets list of environmental layers from SAT server by
     * layer name
     *
     * @return environmental layer names as String[] or null on error
     */
    static public String[] getEnvironmentalLayers() {
        return environmentalLayerNames;
    }

    static void initEnvironmentalLayers() {
        System.out.println("CommonData::initEnvironmentalLayers");
        String[] aslist = null;
        try {
            String envurl = satServer + "/ws/spatial/settings/layers/environmental/string";

            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(envurl);
            get.addRequestHeader("Content-type", "text/plain");

            int result = client.executeMethod(get);
            String slist = get.getResponseBodyAsString();

            aslist = slist.split("\n");

            for (int i = 0; i < aslist.length; i++) {
                aslist[i] = aslist[i].trim();
            }

        } catch (Exception e) {
            System.out.println("error setting up env list");
            aslist = null;
            e.printStackTrace(System.out);
        }

        /* retain list for future calls */
        copy_environmentalLayerNames = aslist;
    }

    /**
     * gets list of contextual layers from SAT server by
     * layer name
     *
     * @return contextual layer names as String[] or null on error
     */
    static public String[] getContextualLayers() {
        return contextualLayerNames;
    }

    static void initContextualLayers() {
        System.out.println("CommonData::initContextualLayers()");
        String[] aslist = null;
        try {
            String envurl = satServer + "/ws/spatial/settings/layers/contextual/string";

            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(envurl);
            get.addRequestHeader("Content-type", "text/plain");

            int result = client.executeMethod(get);
            String slist = get.getResponseBodyAsString();

            aslist = slist.split("\n");

            for (int i = 0; i < aslist.length; i++) {
                aslist[i] = aslist[i].trim();
            }

        } catch (Exception e) {
            System.out.println("error setting up ctx list");
            aslist = null;
            e.printStackTrace(System.out);
        }

        /* retain for future calls */
        copy_contextualLayerNames = aslist;
    }

    static public ArrayList<ListEntry> getListEntriesAll() {
        return listEntriesAll;
    }

    static public String[] getLayerNamesAll() {
        return layerNamesAll;
    }

    static public ArrayList<ListEntry> getListEntriesEnv() {
        return listEntriesEnv;
    }

    static public String[] getLayerNamesEnv() {
        return layerNamesEnv;
    }

    static public float[][] getDistances() {
        return distances;
    }

    static public void initEnvironmentalOnlyList() {
        System.out.println("CommonData::initEnvironmentalOnlyList()");
        try {
            //environmental only
            StringBuffer sbProcessUrl = new StringBuffer();
            sbProcessUrl.append(satServer + "/layers/analysis/inter_layer_association_rawnames.csv");

            System.out.println(sbProcessUrl.toString());
            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(sbProcessUrl.toString());

            get.addRequestHeader("Accept", "text/plain");

            int result = client.executeMethod(get);
            String slist = get.getResponseBodyAsString();

            String[] rows = slist.split("\n");

            //got a csv, put into column names, etc
            copy_layerNamesEnv = new String[rows.length]; //last row is empty
            copy_distances = new float[rows.length - 1][rows.length - 1];

            String[] line = rows[0].split(",");
            copy_layerNamesEnv[0] = line[1];
            for (int i = 1; i < rows.length; i++) {   //last row is empty
                line = rows[i].split(",");
                copy_layerNamesEnv[i] = line[0];
                for (int j = 1; j < line.length; j++) {
                    try {
                        copy_distances[i - 1][j - 1] = Float.parseFloat(line[j]);
                    } catch (Exception e) {
                    }
                }
            }

            copy_listEntriesEnv = new ArrayList<ListEntry>();
            initLayerCatagories(copy_listEntriesEnv, true);

            //match up listEntries
            for (int i = 0; i < copy_listEntriesEnv.size(); i++) {
                String entryName = copy_listEntriesEnv.get(i).name;
                for (int j = 0; j < copy_layerNamesEnv.length; j++) {
                    if (copy_layerNamesEnv[j].equalsIgnoreCase(entryName)) {
                        copy_listEntriesEnv.get(i).row_in_distances = j;
                        break;
                    }
                }

                //remove if missing
                if (copy_listEntriesEnv.get(i).row_in_distances < 0) {
                    //        System.out.println("absent from layers assoc mx: " + listEntries.get(i).name);
                    copy_listEntriesEnv.remove(i);
                    i--;
                } else {
                    copy_listEntriesEnv.get(i).row_in_list = i;
                }
            }
        } catch (Exception e) {
            copy_layerNamesEnv = null;
            copy_distances = null;

            e.printStackTrace();
        }
    }

    static void initLayerCatagories(ArrayList<ListEntry> listEntries, boolean environmentalOnly) {
        try {
            JSONArray layerlist = copy_layerlistJSON;
            for (int i = 0; i < layerlist.size(); i++) {
                JSONObject jo = layerlist.getJSONObject(i);
                String uid = jo.getString("uid");
                String type = jo.getString("type");
                String c1 = jo.getString("classification1");
                String c2 = jo.getString("classification2");
                String name = jo.getString("name");
                String displayname = StringUtils.capitalize(jo.getString("displayname"));

                if (!jo.getBoolean("enabled")) {
                    continue;
                }

                if (environmentalOnly && type.equalsIgnoreCase("Contextual")) {
                    continue;
                }

                if (c1 == null || c1.equalsIgnoreCase("null")) {
                    c1 = "";
                }
                if (c2 == null || c2.equalsIgnoreCase("null")) {
                    c2 = "";
                }

                listEntries.add(new ListEntry(name, displayname, c1, c2, type, 1, -1, -1, uid));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        java.util.Collections.sort(listEntries, new Comparator<ListEntry>() {

            public int compare(ListEntry e1, ListEntry e2) {
                //type, then catagory 1, then catagory 2, then display name
                int c = -1 * e1.type.compareTo(e2.type);
                if (c == 0) {
                    c = e1.catagory1.compareTo(e2.catagory1);
                    if (c == 0) {
                        c = e1.catagory2.compareTo(e2.catagory2);
                        if (c == 0) {
                            c = e1.displayname.compareTo(e2.displayname);
                        }
                    }
                }

                return c;
            }
        });

    }

    static void initEnvironmentalAllList() {
        System.out.println("CommonData::initEnvironmentalAllList()");
        //contextual and environmental
        try {
            String[] ctx = copy_contextualLayerNames;
            ;
            String[] env = copy_environmentalLayerNames;

            copy_layerNamesAll = new String[ctx.length + env.length];
            for (int i = 0; i < env.length; i++) {
                copy_layerNamesAll[i] = env[i];
            }
            for (int i = 0; i < ctx.length; i++) {
                copy_layerNamesAll[i + env.length] = ctx[i];
            }

            copy_listEntriesAll = new ArrayList<ListEntry>();
            initLayerCatagories(copy_listEntriesAll, false);

            //match up listEntries
            for (int i = 0; i < copy_listEntriesAll.size(); i++) {
                String entryName = copy_listEntriesAll.get(i).name;
                for (int j = 0; j < copy_layerNamesAll.length; j++) {
                    if (copy_layerNamesAll[j].equalsIgnoreCase(entryName)) {
                        copy_listEntriesAll.get(i).row_in_distances = j;
                        break;
                    }
                }

                //remove if missing
                if (copy_listEntriesAll.get(i).row_in_distances < 0) {
                    //        System.out.println("absent from layers assoc mx: " + listEntries.get(i).name);
                    copy_listEntriesAll.remove(i);
                    i--;
                } else {
                    copy_listEntriesAll.get(i).row_in_list = i;
                }
            }
        } catch (Exception e) {
            copy_layerNamesAll = null;
            copy_listEntriesAll = null;
            e.printStackTrace();
        }
    }

    public static String getLayerList() {
        return layerlist;
    }

    static void initLayerList() {
        try {
            String layersListURL = satServer + "/ws/layers/list";
            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(layersListURL);
            get.addRequestHeader("Accept", "application/json, text/javascript, */*");

            int result = client.executeMethod(get);
            copy_layerlist = get.getResponseBodyAsString();
            copy_layerlistJSON = JSONArray.fromObject(copy_layerlist);
        } catch (Exception e) {
            copy_layerlist = null;
            copy_layerlistJSON = null;
            e.printStackTrace();
        }
    }

    static public JSONArray getLayerListJSONArray() {
        return layerlistJSON;
    }

    static public List getContextualClasses(JSONObject layer) {
        return contextualClasses.get(layer);
    }

    static void initContextualClasses() {
        System.out.println("CommonData::initContextualClasses()");

        try {
            copy_contextualClasses = new HashMap<JSONObject, List>();

            for (int i = 0; i < copy_layerlistJSON.size(); i++) {
                JSONObject jo = copy_layerlistJSON.getJSONObject(i);

                if (!jo.getBoolean("enabled")) {
                    continue;
                }

                List classNodes = new ArrayList();
                if (jo.getString("type").equalsIgnoreCase("Contextual")) {
                    classNodes = getContextualClassesInit(jo);
                    copy_contextualClasses.put(jo, classNodes);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            copy_contextualClasses = null;
        }
    }

    static List getContextualClassesInit(JSONObject joLayer) {
        String layerName = joLayer.getString("name");
        String layerDisplayName = joLayer.getString("displayname");
        String classesURL = layersServer + "/layer/classes/cl" + joLayer.getString("id");
        HttpClient client = new HttpClient();
        GetMethod get = new GetMethod(classesURL);

        List<String> classList = new ArrayList();
        List classNodes = new ArrayList();
//        try {
//            int result = client.executeMethod(get);
//            String classes = get.getResponseBodyAsString();
//
//            JSONArray ja = JSONArray.fromObject(classes);
//
//            if(ja != null) {
//                for (int i = 0;i<ja.size();i++) {
//                    JSONObject jo = ja.getJSONObject(i);
//
//                    String info = "{displayname:'"
//                            + jo.getString("name")
//                            + "',type:'class',displaypath:'"
//                            + jo.getString("pid")
//                            + "',uid:'"
//                            + joLayer.getString("uid")
//                            + "',classname:'"
//                            + ""
//                            + "',layername:'"
//                            + layerDisplayName
//                            + "'}";
//
//                    JSONObject joClass = JSONObject.fromObject(info);
//                    classNodes.add(new SimpleTreeNode(joClass, empty));
//                }
//            }
//        } catch (Exception e) {
//            System.out.println("Failure to get contextual classes for: " + layerName);
//        }

        return classNodes;
    }

    public static String covertMillisecondsToDate(long ms) {
        DateFormat formatter = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss.SSS");
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(ms);

        return formatter.format(calendar.getTime());

    }

    private static void initSpeciesWMSLayers() {
        try {
            String layersListURL = layersServer + "/distributions";
            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(layersListURL);
            get.addRequestHeader("Accept", "application/json, text/javascript, */*");

            copy_species_wms_layers = new HashMap<String, String[]>();
            copy_species_metadata_layers = new HashMap<String, String[]>();

            int result = client.executeMethod(get);
            String slist = get.getResponseBodyAsString();

            JSONArray ja = JSONArray.fromObject(slist);

            System.out.println(ja.size() + " species wms distributions");

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
                }
            }
        } catch (Exception e) {
            copy_species_wms_layers = null;
            copy_species_metadata_layers = null;
            e.printStackTrace();
        }
    }

    /**
     * returns array of WMS species requests
     */
    static public String[] getSpeciesDistributionWMS(String lsids) {
        if(species_wms_layers == null) {
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
     * returns array of WMS species requests
     */
    static public String[] getSpeciesDistributionMetadata(String lsids) {
        if(species_wms_layers == null) {
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
    static HashMap<String, String> layerToFacet;
    static HashMap<String, String> facetToLayer;
    static HashMap<String, String> facetShapeNameField;
    static HashMap<String, String> facetToLayerDisplayName;

    public static String getLayerFacetName(String layer) {
        return layerToFacet.get(layer.toLowerCase());
    }

    public static String getFacetLayerName(String facet) {
        return facetToLayer.get(facet);
    }

    public static String getFacetShapeNameField(String facet) {
        return facetShapeNameField.get(facet);
    }

    public static String getFacetLayerDisplayName(String facet) {
        return facetToLayerDisplayName.get(facet);
    }

    private static void readLayerInfo() {
        try {
            HashMap<String, String> ftl = new HashMap<String, String>();
            HashMap<String, String> ltf = new HashMap<String, String>();
            HashMap<String, String> fsnf = new HashMap<String, String>();
            HashMap<String, String> ftldn = new HashMap<String, String>();

            String filename = CommonData.class.getResource("/layers.txt").getFile();
            BufferedReader br = new BufferedReader(new FileReader(filename));
            String line;
            while ((line = br.readLine()) != null) {
                String[] record = line.split(",");

                String layer = record[1];
                String facet = (record[2].equals("Contextual") ? "cl" : "el") + record[0];

                ltf.put(layer.toLowerCase(), facet);
                ftl.put(facet, layer);

                ftldn.put(facet, record[3]);

                if (record.length > 4) {
                    fsnf.put(facet, record[4]);
                }
            }
            br.close();

            layerToFacet = ltf;
            facetToLayer = ftl;
            facetShapeNameField = fsnf;
            facetToLayerDisplayName = ftldn;
        } catch (Exception e) {
            e.printStackTrace();
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
            e.printStackTrace();
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
        //requres readLayerInfo() first
        String[] layers = new String[fields.length];
        String[] columns = new String[fields.length];
        for (int i = 0; i < fields.length; i++) {
            layers[i] = getFacetLayerName(fields[i]);
            columns[i] = getFacetShapeNameField(fields[i]);
        }

        if (ssfCache == null) {
            ssfCache = new SimpleShapeFileCache(layers, columns);
        } else {
            ssfCache.update(layers, columns);
        }
    }
}
