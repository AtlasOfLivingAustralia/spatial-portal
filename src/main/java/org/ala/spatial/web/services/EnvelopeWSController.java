package org.ala.spatial.web.services;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.ala.layers.grid.Grid2Shape;
import org.ala.layers.intersect.Grid;
import org.ala.spatial.analysis.index.LayerFilter;
import org.ala.spatial.util.AlaspatialProperties;
import org.ala.spatial.util.CoordinateTransformer;
import org.ala.spatial.util.GridCutter;
import org.ala.spatial.util.SpatialTransformer;
import org.ala.spatial.util.UploadSpatialResource;
import org.ala.spatial.util.Zipper;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 *
 * @author ajay
 */
@Controller
public class EnvelopeWSController {

    @RequestMapping(value = "/ws/envelope", method = { RequestMethod.POST, RequestMethod.GET })
    public
    @ResponseBody
    String envelope(HttpServletRequest req) {
        try {
            String currentPath = AlaspatialProperties.getBaseOutputDir();
            String area = req.getParameter("area");

            String resolution = req.getParameter("res");
            if (resolution == null) {
                resolution = "0.01";
            }

            LayerFilter[] filter = null;
            if (area != null /* && area.startsWith("ENVELOPE")*/) {
                filter = LayerFilter.parseLayerFilters(area);
            }

            System.out.println("filter: " + filter);

            //test envelope
            if (!GridCutter.isValidLayerFilter(resolution, filter)) {
                return null;
            }

            String pid = String.valueOf(System.currentTimeMillis());
            File dir = new File(currentPath + File.separator + "envelope" + File.separator + pid);
            dir.mkdirs();

            File grid = new File(dir.getPath() + File.separator + "envelope");

            double areaSqKm = -1;
            if ((areaSqKm = GridCutter.makeEnvelope(grid.getPath(), resolution, filter)) >= 0) {

                SpatialTransformer.convertDivaToAsc(dir.getPath() + File.separator + "envelope", dir.getPath() + File.separator + "envelope.asc");
                CoordinateTransformer.generate4326prj(dir.getPath() + File.separator, "envelope");
                geoserverLoad(dir.getPath() + File.separator, "envelope_" + pid);

                Grid g = new Grid(grid.getPath());
                String extents = g.xmin + "," + g.ymin + "," + g.xmax + "," + g.ymax;

                return pid + "\n" + extents + "\n" + areaSqKm;
            }

        } catch (Exception e) {
            e.printStackTrace(System.out);
        }

        return null;
    }

    @RequestMapping(value = "/ws/envelope/wkt", method = RequestMethod.GET)
    public void envelope(HttpServletRequest req, HttpServletResponse response) throws IOException {
        String currentPath = AlaspatialProperties.getBaseOutputDir();
        String pid = req.getParameter("pid");

        Grid grid = new Grid(currentPath + File.separator + "envelope" + File.separator + pid + File.separator + "envelope");
        if (grid != null) {
            OutputStream os = response.getOutputStream();
            Grid2Shape.streamGrid2Wkt(os, grid.getGrid(), 1, 1, grid.nrows, grid.ncols, grid.xmin, grid.ymin, grid.xres, grid.yres);
            os.close();
        }
    }

    void geoserverLoad(String filepath, String layerName) {
        //publish layer
        String url = (String) AlaspatialProperties.getGeoserverUrl()
                + "/rest/workspaces/ALA/coveragestores/"
                + layerName
                + "/file.arcgrid?coverageName="
                + layerName;
        String extra = "";
        String username = (String) AlaspatialProperties.getGeoserverUsername();
        String password = (String) AlaspatialProperties.getGeoserverPassword();
        String[] infiles = {filepath + "envelope.asc", filepath + "envelope.prj"};
        String ascZipFile = filepath + layerName + ".asc.zip";
        Zipper.zipFiles(infiles, ascZipFile);

        // Upload the file to GeoServer using REST calls
        System.out.println("Uploading file: " + ascZipFile + " to \n" + url);
        UploadSpatialResource.loadResource(url, extra, username, password, ascZipFile);

        //Apply style
        String data = "<layer><enabled>true</enabled><defaultStyle><name>envelope_style</name></defaultStyle></layer>";
        url = (String) AlaspatialProperties.getGeoserverUrl() + "/rest/layers/ALA:" + layerName;
        UploadSpatialResource.assignSld(url, extra, username, password, data);
    }
}
