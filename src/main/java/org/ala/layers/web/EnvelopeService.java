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

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.ala.layers.dao.LayerIntersectDAO;
import org.ala.layers.dto.IntersectionFile;
import org.ala.layers.grid.Envelope;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author Adam
 */
@Controller
public class EnvelopeService {
    private final String WS_ENVELOPE_WKT = "/envelope/{params:.+}";

    /**
     * Log4j instance
     */
    protected Logger logger = Logger.getLogger(this.getClass());
    @Resource(name = "layerIntersectDao")
    private LayerIntersectDAO layerIntersectDao;

    /*
     * return intersection of a point on layers(s)
     */
    @RequestMapping(value = WS_ENVELOPE_WKT, method = RequestMethod.GET)
    public
    @ResponseBody
    Object wkt(@PathVariable("params") String params, HttpServletRequest req) {
        StringBuilder sb = new StringBuilder();
        String[] p = params.split(",");
        for (int i = 0; i < p.length; i += 3) {
            IntersectionFile f = layerIntersectDao.getConfig().getIntersectionFile(p[i]);
            if (f != null) {
                if (sb.length() > 0) {
                    sb.append(",");
                }
                sb.append(f.getFilePath())
                        .append(",").append(p[i + 1])
                        .append(",").append(p[i + 2]);
            }
        }
        if (sb.length() > 0) {
            return Envelope.getGridEnvelopeAsWkt(sb.toString());
        } else {
            return "";
        }
    }
}
