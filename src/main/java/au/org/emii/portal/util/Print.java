package au.org.emii.portal.util;

import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.value.BoundingBox;
import com.lowagie.text.DocumentException;
import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream;
import org.ala.layers.util.SpatialUtil;
import org.apache.log4j.Logger;
import org.mapfish.print.MapPrinter;
import org.mapfish.print.config.ConfigFactory;
import org.mapfish.print.output.OutputFormat;
import org.mapfish.print.utils.PJsonObject;
import org.springframework.context.ApplicationContext;
import org.zkoss.spring.SpringUtil;

import javax.servlet.ServletException;
import java.io.*;
import java.net.URLEncoder;
import java.util.*;

/**
 * Created by a on 12/05/2014.
 */
public class Print {
    final private static String MAPFISH_CONFIG_PATH = "/data/webportal/config/mapfish-config.yaml";
    final private static Double A4_PORTRAIT_ASPECT_RATIO = 505.0 / 752.0; //full page is 595.0/842.0, footer is 30, comment is 30, scale is 30;
    final private static Double MAX_A4_WIDTH = 595.0-10;
    final private static Double COMMENTS_BUFFER = 90.0;
    final private static Double MAX_A4_HEIGHT = 842.0-10;


    private static Logger logger = Logger.getLogger(Print.class);

    MapComposer mc;
    double [] extents;
    boolean landscape;
    String comment;
    String outputType;

    double aspect_ratio;
    String spec;

    public Print(MapComposer mc, double [] extents, String comment, String outputType) {
        this.mc = mc;
        this.extents = extents;

        //extents (epsg:3857) same as viewportarea (epsg:4326)
        BoundingBox bb = mc.getLeftmenuSearchComposer().getViewportBoundingBox();
        if (bb.getMaxLongitude() < bb.getMinLongitude() || bb.getMaxLongitude() > 180) {
            bb.setMaxLongitude(180);
        }
        if(bb.getMinLongitude() < -180) {
            bb.setMinLongitude(-180);
        }
        this.aspect_ratio = Math.abs((SpatialUtil.convertLngToPixel(bb.getMaxLongitude()) - SpatialUtil.convertLngToPixel(bb.getMinLongitude())) /
                (double) (SpatialUtil.convertLatToPixel(bb.getMaxLatitude()) - SpatialUtil.convertLatToPixel(bb.getMinLatitude())));
        this.landscape = aspect_ratio > A4_PORTRAIT_ASPECT_RATIO;
        this.comment = comment;
        this.outputType = outputType;

        //do coordinates need fixing?
        if (extents[2] < extents[0] || extents[2] >= 20037508.34) {
            extents[2] = 20037508.34 - 1;
        }
        if (extents[0] <= -20037508.34) {
            extents[0] = -20037508.34 + 1;
        }

        makeSpec();
    }

    void makeSpec() {
        String pages = "[{bbox:[" + extents[0] + "," + extents[1] + "," + extents[2] + "," + extents[3] + "]}]";

        String layers = "[";

        //base layer first, then iterate through other layers
        List<MapLayer> allLayers = mc.getPortalSession().getActiveLayers();
        for (int i = -1; i < allLayers.size(); i++) {
            MapLayer layer = null;
            String base = "";
            if (i == -1) {
                //base layer
                base = mc.getBaseMap();
            } else {
                layer = allLayers.get(i);
            }
            if(base.length() > 0 || layer.isDisplayed()) {
                if(base.equalsIgnoreCase("minimal")) {
                    if(layers.length() > 1) {
                        layers = layers + ",";
                    }
                    layers = layers +
                            "{\"baseURL\":\"http://tile.openstreetmap.org/\",\"opacity\":1,\"singleTile\":false,\"type\":\"OSM\",\"maxExtent\":[-20037508.34,-20037508.34,20037508.34,20037508.34],\"tileSize\":[256,256],\"extension\":\"png\",\"resolutions\":[156543.03390625,78271.516953125,39135.7584765625,19567.87923828125,9783.939619140625,4891.9698095703125,2445.9849047851562,1222.9924523925781,611.4962261962891,305.74811309814453,152.87405654907226,76.43702827453613,38.218514137268066,19.109257068634033,9.554628534317017,4.777314267158508,2.388657133579254,1.194328566789627,0.5971642833948135]}";
                } else if(base.equalsIgnoreCase("hybrid") ){
                    if(layers.length() > 1) {
                        layers = layers + ",";
                    }
                    String google_map_type = "hybrid";
                    layers = layers + "{        	\"baseURL\": \"http://maps.google.com/maps/api/staticmap\",        	\"type\" : \"TiledGoogle\",        	\"maxExtent\": [-20037508.3392,-20037508.3392,20037508.3392,20037508.3392],        	\"tileSize\": [256,256],        	\"resolutions\": [156543.0339,78271.51695,39135.758475,19567.8792375,9783.93961875,4891.969809375,2445.9849046875,1222.99245234375,611.496226171875,305.7481130859375,152.87405654296876,76.43702827148438,38.21851413574219,19.109257067871095,9.554628533935547,4.777314266967774,2.388657133483887,1.1943285667419434,0.5971642833709717],        	\"extension\" : \"png\",        	\"format\" : \"png32\",        	\"sensor\": \"false\",        	\"language\": \"english\",   	\"maptype\": \"" + google_map_type + "\"}";
                } else if(base.equalsIgnoreCase("normal")) {
                    if(layers.length() > 1) {
                        layers = layers + ",";
                    }
                    String google_map_type = "roadmap";
                    layers = layers + "{        	\"baseURL\": \"http://maps.google.com/maps/api/staticmap\",        	\"type\" : \"TiledGoogle\",        	\"maxExtent\": [-20037508.3392,-20037508.3392,20037508.3392,20037508.3392],        	\"tileSize\": [256,256],        	\"resolutions\": [156543.0339,78271.51695,39135.758475,19567.8792375,9783.93961875,4891.969809375,2445.9849046875,1222.99245234375,611.496226171875,305.7481130859375,152.87405654296876,76.43702827148438,38.21851413574219,19.109257067871095,9.554628533935547,4.777314266967774,2.388657133483887,1.1943285667419434,0.5971642833709717],        	\"extension\" : \"png\",        	\"format\" : \"png32\",        	\"sensor\": \"false\",        	\"language\": \"english\",   	\"maptype\": \"" + google_map_type + "\"}";
                } else if (!layer.getName().equalsIgnoreCase("Map options")){
                    if(layers.length() > 1) {
                        layers = layers + ",";
                    }
                    String dynamic_style = "";
                    if (layer.isPolygonLayer()) {
                        String colour = Integer.toHexString((0xFF0000 & (layer.getRedVal() << 16)) | (0x00FF00 & layer.getGreenVal() << 8) | (0x0000FF & layer.getBlueVal()));
                        while (colour.length() < 6) {
                            colour = "0" + colour;
                        }
                        String filter;
                        /*
                            two types of areas are displayed as WMS.
                            1. environmental envelopes. these are backed by a grid file.
                            2. layerdb, objects table, areas referenced by a pid.  these are geometries.
                         */
                        if (layer.getUri().contains("ALA:envelope")) {
                            filter = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><StyledLayerDescriptor xmlns=\"http://www.opengis.net/sld\">"
                                    + "<NamedLayer><Name>" + mc.getOpenLayersJavascript().getLayerUtilities().getLayer(layer.getUri()) + "</Name>"
                                    + "<UserStyle><FeatureTypeStyle><Rule><RasterSymbolizer><Geometry></Geometry>"
                                    + "<ColorMap>"
                                    + "<ColorMapEntry color=\"#ffffff\" opacity=\"0\" quantity=\"0\"/>"
                                    + "<ColorMapEntry color=\"#" + colour + "\" opacity=\"1\" quantity=\"1\" />"
                                    + "</ColorMap></RasterSymbolizer></Rule></FeatureTypeStyle></UserStyle></NamedLayer></StyledLayerDescriptor>";

                        } else {
                            filter = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><StyledLayerDescriptor version=\"1.0.0\" xmlns=\"http://www.opengis.net/sld\">"
                                    + "<NamedLayer><Name>" + mc.getOpenLayersJavascript().getLayerUtilities().getLayer(layer.getUri()) + "</Name>"
                                    + "<UserStyle><FeatureTypeStyle><Rule><Title>Polygon</Title><PolygonSymbolizer><Fill>"
                                    + "<CssParameter name=\"fill\">#" + colour + "</CssParameter></Fill>"
                                    + "</PolygonSymbolizer></Rule></FeatureTypeStyle></UserStyle></NamedLayer></StyledLayerDescriptor>";
                        }
                        try {
                            dynamic_style = "&sld_body=" + URLEncoder.encode(filter, "UTF-8");
                        } catch (Exception e) {
                            logger.debug("invalid filter sld", e);
                        }
                    }

                    String params = "&SRS=EPSG:3857" +
                            "&FORMAT=" + layer.getImageFormat() +
                            "&LAYERS=" + layer.getLayer() +
                            "&REQUEST=GetMap" +
                            "&SERVICE=WMS" +
                            "&VERSION=" + mc.getOpenLayersJavascript().getLayerUtilities().getWmsVersion(layer) +
                            dynamic_style;

                    if (!Validate.empty(layer.getCql())) {
                        params = params + "&CQL_FILTER=" + layer.getCql();
                    }
                    if (!Validate.empty(layer.getEnvParams())) {
                        try {
                            params += "&ENV=" + URLEncoder.encode(layer.getEnvParams().replace("'", "\\'"), "UTF-8");
                        } catch (UnsupportedEncodingException e) {
                            logger.error("failed to encode env params : " + layer.getEnvParams().replace("'", "\\'"), e);
                        }
                    }

                    layers = layers + "{type: \"WMS\",baseURL: \"" + layer.getUri().replace("gwc/service/","") + params + "\"," +
                            "format: \"" + "image/png" + "\"," +
                            "opacity: " + layer.getOpacity() + "," +
                            "layers:[\"" + layer.getLayer() + "\"]," +
                            "singleTile: false}";
                }
            }
        }
        layers = layers + "]";

        int height,width;
        if(landscape) {
            width = MAX_A4_HEIGHT.intValue();
            height = (int)(width / aspect_ratio);
            if(height > MAX_A4_WIDTH - COMMENTS_BUFFER) {
                height = (int)(MAX_A4_WIDTH.intValue() - COMMENTS_BUFFER);
                width = (int)(height * aspect_ratio);
            }
        } else {
            height = (int)(MAX_A4_HEIGHT - COMMENTS_BUFFER);
            width = (int)(height / aspect_ratio);
            if(width > MAX_A4_WIDTH) {
                width = MAX_A4_WIDTH.intValue();
                height = (int)(width * aspect_ratio);
            }
        }

         spec = "{\"dpi\":127,\"units\":\"m\"," +
                "\"srs\": \"EPSG:3857\",\"geodetic\":\"false\"," +
                "\"layout\":\"" + (landscape?"A4 landscape":"A4 portrait") + "\"," +
                "\"outputFormat\":\"" + outputType + "\"," +
                "\"comment\":\"" + comment.replace("\"","\'") + "\"," +
                "\"height\":" + height + "," +
                "\"width\":" + width + "," +
                "\"layers\": " + layers + ",\"pages\": " + pages + "}";

        logger.debug("print spec: " + spec);
    }

    public byte [] get() {

        PJsonObject specJson = MapPrinter.parseSpec(spec);

        MapPrinter mapPrinter = getMapPrinter();

        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Referer",mc.getSettingsSupplementary().getProperty("webportal_url"));

        ByteOutputStream out = new ByteOutputStream();

        try {
            mapPrinter.print(specJson, out, headers);
        } catch (Exception e) {
            logger.error("failed to print map: " + spec, e);
        }

        return out.getBytes();
    }

    MapPrinter getMapPrinter() {
        MapPrinter printer = null;
        File configFile = new File(MAPFISH_CONFIG_PATH);

        try {
            logger.info("Loading configuration file: " + configFile.getAbsolutePath());
            printer = SpringUtil.getApplicationContext().getBean(MapPrinter.class);
            printer.setYamlConfigFile(configFile);
        } catch (FileNotFoundException e) {
            logger.error("Cannot read configuration file: " + MAPFISH_CONFIG_PATH, e);
        } catch (Throwable e) {
            logger.error("Error occurred while reading configuration file '" + configFile + "': " + e );
        }

        return printer;
    }
}
