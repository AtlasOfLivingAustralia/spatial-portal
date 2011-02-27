package org.ala.spatial.web.services;

import java.net.URLDecoder;
import javax.servlet.http.HttpServletRequest;
import org.ala.spatial.analysis.index.LayerFilter;
import org.ala.spatial.analysis.service.FilteringService;
import org.ala.spatial.analysis.service.ShapeIntersectionService;
import org.ala.spatial.util.SimpleRegion;
import org.ala.spatial.util.SimpleShapeFile;
import org.ala.spatial.util.SpatialSettings;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 *
 * @author ajay
 */
@Controller
@RequestMapping("/ws/intersect")
public class ShapeIntersectionWSController {

    /*
     * returns result of species distribution maps intersection with param area
     */
    @RequestMapping(value = "/shape", method = RequestMethod.POST)
    public
    @ResponseBody
    String intersect(HttpServletRequest req) {
        try {
            String area = URLDecoder.decode(req.getParameter("area"),"UTF-8");

            LayerFilter[] filter = null;
            SimpleRegion region = null;
            if (area != null && area.startsWith("ENVELOPE")) {
                filter = FilteringService.getFilters(area);
            } else {
                region = SimpleShapeFile.parseWKT(area);
            }

            if(region != null) {
                String header = ShapeIntersectionService.getHeader();
                int [] r = ShapeIntersectionService.getIntersections(region);
                return header + "\n" + ShapeIntersectionService.convertToString(r);
            }
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
        return "";
    }
    /*
     * returns result of species distribution maps intersection with param area
     */
    @RequestMapping(value = "/shape/count", method = RequestMethod.POST)
    public
    @ResponseBody
    String intersectCount(HttpServletRequest req) {
        try {
            String area = URLDecoder.decode(req.getParameter("area"),"UTF-8");

            LayerFilter[] filter = null;
            SimpleRegion region = null;
            if (area != null && area.startsWith("ENVELOPE")) {
                filter = FilteringService.getFilters(area);
            } else {
                region = SimpleShapeFile.parseWKT(area);
            }

            if(region != null) {
                String header = ShapeIntersectionService.getHeader();
                int [] r = ShapeIntersectionService.getIntersections(region);
                return String.valueOf(r.length);
            }
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
        return "";
    }

    /**
     * lists LSIDs that have an associated Layer + WMS Get info
     * @param req
     * @return
     */
    @RequestMapping(value = "/list", method = RequestMethod.GET)
    public
    @ResponseBody
    String list(HttpServletRequest req) {
        try {
            return ShapeIntersectionService.list();
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
        return "";
    }
}
