package org.ala.layers.web;

import javax.servlet.http.HttpServletRequest;
import org.ala.layers.util.Intersect;
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
public class IntersectService {

    /*
     * return intersection of a point on layers(s)
     */
    @RequestMapping(value = "/intersect/{ids}/{lat}/{lng}", method = RequestMethod.GET)
    public
    @ResponseBody
    String single(@PathVariable("ids") String ids, @PathVariable("lat") Double lat, @PathVariable("lng") Double lng, HttpServletRequest req) {
        return Intersect.Intersect(ids, lat, lng);
    }

    /**
     * Adam: Is this used anywhere yet?
     * @param id
     * @return 
     */
    private String cleanObjectId(String id) {
        return id.replaceAll("[^a-zA-Z0-9]:", "");
    }
}
