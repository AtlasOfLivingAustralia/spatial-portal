package au.org.emii.portal.util;

import au.org.ala.spatial.StringConstants;
import au.org.ala.spatial.util.CommonData;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.value.BoundingBox;
import org.ala.layers.util.SpatialUtil;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.edit.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.xobject.PDJpeg;
import org.apache.pdfbox.pdmodel.graphics.xobject.PDXObjectImage;
import org.hsqldb.lib.tar.TarFileOutputStream;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.RescaleOp;
import java.io.*;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Created by a on 12/05/2014.
 */
public class PrintMapComposer {
    //HIGH_RES=(approx A4 600dpi) is only available for 'Outline' basemap
    private static final int MAX_WIDTH_LOW_RES = 2080;
    private static final int MAX_HEIGHT_LOW_RES = 2080;
    private static final int MAX_WIDTH_HIGH_RES = 7016;
    private static final int MAX_HEIGHT_HIGH_RES = 4960;
    private static final int DPI_HIGH_RES = 600;
    private static final int DPI_LOW_RES = 200;

    private static final Logger LOGGER = Logger.getLogger(PrintMapComposer.class);

    //Object is [Long, Integer, BufferedImage] where Long is last access time and Integer is size of BufferedImage
    private static final Map<String, Object[]> IMAGE_CACHE = new ConcurrentHashMap<String, Object[]>();
    private static Long imageCacheSize = 0L;
    private static Long maxImageCacheSize = 100000000L;
    private String baseMap;
    private double[] extents;
    private int[] windowSize;
    private String comment;
    private String outputType;
    private double aspectRatio;
    private int width;
    private int height;
    private int dpi;
    private double scale;
    private List<MapLayer> mapLayers;
    private LayerUtilities layerUtilities;

    //uses MapComposer information
    public PrintMapComposer(String baseMap, List<MapLayer> mapLayers, BoundingBox bb, double[] extents, int[] windowSize, String comment, String outputType, int resolution) {
        layerUtilities = new LayerUtilitiesImpl();
        this.mapLayers = new ArrayList(mapLayers);
        this.baseMap = baseMap;

        this.extents = extents == null ? null : extents.clone();
        this.windowSize = windowSize == null ? null : windowSize.clone();

        //extents (epsg:3857) same as viewportarea (epsg:4326)
        if (bb.getMaxLongitude() < bb.getMinLongitude() || bb.getMaxLongitude() > 180) {
            bb.setMaxLongitude(180);
        }
        if (bb.getMinLongitude() < -180) {
            bb.setMinLongitude(-180);
        }
        this.aspectRatio = this.windowSize[0] / (double) this.windowSize[1];

        //if aspect ratio is odd, attempt to calc it from extents.
        if (this.aspectRatio > 10 || this.aspectRatio < 0.1) {
            //TODO: when an error occurs because of windowSize, make sure extents are not 'bad' as well
            LOGGER.error("bad aspect ratio, windowSize = " + this.windowSize[0] + ", " + this.windowSize[1]
                    + ", extents = " + this.extents[0] + " " + this.extents[1] + " " + this.extents[2] + " " + this.extents[3]);

            this.windowSize[0] = SpatialUtil.convertLngToPixel(bb.getMaxLongitude())
                    - SpatialUtil.convertLngToPixel(bb.getMinLongitude());
            this.windowSize[1] = SpatialUtil.convertLatToPixel(bb.getMaxLatitude())
                    - SpatialUtil.convertLatToPixel(bb.getMinLatitude());

            this.aspectRatio = this.windowSize[0] / (double) this.windowSize[1];
        }

        this.comment = comment;
        this.outputType = outputType;

        int w = (resolution == 1 && "outline".equalsIgnoreCase(baseMap)) ? MAX_WIDTH_HIGH_RES : MAX_WIDTH_LOW_RES;
        int h = (resolution == 1 && "outline".equalsIgnoreCase(baseMap)) ? MAX_HEIGHT_HIGH_RES : MAX_HEIGHT_LOW_RES;
        if (aspectRatio > w / (double) h) {
            width = w;
            height = (int) (w / aspectRatio);
        } else {
            height = h;
            width = (int) (h * aspectRatio);
        }
        scale = width / (double) this.windowSize[0];

        dpi = (resolution == 1 && "outline".equalsIgnoreCase(baseMap)) ? DPI_HIGH_RES : DPI_LOW_RES;
    }

    //extents are in 4326
    public PrintMapComposer(double[] bbox, String baseMap, MapLayer[] mapLayers, double aspectRatio, String comment, String type, int resolution) {
        this.layerUtilities = new LayerUtilitiesImpl();
        this.mapLayers = Arrays.asList(mapLayers);
        this.baseMap = baseMap;
        this.aspectRatio = aspectRatio;

        this.extents = new double[]{
                SpatialUtil.convertLngToMeters(bbox[0])
                , SpatialUtil.convertLatToMeters(bbox[1])
                , SpatialUtil.convertLngToMeters(bbox[2])
                , SpatialUtil.convertLatToMeters(bbox[3])
        };

        //bbox is (epsg:4326)
        windowSize = new int[2];
        windowSize[0] = SpatialUtil.convertLngToPixel(bbox[2]) - SpatialUtil.convertLngToPixel(bbox[0]);
        windowSize[1] = SpatialUtil.convertLatToPixel(bbox[1]) - SpatialUtil.convertLatToPixel(bbox[3]);

        this.comment = comment;
        this.outputType = type;

        int w = (resolution == 1 && "outline".equalsIgnoreCase(baseMap)) ? MAX_WIDTH_HIGH_RES : MAX_WIDTH_LOW_RES;
        int h = (resolution == 1 && "outline".equalsIgnoreCase(baseMap)) ? MAX_HEIGHT_HIGH_RES : MAX_HEIGHT_LOW_RES;
        if (aspectRatio < windowSize[0] / (double) windowSize[1]) {
            width = w;
            height = (int) (w / aspectRatio);

            //adjust extents[1] and extents[3]
            double mid = (windowSize[1]) / 2.0 + SpatialUtil.convertLatToPixel(bbox[3]);

            //adjust windowSize[1]
            windowSize[1] = (int) (windowSize[0] / aspectRatio);

            double half = windowSize[1] / 2.0;
            extents[1] = SpatialUtil.convertLatToMeters(SpatialUtil.convertPixelToLat((int) (mid + half)));
            extents[3] = SpatialUtil.convertLatToMeters(SpatialUtil.convertPixelToLat((int) (mid - half)));

        } else {
            height = h;
            width = (int) (h * aspectRatio);

            //adjust extents[0] and extents[2]
            double mid = (windowSize[0]) / 2.0 + SpatialUtil.convertLngToPixel(bbox[0]);

            //adjust windowSize[0]
            windowSize[0] = (int) (windowSize[1] * aspectRatio);

            double half = windowSize[0] / 2.0;
            extents[0] = SpatialUtil.convertLngToMeters(SpatialUtil.convertPixelToLng((int) (mid - half)));
            extents[2] = SpatialUtil.convertLngToMeters(SpatialUtil.convertPixelToLng((int) (mid + half)));

        }
        scale = 1;

        dpi = (resolution == 1 && "outline".equalsIgnoreCase(baseMap)) ? DPI_HIGH_RES : DPI_LOW_RES;
    }


    private List drawTiles(Graphics2D g, boolean drawTiles) {
        //returns when all tiles are retrieved
        List urls = new ArrayList();

        //base layer
        if ("normal".equalsIgnoreCase(baseMap)) {
            //google
            urls.addAll(drawGoogle(g, "roadmap", drawTiles));
        } else if ("hybrid".equalsIgnoreCase(baseMap)) {
            //google hybrid
            urls.addAll(drawGoogle(g, "hybrid", drawTiles));
        } else if ("minimal".equalsIgnoreCase(baseMap)) {
            //openstreetmap
            urls.addAll(drawOSM(g, drawTiles));
        } else {
            //outline
            //world layer
            String uri = CommonData.getGeoServer() + "/wms/reflect?LAYERS=ALA%3Aworld&SERVICE=WMS&VERSION=1.1.1&REQUEST=GetMap&STYLES=&FORMAT=image%2Fjpeg&SRS=EPSG%3A3857&DPI=" + dpi;
            urls.addAll(drawUri(g, uri, 1, false, drawTiles));
        }

        //wms layers
        for (int i = mapLayers.size() - 1; i >= 0; i--) {
            if (!"Map options".equalsIgnoreCase(mapLayers.get(i).getName())
                    && mapLayers.get(i).isDisplayed()) {
                for (int j = mapLayers.get(i).getChildCount() - 1; j >= 0; j--) {
                    if (mapLayers.get(i).getChild(j).isDisplayed()) {
                        urls.addAll(drawLayer(g, mapLayers.get(i).getChild(j), drawTiles));
                    }
                }
                urls.addAll(drawLayer(g, mapLayers.get(i), drawTiles));
            }
        }


        return urls;
    }

    public void fetchTiles(Graphics2D g) {
        List list = drawTiles(g, false);

        class FileCacheUrl implements Callable<String> {
            String url;
            Map map;

            public FileCacheUrl(String url){
                this.url = url;
            }

            @Override
            public String call() throws Exception {
                String cacheFilename = getCacheFilename(url);
                File file = new File(cacheFilename);

                if (!file.exists()) {
                    try {
                        HttpClient client = new HttpClient();
                        GetMethod get = new GetMethod(url);

                        client.executeMethod(get);

                        //construct cache filename\
                        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(cacheFilename));
                        BufferedInputStream bis = new BufferedInputStream(get.getResponseBodyAsStream());
                        byte[] bytes = new byte[1024];
                        int n;
                        while ((n = bis.read(bytes)) > 0) {
                            bos.write(bytes, 0, n);
                        }
                        bos.close();
                        bis.close();
                    } catch (Exception e) {
                        LOGGER.error("failed to get image at url: " + url + ", or write to file failed for: " + getCacheFilename(url), e);
                    }
                }

                return null;
            }
        }

        List fileCacheUrls = new ArrayList();
        for (Object o : list) {
            fileCacheUrls.add(new FileCacheUrl((String) o));
        }

        int NUMBER_OF_GET_IMAGE_THREADS = 4;    //best not keep it at 4 unless updating code to 4 per site
        ExecutorService executorService = Executors.newFixedThreadPool(NUMBER_OF_GET_IMAGE_THREADS);

        try {
            executorService.invokeAll(fileCacheUrls);
        } catch (InterruptedException e) {
            LOGGER.error("getting image urls interrupted", e);
        }
    }

    private String getCacheFilename(String url) {
        String hash = url.hashCode() + "";
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] b = md.digest(url.getBytes());
            hash = "";
            for (byte i : b) {
                hash += i;
            }
        } catch (NoSuchAlgorithmException e) {
            LOGGER.error("failed to use MD5 as filename");
        }
        return "/data/webportal/cache/" + hash;
    }


    public byte[] get() {
        BufferedImage map = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) map.getGraphics();
        g.setPaint(Color.white);
        g.fillRect(0, 0, width, height);

        //fetch tiles > 1 at a time
        fetchTiles(g);

        //draw tiles
        drawTiles(g, true);

        //remove alpha and add user comment
        int fontSize = 30;
        String[] lines = comment.split("\n");
        int commentHeight = comment.length() > 0 ? (int) (fontSize * lines.length * 1.5) : 0;
        BufferedImage mapFlat = new BufferedImage(width, height + commentHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D gFlat = (Graphics2D) mapFlat.getGraphics();
        gFlat.setPaint(Color.white);
        gFlat.fillRect(0, 0, width, height + commentHeight);
        gFlat.drawImage(map, 0, 0, width, height, Color.white, null);

        if (commentHeight > 0) {
            gFlat.setColor(Color.black);
            gFlat.setFont(new Font("Arial", Font.PLAIN, fontSize));
            int h = height + fontSize;
            for (int i = 0; i < lines.length; i++) {
                gFlat.drawString(lines[i], 20, (int) (h + i * 1.5 * fontSize));
            }
        }

        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        try {
            if ("png".equalsIgnoreCase(outputType)) {
                ImageIO.write(mapFlat, "png", bos);
            } else if ("jpg".equalsIgnoreCase(outputType)) {
                ImageIO.write(mapFlat, "jpg", bos);
            } else if ("pdf".equalsIgnoreCase(outputType)) {
                ImageIO.write(mapFlat, "jpg", bos);
                bos.flush();

                return makePdfFromJpg(bos.toByteArray());
            }
            bos.flush();
        } catch (IOException e) {
            LOGGER.error("failed output image", e);
        }

        return bos.toByteArray();
    }

    private List drawOSM(Graphics2D g, boolean drawTiles) {

        List imageUrls = new ArrayList();

        double[] resolutions = {
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

        double[] origin = {-20037508.34, -20037508.34};

        //nearest resolution
        double actualRes = (extents[2] - extents[0]) / width;
        int res = 0;
        while (res < resolutions.length && resolutions[res] > actualRes) {
            res++;
        }
        if (res > 0) {
            res--;
        }

        int tileWidth = 256;
        int tileHeight = 256;
        int tiles = (int) Math.pow(2, res);

        int sx = (int) Math.floor((extents[0] - origin[0]) / resolutions[res] / tileWidth);
        int sy = tiles - (int) Math.ceil((extents[3] - origin[1]) / resolutions[res] / tileHeight);
        int mx = (int) Math.ceil((extents[2] - origin[0]) / resolutions[res] / tileWidth);
        int my = tiles - (int) Math.floor((extents[1] - origin[1]) / resolutions[res] / tileHeight);

        if (sx < 0) {
            sx = 0;
        }
        if (my < 0) {
            my = 0;
        }
        if (sy >= tiles) {
            sy = tiles - 1;
        }

        int destWidth = width;

        //square tiles
        int srcWidth = (int) (destWidth / (extents[2] - extents[0]) * (tileWidth * resolutions[res]));
        int srcHeight = srcWidth;

        int xOffset = (int) ((sx - ((extents[0] - origin[0]) / resolutions[res] / tileWidth)) * srcWidth);
        int yOffset = (int) ((sy - (((-1 * origin[1]) - extents[3]) / resolutions[res] / tileHeight)) * srcHeight);

        RescaleOp op = new RescaleOp(new float[]{1f, 1f, 1f, 1f}, new float[]{0f, 0f, 0f, 0f}, null);

        String uri = "http://tile.openstreetmap.org/";
        for (int iy = my; iy >= sy; iy--) {
            for (int ix = sx; ix <= mx; ix++) {
                String bbox = res + "/" + (ix % tiles) + "/" + iy + ".png";
                LOGGER.debug("print uri: " + uri + bbox);

                imageUrls.add(uri + bbox);

                if (drawTiles) {
                    BufferedImage img = getImage(uri + bbox, true);
                    if (img != null) {
                        int nx = (ix - sx) * srcWidth + xOffset;
                        int ny = (iy - sy) * srcHeight + yOffset;
                        BufferedImage tmp = new BufferedImage(srcWidth, srcHeight, BufferedImage.TYPE_INT_ARGB);
                        tmp.getGraphics().drawImage(img, 0, 0, srcWidth, srcHeight, 0, 0, tileWidth, tileHeight, null);
                        g.drawImage(tmp, op, nx, ny);
                    }
                }
            }
        }

        return imageUrls;
    }

    private List drawGoogle(Graphics2D g, String maptype, boolean drawTiles) {

        List imageUrls = new ArrayList();

        double[] resolutions = {
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

        //nearest resolution
        int imgSize = 640;
        int gScale = 2;
        double actualWidth = extents[2] - extents[0];
        double actualHeight = extents[3] - extents[1];
        int res = 0;
        while (res < resolutions.length - 1 && resolutions[res + 1] * imgSize > actualWidth
                && resolutions[res + 1] * imgSize > actualHeight) {
            res++;
        }

        int centerX = (int) ((extents[2] - extents[0]) / 2 + extents[0]);
        int centerY = (int) ((extents[3] - extents[1]) / 2 + extents[1]);
        double latitude = SpatialUtil.convertMetersToLat(centerY);
        double longitude = SpatialUtil.convertMetersToLng(centerX);

        //need to change the size requested so the extents match the output extents.
        int imgWidth = (int) ((extents[2] - extents[0]) / resolutions[res]);
        int imgHeight = (int) ((extents[3] - extents[1]) / resolutions[res]);

        String uri = "http://maps.googleapis.com/maps/api/staticmap?";
        String parameters = "center=" + latitude + "," + longitude + "&zoom=" + res + "&scale=" + gScale + "&size=" + imgWidth + "x" + imgHeight + "&maptype=" + maptype;

        RescaleOp op = new RescaleOp(new float[]{1f, 1f, 1f, 1f}, new float[]{0f, 0f, 0f, 0f}, null);

        LOGGER.debug("print uri: " + uri + parameters);

        imageUrls.add(uri + parameters);

        if (drawTiles) {
            BufferedImage img = getImage(uri + parameters, true);

            if (img != null) {
                BufferedImage tmp = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                tmp.getGraphics().drawImage(img, 0, 0, width, height, 0, 0, imgWidth * gScale, imgHeight * gScale, null);

                g.drawImage(tmp, op, 0, 0);
            }
        }

        return imageUrls;
    }

    private byte[] makePdfFromJpg(byte[] bytes) {

        PDDocument doc;
        try {
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

            PDXObjectImage image;
            image = new PDJpeg(doc, new ByteArrayInputStream(bytes));

         /* Create a content stream mentioning the document, the page in the dcoument where the content stream is to be added.
          * Note that this step has to be completed after the above two steps are complete.
          */
            PDPageContentStream content = new PDPageContentStream(doc, page);

       /* Step 3:
        * Add (draw) the image to the content stream mentioning the position where it should be drawn
        * and leaving the size of the image as it is
        */

            int scaledWidth = (int) page.getMediaBox().getWidth();
            int scaledHeight = (int) (scaledWidth / aspectRatio);
            content.drawXObject(image, 0, 0, scaledWidth, scaledHeight);
            content.close();

         /* Step 4:
          * Save the document as a pdf file mentioning the name of the file
          */

            ByteArrayOutputStream bos = new ByteArrayOutputStream();

            doc.save(bos);

            return bos.toByteArray();

        } catch (Exception e) {
            LOGGER.error("failed to make pdf from jpg", e);
        }

        return new byte[0];
    }

    private List drawLayer(Graphics2D g, MapLayer layer, boolean drawTiles) {

        List imageUrls = new ArrayList();

        int oldSize = layer.getSizeVal();
        layer.setSizeVal((int) (oldSize * scale));
        String oldEnvParams = layer.getEnvParams();
        if (oldEnvParams != null) {
            layer.setEnvParams(oldEnvParams.replace(";size:" + oldSize + ";", ";size:" + layer.getSizeVal() + ";"));
        }

        //make the URL

        String dynamicStyle = "";
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
                        + "<NamedLayer><Name>" + layerUtilities.getLayer(layer.getUri()) + "</Name>"
                        + "<UserStyle><FeatureTypeStyle><Rule><RasterSymbolizer><Geometry></Geometry>"
                        + "<ColorMap>"
                        + "<ColorMapEntry color=\"#ffffff\" opacity=\"0\" quantity=\"0\"/>"
                        + "<ColorMapEntry color=\"#" + colour + "\" opacity=\"1\" quantity=\"1\" />"
                        + "</ColorMap></RasterSymbolizer></Rule></FeatureTypeStyle></UserStyle></NamedLayer></StyledLayerDescriptor>";

            } else if (layer.getColourMode() != null && "hatching".equals(layer.getColourMode())) {
                filter = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><StyledLayerDescriptor version=\"1.0.0\" xmlns=\"http://www.opengis.net/sld\">"
                        + "<NamedLayer><Name>" + layerUtilities.getLayer(layer.getUri()) + "</Name>"
                        + "<UserStyle><FeatureTypeStyle><Rule><Title>Polygon</Title><PolygonSymbolizer>"
                        + "<Stroke>"
                        + "<CssParameter name=\"stroke\">#" + colour + "</CssParameter>"
                        + "<CssParameter name=\"stroke-width\">4</CssParameter>"
                        + "</Stroke>"
                        + "<Fill>"
                        + "<GraphicFill><Graphic><Mark><WellKnownName>shape://times</WellKnownName><Stroke>"
                        + "<CssParameter name=\"stroke\">#" + colour + "</CssParameter>"
                        + "<CssParameter name=\"stroke-width\">1</CssParameter>"
                        + "</Stroke></Mark></Graphic></GraphicFill>"
                        + "</Fill>"
                        + "</PolygonSymbolizer></Rule></FeatureTypeStyle></UserStyle></NamedLayer></StyledLayerDescriptor>";
            } else {
                filter = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><StyledLayerDescriptor version=\"1.0.0\" xmlns=\"http://www.opengis.net/sld\">"
                        + "<NamedLayer><Name>" + layerUtilities.getLayer(layer.getUri()) + "</Name>"
                        + "<UserStyle><FeatureTypeStyle><Rule><Title>Polygon</Title><PolygonSymbolizer><Fill>"
                        + "<CssParameter name=\"fill\">#" + colour + "</CssParameter></Fill>"
                        + "</PolygonSymbolizer></Rule></FeatureTypeStyle></UserStyle></NamedLayer></StyledLayerDescriptor>";
            }
            try {
                if (filter.startsWith("&")) {
                    //use as-is
                    dynamicStyle = filter;
                } else {
                    dynamicStyle = "&sld_body=" + URLEncoder.encode(filter, StringConstants.UTF_8);
                }
            } catch (Exception e) {
                LOGGER.debug("invalid filter sld", e);
            }
        }

        if (layer.getColourMode() != null && layer.getColourMode().startsWith("&")) {
            dynamicStyle = layer.getColourMode();
        }

        String params = "&SRS=EPSG:3857" +
                "&FORMAT=" + layer.getImageFormat() +
                "&LAYERS=" + layer.getLayer() +
                "&REQUEST=GetMap" +
                "&SERVICE=WMS" +
                "&VERSION=" + layerUtilities.getWmsVersion(layer) +
                dynamicStyle;

        if (!Validate.empty(layer.getCql())) {
            params = params + "&CQL_FILTER=" + layer.getCql();
        }
        if (!Validate.empty(layer.getEnvParams())) {
            try {
                params += "&ENV=" + URLEncoder.encode(URLEncoder.encode(layer.getEnvParams().replace("'", "\\'"), StringConstants.UTF_8), StringConstants.UTF_8);
            } catch (UnsupportedEncodingException e) {
                LOGGER.error("failed to encode env params : " + layer.getEnvParams().replace("'", "\\'"), e);
            }
        }

        String uri = layer.getUri().replace("gwc/service/", "") + params +
                "&FORMAT=" + layer.getImageFormat() +
                "&LAYERS=" + layer.getLayer() +
                "&SRS=EPSG:3857" +
                "&DPI=" + dpi;

        imageUrls.addAll(drawUri(g, uri, layer.getOpacity(), layer.getColourMode() != null && layer.getColourMode().equalsIgnoreCase(StringConstants.GRID), drawTiles));

        layer.setEnvParams(oldEnvParams);
        layer.setSizeVal(oldSize);

        return imageUrls;
    }

    private List drawUri(Graphics2D g, String uri, float opacity, boolean imageOnly256, boolean drawTiles) {

        List imageUrls = new ArrayList();

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
        if (imageOnly256) {
            tileWidth = 256;
            tileHeight = 256;
        }
        int ix;
        int iy;

        stepX = (maxX - minX) / pageWidth * tileWidth;
        stepY = (maxY - minY) / pageHeight * tileHeight;

        RescaleOp op = new RescaleOp(new float[]{1f, 1f, 1f, opacity}, new float[]{0f, 0f, 0f, 0f}, null);

        iy = 0;
        for (double y = maxY; y > minY; y -= stepY, iy++) {
            ix = 0;
            for (double x = minX; x < maxX; x += stepX, ix++) {
                String bbox = "&BBOX=" + x + "," + (y - stepY) + "," + (x + stepX) + "," + y + "&WIDTH=" + tileWidth + "&HEIGHT=" + tileHeight + "&TRANSPARENT=true";
                LOGGER.debug("print uri: " + uri + bbox);

                imageUrls.add(uri + bbox);

                if (drawTiles) {
                    BufferedImage img = getImage(uri + bbox, true);
                    if (img != null) {
                        int nx = ix * tileWidth;
                        int ny = iy * tileHeight;

                        BufferedImage tmp = new BufferedImage(tileWidth, tileHeight, BufferedImage.TYPE_INT_ARGB);
                        tmp.getGraphics().drawImage(img, 0, 0, null);
                        g.drawImage(tmp, op, nx, ny);

                    }
                }
            }
        }

        return imageUrls;
    }

    private BufferedImage getImage(String path, boolean useCache) {
        //fix for empty style in the request
        String pth = path.replace("&styles=&", "&");

        if (useCache) {
            synchronized (IMAGE_CACHE) {
                //using disk cache as prepared by the ExecutorService
                String filename = getCacheFilename(path);
                File file = new File(filename);

                if (file.exists()) {
                    try {
                        return ImageIO.read(file);
                    } catch (IOException e) {
                        LOGGER.error("failed to read cache image: " + filename, e);
                    }
                }
            }
        }

        try {
            BufferedImage img = ImageIO.read(new URL(pth));

            return img;
        } catch (Exception e) {
            LOGGER.error("failed to get image at: " + pth);
        }
        return null;
    }
}
