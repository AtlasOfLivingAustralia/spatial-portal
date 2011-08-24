package org.ala.layers.web;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.sql.ResultSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.ala.layers.util.DBConnection;
import org.ala.layers.util.Utils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 *
 * @author Adam
 */
@Controller
public class SearchService {
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
            Logger.getLogger(SearchService.class.getName()).log(Level.SEVERE, null, ex);
        }
        if(types != null) {
            try {
                types = URLDecoder.decode(types, "UTF-8");
            } catch (UnsupportedEncodingException ex) {
                Logger.getLogger(SearchService.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        StringBuilder sb = new StringBuilder();

        String query = "SELECT * FROM searchobjects('%" + q + "%',20)";
        
        ResultSet rs = DBConnection.query(query);
        
        sb.append(Utils.resultSetToJSON(rs));

        return sb.toString();
    }
}
