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
import java.io.FileInputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;
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
import org.ala.layers.util.BatchConsumer;
import org.ala.layers.util.BatchProducer;
import org.ala.layers.util.IntersectUtil;
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
    private final String WS_INTERSECT_BATCH_STATUS = "/intersect/batch/{id}";
    private final String WS_INTERSECT_BATCH_DOWNLOAD = "/intersect/batch/download/{id}";

    private final String BATCH_PATH = "/data/batch_sampling/";

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

            ArrayList<String> sample = layerIntersectDao.sampling(fids, pointsString);

            //setup output stream
            OutputStream os = response.getOutputStream();

            IntersectUtil.writeSampleToStream(fields, pointsArray, sample, os);

            os.flush();
            os.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static Properties userProperties;

    @RequestMapping(value = WS_INTERSECT_BATCH, method = RequestMethod.POST)
    public Map batch(
            @RequestParam(value = "fids", required = false, defaultValue = "") String fids,
            @RequestParam(value = "points", required = false, defaultValue = "") String pointsString,
            HttpServletRequest request, HttpServletResponse response) {
        Map map = new HashMap();
        String batchId = null;
        try {

            //get limits
            int pointsLimit, fieldsLimit;

            String [] passwords = userProperties.getProperty("batch_sampling_passwords").split(",");
            pointsLimit = Integer.parseInt(userProperties.getProperty("batch_sampling_points_limit"));
            fieldsLimit = Integer.parseInt(userProperties.getProperty("batch_sampling_fields_limit"));

            String password = request.getParameter("pw");
            for(int i=0;password != null && i<passwords.length;i++) {
                if(passwords[i].equals(password)) {
                    pointsLimit = Integer.MAX_VALUE;
                    fieldsLimit = Integer.MAX_VALUE;
                }
            }

            //count fields
            int countFields = 0;
            int p = 0;
            while((p = fids.indexOf(',', p+1)) > 0) countFields++;

            //count points
            int countPoints = 0;
            p = 0;
            while((p = pointsString.indexOf(',', p+1)) > 0) countPoints++;

            if(countPoints/2 < pointsLimit && countFields < fieldsLimit) {
                BatchConsumer.start(layerIntersectDao);
                batchId = BatchProducer.produceBatch(BATCH_PATH,request.getRemoteAddr(),fids, pointsString);

                map.put("batchId", batchId);
                BatchProducer.addInfoToMap(BATCH_PATH, batchId, map);
                map.put("statusUrl", WS_INTERSECT_BATCH_STATUS.replace("{id}", batchId));

                return map;
            } else {
                map.put("error","too many fields or points");
                return map;
            }

//            ArrayList<String> sample = layerIntersectDao.sampling(fids, pointsString);
//
//            //setup output stream
//            OutputStream os = response.getOutputStream();
//            GZIPOutputStream gzip = new GZIPOutputStream(os);
//
//            IntersectUtil.writeSampleToStream(fids.split(","), pointsString.split(","), sample, gzip);
//
//            gzip.flush();
//            gzip.close();
//            os.flush();
//            os.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        map.put("error", "failed to create new batch");
        return map;
    }

    @RequestMapping(value = WS_INTERSECT_BATCH_STATUS, method = RequestMethod.GET)
    public Map batchStatus(
            @PathVariable("id") String id,
            HttpServletRequest request, HttpServletResponse response) {
        Map map = new HashMap();
        try {
            BatchProducer.addInfoToMap(BATCH_PATH, id, map);
            if(map.get("finished") != null) {
                map.put("downloadUrl", WS_INTERSECT_BATCH_DOWNLOAD.replace("{id}", id));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return map;
    }

    @RequestMapping(value = WS_INTERSECT_BATCH_DOWNLOAD, method = RequestMethod.GET)
    public void batchDownload(
            @PathVariable("id") String id,
            HttpServletRequest request, HttpServletResponse response) {
        try {
            Map map = new HashMap();
            BatchProducer.addInfoToMap(BATCH_PATH, id, map);
            if(map.get("finished") != null) {
                OutputStream os = response.getOutputStream();
                FileInputStream fis = new FileInputStream(BATCH_PATH + File.separator + id + File.separator + "sample.csv.gz");
                byte [] buffer = new byte[409600];
                int size;
                while((size = fis.read(buffer)) > 0) {
                    os.write(buffer, 0, size);
                }
                fis.close();
                os.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return;
    }
}
