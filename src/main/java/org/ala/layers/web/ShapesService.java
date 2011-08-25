package org.ala.layers.web;

import java.sql.ResultSet;
import java.sql.SQLException;
import javax.servlet.http.HttpServletRequest;
import org.ala.layers.util.DBConnection;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.log4j.Logger;
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
    
    /*
     * return a shape as wkt
     */
    @RequestMapping(value = "/shape/wkt/{id}", method = RequestMethod.GET)
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
    @RequestMapping(value = "/shape/geojson/{id}", method = RequestMethod.GET)
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
    @RequestMapping(value = "/shape/kml/{id}", method = RequestMethod.GET)
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

    private String cleanObjectId(String id) {
        return id.replaceAll("[^a-zA-Z0-9]:", "");
    }
}
