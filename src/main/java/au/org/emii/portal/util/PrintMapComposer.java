package au.org.emii.portal.util;

import au.org.ala.spatial.util.CommonData;
import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.value.BoundingBox;
import org.ala.layers.util.SpatialUtil;
import org.apache.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.edit.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.xobject.PDJpeg;
import org.apache.pdfbox.pdmodel.graphics.xobject.PDXObjectImage;
import org.zkoss.spring.SpringUtil;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.io.*;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;

/**
 * Created by a on 12/05/2014.
 */
public class PrintMapComposer {
    final private static int MAX_WIDTH = 2080;
    final private static int MAX_HEIGHT = 2080;

    private static Logger logger = Logger.getLogger(PrintMapComposer.class);

    MapComposer mc;
    double [] extents;
    int [] windowSize;
    String comment;
    String outputType;

    double aspect_ratio;

    int width;
    int height;

    int dpi;

    double scale;

    public PrintMapComposer(MapComposer mc, double[] extents, int [] windowSize, String comment, String outputType) {
        this.mc = mc;
        this.extents = extents;
        this.windowSize = windowSize;

        //extents (epsg:3857) same as viewportarea (epsg:4326)
        BoundingBox bb = mc.getLeftmenuSearchComposer().getViewportBoundingBox();
        if (bb.getMaxLongitude() < bb.getMinLongitude() || bb.getMaxLongitude() > 180) {
            bb.setMaxLongitude(180);
        }
        if(bb.getMinLongitude() < -180) {
            bb.setMinLongitude(-180);
        }
        this.aspect_ratio = windowSize[0] / (double) windowSize[1];

        this.comment = comment;
        this.outputType = outputType;

        if (aspect_ratio > 1) {
            width = MAX_WIDTH;
            height = (int)(MAX_HEIGHT / aspect_ratio);
        } else {
            height = MAX_HEIGHT;
            width = (int)(MAX_WIDTH * aspect_ratio);
        }
        scale = width / (double) windowSize[0];

        dpi = 300;
    }

    public byte [] get() {
        BufferedImage map = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) map.getGraphics();
        g.setPaint(Color.white);
        g.fillRect(0,0,width,height);

        //base layer
        if (mc.getBaseMap().equalsIgnoreCase("normal")) {
            //google
            drawGoogle(g, "roadmap");
        } else if (mc.getBaseMap().equalsIgnoreCase("hybrid")) {
            //google hybrid
            drawGoogle(g, "hybrid");
        } else if (mc.getBaseMap().equalsIgnoreCase("minimal")) {
            //openstreetmap
            drawOSM(g);
        } else { //outline
           //world layer
           String uri = CommonData.geoServer + "/wms/reflect?LAYERS=ALA%3Aworld&SERVICE=WMS&VERSION=1.1.1&REQUEST=GetMap&STYLES=&FORMAT=image%2Fjpeg&SRS=EPSG%3A3857&DPI=" + dpi;
           drawUri(g, uri, 1, false);
        }

        //wms layers
        List<MapLayer> allLayers = mc.getPortalSession().getActiveLayers();
        for (int i = allLayers.size() - 1; i >= 0; i--) {
            if(!allLayers.get(i).getName().equalsIgnoreCase("Map options") && allLayers.get(i).isDisplayed()) {
                for(int j=allLayers.get(i).getChildCount()-1;j>=0;j--) {
                    if (allLayers.get(i).getChild(j).isDisplayed()) {
                        drawLayer(g, allLayers.get(i).getChild(j));
                    }
                }
                drawLayer(g, allLayers.get(i));
            }
        }

        //remove alpha and add user comment
        int fontSize = 30;
        String [] lines = comment.split("\n");
        int commentHeight = comment.length() > 0?(int)(fontSize * lines.length * 1.5):0;
        BufferedImage mapFlat = new BufferedImage(width, height + commentHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D gFlat = (Graphics2D) mapFlat.getGraphics();
        gFlat.setPaint(Color.white);
        gFlat.fillRect(0,0,width,height + commentHeight);
        gFlat.drawImage(map, 0, 0, width, height, Color.white, null);

        if (commentHeight > 0) {
            gFlat.setColor(Color.black);
            gFlat.setFont(new Font("Arial",Font.PLAIN,fontSize));
            int h = height + (int) (fontSize);
            for(int i=0;i<lines.length;i++) {
                gFlat.drawString(lines[i], 20, (int) (h + i * 1.5 * fontSize));
            }
        }

        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        try {
            if (outputType.equalsIgnoreCase("png")) {
                ImageIO.write(mapFlat, "png", bos);
            } else if (outputType.equalsIgnoreCase("jpg")){
                ImageIO.write(mapFlat, "jpg", bos);
            } else if (outputType.equalsIgnoreCase("pdf")) {
                ImageIO.write(mapFlat, "jpg", bos);
                bos.flush();

                return makePdfFromJpg(bos.toByteArray());
            }
            bos.flush();
        } catch (IOException e) {
            logger.error("failed output image", e);
        }

        return bos.toByteArray();
    }

    private void drawOSM(Graphics2D g) {
        double [] resolutions = {
            156543.03390625,
            78271.516953125,
            39135.7584765625,
            19567.87923828125,
            9783.939619140625,
            4891.9698095703125,
            2445.9849047851562,
            1222.9924523925781,
            611.4962261962891,
            305.74811309814453,
            152.87405654907226,
            76.43702827453613,
            38.218514137268066,
            19.109257068634033,
            9.554628534317017,
            4.777314267158508,
            2.388657133579254,
            1.194328566789627,
            0.5971642833948135};

        double [] origin = {-20037508.34, -20037508.34};

        //nearest resolution
        double actual_res = (extents[2] - extents[0]) / width;
        int res = 0;
        while(res < resolutions.length && resolutions[res] > actual_res) {
            res++;
        }
        if (res > 0) res--;

        int tileWidth = 256;
        int tileHeight = 256;
        int tiles = (int) Math.pow(2,res);

        int sx = (int) Math.floor((extents[0] - origin[0]) / resolutions[res] / tileWidth);
        int sy = tiles - (int) Math.ceil((extents[3] - origin[1])/ resolutions[res] / tileHeight);
        int mx = (int) Math.ceil((extents[2] - origin[0]) / resolutions[res] / tileWidth);
        int my = tiles - (int) Math.floor((extents[1] - origin[1])/ resolutions[res] / tileHeight);

        //if (mx >= tiles) mx = tiles - 1;  //for > 360 degrees longitude
        if (sx < 0) sx = 0;
        if (my < 0) my = 0;
        if (sy >= tiles) sy = tiles - 1;

        int destWidth = width;
        int destHeight = height;

        //square tiles
        int srcWidth = (int)(destWidth / (extents[2] - extents[0]) * (tileWidth * resolutions[res]));
        int srcHeight = srcWidth; //(int)(destHeight / (extents[3] - extents[1]) * (tileHeight * resolutions[res]));

        int xOffset = (int)((sx - ((extents[0] - origin[0]) / resolutions[res] / tileWidth))*srcWidth);
        int yOffset = (int)((sy - (((-1*origin[1]) - extents[3])/ resolutions[res] / tileHeight))*srcHeight);

        RescaleOp op = new RescaleOp(new float []{ 1f, 1f, 1f, 1f }, new float [] {0f,0f,0f,0f}, null);

        String uri = "http://tile.openstreetmap.org/";
        for(int iy = my;iy >= sy;iy--) {
            for(int ix = sx;ix <=mx; ix++) {
                String bbox = res + "/" + (ix % tiles) + "/" + iy + ".png";
                logger.debug("print uri: " + uri + bbox);
                BufferedImage img = getImage(uri + bbox);
                if(img != null) {
                    int nx = (ix - sx) * srcWidth + xOffset;
                    int ny = (iy - sy) * srcHeight + yOffset;
                    BufferedImage tmp = new BufferedImage(srcWidth, srcHeight, BufferedImage.TYPE_INT_ARGB);
                    tmp.getGraphics().drawImage(img,0,0,srcWidth,srcHeight, 0,0, tileWidth, tileHeight, null);
                    g.drawImage(tmp, op, nx, ny);
                }
            }
        }

    }

    private void drawGoogle(Graphics2D g, String maptype) {
        double [] resolutions = {
                156543.03390625,
                78271.516953125,
                39135.7584765625,
                19567.87923828125,
                9783.939619140625,
                4891.9698095703125,
                2445.9849047851562,
                1222.9924523925781,
                611.4962261962891,
                305.74811309814453,
                152.87405654907226,
                76.43702827453613,
                38.218514137268066,
                19.109257068634033,
                9.554628534317017,
                4.777314267158508,
                2.388657133579254,
                1.194328566789627,
                0.5971642833948135};

        double [] origin = {-20037508.34, -20037508.34};

        //nearest resolution
        int imgSize = 640;
        int gScale = 2;
        double actual_width = (extents[2] - extents[0]);
        double actual_height = (extents[3] - extents[1]);
        int res = 0;
        while(res < resolutions.length - 1 && resolutions[res + 1] * imgSize > actual_width
            && resolutions[res + 1] * imgSize > actual_height) {
            res++;
        }

        int centerX = (int)((extents[2] - extents[0]) / 2 + extents[0]);
        int centerY = (int)((extents[3] - extents[1]) / 2 + extents[1]);
        double latitude = SpatialUtil.convertMetersToLat(centerY);
        double longitude = SpatialUtil.convertMetersToLng(centerX);

        //need to change the size requested so the extents match the output extents.
        int imgWidth = (int)(((extents[2] - extents[0])) / resolutions[res]);
        int imgHeight = (int)(((extents[3] - extents[1])) / resolutions[res]);

        String uri = "http://maps.googleapis.com/maps/api/staticmap?";
        String parameters = "center=" + latitude + "," + longitude + "&zoom=" + res + "&scale=" + gScale + "&size=" + imgWidth + "x" + imgHeight + "&maptype=" + maptype;

        RescaleOp op = new RescaleOp(new float []{ 1f, 1f, 1f, 1f }, new float [] {0f,0f,0f,0f}, null);

        logger.debug("print uri: " + uri + parameters);
        BufferedImage img = getImage(uri + parameters);

        if(img != null) {
            BufferedImage tmp = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            tmp.getGraphics().drawImage(img,0,0,width, height, 0,0, imgWidth * gScale, imgHeight * gScale, null);

            g.drawImage(tmp, op, 0, 0);
        }
    }

    private byte[] makePdfFromJpg(byte[] bytes) {

        PDDocument doc = null;
        try{
          /* Step 1: Prepare the document.
           */
            doc = new PDDocument();
            PDPage page = new PDPage();
            doc.addPage(page);

         /* Step 2: Prepare the image
          * PDJpeg is the class you use when dealing with jpg images.
          * You will need to mention the jpg file and the document to which it is to be added
          * Note that if you complete these steps after the creating the content stream the PDF
          * file created will show "Out of memory" error.
          */

            PDXObjectImage image = null;
            image = new PDJpeg(doc, new ByteArrayInputStream(bytes));

         /* Create a content stream mentioning the document, the page in the dcoument where the content stream is to be added.
          * Note that this step has to be completed after the above two steps are complete.
          */
            PDPageContentStream content = new PDPageContentStream(doc, page);

       /* Step 3:
        * Add (draw) the image to the content stream mentioning the position where it should be drawn
        * and leaving the size of the image as it is
        */

            int scaledWidth = (int)page.getMediaBox().getWidth();
            int scaledHeight = (int)(scaledWidth / aspect_ratio);
            content.drawXObject(image,0,0,scaledWidth,scaledHeight);
            content.close();

         /* Step 4:
          * Save the document as a pdf file mentioning the name of the file
          */

            ByteArrayOutputStream bos = new ByteArrayOutputStream();

            doc.save(bos);

            return bos.toByteArray();

        } catch (Exception e){
            logger.error("failed to make pdf from jpg",e);
        }

        return null;
    }

    void drawLayer(Graphics2D g, MapLayer layer) {
        int oldSize = layer.getSizeVal();
        layer.setSizeVal((int)(oldSize * scale));
        String oldEnvParams = layer.getEnvParams();
        if (oldEnvParams != null) {
            layer.setEnvParams(oldEnvParams.replace(";size:" + oldSize + ";", ";size:" + layer.getSizeVal() + ";"));
        }

        //make the URL

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

        String uri = layer.getUri().replace("gwc/service/","") + params +
                        "&FORMAT=" + layer.getImageFormat() +
                        "&LAYERS=" + layer.getLayer() +
                        "&SRS=EPSG:3857" +
                        "&DPI=" + dpi;

        drawUri(g, uri, layer.getOpacity(), layer.getColourMode() != null && layer.getColourMode().equalsIgnoreCase("grid"));

        layer.setEnvParams(oldEnvParams);
        layer.setSizeVal(oldSize);
    }

    private void drawUri(Graphics2D g, String uri, float opacity, boolean imageOnly256) {
        //tiles
        double minX, maxX, minY, maxY, stepX, stepY;
        minX = extents[0];
        maxX = extents[2];
        minY = extents[1];
        maxY = extents[3];

        int pageWidth = width;
        int pageHeight = height;

        int tileWidth = 1024;
        int tileHeight = 1024;
        if(imageOnly256) {
            tileWidth = 256;
            tileHeight = 256;
        }
        int ix = 0;
        int iy = 0;

        stepX = (maxX - minX) / pageWidth * tileWidth;
        stepY = (maxY - minY) / pageHeight * tileHeight;

        RescaleOp op = new RescaleOp(new float []{ 1f, 1f, 1f, opacity }, new float [] {0f,0f,0f,0f}, null);

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
                    tmp.getGraphics().drawImage(img, 0, 0, null);
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
}
