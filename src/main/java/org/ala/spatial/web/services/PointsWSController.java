package org.ala.spatial.web.services;

import java.net.URLDecoder;
import javax.servlet.http.HttpServletRequest;
import org.ala.spatial.analysis.service.LoadedPoints;
import org.ala.spatial.analysis.service.LoadedPointsService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 *
 * @author adam
 */
@Controller
@RequestMapping("/ws/points/")
public class PointsWSController {

    @RequestMapping(value = "/register", method = RequestMethod.POST)
    public
    @ResponseBody
    String register(HttpServletRequest req) {

        try {

            String[] pointsString = req.getParameter("points").split("\n");
            String idss = req.getParameter("ids");
            String[] ids = null;
            if (idss != null) {
                ids = idss.split("\n");
            }
            String name = URLDecoder.decode(req.getParameter("name"), "UTF-8");

            double[][] points = new double[pointsString.length][2];
            String[] line;
            int count = 0;
            for (int i = 0; i < pointsString.length; i++) {
                line = pointsString[i].split(",");
                try {
                    points[count][0] = Double.parseDouble(line[0]);
                    points[count][1] = Double.parseDouble(line[1]);
                    count++;
                } catch (Exception e) {
                }
            }

            //resize
            double[][] pointsTrimmed = new double[count][2];
            for (int i = 0; i < count; i++) {
                pointsTrimmed[i][0] = points[i][0];
                pointsTrimmed[i][1] = points[i][1];
            }
            points = pointsTrimmed;

            String id = String.valueOf(System.currentTimeMillis());

            LoadedPointsService.addCluster(id, new LoadedPoints(points, name, ids));

            return id;

        } catch (Exception e) {
            System.out.println("Error processing Sampling request:");
            e.printStackTrace(System.out);
        }

        return "";
    }
}
