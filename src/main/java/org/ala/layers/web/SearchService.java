package org.ala.layers.web;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.sql.ResultSet;
import javax.servlet.http.HttpServletRequest;
import org.ala.layers.util.DBConnection;
import org.ala.layers.util.Utils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.apache.log4j.Logger;


/**
 *
 * @author Adam
 */
@Controller
public class SearchService {
    
    /**
     * Log4j instance
     */
    protected Logger logger = Logger.getLogger(this.getClass());
    
    /*
     * perform a search operation
     */
    @RequestMapping(value = "/search", method = RequestMethod.GET)
    public
    @ResponseBody
    String search(HttpServletRequest req) {
        String q = req.getParameter("q");
        String types = req.getParameter("types");

        if(q == null) {
            return "";
        }
        try {
            q = URLDecoder.decode(q, "UTF-8");
            q = q.trim().toLowerCase();
            //TODO: check q

        } catch (UnsupportedEncodingException ex) {
            logger.error("An error has occurred constructing search term.");
            logger.error(ExceptionUtils.getFullStackTrace(ex));
        }
        if(types != null) {
            try {
                types = URLDecoder.decode(types, "UTF-8");
            } catch (UnsupportedEncodingException ex) {
                logger.error("An error has occurred constructing search term.");
                logger.error(ExceptionUtils.getFullStackTrace(ex));
            }
        }
        
        StringBuilder sb = new StringBuilder();

        String query = "SELECT * FROM searchobjects('%" + q + "%',20)";
        logger.info("Executing query " + query);
        
        ResultSet rs = DBConnection.query(query);
        sb.append(Utils.resultSetToJSON(rs));
        return sb.toString();
    }
}
