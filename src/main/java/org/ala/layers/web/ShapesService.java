/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.ala.layers.web;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.ala.layers.util.DBConnection;
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
    /*
     * return a shape as wkt
     */
    @RequestMapping(value = "/shape/wkt/{id}", method = RequestMethod.GET)
    public
    @ResponseBody
    String wkt(@PathVariable("id") String id, HttpServletRequest req) {
        //validate object id
        id = cleanObjectId(id);

        String query = "SELECT ST_AsText(ST_Transform(the_geom, 4326)) FROM objects WHERE pid='" + id + "';";

        ResultSet r = DBConnection.query(query);

        String s = null;
        try {
            if (r != null && r.next()) {
                s = r.getString(1);
            }
        } catch (SQLException ex) {
            Logger.getLogger(ShapesService.class.getName()).log(Level.SEVERE, null, ex);
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

        String query = "SELECT ST_AsGeoJSON(ST_Transform(the_geom, 4326)) FROM objects WHERE pid='" + id + "';";

        ResultSet r = DBConnection.query(query);

        String s = null;
        try {
            if (r != null && r.next()) {
                s = r.getString(1);
            }
        } catch (SQLException ex) {
            Logger.getLogger(ShapesService.class.getName()).log(Level.SEVERE, null, ex);
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

        String query = "SELECT ST_AsKml(ST_Transform(the_geom, 4326)) FROM objects WHERE pid='" + id + "';";

        ResultSet r = DBConnection.query(query);

        String s = null;
        try {
            if (r != null && r.next()) {
                s = r.getString(1);
            }
        } catch (SQLException ex) {
            Logger.getLogger(ShapesService.class.getName()).log(Level.SEVERE, null, ex);
        }

        return s;
    }

    private String cleanObjectId(String id) {
        return id.replaceAll("[^a-zA-Z0-9]:", "");
    }
}
