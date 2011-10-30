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
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import org.ala.layers.dao.DistributionDAO;
import org.ala.layers.dto.Distribution;
import org.ala.layers.dto.Field;
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
    private final String WS_DISTRIBUTION_ID = "/distribution/{spcode}";

    /**
     * Log4j instance
     */
    protected Logger logger = Logger.getLogger(this.getClass());

    @Resource(name="distributionDao")
    private DistributionDAO distributionDao;

    /*
     * list distribution table records, GET
     */
    @RequestMapping(value = WS_DISTRIBUTIONS, method = RequestMethod.GET)
    public @ResponseBody List<Distribution> listDistributionsGet(
            @RequestParam(value = "wkt", required = false, defaultValue = "") String wkt,
            @RequestParam(value = "min_depth", required = false, defaultValue = "-1") Double min_depth,
            @RequestParam(value = "max_depth", required = false, defaultValue = "-1") Double max_depth,
            @RequestParam(value = "lsids", required = false, defaultValue = "") String lsids,
            HttpServletRequest req) {

        return distributionDao.queryDistributions(wkt, min_depth, max_depth, lsids);
    }

    /*
     * list distribution table records, POST
     */
    @RequestMapping(value = WS_DISTRIBUTIONS, method = RequestMethod.POST)
    public @ResponseBody List<Distribution> listDistributionsPost(
            @RequestParam(value = "wkt", required = false, defaultValue = "") String wkt,
            @RequestParam(value = "min_depth", required = false, defaultValue = "-1") Double min_depth,
            @RequestParam(value = "max_depth", required = false, defaultValue = "-1") Double max_depth,
            @RequestParam(value = "lsids", required = false, defaultValue = "") String lsids,
            HttpServletRequest req) {

        return distributionDao.queryDistributions(wkt, min_depth, max_depth, lsids);
    }

    /*
     * get distribution by id
     */
    @RequestMapping(value = WS_DISTRIBUTION_ID, method = RequestMethod.GET)
    public @ResponseBody Distribution getDistribution(@PathVariable Long spcode,
            HttpServletRequest req) {

        return distributionDao.getDistributionBySpcode(spcode);
    }
}
