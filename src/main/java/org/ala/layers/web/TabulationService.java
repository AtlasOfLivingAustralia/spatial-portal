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

    private final String WS_TABULATION_LIST = "/tabulations";
    private final String WS_TABULATION_SINGLE = "/tabulation/{fid1}/{type}";

    /*
     * URLs: /tabulation/area/area/{fid1}/{fid2}/html
     *       /tabulation/area/rows/{fid1}/{fid2}/html
     *       /tabulation/area/columns/{fid1}/{fid2}/html
     *       /tabulation/area/area/{fid1}/{fid2}/csv
     *       /tabulation/area/rows/{fid1}/{fid2}/csv
     *       /tabulation/area/columns/{fid1}/{fid2}/csv
     *       /tabulation/area/area/{fid1}/{fid2}/json
     *       /tabulation/area/rows/{fid1}/{fid2}/json
     *       /tabulation/area/columns/{fid1}/{fid2}/json
     *       /tabulations/html
     */  
    
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
     *
     * {type} one of 'area', 'occurrences', 'species'
     * {value} one of 'area', 'rows', 'columns'
     * {output} one of 'html', 'csv', 'json'
     */
    @RequestMapping(value = "/tabulation/{type}/{value}/{fid1}/{fid2}/{output}", method = {RequestMethod.GET, RequestMethod.POST})
    public void getTabulationAreaCsv(
            @PathVariable("type") String type, @PathVariable("value") String value,
            @PathVariable("fid1") String fid1, @PathVariable("fid2") String fid2,
            @PathVariable("output") String output,
            @RequestParam(value = "wkt", required = false, defaultValue = "") String wkt,
            HttpServletRequest req, HttpServletResponse resp) throws IOException {

        List<Tabulation> tabulations = tabulationDao.getTabulation(fid1, fid2, wkt);

        if(type.equals("area")) {
            writeArea(tabulations, resp, fid1, fid2, wkt, output, value);
        } else if(type.equals("occurrences")) {

        } else if(type.equals("species")) {

        }
    }

    private void writeArea(List<Tabulation> tabulations, HttpServletResponse resp, String fid1, String fid2, String wkt, String type, String func) throws IOException {
        //determine x & y field names
        TreeMap<String, String> objects1 = new TreeMap<String, String>();
        TreeMap<String, String> objects2 = new TreeMap<String, String>();

        for (Tabulation t : tabulations) {
            objects1.put(t.getName1() + "|" + t.getPid1(), t.getName1());
            objects2.put(t.getName2() + "|" + t.getPid2(), t.getName2());
        }

        //define grid
        double[][] grid;
        String [] rowHeader;
        String [] colHeader;
        boolean swap = false;
        if (objects1.size() > objects2.size()) {
            TreeMap<String, String> tmp = objects1;
            objects1 = objects2;
            objects2 = tmp;
            swap = true;
        }
        grid = new double[objects2.size()][objects1.size()];
        rowHeader = new String[objects2.size()];
        colHeader = new String[objects1.size()];


        //populate grid
        //row and column sort order and labels
        TreeMap<String, Integer> order1 = new TreeMap<String, Integer>();
        TreeMap<String, Integer> order2 = new TreeMap<String, Integer>();
        for (String s : objects1.keySet()) {
            order1.put(s, 0);
        }
        for (String s : objects2.keySet()) {
            order2.put(s, 0);
        }

        //sort
        int pos = 0;
        for(String s : order1.keySet()) {
            order1.put(s, pos);
            colHeader[pos] = objects1.get(s);
            pos++;
        }
        pos = 0;
        for(String s : order2.keySet()) {
            order2.put(s, pos);
            rowHeader[pos] = objects2.get(s);
            pos++;
        }        

        //grid
        for (Tabulation t : tabulations) {
            if (swap) {
                grid[order2.get(t.getName1() + "|" + t.getPid1())][order1.get(t.getName2() + "|" + t.getPid2())] = t.getArea();
            } else {
                grid[order2.get(t.getName2() + "|" + t.getPid2())][order1.get(t.getName1() + "|" + t.getPid1())] = t.getArea();
            }
        }        

        //define row and column totals
        double[] sumofcolumns = new double[grid.length];
        double[] sumofrows = new double[grid[0].length];
        double total = 0;
        for (int k = 0; k < grid.length; k++) {
            for (int j = 0; j < grid[0].length; j++) {
                sumofcolumns[k] += grid[k][j];
                sumofrows[j] += grid[k][j];
                total += grid[k][j];
            }
        }

        //update grid values
        if(func.equals("rows")) {
            for(int j=0;j<grid[0].length;j++) {
                sumofrows[j] /= (total/100);
            }
            for (int i = 0; i < grid.length; i++) {
                for (int j = 0; j < grid[i].length; j++) {
                    grid[i][j] = grid[i][j] / sumofcolumns[i] * 100.0;
                }
                sumofcolumns[i] = 100;  //avoid rounding errors
            }
        } else if(func.equals("columns")) {
            for (int i = 0; i < grid.length; i++) {
                sumofcolumns[i] /= (total/100);
            }
            for (int j = 0; j < grid[0].length; j++) {
                for (int i = 0; i < grid.length; i++) {
                    grid[i][j] = grid[i][j] / sumofrows[j] * 100.0;
                }
                sumofrows[j] = 100;  //avoid any rounding errors
            }
        } else {
            //convert to sq km
            for (int j = 0; j < grid[0].length; j++) {
                for (int i = 0; i < grid.length; i++) {
                    grid[i][j] /= 1000000.0;
                }
                sumofrows[j] /= 1000000.0;
            }
            for (int i = 0; i < grid.length; i++) {
                sumofcolumns[i] /= 1000000.0;
            }
        }

        //write to csv,json or html
        StringBuilder sb = new StringBuilder();
        if (type.equals("csv")) {
            resp.setContentType("text/plain");
            for (int i = 0; i < grid.length + 1; i++) {
                if (i > 0) {
                    sb.append("\n");
                }
                for (int j = 0; j < grid[0].length + 1; j++) {
                    if (j > 0) {
                        sb.append(",");
                    }
                    if (i == 0) {
                        if (j > 0 && colHeader[j-1] != null) {
                            sb.append("\"").append(colHeader[j-1].replace("\"", "\"\"")).append("\"");
                        }
                    } else if (j == 0) {
                        if (i > 0 && rowHeader[i-1] != null) {
                            sb.append("\"").append(rowHeader[i-1].replace("\"", "\"\"")).append("\"");
                        }
                    } else {
                        if (grid[i-1][j-1] != 0) {
                            sb.append(String.valueOf(grid[i-1][j-1]));
                        }
                    }
                }
                if (i == 0) {
                    if (func.equals("area")){
                        sb.append(",\"Total area\"");
                    } else {
                        sb.append(",\"Total %\"");
                    }
                }  else {
                    sb.append(",").append(sumofcolumns[i - 1]);
                }
            }
            sb.append("\n");
            if (func.equals("area")){
                sb.append("\"Total area\"");
            } else {
                sb.append("\"Total %\"");
            }
            for (int j = 0; j < grid[0].length; j++) {
                sb.append("," + sumofrows[j]);
            }
        } else if (type.equals("json")) {
            resp.setContentType("application/json");
            sb.append("{");
            for (int i = 0; i < grid.length; i++) {
                sb.append("\"").append(rowHeader[i].replace("\"", "\"\"")).append("\":");
                sb.append("{");
                for (int j = 0; j < grid[0].length; j++) {
                    if (grid[i][j] != 0) {
                        sb.append("\"").append(colHeader[j].replace("\"", "\"\"")).append("\":");
                        sb.append(grid[i][j]);
                        sb.append(",");
                    }
                }
                
                if (func.equals("area")){
                    sb.append("\"Total area\":");
                } else {
                    sb.append("\"Total %\":");
                }
                sb.append(String.valueOf(sumofcolumns[i]));

                sb.append("}");
                if (i < grid.length - 1) {
                    sb.append(",");
                }
                sb.append("\n");
            }
            if (func.equals("area")){
                sb.append(",\"Total area\":");                
            } else {
                sb.append(",\"Total %\":");
            }
            sb.append("{");
            for (int j = 0; j < grid[0].length; j++) {
                sb.append("\"").append(colHeader[j].replace("\"", "\"\"")).append("\":");
                sb.append(sumofrows[j]).append(",");
            }
            if (sb.toString().endsWith(",")) {
                sb.deleteCharAt(sb.toString().length() - 1);
            }
            sb.append("\n");
            
            sb.append("}");
            sb.append("}");
        } 
        else {
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
            sb.append("<h3>");
            sb.append(title + "\" (sq km)");
            sb.append("</h3><br>");
            if (wkt != null && wkt.length() > 0) {
                sb.append("within area ").append(wkt).append("<br>");
            }
            sb.append("<table border='1'>");
            // write area table
            for (int i = 0; i < grid.length + 1; i++) {
                sb.append("<tr>");
                for (int j = 0; j < grid[0].length + 1; j++) {
                    sb.append("<td>");

                    if (i == 0) {
                        if (j> 0 && colHeader[j-1] != null) {
                            sb.append(colHeader[j-1]);
                        }
                    } else if (j == 0) {
                        if (i > 0 && rowHeader[i-1] != null) {
                            sb.append(rowHeader[i-1]);
                        }
                    } else {
                        if (grid[i-1][j-1] != 0) {
                            sb.append(String.format("%.2f",grid[i-1][j-1]));
                        }
                    }
                    sb.append("</td>");
                }
                if (i == 0) {
                    sb.append("<td>");
                    if (func.equals("area")){
                        sb.append("Total area");
                    } else{
                        sb.append("Total %");
                    }
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
            if (func.equals("area")){
                sb.append("Total area");
            } else {
                sb.append("Total %");
            }
            sb.append("</td>");
            for (int j = 0; j < grid[0].length; j++) {
                sb.append("<td>");
                sb.append(String.format("%.2f", sumofrows[j]));
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
    
    @RequestMapping(value = "/tabulations/html", method = RequestMethod.GET)
    public void listAvailableTabulationsHtml(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        List<Tabulation> tabulations = tabulationDao.listTabulations();
        //write to string
        StringBuilder sb = new StringBuilder();
            sb.append("<html>");
            sb.append("<head>");
            sb.append("<title>Available tabulation layers</title>");
            sb.append("</head>");
            sb.append("<body");
            sb.append("<basefont size=\"2\" >");
            sb.append("<h2>");
            sb.append("Available tabulation layers");
            sb.append("</h2><br>");
        
        sb.append("<table border=1<tr><td><b>Field 1</b></td><td><b>Field 2</b></td></tr>");
        for (Tabulation t : tabulations) {
            sb.append("<tr><td>");
            //add field 1
            sb.append(t.getName1()).append("</td><td>");
            //add field 2
            sb.append(t.getName2()).append("</td>");
            //add link to area intersection
            sb.append("<td><a href='../tabulation/area/area/").append(t.getFid1());
            sb.append("/").append(t.getFid2()).append("/html'>area html</a></td>");

            sb.append("<td><a href='../tabulation/area/area/").append(t.getFid1());
            sb.append("/").append(t.getFid2()).append("/csv'>area csv</a></td>");

            sb.append("<td><a href='../tabulation/area/area/").append(t.getFid1());
            sb.append("/").append(t.getFid2()).append("/json'>area json</a></td>");
            
            sb.append("<td><a href='../tabulation/area/rows/").append(t.getFid1());
            sb.append("/").append(t.getFid2()).append("/html'>rows html</a></td>");

            sb.append("<td><a href='../tabulation/area/rows/").append(t.getFid1());
            sb.append("/").append(t.getFid2()).append("/csv'>rows csv</a></td>");

            sb.append("<td><a href='../tabulation/area/rows/").append(t.getFid1());
            sb.append("/").append(t.getFid2()).append("/json'>rows json</a></td>");

            sb.append("<td><a href='../tabulation/area/columns/").append(t.getFid1());
            sb.append("/").append(t.getFid2()).append("/html'>columns html</a></td>");

            sb.append("<td><a href='../tabulation/area/columns/").append(t.getFid1());
            sb.append("/").append(t.getFid2()).append("/csv'>columns csv</a></td>");

            sb.append("<td><a href='../tabulation/area/columns/").append(t.getFid1());
            sb.append("/").append(t.getFid2()).append("/json'>columns json</a></td>");

            sb.append("</tr>");
        }
        sb.append("</table></body></html>");
    
        OutputStream os = resp.getOutputStream();
        os.write(sb.toString().getBytes("UTF-8"));
        os.close();

//        ModelMap m = new ModelMap();
//        m.addAttribute("tabulations", tabulations);
//        return new ModelAndView("tabulations/list", m);

    }

    @RequestMapping(value = WS_TABULATION_LIST, method = RequestMethod.GET)
    public
    @ResponseBody
    List<Tabulation> listAvailableTabulations(HttpServletRequest req) throws IOException {

        return tabulationDao.listTabulations();
    }


    /*
     * list distribution table records, GET
     */
    @RequestMapping(value = WS_TABULATION_SINGLE, method = RequestMethod.GET)
    public
    @ResponseBody
    List<Tabulation> getTabulationSingle(@PathVariable("fid1") String fid1,
            @RequestParam(value = "wkt", required = false, defaultValue = "") String wkt,
            HttpServletRequest req) throws IOException {

        return tabulationDao.getTabulationSingle(fid1, wkt);
    }

    /*
     * list distribution table records, GET
     */
    @RequestMapping(value = WS_TABULATION_SINGLE, method = RequestMethod.GET)
    public void getTabulationSingleCsv(@PathVariable("fid1") String fid1,
            @PathVariable("output") String output,
            @RequestParam(value = "wkt", required = false, defaultValue = "") String wkt,
            HttpServletRequest req, HttpServletResponse resp) throws IOException {

        List<Tabulation> tabulations = tabulationDao.getTabulationSingle(fid1, wkt);

        writeArea(tabulations, resp, fid1, null, wkt, output,"area");
    }
}
