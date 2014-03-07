/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.ala.spatial.ws;

import au.org.ala.spatial.data.FacetCache;
import au.org.ala.spatial.data.QueryField;
import au.org.ala.spatial.userpoints.RecordsLookup;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;

/**
 * @author Adam
 */
@Controller
public class General {
    private static Logger logger = Logger.getLogger(General.class);

    final static int HIGHLIGHT_RADIUS = 3;
    @Inject
    private FacetCache facetCache;

    @RequestMapping(value = "/occurrences", method = RequestMethod.GET)
    public
    @ResponseBody
    String getOccurrencesUploaded(HttpServletRequest request) {
        String q = request.getParameter("q");
        String box = request.getParameter("box");
        int start = Integer.parseInt(request.getParameter("start"));

        String[] bb = box.split(",");

        double long1 = Double.parseDouble(bb[0]);
        double lat1 = Double.parseDouble(bb[1]);
        double long2 = Double.parseDouble(bb[2]);
        double lat2 = Double.parseDouble(bb[3]);

        Object[] data = (Object[]) RecordsLookup.getData(q);

        int count = 0;
        String record = null;
        if (data != null) {
            double[] points = (double[]) data[0];
            ArrayList<QueryField> fields = (ArrayList<QueryField>) data[1];
            double[] pointsBB = (double[]) data[2];
            String metadata = (String) data[3];

            if (points == null || points.length == 0
                    || pointsBB[0] > long2 || pointsBB[2] < long1
                    || pointsBB[1] > lat2 || pointsBB[3] < lat1) {
                return null;
            } else {
                for (int i = 0; i < points.length; i += 2) {
                    if (points[i] >= long1 && points[i] <= long2
                            && points[i + 1] >= lat1 && points[i + 1] <= lat2) {
                        if (count == start) {
                            StringBuilder sb = new StringBuilder();
                            for (QueryField qf : fields) {
                                if (sb.length() == 0) {
                                    sb.append("{\"totalRecords\":<totalCount>,\"occurrences\":[{");
                                } else {
                                    sb.append(",");
                                }
                                sb.append("\"").append(qf.getDisplayName()).append("\":\"");
                                sb.append(qf.getAsString(i / 2).replace("\"", "\\\"")).append("\"");
                            }
                            sb.append("}]");
                            sb.append(",\"metadata\":\"");
                            sb.append(metadata);
                            sb.append("\"");
                            sb.append("}");
                            record = sb.toString();
                        }
                        count++;
                    }
                }
            }
        }

        if (record != null) {
            record = record.replace("<totalCount>", String.valueOf(count));
        }

        return record;
    }

    @RequestMapping(value = "/admin/reloadconfig", method = RequestMethod.GET)
    @ResponseBody
    public String reloadConfig() {
        //signal for reload
        // ConfigurationLoaderStage1.loaders.get(0).interrupt();

        //reload the facet cache
        facetCache.reloadCache();

        //return summary

        //was it successful?
        return "";
    }
}
