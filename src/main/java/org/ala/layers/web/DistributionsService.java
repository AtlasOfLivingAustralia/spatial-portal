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

import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

import org.ala.layers.dao.DistributionDAO;
import org.ala.layers.dao.ObjectDAO;
import org.ala.layers.dto.Distribution;
import org.ala.layers.dto.Facet;
import org.ala.layers.dto.Objects;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 
 * @author Adam
 */
@Controller
public class DistributionsService {

    private final String WS_DISTRIBUTIONS = "/distributions";
    private final String WS_DISTRIBUTIONS_COUNTS = "/distributions/counts";
    private final String WS_DISTRIBUTIONS_RADIUS = "/distributions/radius";
    private final String WS_DISTRIBUTIONS_RADIUS_COUNTS = "/distributions/radius/counts";
    private final String WS_DISTRIBUTION_ID = "/distribution/{spcode}";
    private final String WS_DISTRIBUTION_LSID = "/distribution/lsid/{lsid:.+}";
    private final String WS_DISTRIBUTION_OVERVIEWMAP = "/distribution/map/{lsid:.+}";
    private final String WS_DISTRIBUTION_OUTLIERS = "/distribution/outliers/{lsid:.+}";

    /** Log4j instance */
    protected Logger logger = Logger.getLogger(this.getClass());

    @Resource(name = "distributionDao")
    private DistributionDAO distributionDao;

    @Resource(name = "objectDao")
    private ObjectDAO objectDao;

    /*
     * list distribution table records, GET
     */
    @RequestMapping(value = WS_DISTRIBUTIONS, method = { RequestMethod.GET, RequestMethod.POST })
    public @ResponseBody
    List<Distribution> listDistributionsGet(@RequestParam(value = "wkt", required = false, defaultValue = "") String wkt,
            @RequestParam(value = "min_depth", required = false, defaultValue = "-1") Double min_depth, @RequestParam(value = "max_depth", required = false, defaultValue = "-1") Double max_depth,
            @RequestParam(value = "lsids", required = false, defaultValue = "") String lsids, @RequestParam(value = "geom_idx", required = false, defaultValue = "-1") Integer geom_idx,
            @RequestParam(value = "fid", required = false) String fid, @RequestParam(value = "objectName", required = false) String objectName,
            @RequestParam(value = "pelagic", required = false) Boolean pelagic, @RequestParam(value = "coastal", required = false) Boolean coastal,
            @RequestParam(value = "estuarine", required = false) Boolean estuarine, @RequestParam(value = "desmersal", required = false) Boolean desmersal,
            @RequestParam(value = "groupName", required = false) String groupName, @RequestParam(value = "family", required = false) String[] families,
            @RequestParam(value = "familyLsid", required = false) String[] familyLsids, @RequestParam(value = "genus", required = false) String[] genera,
            @RequestParam(value = "genusLsid", required = false) String[] generaLsids, HttpServletResponse response) {

        if (StringUtils.isEmpty(wkt) && fid != null && objectName != null) {
            List<Objects> objects = objectDao.getObjectByFidAndName(fid, objectName);
            // TODO this might be better served with a stored proc
            // so that the polygon isnt passed from DB to java
            wkt = objects.get(0).getGeometry();
            if (wkt == null) {
                logger.info("Unmatched geometry for name: " + objectName + " and layer " + fid);
                try {
                    response.sendError(400);
                } catch (Exception e) {
                    logger.error("Error sending response code 400 to client.");
                }
                return null;
            }
        }
        return distributionDao.queryDistributions(wkt, min_depth, max_depth, pelagic, coastal, estuarine, desmersal, groupName, geom_idx, lsids, families, familyLsids, genera, generaLsids,
                Distribution.EXPERT_DISTRIBUTION);
    }
    
    /*
     * list distribution table records, GET
     */
    @RequestMapping(value = WS_DISTRIBUTIONS_COUNTS, method = { RequestMethod.GET, RequestMethod.POST })
    public @ResponseBody
    List<Facet> listDistributionsGetCounts(@RequestParam(value = "wkt", required = false, defaultValue = "") String wkt,
            @RequestParam(value = "min_depth", required = false, defaultValue = "-1") Double min_depth, @RequestParam(value = "max_depth", required = false, defaultValue = "-1") Double max_depth,
            @RequestParam(value = "lsids", required = false, defaultValue = "") String lsids, @RequestParam(value = "geom_idx", required = false, defaultValue = "-1") Integer geom_idx,
            @RequestParam(value = "fid", required = false) String fid, @RequestParam(value = "objectName", required = false) String objectName,
            @RequestParam(value = "pelagic", required = false) Boolean pelagic, @RequestParam(value = "coastal", required = false) Boolean coastal,
            @RequestParam(value = "estuarine", required = false) Boolean estuarine, @RequestParam(value = "desmersal", required = false) Boolean desmersal,
            @RequestParam(value = "groupName", required = false) String groupName, @RequestParam(value = "family", required = false) String[] families,
            @RequestParam(value = "familyLsid", required = false) String[] familyLsids, @RequestParam(value = "genus", required = false) String[] genera,
            @RequestParam(value = "genusLsid", required = false) String[] generaLsids, HttpServletResponse response) {

        if (StringUtils.isEmpty(wkt) && fid != null && objectName != null) {
            List<Objects> objects = objectDao.getObjectByFidAndName(fid, objectName);
            // TODO this might be better served with a stored proc
            // so that the polygon isn't passed from DB to java
            wkt = objects.get(0).getGeometry();
            if (wkt == null) {
                logger.info("Unmatched geometry for name: " + objectName + " and layer " + fid);
                try {
                    response.sendError(400);
                } catch (Exception e) {
                    logger.error("Error sending response code 400 to client.");
                }
                return null;
            }
        }
        return distributionDao.queryDistributionsFamilyCounts(wkt, min_depth, max_depth, pelagic, coastal, estuarine, desmersal, groupName, geom_idx, lsids, families, familyLsids, genera, generaLsids,
                Distribution.EXPERT_DISTRIBUTION);
    }    

    @RequestMapping(value = WS_DISTRIBUTIONS_RADIUS, method = { RequestMethod.GET, RequestMethod.POST })
    public @ResponseBody
    List<Distribution> listDistributionsForRadiusGet(@RequestParam(value = "min_depth", required = false, defaultValue = "-1") Double min_depth,
            @RequestParam(value = "max_depth", required = false, defaultValue = "-1") Double max_depth, @RequestParam(value = "lsids", required = false, defaultValue = "") String lsids,
            @RequestParam(value = "geom_idx", required = false, defaultValue = "-1") Integer geom_idx, @RequestParam(value = "lon", required = true) Float longitude,
            @RequestParam(value = "lat", required = true) Float latitude, @RequestParam(value = "radius", required = true) Float radius,
            @RequestParam(value = "pelagic", required = false) Boolean pelagic, @RequestParam(value = "coastal", required = false) Boolean coastal,
            @RequestParam(value = "estuarine", required = false) Boolean estuarine, @RequestParam(value = "desmersal", required = false) Boolean desmersal,
            @RequestParam(value = "groupName", required = false) String groupName, @RequestParam(value = "family", required = false) String[] families,
            @RequestParam(value = "familyLsid", required = false) String[] familyLsids, @RequestParam(value = "genus", required = false) String[] genera,
            @RequestParam(value = "genusLsid", required = false) String[] generaLsids) {
        return distributionDao.queryDistributionsByRadius(longitude, latitude, radius, min_depth, max_depth, pelagic, coastal, estuarine, desmersal, groupName, geom_idx, lsids, families, familyLsids,
                genera, generaLsids, Distribution.EXPERT_DISTRIBUTION);
    }

    @RequestMapping(value = WS_DISTRIBUTIONS_RADIUS_COUNTS, method = { RequestMethod.GET, RequestMethod.POST })
    public @ResponseBody
    List<Facet> listDistributionCountsForRadiusGet(@RequestParam(value = "min_depth", required = false, defaultValue = "-1") Double min_depth,
            @RequestParam(value = "max_depth", required = false, defaultValue = "-1") Double max_depth, @RequestParam(value = "lsids", required = false, defaultValue = "") String lsids,
            @RequestParam(value = "geom_idx", required = false, defaultValue = "-1") Integer geom_idx, @RequestParam(value = "lon", required = true) Float longitude,
            @RequestParam(value = "lat", required = true) Float latitude, @RequestParam(value = "radius", required = true) Float radius,
            @RequestParam(value = "pelagic", required = false) Boolean pelagic, @RequestParam(value = "coastal", required = false) Boolean coastal,
            @RequestParam(value = "estuarine", required = false) Boolean estuarine, @RequestParam(value = "desmersal", required = false) Boolean desmersal,
            @RequestParam(value = "groupName", required = false) String groupName, @RequestParam(value = "family", required = false) String[] families,
            @RequestParam(value = "familyLsid", required = false) String[] familyLsids, @RequestParam(value = "genus", required = false) String[] genera,
            @RequestParam(value = "genusLsid", required = false) String[] generaLsids) {
        return distributionDao.queryDistributionsByRadiusFamilyCounts(longitude, latitude, radius, min_depth, max_depth, pelagic, coastal, estuarine, desmersal, groupName, geom_idx, lsids, families, familyLsids,
                genera, generaLsids, Distribution.EXPERT_DISTRIBUTION);
    }    
    
    /*
     * get distribution by id
     */
    @RequestMapping(value = WS_DISTRIBUTION_ID, method = RequestMethod.GET)
    public @ResponseBody
    Distribution getDistribution(@PathVariable Long spcode) {
        return distributionDao.getDistributionBySpcode(spcode, Distribution.EXPERT_DISTRIBUTION);
    }

    /*
     * get distribution by id
     */
    @RequestMapping(value = WS_DISTRIBUTION_LSID, method = RequestMethod.GET)
    public @ResponseBody
    Distribution getDistribution(@PathVariable String lsid, HttpServletResponse response) throws Exception {
        List<Distribution> distributions = distributionDao.getDistributionByLSID(new String[] { lsid });
        if (distributions != null && !distributions.isEmpty()) {
            return distributions.get(0);
        } else {
            response.sendError(404);
            return null;
        }
    }

    /*
     * get distribution by id
     */
    @RequestMapping(value = WS_DISTRIBUTION_OVERVIEWMAP, method = RequestMethod.GET)
    public void getDistributionOverviewMap(@PathVariable String lsid, @RequestParam(value = "height", required = false, defaultValue = "504") Integer height,
            @RequestParam(value = "width", required = false, defaultValue = "512") Integer width, HttpServletResponse response) throws Exception {
        List<Distribution> distributions = distributionDao.getDistributionByLSID(new String[] { lsid });
        if (distributions != null && !distributions.isEmpty()) {
            Distribution distribution = distributions.get(0);
            logger.info("Sending redirect to WMS request for: " + lsid);
            response.sendRedirect("http://spatial.ala.org.au/geoserver/ALA/wms?service=WMS&version=1.1.0&request=GetMap" + "&layers=ALA:aus1,ALA:distributions&styles="
                    + "&bbox=112.911,-54.778,159.113,-9.221" + "&width=" + width + "&height=" + height + "&srs=EPSG:4326" + "&format=image/png" + "&viewparams=s:" + distribution.getGeom_idx());
        } else {
            logger.info("Unrecognised LSID for distribution: " + lsid);
            response.sendError(404);
        }
    }

    /**
     * For a given set of points and an lsid, identify the points which do not
     * fall within the expert distribution associated with the lsid.
     * 
     * @param lsid
     *            the lsid associated with the expert distribution
     * @param pointsJson
     *            the points to test in JSON format. This must be a map whose
     *            keys are point ids (strings - typically these will be
     *            occurrence record ids). The values are maps containing the
     *            point's decimal latitude (with key "decimalLatitude") and
     *            decimal longitude (with key "decimalLongitude"). The decimal
     *            latitude and longitude values must be numbers.
     * @param response
     *            the http response
     * @return A map containing the distance outside the expert distribution for
     *         each point which falls outside the area defined by the
     *         distribution. Keys are point ids, values are the distances
     * 
     * @throws Exception
     */
    @RequestMapping(value = WS_DISTRIBUTION_OUTLIERS, method = RequestMethod.POST)
    public @ResponseBody
    Map<String, Double> getDistributionOutliers(@PathVariable String lsid, @RequestParam(value = "pointsJson", required = true) String pointsJson, HttpServletResponse response) throws Exception {
        JSONParser parser = new JSONParser();
        try {
            Map<String, Map<String, Double>> pointsMap = (Map<String, Map<String, Double>>) parser.parse(pointsJson);
            try {
                Map<String, Double> outlierDistances = distributionDao.identifyOutlierPointsForDistribution(lsid, pointsMap);
                return outlierDistances;
            } catch (IllegalArgumentException ex) {
                response.sendError(400, "No expert distribution for species associated with supplied lsid");
                return null;
            }
        } catch (ParseException ex) {
            response.sendError(400, "Invalid JSON for point information");
            return null;
        } catch (ClassCastException ex) {
            response.sendError(400, "Invalid format for point information");
            return null;
        }
    }
}
