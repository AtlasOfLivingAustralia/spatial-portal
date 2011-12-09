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
    /*
     * URLs: /tabulation/area/{fid1}/{fid2}/html
     *       /tabulation/area/rows/{fid1}/{fid2}/html
     *       /tabulation/area/columns/{fid1}/{fid2}/html
     *       /tabulation/area/{fid1}/{fid2}/csv
     *       /tabulation/area/rows/{fid1}/{fid2}/csv
     *       /tabulation/area/columns/{fid1}/{fid2}/csv
     *       /tabulation/area/{fid1}/{fid2}/json
     *       /tabulation/area/rows/{fid1}/{fid2}/json
     *       /tabulation/area/columns/{fid1}/{fid2}/json
     *       /tabulations/html
     */
    private final String WS_TABULATION_LIST = "/tabulations";
    private final String WS_TABULATION_SINGLE = "/tabulation/{fid1}";
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
    @RequestMapping(value = "/tabulation/area/{fid1}/{fid2}/csv", method = {RequestMethod.GET, RequestMethod.POST})
    public void getTabulationAreaCsv(@PathVariable("fid1") String fid1, @PathVariable("fid2") String fid2,
            @RequestParam(value = "wkt", required = false, defaultValue = "") String wkt,
            HttpServletRequest req, HttpServletResponse resp) throws IOException {

        List<Tabulation> tabulations = tabulationDao.getTabulation(fid1, fid2, wkt);

        writeArea(tabulations, resp, fid1, fid2, wkt, "csv","area");
    }

    /*
     * list distribution table records, GET
     */
    @RequestMapping(value = "/tabulation/area/rows/{fid1}/{fid2}/csv", method = {RequestMethod.GET, RequestMethod.POST})
    public void getTabulationAreaRowsCsv(@PathVariable("fid1") String fid1, @PathVariable("fid2") String fid2,
            @RequestParam(value = "wkt", required = false, defaultValue = "") String wkt,
            HttpServletRequest req, HttpServletResponse resp) throws IOException {

        List<Tabulation> tabulations = tabulationDao.getTabulation(fid1, fid2, wkt);

        writeArea(tabulations, resp, fid1, fid2, wkt, "csv","areaRows");
    }

    /*
     * list distribution table records, GET
     */
    @RequestMapping(value = "/tabulation/area/columns/{fid1}/{fid2}/csv", method = {RequestMethod.GET, RequestMethod.POST})
    public void getTabulationAreaColumnsCsv(@PathVariable("fid1") String fid1, @PathVariable("fid2") String fid2,
            @RequestParam(value = "wkt", required = false, defaultValue = "") String wkt,
            HttpServletRequest req, HttpServletResponse resp) throws IOException {

        List<Tabulation> tabulations = tabulationDao.getTabulation(fid1, fid2, wkt);

        writeArea(tabulations, resp, fid1, fid2, wkt, "csv","areaColumns");
    }

    /*
     * list distribution table records, GET
     */
    @RequestMapping(value = "/tabulation/area/{fid1}/{fid2}/html", method = {RequestMethod.GET, RequestMethod.POST})
    public void getTabulationAreaHtml(@PathVariable("fid1") String fid1, @PathVariable("fid2") String fid2,
            @RequestParam(value = "wkt", required = false, defaultValue = "") String wkt,
            HttpServletRequest req, HttpServletResponse resp) throws IOException {

        List<Tabulation> tabulations = tabulationDao.getTabulation(fid1, fid2, wkt);

        writeArea(tabulations, resp, fid1, fid2, wkt, "html","area");
    }

    /*
     * list distribution table records, GET
     */
    @RequestMapping(value = "/tabulation/area/rows/{fid1}/{fid2}/html", method = {RequestMethod.GET, RequestMethod.POST})
    public void getTabulationAreaRowsHtml(@PathVariable("fid1") String fid1, @PathVariable("fid2") String fid2,
            @RequestParam(value = "wkt", required = false, defaultValue = "") String wkt,
            HttpServletRequest req, HttpServletResponse resp) throws IOException {

        List<Tabulation> tabulations = tabulationDao.getTabulation(fid1, fid2, wkt);

        writeArea(tabulations, resp, fid1, fid2, wkt, "html","areaRows");
    }

    /*
     * list distribution table records, GET
     */
    @RequestMapping(value = "/tabulation/area/columns/{fid1}/{fid2}/html", method = {RequestMethod.GET, RequestMethod.POST})
    public void getTabulationAreaColumnsHtml(@PathVariable("fid1") String fid1, @PathVariable("fid2") String fid2,
            @RequestParam(value = "wkt", required = false, defaultValue = "") String wkt,
            HttpServletRequest req, HttpServletResponse resp) throws IOException {

        List<Tabulation> tabulations = tabulationDao.getTabulation(fid1, fid2, wkt);

        writeArea(tabulations, resp, fid1, fid2, wkt, "html","areaColumns");
    }

    /*
     * list distribution table records, GET
     */
    @RequestMapping(value = "/tabulation/area/{fid1}/{fid2}/json", method = {RequestMethod.GET, RequestMethod.POST})
    public void getTabulationAreaJson(@PathVariable("fid1") String fid1, @PathVariable("fid2") String fid2,
            @RequestParam(value = "wkt", required = false, defaultValue = "") String wkt,
            HttpServletRequest req, HttpServletResponse resp) throws IOException {

        List<Tabulation> tabulations = tabulationDao.getTabulation(fid1, fid2, wkt);

        writeArea(tabulations, resp, fid1, fid2, wkt, "json","area");
    }

    /*
     * list distribution table records, GET
     */
    @RequestMapping(value = "/tabulation/area/rows/{fid1}/{fid2}/json", method = {RequestMethod.GET, RequestMethod.POST})
    public void getTabulationAreaRowsJson(@PathVariable("fid1") String fid1, @PathVariable("fid2") String fid2,
            @RequestParam(value = "wkt", required = false, defaultValue = "") String wkt,
            HttpServletRequest req, HttpServletResponse resp) throws IOException {

        List<Tabulation> tabulations = tabulationDao.getTabulation(fid1, fid2, wkt);

        writeArea(tabulations, resp, fid1, fid2, wkt, "json","areaRows");
    }

    /*
     * list distribution table records, GET
     */
    @RequestMapping(value = "/tabulation/area/columns/{fid1}/{fid2}/json", method = {RequestMethod.GET, RequestMethod.POST})
    public void getTabulationAreaColumnsJson(@PathVariable("fid1") String fid1, @PathVariable("fid2") String fid2,
            @RequestParam(value = "wkt", required = false, defaultValue = "") String wkt,
            HttpServletRequest req, HttpServletResponse resp) throws IOException {

        List<Tabulation> tabulations = tabulationDao.getTabulation(fid1, fid2, wkt);

        writeArea(tabulations, resp, fid1, fid2, wkt, "json","areaColumns");
    }

    private void writeArea(List<Tabulation> tabulations, HttpServletResponse resp, String fid1, String fid2, String wkt, String type, String func) throws IOException {
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
        } 
        else {
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
                            if (func.equals("area")){
                                sb.append(Double.parseDouble(grid[i][j]) / 1000000.0);
                            }
                            else if (func.equals("areaRows")) {
                                double temp = Double.parseDouble(grid[i][j]) / 1000000.0 / sumofcolumns[i - 1] * 100.0;
                                sb.append(temp);
                            }
                            else if (func.equals("areaColumns")){
                                double temp = Double.parseDouble(grid[i][j]) / 1000000.0 / sumofrows[j - 1] * 100.0;
                                sb.append(temp);
                            }
                        }
                    }
                }
                if (i == 0) {
                    if (func.equals("area")){
                        sb.append(",\"Total area\"");
                    }
                    else if (func.equals("areaRows")) {
                        sb.append(",\"Total %\"");
                    }
                    else if (func.equals("areaColumns")){
                        sb.append(",\"Total % / number of non-zero classes\"");
                    }
                } 
                else {
                    if (func.equals("area")){
                        sb.append("," + sumofcolumns[i - 1]);
                    }
                    else if (func.equals("areaRows")) {
                        double sumofColumnPercentage = 0.0;
                        for (int k = 1;k < grid[0].length; k++){
                            if (grid[i][k] != null) {
                                sumofColumnPercentage = sumofColumnPercentage + Double.parseDouble(grid[i][k]) / 1000000.0 / sumofcolumns[i - 1] * 100.0;
                            }
                        }
                        sb.append("," + sumofColumnPercentage);
                    }
                    else if (func.equals("areaColumns")){
                        double sumofColumnPercentage = 0.0;
                        for (int k = 1;k < grid[0].length; k++){
                            if (grid[i][k] != null) {
                                sumofColumnPercentage = sumofColumnPercentage + Double.parseDouble(grid[i][k]) / 1000000.0 / sumofrows[k - 1] * 100.0;
                            }
                        }
                    sb.append("," + sumofColumnPercentage);
                    }
                }
            }
            sb.append("\n");
            if (func.equals("area")){
                sb.append("\"Total area\"");
            }
            else if (func.equals("areaRows")) {
                sb.append("\"Total % / number of non-zero classes\"");
            }
            else if (func.equals("areaColumns")){
                sb.append("\"Total %\"");
            }
            for (int j = 1; j < grid[0].length; j++) {
                if (func.equals("area")){
                    sb.append("," + sumofrows[j - 1]);
                }
                else if (func.equals("areaRows")) {
                    double sumofRowPercentage = 0.0;
                    for (int k = 1;k < grid.length; k++){
                        if (grid[k][j] != null) {
                            sumofRowPercentage = sumofRowPercentage + Double.parseDouble(grid[k][j]) / 1000000.0 / sumofcolumns[k - 1] * 100.0;
                        }
                    }
                sb.append("," + sumofRowPercentage);
                }
                else if (func.equals("areaColumns")){
                    double sumofRowPercentage = 0.0;
                    for (int k = 1;k < grid.length; k++){
                        if (grid[k][j] != null) {
                            sumofRowPercentage = sumofRowPercentage + Double.parseDouble(grid[k][j]) / 1000000.0 / sumofrows[j - 1] * 100.0;
                        }
                    }
                sb.append("," + sumofRowPercentage);
                }
            }
        } 
        else if (type.equals("json")) {
            resp.setContentType("application/json");
            sb.append("{");
            for (int i = 1; i < grid.length; i++) {
                sb.append("\"").append(grid[i][0].replace("\"", "\"\"")).append("\":");
                sb.append("{");
                for (int j = 1; j < grid[i].length; j++) {
                    if (grid[i][j] != null) {
                        sb.append("\"").append(grid[0][j].replace("\"", "\"\"")).append("\":");
                        if (func.equals("area")){
                            sb.append(Double.parseDouble(grid[i][j]) / 1000000.0);
                        }
                        else if (func.equals("areaRows")){
                            sb.append(Double.parseDouble(grid[i][j]) / 1000000.0 / sumofcolumns[i - 1] * 100.0);
                        }
                        else if (func.equals("areaColumns")){
                            sb.append(Double.parseDouble(grid[i][j]) / 1000000.0 / sumofrows[j - 1] * 100.0);
                        }
                        sb.append(",");
                    }
                }
                
                if (func.equals("area")){
                    sb.append("\"Total area\":");
                    sb.append(String.format("%.2f", sumofcolumns[i - 1]));
                }
                else if (func.equals("areaRows")){
                    sb.append("\"Total %\":");
                    double sumofColumnPercentage = 0.0;
                    for (int k = 1;k < grid[0].length; k++){
                        if (grid[i][k] != null) {
                            sumofColumnPercentage = sumofColumnPercentage + Double.parseDouble(grid[i][k]) / 1000000.0 / sumofcolumns[i - 1] * 100.0;
                        }
                    }
                    sb.append(sumofColumnPercentage);
                }
                else if (func.equals("areaColumns")){
                    sb.append("\"Total % / number of non-zero classes\":");
                    double sumofColumnPercentage = 0.0;
                    int numOfNonzeroClasses = 0;
                    for (int k = 1;k < grid[0].length; k++){
                        if (grid[i][k] != null) {
                            sumofColumnPercentage = sumofColumnPercentage + Double.parseDouble(grid[i][k]) / 1000000.0 / sumofcolumns[i - 1] * 100.0;
                            numOfNonzeroClasses = numOfNonzeroClasses+1;
                        }
                    }
                    sb.append(sumofColumnPercentage / numOfNonzeroClasses);
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
            if (func.equals("area")){
                sb.append(",\"Total area\":");
                sb.append("{");
                for (int j = 1; j < grid[0].length; j++) {
                    sb.append("\"").append(grid[0][j].replace("\"", "\"\"")).append("\":");
                
                        sb.append(sumofrows[j - 1]+",");
                }
            }            
            else if (func.equals("areaRows")){
                sb.append(",\"Total % / number of non-zero classes\":");
                sb.append("{");
                for (int j = 1; j < grid[0].length; j++) {
                    sb.append("\"").append(grid[0][j].replace("\"", "\"\"")).append("\":");
                    double sumofRowPercentage = 0.0;
                    int numOfNonzeroClasses = 0;
                    for (int k = 1;k < grid.length; k++){
                         if (grid[k][j] != null) {
                            sumofRowPercentage = sumofRowPercentage + Double.parseDouble(grid[k][j]) / 1000000.0 / sumofcolumns[k - 1] * 100.0;
                            numOfNonzeroClasses = numOfNonzeroClasses + 1;
                        }
                    }
                    sb.append(sumofRowPercentage / numOfNonzeroClasses);
                    sb.append(",");
                }
            }
            else if (func.equals("areaColumns")){
                sb.append(",\"Total % \":");
                sb.append("{");
                for (int j = 1; j < grid[0].length; j++) {
                    sb.append("\"").append(grid[0][j].replace("\"", "\"\"")).append("\":");
                    double sumofRowPercentage = 0.0;
                    for (int k = 1;k < grid.length; k++){
                         if (grid[k][j] != null) {
                            sumofRowPercentage = sumofRowPercentage + Double.parseDouble(grid[k][j]) / 1000000.0 / sumofcolumns[k - 1] * 100.0;
                        }
                    }
                    sb.append(sumofRowPercentage);
                    sb.append(",");
                }
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
            if (func.equals("area")){
                sb.append(title + "\" (sq km)");
            }
            else if (func.equals("areaRows")){
                sb.append(title + "\" (% of row area)");
            }
            else if (func.equals("areaColumns")){
                sb.append(title + "\" (% of column area)");
            }
            sb.append("</h3><br>");
            if (wkt != null && wkt.length() > 0) {
                sb.append("within area ").append(wkt).append("<br>");
            }
            sb.append("<table border='1' style=\"right\">" );
            // write area table
            for (int i = 0; i < grid.length; i++) {
                sb.append("<tr valign=\"middle\">");
                for (int j = 0; j < grid[i].length; j++) {                    
                    if (i == 0 || j == 0) {
                        sb.append("<td>");
                        //row and column headers
                        if (grid[i][j] != null) {                           
                            sb.append(grid[i][j]);
                        }
                    } else {
                        sb.append("<td align=\"right\">");
                        //data
                        if (grid[i][j] != null) {                            
                            try {
                                if (func.equals("area")){
                                    sb.append(String.format("%.2f", Double.parseDouble(grid[i][j]) / 1000000.0));
                                }
                                else if (func.equals("areaRows")){
                                    sb.append(String.format("%.2f", Double.parseDouble(grid[i][j]) / 1000000.0 / sumofcolumns[i - 1] * 100.0));
                                }
                                else if (func.equals("areaColumns")){
                                    sb.append(String.format("%.2f", Double.parseDouble(grid[i][j]) / 1000000.0 / sumofrows[j - 1] * 100.0));
                                }
                            } catch (Exception e) {
                                sb.append(grid[i][j]);
                            }
                        }
                    }
                    sb.append("</td>");
                }
                if (i == 0) {
                    sb.append("<td>");
                    if (func.equals("area")){
                        sb.append("Total area");
                    }
                    else if (func.equals("areaRows")){
                        sb.append("Total %");
                    }
                    else if (func.equals("areaColumns")){
                        sb.append("Total % / number of non-zero classes");
                    }
                    sb.append("</td>");
                } else {
                    sb.append("<td align=\"right\">");
                    if (func.equals("area")){
                        sb.append(String.format("%.2f", sumofcolumns[i - 1]));
                    }
                    else if (func.equals("areaRows")){
                        double sumofColumnPercentage = 0.0;
                        for (int k = 1;k < grid[0].length; k++){
                            if (grid[i][k] != null) {
                                sumofColumnPercentage = sumofColumnPercentage + Double.parseDouble(grid[i][k]) / 1000000.0 / sumofcolumns[i - 1] * 100.0;
                            }
                        }
                        sb.append(String.format("%.2f", sumofColumnPercentage));
                    }
                    else if (func.equals("areaColumns")){
                        double sumofColumnPercentage = 0.0;
                        int numOfNonzeroClasses = 0;
                        for (int k = 1;k < grid[0].length; k++){
                            if (grid[i][k] != null) {
                                sumofColumnPercentage = sumofColumnPercentage + Double.parseDouble(grid[i][k]) / 1000000.0 / sumofrows[k - 1] * 100.0;
                                numOfNonzeroClasses = numOfNonzeroClasses + 1;
                            }
                            
                        }
                        sb.append(String.format("%.2f", sumofColumnPercentage/numOfNonzeroClasses));
                    }
                    sb.append("</td>");
                }
                sb.append("</tr>");
            }
            sb.append("<tr valign=\"middle\">");
            sb.append("<td>");
            if (func.equals("area")){
                sb.append("Total area");
            }
            else if (func.equals("areaRows")){
                sb.append("Total % / number of non-zero classes");
            }
            else if (func.equals("areaColumns")){
                sb.append("Total %");
            }
            sb.append("</td>");
            for (int j = 1; j < grid[0].length; j++) {
                sb.append("<td align='right' >");
                if (func.equals("area")){
                    sb.append(String.format("%.2f", sumofrows[j - 1]));
                }
                else if (func.equals("areaRows")){
                    double sumofRowPercentage = 0.0;
                    int numOfNonzeroClasses = 0;
                    for (int k = 1;k < grid.length; k++){
                        if (grid[k][j] != null) {
                            sumofRowPercentage = sumofRowPercentage + Double.parseDouble(grid[k][j]) / 1000000.0 / sumofcolumns[k - 1] * 100.0;
                            numOfNonzeroClasses = numOfNonzeroClasses + 1;
                        }
                    }
                    sb.append(String.format("%.2f", sumofRowPercentage/numOfNonzeroClasses));
                }
                else if (func.equals("areaColumns")){
                    double sumofRowPercentage = 0.0;
                    for (int k = 1;k < grid.length; k++){
                        if (grid[k][j] != null) {
                            sumofRowPercentage = sumofRowPercentage + Double.parseDouble(grid[k][j]) / 1000000.0 / sumofrows[j - 1] * 100.0;
                        }
                    }
                    sb.append(String.format("%.2f", sumofRowPercentage));
                }
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
    
    @RequestMapping(value = WS_TABULATION_LIST, method = RequestMethod.GET)
    public void listAvailableTabulationsHtml(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        List<Tabulation> tabulations = tabulationDao.listTabulations();
        //write to string
        StringBuilder sb = new StringBuilder();
        sb.append("<html>");
        sb.append("<head>");
        sb.append("<title>Available tabulation layers</title>");
        sb.append("<script type=\"text/javascript\" src=\"/layers-service/javascript/SortingTable.js\"></script>");           
        sb.append("</head>");
        sb.append("<body");
        sb.append("<basefont size=\"2\" >");
        sb.append("<h2>");
        sb.append("Available tabulation layers");
        sb.append("</h2>");                    
        sb.append("<table border=1 class=\"sortable\">");  
        sb.append("<thead>");
        sb.append("<tr>");
        sb.append("<th style=\"-moz-user-select: none;\" class=\"sortable-keep fd-column-0\"><a title=\"Sort on Field\" href=\"#\">Field 1</a></th>");
        sb.append("<th style=\"-moz-user-select: none;\" class=\"sortable-numeric fd-column-1\"><a title=\"Sort on Field\" href=\"#\">Field 2</a></th>");
        sb.append("<th>Area intersection </th>");
        sb.append("<th>Area intersection (row %)</th>");
        sb.append("<th>Area intersection (column %)</th>");
        sb.append("</tr>");
        sb.append("</thead>");
        sb.append("<tbody>");

        
        for (Tabulation t : tabulations) {
            sb.append("<tr class=\"\">");
            sb.append("<td>");
            //add field 1
            sb.append(t.getName1()).append("</td><td>");
            //add field 2
            sb.append(t.getName2()).append("</td><td>");
            //add link to area intersection (html)
            sb.append("<a href='tabulation/area/").append(t.getFid1());
            sb.append("/").append(t.getFid2()).append("/html'>html</a>; ");
            //add link to area intersection (csv)
            sb.append("<a href='tabulation/area/").append(t.getFid1());
            sb.append("/").append(t.getFid2()).append("/csv'>csv</a>; ");
            //add link to area intersection (json)
            sb.append("<a href='tabulation/area/").append(t.getFid1());
            sb.append("/").append(t.getFid2()).append("/json'>json</a>");
            sb.append("</td><td>");
            //add link to area intersection rows % (html)
            sb.append("<a href='tabulation/area/rows/").append(t.getFid1());
            sb.append("/").append(t.getFid2()).append("/html'>html</a>; ");
            //add link to area intersection rows % (csv)
            sb.append("<a href='tabulation/area/rows/").append(t.getFid1());
            sb.append("/").append(t.getFid2()).append("/csv'>csv</a>; ");
            //add link to area intersection rows % (json)
            sb.append("<a href='tabulation/area/rows/").append(t.getFid1());
            sb.append("/").append(t.getFid2()).append("/json'>json</a>");
            sb.append("</td><td>");
            //add link to area intersection columns % (html)
            sb.append("<a href='tabulation/area/columns/").append(t.getFid1());
            sb.append("/").append(t.getFid2()).append("/html'>html</a>; ");
            //add link to area intersection columns % (csv)
            sb.append("<a href='tabulation/area/columns/").append(t.getFid1());
            sb.append("/").append(t.getFid2()).append("/csv'>csv</a>; ");
            //add link to area intersection columns % (json)
            sb.append("<a href='tabulation/area/columns/").append(t.getFid1());
            sb.append("/").append(t.getFid2()).append("/json'>json</a>");
            sb.append("</td>");
            sb.append("</tr>");
        }
        
        sb.append("</tbody>");
  
        sb.append("</table>");

        sb.append("</body></html>");    
    
        OutputStream os = resp.getOutputStream();
        os.write(sb.toString().getBytes("UTF-8"));
        os.close();

//        ModelMap m = new ModelMap();
//        m.addAttribute("tabulations", tabulations);
//        return new ModelAndView("tabulations/list", m);

    }
    

    
    /*
     * list distribution table records, GET
     */
    @RequestMapping(value = "/tabulation/area/{fid1}/{type}", method = {RequestMethod.GET, RequestMethod.POST})
    public void getTabulationAreaSingleHtml(@PathVariable("fid1") String fid1,@PathVariable("type") String type,
            @RequestParam(value = "wkt", required = false, defaultValue = "") String wkt,
            HttpServletRequest req, HttpServletResponse resp) throws IOException {
        List<Tabulation> tabulations = tabulationDao.getTabulationSingle(fid1,wkt);
        writeArea(tabulations, resp, fid1,null,wkt,type,"area");
        
    }

}