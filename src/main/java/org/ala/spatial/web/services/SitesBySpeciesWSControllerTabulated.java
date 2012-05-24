package org.ala.spatial.web.services;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.sf.json.JSONObject;
import org.ala.spatial.analysis.index.LayerFilter;
import org.ala.spatial.util.AnalysisQueue;
import org.ala.layers.intersect.SimpleRegion;
import org.ala.layers.intersect.SimpleShapeFile;
import org.ala.spatial.analysis.layers.SxS;
import org.ala.spatial.util.AlaspatialProperties;
import org.ala.spatial.util.AnalysisJob;
import org.ala.spatial.util.AnalysisJobSitesBySpeciesTabulated;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

/**
 *
 * @author ajay
 */
@Controller
public class SitesBySpeciesWSControllerTabulated {

    static Object lockPropertiesForWriting = new Object();

    @RequestMapping(value = "/ws/sitesbyspeciestabulated", method = {RequestMethod.POST, RequestMethod.GET})
    public
    @ResponseBody
    String processgeoq(HttpServletRequest req) {

        try {

            long currTime = System.currentTimeMillis();

            String currentPath = AlaspatialProperties.getBaseOutputDir() + "output" + File.separator + "sitesbyspecies";
            String speciesq = URLDecoder.decode(req.getParameter("q"), "UTF-8").replace("__", ".");
            String area = req.getParameter("area");
            String biocacheurl = URLDecoder.decode(req.getParameter("bs"), "UTF-8");
            double gridsize = Double.parseDouble(req.getParameter("gridsize"));
            String layers = req.getParameter("layers");
            if (layers != null) {
                layers = URLDecoder.decode(layers, "UTF-8");
            }
            String facetName = req.getParameter("facetname");
            if (facetName == null) {
                facetName = "names_and_lsid";
            }

            LayerFilter[] filter = null;
            SimpleRegion region = null;
            if (area != null && area.startsWith("ENVELOPE")) {
                filter = LayerFilter.parseLayerFilters(area);
            } else {
                region = SimpleShapeFile.parseWKT(area);
            }

            String pid = Long.toString(currTime);

            AnalysisJobSitesBySpeciesTabulated sbs =
                    new AnalysisJobSitesBySpeciesTabulated(pid, currentPath, "",
                    speciesq, gridsize, region, filter, biocacheurl,
                    layers == null ? null : layers.split(","),
                    true, facetName);

            StringBuffer inputs = new StringBuffer();
            inputs.append("pid:").append(pid);
            inputs.append(";speciesq:").append(speciesq);
            inputs.append(";gridsize:").append(gridsize);
            inputs.append(";area:").append(area);
            sbs.setInputs(inputs.toString());
            AnalysisQueue.addJob(sbs);

            return pid;

        } catch (Exception e) {
            System.out.println("Error processing SitesBySpecies request:");
            e.printStackTrace(System.out);
        }

        return "";
    }

    /*
     * list distribution table records, GET
     */
    @RequestMapping(value = "sxs", method = RequestMethod.GET)
    public ModelAndView sxsList(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        List<SxS> list = getSxSList();

        ModelMap m = new ModelMap();
        m.addAttribute("sxs", list);
        return new ModelAndView("sxs/list", m);

    }

    /*
     * list distribution table records, GET
     */
    @RequestMapping(value = "sxs/{analysisId}", method = RequestMethod.GET)
    public ModelAndView sxsView(@PathVariable("analysisId") String analysisId, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        List tables = getTablesFor(analysisId);
        String tableType = (String) tables.get(0);

        return new ModelAndView("redirect:" + AlaspatialProperties.getAlaspatialUrl() + "/sxs/" + analysisId + "/" + tableType);
    }

    /*
     * list distribution table records, GET
     */
    @RequestMapping(value = "sxs/{analysisId}/{tableType}", method = RequestMethod.GET)
    public ModelAndView sxsViewByType(@PathVariable("analysisId") String analysisId,
            @PathVariable("tableType") String tableType,
            HttpServletRequest req, HttpServletResponse resp) throws IOException {
        List tables = getTablesFor(analysisId);

        String json = readFile(AlaspatialProperties.getBaseOutputDir() + File.separator + "output" + File.separator + "sitesbyspecies" + File.separator + analysisId + File.separator + tableType + ".json");
        JSONObject jo = JSONObject.fromObject(json);

        jo.put("id", analysisId);
        jo.put("tables", tables);
        jo.put("tablename", tableType);
        jo.put("table", jo.get(tableType));
        jo.remove(tableType);
        jo.put("csvurl", AlaspatialProperties.getBaseOutputURL() + "/output/sitesbyspecies/" + analysisId + "/" + tableType + ".csv");
        jo.put("jsonurl", AlaspatialProperties.getBaseOutputURL() + "/output/sitesbyspecies/" + analysisId + "/" + tableType + ".json");

        ModelMap m = new ModelMap();
        m.addAttribute("sxs", jo);
        return new ModelAndView("sxs/view", m);

    }

    List<SxS> getSxSList() throws IOException {
        initListProperties();

        String pth = AlaspatialProperties.getAnalysisWorkingDir() + File.separator + "sxs" + File.separator;

        Properties p = new Properties();
        p.load(new FileReader(pth + "list.properties"));

        ArrayList<SxS> list = new ArrayList<SxS>();

        for (Entry entry : p.entrySet()) {
            if (new File(pth + entry.getKey()).exists()) {
                BufferedReader br = new BufferedReader(new FileReader(pth + entry.getKey()));
                String analysisId = br.readLine();
                br.close();
                String status;
                if ((analysisId + "").length() == 0 && AnalysisQueue.getState(analysisId) == null) {
                    new File(pth + entry.getKey()).delete();
                    status = "Not yet run";
                } else {
                    status = (analysisId + "").length() == 0 ? AnalysisJob.WAITING : AnalysisQueue.getState(analysisId);
                }

                list.add(new SxS((String) entry.getValue(), analysisId, status));
            } else {
                list.add(new SxS((String) entry.getValue(), "", "Not yet run"));
            }
        }

        return list;
    }

    private String readFile(String file) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader br = new BufferedReader(new FileReader(file));
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line).append("\n");
        }
        br.close();
        return sb.toString().trim();
    }

    /*
     * list distribution table records, GET
     */
    @RequestMapping(value = "sxsrun", method = RequestMethod.GET)
    public
    @ResponseBody
    String sxsRun(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        run();

        return "OK";
    }

    public void run() throws IOException {
        //get all incomplete runs
        String pth = AlaspatialProperties.getAnalysisWorkingDir() + File.separator + "sxs" + File.separator;
        Properties p = new Properties();
        p.load(new FileReader(pth + "list.properties"));
        ArrayList<SxS> list = new ArrayList<SxS>();
        for (Entry entry : p.entrySet()) {
            if (!new File(pth + entry.getKey()).exists()) {
                list.add(new SxS((String) entry.getValue(), (String) entry.getKey(), ""));
                FileWriter fw = new FileWriter(pth + entry.getKey());
                fw.close();
            }
        }

        for (SxS sxs : list) {
            String url = AlaspatialProperties.getAlaspatialUrl() + "/ws/sitesbyspeciestabulated?" + sxs.getValue();
            try {
                //start
                GetMethod get = new GetMethod(url);
                HttpClient client = new HttpClient();
                client.executeMethod(get);
                if (get.getStatusCode() == 200) {
                    String id = get.getResponseBodyAsString();
                    FileWriter fw = new FileWriter(pth + sxs.getAnalysisId());
                    fw.write(id);
                    fw.close();

                    //wait until finished
                    while (AnalysisQueue.getState(id).equals(AnalysisJob.RUNNING)
                            || AnalysisQueue.getState(id).equals(AnalysisJob.WAITING)) {
                        Thread.currentThread().sleep(30000); //wait 30s
                    }

                    //test
                    if (!AnalysisQueue.getState(id).equals(AnalysisJob.SUCCESSFUL)) {
                        System.out.println("sxs failed for: " + url);
                        System.out.println(AnalysisQueue.getMessage(id));
                        System.out.println(AnalysisQueue.getLog(id));
                        new File(pth + sxs.getAnalysisId()).delete();
                    }
                } else {
                    System.out.println("sxs failed for: " + url);
                    System.out.println("response code: " + get.getStatusCode());
                    new File(pth + sxs.getAnalysisId()).delete();
                }
            } catch (Exception e) {
                new File(pth + sxs.getAnalysisId()).delete();
                System.out.println("sxs failed for: " + url);
                e.printStackTrace();
            }
        }
    }

    private List getTablesFor(String analysisId) {
        ArrayList<String> list = new ArrayList<String>();
        File[] files = new File(AlaspatialProperties.getBaseOutputDir() + File.separator + "output" + File.separator + "sitesbyspecies" + File.separator + analysisId + File.separator).listFiles();
        for (File f : files) {
            if (f.getPath().endsWith(".json")) {
                list.add(f.getName().substring(0, f.getName().length() - ".json".length()));
            }
        }
        return list;
    }

    private void initListProperties() throws IOException {
        //create a list.properties if it does not exist
        String pth = AlaspatialProperties.getAnalysisWorkingDir() + File.separator + "sxs" + File.separator;
        File f = new File(pth + "list.properties");
        if (!f.exists()) {
            new File(pth).mkdir();
            FileWriter fw = new FileWriter(f);
            fw.write("1=q=tasmanian%20devil&bs=http%3A%2F%2Fbiocache.ala.org.au%2Fws&gridsize=0.01&layers=aus1\n"
                    + "2=q=tasmanian%20devil&bs=http%3A%2F%2Fbiocache.ala.org.au%2Fws&gridsize=0.1&layers=aus1\n"
                    + "3=q=tasmanian%20devil&bs=http%3A%2F%2Fbiocache.ala.org.au%2Fws&gridsize=10&layers=aus1");
            fw.close();
        }
    }

    /*
     * add to analysis, POST
     */
    @RequestMapping(value = {"sxs/add", "sxs/sxs/add"}, method = {RequestMethod.GET, RequestMethod.POST})
    public ModelAndView sxsView(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String speciesquery = req.getParameter("speciesquery");
        String layers = req.getParameter("layers");
        String bs = URLEncoder.encode(AlaspatialProperties.getBiocacheWsURL(), "UTF-8");
        String gridsize = req.getParameter("gridsize");

        try {
            String url = "q=" + speciesquery + "&gridsize=" + gridsize + "&layers=" + layers + "&bs=" + bs;

            String pth = AlaspatialProperties.getAnalysisWorkingDir() + File.separator + "sxs" + File.separator;

            initListProperties();
            Properties p = new Properties();
            p.load(new FileReader(pth + "list.properties"));

            synchronized (lockPropertiesForWriting) {
                FileWriter fw = new FileWriter(pth + "list.properties", true);
                if (!p.containsValue(url)) {
                    for (int i = 1; i < Integer.MAX_VALUE; i++) {
                        if (!p.containsKey(String.valueOf(i))) {
                            fw.write("\n" + i + "=" + url);
                            break;
                        }
                    }
                }
                fw.flush();
                fw.close();
            }

            class RunThread extends Thread {
                SitesBySpeciesWSControllerTabulated c;
                public RunThread(SitesBySpeciesWSControllerTabulated c) {
                    this.c = c;
                }
                @Override
                public void run() {
                    try {
                        c.run();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            new RunThread(this).start();
            
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new ModelAndView("redirect:" + AlaspatialProperties.getAlaspatialUrl() + "/sxs");
    }
}
