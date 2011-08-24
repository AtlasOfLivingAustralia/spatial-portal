package org.ala.layers.web;

import java.sql.ResultSet;
import javax.servlet.http.HttpServletRequest;
import org.ala.layers.util.DBConnection;
import org.ala.layers.util.Utils;
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
public class ObjectsService {
     /**This method returns all objects associated with a field
     * 
     * @param id
     * @param req
     * @return 
     */
    @RequestMapping(value = "/objects/{id}", method = RequestMethod.GET)
    public
    @ResponseBody
    String fieldObjects(@PathVariable("id") String id, HttpServletRequest req) {
        String query = "SELECT pid, id, name, \"desc\" FROM objects WHERE fid='" + id + "';";
        ResultSet r = DBConnection.query(query);
        return Utils.resultSetToJSON(r);   
    }
    
    /**This method returns a single object, provided a UUID
     * 
     * @param uuid
     * @param req
     * @return 
     */
    @RequestMapping(value = "/object/{pid}", method = RequestMethod.GET)
    public
    @ResponseBody
    String fieldObject(@PathVariable("pid") String pid, HttpServletRequest req) {
        String query = "SELECT pid, id, name, \"desc\" FROM objects WHERE pid='" + pid + "';";
        ResultSet r = DBConnection.query(query);
        return Utils.resultSetToJSON(r);   
    }    
}
