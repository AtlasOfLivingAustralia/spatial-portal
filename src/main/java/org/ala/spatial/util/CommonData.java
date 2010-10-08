/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
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

    //(2) for EnvironmentalList
    static ArrayList<ListEntry> listEntriesAll;
    static String[] layerNamesAll;
    static ArrayList<ListEntry> listEntriesEnv;
    static String[] layerNamesEnv;
    static float[][] distances;

    //(3) for layer list json
    static String layerlist = null;
    static JSONArray layerlistJSON = null;

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
        
        //(1) for LayersUtil
        getEnvironmentalLayers();
        getContextualLayers();

        //(2) for EnvironmentalList
        initEnvironmentalOnlyList();
        initEnvironmentalAllList();

        //(3) for layer list json
        getLayerList();
        initContextualClasses();
    }

    /**
     * gets list of environmental layers from SAT server by
     * layer name
     *
     * @return environmental layer names as String[] or null on error
     */
    static public String[] getEnvironmentalLayers() {
        /* return previously generated list if available */
        if (environmentalLayerNames == null) {
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
                e.printStackTrace(System.out);
            }

            /* retain list for future calls */
            environmentalLayerNames = aslist;
        }

        return environmentalLayerNames;
    }

    /**
     * gets list of contextual layers from SAT server by
     * layer name
     *
     * @return contextual layer names as String[] or null on error
     */
    static public String[] getContextualLayers() {
        /* return previously generated list if available */
        if (contextualLayerNames == null) {
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
                e.printStackTrace(System.out);
            }

            /* retain for future calls */
            contextualLayerNames = aslist;
        }

        return contextualLayerNames;
    }
   
    static public ArrayList<ListEntry> getListEntriesAll(){
        return listEntriesAll;
    }
    static public String[] getLayerNamesAll(){
        return layerNamesAll;
    }
    static public ArrayList<ListEntry> getListEntriesEnv(){
        return listEntriesEnv;
    }
    static public String[] getLayerNamesEnv(){
        return layerNamesEnv;
    }
    static public float[][] getDistances(){
        return distances;
    }

    static public void initEnvironmentalOnlyList() {
        try{
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
            layerNamesEnv = new String[rows.length]; //last row is empty
            distances = new float[rows.length - 1][rows.length - 1];

            String[] line = rows[0].split(",");
            layerNamesEnv[0] = line[1];
            for (int i = 1; i < rows.length; i++) {   //last row is empty
                line = rows[i].split(",");
                layerNamesEnv[i] = line[0];
                for (int j = 1; j < line.length; j++) {
                    try {
                        distances[i - 1][j - 1] = Float.parseFloat(line[j]);
                    } catch (Exception e) {
                    }
                }
            }

            listEntriesEnv = new ArrayList<ListEntry>();
            initLayerCatagories(listEntriesEnv, true);

            //match up listEntries
            for (int i = 0; i < listEntriesEnv.size(); i++) {
                String entryName = listEntriesEnv.get(i).name;
                for (int j = 0; j < layerNamesEnv.length; j++) {
                    if (layerNamesEnv[j].equalsIgnoreCase(entryName)) {
                        listEntriesEnv.get(i).row_in_distances = j;
                        break;
                    }
                }

                //remove if missing
                if (listEntriesEnv.get(i).row_in_distances < 0) {
                    //        System.out.println("absent from layers assoc mx: " + listEntries.get(i).name);
                    listEntriesEnv.remove(i);
                    i--;
                } else {
                    listEntriesEnv.get(i).row_in_list = i;
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    static void initLayerCatagories(ArrayList<ListEntry> listEntries, boolean environmentalOnly) {
        try {            
            String llist = getLayerList();
            
            JSONArray layerlist = getLayerListJSONArray();
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
                String displayname = jo.getString("displayname");
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
                int c = e1.catagory1.compareTo(e2.catagory1);
                if (c == 0) {
                    c = e1.catagory2.compareTo(e2.catagory2);
                    if (c == 0) {
                        c = e1.displayname.compareTo(e2.displayname);
                    }
                }
                return c;
            }
        });
    }

    static public void initEnvironmentalAllList() {
        //contextual and environmental
        try {
            String[] ctx = getContextualLayers();
            String[] env = getEnvironmentalLayers();

            layerNamesAll = new String[ctx.length + env.length];
            for (int i = 0; i < env.length; i++) {
                layerNamesAll[i] = env[i];
            }
            for (int i = 0; i < ctx.length; i++) {
                layerNamesAll[i + env.length] = ctx[i];
            }

            listEntriesAll = new ArrayList<ListEntry>();
            initLayerCatagories(listEntriesAll, false);

            //match up listEntries
            for (int i = 0; i < listEntriesAll.size(); i++) {
                String entryName = listEntriesAll.get(i).name;
                for (int j = 0; j < layerNamesAll.length; j++) {
                    if (layerNamesAll[j].equalsIgnoreCase(entryName)) {
                        listEntriesAll.get(i).row_in_distances = j;
                        break;
                    }
                }

                //remove if missing
                if (listEntriesAll.get(i).row_in_distances < 0) {
                    //        System.out.println("absent from layers assoc mx: " + listEntries.get(i).name);
                    listEntriesAll.remove(i);
                    i--;
                } else {
                    listEntriesAll.get(i).row_in_list = i;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getLayerList() {
        if(layerlist == null){
            try{
                String layersListURL = satServer + "/alaspatial/ws/layers/list";
                HttpClient client = new HttpClient();
                GetMethod get = new GetMethod(layersListURL);
                get.addRequestHeader("Accept", "application/json, text/javascript, */*");

                int result = client.executeMethod(get);
                layerlist = get.getResponseBodyAsString();
            }catch(Exception e){
                e.printStackTrace();
            }
        }
        return layerlist;
    }


    static public JSONArray getLayerListJSONArray(){
        if(layerlistJSON == null){
            layerlistJSON = JSONArray.fromObject(getLayerList());
        }
        return layerlistJSON;
    }

    static HashMap<JSONObject, List> contextualClasses = null;
    static private ArrayList empty = new ArrayList();

    static public List getContextualClasses(JSONObject layer) {
        if(contextualClasses == null){
            initContextualClasses();
        }
        return contextualClasses.get(layer);
    }

    static void initContextualClasses(){
        contextualClasses = new HashMap<JSONObject, List>();

        for (int i = 0; i < layerlistJSON.size(); i++) {
            JSONObject jo = layerlistJSON.getJSONObject(i);

            if (!jo.getBoolean("enabled")) {
                continue;
            }

            List classNodes = new ArrayList();
            if (jo.getString("type").equalsIgnoreCase("Contextual")) {
                classNodes = getContextualClassesInit(jo);
                contextualClasses.put(jo, classNodes);
            }
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

            //JSONObject joClasses = JSONObject.fromObject(classes);
            // System.out.println("CLASSES JSON:" + classes);
            classes = classes.replace("\"", "").replace("{", "").replace("}", "");

            String classAttribute = classes.split(":")[0];
            classList = Arrays.asList(classes.split(":")[1].split(","));
            // System.out.println("KEY:" + classAttribute);
            // classList = Arrays.asList((jo.getString(classAttribute)).split(","));

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
            System.out.println("Failure to get contextual classes.");
            return classNodes;
        }
    }
}
