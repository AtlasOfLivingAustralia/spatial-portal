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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.ala.layers.dao.ObjectDAO;
import org.ala.layers.dto.Distribution;
import org.ala.layers.dto.Objects;
import org.ala.layers.util.LayerFilter;
import org.ala.layers.util.SpatialConversionUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author jac24n
 */
@Controller
public class ObjectsService {

    /**
     * Log4j instance
     */
    protected Logger logger = Logger.getLogger(this.getClass());
    @Resource(name = "objectDao")
    private ObjectDAO objectDao;

    /**
     * This method returns all objects associated with a field
     *
     * @param id
     * @param req
     * @return
     */
    @RequestMapping(value = "/objects/{id}", method = RequestMethod.GET)
    public
    @ResponseBody
    List<Objects> fieldObjects(@PathVariable("id") String id, HttpServletRequest req) {
        return objectDao.getObjectsById(id);
    }

    /**
     * This method returns a single object, provided a UUID
     *
     * @param req
     * @return
     */
    @RequestMapping(value = "/object/{pid}", method = RequestMethod.GET)
    public
    @ResponseBody
    Objects fieldObject(@PathVariable("pid") String pid, HttpServletRequest req) {
//        String query = "SELECT pid, id, name, \"desc\" FROM objects WHERE pid='" + pid + "';";
//        ResultSet r = DBConnection.query(query);
//        return Utils.resultSetToJSON(r);
        return objectDao.getObjectByPid(pid);
    }

    /**
     * This method returns all objects associated with a field
     *
     * @param id
     * @param req
     * @return
     */
    @RequestMapping(value = "/objects/{id}/{lat}/{lng:.+}", method = RequestMethod.GET)
    public
    @ResponseBody
    List<Objects> fieldObjects(@PathVariable("id") String id,
                               @PathVariable("lat") Double lat, @PathVariable("lng") Double lng,
                               @RequestParam(value = "limit", required = false, defaultValue = "40") Integer limit,
                               HttpServletRequest req) {
        return objectDao.getNearestObjectByIdAndLocation(id, limit, lng, lat);
    }

    /**
     * This method returns all objects associated with a field
     *
     * @param id
     * @param req
     * @return
     */
    @RequestMapping(value = "/objects/inarea/{id}", method = {RequestMethod.GET, RequestMethod.POST})
    public
    @ResponseBody
    List<Objects> fieldObjects(@PathVariable("id") String id,
                               @RequestParam(value = "limit", required = false, defaultValue = "40") Integer limit,
                               HttpServletRequest req) throws UnsupportedEncodingException {
        String wkt = URLDecoder.decode(req.getParameter("wkt"), "UTF-8");
        if (wkt.startsWith("ENVELOPE(")) {
            //get results of each filter
            LayerFilter[] filters = LayerFilter.parseLayerFilters(wkt);
            List<List<Objects>> all = new ArrayList<List<Objects>>();
            for (int i = 0; i < filters.length; i++) {
                all.add(objectDao.getObjectsByIdAndIntersection(id, limit, filters[i]));
            }
            //merge common entries only
            HashMap<String, Integer> objectCounts = new HashMap<String, Integer>();
            List<Objects> list = all.get(0);
            for (int j = 0; j < list.size(); j++) {
                objectCounts.put(list.get(j).getPid(), 1);
            }
            for (int i = 1; i < all.size(); i++) {
                List<Objects> t = all.get(i);
                for (int j = 0; j < t.size(); j++) {
                    Integer v = objectCounts.get(t.get(j).getPid());
                    if (v != null) {
                        objectCounts.put(t.get(j).getPid(), v + 1);
                    }
                }
            }
            List<Objects> inAllGroups = new ArrayList<Objects>(list.size());
            for (int j = 0; j < list.size(); j++) {
                if (objectCounts.get(list.get(j).getPid()) == all.size()) {
                    inAllGroups.add(list.get(j));
                }
            }

            return inAllGroups;
        } else if (wkt.startsWith("OBJECT(")) {
            String pid = wkt.substring("OBJECT(".length(), wkt.length() - 1);
            return objectDao.getObjectsByIdAndIntersection(id, limit, pid);
        } else if (wkt.startsWith("GEOMETRYCOLLECTION")) {
            List<String> collectionParts = SpatialConversionUtils.getGeometryCollectionParts(wkt);

            Set<Objects> objectsSet = new HashSet<Objects>();

            for (String part : collectionParts) {
                objectsSet.addAll(objectDao.getObjectsByIdAndArea(id, limit, part));
            }

            return new ArrayList<Objects>(objectsSet);
        } else {
            return objectDao.getObjectsByIdAndArea(id, limit, wkt);
        }
    }
}
