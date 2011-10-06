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

import java.io.File;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.zip.GZIPOutputStream;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.ala.layers.dao.FieldDAO;
import org.ala.layers.dao.LayerDAO;
import org.ala.layers.dao.LayerIntersectDAO;
import org.ala.layers.dao.ObjectDAO;
import org.ala.layers.dto.Layer;
import org.ala.layers.dto.Objects;
import org.ala.layers.intersect.Grid;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 *
 * @author ajay
 */
@Controller
public class IntersectService {
    private final String WS_INTERSECT_SINGLE = "/intersect/{ids}/{lat}/{lng:.+}";
    private final String WS_INTERSECT_BATCH = "/intersect/batch";
    
    /**
     * Log4j instance
     */
    protected Logger logger = Logger.getLogger(this.getClass());
    @Resource(name = "fieldDao")
    private FieldDAO fieldDao;
    @Resource(name = "layerDao")
    private LayerDAO layerDao;
    @Resource(name = "objectDao")
    private ObjectDAO objectDao;
    @Resource(name = "layerIntersectDao")
    private LayerIntersectDAO layerIntersectDao;

    /*
     * return intersection of a point on layers(s)
     */
    // @ResponseBody String
    @RequestMapping(value = WS_INTERSECT_SINGLE, method = RequestMethod.GET)
    public
    @ResponseBody
    Object single(@PathVariable("ids") String ids, @PathVariable("lat") Double lat, @PathVariable("lng") Double lng, HttpServletRequest req) {
//        return Intersect.Intersect(ids, lat, lng);

        Vector out = new Vector();

        for (String id : ids.split(",")) {

            Layer layer = null;
            int newid = cleanObjectId(id);
            
            if (newid != -1) {
                layer = layerDao.getLayerById(newid);
            } 
            if(layer == null) {
                layer = layerDao.getLayerByName(id);
            }

            double[][] p = {{lng, lat}};

            if (layer != null) {
                if (layer.isShape()) {
                    Objects o = objectDao.getObjectByIdAndLocation("cl" + layer.getId(), lng, lat);
                    Map m = new HashMap();
                    m.put("value", o.getName());
                    m.put("layername", o.getFieldname());   //close enough
                    m.put("pid", o.getPid());
                    m.put("description", o.getDescription());
                    //m.put("fid", o.getFid());

                    out.add(m);
                } else if (layer.isGrid()) {
                    Grid g = new Grid(layerIntersectDao.getConfig().getLayerFilesPath() + layer.getPath_orig());
                    float[] v = g.getValues(p);
                    //s = "{\"value\":" + v[0] + ",\"layername\":\"" + layer.getDisplayname() + "\"}";
                    Map m = new HashMap();
                    m.put("value", v[0]);
                    m.put("layername", layer.getDisplayname());

                    out.add(m);
                }
            } else {
                String gid = null;
                String filename = null;
                String name = null;

                if (id.startsWith("species_")) {
                    //maxent layer
                    gid = id.substring(8);
                    filename = layerIntersectDao.getConfig().getAlaspatialOutputPath() + File.separator + "maxent" + File.separator + gid + File.separator + gid;
                    name = "Prediction";
                } else if (id.startsWith("aloc_")) {
                    //aloc layer
                    gid = id.substring(8);
                    filename = layerIntersectDao.getConfig().getAlaspatialOutputPath() + File.separator + "aloc" + File.separator + gid + File.separator + gid;
                    name = "Classification";
                }

                if (filename != null) {
                    Grid grid = new Grid(filename);

                    if (grid != null && (new File(filename + ".grd").exists())) {
                        float[] v = grid.getValues(p);
                        if (v != null) {
                            Map m = new HashMap();
                            if (Float.isNaN(v[0])) {
                                //s = "{\"value\":\"no data\",\"layername\":\"" + name + " (" + gid + ")\"}";
                                m.put("value", "no data");
                                m.put("layername", name + "(" + gid + ")");
                            } else {
                                //s = "{\"value\":" + v[0] + ",\"layername\":\"" + name + " (" + gid + ")\"}";
                                m.put("value", v[0]);
                                m.put("layername", name + "(" + gid + ")");
                            }
                            out.add(m);
                        }
                    }
                }
            }
        }

        return out;
    }

    /**
     * Clean up and just return an int for LAYER object
     * @param id
     * @return
     */
    private int cleanObjectId(String id) {
        //test field id value
        int len = Math.min(6, id.length());
        id = id.substring(0, len);
        char prefix = id.toUpperCase().charAt(0);
        String number = id.substring(2, len);
        try {
            int i = Integer.parseInt(number);
            return i;
        } catch (Exception e) {}

        return -1;
    }

    @RequestMapping(value = WS_INTERSECT_BATCH, method = RequestMethod.GET)
    public void batchGet(
            @RequestParam(value = "fids", required = false, defaultValue = "") String fids,
            @RequestParam(value = "points", required = false, defaultValue = "") String pointsString,
            HttpServletRequest request, HttpServletResponse response) {
        try {
            String [] pointsArray = pointsString.split(",");
            String [] fields = fids.split(",");

            List<String []> sample = layerIntersectDao.sampling(fids, pointsString);

            //setup output stream
            OutputStream os = response.getOutputStream();

            String charSet = "UTF-8";
            byte [] bComma = ",".getBytes(charSet);
            byte [] bNewLine = "\n".getBytes(charSet);
            byte [] bDblQuote = "\"".getBytes(charSet);

            //header
            os.write("longitude,latitude".getBytes(charSet));
            for(int i=0;i<fields.length;i++) {
                os.write(bComma);
                os.write(fields[i].getBytes(charSet));
            }
            //rows
            int rows = sample.get(0).length;
            for(int i=0;i<rows;i++) {
                os.write(bNewLine);
                os.write(pointsArray[i*2].getBytes(charSet));
                os.write(bComma);
                os.write(pointsArray[i*2+1].getBytes(charSet));
                for(int j=0;j<sample.size();j++) {
                    os.write(bComma);
                    String s = sample.get(j)[i];
                    boolean useQuotes = false;
                    if(s != null) {
                        if(s.contains("\"")) {
                            s = s.replace("\"", "\"\"");
                            useQuotes = true;
                        } else if(s.contains(",")) {
                            useQuotes = true;
                        }
                        if(useQuotes) {
                            os.write(bDblQuote);
                            os.write(s.getBytes(charSet));
                            os.write(bDblQuote);
                        } else {
                            os.write(s.getBytes(charSet));
                        }
                    }
                }
            }

            os.flush();
            os.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @RequestMapping(value = WS_INTERSECT_BATCH, method = RequestMethod.POST)
    public void batch(
            @RequestParam(value = "fids", required = false, defaultValue = "") String fids,
            @RequestParam(value = "points", required = false, defaultValue = "") String pointsString,
            HttpServletRequest request, HttpServletResponse response) {

        try {
            String [] pointsArray = pointsString.split(",");
            String [] fields = fids.split(",");

            List<String []> sample = layerIntersectDao.sampling(fids, pointsString);

            //setup output stream
            OutputStream os = response.getOutputStream();
            GZIPOutputStream gzip = new GZIPOutputStream(os);

            String charSet = "UTF-8";
            byte [] bComma = ",".getBytes(charSet);
            byte [] bNewLine = "\n".getBytes(charSet);
            byte [] bDblQuote = "\"".getBytes(charSet);

            //header
            gzip.write("longitude,latitude".getBytes(charSet));
            for(int i=0;i<fields.length;i++) {
                gzip.write(bComma);
                gzip.write(fields[i].getBytes(charSet));
            }
            //rows
            int rows = sample.get(0).length;
            for(int i=0;i<rows;i++) {
                gzip.write(bNewLine);
                gzip.write(pointsArray[i*2].getBytes(charSet));
                gzip.write(bComma);
                gzip.write(pointsArray[i*2+1].getBytes(charSet));
                for(int j=0;j<sample.size();j++) {
                    gzip.write(bComma);
                    String s = sample.get(j)[i];
                    boolean useQuotes = false;
                    if(s != null) {
                        if(s.contains("\"")) {
                            s = s.replace("\"", "\"\"");
                            useQuotes = true;
                        } else if(s.contains(",")) {
                            useQuotes = true;
                        }
                        if(useQuotes) {
                            gzip.write(bDblQuote);
                            gzip.write(s.getBytes(charSet));
                            gzip.write(bDblQuote);
                        } else {
                            gzip.write(s.getBytes(charSet));
                        }
                    }
                }
            }

            gzip.flush();
            gzip.close();
            os.flush();
            os.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
