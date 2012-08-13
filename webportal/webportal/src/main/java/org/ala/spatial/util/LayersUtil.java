package org.ala.spatial.util;

import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.menu.MapLayer;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Map.Entry;
import net.sf.json.JSONObject;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;

/**
 * joins active layer names to
 * - species names
 * - environmental layer names
 * - contextual layer names
 *
 * can maintain minimum distance between selected and remaining layers.
 *
 * used to autocomplete layer entries
 *
 * @author adam
 *
 */
    public class LayersUtil {

    public static final String LAYER_TYPE_PLAIN = "text/plain";
    public static final String LAYER_TYPE_CSV = "text/csv";
    public static final String LAYER_TYPE_KML = "application/vnd.google-earth.kml+xml";
    public static final String LAYER_TYPE_CSV_EXCEL = "text/x-comma-separated-values";
    public static final String LAYER_TYPE_EXCEL = "application/vnd.ms-excel";
    public static final String LAYER_TYPE_ZIP = "application/zip";

    /**
     * generate basic HTML for metadata of a WKT layer.
     * 
     * @param wkt as String
     * @return html as String
     */
    public static String getMetadataForWKT(String method, String wkt) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        String date = sdf.format(new Date());
        String area = String.format("%,d", (int) Util.calculateArea(wkt));
        String shortWkt = (wkt.length() > 300)? wkt.substring(0,300) + "...": wkt;

        return "Area metadata\n" + method + "<br>" + date + "<br>" + area + " sq km<br>" + shortWkt;
    }

    /**
     * generate basic HTML for metadata of a layer with a description.
     *
     * @param wkt as String
     * @return html as String
     */
    public static String getMetadata(String description) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        String date = sdf.format(new Date());
        return "Area metadata\n" + description + "<br>" + date;
    }

    /**
     * MapComposer for retrieving active layer names
     */
    MapComposer mc;
    /**
     * list of contextual layer names
     *
     * populated on first getContextualLayers call
     */
//    String[] contextualLayerNames = null;
    /**
     * list of environmental layer names
     *
     * populated on first getEnvironmentalLayers call
     */
//    String[] environmentalLayerNames = null;
    private static String[] commonTaxonRanks = new String[]{
        "cultivar",
        "superfamily",
        "subgenus",
        "unranked",
        "infrageneric",
        "subfamily",
        "subspecies",
        "section",
        "infraspecific",
        "hybrid",
        "variety",
        "form",
        "series",
        "tribe"
    };

    /**
     * constructor
     *
     * @param mc_ current MapComposer
     */
    public LayersUtil(MapComposer mc_) {
        mc = mc_;
    }

    /**
     * gets first species layer found from active layers list
     *
     * @return species name found as String or null if none found
     */
    public String getFirstSpeciesLayer() {
        List<MapLayer> activeLayers = mc.getPortalSession().getActiveLayers();
        for (MapLayer ml : activeLayers) {
            if (ml.isDisplayed() && ml.getData("query") != null) {
                return ml.getName();
            }
        }
        return null;
    }

//    /**
//     * gets whole list of environmental or contextual layers
//     * that appear in the active layers list
//     *
//     * @return list of layer names as String [] or null if none
//     * found
//     */
//    public String[] getActiveEnvCtxLayers() {
//        List<MapLayer> activeLayers = mc.getPortalSession().getActiveLayers();
//        ArrayList<String> layers = new ArrayList<String>();
//        for (MapLayer ml : activeLayers) {
//            if (isEnvCtxLayer(ml.getName())) {
//                layers.add(ml.getName());
//            }
//        }
//        if (layers.size() == 0) {
//            return null;
//        }
//        String[] ret = new String[layers.size()];
//        layers.toArray(ret);
//        return ret;
//    }

//    /**
//     * gets first environmental layer that appears in the
//     * active layers list
//     *
//     * @return name of first environmental layer as String or null
//     * if none found
//     */
//    public String getFirstEnvLayer() {
//        List<MapLayer> activeLayers = mc.getPortalSession().getActiveLayers();
//        for (MapLayer ml : activeLayers) {
//            if (isEnvLayer(ml.getName())) {
//                return ml.getName();
//            }
//        }
//        return null;
//    }

    /**
     * Check if the species is a pest species
     * @param lsid LSID of the species
     * @return
     */
    static public boolean isPestSpecies(String lsid) {
        //TODO: do this function when pestStatuses is available
        return false;
    /*
        String snUrl = CommonData.bieServer + "/species/" + lsid + ".json";

        try {
            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(snUrl);
            get.addRequestHeader("Content-type", "application/json");

            int result = client.executeMethod(get);
            String slist = get.getResponseBodyAsString();

            JSONObject jo = JSONObject.fromObject(slist);
            JSONArray pestStatuses = jo.getJSONArray("pestStatuses");
            JSONObject pstatus = pestStatuses.getJSONObject(0);


        } catch (Exception e) {
            System.out.println("Error checking if pest species ");
            e.printStackTrace(System.out);
        }


        return false;*/
    }

    /**
     * return first scientific name for lsid
     *
     * @param lsid LSID of the species
     * @return
     */
    static public String getScientificName(String lsid) {

        String snUrl = CommonData.bieServer + "/species/" + lsid + ".json";

        try {
            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(snUrl);
            get.addRequestHeader("Content-type", "application/json");

            int result = client.executeMethod(get);
            String slist = get.getResponseBodyAsString();

            JSONObject jo = JSONObject.fromObject(slist);
            String scientficName = jo.getJSONObject("taxonConcept").getString("nameString");
            return scientficName;
        } catch (Exception e) {
            System.out.println("Error getting scientific name");
            e.printStackTrace(System.out);
        }


        return null;
    }

    static public String getScientificNameRank(String lsid) {

        String snUrl = CommonData.bieServer + "/species/" + lsid + ".json";
        System.out.println(snUrl);

        try {
            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(snUrl);
            get.addRequestHeader("Content-type", "application/json");

            int result = client.executeMethod(get);
            String slist = get.getResponseBodyAsString();

            JSONObject jo = JSONObject.fromObject(slist);
            String scientficName = jo.getJSONObject("taxonConcept").getString("nameString");
            String rank = jo.getJSONObject("taxonConcept").getString("rankString");

            System.out.println("Arrays.binarySearch(commonTaxonRanks, rank): " + Arrays.binarySearch(commonTaxonRanks, rank));
            if (Arrays.binarySearch(commonTaxonRanks, rank) > -1) {
                rank = "taxon";
            }

            return scientficName + "," + rank;
        } catch (Exception e) {
            System.out.println("Error getting scientific name");
            e.printStackTrace(System.out);
        }


        return null;
    }

//    /**
//     * tests if a String is an environmental layer name
//     *
//     * uses SAT environmental list service call
//     *
//     * @param val text value to test as String
//     * @return true iff val is an exact match with an
//     * environmental layer name. Note that false is
//     * returned if there is error communicating with SAT
//     */
//    public boolean isEnvLayer(String val) {
//        String[] layers = getEnvironmentalLayers();
//        if (layers == null) {
//            return false;
//        }
//        val = val.trim();
//        for (String s : layers) {
//            if (s.equalsIgnoreCase(val)) {
//                return true;
//            }
//        }
//        return false;
//    }
//
//    /**
//     * tests if a String is a contextual layer name
//     *
//     * uses SAT contextual list service call
//     *
//     * @param val text value to test as String
//     * @return true iff val is an exact match with a
//     * contextual layer name. Note that false is
//     * returned if there is error communicating with SAT
//     */
//    private boolean isCtxLayer(String val) {
//        String[] layers = getContextualLayers();
//        if (layers == null) {
//            return false;
//        }
//        val = val.trim();
//        for (String s : layers) {
//            if (s.equalsIgnoreCase(val)) {
//                return true;
//            }
//        }
//        return false;
//    }
//
//    /**
//     * tests if a String is an environmental
//     * or contextual layer name
//     *
//     * @param val text value to test as String
//     * @return true iff val is an exact match with an
//     * environmental or a contextual layer name
//     */
//    public boolean isEnvCtxLayer(String val) {
//        return isEnvLayer(val) || isCtxLayer(val);
//    }
//
//    /**
//     * gets list of environmental layers from SAT server by
//     * layer name
//     *
//     * @return environmental layer names as String[] or null on error
//     */
//    public String[] getEnvironmentalLayers() {
//        /* return previously generated list if available */
//        if (environmentalLayerNames != null) {
//            return environmentalLayerNames;
//        }
//
//        environmentalLayerNames = CommonData.getEnvironmentalLayers();
//
//        return environmentalLayerNames;
//    }
//
//    /**
//     * gets list of contextual layers from SAT server by
//     * layer name
//     *
//     * @return contextual layer names as String[] or null on error
//     */
//    public String[] getContextualLayers() {
//        /* return previously generated list if available */
//        if (contextualLayerNames != null) {
//            return contextualLayerNames;
//        }
//
//        contextualLayerNames = CommonData.getContextualLayers();
//
//        return contextualLayerNames;
//    }

    public Entry<String, UserData> getUserData(String displayName) {
        //check against user uploaded records
        Hashtable<String, UserData> htUserSpecies = (Hashtable) mc.getSession().getAttribute("userpoints");
        if (htUserSpecies != null) {
            for (Entry<String, UserData> entry : htUserSpecies.entrySet()) {
                if (entry.getValue().getName().equalsIgnoreCase(displayName)) {
                    return entry;
                }
            }
        }

        return null;
    }
}
