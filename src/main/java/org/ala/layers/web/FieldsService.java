package org.ala.layers.web;

import java.sql.ResultSet;
import javax.servlet.http.HttpServletRequest;
import org.ala.layers.util.DBConnection;
import org.ala.layers.util.Utils;
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
public class FieldsService {
    
    /**
     * Log4j instance
     */
    protected Logger logger = Logger.getLogger(this.getClass());
    
    /*
     * list fields table
     */
    

    @RequestMapping(value = "/fields", method = RequestMethod.GET)
    public
    @ResponseBody
    String listFields(HttpServletRequest req) {

        String query = "SELECT * FROM fields WHERE enabled=TRUE;";

        ResultSet r = DBConnection.query(query);

        return Utils.resultSetToJSON(r);
    }

    /*
     * list fields table with db only records
     */
    @RequestMapping(value = "/fieldsdb", method = RequestMethod.GET)
    public
    @ResponseBody
    String listFieldsDBOnly(HttpServletRequest req) {

        String query = "SELECT * FROM fields WHERE enabled=TRUE AND indb=TRUE;";

        ResultSet r = DBConnection.query(query);

        return Utils.resultSetToJSON(r);
    }

    /*
     * one fields table record
     */
    @RequestMapping(value = "/field/{id}", method = RequestMethod.GET)
    public
    @ResponseBody
    String oneField(@PathVariable("id") String id, HttpServletRequest req) {
        logger.info("calling /field/" + id);
        //test field id value
        int len = Math.min(6, id.length());
        id = id.substring(0, len);
        char prefix = id.toUpperCase().charAt(0);
        String number = id.substring(2, len);
        boolean numberOk = false;
        try {
            int i = Integer.parseInt(number);
            numberOk = true;
        } catch (Exception e) {
            logger.error("An error has occurred");
            logger.error(ExceptionUtils.getFullStackTrace(e));
        }

        //query
        if (prefix <= 'Z' && prefix >= 'A' && numberOk) {
            //Adam: not sure if this was correct
            //String query = "SELECT pid, id, name, \"desc\" FROM objects WHERE fid='" + id + "';";
            String query = "SELECT * from fields WHERE enabled=TRUE and id = '" + id + "';";
            logger.debug("Executing sql: " + query);
            ResultSet r = DBConnection.query(query);
            return Utils.resultSetToJSON(r);         
        } else {
            //error
            return null;
        }
    }  
}
