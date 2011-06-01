package org.ala.spatial.web.services;

import java.io.File;
import java.net.URLDecoder;
import javax.servlet.http.HttpServletRequest;
import org.ala.spatial.analysis.index.LayerFilter;
import org.ala.spatial.analysis.service.FilteringService;
import org.ala.spatial.analysis.service.ShapeIntersectionService;
import org.ala.spatial.util.Grid;
import org.ala.spatial.util.Layer;
import org.ala.spatial.util.Layers;
import org.ala.spatial.util.SimpleRegion;
import org.ala.spatial.util.SimpleShapeFile;
import org.ala.spatial.util.SpatialSettings;
import org.ala.spatial.util.TabulationSettings;
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
public class IntersectionWSController {

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

    @RequestMapping(value = "/layer", method = RequestMethod.GET)
    public
    @ResponseBody
    String intersectLayer(HttpServletRequest req) {
        try {
            long start = System.currentTimeMillis();

            String [] envlist = URLDecoder.decode(req.getParameter("layers"),"UTF-8").split(",");
            String [] latlong = URLDecoder.decode(req.getParameter("latlong"),"UTF-8").split(",");
            String useCached = req.getParameter("useCached");

            Layer [] layers = Layers.getLayers(envlist);
            double [][] point = {{ Double.parseDouble(latlong[1]), Double.parseDouble(latlong[0]) }};

            StringBuilder sb = new StringBuilder();
            for(int i=0;i<layers.length;i++) {
                Layer l = layers[i];
                if(l != null && l.type.equalsIgnoreCase("environmental")) {
                    Grid grid;
                    if(useCached != null && useCached.equals("1")) {
                        //to use WHOLE grid/caching
                        grid = Grid.getGrid(TabulationSettings.environmental_data_path + l.name);
                    } else {
                        //to use ini re-read + seek in grid file
                        grid = new Grid(TabulationSettings.environmental_data_path + l.name);
                    }

                    float [] value = grid.getValues(point);
                    if(value != null) {
                        if(Float.isNaN(value[0])) {
                            sb.append(l.display_name).append("\t").append("no data").append("\n");
                        } else {
                            sb.append(l.display_name).append("\t").append(value[0]).append("\n");
                        }
                    }
                } else if(envlist[i].startsWith("species_")) {
                    //maxent layer
                    String id = envlist[i].substring(8);

                    String filename = TabulationSettings.base_output_dir + "output/maxent/" + id + "/" + id;

                    Grid grid = new Grid(filename);

                    if(grid != null && (new File(filename + ".grd").exists()) ) {
                        float [] value = grid.getValues(point);
                        if(value != null) {
                            if(Float.isNaN(value[0])) {
                                sb.append("Prediction").append("\t").append("no data").append("\n");
                            } else {
                                sb.append("Prediction").append("\t").append(value[0]).append("\n");
                            }
                        }
                    }
                } else if(envlist[i].startsWith("aloc_")) {
                    //aloc layer
                    String id = envlist[i].substring(5);

                    String filename = TabulationSettings.base_output_dir + "output/aloc/" + id + "/" + id;

                    Grid grid = new Grid(filename);

                    if(grid != null && (new File(filename + ".grd").exists()) ) {
                        float [] value = grid.getValues(point);
                        if(value != null) {
                            if(Float.isNaN(value[0])) {
                                sb.append("Classification").append("\t").append("no data").append("\n");
                            } else {
                                sb.append("Classification").append("\t").append(value[0]).append("\n");
                            }
                        }
                    }
                }
            }

            System.out.println("envlayers intersect: n=" + envlist.length + " t=" + (System.currentTimeMillis() - start) + "ms");

            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
        return "";
    }
}
