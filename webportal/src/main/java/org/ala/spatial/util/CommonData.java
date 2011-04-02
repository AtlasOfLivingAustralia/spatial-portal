/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.lang.StringUtils;
import org.zkoss.zul.SimpleTreeNode;

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

    //common parameters
    static public final String SAT_URL = "sat_url";
    static public final String GEOSERVER_URL = "geoserver_url";
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
    //(4) species with distribution layres
    /**
     * key = LSID
     * value = list of WMS names
     */
    static HashMap<String, String[]> species_wms_layers = null;
    static HashMap<String, String[]> copy_species_wms_layers = null;
    //Common
    static String satServer;
    static String geoServer;

    /*
     * initialize common data from geoserver and satserver
     */
    static public void init(String satServer_, String geoServer_) {
        System.out.println("CommonData.init(" + satServer_ + "," + geoServer_);

        //Common
        satServer = satServer_;
        geoServer = geoServer_;

        //TODO: allow for data refresh

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
        if(copy_species_wms_layers != null) {
            species_wms_layers = copy_species_wms_layers;
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
            String envurl = satServer + "/alaspatial/ws/spatial/settings/layers/environmental/string";

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
            String envurl = satServer + "/alaspatial/ws/spatial/settings/layers/contextual/string";

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
            sbProcessUrl.append(satServer + "/alaspatial/layers/analysis/inter_layer_association_rawnames.csv");

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
            String llist = copy_layerlist;

            JSONArray layerlist = copy_layerlistJSON;
            JSONArray.fromObject(llist);
            for (int i = 0; i < layerlist.size(); i++) {
                JSONObject jo = layerlist.getJSONObject(i);

                if (!jo.getBoolean("enabled")) {
                    continue;
                }

                if (environmentalOnly && jo.getString("type").equalsIgnoreCase("Contextual")) {
                    continue;
                }

                String c1 = jo.getString("classification1");
                String c2 = jo.getString("classification2");
                String name = jo.getString("name");
                String displayname = StringUtils.capitalize(jo.getString("displayname"));
                if (c1 == null || c1.equalsIgnoreCase("null")) {
                    c1 = "";
                }
                if (c2 == null || c2.equalsIgnoreCase("null")) {
                    c2 = "";
                }
                String uid = jo.getString("uid");

                listEntries.add(new ListEntry(name, displayname, c1, c2, 1, -1, -1, uid));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        java.util.Collections.sort(listEntries, new Comparator<ListEntry>() {

            public int compare(ListEntry e1, ListEntry e2) {
                //catagory 1, then catagory 2, then display name
                int c = e1.displayname.compareTo(e2.displayname);
                if (c == 0) {
                    c = e1.catagory1.compareTo(e2.catagory1);
                    if (c == 0) {
                        c = e1.catagory2.compareTo(e2.catagory2);
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
            String layersListURL = satServer + "/alaspatial/ws/layers/list";
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
        String classesURL = geoServer + "/geoserver/rest/gazetteer/" + layerName + ".json";
        HttpClient client = new HttpClient();
        GetMethod get = new GetMethod(classesURL);
        //get.addRequestHeader("Content-type", "application/json");
        //get.addRequestHeader("Accept", "application/json, text/javascript, */*");
        List<String> classList = new ArrayList();
        List classNodes = new ArrayList();
        try {
            int result = client.executeMethod(get);
            String classes = get.getResponseBodyAsString();

            JSONObject joLayers = JSONObject.fromObject(classes);
            JSONObject joClasses = joLayers.getJSONObject("layer_classes");
            String classAttribute = joClasses.keys().next().toString();
            classList = Arrays.asList(joClasses.getString(classAttribute).split(","));

            for (String classVal : classList) {
                //     System.out.println("CLASS:"+(String)classVal);
                if (!classVal.contentEquals("none")) {
                    String info = "{displayname:'"
                            + classVal
                            + "',type:'class',displaypath:'"
                            + joLayer.getString("displaypath")
                            + "',uid:'"
                            + joLayer.getString("uid")
                            + "',classname:'"
                            + classAttribute
                            + "',layername:'"
                            + layerDisplayName
                            + "'}";
                    //           System.out.println(info);
                    JSONObject joClass = JSONObject.fromObject(info);
                    classNodes.add(new SimpleTreeNode(joClass, empty));
                }
            }
            return classNodes;
        } catch (Exception e) {
            System.out.println("Failure to get contextual classes for: " + layerName);
            return classNodes;
        }
    }

    public static String covertMillisecondsToDate(long ms) {
        DateFormat formatter = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss.SSS");
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(ms);

        return formatter.format(calendar.getTime());

    }

    private static void initSpeciesWMSLayers() {
        try {
            String layersListURL = satServer + "/alaspatial/ws/intersect/list";
            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(layersListURL);
            get.addRequestHeader("Accept", "application/json, text/javascript, */*");

            copy_species_wms_layers = new HashMap<String, String[]>();

            int result = client.executeMethod(get);
            String slist = get.getResponseBodyAsString();

            System.out.println("****** species wms distributions ******");
            System.out.println(slist);

            if(slist != null && slist.length() > 0) {
                String [] lines = slist.split("\n");
                for(int i=0;i<lines.length;i++){
                    String [] words = lines[i].split(",");
                    copy_species_wms_layers.put(words[0], words[1].split("\t"));
                }
            }
        } catch (Exception e) {
            copy_species_wms_layers = null;
            e.printStackTrace();
        }
    }

    /**
     * returns array of WMS species requests
     */
    static public String [] getSpeciesDistributionWMS(String lsid) {
        return species_wms_layers.get(lsid);
    }
}
