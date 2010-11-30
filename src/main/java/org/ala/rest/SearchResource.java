package org.ala.rest;

import com.thoughtworks.xstream.XStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import org.geoserver.rest.AbstractResource;
import org.geoserver.rest.format.DataFormat;
import org.geoserver.rest.format.StringFormat;
import org.geotools.util.logging.Logging;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Representation;

/**
 * Search resource used for querying the gazetteer.
 * @author Angus
 */
public class SearchResource extends AbstractResource {//ReflectiveResource {

    private static final Logger logger = Logging.getLogger("org.ala.rest.SearchResource");

    @Override
    protected List<DataFormat> createSupportedFormats(Request request, Response response) {

        List<DataFormat> formats = new ArrayList();
        formats.add(new StringFormat(MediaType.APPLICATION_XML));
        return formats;
    }

    /***
     * Handles the get request for a gazetteer search. Responds with a list of search results
     */
    @Override
    public void handleGet() {
        XStream xstream = new XStream();
        DataFormat format = getFormatGet();

        logger.info(getRequest().getAttributes().toString());

        String q = "";
        String lon = "";
        String lat = "";
        int radius = 0;
        String wkt = "";
        String layer = "";
        String layers = "";
       


        if (getRequest().getAttributes().containsKey("q")) {
            String[] pieces = getRequest().getAttributes().get("q").toString().split("&");
            logger.finer("We have " + pieces.length + " search components.");
            for (String get_param : pieces) {
                if (get_param.contains("q=")) {
                    q = get_param.replace("q=", "");
                    logger.finer("q is " + q);
                }
                if (get_param.contains("lat=")) {
                    lat = get_param.replace("lat=", "");
                    logger.finer("lat is " + lat);
                }
                if (get_param.contains("lon=")) {
                    lon = get_param.replace("lon=", "");
                    logger.finer("lon is " + lon);
                }
                if (get_param.contains("radius=")) {
                    radius = new Integer(get_param.replace("radius=", "")).intValue();
                    logger.finer("radius is " + radius);
                }
                if (get_param.contains("layer=")) {
                    layer = get_param.replace("layer=", "");
                    logger.finer("layer is " + layer);
                }
                if (get_param.contains("layers=")) {
                    layers = get_param.replace("layers=", "");
                    logger.finer("layers are " + layers);
                }
            }
        }
        //doing a cacheable point in poly request eg. /layer/latlon/lat,lon
        else if ((getRequest().getAttributes().containsKey("latlon"))) {
            //point search without query params
             String latlon =  getRequest().getAttributes().get("latlon").toString();
              lat = latlon.split(",")[0];
               lon = latlon.split(",")[1];
             PointSearch searchObj;
            if (getRequest().getAttributes().containsKey("layer")) {
                //search the specified layer
               layer = getRequest().getAttributes().get("layer").toString();
                searchObj = new PointSearch(lon, lat, 0, layer); //0 Radius means not using radius?
                
            } else {
                //search the default layers
                searchObj = new PointSearch(lon, lat, 0);
            }
            xstream.processAnnotations(PointSearch.class);
            String xmlString = xstream.toXML(searchObj);
            Date d = new Date();
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.YEAR, 10);
            d = cal.getTime();
            Representation rep = format.toRepresentation(xmlString);
            rep.setExpirationDate(d);
            getResponse().setEntity(rep);

            return;
        }

        String[] layers_arr = getLayers(layer, layers);

        //normal search query
        if (q.compareTo("") != 0) {
            Search searchObj;
            if (layers_arr.length > 0) {
                searchObj = new Search(q.replace("+", "* AND ") + "*", layers_arr);
            } else {
                //we need to search default layers ...
                GazetteerConfig gc = new GazetteerConfig();
                searchObj = new Search(q.replace("+", "* AND ") + "*", gc.getDefaultLayerNames().toArray(new String[gc.getDefaultLayerNames().size()]));
            }
            xstream.processAnnotations(Search.class);
            String xmlString = xstream.toXML(searchObj);
            getResponse().setEntity(format.toRepresentation(xmlString));
        } //point search
        else if ((lat.compareTo("") != 0) && (lon.compareTo("") != 0)) {
            PointSearch searchObj;
            if (layers_arr.length > 0) {
                searchObj = new PointSearch(lon, lat, radius, layers_arr);
            } else {
                searchObj = new PointSearch(lon, lat, radius);
            }
            xstream.processAnnotations(PointSearch.class);
            String xmlString = xstream.toXML(searchObj);
            getResponse().setEntity(format.toRepresentation(xmlString));
        }
    }

    /**
     * Helper method to merge and parse layer and layers into an array of strings
     * Also check to see if they are valid layers, and checks for layer aliases
     * @param layer
     * @param Layers
     * @return
     */
    public String[] getLayers(String layer, String layers) {
        GazetteerConfig gc = new GazetteerConfig();
        ArrayList<String> layer_al = new ArrayList<String>();
        if (layers.compareTo("") != 0) {
            String[] layers_a = layers.split(",");
            for (String layer_str : layers_a) {
                if (gc.layerNameExists(layer_str)) {
                    layer_al.add(layer_str);
                } else {
                    String layer_a = gc.getNameFromAlias(layer_str);
                    if (layer_a.compareTo("") != 0) {
                        layer_al.add(layer_a);
                    } else {
                        logger.info("Layer " + layer + " layer name or alias does not exist, ignoring.");
                    }
                }

            }
        }
        if (layer.compareTo("") != 0) {
            if (gc.layerNameExists(layer)) {
                layer_al.add(layer);
            } else {
                String layer_a = gc.getNameFromAlias(layer);
                if (layer_a.compareTo("") != 0) {
                    layer_al.add(layer_a);
                } else {
                    logger.info("Layer " + layer + " layer name or alias does not exist, ignoring.");
                }
            }
        }
        for (String string : layer_al) {
            logger.info(string);
        }
        return layer_al.toArray(new String[layer_al.size()]);
    }
}
