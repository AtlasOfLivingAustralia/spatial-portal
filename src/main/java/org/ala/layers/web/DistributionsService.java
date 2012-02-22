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

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ala.layers.dao.DistributionDAO;
import org.ala.layers.dao.ObjectDAO;
import org.ala.layers.dto.Distribution;
import org.ala.layers.dto.Field;
import org.ala.layers.dto.Objects;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.log4j.Logger;
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
    private final String WS_DISTRIBUTIONS_POST = "/distributions/post";
    private final String WS_DISTRIBUTIONS_INTERSECTS = "/distributions/intersects";
    private final String WS_DISTRIBUTIONS_RADIUS = "/distributions/radius";
    private final String WS_DISTRIBUTION_ID = "/distribution/{spcode}";
    private final String WS_DISTRIBUTION_LSID = "/distribution/lsid/{lsid}";

    /**
     * Log4j instance
     */
    protected Logger logger = Logger.getLogger(this.getClass());

    @Resource(name="distributionDao")
    private DistributionDAO distributionDao;

    @Resource(name="objectDao")
    private ObjectDAO objectDao;

    /*
     * list distribution table records, GET
     */
    @RequestMapping(value = WS_DISTRIBUTIONS, method = {RequestMethod.GET, RequestMethod.POST} )
    public @ResponseBody List<Distribution> listDistributionsGet(
            @RequestParam(value = "wkt", required = false, defaultValue = "") String wkt,
            @RequestParam(value = "min_depth", required = false, defaultValue = "-1") Double min_depth,
            @RequestParam(value = "max_depth", required = false, defaultValue = "-1") Double max_depth,
            @RequestParam(value = "lsids", required = false, defaultValue = "") String lsids,
            @RequestParam(value = "geom_idx", required = false, defaultValue = "-1") Integer geom_idx,
            @RequestParam(value = "fid", required = false) String fid,
            @RequestParam(value = "objectName", required = false) String objectName,
            HttpServletResponse response) {

        if(StringUtils.isEmpty(wkt) && fid != null && objectName != null){
            List<Objects> objects = objectDao.getObjectByFidAndName(fid, objectName);
            //TODO this might be better served with a stored proc
            // so that the polygon isnt passed from DB to java
            wkt = objects.get(0).getGeometry();
            if(wkt == null){
                logger.info("Unmatched geometry for name: "+ objectName + " and layer " + fid);
                try {
                    response.sendError(400);
                } catch (Exception e){
                    logger.error("Error sending response code 400 to client.");
                }
                return null;
            }
        }
        return distributionDao.queryDistributions(wkt, min_depth, max_depth, geom_idx, lsids, Distribution.EXPERT_DISTRIBUTION);
    }

    @RequestMapping(value = WS_DISTRIBUTIONS_RADIUS, method = {RequestMethod.GET, RequestMethod.POST} )
    public @ResponseBody List<Distribution> listDistributionsForRadiusGet(
            @RequestParam(value = "min_depth", required = false, defaultValue = "-1") Double min_depth,
            @RequestParam(value = "max_depth", required = false, defaultValue = "-1") Double max_depth,
            @RequestParam(value = "lsids", required = false, defaultValue = "") String lsids,
            @RequestParam(value = "geom_idx", required = false, defaultValue = "-1") Integer geom_idx,
            @RequestParam(value = "lon", required = true) Float longitude,
            @RequestParam(value = "lat", required = true) Float latitude,
            @RequestParam(value = "radius", required = true) Float radius) {

        return distributionDao.queryDistributionsByRadius(longitude, latitude, radius, min_depth, max_depth, geom_idx, lsids, Distribution.EXPERT_DISTRIBUTION);
    }

    /*
     * get distribution by id
     */
    @RequestMapping(value = WS_DISTRIBUTION_ID, method = RequestMethod.GET)
    public @ResponseBody Distribution getDistribution(@PathVariable Long spcode,
            HttpServletRequest req) {
        return distributionDao.getDistributionBySpcode(spcode, Distribution.EXPERT_DISTRIBUTION);
    }

    /*
    * get distribution by id
    */
    @RequestMapping(value = WS_DISTRIBUTION_LSID, method = RequestMethod.GET)
    public @ResponseBody Distribution getDistribution(@PathVariable String taxonID, HttpServletResponse response) throws Exception{
        List<Distribution> distributions =  distributionDao.getDistributionByLSID(new String[]{taxonID});
        if(distributions != null && !distributions.isEmpty()) {
            return distributions.get(0);
        } else {
            response.sendError(404);
            return null;
        }
    }
}
