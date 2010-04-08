package org.ala.spatial.web.services;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Hashtable;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.ala.spatial.analysis.maxent.MaxentServiceImpl;
import org.ala.spatial.analysis.maxent.MaxentSettings;
import org.ala.spatial.analysis.tabulation.SamplingService;
import org.ala.spatial.util.Layer;
import org.ala.spatial.util.SpatialSettings;
import org.ala.spatial.util.UploadSpatialResource;
import org.ala.spatial.util.Zipper;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 *
 * @author ajay
 */
@Controller
@RequestMapping("/ws/maxent/")
public class MaxentWSController {

    private SpatialSettings ssets;

    @RequestMapping(value = "/process", method = RequestMethod.GET)
    public
    @ResponseBody
    String process(HttpServletRequest req) {

        try {


            HttpSession session = req.getSession(true);
            long currTime = System.currentTimeMillis();

            String currentPath = session.getServletContext().getRealPath("/");

            String taxon = req.getParameter("taxonid");

            ssets = new SpatialSettings();

            // dump the species data to a file
            SamplingService ss = new SamplingService();
            String[] csvdata = ss.sampleSpecies(taxon, null).split("\n");
            StringBuffer sbSpecies = new StringBuffer();
            for (int i = 0; i < csvdata.length; i++) {
                String[] recdata = csvdata[i].split(",");
                sbSpecies.append("species, " + recdata[recdata.length - 2] + ", " + recdata[recdata.length - 1]);
            }



            MaxentSettings msets = new MaxentSettings();
            //msets.setMaxentPath((String) session.getAttribute("maxentCmdPath"));
            msets.setMaxentPath(ssets.getMaxentCmd());
            //msets.setEnvList(Arrays.asList(envsel));
            //msets.setEnvList(Arrays.asList(req.getParameterValues("envsel")));
            msets.setEnvList(Arrays.asList(getEnvFiles(req.getParameter("envlist"))));
            msets.setRandomTestPercentage(Integer.parseInt(req.getParameter("txtTestPercentage")));
            msets.setEnvPath(ssets.getEnvDataPath());
            msets.setEnvVarToggler("world");
            msets.setSpeciesFilepath(setupSpecies(sbSpecies.toString(), currentPath + "output/maxent/" + currTime + "/"));
            msets.setOutputPath(currentPath + "output/maxent/" + currTime + "/");
            if (req.getParameter("chkJackknife") != null) {
                msets.setDoJackknife(true);
            }
            if (req.getParameter("chkResponseCurves") != null) {
                msets.setDoResponsecurves(true);
            }

            System.out.println("To run: " + msets.toString());


            MaxentServiceImpl maxent = new MaxentServiceImpl();
            maxent.setMaxentSettings(msets);
            int exitValue = maxent.process();

            System.out.println("Completed: " + exitValue);

            Hashtable htProcess = new Hashtable();
            if (exitValue == 0) {
                // TODO: Should probably move this part an external "parent"
                // function so can be used by other functions
                //

                Hashtable htGeoserver = ssets.getGeoserverSettings();

                // if generated successfully, then add it to geoserver
                String url = (String) htGeoserver.get("geoserver_url") + "/rest/workspaces/ALA/coveragestores/maxent_" + currTime + "/file.arcgrid?coverageName=species_" + currTime;
                String extra = "";
                String username = (String) htGeoserver.get("geoserver_username");
                String password = (String) htGeoserver.get("geoserver_password");

                // first zip up the file as it's going to be sent as binary
                String ascZipFile = Zipper.zipFile(msets.getOutputPath() + "species.asc");

                // Upload the file to GeoServer using REST calls
                System.out.println("Uploading file: " + ascZipFile + " to \n" + url);
                UploadSpatialResource.loadResource(url, extra, username, password, ascZipFile);

                //extraout += "status: success"; ///  \n <br />
                //extraout += "file: " + "output/maxent/" + currTime + "/species.asc \n <br />";
                //extraout += "info: " + "output/maxent/" + currTime + "/species.html \n <br />";
                //extraout += "map: " + "/wms?service=WMS&version=1.1.0&request=GetMap&layers=ALA:species_" + currTime + "&styles=alastyles&bbox=112.0,-44.0,154.0,-9.0&width=700&height=500&srs=EPSG:4326&format=application/openlayers";
                //extraout += "";

                //mapframe.setSrc((String) htGeoserver.get("geoserver_url") + "/wms?service=WMS&version=1.1.0&request=GetMap&layers=ALA:species_" + currTime + "&styles=alastyles&bbox=112.0,-44.0,154.0,-9.0&width=700&height=500&srs=EPSG:4326&format=application/openlayers");
                //infoframe.setSrc("/output/maxent/" + currTime + "/species.html");
                //outputtab.setVisible(true);

                htProcess.put("status", "success"); ///
                htProcess.put("pid", currTime);
                htProcess.put("info", "/output/maxent/" + currTime + "/species.html");
                //htProcess.put("","");
                //htProcess.put("","");

                /*
                StringWriter sw = new StringWriter();

                JsonFactory f = new JsonFactory();
                JsonGenerator g = f.createJsonGenerator(sw);
                g.writeObject(htProcess);
                g.close();

                 System.out.println("sw: \n" + sw.toString());
                 *
                 */


                //return "status:success;pid:" + currTime + ";info:"+"/output/maxent/" + currTime + "/species.html";

                return "status:success;pid:" + currTime + ";info:" + "/output/maxent/" + currTime + "/species.html";


            } else {
                //extraout += "Status: failure\n";
                //htProcess.put("status", "failure");
                return "status:failure;";

            }

        } catch (Exception e) {
            System.out.println("Error processing Maxent request:");
            e.printStackTrace(System.out);
        }

        return "";

    }

    private String setupSpecies(String speciesList, String outputpath) {
        try {
            File fDir = new File(outputpath);
            fDir.mkdir();

            File spFile = File.createTempFile("points_", ".csv", fDir);
            PrintWriter spWriter = new PrintWriter(new BufferedWriter(new FileWriter(spFile)));

            //spWriter.write("spname, longitude, latitude \n");
            spWriter.write(speciesList);
            spWriter.close();

            return spFile.getAbsolutePath();
        } catch (IOException ex) {
            //Logger.getLogger(MaxentServiceImpl.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("error writing species file:");
            ex.printStackTrace(System.out);
        }

        return null;
    }

    private String[] getEnvFiles(String envNames) {
        String[] nameslist = envNames.split(":");
        String[] pathlist = new String[nameslist.length];

        for (int j = 0; j < nameslist.length; j++) {

            Layer[] _layerlist = ssets.getEnvData();

            for (int i = 0; i < _layerlist.length; i++) {
                if (_layerlist[i].display_name.equalsIgnoreCase(nameslist[j])) {
                    pathlist[i] = _layerlist[i].name;
                    continue;
                }
            }
        }

        return pathlist;
    }
}
