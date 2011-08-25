package org.ala.layers.web;

import java.sql.ResultSet;
import javax.servlet.http.HttpServletRequest;
import org.ala.layers.util.DBConnection;
import org.ala.layers.util.Utils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 *
 * @author jac24n
 */
@Controller
public class LayersService {
    /**
     * Log4j instance
     */
    protected Logger logger = Logger.getLogger(this.getClass());
     
    /**This method returns all layers
     * 
     * @param req
     * @return 
     */
    @RequestMapping(value = "/layers", method = RequestMethod.GET)
    public
    @ResponseBody
    String layerObjects(HttpServletRequest req) {
        String query = "SELECT * FROM layers WHERE enabled='TRUE';";
        ResultSet r = DBConnection.query(query);
        return Utils.resultSetToJSON(r);   
    }
    
    /**This method returns a single layer, provided an id
     * 
     * @param uuid
     * @param req
     * @return 
     */
    @RequestMapping(value = "/layer/{id}", method = RequestMethod.GET)
    public
    @ResponseBody
    String layerObject(@PathVariable("id") String id, HttpServletRequest req) {
        String query = "SELECT * FROM layers WHERE id='" + id + "';";
        ResultSet r = DBConnection.query(query);
        return Utils.resultSetToJSON(r);   
    }
    
    @RequestMapping(value = "/layers/grids", method = RequestMethod.GET)
    public
    @ResponseBody
    String gridsLayerObjects(HttpServletRequest req) {
        String query = "SELECT * FROM layers WHERE enabled='TRUE' and type='Environmental';";
        ResultSet r = DBConnection.query(query);
        return Utils.resultSetToJSON(r);   
    }
        
    @RequestMapping(value = "/layers/shapes", method = RequestMethod.GET)
    public
    @ResponseBody
    String shapesLayerObjects(HttpServletRequest req) {
        String query = "SELECT * FROM layers WHERE enabled='TRUE' and type='Contextual';";
        ResultSet r = DBConnection.query(query);
        return Utils.resultSetToJSON(r);
    }
    
}
