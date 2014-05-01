/**************************************************************************
 *  Copyright (C) 2010 Atlas of Living Australia
 *  All Rights Reserved.
 *
 *  The contents of this file are subject to the Mozilla Public
 *  License Version 1.1 (the "License"); you may not use this file
 *  except in compliance with the License. You may obtain a copy of
 *  the License at http://www.mozilla.org/MPL/
 *
 *  Software distributed under the License is distributed on an "AS
 *  IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 *  implied. See the License for the specific language governing
 *  rights and limitations under the License.
 ***************************************************************************/
package org.ala.layers.web;

import au.com.bytecode.opencsv.CSVReader;
import org.ala.layers.dao.DistributionDAO;
import org.ala.layers.dao.FieldDAO;
import org.ala.layers.dao.UserDataDAO;
import org.ala.layers.dto.Distribution;
import org.ala.layers.dto.Ud_header;
import org.ala.layers.legend.Facet;
import org.ala.layers.legend.Legend;
import org.ala.layers.legend.LegendObject;
import org.ala.layers.legend.QueryField;
import org.ala.layers.userdata.RecordsLookup;
import org.ala.layers.util.SpatialUtils;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.imageio.ImageIO;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.*;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @author Adam
 */
@Controller
public class UserDataService {

    private final String WS_USERDATA_ADD = "/userdata/add";
    private final String WS_USERDATA_FACET = "/userdata/facet";
    private final String WS_USERDATA_LIST = "/userdata/list";
    private final String WS_USERDATA_GET = "/userdata/get";
    private final String WS_USERDATA_GETQF = "/userdata/getqf";
    private final String WS_USERDATA_SAMPLE = "/userdata/sample";

    private final String WS_USERDATA_WMS = "/userdata/wms/reflect";

    final static int HIGHLIGHT_RADIUS = 3;

    @Resource(name = "userDataDao")
    private UserDataDAO userDataDao;

    /**
     * Log4j instance
     */
    protected Logger logger = Logger.getLogger(this.getClass());

    @RequestMapping(value = WS_USERDATA_ADD, method = {RequestMethod.POST, RequestMethod.GET})
    public
    @ResponseBody
    Ud_header add(HttpServletRequest req) {
        RecordsLookup.setUserDataDao(userDataDao);

        String name = req.getParameter("name");
        String description = req.getParameter("description");
        String csv = req.getParameter("csv");
        String user_id = req.getParameter("user_id");

        Ud_header ud_header = userDataDao.put(user_id, "Imported CSV", name, description, null, null);

        if (importCSV(name, String.valueOf(ud_header.getUd_header_id()), csv)) {
            return ud_header;
        }

        return null;
    }

    boolean importCSV(String name, String ud_header_id, String csv) {
        try {
            CSVReader reader = new CSVReader(new StringReader(csv));

            List userPoints = reader.readAll();

            logger.debug("userPoints.size(): " + userPoints.size());
            //if only one column treat it as a list of LSID's
            if (userPoints.size() == 0) {
                throw (new RuntimeException("no data in csv"));
            }

            boolean hasHeader = false;

            // check if it has a header
            String[] upHeader = (String[]) userPoints.get(0);
            try {
                Double d1 = new Double(upHeader[1]);
                Double d2 = new Double(upHeader[2]);
            } catch (Exception e) {
                hasHeader = true;
            }

            logger.debug("hasHeader: " + hasHeader);

            // check if the count of points goes over the threshold.
            int sizeToCheck = (hasHeader) ? userPoints.size() - 1 : userPoints.size();

            ArrayList<QueryField> fields = new ArrayList<QueryField>();
            if (upHeader.length == 2) {
                //only points upload, add 'id' column at the start
                fields.add(new QueryField("id"));
                fields.get(0).ensureCapacity(sizeToCheck);
            }
            String[] defaultHeader = {"id", "longitude", "latitude"};
            for (int i = 0; i < upHeader.length; i++) {
                String header = upHeader[i];
                if (upHeader.length == 2 && i < 2) {
                    header = defaultHeader[i + 1];
                } else if (upHeader.length > 2 && i < 3) {
                    header = defaultHeader[i];
                }
                fields.add(new QueryField("__f" + String.valueOf(i), header, QueryField.FieldType.AUTO));
                fields.get(fields.size() - 1).ensureCapacity(sizeToCheck);
            }

            double[] points = new double[sizeToCheck * 2];
            int counter = 1;
            int hSize = hasHeader ? 1 : 0;
            double minx = 1000;
            double maxx = -1000;
            double miny = 1000;
            double maxy = -1000;
            for (int i = 0; i < userPoints.size() - hSize; i++) {
                String[] up = (String[]) userPoints.get(i + hSize);
                if (up.length > 2) {
                    for (int j = 0; j < up.length && j < fields.size(); j++) {
                        //replace anything that may interfere with facet parsing
                        String s = up[j].replace("\"", "'").replace(" AND ", " and ").replace(" OR ", " or ");
                        if (s.length() > 0 && s.charAt(0) == '*') {
                            s = "_" + s;
                        }
                        fields.get(j).add(s);
                    }
                    try {
                        points[i * 2] = Double.parseDouble(up[1]);
                        points[i * 2 + 1] = Double.parseDouble(up[2]);
                    } catch (Exception e) {
                    }
                } else if (up.length > 1) {
                    fields.get(0).add(ud_header_id + "-" + counter);
                    for (int j = 0; j < up.length && j < fields.size(); j++) {
                        fields.get(j + 1).add(up[j]);
                    }
                    try {
                        points[i * 2] = Double.parseDouble(up[0]);
                        points[i * 2 + 1] = Double.parseDouble(up[1]);
                    } catch (Exception e) {
                    }
                    counter++;
                }
                if (!Double.isNaN(points[i * 2])) {
                    if (points[i * 2] < minx) {
                        minx = points[i * 2];
                    }
                    if (points[i * 2] > maxx) {
                        maxx = points[i * 2];
                    }
                }
                if (!Double.isNaN(points[i * 2 + 1])) {
                    if (points[i * 2 + 1] < miny) {
                        miny = points[i * 2 + 1];
                    }
                    if (points[i * 2 + 1] > maxy) {
                        maxy = points[i * 2 + 1];
                    }
                }
            }

            //store data
            userDataDao.setDoubleArray(ud_header_id, "points", points);

            HashMap<String, LegendObject> field_details = new HashMap<String, LegendObject>();
            for (int i = 0; i < fields.size(); i++) {
                fields.get(i).store();  //finalize qf
                userDataDao.setQueryField(ud_header_id, fields.get(i).getName(), fields.get(i));
                field_details.put(fields.get(i).getName() + "\r\n" + fields.get(i).getDisplayName(), fields.get(i).getLegend());
            }

            HashMap<String, Object> metadata = new HashMap<String, Object>();
            metadata.put("title", "User uploaded points");
            metadata.put("name", name);
            metadata.put("date", System.currentTimeMillis());
            metadata.put("number_of_records", points.length / 2);
            metadata.put("bbox", minx + "," + miny + "," + maxx + "," + maxy);
            //metadata.put("user_fields",field_details);

            userDataDao.setMetadata(Long.parseLong(ud_header_id), metadata);

            // close the reader and data streams
            reader.close();

            return true;
        } catch (Exception e) {
            logger.error("failed to import user csv", e);
        }

        return false;
    }

    @RequestMapping(value = "/occurrences", method = RequestMethod.GET)
    public
    @ResponseBody
    String getOccurrencesUploaded(HttpServletRequest request) {
        RecordsLookup.setUserDataDao(userDataDao);

        String q = request.getParameter("q");
        String box = request.getParameter("box");
        int start = Integer.parseInt(request.getParameter("start"));

        //q can have 2 parts, ud_header_id and facet_id
        String ud_header_id = q.split(":")[0];
        String facet_id = (q.contains(":")) ? q.split(":")[1] : null;

        String[] bb = box.split(",");

        double long1 = Double.parseDouble(bb[0]);
        double lat1 = Double.parseDouble(bb[1]);
        double long2 = Double.parseDouble(bb[2]);
        double lat2 = Double.parseDouble(bb[3]);

        Object[] data = (Object[]) RecordsLookup.getData(q);

        int count = 0;
        String record = null;
        if (data != null) {
            double[] points = (double[]) data[1];
            ArrayList<QueryField> fields = (ArrayList<QueryField>) data[2];
            double[] pointsBB = (double[]) data[4];
            Map metadata = (Map) data[3];
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
                            sb.append("{\"totalRecords\":<totalCount>,\"occurrences\":[{");
                            int start_length = sb.length();
                            for (QueryField qf : fields) {
                                if (sb.length() == start_length) {
                                    //
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

    @RequestMapping(value = WS_USERDATA_WMS, method = RequestMethod.GET)
    public void getPointsMap(
            @RequestParam(value = "CQL_FILTER", required = false, defaultValue = "") String cql_filter,
            @RequestParam(value = "ENV", required = false, defaultValue = "") String env,
            @RequestParam(value = "BBOX", required = false, defaultValue = "") String bboxString,
            @RequestParam(value = "WIDTH", required = false, defaultValue = "") String widthString,
            @RequestParam(value = "HEIGHT", required = false, defaultValue = "") String heightString,
            HttpServletRequest request, HttpServletResponse response) {
        RecordsLookup.setUserDataDao(userDataDao);

        response.setHeader("Cache-Control", "max-age=86400"); //age == 1 day
        response.setContentType("image/png"); //only png images generated

        int width = 256, height = 256;
        try {
            width = Integer.parseInt(widthString);
            height = Integer.parseInt(heightString);
        } catch (Exception e) {
            logger.error("error parsing to int: " + widthString + " or " + heightString, e);
        }

        try {
            env = URLDecoder.decode(env, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            logger.error("error decoding env from UTF-8: " + env, e);
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
                    highlight = URLDecoder.decode(s.substring(4), "UTF-8").replace("%3B", ";");
                } catch (Exception e) {
                }
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
                logger.error("error converting bounding box value to double: " + s, e);
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
            double xoffset = (bbox[2] - bbox[0]) / (double) width * (size + (highlight != null ? HIGHLIGHT_RADIUS * 2 + size * 0.2 : 0) + 5);
            double yoffset = (bbox[3] - bbox[1]) / (double) height * (size + (highlight != null ? HIGHLIGHT_RADIUS * 2 + size * 0.2 : 0) + 5);

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

            double[][] bb = {{SpatialUtils.convertMetersToLng(bbox[0] - xoffset), SpatialUtils.convertMetersToLat(bbox[1] - yoffset)}, {SpatialUtils.convertMetersToLng(bbox[2] + xoffset), SpatialUtils.convertMetersToLat(bbox[3] + yoffset)}};

            double[] pbbox = new double[4]; //pixel bounding box
            pbbox[0] = SpatialUtils.convertLngToPixel(SpatialUtils.convertMetersToLng(bbox[0]));
            pbbox[1] = SpatialUtils.convertLatToPixel(SpatialUtils.convertMetersToLat(bbox[1]));
            pbbox[2] = SpatialUtils.convertLngToPixel(SpatialUtils.convertMetersToLng(bbox[2]));
            pbbox[3] = SpatialUtils.convertLatToPixel(SpatialUtils.convertMetersToLat(bbox[3]));

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

            boolean[] valid = null;
            if (lsid != null) {
                Object[] data = (Object[]) RecordsLookup.getData(lsid);
                points = (double[]) data[1];
                pointsBB = (double[]) data[4];

                if (points == null || points.length == 0
                        || pointsBB[0] > bb[1][0] || pointsBB[2] < bb[0][0]
                        || pointsBB[1] > bb[1][1] || pointsBB[3] < bb[0][1]) {
                    setImageBlank(response);
                    return;
                }

                ArrayList<QueryField> fields = (ArrayList<QueryField>) data[2];

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
                    if (valid != null && !valid[i / 2]) {
                        continue;
                    }
                    x = (int) ((SpatialUtils.convertLngToPixel(points[i]) - pbbox[0]) * grid_width_mult);
                    y = (int) ((SpatialUtils.convertLatToPixel(points[i + 1]) - pbbox[3]) * grid_height_mult);
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
                            if (valid != null && !valid[i / 2]) {
                                continue;
                            }
                            if (points[i] >= bb[0][0] && points[i] <= bb[1][0]
                                    && points[i + 1] >= bb[0][1] && points[i + 1] <= bb[1][1]) {
                                x = (int) ((SpatialUtils.convertLngToPixel(points[i]) - pbbox[0]) * width_mult);
                                y = (int) ((SpatialUtils.convertLatToPixel(points[i + 1]) - pbbox[3]) * height_mult);
                                g.fillOval(x - size, y - size, pointWidth, pointWidth);
                            }
                        }
                    } else {
                        int prevColour = -1;    //!= colours[0]
                        g.setColor(new Color(prevColour));
                        for (i = 0; i < points.length; i += 2) {
                            if (valid != null && !valid[i / 2]) {
                                continue;
                            }
                            if (points[i] >= bb[0][0] && points[i] <= bb[1][0]
                                    && points[i + 1] >= bb[0][1] && points[i + 1] <= bb[1][1]) {
                                int thisColour = colours.getColour(i / 2);
                                if (thisColour != prevColour) {
                                    g.setColor(new Color(thisColour));
                                    prevColour = thisColour;
                                }
                                x = (int) ((SpatialUtils.convertLngToPixel(points[i]) - pbbox[0]) * width_mult);
                                y = (int) ((SpatialUtils.convertLatToPixel(points[i + 1]) - pbbox[3]) * height_mult);
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
                        if (valid != null && !valid[i / 2]) {
                            continue;
                        }
                        if (points[i] >= bb[0][0] && points[i] <= bb[1][0]
                                && points[i + 1] >= bb[0][1] && points[i + 1] <= bb[1][1]) {
                            if (facet.isValid(listHighlight, i / 2)) {
                                x = (int) ((SpatialUtils.convertLngToPixel(points[i]) - pbbox[0]) * width_mult);
                                y = (int) ((SpatialUtils.convertLatToPixel(points[i + 1]) - pbbox[3]) * height_mult);
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
                logger.error("error in outputting wms/reflect image as png", e);
            }
        } catch (Exception e) {
            logger.error("error generating wms/reflect tile", e);
        }

        //logger.debug("[wms tile: " + (System.currentTimeMillis() - start) + "ms]");
    }

    //256x256 transparent image
    static Object blankImageObject = new Object();
    static byte[] blankImageBytes = null;

    private void setImageBlank(HttpServletResponse response) {
        if (blankImageBytes == null && blankImageObject != null) {
            synchronized (blankImageObject) {
                if (blankImageBytes == null) {
                    try {
                        RandomAccessFile raf = new RandomAccessFile(UserDataService.class.getResource("/blank.png").getFile(), "r");
                        blankImageBytes = new byte[(int) raf.length()];
                        raf.read(blankImageBytes);
                        raf.close();
                    } catch (IOException e) {
                        logger.error("error reading default blank tile", e);
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
            } catch (IOException e) {
                logger.error("error outputting blank tile", e);
            }
        }
    }

    @RequestMapping(value = WS_USERDATA_FACET, method = {RequestMethod.POST, RequestMethod.GET})
    public
    @ResponseBody
    Ud_header facet(HttpServletRequest req) {
        RecordsLookup.setUserDataDao(userDataDao);

        String ud_header_id = req.getParameter("id");
        String new_wkt = req.getParameter("wkt");

        ArrayList<String> new_facets = new ArrayList<String>();
        int i = 0;
        while (req.getParameter("facet" + i) != null) {
            new_facets.add(req.getParameter("facet" + i));
            i++;
        }

        if (new_facets.size() == 0 && new_wkt == null) {
            Long id = Long.parseLong(ud_header_id.split(":")[0]);
            return userDataDao.get(id);
        } else {
            return userDataDao.facet(ud_header_id, new_facets, new_wkt);
        }
    }

    @RequestMapping(value = WS_USERDATA_LIST, method = {RequestMethod.POST, RequestMethod.GET})
    public
    @ResponseBody
    List<String> list(HttpServletRequest req) {
        RecordsLookup.setUserDataDao(userDataDao);

        String id = req.getParameter("id");

        ArrayList<String> ret = new ArrayList<String>(userDataDao.listData(id, "QueryField"));

        return ret;
    }

    @RequestMapping(value = WS_USERDATA_GETQF, method = {RequestMethod.POST, RequestMethod.GET})
    public
    @ResponseBody
    QueryField getqf(HttpServletRequest req) {
        RecordsLookup.setUserDataDao(userDataDao);

        String id = req.getParameter("id");
        String field = req.getParameter("field");

        QueryField qf = userDataDao.getQueryField(id, field);

        //Because RecordsLookup does not requery what it has already loaded, add it to the in mem list of QueryFields
        RecordsLookup.addQf(id, qf);

        return qf;
    }


    @RequestMapping(value = WS_USERDATA_SAMPLE, method = {RequestMethod.POST, RequestMethod.GET})
    public
    @ResponseBody
    void samplezip(HttpServletRequest req, HttpServletResponse resp) {
        RecordsLookup.setUserDataDao(userDataDao);

        String id = req.getParameter("q");
        String fields = req.getParameter("fl");

        String sample = userDataDao.getSampleZip(id, fields);

        try {
            // Create the ZIP file
            ZipOutputStream out = new ZipOutputStream(resp.getOutputStream());

            //put entry
            out.putNextEntry(new ZipEntry("sample.csv"));
            out.write(sample.getBytes());
            out.closeEntry();

            resp.setContentType("application/zip");
            resp.setHeader("Content-Disposition", "attachment; filename=\"sample.zip\"");

            // Complete the ZIP file
            out.close();
        } catch (Exception e) {
            logger.error("failed to zip sampling",e);
        }

    }
}
