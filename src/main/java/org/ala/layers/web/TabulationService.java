package org.ala.layers.web;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.TreeMap;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.ala.layers.dao.FieldDAO;
import org.ala.layers.dao.TabulationDAO;
import org.ala.layers.dto.Tabulation;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 *
 * @author Adam
 */
@Controller
public class TabulationService {

    private final String WS_TABULATION = "/tabulation/{fid1}/{fid2}";
    private final String WS_TABULATION_CSV = "/tabulation/{fid1}/{fid2}/csv";
    private final String WS_TABULATION_HTML = "/tabulation/{fid1}/{fid2}/html";
    private final String WS_TABULATION_LIST = "/tabulations";
    private final String WS_TABULATION_LIST_HTML = "/tabulations/html";
    private final String WS_TABULATION_SINGLE = "/tabulation/{fid1}";
    private final String WS_TABULATION_SINGLE_CSV = "/tabulation/{fid1}/csv";
    private final String WS_TABULATION_SINGLE_HTML = "/tabulation/{fid1}/html";

    /**
     * Log4j instance
     */
    protected Logger logger = Logger.getLogger(this.getClass());
    @Resource(name = "fieldDao")
    private FieldDAO fieldDao;
    @Resource(name = "tabulationDao")
    private TabulationDAO tabulationDao;

    /*
     * list distribution table records, GET
     */
    @RequestMapping(value = WS_TABULATION, method = RequestMethod.GET)
    public @ResponseBody List<Tabulation> getTabulation(@PathVariable("fid1") String fid1,@PathVariable("fid2") String fid2,
            @RequestParam(value = "wkt", required = false, defaultValue = "") String wkt,
            HttpServletRequest req) throws IOException {

         return tabulationDao.getTabulation(fid1, fid2, wkt);
    }
    
    /*
     * list distribution table records, GET
     */
    @RequestMapping(value = WS_TABULATION_CSV, method = RequestMethod.GET)
    public void getTabulationCsv(@PathVariable("fid1") String fid1,@PathVariable("fid2") String fid2,
            @RequestParam(value = "wkt", required = false, defaultValue = "") String wkt,
            HttpServletRequest req, HttpServletResponse resp) throws IOException {

        List<Tabulation> tabulations = tabulationDao.getTabulation(fid1, fid2, wkt);

        write(tabulations, resp, fid1, fid2, wkt, "csv");
    }

    /*
     * list distribution table records, GET
     */
    @RequestMapping(value = WS_TABULATION_HTML, method = RequestMethod.GET)
    public void getTabulationHtml(@PathVariable("fid1") String fid1,@PathVariable("fid2") String fid2,
            @RequestParam(value = "wkt", required = false, defaultValue = "") String wkt,
            HttpServletRequest req, HttpServletResponse resp) throws IOException {

        List<Tabulation> tabulations = tabulationDao.getTabulation(fid1, fid2, wkt);

        write(tabulations, resp, fid1, fid2, wkt, "html");
    }

    /*
     * list distribution table records, GET
     */
    @RequestMapping(value = WS_TABULATION, method = RequestMethod.POST)
    public @ResponseBody List<Tabulation> getTabulationPost(@PathVariable("fid1") String fid1,@PathVariable("fid2") String fid2,
            @RequestParam(value = "wkt", required = false, defaultValue = "") String wkt,
            HttpServletRequest req) throws IOException {

         return tabulationDao.getTabulation(fid1, fid2, wkt);
    }

    /*
     * list distribution table records, GET
     */
    @RequestMapping(value = WS_TABULATION_CSV, method = RequestMethod.POST)
    public void getTabulationCsvPost(@PathVariable("fid1") String fid1,@PathVariable("fid2") String fid2,
             @RequestParam(value = "wkt", required = false, defaultValue = "") String wkt,
            HttpServletRequest req, HttpServletResponse resp) throws IOException {

        getTabulationCsv(fid1, fid2, wkt, req, resp);
    }

    @RequestMapping(value = WS_TABULATION_LIST_HTML, method = RequestMethod.GET)
    public void listAvailableTabulationsHtml(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        List<Tabulation> tabulations = tabulationDao.listTabulations();

        //write to string
        StringBuilder sb = new StringBuilder();
        sb.append("<table border=1<tr><td><b>Field 1</b></td><td><b>Field 2</b></td><td><b>link</b></tr>");
        for (Tabulation t : tabulations) {
            sb.append("<tr><td>");
            sb.append(t.getName1()).append("</td><td>");
            sb.append(t.getName2()).append("</td><td>");
            sb.append("<a href='../tabulation/").append(t.getFid1());
            sb.append("/").append(t.getFid2()).append("/html'>table</a>");
            sb.append("</td></tr>");
        }
        sb.append("</table>");

        OutputStream os = resp.getOutputStream();
        os.write(sb.toString().getBytes("UTF-8"));
        os.close();
    }

    @RequestMapping(value = WS_TABULATION_LIST, method = RequestMethod.GET)
    public @ResponseBody List<Tabulation> listAvailableTabulations(HttpServletRequest req) throws IOException {

        return tabulationDao.listTabulations();
    }


    /*
     * list distribution table records, GET
     */
    @RequestMapping(value = WS_TABULATION_SINGLE, method = RequestMethod.GET)
    public @ResponseBody List<Tabulation> getTabulationSingle(@PathVariable("fid1") String fid1,
            @RequestParam(value = "wkt", required = false, defaultValue = "") String wkt,
            HttpServletRequest req) throws IOException {

         return tabulationDao.getTabulationSingle(fid1, wkt);
    }

    /*
     * list distribution table records, GET
     */
    @RequestMapping(value = WS_TABULATION_SINGLE_CSV, method = RequestMethod.GET)
    public void getTabulationSingleCsv(@PathVariable("fid1") String fid1,
            @RequestParam(value = "wkt", required = false, defaultValue = "") String wkt,
            HttpServletRequest req, HttpServletResponse resp) throws IOException {

        List<Tabulation> tabulations = tabulationDao.getTabulationSingle(fid1, wkt);

        write(tabulations, resp, fid1, null, wkt, "csv");
    }

    /*
     * list distribution table records, GET
     */
    @RequestMapping(value = WS_TABULATION_SINGLE_HTML, method = RequestMethod.GET)
    public void getTabulationSingleHtml(@PathVariable("fid1") String fid1,
            @RequestParam(value = "wkt", required = false, defaultValue = "") String wkt,
            HttpServletRequest req, HttpServletResponse resp) throws IOException {

        List<Tabulation> tabulations = tabulationDao.getTabulationSingle(fid1, wkt);

        write(tabulations,resp,fid1,null,wkt,"html");
    }

    /*
     * list distribution table records, GET
     */
    @RequestMapping(value = WS_TABULATION_SINGLE, method = RequestMethod.POST)
    public @ResponseBody List<Tabulation> getTabulationSinglePost(@PathVariable("fid1") String fid1,
            @RequestParam(value = "wkt", required = false, defaultValue = "") String wkt,
            HttpServletRequest req) throws IOException {

         return tabulationDao.getTabulationSingle(fid1, wkt);
    }

    /*
     * list distribution table records, GET
     */
    @RequestMapping(value = WS_TABULATION_SINGLE_CSV, method = RequestMethod.POST)
    public void getTabulationSingleCsvPost(@PathVariable("fid1") String fid1,
             @RequestParam(value = "wkt", required = false, defaultValue = "") String wkt,
            HttpServletRequest req, HttpServletResponse resp) throws IOException {

        getTabulationSingleCsv(fid1, wkt, req, resp);
    }

    private void write(List<Tabulation> tabulations, HttpServletResponse resp, String fid1, String fid2, String wkt, String type) throws IOException {
        //determine x & y field names
        TreeMap<String, String> objects1 = new TreeMap<String, String>();
        TreeMap<String, String> objects2 = new TreeMap<String, String>();
        for (Tabulation t : tabulations) {
            objects1.put(t.getPid1(), t.getName1());
            objects2.put(t.getPid2(), t.getName2());
        }

        //grid
        String[][] grid = new String[objects1.size() + 1][objects2.size() + 1];

        //row and column sort order and labels
        TreeMap<String, Integer> order1 = new TreeMap<String, Integer>();
        TreeMap<String, Integer> order2 = new TreeMap<String, Integer>();
        int pos = 0;
        for (String s : objects1.keySet()) {
            order1.put(s, pos++);
            if(s.equals(objects1.get(s))){
                grid[pos][0] = objects1.get(s);
            } else {
                grid[pos][0] = objects1.get(s) + " (" + s + ") ";
            }
        }
        pos = 0;
        for (String s : objects2.keySet()) {
            order2.put(s, pos++);
            if(s.equals(objects2.get(s))){
                grid[pos][0] = objects2.get(s);
            } else {
                grid[pos][0] = objects2.get(s) + " (" + s + ") ";
            }
        }

        //grid
        for (Tabulation t : tabulations) {
            grid[order1.get(t.getPid1()) + 1][order2.get(t.getPid2()) + 1] = String.valueOf(t.getArea());
        }

        //write to csv or html
        StringBuilder sb = new StringBuilder();
        if(type.equals("html")) {
            sb.append("<h1>\"");
            if(fieldDao.getFieldById(fid1) != null) {
                    sb.append(fieldDao.getFieldById(fid1).getName());
            } else {
                sb.append(fid1);
            }
            sb.append("\" and \"");
            if(fieldDao.getFieldById(fid2) != null) {
                    sb.append(fieldDao.getFieldById(fid2).getName());
            } else {
                sb.append(fid2);
            }
            sb.append("\" (sq km)</h1><br>");
            if(wkt != null && wkt.length() > 0) {
                sb.append("within area ").append(wkt).append("<br>");
            }
            sb.append("<table border=1>");
            for (int i = 0; i < grid.length; i++) {
                sb.append("<tr>");
                for (int j = 0; j < grid[i].length; j++) {
                    sb.append("<td>");
                    if (i == 0 || j == 0) {
                        //row and column headers
                        if (grid[i][j] != null) {
                            sb.append(grid[i][j]);
                        }
                    } else {
                        //data
                        if (grid[i][j] != null) {
                            try {
                                sb.append(String.format("%.2f",Double.parseDouble(grid[i][j]) / 1000000.0));
                            } catch (Exception e) {
                                sb.append(grid[i][j]);
                            }
                        }
                    }
                    sb.append("</td>");
                }
                sb.append("</tr>");
            }
            sb.append("</table>");
        } else {
            for (int i = 0; i < grid.length; i++) {
            if (i > 0) {
                sb.append("\n");
            }
            for (int j = 0; j < grid[i].length; j++) {
                if (i == 0 || j == 0) {
                    if (j > 0) {
                        sb.append(",");
                    }
                    if (grid[i][j] != null) {
                        sb.append("\"").append(grid[i][j].replace("\"", "\"\"")).append("\"");
                    }
                } else {
                    sb.append(",");
                    if (grid[i][j] != null) {
                        sb.append(grid[i][j]);
                    }
                }
            }
        }
        }

        OutputStream os = resp.getOutputStream();
        os.write(sb.toString().getBytes("UTF-8"));
        os.close();
    }
}
