/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.wms;

import au.org.emii.portal.config.ConfigurationLoaderStage1;
import au.org.emii.portal.util.SessionPrint;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.ala.spatial.data.Facet;
import org.ala.spatial.data.Legend;
import org.ala.spatial.data.QueryField;
import org.ala.spatial.util.CommonData;
import org.ala.spatial.util.Util;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 *
 * @author Adam
 */
@Controller
public class WMSService {
    final static int HIGHLIGHT_RADIUS = 3;

    /*
    http://spatial.ala.org.au/geoserver/wms/reflect?styles=&format=image/png&
    layers=ALA:occurrences&transparent=true&
    CQL_FILTER=speciesconceptid='urn:lsid:biodiversity.org.au:afd.taxon:cd149740-87b2-4da2-96dc-e1aa1f693438'&SRS=EPSG%3A900913&
    ENV=color%3A1dd183%3Bname%3Acircle%3Bsize%3A8%3Bopacity%3A0.8&
    VERSION=1.1.0&
    SERVICE=WMS&REQUEST=GetMap&
    EXCEPTIONS=application%2Fvnd.ogc.se_inimage&
    BBOX=15654303.7292,-1408886.9659,15810846.7631,-1252343.932&
    WIDTH=256&
    HEIGHT=256
     *
     */

    @RequestMapping(value = "/wms/reflect", method = RequestMethod.GET)
    public void getPointsMap(
            @RequestParam(value = "CQL_FILTER", required = false, defaultValue = "") String cql_filter,
            @RequestParam(value = "ENV", required = false, defaultValue = "") String env,
            @RequestParam(value = "BBOX", required = false, defaultValue = "") String bboxString,
            @RequestParam(value = "WIDTH", required = false, defaultValue = "") String widthString,
            @RequestParam(value = "HEIGHT", required = false, defaultValue = "") String heightString,
            HttpServletRequest request, HttpServletResponse response) {

        response.setHeader("Cache-Control", "max-age=86400"); //age == 1 day
        response.setContentType("image/png"); //only png images generated

        int width = 256, height = 256;
        try {
            width = Integer.parseInt(widthString);
            height = Integer.parseInt(heightString);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            env = URLDecoder.decode(env, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(WMSService.class.getName()).log(Level.SEVERE, null, ex);
        }
        int red = 0, green = 0, blue = 0, alpha = 0;
        String name = "circle";
        int size = 4;
        boolean uncertainty = false;
        String highlight = null;
        String colourMode = null;
        for (String s : env.split(";")) {
            String[] pair = s.split(":");
            if (pair[0].equals("color")) {
                while (pair[1].length() < 6) {
                    pair[1] = "0" + pair[1];
                }
                red = Integer.parseInt(pair[1].substring(0, 2), 16);
                green = Integer.parseInt(pair[1].substring(2, 4), 16);
                blue = Integer.parseInt(pair[1].substring(4), 16);
            } else if (pair[0].equals("name")) {
                name = pair[1];
            } else if (pair[0].equals("size")) {
                size = Integer.parseInt(pair[1]);
            } else if (pair[0].equals("opacity")) {
                alpha = (int) (255 * Double.parseDouble(pair[1]));
//            } else if (pair[0].equals("uncertainty")) {
//                uncertainty = true;
            } else if (pair[0].equals("sel")) {
                try {
                    highlight = URLDecoder.decode(s.substring(4),"UTF-8").replace("%3B",";");
                } catch (Exception e) {}
            } else if (pair[0].equals("colormode")) {
                colourMode = pair[1];
            }
        }

        double[] bbox = new double[4];
        int i;
        i = 0;
        for (String s : bboxString.split(",")) {
            try {
                bbox[i] = Double.parseDouble(s);
                i++;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        try {

            //adjust bbox extents with half pixel width/height
            double pixelWidth = (bbox[2] - bbox[0]) / width;
            double pixelHeight = (bbox[3] - bbox[1]) / height;
            bbox[0] += pixelWidth / 2;
            bbox[2] -= pixelWidth / 2;
            bbox[1] += pixelHeight / 2;
            bbox[3] -= pixelHeight / 2;

            //offset for points bounding box by size
            double xoffset = (bbox[2] - bbox[0]) / (double) width * (size + (highlight!=null?HIGHLIGHT_RADIUS*2 + size * 0.2:0) + 5);
            double yoffset = (bbox[3] - bbox[1]) / (double) height * (size + (highlight!=null?HIGHLIGHT_RADIUS*2 + size * 0.2:0) + 5);

            //check offset for points bb by maximum uncertainty (?? 30k ??)
            if (uncertainty) {
                double xuoffset = 30000;
                double yuoffset = 30000;
                if (xoffset < xuoffset) {
                    xoffset = xuoffset;
                }
                if (yoffset < yuoffset) {
                    yoffset = yuoffset;
                }
            }

            //adjust offset for pixel height/width
            xoffset += pixelWidth;
            yoffset += pixelHeight;

            double[][] bb = {{Utils.convertMetersToLng(bbox[0] - xoffset), Utils.convertMetersToLat(bbox[1] - yoffset)}, {Utils.convertMetersToLng(bbox[2] + xoffset), Utils.convertMetersToLat(bbox[3] + yoffset)}};

            double[] pbbox = new double[4]; //pixel bounding box
            pbbox[0] = Utils.convertLngToPixel(Utils.convertMetersToLng(bbox[0]));
            pbbox[1] = Utils.convertLatToPixel(Utils.convertMetersToLat(bbox[1]));
            pbbox[2] = Utils.convertLngToPixel(Utils.convertMetersToLng(bbox[2]));
            pbbox[3] = Utils.convertLatToPixel(Utils.convertMetersToLat(bbox[3]));

            String lsid = null;
            int p1 = 0;
            int p2 = cql_filter.indexOf('&', p1 + 1);
            if (p2 < 0) {
                p2 = cql_filter.indexOf(';', p1 + 1);
            }
            if (p2 < 0) {
                p2 = cql_filter.length();
            }
            if (p1 >= 0) {
                lsid = cql_filter.substring(0, p2);
            }

            double[] points = null;
            ArrayList<QueryField> listHighlight = null;
            QueryField colours = null;
            double[] pointsBB = null;
            Facet facet = null;
            String[] facetFields = null;
            if (highlight != null && !(colourMode != null && colourMode.equals("grid"))) {
                facet = Facet.parseFacet(highlight);
                facetFields = facet.getFields();
                listHighlight = new ArrayList<QueryField>();
            }
            if (lsid != null) {
                Object[] data = (Object[]) RecordsLookup.getData(lsid);
                points = (double[]) data[0];
                pointsBB = (double[]) data[2];

                if (points == null || points.length == 0
                        || pointsBB[0] > bb[1][0] || pointsBB[2] < bb[0][0]
                        || pointsBB[1] > bb[1][1] || pointsBB[3] < bb[0][1]) {
                    setImageBlank(response);
                    return;
                }

                ArrayList<QueryField> fields = (ArrayList<QueryField>) data[1];

                for (int j = 0; j < fields.size(); j++) {
                    if (facet != null) {
                        for (int k = 0; k < facetFields.length; k++) {
                            if (facetFields[k].equals(fields.get(j).getName())) {
                                listHighlight.add(fields.get(j));
                            }
                        }
                    }
                    if (colourMode != null) {
                        if (fields.get(j).getName().equals(colourMode)) {
                            colours = fields.get(j);
                        }
                    }
                }
            }

            /* TODO: make this a copy instead of create */
            BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = (Graphics2D) img.getGraphics();
            g.setColor(new Color(0, 0, 0, 0));
            g.fillRect(0, 0, width, height);

            g.setColor(new Color(red, green, blue, alpha));
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int x, y;
            int pointWidth = size * 2 + 1;
            double width_mult = (width / (pbbox[2] - pbbox[0]));
            double height_mult = (height / (pbbox[1] - pbbox[3]));

            if (colourMode != null && colourMode.equals("grid")) {
                int divs = 16;
                double grid_width_mult = (width / (pbbox[2] - pbbox[0])) / (256 / divs);
                double grid_height_mult = (height / (pbbox[1] - pbbox[3])) / (256 / divs);
                int[][] gridCounts = new int[divs][divs];
                for (i = 0; i < points.length; i += 2) {
                    x = (int) ((Utils.convertLngToPixel(points[i]) - pbbox[0]) * grid_width_mult);
                    y = (int) ((Utils.convertLatToPixel(points[i + 1]) - pbbox[3]) * grid_height_mult);
                    if (x >= 0 && x < divs && y >= 0 && y < divs) {
                        gridCounts[x][y]++;
                    }
                }
                int xstep = 256 / divs;
                int ystep = 256 / divs;
                for (x = 0; x < divs; x++) {
                    for (y = 0; y < divs; y++) {
                        int v = gridCounts[x][y];
                        if (v > 0) {
                            if (v > 500) {
                                v = 500;
                            }
                            int colour = Legend.getLinearColour(v, 0, 500, 0xFFFFFF00, 0xFFFF0000);
                            g.setColor(new Color(colour));
                            g.fillRect(x * xstep, y * ystep, xstep, ystep);
                        }
                    }
                }
            } else {
                //circle type
                if (name.equals("circle")) {
                    if (colours == null) {
                        for (i = 0; i < points.length; i += 2) {
                            if (points[i] >= bb[0][0] && points[i] <= bb[1][0]
                                    && points[i + 1] >= bb[0][1] && points[i + 1] <= bb[1][1]) {
                                x = (int) ((Utils.convertLngToPixel(points[i]) - pbbox[0]) * width_mult);
                                y = (int) ((Utils.convertLatToPixel(points[i + 1]) - pbbox[3]) * height_mult);
                                g.fillOval(x - size, y - size, pointWidth, pointWidth);
                            }
                        }
                    } else {
                        int prevColour = -1;    //!= colours[0]
                        g.setColor(new Color(prevColour));
                        for (i = 0; i < points.length; i += 2) {
                            if (points[i] >= bb[0][0] && points[i] <= bb[1][0]
                                    && points[i + 1] >= bb[0][1] && points[i + 1] <= bb[1][1]) {
                                int thisColour = colours.getColour(i / 2);
                                if (thisColour != prevColour) {
                                    g.setColor(new Color(thisColour));
                                    prevColour = thisColour;
                                }
                                x = (int) ((Utils.convertLngToPixel(points[i]) - pbbox[0]) * width_mult);
                                y = (int) ((Utils.convertLatToPixel(points[i + 1]) - pbbox[3]) * height_mult);
                                g.fillOval(x - size, y - size, pointWidth, pointWidth);
                            }
                        }
                    }
                }

                if (highlight != null && facet != null) {
                    g.setStroke(new BasicStroke(2));
                    g.setColor(new Color(255, 0, 0, 255));
                    int sz = size + HIGHLIGHT_RADIUS;
                    int w = sz * 2 + 1;
                    for (i = 0; i < points.length; i += 2) {
                        if (points[i] >= bb[0][0] && points[i] <= bb[1][0]
                                && points[i + 1] >= bb[0][1] && points[i + 1] <= bb[1][1]) {
                            if (facet.isValid(listHighlight, i / 2)) {
                                x = (int) ((Utils.convertLngToPixel(points[i]) - pbbox[0]) * width_mult);
                                y = (int) ((Utils.convertLatToPixel(points[i + 1]) - pbbox[3]) * height_mult);
                                g.drawOval(x - sz, y - sz, w, w);
                            }
                        }
                    }
                }
            }

            g.dispose();

            try {
                OutputStream os = response.getOutputStream();
                ImageIO.write(img, "png", os);
                os.flush();
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        //System.out.println("[wms tile: " + (System.currentTimeMillis() - start) + "ms]");
    }
    //256x256 transparent image
    static Object blankImageObject = new Object();
    static byte[] blankImageBytes = null;

    private void setImageBlank(HttpServletResponse response) {
        if (blankImageBytes == null && blankImageObject != null) {
            synchronized (blankImageObject) {
                if (blankImageBytes == null) {
                    try {
                        RandomAccessFile raf = new RandomAccessFile(WMSService.class.getResource("/blank.png").getFile(), "r");
                        blankImageBytes = new byte[(int) raf.length()];
                        raf.read(blankImageBytes);
                        raf.close();
                    } catch (IOException ex) {
                        Logger.getLogger(WMSService.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }
        if (blankImageObject != null) {
            response.setContentType("image/png");
            try {
                ServletOutputStream outStream = response.getOutputStream();
                outStream.write(blankImageBytes);
                outStream.flush();
                outStream.close();
            } catch (IOException ex) {
                Logger.getLogger(WMSService.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @RequestMapping(value = "/occurrences", method = RequestMethod.GET)
    public
    @ResponseBody
    String getOccurrencesUploaded(HttpServletRequest request) {
        String q = request.getParameter("q");
        String box = request.getParameter("box");
        int start = Integer.parseInt(request.getParameter("start"));

        String[] bb = box.split(",");

        double long1 = Double.parseDouble(bb[0]);
        double lat1 = Double.parseDouble(bb[1]);
        double long2 = Double.parseDouble(bb[2]);
        double lat2 = Double.parseDouble(bb[3]);

        Object[] data = (Object[]) RecordsLookup.getData(q);

        int count = 0;
        String record = null;
        if (data != null) {
            double[] points = (double[]) data[0];
            ArrayList<QueryField> fields = (ArrayList<QueryField>) data[1];
            double[] pointsBB = (double[]) data[2];
            String metadata = (String) data[3];

            if (points == null || points.length == 0
                    || pointsBB[0] > long2 || pointsBB[2] < long1
                    || pointsBB[1] > lat2 || pointsBB[3] < lat1) {
                return null;
            } else {
                for (int i = 0; i < points.length; i += 2) {
                    if (points[i] >= long1 && points[i] <= long2
                            && points[i + 1] >= lat1 && points[i + 1] <= lat2) {
                        if (count == start) {
                            StringBuilder sb = new StringBuilder();
                            for (QueryField qf : fields) {
                                if (sb.length() == 0) {
                                    sb.append("{\"totalRecords\":<totalCount>,\"occurrences\":[{");
                                } else {
                                    sb.append(",");
                                }
                                sb.append("\"").append(qf.getDisplayName()).append("\":\"");
                                sb.append(qf.getAsString(i / 2).replace("\"", "\\\"")).append("\"");
                            }                            
                            sb.append("}]");
                            sb.append(",\"metadata\":\"");
                            sb.append(metadata);
                            sb.append("\"");
                            sb.append("}");
                            record = sb.toString();
                        }
                        count++;
                    }
                }
            }
        }

        if (record != null) {
            record = record.replace("<totalCount>", String.valueOf(count));
        }

        return record;
    }

    @RequestMapping(value = "/admin/reloadconfig", method = RequestMethod.GET)
    @ResponseBody
    public String reloadConfig() {
        //signal for reload
        ConfigurationLoaderStage1.loaders.get(0).interrupt();

        //return summary
        StringBuilder html = new StringBuilder();

        //was it successful?

        return html.toString();
    }

    @RequestMapping(value = "/image2", method = RequestMethod.GET)
    public void image2(
            @RequestParam(value = "type", required = false, defaultValue = "jpg") String type,
            @RequestParam(value = "extents", required = true) String bbox,
            @RequestParam(value = "pixelwidth", required = false, defaultValue = "800") Integer width,
            @RequestParam(value = "psize", required = false, defaultValue = "3") Integer pointSize,
            @RequestParam(value = "pcolour", required = false, defaultValue = "FF0000") String pointColour,
            @RequestParam(value = "popacity", required = false, defaultValue = "0.6") Double pointOpacity,
            @RequestParam(value = "basemap", required = false, defaultValue = "world") String basemap,
            @RequestParam(value = "legend", required = false, defaultValue = "off") String legend,
            @RequestParam(value = "bs", required = false) String biocacheServer,

            HttpServletRequest request, HttpServletResponse response) throws Exception {

        try {
            String [] bb = bbox.split(",");
            int pminx = Utils.convertLngToPixel(Double.parseDouble(bb[0]));
            int pminy = Utils.convertLatToPixel(Double.parseDouble(bb[1]));
            int pmaxx = Utils.convertLngToPixel(Double.parseDouble(bb[2]));
            int pmaxy = Utils.convertLatToPixel(Double.parseDouble(bb[3]));
            int height = (int) Math.round(width * ((pminy - pmaxy) / (double)(pmaxx - pminx)));

            double [] extents = Util.transformBbox4326To900913(Double.parseDouble(bb[0]),Double.parseDouble(bb[1]),Double.parseDouble(bb[2]),Double.parseDouble(bb[3]));

            //"http://biocache.ala.org.au/ws/webportal/wms/reflect?
            //q=macropus&ENV=color%3Aff0000%3Bname%3Acircle%3Bsize%3A3%3Bopacity%3A1
            //&BBOX=12523443.0512,-2504688.2032,15028131.5936,0.33920000120997&WIDTH=256&HEIGHT=256");
            String speciesAddress = ((biocacheServer == null) ? CommonData.biocacheServer : biocacheServer)
                    + "/webportal/wms/reflect?"
                    + "ENV=color%3A" + pointColour
                    + "%3Bname%3Acircle%3Bsize%3A" + pointSize
                    + "%3Bopacity%3A" + pointOpacity
                    + "&BBOX=" + extents[0] + "," + extents[1] + "," + extents[2] + "," + extents[3]
                    + "&WIDTH=" + width + "&HEIGHT=" + height
                    + "&" + request.getQueryString();
            System.out.println(speciesAddress);
            URL speciesURL = new URL(speciesAddress);

            BufferedImage speciesImage = ImageIO.read(speciesURL);

            //"http://spatial.ala.org.au/geoserver/wms/reflect?
            //LAYERS=ALA%3Aworld&SERVICE=WMS&VERSION=1.1.1&REQUEST=GetMap&STYLES=
            //&FORMAT=image%2Fjpeg&SRS=EPSG%3A900913&BBOX=12523443.0512,-1252343.932,13775787.3224,0.33920000004582&WIDTH=256&HEIGHT=256"
            String basemapAddress = CommonData.geoServer + "/wms/reflect?"
                    + "LAYERS=ALA%3A" + basemap
                    + "&SERVICE=WMS&VERSION=1.1.1&REQUEST=GetMap&STYLES="
                    + "&FORMAT=image%2Fjpeg&SRS=EPSG%3A900913"
                    + "&BBOX=" + extents[0] + "," + extents[1] + "," + extents[2] + "," + extents[3]
                    + "&WIDTH=" + width + "&HEIGHT=" + height
                    + (!legend.equals("off")?"&format_options=layout:legend":"");
            System.out.println(basemapAddress);
            URL basemapURL = new URL(basemapAddress);
            BufferedImage basemapImage = ImageIO.read(basemapURL);

            BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D combined = (Graphics2D) img.getGraphics();

            combined.drawImage(basemapImage, 0, 0, Color.WHITE, null);
            combined.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6f));
            combined.drawImage(speciesImage, null, 0, 0);
            combined.dispose();

            //response.setHeader("Cache-Control", "max-age=3600"); //age == 1hr

            if (type.equalsIgnoreCase("png")) {
                response.setContentType("image/png");
                OutputStream os = response.getOutputStream();
                ImageIO.write(img, type, os);
                os.close();
            } else {
                //handle jpeg + BufferedImage.TYPE_INT_ARGB
                BufferedImage img2;
                Graphics2D c2;
                (c2 = (Graphics2D) (img2 = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)).getGraphics()).drawImage(img, 0,0,Color.WHITE, null);
                c2.dispose();
                OutputStream os = response.getOutputStream();
                ImageIO.write(img2, type, os);
                os.close();
                response.setContentType("image/jpeg");
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    @RequestMapping(value = "/image", method = RequestMethod.GET)
    public void image(
            @RequestParam(value = "type", required = false, defaultValue = "jpg") String type,
            @RequestParam(value = "bbox", required = false, defaultValue = "0,0,0,0") String bbox,
            @RequestParam(value = "width", required = false, defaultValue = "800") String width,
            @RequestParam(value = "height", required = false, defaultValue = "640") String height,
            @RequestParam(value = "basemap", required = false, defaultValue = "outline") String basemap,

            HttpServletRequest request, HttpServletResponse response) throws IOException {

        //zoom (minlong, minlat, maxlong, maxlat, lhs width, basemap
        String zoom = "n"  + bbox + ",0," + basemap;

        //unique id
        String uid = String.valueOf(System.currentTimeMillis());
        String htmlpth = CommonData.print_output_path;
        String htmlurl = CommonData.print_output_url;

        SessionPrint sp = new SessionPrint(
                CommonData.webportalServer, "&" + request.getQueryString(),
                height, width, 
                htmlpth, htmlurl, uid, 
                zoom, "", 0, type);

        sp.print();

        response.setHeader("Cache-Control", "max-age=86400"); //age == 1 day
        if (type.equalsIgnoreCase("png")) {
            response.setContentType("image/png");
        } else if (type.equalsIgnoreCase("pdf")) {
            response.setContentType("application/pdf");
        } else {
            response.setContentType("image/jpeg");
        }

        OutputStream os = response.getOutputStream();
        FileInputStream fis = new FileInputStream(sp.getImageFilename());
        byte [] buffer = new byte[1024];
        int n;
        while((n = fis.read(buffer)) > 0) {
            os.write(buffer, 0, n);
        }
        fis.close();
        os.close();
    }
}
