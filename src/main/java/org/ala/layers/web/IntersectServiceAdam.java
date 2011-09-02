/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.ala.layers.web;

import java.io.File;
import java.sql.ResultSet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.ala.layers.util.DBConnection;
import org.ala.layers.util.Grid;
import org.ala.layers.util.Layer;
import org.ala.layers.util.Utils;
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
public class IntersectServiceAdam {

    final static String ALASPATIAL_OUTPUT_PATH = "/data/ala/runtime/output";
    final static String DATA_FILES_PATH = "/data/ala/data/envlayers/WorldClimCurrent/10minutes/";
    /*
     * return intersection of a point on a layer (field)
     */
    @RequestMapping(value = "/intersect/{ids}/{lat}/{lng}/adam", method = RequestMethod.GET)
    public
    @ResponseBody
    String single(@PathVariable("ids") String ids, @PathVariable("lat") Double lat, @PathVariable("lng") Double lng, HttpServletRequest req) {
        StringBuilder sb = new StringBuilder();

        sb.append("[");

        for(String id : ids.split(",")) {
            if(sb.length() > 1) {
                sb.append(",");
            }

            String s = "";

            Layer layer = Layer.getLayer(id);

            double [][] p = {{lng, lat}};

            if(layer != null) {
                if(layer.isShape()) {
                    String query = "SELECT fid, id, name as value, \"desc\", '"
                            + layer.getDisplayName()
                            + "' as layername FROM objects WHERE fid='cl"
                            + layer.getId()
                            + "' AND ST_Within(ST_Transform(ST_SETSRID(ST_Point("
                            + lng
                            + "," 
                            + lat
                            + "),4326),900913), the_geom);";

                    System.out.println(query);

                    ResultSet r = DBConnection.query(query);

                    s = Utils.resultSetToJSON(r);
                    s = s.substring(1,s.length() - 1);

                } else if (layer.isGrid()) {
                    Grid g = new Grid(DATA_FILES_PATH + layer.getPathOrig());

                    float [] v = g.getValues(p);

                    s = "{\"value\":" + v[0] + ",\"layername\":\"" + layer.getDisplayName() + "\"}";
                }       
            } else {
                String gid = null;
                String filename = null;
                String name = null;

                if(id.startsWith("species_")) {
                    //maxent layer
                    gid = id.substring(8);
                    filename = ALASPATIAL_OUTPUT_PATH + "/maxent/" + gid + "/" + gid;
                    name = "Prediction";
                } else if(id.startsWith("aloc_")) {
                    //aloc layer
                    gid = id.substring(8);
                    filename = ALASPATIAL_OUTPUT_PATH + "/aloc/" + gid + "/" + gid;
                    name = "Classification";
                }

                if (filename != null) {
                    Grid grid = new Grid(filename);

                    if(grid != null && (new File(filename + ".grd").exists()) ) {
                        float [] v = grid.getValues(p);
                        if(v != null) {
                            if(Float.isNaN(v[0])) {
                                s = "{\"value\":\"no data\",\"layername\":\"" + name + " (" + gid + ")\"}";
                            } else {
                                s = "{\"value\":" + v[0] + ",\"layername\":\"" + name + " (" + gid + ")\"}";
                            }
                        }
                    }
                }
            }
            
            sb.append(s);
        }

        sb.append("]");

        return sb.toString();
    }

    /*
     * return intersection of multiple points
     */
    @RequestMapping(value = "/intersect/batch/adam", method = RequestMethod.POST)
    public
    void single(HttpServletRequest req, HttpServletResponse res) {
        String [] ids = req.getParameter("fields").split(",");
        String [] latlngs = req.getParameter("points").split(",");
        String absences = req.getParameter("absences");

        double [][] points = new double[latlngs.length/2][2];
        for(int i=0;i<points.length/2;i++) {
            points[i][0] = Double.parseDouble(latlngs[i*2]);
            points[i][1] = Double.parseDouble(latlngs[i*2+1]);
        }

        //TODO... zip and return response
    }

    private String cleanObjectId(String id) {
        return id.replaceAll("[^a-zA-Z0-9]:", "");
    }
}
