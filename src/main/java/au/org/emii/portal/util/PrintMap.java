package au.org.emii.portal.util;

import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.value.BoundingBox;
import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream;
import org.ala.layers.util.SpatialUtil;
import org.apache.log4j.Logger;
import org.geotools.data.ows.Layer;
import org.geotools.data.ows.WMSCapabilities;
import org.geotools.data.wms.WebMapServer;
import org.geotools.data.wms.request.AbstractWMSRequest;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.map.MapContent;
import org.geotools.map.WMSLayer;
import org.geotools.renderer.lite.StreamingRenderer;
import org.geotools.styling.StyleFactory;
import org.mapfish.print.MapPrinter;
import org.opengis.filter.FilterFactory2;
import org.zkoss.spring.SpringUtil;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;

/**
 * Created by a on 12/05/2014.
 */
public class PrintMap {
    final private static String MAPFISH_CONFIG_PATH = "/data/webportal/config/mapfish-config.yaml";
    final private static Double A4_PORTRAIT_ASPECT_RATIO = 505.0 / 752.0; //full page is 595.0/842.0, footer is 30, comment is 30, scale is 30;
    final private static Double MAX_A4_WIDTH = 1595.0-10;
    final private static Double COMMENTS_BUFFER = 90.0;
    final private static Double MAX_A4_HEIGHT = 1842.0-10;


    private static Logger logger = Logger.getLogger(PrintMap.class);

    MapComposer mc;
    double [] extents;
    boolean landscape;
    String comment;
    String outputType;

    double aspect_ratio;
    String spec;

    int width;
    int height;

    public PrintMap(MapComposer mc, double[] extents, String comment, String outputType) {
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

    }

    public byte [] get() {
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

        BufferedImage map = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) map.getGraphics();
        g.setPaint(Color.white);
        g.fillRect(0,0,width,height);

        MapContent mapContent = new MapContent();

        //base layer

        //wms layers
        List<MapLayer> allLayers = mc.getPortalSession().getActiveLayers();
        for (int i = allLayers.size() - 1; i >= 0; i--) {
            if(!allLayers.get(i).getName().equalsIgnoreCase("Map options") && allLayers.get(i).isDisplayed()) {
                for(int j=allLayers.get(i).getChildCount()-1;j>=0;j--) {
                    if (allLayers.get(i).getChild(j).isDisplayed()) {
                        WMSLayer l = getWMSLayer(allLayers.get(i).getChild(j));
                        if(l != null) {
                            mapContent.addLayer(l);
                        }
                    }
                }
                WMSLayer l = getWMSLayer(allLayers.get(i));
                if(l != null) {
                    mapContent.addLayer(l);
                }
            }
        }

        ByteOutputStream bos = new ByteOutputStream();

        try {
            ImageIO.write(map, "png", bos);
            bos.flush();
        } catch (IOException e) {
            logger.error("failed output image", e);
        }

        return bos.getBytes();
    }

    WMSLayer getWMSLayer(MapLayer layer) {

       /* WebMapServer wms = getWMS(url);
        WMSCapabilities capabilities = getCapabilities(url);

        //gets the top most layer, which will contain all the others
        Layer rootLayer = capabilities.getLayer();

        //gets all the layers in a flat list, in the order they appear in
        //the capabilities document (so the rootLayer is at index 0)
        List layers = capabilities.getLayerList();

        for(int i=0;i<layers.size();i++) {
            Layer l = (Layer) layers.get(i);
            if(l.getName() != null && l.getName().equalsIgnoreCase(layer.getLayer())) {
                WMSLayer wmsLayer = new WMSLayer(wms, l);
                if(wmsLayer != null) {
                    //set parameters
                    WMSLayerExt wmsLayerExt = new WMSLayerExt(wmsLayer);
                }
            }
        }
*/
        return null;
    }

    void drawLayer(Graphics2D g, MapLayer layer) {
        //make the URL
        String url = layer.getUri();

       /* mapcontent.addLayer(getWMSLayer(layer));

        StreamingRenderer sr = new StreamingRenderer();
        sr.setMapContent(mapcontent);
*/


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

        int dpi = 127;

        String uri = layer.getUri().replace("gwc/service/","") + params +
                        "&FORMAT=" + layer.getImageFormat() +
                        "&LAYERS=" + layer.getLayer() +
                        "&SRS=EPSG:3857" +
                        "&DPI=" + dpi;

        //tiles

        double minX, maxX, minY, maxY, stepX, stepY;
        minX = extents[0];
        maxX = extents[2];
        minY = extents[1];
        maxY = extents[3];

        int pageWidth = width;
        int pageHeight = height;

        int tileWidth = 512;
        int tileHeight = 512;
        int ix = 0;
        int iy = 0;

        stepX = (maxX - minX) / pageWidth * tileWidth;
        stepY = (maxY - minY) / pageHeight * tileHeight;

        RescaleOp op = new RescaleOp(new float []{ 1f, 1f, 1f, layer.getOpacity() }, new float [] {0f,0f,0f,0f}, null);

        iy = 0;
        for(double y = maxY;y>minY;y-=stepY, iy++) {
            ix = 0;
            for(double x = minX;x<maxX;x+=stepX, ix++) {
                String bbox = "&BBOX=" + x + "," + (y - stepY) + "," + (x+stepX) + "," + y + "&WIDTH=" + tileWidth + "&HEIGHT=" + tileHeight + "&TRANSPARENT=true";
                logger.debug("print uri: " + uri + bbox);
                BufferedImage img = getImage(uri + bbox);
                if(img != null) {
                    int nx = ix * tileWidth;
                    int ny = iy * tileHeight;
                    BufferedImage tmp = new BufferedImage(tileWidth, tileHeight, BufferedImage.TYPE_INT_ARGB);
                    tmp.getGraphics().drawImage(img,0,0,null);
                    g.drawImage(tmp, op, nx, ny);
                }
            }
        }
    }

    private BufferedImage getImage(String path) {
        try {
            BufferedImage img = ImageIO.read(new URL(path));

            return img;
        } catch (Exception e) {
            logger.error("failed to get image at: " + path);
        }
        return null;
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
