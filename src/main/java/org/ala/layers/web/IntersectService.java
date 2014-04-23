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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
import org.ala.layers.intersect.IntersectConfig;
import org.ala.layers.util.BatchConsumer;
import org.ala.layers.util.BatchProducer;
import org.ala.layers.util.IntersectUtil;
import org.ala.layers.util.SpatialConversionUtils;
import org.ala.layers.util.UserProperties;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author ajay
 */
@Controller
public class IntersectService {
    private final String WS_INTERSECT_SINGLE = "/intersect/{ids}/{lat}/{lng:.+}";
    private final String WS_INTERSECT_BATCH = "/intersect/batch";
    private final String WS_INTERSECT_BATCH_STATUS = "/intersect/batch/{id}";
    private final String WS_INTERSECT_BATCH_DOWNLOAD = "/intersect/batch/download/{id}";
    private final String WS_INTERSECT_RELOAD_CONFIG = "/intersect/reloadconfig";

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
    private Properties userProperties = (new UserProperties()).getProperties();

    /*
     * return intersection of a point on layers(s)
     */
    // @ResponseBody String
    @RequestMapping(value = WS_INTERSECT_SINGLE, method = RequestMethod.GET)
    public
    @ResponseBody
    Object single(@PathVariable("ids") String ids, @PathVariable("lat") Double lat, @PathVariable("lng") Double lng, HttpServletRequest req) {
        return layerIntersectDao.samplingFull(ids, lng, lat);
    }

    /**
     * Clean up and just return an int for LAYER object
     *
     * @param id
     * @return
     */
    private int cleanObjectId(String id) {
        // test field id value
        int len = Math.min(6, id.length());
        id = id.substring(0, len);
        char prefix = id.toUpperCase().charAt(0);
        String number = id.substring(2, len);
        try {
            int i = Integer.parseInt(number);
            return i;
        } catch (Exception e) {
        }

        return -1;
    }

    @RequestMapping(value = WS_INTERSECT_BATCH, method = {RequestMethod.GET, RequestMethod.POST})
    @ResponseBody
    public Map batch(@RequestParam(value = "fids", required = false, defaultValue = "") String fids, @RequestParam(value = "points", required = false, defaultValue = "") String pointsString,
                     HttpServletRequest request, HttpServletResponse response) {

        Map map = new HashMap();
        String batchId = null;
        try {

            // get limits
            int pointsLimit, fieldsLimit;

            String[] passwords = userProperties.getProperty("batch_sampling_passwords").split(",");
            pointsLimit = Integer.parseInt(userProperties.getProperty("batch_sampling_points_limit"));
            fieldsLimit = Integer.parseInt(userProperties.getProperty("batch_sampling_fields_limit"));

            String password = request.getParameter("pw");
            for (int i = 0; password != null && i < passwords.length; i++) {
                if (passwords[i].equals(password)) {
                    pointsLimit = Integer.MAX_VALUE;
                    fieldsLimit = Integer.MAX_VALUE;
                }
            }

            // count fields
            int countFields = 1;
            int p = 0;
            while ((p = fids.indexOf(',', p + 1)) > 0)
                countFields++;

            // count points
            int countPoints = 1;
            p = 0;
            while ((p = pointsString.indexOf(',', p + 1)) > 0)
                countPoints++;

            if (countPoints / 2 > pointsLimit) {
                map.put("error", "Too many points.  Maximum is " + pointsLimit);
            } else if (countFields > fieldsLimit) {
                map.put("error", "Too many fields.  Maximum is " + fieldsLimit);
            } else {
                BatchConsumer.start(layerIntersectDao);
                batchId = BatchProducer.produceBatch(userProperties.getProperty("batch_path"), "request address:" + request.getRemoteAddr(), fids, pointsString);

                map.put("batchId", batchId);
                BatchProducer.addInfoToMap(userProperties.getProperty("batch_path"), batchId, map);
                map.put("statusUrl", userProperties.getProperty("layers_service_url") + WS_INTERSECT_BATCH_STATUS.replace("{id}", batchId));
            }

            return map;
        } catch (Exception e) {
            e.printStackTrace();
        }

        map.put("error", "failed to create new batch");
        return map;
    }

    @RequestMapping(value = WS_INTERSECT_BATCH_STATUS, method = RequestMethod.GET)
    @ResponseBody
    public Map batchStatus(@PathVariable("id") String id, HttpServletRequest request, HttpServletResponse response) {

        Map map = new HashMap();
        try {
            BatchProducer.addInfoToMap(userProperties.getProperty("batch_path"), id, map);
            if (map.get("finished") != null) {
                map.put("downloadUrl", userProperties.getProperty("layers_service_url") + WS_INTERSECT_BATCH_DOWNLOAD.replace("{id}", id));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return map;
    }

    @RequestMapping(value = WS_INTERSECT_BATCH_DOWNLOAD, method = RequestMethod.GET)
    public void batchDownload(@PathVariable("id") String id, HttpServletRequest request, HttpServletResponse response) {

        try {
            Map map = new HashMap();
            BatchProducer.addInfoToMap(userProperties.getProperty("batch_path"), id, map);
            if (map.get("finished") != null) {
                OutputStream os = response.getOutputStream();
                FileInputStream fis = new FileInputStream(userProperties.getProperty("batch_path") + File.separator + id + File.separator + "sample.zip");
                byte[] buffer = new byte[4096];
                int size;
                while ((size = fis.read(buffer)) > 0) {
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

    @RequestMapping(value = WS_INTERSECT_RELOAD_CONFIG, method = RequestMethod.GET)
    @ResponseBody
    public Map reloadConfig(@RequestParam(value = "p", required = false, defaultValue = "") String p, HttpServletRequest request) {
        Map map = new HashMap();

        if (userProperties == null || !userProperties.containsKey("reload_config_password") || userProperties.getProperty("reload_config_password").equals(p)) {
            map.put("result", "authorised");

            (new UserProperties()).getProperties();
            layerIntersectDao.reload();
            map.put("layerIntersectDao", "successful");
        } else {
            map.put("result", "not authorised");
        }

        return map;
    }

    // Point radius intersect
    @RequestMapping(value = "/intersect/pointradius/{fid}/{lat}/{lng}/{radius}", method = RequestMethod.GET)
    @ResponseBody
    public List<Objects> pointRadiusIntersect(@PathVariable("fid") String fid, @PathVariable("lat") Double lat, @PathVariable("lng") Double lng, @PathVariable("radius") Double radiusKm) throws Exception {
        return objectDao.getObjectsWithinRadius(fid, lat, lng, radiusKm);
    }

    // WKT geometry intersect
    @RequestMapping(value = "/intersect/wkt/{fid}", method = RequestMethod.POST)
    @ResponseBody
    public List<Objects> wktGeometryIntersect(@RequestBody String wkt, @PathVariable("fid") String fid) throws Exception {
        return objectDao.getObjectsIntersectingWithGeometry(fid, wkt);
    }

    // geojson geometry intersect
    @RequestMapping(value = "/intersect/geojson/{fid}", method = RequestMethod.POST)
    @ResponseBody
    public List<Objects> geojsonGeometryIntersect(@RequestBody String geoJson, @PathVariable("fid") String fid) throws Exception {
        String wkt = SpatialConversionUtils.geoJsonToWkt(geoJson);
        return objectDao.getObjectsIntersectingWithGeometry(fid, wkt);
    }

    // Object intersect
    @RequestMapping(value = "/intersect/object/{fid}/{pid}", method = RequestMethod.GET)
    @ResponseBody
    public List<Objects> objectIntersect(@PathVariable("fid") String fid, @PathVariable("pid") int pid) throws Exception {
        return objectDao.getObjectsIntersectingWithObject(fid, Integer.toString(pid));
    }

    // Point radius intersect
    @RequestMapping(value = "/intersect/poi/pointradius/{lat}/{lng}/{radius}", method = RequestMethod.GET)
    @ResponseBody
    public List<Map<String, Object>> poiPointRadiusIntersect(@PathVariable("lat") Double lat, @PathVariable("lng") Double lng, @PathVariable("radius") Double radiusKm) throws Exception {
        return objectDao.getPointsOfInterestWithinRadius(lat, lng, radiusKm);
    }


    // WKT geometry intersect
    @RequestMapping(value = "/intersect/poi/wkt", method = {RequestMethod.GET, RequestMethod.POST})
    @ResponseBody
    public List<Map<String, Object>> wktPoiIntersectGet(@RequestParam(value = "wkt", required = true, defaultValue = "") String wkt) throws Exception {
        return objectDao.pointsOfInterestGeometryIntersect(wkt);
    }

    // geojson geometry intersect
    @RequestMapping(value = "/intersect/poi/geojson", method = {RequestMethod.GET, RequestMethod.POST})
    @ResponseBody
    public List<Map<String, Object>> geojsonPoiIntersectGet(@RequestParam(value = "geojson", required = true, defaultValue = "") String geojson) throws Exception {
        String wkt = SpatialConversionUtils.geoJsonToWkt(geojson);
        return objectDao.pointsOfInterestGeometryIntersect(wkt);
    }

    // Object intersect
    @RequestMapping(value = "/intersect/poi/object/{pid}", method = RequestMethod.GET)
    @ResponseBody
    public List<Map<String, Object>> objectPoiIntersect(@PathVariable("pid") int pid) throws Exception {
        return objectDao.pointsOfInterestObjectIntersect(Integer.toString(pid));
    }

}
