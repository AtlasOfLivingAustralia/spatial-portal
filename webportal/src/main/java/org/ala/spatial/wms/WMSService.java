/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.wms;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.ala.spatial.data.Facet;
import org.ala.spatial.data.Legend;
import org.ala.spatial.data.QueryField;
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

        //grid redirect
        if (env.contains("grid")) {
            getGridMap(cql_filter, env, bboxString, widthString, heightString, request, response);
            return;
        }

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
            } else if (pair[0].equals("uncertainty")) {
                uncertainty = true;
            } else if (pair[0].equals("sel")) {
                highlight = s.replace("sel:","");//pair[1];
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
            double xoffset = (bbox[2] - bbox[0]) / (double) width * size;
            double yoffset = (bbox[3] - bbox[1]) / (double) height * size;

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

            SimpleRegion region = new SimpleRegion();
            region.setBox(Utils.convertMetersToLng(bbox[0] - xoffset), Utils.convertMetersToLat(bbox[1] - yoffset), Utils.convertMetersToLng(bbox[2] + xoffset), Utils.convertMetersToLat(bbox[3] + yoffset));

            double[][] bb = region.getBoundingBox();

            double[] pbbox = new double[4]; //pixel bounding box
            pbbox[0] = Utils.convertLngToPixel(Utils.convertMetersToLng(bbox[0]));
            pbbox[1] = Utils.convertLatToPixel(Utils.convertMetersToLat(bbox[1]));
            pbbox[2] = Utils.convertLngToPixel(Utils.convertMetersToLng(bbox[2]));
            pbbox[3] = Utils.convertLatToPixel(Utils.convertMetersToLat(bbox[3]));

            String lsid = null;
            SimpleRegion r = null;
            int p1 = cql_filter.indexOf("qid:");
            if (p1 > 0) {
                int p2 = cql_filter.indexOf('&', p1 + 1);
                if (p2 < 0) {
                    p2 = cql_filter.indexOf(';', p1 + 1);
                }
                if (p2 < 0) {
                    p2 = cql_filter.length();
                }
                lsid = cql_filter.substring(p1, p2);
            }
            if (lsid == null) {
                p1 = 0;
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
            }

            double[] points = null;
            int [] uncertainties = null;
            short[] uncertaintiesType = null;
            QueryField uncertaintyField = null;
            QueryField [] listHighlight = null;
            QueryField colours = null;
            double[] pointsBB = null;
            Facet [] facets = null;
            if(highlight != null) {
                facets = Facet.parseFacets(highlight);
                listHighlight = new QueryField[facets.length];
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
                    if (uncertainty) {
                        if(fields.get(j).getName().equalsIgnoreCase("uncertainty")) {
                            uncertaintyField = fields.get(j);
                        }
                    }
                    if (facets != null) {
                        for(i=0;i<facets.length;i++) {
                            if(fields.get(j).getName().equalsIgnoreCase(facets[i].getField())) {
                                listHighlight[i] = fields.get(j);
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

            //fix uncertanties to max 30000 and alter colours
            if (uncertainty) {
                uncertaintiesType = new short[points.length/2];
                uncertainties = new int[points.length/2];
                for (int j = 0; j < uncertaintiesType.length; j++) {
                    if (uncertaintyField == null || uncertaintyField.getInt(j) > 30000) {
                        uncertaintiesType[j] = 2;
                        uncertainties[j] = 30000;
                    } else if (uncertaintyField.getInt(j) == Integer.MIN_VALUE) {
                        uncertaintiesType[j] = 1;
                        uncertainties[j] = 30000;
                    } else {
                        uncertaintiesType[j] = 0;
                        uncertainties[j] = uncertaintyField.getInt(j);
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
            int pointWidth = size * 2;
            double width_mult = (width / (pbbox[2] - pbbox[0]));
            double height_mult = (height / (pbbox[1] - pbbox[3]));

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
                    for (i = 0; i < points.length; i += 2) {
                        if (points[i] >= bb[0][0] && points[i] <= bb[1][0]
                                && points[i + 1] >= bb[0][1] && points[i + 1] <= bb[1][1]) {
                            int thisColour = colours.getColour(i / 2);
                            if (thisColour != prevColour) {
                                g.setColor(new Color(thisColour));
                            }
                            x = (int) ((Utils.convertLngToPixel(points[i]) - pbbox[0]) * width_mult);
                            y = (int) ((Utils.convertLatToPixel(points[i + 1]) - pbbox[3]) * height_mult);
                            g.fillOval(x - size, y - size, pointWidth, pointWidth);
                        }
                    }
                }
            }

            if (highlight != null) {
                g.setColor(new Color(255, 0, 0, alpha));
                int sz = size + 3;
                int w = sz * 2;
                int h;
                for (i = 0; i < points.length; i += 2) {
                    if (points[i] >= bb[0][0] && points[i] <= bb[1][0]
                            && points[i + 1] >= bb[0][1] && points[i + 1] <= bb[1][1]) {
                        h = 0;
                        for(int j=0;j<listHighlight.length;j++) {
                            //TODO: do type based comparisons instead of only String
                            if(facets[j].isValid(listHighlight[j].getAsString(i/2))) {
                                h++;
                            }
                        }
                        if (h == listHighlight.length) {
                            x = (int) ((Utils.convertLngToPixel(points[i]) - pbbox[0]) * width_mult);
                            y = (int) ((Utils.convertLatToPixel(points[i + 1]) - pbbox[3]) * height_mult);
                            g.drawOval(x - sz, y - sz, w, w);
                        }
                    }
                }
            }

            //uncertainty
            if (uncertainty) {
                int uncertaintyRadius;

                //white, uncertainty value
                g.setColor(new Color(255, 255, 255, alpha));
                double hmult = (height / (bbox[3] - bbox[1]));
                for (i = 0; i < points.length; i += 2) {
                    if (points[i] >= bb[0][0] && points[i] <= bb[1][0]
                            && points[i + 1] >= bb[0][1] && points[i + 1] <= bb[1][1]) {
                        if (uncertaintiesType[i / 2] == 0) {
                            x = (int) ((Utils.convertLngToPixel(points[i]) - pbbox[0]) * width_mult);
                            y = (int) ((Utils.convertLatToPixel(points[i + 1]) - pbbox[3]) * height_mult);
                            uncertaintyRadius = (int) Math.ceil(uncertainties[i / 2] * hmult);
                            g.drawOval(x - uncertaintyRadius, y - uncertaintyRadius, uncertaintyRadius * 2, uncertaintyRadius * 2);
                        }
                    }
                }

                //yellow, undefined uncertainty value
                g.setColor(new Color(255, 255, 100, alpha));
                uncertaintyRadius = (int) Math.ceil(30000 * hmult);
                for (i = 0; i < points.length; i += 2) {
                    if (points[i] >= bb[0][0] && points[i] <= bb[1][0]
                            && points[i + 1] >= bb[0][1] && points[i + 1] <= bb[1][1]) {
                        if (uncertaintiesType[i / 2] == 1) {
                            x = (int) ((Utils.convertLngToPixel(points[i]) - pbbox[0]) * width_mult);
                            y = (int) ((Utils.convertLatToPixel(points[i + 1]) - pbbox[3]) * height_mult);
                            g.drawOval(x - uncertaintyRadius, y - uncertaintyRadius, uncertaintyRadius * 2, uncertaintyRadius * 2);
                        }
                    }
                }

                //green, capped uncertainty value
                g.setColor(new Color(100, 255, 100, alpha));
                uncertaintyRadius = (int) Math.ceil(30000 * hmult);
                for (i = 0; i < points.length; i += 2) {
                    if (points[i] >= bb[0][0] && points[i] <= bb[1][0]
                            && points[i + 1] >= bb[0][1] && points[i + 1] <= bb[1][1]) {
                        if (uncertaintiesType[i / 2] == 2) {
                            x = (int) ((Utils.convertLngToPixel(points[i]) - pbbox[0]) * width_mult);
                            y = (int) ((Utils.convertLatToPixel(points[i + 1]) - pbbox[3]) * height_mult);
                            g.drawOval(x - uncertaintyRadius, y - uncertaintyRadius, uncertaintyRadius * 2, uncertaintyRadius * 2);
                        }
                    }
                }
            }

            g.dispose();

            try {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                ImageIO.write(img, "png", outputStream);
                ServletOutputStream outStream = response.getOutputStream();
                outStream.write(outputStream.toByteArray());
                outStream.flush();
                outStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch(Exception e) {
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

    @RequestMapping(value = "/wms/reflect2", method = RequestMethod.GET)
    public void getGridMap(
            @RequestParam(value = "CQL_FILTER", required = false, defaultValue = "") String cql_filter,
            @RequestParam(value = "ENV", required = false, defaultValue = "") String env,
            @RequestParam(value = "BBOX", required = false, defaultValue = "") String bboxString,
            @RequestParam(value = "WIDTH", required = false, defaultValue = "") String widthString,
            @RequestParam(value = "HEIGHT", required = false, defaultValue = "") String heightString,
            HttpServletRequest request, HttpServletResponse response) {

        int divs = 16; //number of x & y divisions in the WIDTH/HEIGHT

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
        }
        int red = 0, green = 0, blue = 0, alpha = 0;
        for (String s : env.split(";")) {
            String[] pair = s.split(":");
            if (pair[0].equals("color")) {
                while (pair[1].length() < 6) {
                    pair[1] = "0" + pair[1];
                }
                red = Integer.parseInt(pair[1].substring(0, 2), 16);
                green = Integer.parseInt(pair[1].substring(2, 4), 16);
                blue = Integer.parseInt(pair[1].substring(4), 16);
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

//adjust bbox extents with half pixel width/height
        double pixelWidth = (bbox[2] - bbox[0]) / width;
        double pixelHeight = (bbox[3] - bbox[1]) / height;
        bbox[0] += pixelWidth / 2;
        bbox[2] -= pixelWidth / 2;
        bbox[1] += pixelHeight / 2;
        bbox[3] -= pixelHeight / 2;

        SimpleRegion region = new SimpleRegion();
        region.setBox(Utils.convertMetersToLng(bbox[0]), Utils.convertMetersToLat(bbox[1]), Utils.convertMetersToLng(bbox[2]), Utils.convertMetersToLat(bbox[3]));

        double[] pbbox = new double[4]; //pixel bounding box
        pbbox[0] = Utils.convertLngToPixel(Utils.convertMetersToLng(bbox[0]));
        pbbox[1] = Utils.convertLatToPixel(Utils.convertMetersToLat(bbox[1]));
        pbbox[2] = Utils.convertLngToPixel(Utils.convertMetersToLng(bbox[2]));
        pbbox[3] = Utils.convertLatToPixel(Utils.convertMetersToLat(bbox[3]));

        String lsid = null;
        SimpleRegion r = null;
        int p1 = cql_filter.indexOf("qid:");
        if (p1 > 0) {
            int p2 = cql_filter.indexOf('&', p1 + 1);
            if (p2 < 0) {
                p2 = cql_filter.indexOf(';', p1 + 1);
            }
            if (p2 < 0) {
                p2 = cql_filter.length();
            }
            lsid = cql_filter.substring(p1, p2);
        }
        if (lsid == null) {
            p1 = 0;
            int p2 = cql_filter.indexOf('&', p1 + 1);
            if (p2 < 0) {
                p2 = cql_filter.indexOf(';', p1 + 1);
            }
            if (p2 < 0) {
                p2 = cql_filter.length();
            }
            if (p1 >= 0) {
                lsid = cql_filter.substring(p1 + 4);
            }
        }

        double[][] bb = region.getBoundingBox();

        double[] points = null;
        int[] uncertainties = null;
        short[] uncertaintiesType = null;
        boolean[] listHighlight = null;
        QueryField colours = null;
        double[] pointsBB = null;
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
        }

        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) img.getGraphics();
        g.setColor(new Color(0, 0, 0, 0));
        g.fillRect(0, 0, width, height);

        g.setColor(new Color(red, green, blue, alpha));
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int x, y;
        double width_mult = (width / (pbbox[2] - pbbox[0])) / (256 / divs);
        double height_mult = (height / (pbbox[1] - pbbox[3])) / (256 / divs);

        //count
        int[][] gridCounts = new int[divs][divs];

        for (i = 0; i < points.length; i += 2) {
            x = (int) ((Utils.convertLngToPixel(points[i]) - pbbox[0]) * width_mult);
            y = (int) ((Utils.convertLatToPixel(points[i + 1]) - pbbox[3]) * height_mult);
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

        g.dispose();

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(img, "png", outputStream);
            ServletOutputStream outStream = response.getOutputStream();
            outStream.write(outputStream.toByteArray());
            outStream.flush();
            outStream.close();
        } catch (IOException ex) {
        }
    }

    @RequestMapping(value = "/occurrences", method = RequestMethod.GET)
    public 
    @ResponseBody
    String getOccurrencesUploaded(HttpServletRequest request) {
        String q = request.getParameter("q");
        String box = request.getParameter("box");
        int start = Integer.parseInt(request.getParameter("start"));

        String [] bb = box.split(",");

        double long1 = Double.parseDouble(bb[0]);
        double lat1 = Double.parseDouble(bb[1]);
        double long2 = Double.parseDouble(bb[2]);
        double lat2 = Double.parseDouble(bb[3]);

        Object[] data = (Object[]) RecordsLookup.getData(q);

        int count = 0;
        String record = null;
        if(data != null) {
            double [] points = (double[]) data[0];
            ArrayList<QueryField> fields = (ArrayList<QueryField>) data[1];
            double [] pointsBB = (double[]) data[2];

            if (points == null || points.length == 0
                    || pointsBB[0] > long2 || pointsBB[2] < long1
                    || pointsBB[1] > lat2 || pointsBB[3] < lat1) {
                return null;
            } else {
                for(int i=0;i<points.length;i+=2) {
                    if(points[i] >= long1 && points[i] <= long2
                            && points[i+1] >= lat1 && points[i+1] <= lat2){
                        if(count == start) {
                            StringBuilder sb = new StringBuilder();
                            for(QueryField qf : fields) {
                                if(sb.length() == 0) {
                                    sb.append("{\"totalRecords\":<totalCount>,\"occurrences\":[{");
                                } else {
                                    sb.append(",");
                                }
                                sb.append("\"").append(qf.getDisplayName()).append("\":\"");
                                sb.append(qf.getAsString(i/2).replace("\"","\\\"")).append("\"");
                            }
                            sb.append("}]}");
                            record = sb.toString();
                        }
                        count++;
                    }
                }
            }
        }

        if(record != null) {
            record = record.replace("<totalCount>", String.valueOf(count));
        }

        return record;
    }
}
