/**
 * ************************************************************************
 * Copyright (C) 2010 Atlas of Living Australia All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 * *************************************************************************
 */
package org.ala.layers.web;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.TreeMap;
import javax.annotation.Resource;
import javax.measure.quantity.Length;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.ala.layers.dao.FieldDAO;
import org.ala.layers.dao.TabulationDAO;
import org.ala.layers.dto.Tabulation;
import org.apache.log4j.Logger;
import org.springframework.ui.ModelMap;
import org.springframework.web.servlet.ModelAndView;
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
    public @ResponseBody
    List<Tabulation> getTabulation(@PathVariable("fid1") String fid1, @PathVariable("fid2") String fid2,
            @RequestParam(value = "wkt", required = false, defaultValue = "") String wkt,
            HttpServletRequest req) throws IOException {

        return tabulationDao.getTabulation(fid1, fid2, wkt);
    }

    /*
     * list distribution table records, GET
     */
    @RequestMapping(value = WS_TABULATION_CSV, method = RequestMethod.GET)
    public void getTabulationCsv(@PathVariable("fid1") String fid1, @PathVariable("fid2") String fid2,
            @RequestParam(value = "wkt", required = false, defaultValue = "") String wkt,
            HttpServletRequest req, HttpServletResponse resp) throws IOException {

        List<Tabulation> tabulations = tabulationDao.getTabulation(fid1, fid2, wkt);

        write(tabulations, resp, fid1, fid2, wkt, "csv");
    }

    /*
     * list distribution table records, GET
     */
    @RequestMapping(value = "/tabulation/area/{fid1}/{fid2}/csv", method = RequestMethod.GET)
    public void getTabulationAreaCsv(@PathVariable("fid1") String fid1, @PathVariable("fid2") String fid2,
            @RequestParam(value = "wkt", required = false, defaultValue = "") String wkt,
            HttpServletRequest req, HttpServletResponse resp) throws IOException {

        List<Tabulation> tabulations = tabulationDao.getTabulation(fid1, fid2, wkt);

        writeArea(tabulations, resp, fid1, fid2, wkt, "csv");
    }

    /*
     * list distribution table records, GET
     */
    @RequestMapping(value = "/tabulation/area/rows/{fid1}/{fid2}/csv", method = RequestMethod.GET)
    public void getTabulationAreaRowsCsv(@PathVariable("fid1") String fid1, @PathVariable("fid2") String fid2,
            @RequestParam(value = "wkt", required = false, defaultValue = "") String wkt,
            HttpServletRequest req, HttpServletResponse resp) throws IOException {

        List<Tabulation> tabulations = tabulationDao.getTabulation(fid1, fid2, wkt);

        writeAreaRows(tabulations, resp, fid1, fid2, wkt, "csv");
    }

    /*
     * list distribution table records, GET
     */
    @RequestMapping(value = "/tabulation/area/columns/{fid1}/{fid2}/csv", method = RequestMethod.GET)
    public void getTabulationAreaColumnsCsv(@PathVariable("fid1") String fid1, @PathVariable("fid2") String fid2,
            @RequestParam(value = "wkt", required = false, defaultValue = "") String wkt,
            HttpServletRequest req, HttpServletResponse resp) throws IOException {

        List<Tabulation> tabulations = tabulationDao.getTabulation(fid1, fid2, wkt);

        writeAreaColumns(tabulations, resp, fid1, fid2, wkt, "csv");
    }

    /*
     * list distribution table records, GET
     */
    @RequestMapping(value = WS_TABULATION_HTML, method = RequestMethod.GET)
    public void getTabulationHtml(@PathVariable("fid1") String fid1, @PathVariable("fid2") String fid2,
            @RequestParam(value = "wkt", required = false, defaultValue = "") String wkt,
            HttpServletRequest req, HttpServletResponse resp) throws IOException {

        List<Tabulation> tabulations = tabulationDao.getTabulation(fid1, fid2, wkt);

        write(tabulations, resp, fid1, fid2, wkt, "html");
    }

    /*
     * list distribution table records, GET
     */
    @RequestMapping(value = "/tabulation/area/{fid1}/{fid2}/html", method = RequestMethod.GET)
    public void getTabulationAreaHtml(@PathVariable("fid1") String fid1, @PathVariable("fid2") String fid2,
            @RequestParam(value = "wkt", required = false, defaultValue = "") String wkt,
            HttpServletRequest req, HttpServletResponse resp) throws IOException {

        List<Tabulation> tabulations = tabulationDao.getTabulation(fid1, fid2, wkt);

        writeArea(tabulations, resp, fid1, fid2, wkt, "html");
    }

    /*
     * list distribution table records, GET
     */
    @RequestMapping(value = "/tabulation/area/rows/{fid1}/{fid2}/html", method = RequestMethod.GET)
    public void getTabulationAreaRowsHtml(@PathVariable("fid1") String fid1, @PathVariable("fid2") String fid2,
            @RequestParam(value = "wkt", required = false, defaultValue = "") String wkt,
            HttpServletRequest req, HttpServletResponse resp) throws IOException {

        List<Tabulation> tabulations = tabulationDao.getTabulation(fid1, fid2, wkt);

        writeAreaRows(tabulations, resp, fid1, fid2, wkt, "html");
    }

    /*
     * list distribution table records, GET
     */
    @RequestMapping(value = "/tabulation/area/columns/{fid1}/{fid2}/html", method = RequestMethod.GET)
    public void getTabulationAreaColumnsHtml(@PathVariable("fid1") String fid1, @PathVariable("fid2") String fid2,
            @RequestParam(value = "wkt", required = false, defaultValue = "") String wkt,
            HttpServletRequest req, HttpServletResponse resp) throws IOException {

        List<Tabulation> tabulations = tabulationDao.getTabulation(fid1, fid2, wkt);

        writeAreaColumns(tabulations, resp, fid1, fid2, wkt, "html");
    }

    @RequestMapping(value = "/tabulation/area/{fid1}/{fid2}/json", method = RequestMethod.GET)
    public void getTabulationAreaJason(@PathVariable("fid1") String fid1, @PathVariable("fid2") String fid2,
            @RequestParam(value = "wkt", required = false, defaultValue = "") String wkt,
            HttpServletRequest req, HttpServletResponse resp) throws IOException {

        List<Tabulation> tabulations = tabulationDao.getTabulation(fid1, fid2, wkt);

        writeArea(tabulations, resp, fid1, fid2, wkt, "json");
    }

    /*
     * list distribution table records, GET
     */
    @RequestMapping(value = "/tabulation/area/rows/{fid1}/{fid2}/json", method = RequestMethod.GET)
    public void getTabulationAreaRowsJason(@PathVariable("fid1") String fid1, @PathVariable("fid2") String fid2,
            @RequestParam(value = "wkt", required = false, defaultValue = "") String wkt,
            HttpServletRequest req, HttpServletResponse resp) throws IOException {

        List<Tabulation> tabulations = tabulationDao.getTabulation(fid1, fid2, wkt);

        writeAreaRows(tabulations, resp, fid1, fid2, wkt, "json");
    }

    /*
     * list distribution table records, GET
     */
    @RequestMapping(value = "/tabulation/area/columns/{fid1}/{fid2}/json", method = RequestMethod.GET)
    public void getTabulationAreaColumnsJason(@PathVariable("fid1") String fid1, @PathVariable("fid2") String fid2,
            @RequestParam(value = "wkt", required = false, defaultValue = "") String wkt,
            HttpServletRequest req, HttpServletResponse resp) throws IOException {

        List<Tabulation> tabulations = tabulationDao.getTabulation(fid1, fid2, wkt);

        writeAreaColumns(tabulations, resp, fid1, fid2, wkt, "json");
    }

    /*
     * list distribution table records, GET
     */
    @RequestMapping(value = WS_TABULATION, method = RequestMethod.POST)
    public @ResponseBody
    List<Tabulation> getTabulationPost(@PathVariable("fid1") String fid1, @PathVariable("fid2") String fid2,
            @RequestParam(value = "wkt", required = false, defaultValue = "") String wkt,
            HttpServletRequest req) throws IOException {

        return tabulationDao.getTabulation(fid1, fid2, wkt);
    }

    /*
     * list distribution table records, GET
     */
    @RequestMapping(value = WS_TABULATION_CSV, method = RequestMethod.POST)
    public void getTabulationCsvPost(@PathVariable("fid1") String fid1, @PathVariable("fid2") String fid2,
            @RequestParam(value = "wkt", required = false, defaultValue = "") String wkt,
            HttpServletRequest req, HttpServletResponse resp) throws IOException {

        getTabulationCsv(fid1, fid2, wkt, req, resp);
    }

    @RequestMapping(value = WS_TABULATION_LIST_HTML, method = RequestMethod.GET)
    public ModelAndView listAvailableTabulationsHtml(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        List<Tabulation> tabulations = tabulationDao.listTabulations();

//        //write to string
//        StringBuilder sb = new StringBuilder();
//        sb.append("<table border=1<tr><td><b>Field 1</b></td><td><b>Field 2</b></td><td><b>link</b></tr>");
//        for (Tabulation t : tabulations) {
//            sb.append("<tr><td>");
//            sb.append(t.getName1()).append("</td><td>");
//            sb.append(t.getName2()).append("</td><td>");
//            sb.append("<a href='../tabulation/").append(t.getFid1());
//            sb.append("/").append(t.getFid2()).append("/html.html'>table</a>");
//            sb.append("</td></tr>");
//        }
//        sb.append("</table>");

//        OutputStream os = resp.getOutputStream();
//        os.write(sb.toString().getBytes("UTF-8"));
//        os.close();

        ModelMap m = new ModelMap();
        m.addAttribute("tabulations", tabulations);
        return new ModelAndView("tabulations/list", m);

    }

    @RequestMapping(value = WS_TABULATION_LIST, method = RequestMethod.GET)
    public @ResponseBody
    List<Tabulation> listAvailableTabulations(HttpServletRequest req) throws IOException {

        return tabulationDao.listTabulations();
    }


    /*
     * list distribution table records, GET
     */
    @RequestMapping(value = WS_TABULATION_SINGLE, method = RequestMethod.GET)
    public @ResponseBody
    List<Tabulation> getTabulationSingle(@PathVariable("fid1") String fid1,
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

        write(tabulations, resp, fid1, null, wkt, "html");
    }

    /*
     * list distribution table records, GET
     */
    @RequestMapping(value = WS_TABULATION_SINGLE, method = RequestMethod.POST)
    public @ResponseBody
    List<Tabulation> getTabulationSinglePost(@PathVariable("fid1") String fid1,
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

        //create grid
        int rows = 0;
        int columns = 0;


        for (Tabulation t : tabulations) {
            objects1.put(t.getPid1(), t.getName1());
            objects2.put(t.getPid2(), t.getName2());
        }

        if (objects1.size() <= objects2.size()) {
            rows = objects2.size();
            columns = objects1.size();
        } else {
            rows = objects1.size();
            columns = objects2.size();
        }
        String[][] grid = new String[rows + 1][columns + 1];

        //populate grid
        if (objects1.size() <= objects2.size()) {

            //row and column sort order and labels
            TreeMap<String, Integer> order1 = new TreeMap<String, Integer>();
            TreeMap<String, Integer> order2 = new TreeMap<String, Integer>();
            int pos = 0;
            for (String s : objects1.keySet()) {
                order1.put(s, pos++);
                /*
                 * if(s.equals(objects1.get(s))){ //grid[pos][0] =
                 * objects1.get(s); grid[0][pos] = objects1.get(s); } else {
                 * grid[0][pos] = objects1.get(s) + " (" + s + ") "; }*
                 */
                grid[0][pos] = objects1.get(s);
            }
            pos = 0;
            for (String s : objects2.keySet()) {
                order2.put(s, pos++);
                /*
                 * if(s.equals(objects2.get(s))){ grid[pos][0] =
                 * objects2.get(s); } else { grid[pos][0] = objects2.get(s) + "
                 * (" + s + ") "; } *
                 */
                grid[pos][0] = objects2.get(s);
            }

            //grid
            for (Tabulation t : tabulations) {
                //grid[order1.get(t.getPid1()) + 1][order2.get(t.getPid2()) + 1] = String.valueOf(t.getArea());
                grid[order2.get(t.getPid2()) + 1][order1.get(t.getPid1()) + 1] = String.valueOf(t.getArea());
            }
        } else {
            //row and column sort order and labels
            TreeMap<String, Integer> order1 = new TreeMap<String, Integer>();
            TreeMap<String, Integer> order2 = new TreeMap<String, Integer>();
            int pos = 0;
            for (String s : objects1.keySet()) {
                order1.put(s, pos++);
                /*
                 * if(s.equals(objects1.get(s))){ grid[pos][0] =
                 * objects1.get(s); } else { grid[pos][0] = objects1.get(s) + "
                 * (" + s + ") "; }
                 *
                 */
                grid[pos][0] = objects1.get(s);
            }
            pos = 0;
            for (String s : objects2.keySet()) {
                order2.put(s, pos++);
                /*
                 * if(s.equals(objects2.get(s))){ grid[0][pos] =
                 * objects2.get(s); } else { grid[0][pos] = objects2.get(s) + "
                 * (" + s + ") "; }*
                 *
                 */
                grid[0][pos] = objects2.get(s);
            }

            //grid
            for (Tabulation t : tabulations) {
                grid[order1.get(t.getPid1()) + 1][order2.get(t.getPid2()) + 1] = String.valueOf(t.getArea());
            }
        }

        //define row totals
        double[] sumofcolumns = new double[grid.length - 1];
        for (int k = 1; k < grid.length; k++) {
            //sum of rows
            for (int j = 1; j < grid[0].length; j++) {
                //not row and column headers
                if (grid[k][j] != null) {
                    sumofcolumns[k - 1] = sumofcolumns[k - 1] + Double.parseDouble(grid[k][j]) / 1000000.0;
                }
            }
        }

        //define column totals
        double[] sumofrows = new double[grid[0].length - 1];
        for (int j = 1; j < grid[0].length; j++) {
            //sum of rows
            for (int k = 1; k < grid.length; k++) {
                //not row and column headers
                if (grid[k][j] != null) {
                    sumofrows[j - 1] = sumofrows[j - 1] + Double.parseDouble(grid[k][j]) / 1000000.0;
                }
            }
        }
        //write to csv or html
        StringBuilder sb = new StringBuilder();
        if (type.equals("html")) {
            resp.setContentType("text/html");
            String title = "\"";
            if (fieldDao.getFieldById(fid1) != null) {
                title += fieldDao.getFieldById(fid1).getName();
            } else {
                title += fid1;
            }
            title += "\" and \"";
            if (fieldDao.getFieldById(fid2) != null) {
                title += fieldDao.getFieldById(fid2).getName();
            } else {
                title += fid2;
            }

            sb.append("<html>");
            sb.append("<title>Tabulation for " + title + "</title>");
            sb.append("<body>");
            sb.append("<h2>");
            sb.append(title + "\" (sq km)");
            sb.append("</h2><br>");
            if (wkt != null && wkt.length() > 0) {
                sb.append("within area ").append(wkt).append("<br>");
            }
            sb.append("<table border='1'>");
            // write area table
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
                                sb.append(String.format("%.2f", Double.parseDouble(grid[i][j]) / 1000000.0));
                            } catch (Exception e) {
                                sb.append(grid[i][j]);
                            }
                        }
                    }
                    sb.append("</td>");
                }
                if (i == 0) {
                    sb.append("<td>");
                    sb.append("Rows in total");
                    sb.append("</td>");
                } else {
                    sb.append("<td>");
                    sb.append(String.format("%.2f", sumofcolumns[i - 1]));
                    sb.append("</td>");
                }
                sb.append("</tr>");
            }
            sb.append("<tr>");
            sb.append("<td>");
            sb.append("Columns in total");
            sb.append("</td>");
            for (int j = 1; j < grid[0].length; j++) {
                sb.append("<td>");
                sb.append(String.format("%.2f", sumofrows[j - 1]));
                sb.append("</td>");
            }
            sb.append("</tr>");
            sb.append("</table>");

            sb.append("<h2>");
            sb.append(title + "\" (row percentage %)");
            sb.append("</h2><br>");
            if (wkt != null && wkt.length() > 0) {
                sb.append("within area ").append(wkt).append("<br>");
            }
            sb.append("<table border='1'>");
            // write row percentage table
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
                                sb.append(String.format("%.2f", Double.parseDouble(grid[i][j]) / 1000000.0 / sumofcolumns[i - 1] * 100.0));
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

            sb.append("<h2>");
            sb.append(title + "\" (column percentage %)");
            sb.append("</h2><br>");
            if (wkt != null && wkt.length() > 0) {
                sb.append("within area ").append(wkt).append("<br>");
            }
            sb.append("<table border='1'>");
            // write column percentage table: assume every column has equal length

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
                                sb.append(String.format("%.2f", Double.parseDouble(grid[i][j]) / 1000000.0 / sumofrows[j - 1] * 100.0));
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
            sb.append("</body></html>");
        } else {
            resp.setContentType("text/plain");
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
                if (i == 0) {
                    sb.append(",Rows in total");
                } else {
                    sb.append(sumofcolumns[i - 1] * 1000000.0);
                }
            }
            sb.append("\n");
            sb.append("Columns in total");
            for (int j = 1; j < grid[0].length; j++) {
                sb.append("," + sumofrows[j - 1] * 1000000.0);
            }
        }

        OutputStream os = resp.getOutputStream();
        os.write(sb.toString().getBytes("UTF-8"));
        os.close();
    }

    private void writeArea(List<Tabulation> tabulations, HttpServletResponse resp, String fid1, String fid2, String wkt, String type) throws IOException {
        //determine x & y field names
        TreeMap<String, String> objects1 = new TreeMap<String, String>();
        TreeMap<String, String> objects2 = new TreeMap<String, String>();

        int rows = 0;
        int columns = 0;


        for (Tabulation t : tabulations) {
            objects1.put(t.getPid1(), t.getName1());
            objects2.put(t.getPid2(), t.getName2());
        }

        //define grid
        if (objects1.size() <= objects2.size()) {
            rows = objects2.size();
            columns = objects1.size();
        } else {
            rows = objects1.size();
            columns = objects2.size();
        }
        String[][] grid = new String[rows + 1][columns + 1];

        //populate grid
        if (objects1.size() <= objects2.size()) {

            //row and column sort order and labels
            TreeMap<String, Integer> order1 = new TreeMap<String, Integer>();
            TreeMap<String, Integer> order2 = new TreeMap<String, Integer>();
            int pos = 0;
            for (String s : objects1.keySet()) {
                order1.put(s, pos++);
                /*
                 * if(s.equals(objects1.get(s))){ //grid[pos][0] =
                 * objects1.get(s); grid[0][pos] = objects1.get(s); } else {
                 * grid[0][pos] = objects1.get(s) + " (" + s + ") "; }*
                 *
                 */
                grid[0][pos] = objects1.get(s);
            }
            pos = 0;
            for (String s : objects2.keySet()) {
                order2.put(s, pos++);
                /*
                 * if(s.equals(objects2.get(s))){ grid[pos][0] =
                 * objects2.get(s); } else { grid[pos][0] = objects2.get(s) + "
                 * (" + s + ") "; }*
                 *
                 */
                grid[pos][0] = objects2.get(s);
            }

            //grid
            for (Tabulation t : tabulations) {
                //grid[order1.get(t.getPid1()) + 1][order2.get(t.getPid2()) + 1] = String.valueOf(t.getArea());
                grid[order2.get(t.getPid2()) + 1][order1.get(t.getPid1()) + 1] = String.valueOf(t.getArea());
            }
        } else {
            //row and column sort order and labels
            TreeMap<String, Integer> order1 = new TreeMap<String, Integer>();
            TreeMap<String, Integer> order2 = new TreeMap<String, Integer>();
            int pos = 0;
            for (String s : objects1.keySet()) {
                order1.put(s, pos++);
                /*
                 * if(s.equals(objects1.get(s))){ grid[pos][0] =
                 * objects1.get(s); } else { grid[pos][0] = objects1.get(s) + "
                 * (" + s + ") "; }*
                 *
                 */
                grid[pos][0] = objects1.get(s);
            }
            pos = 0;
            for (String s : objects2.keySet()) {
                order2.put(s, pos++);
                /*
                 * if(s.equals(objects2.get(s))){ grid[0][pos] =
                 * objects2.get(s); } else { grid[0][pos] = objects2.get(s) + "
                 * (" + s + ") "; }*
                 *
                 */
                grid[0][pos] = objects2.get(s);
            }

            //grid
            for (Tabulation t : tabulations) {
                grid[order1.get(t.getPid1()) + 1][order2.get(t.getPid2()) + 1] = String.valueOf(t.getArea());
            }
        }

        //define row totals
        double[] sumofcolumns = new double[grid.length - 1];
        for (int k = 1; k < grid.length; k++) {
            //sum of rows
            for (int j = 1; j < grid[0].length; j++) {
                //not row and column headers
                if (grid[k][j] != null) {
                    sumofcolumns[k - 1] = sumofcolumns[k - 1] + Double.parseDouble(grid[k][j]) / 1000000.0;
                }
            }
        }

        //define column totals
        double[] sumofrows = new double[grid[0].length - 1];
        for (int j = 1; j < grid[0].length; j++) {
            //sum of rows
            for (int k = 1; k < grid.length; k++) {
                //not row and column headers
                if (grid[k][j] != null) {
                    sumofrows[j - 1] = sumofrows[j - 1] + Double.parseDouble(grid[k][j]) / 1000000.0;
                }
            }
        }
        //write to csv,json or html
        StringBuilder sb = new StringBuilder();
        if (type.equals("csv")) {
            resp.setContentType("text/plain");
            for (int i = 0; i < grid.length; i++) {
                if (i > 0) {
                    sb.append("\n");
                }
                for (int j = 0; j < grid[i].length; j++) {
                    if (i == 0 || j == 0) {
                        if (i == 0 && j == 0) {
                            sb.append("\"\"");
                        }
                        if (j > 0) {
                            sb.append(",");
                        }
                        if (grid[i][j] != null) {
                            sb.append("\"").append(grid[i][j].replace("\"", "\"\"")).append("\"");
                        }
                    } else {
                        sb.append(",");
                        if (grid[i][j] != null) {
                            sb.append(Double.parseDouble(grid[i][j]) / 1000000.0);
                        }
                    }
                }
                if (i == 0) {
                    sb.append(",\"Rows in total\"");
                } else {
                    sb.append("," + sumofcolumns[i - 1]);
                }
            }
            sb.append("\n");
            sb.append("\"Columns in total\"");
            for (int j = 1; j < grid[0].length; j++) {
                sb.append("," + sumofrows[j - 1]);
            }
        } else if (type.equals("json")) {
            resp.setContentType("application/json");
            sb.append("{");
            for (int i = 1; i < grid.length; i++) {
                sb.append("\"").append(grid[i][0].replace("\"", "\"\"")).append("\":");
                sb.append("{");
                for (int j = 1; j < grid[i].length; j++) {
                    if (grid[i][j] != null) {
                        sb.append("\"").append(grid[0][j].replace("\"", "\"\"")).append("\":");
                        sb.append(Double.parseDouble(grid[i][j]) / 1000000.0 + ",\n");
                    }
                }
                sb.append("\"Rows in total\":");
                sb.append(String.format("%.2f", sumofcolumns[i - 1]));
                sb.append("},\n");
            }
            sb.append("\"Columns in total\":");
            sb.append("{");
            for (int j = 1; j < grid[0].length; j++) {
                sb.append("\"").append(grid[0][j].replace("\"", "\"\"")).append("\":");
                sb.append(sumofrows[j - 1]);
                if (j < grid[0].length - 1) {
                    sb.append(",");
                }
                sb.append("\n");
            }
            sb.append("}");
            sb.append("}");
        } else {
            resp.setContentType("text/html");
            String title = "\"";
            if (fieldDao.getFieldById(fid1) != null) {
                title += fieldDao.getFieldById(fid1).getName();
            } else {
                title += fid1;
            }
            title += "\" and \"";
            if (fieldDao.getFieldById(fid2) != null) {
                title += fieldDao.getFieldById(fid2).getName();
            } else {
                title += fid2;
            }
            //title += "\" (sq km)";

            sb.append("<html>");
            sb.append("<head>");
            sb.append("<title>Tabulation for " + title + "</title>");
            sb.append("</head>");
            sb.append("<body");
            sb.append("<basefont size=\"2\" >");
            sb.append("<h2>");
            sb.append(title + "\" (sq km)");
            sb.append("</h2><br>");
            if (wkt != null && wkt.length() > 0) {
                sb.append("within area ").append(wkt).append("<br>");
            }
            sb.append("<table border='1'>");
            // write area table
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
                                sb.append(String.format("%.2f", Double.parseDouble(grid[i][j]) / 1000000.0));
                            } catch (Exception e) {
                                sb.append(grid[i][j]);
                            }
                        }
                    }
                    sb.append("</td>");
                }
                if (i == 0) {
                    sb.append("<td>");
                    sb.append("Rows in total");
                    sb.append("</td>");
                } else {
                    sb.append("<td>");
                    sb.append(String.format("%.2f", sumofcolumns[i - 1]));
                    sb.append("</td>");
                }
                sb.append("</tr>");
            }
            sb.append("<tr>");
            sb.append("<td>");
            sb.append("Columns in total");
            sb.append("</td>");
            for (int j = 1; j < grid[0].length; j++) {
                sb.append("<td>");
                sb.append(String.format("%.2f", sumofrows[j - 1]));
                sb.append("</td>");
            }
            sb.append("<td></td>");
            sb.append("</tr>");
            sb.append("</table>");
            sb.append("</body></html>");
        }

        OutputStream os = resp.getOutputStream();
        os.write(sb.toString().getBytes("UTF-8"));
        os.close();
    }

    private void writeAreaRows(List<Tabulation> tabulations, HttpServletResponse resp, String fid1, String fid2, String wkt, String type) throws IOException {
        //determine x & y field names
        TreeMap<String, String> objects1 = new TreeMap<String, String>();
        TreeMap<String, String> objects2 = new TreeMap<String, String>();

        int rows = 0;
        int columns = 0;


        for (Tabulation t : tabulations) {
            objects1.put(t.getPid1(), t.getName1());
            objects2.put(t.getPid2(), t.getName2());
        }

        //define grid
        if (objects1.size() <= objects2.size()) {
            rows = objects2.size();
            columns = objects1.size();
        } else {
            rows = objects1.size();
            columns = objects2.size();
        }
        String[][] grid = new String[rows + 1][columns + 1];

        //populate grid
        if (objects1.size() <= objects2.size()) {

            //row and column sort order and labels
            TreeMap<String, Integer> order1 = new TreeMap<String, Integer>();
            TreeMap<String, Integer> order2 = new TreeMap<String, Integer>();
            int pos = 0;
            for (String s : objects1.keySet()) {
                order1.put(s, pos++);
                /*
                 * if(s.equals(objects1.get(s))){ //grid[pos][0] =
                 * objects1.get(s); grid[0][pos] = objects1.get(s); } else {
                 * grid[0][pos] = objects1.get(s) + " (" + s + ") "; }*
                 *
                 */
                grid[0][pos] = objects1.get(s);
            }
            pos = 0;
            for (String s : objects2.keySet()) {
                order2.put(s, pos++);
                /*
                 * if(s.equals(objects2.get(s))){ grid[pos][0] =
                 * objects2.get(s); } else { grid[pos][0] = objects2.get(s) + "
                 * (" + s + ") "; }*
                 *
                 */
                grid[pos][0] = objects2.get(s);
            }

            //grid
            for (Tabulation t : tabulations) {
                //grid[order1.get(t.getPid1()) + 1][order2.get(t.getPid2()) + 1] = String.valueOf(t.getArea());
                grid[order2.get(t.getPid2()) + 1][order1.get(t.getPid1()) + 1] = String.valueOf(t.getArea());
            }
        } else {
            //row and column sort order and labels
            TreeMap<String, Integer> order1 = new TreeMap<String, Integer>();
            TreeMap<String, Integer> order2 = new TreeMap<String, Integer>();
            int pos = 0;
            for (String s : objects1.keySet()) {
                order1.put(s, pos++);
                /*
                 * if(s.equals(objects1.get(s))){ grid[pos][0] =
                 * objects1.get(s); } else { grid[pos][0] = objects1.get(s) + "
                 * (" + s + ") "; }*
                 *
                 */
                grid[pos][0] = objects1.get(s);
            }
            pos = 0;
            for (String s : objects2.keySet()) {
                order2.put(s, pos++);
                if (s.equals(objects2.get(s))) {
                    grid[0][pos] = objects2.get(s);
                } else {
                    grid[0][pos] = objects2.get(s) + " (" + s + ") ";
                }
            }

            //grid
            for (Tabulation t : tabulations) {
                grid[order1.get(t.getPid1()) + 1][order2.get(t.getPid2()) + 1] = String.valueOf(t.getArea());
            }
        }

        //define row totals
        double[] sumofcolumns = new double[grid.length - 1];
        for (int k = 1; k < grid.length; k++) {
            //sum of rows
            for (int j = 1; j < grid[0].length; j++) {
                //not row and column headers
                if (grid[k][j] != null) {
                    sumofcolumns[k - 1] = sumofcolumns[k - 1] + Double.parseDouble(grid[k][j]) / 1000000.0;
                }
            }
        }

        //define column totals
        double[] sumofrows = new double[grid[0].length - 1];
        for (int j = 1; j < grid[0].length; j++) {
            //sum of rows
            for (int k = 1; k < grid.length; k++) {
                //not row and column headers
                if (grid[k][j] != null) {
                    sumofrows[j - 1] = sumofrows[j - 1] + Double.parseDouble(grid[k][j]) / 1000000.0;
                }
            }
        }
        //write to csv or html
        StringBuilder sb = new StringBuilder();
        if (type.equals("html")) {
            resp.setContentType("text/html");
            String title = "\"";
            if (fieldDao.getFieldById(fid1) != null) {
                title += fieldDao.getFieldById(fid1).getName();
            } else {
                title += fid1;
            }
            title += "\" and \"";
            if (fieldDao.getFieldById(fid2) != null) {
                title += fieldDao.getFieldById(fid2).getName();
            } else {
                title += fid2;
            }
            //title += "\" (sq km)";

            sb.append("<html>");
            sb.append("<title>Tabulation for " + title + "</title>");
            sb.append("<body>");

            sb.append("<h2>");
            sb.append(title + "\" (row percentage %)");
            sb.append("</h2><br>");
            if (wkt != null && wkt.length() > 0) {
                sb.append("within area ").append(wkt).append("<br>");
            }
            sb.append("<table border='1'>");
            // write row percentage table
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
                                sb.append(String.format("%.2f", Double.parseDouble(grid[i][j]) / 1000000.0 / sumofcolumns[i - 1] * 100.0));
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

            sb.append("</body></html>");
        } else if (type.equals("csv")) {
            resp.setContentType("text/plain");
            for (int i = 0; i < grid.length; i++) {
                if (i > 0) {
                    sb.append("\n");
                }
                for (int j = 0; j < grid[i].length; j++) {
                    if (i == 0 || j == 0) {
                        if (i == 0 && j == 0) {
                            sb.append("\"\"");
                        }
                        if (j > 0) {
                            sb.append(",");
                        }
                        if (grid[i][j] != null) {
                            sb.append("\"").append(grid[i][j].replace("\"", "\"\"")).append("\"");
                        }
                    } else {
                        sb.append(",");
                        if (grid[i][j] != null) {
                            double temp = Double.parseDouble(grid[i][j]) / 1000000.0 / sumofcolumns[i - 1] * 100.0;
                            sb.append(temp);
                        }
                    }
                }
            }
        } else {
            resp.setContentType("application/json");
            sb.append("{");
            for (int i = 1; i < grid.length; i++) {
                sb.append("\"").append(grid[i][0].replace("\"", "\"\"")).append("\":");
                sb.append("{");
                for (int j = 1; j < grid[i].length; j++) {
                    if (grid[i][j] != null) {
                        sb.append("\"").append(grid[0][j].replace("\"", "\"\"")).append("\":");
                        sb.append(Double.parseDouble(grid[i][j]) / 1000000.0 / sumofcolumns[i - 1] * 100.0);
                        sb.append(",");
                    }
                }
                if (sb.toString().endsWith(",")) {
                    sb.deleteCharAt(sb.toString().length() - 1);
                }
                sb.append("}");
                if (i < grid.length - 1) {
                    sb.append(",");
                }
                sb.append("\n");
            }
            sb.append("}");
        }

        OutputStream os = resp.getOutputStream();
        os.write(sb.toString().getBytes("UTF-8"));
        os.close();
    }

    private void writeAreaColumns(List<Tabulation> tabulations, HttpServletResponse resp, String fid1, String fid2, String wkt, String type) throws IOException {
        //determine x & y field names
        TreeMap<String, String> objects1 = new TreeMap<String, String>();
        TreeMap<String, String> objects2 = new TreeMap<String, String>();

        int rows = 0;
        int columns = 0;


        for (Tabulation t : tabulations) {
            objects1.put(t.getPid1(), t.getName1());
            objects2.put(t.getPid2(), t.getName2());
        }

        //define grid
        if (objects1.size() <= objects2.size()) {
            rows = objects2.size();
            columns = objects1.size();
        } else {
            rows = objects1.size();
            columns = objects2.size();
        }
        String[][] grid = new String[rows + 1][columns + 1];

        //populate grid
        if (objects1.size() <= objects2.size()) {

            //row and column sort order and labels
            TreeMap<String, Integer> order1 = new TreeMap<String, Integer>();
            TreeMap<String, Integer> order2 = new TreeMap<String, Integer>();
            int pos = 0;
            for (String s : objects1.keySet()) {
                order1.put(s, pos++);
                /*
                 * if(s.equals(objects1.get(s))){ //grid[pos][0] =
                 * objects1.get(s); grid[0][pos] = objects1.get(s); } else {
                 * grid[0][pos] = objects1.get(s) + " (" + s + ") "; }*
                 *
                 */
                grid[0][pos] = objects1.get(s);
            }
            pos = 0;
            for (String s : objects2.keySet()) {
                order2.put(s, pos++);
                /*
                 * if(s.equals(objects2.get(s))){ grid[pos][0] =
                 * objects2.get(s); } else { grid[pos][0] = objects2.get(s) + "
                 * (" + s + ") "; }*
                 *
                 */
                grid[pos][0] = objects2.get(s);
            }

            //grid
            for (Tabulation t : tabulations) {
                //grid[order1.get(t.getPid1()) + 1][order2.get(t.getPid2()) + 1] = String.valueOf(t.getArea());
                grid[order2.get(t.getPid2()) + 1][order1.get(t.getPid1()) + 1] = String.valueOf(t.getArea());
            }
        } else {
            //row and column sort order and labels
            TreeMap<String, Integer> order1 = new TreeMap<String, Integer>();
            TreeMap<String, Integer> order2 = new TreeMap<String, Integer>();
            int pos = 0;
            for (String s : objects1.keySet()) {
                order1.put(s, pos++);
                if (s.equals(objects1.get(s))) {
                    grid[pos][0] = objects1.get(s);
                } else {
                    grid[pos][0] = objects1.get(s) + " (" + s + ") ";
                }
            }
            pos = 0;
            for (String s : objects2.keySet()) {
                order2.put(s, pos++);
                if (s.equals(objects2.get(s))) {
                    grid[0][pos] = objects2.get(s);
                } else {
                    grid[0][pos] = objects2.get(s) + " (" + s + ") ";
                }
            }

            //grid
            for (Tabulation t : tabulations) {
                grid[order1.get(t.getPid1()) + 1][order2.get(t.getPid2()) + 1] = String.valueOf(t.getArea());
            }
        }

        //define row totals
        double[] sumofcolumns = new double[grid.length - 1];
        for (int k = 1; k < grid.length; k++) {
            //sum of rows
            for (int j = 1; j < grid[0].length; j++) {
                //not row and column headers
                if (grid[k][j] != null) {
                    sumofcolumns[k - 1] = sumofcolumns[k - 1] + Double.parseDouble(grid[k][j]) / 1000000.0;
                }
            }
        }

        //define column totals
        double[] sumofrows = new double[grid[0].length - 1];
        for (int j = 1; j < grid[0].length; j++) {
            //sum of rows
            for (int k = 1; k < grid.length; k++) {
                //not row and column headers
                if (grid[k][j] != null) {
                    sumofrows[j - 1] = sumofrows[j - 1] + Double.parseDouble(grid[k][j]) / 1000000.0;
                }
            }
        }
        //write to csv or html
        StringBuilder sb = new StringBuilder();
        if (type.equals("html")) {
            resp.setContentType("text/html");
            String title = "\"";
            if (fieldDao.getFieldById(fid1) != null) {
                title += fieldDao.getFieldById(fid1).getName();
            } else {
                title += fid1;
            }
            title += "\" and \"";
            if (fieldDao.getFieldById(fid2) != null) {
                title += fieldDao.getFieldById(fid2).getName();
            } else {
                title += fid2;
            }
            //title += "\" (sq km)";

            sb.append("<html>");
            sb.append("<title>Tabulation for " + title + "</title>");
            sb.append("<body>");

            sb.append("<h2>");
            sb.append(title + "\" (column percentage %)");
            sb.append("</h2><br>");
            if (wkt != null && wkt.length() > 0) {
                sb.append("within area ").append(wkt).append("<br>");
            }
            sb.append("<table border='1'>");
            // write column percentage table: assume every column has equal length

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
                                sb.append(String.format("%.2f", Double.parseDouble(grid[i][j]) / 1000000.0 / sumofrows[j - 1] * 100.0));
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
            sb.append("</body></html>");
        } else if (type.equals("csv")) {
            resp.setContentType("text/plain");
            for (int i = 0; i < grid.length; i++) {
                if (i > 0) {
                    sb.append("\n");
                }
                for (int j = 0; j < grid[i].length; j++) {
                    if (i == 0 || j == 0) {
                        if (i == 0 && j == 0) {
                            sb.append("\"\"");
                        }
                        if (j > 0) {
                            sb.append(",");
                        }
                        if (grid[i][j] != null) {
                            sb.append("\"").append(grid[i][j].replace("\"", "\"\"")).append("\"");
                        }
                    } else {
                        sb.append(",");
                        if (grid[i][j] != null) {
                            double temp = Double.parseDouble(grid[i][j]) / 1000000.0 / sumofrows[j - 1] * 100.0;
                            sb.append(temp);
                        }
                    }
                }
            }
        } else {
            resp.setContentType("application/json");
            sb.append("{");
            for (int i = 1; i < grid.length; i++) {
                sb.append("\"").append(grid[i][0].replace("\"", "\"\"")).append("\":");
                sb.append("{");
                for (int j = 1; j < grid[i].length; j++) {
                    if (grid[i][j] != null) {
                        sb.append("\"").append(grid[0][j].replace("\"", "\"\"")).append("\":");
                        sb.append(Double.parseDouble(grid[i][j]) / 1000000.0 / sumofrows[j - 1] * 100.0);
                        sb.append(",");
                    }
                }
                if (sb.toString().endsWith(",")) {
                    sb.deleteCharAt(sb.toString().length() - 1);
                }
                sb.append("}");
                if (i < grid.length - 1) {
                    sb.append(",");
                }
                sb.append("\n");
            }
            sb.append("}");
        }
        OutputStream os = resp.getOutputStream();
        os.write(sb.toString().getBytes("UTF-8"));
        os.close();
    }
}
