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

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKTWriter;
import java.io.ByteArrayOutputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.ala.layers.dao.ObjectDAO;
import org.ala.layers.dto.Objects;
import org.ala.layers.util.DBConnection;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.log4j.Logger;
import org.geotools.kml.KML;
import org.geotools.kml.KMLConfiguration;
import org.geotools.xml.Encoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 *
 * @author Adam
 */
@Controller
public class ShapesService {

    /**
     * Log4j instance
     */
    protected Logger logger = Logger.getLogger(this.getClass());
    private ObjectDAO objectDao;

    @Autowired
    public void setObjectsDao(ObjectDAO objectDao) {
        System.out.println("setting layer dao");
        this.objectDao = objectDao;
    }

    /*
     * return a shape as wkt
     */
    @RequestMapping(value = "/shape/wkt/{id}/adam", method = RequestMethod.GET)
    public
    @ResponseBody
    String wkt(@PathVariable("id") String id, HttpServletRequest req) {
        //validate object id
        id = cleanObjectId(id);

        String query = "SELECT ST_AsText(the_geom) FROM objects WHERE pid='" + id + "';";

        ResultSet r = DBConnection.query(query);

        String s = null;
        try {
            if (r != null && r.next()) {
                s = r.getString(1);
            }
        } catch (SQLException ex) {
            logger.error("An error has occurred retrieving wkt for object id " + id);
            logger.error(ExceptionUtils.getFullStackTrace(ex));
        }

        return s;



    }

    /*
     * return a shape as geojson
     */
    @RequestMapping(value = "/shape/geojson/{id}/adam", method = RequestMethod.GET)
    public
    @ResponseBody
    String geojson(@PathVariable("id") String id, HttpServletRequest req) {
        //validate object id
        id = cleanObjectId(id);

        String query = "SELECT ST_AsGeoJSON(the_geom) FROM objects WHERE pid='" + id + "';";

        ResultSet r = DBConnection.query(query);

        String s = null;
        try {
            if (r != null && r.next()) {
                s = r.getString(1);
            }
        } catch (SQLException ex) {
            logger.error("An error has occurred retrieving geojson for object id " + id);
            logger.error(ExceptionUtils.getFullStackTrace(ex));
        }

        return s;
    }

    /*
     * return a shape as kml
     */
    @RequestMapping(value = "/shape/kml/{id}/adam", method = RequestMethod.GET)
    public
    @ResponseBody
    String kml(@PathVariable("id") String id, HttpServletRequest req) {
        //validate object id
        id = cleanObjectId(id);

        String query = "SELECT ST_AsKml(the_geom) FROM objects WHERE pid='" + id + "';";

        ResultSet r = DBConnection.query(query);

        String s = null;
        try {
            if (r != null && r.next()) {
                s = r.getString(1);
            }
        } catch (SQLException ex) {
            logger.error("An error has occurred retrieving kml for object id " + id);
            logger.error(ExceptionUtils.getFullStackTrace(ex));

        }

        return s;
    }

    /*
     * return a shape as kml
     */
    @RequestMapping(value = "/shape/{type}/{id}", method = RequestMethod.GET)
    public
    @ResponseBody
    String findShape(@PathVariable("type") String type, @PathVariable("id") String id, HttpServletRequest req) {
        try {

            //validate object id
            id = cleanObjectId(id);

            List<Objects> objects = objectDao.getObjectsById(id);
            if (objects.size() > 0) {
                Geometry geom = objects.get(0).getGeometry();
                if (type.equalsIgnoreCase("wkt")) {
                    WKTWriter wkt = new WKTWriter();
                    return wkt.write(geom);
                } else if (type.equalsIgnoreCase("kml")) {
                    Encoder e = new Encoder(new KMLConfiguration());
                    e.setIndenting(true);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    e.encode(geom, KML.Geometry, baos);
                    String kmlGeometry = new String(baos.toByteArray());
                    return kmlGeometry.substring(kmlGeometry.indexOf('\n'));
                } else if (type.equalsIgnoreCase("geojson")) {
                    return "Not supported yet."; 
                }

            } else {
                return "";
            }

        } catch (Exception e) {
            logger.error("An error has occurred retrieving '" + type + "' for object id " + id);
            logger.error(ExceptionUtils.getFullStackTrace(e));
        }
        return "";
    }

    private String cleanObjectId(String id) {
        return id.replaceAll("[^a-zA-Z0-9]:", "");
    }
}
