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
import java.text.DecimalFormat;
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
    private final String WS_TABULATION_SINGLE = "/tabulation/{fid1}";
    /*
     * URLs:    /tabulation/area/{fid1}/{fid2}/{type}
     *          /tabulation/area/rows/{fid1}/{fid2}/{type}
     *          /tabulation/area/columns/{fid1}/{fid2}/{type}
     *          /tabulation/area/total/{fid1}/{fid2}/{type}
     *          /tabulation/occurrences/{fid1}/{fid2}/{type}
     *          /tabulation/occurrences/rows/{fid1}/{fid2}/{type}
     *          /tabulation/occurrences/columns/{fid1}/{fid2}/{type}
     *          /tabulation/occurrences/total/{fid1}/{fid2}/{type}
     *          /tabulation/species/{fid1}/{fid2}/{type}
     *          /tabulation/species/rows/{fid1}/{fid2}/{type}
     *          /tabulation/species/columns/{fid1}/{fid2}/{type}
     *          /tabulation/species/total/{fid1}/{fid2}/{type}
     */

    /*
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
    @RequestMapping(value = "/tabulation/area/{fid1}/{fid2}/{type}", method = {RequestMethod.GET, RequestMethod.POST})
    public void getTabulationArea(@PathVariable("fid1") String fid1, @PathVariable("fid2") String fid2, @PathVariable("type") String type,
            @RequestParam(value = "wkt", required = false, defaultValue = "") String wkt,
            HttpServletRequest req, HttpServletResponse resp) throws IOException {
        List<Tabulation> tabulations = tabulationDao.getTabulation(fid1, fid2, wkt);
        write(tabulations, resp, fid1, fid2, wkt, type,"area");
    }
    
    /*
     * list distribution table records, GET
     */
    @RequestMapping(value = "/tabulation/area/rows/{fid1}/{fid2}/{type}", method = {RequestMethod.GET, RequestMethod.POST})
    public void getTabulationAreaRows(@PathVariable("fid1") String fid1, @PathVariable("fid2") String fid2, @PathVariable("type") String type,
            @RequestParam(value = "wkt", required = false, defaultValue = "") String wkt,
            HttpServletRequest req, HttpServletResponse resp) throws IOException {

        List<Tabulation> tabulations = tabulationDao.getTabulation(fid1, fid2, wkt);

        write(tabulations, resp, fid1, fid2, wkt, type,"area row%");
    }

    /*
     * list distribution table records, GET
     */
    
    @RequestMapping(value = "/tabulation/area/columns/{fid1}/{fid2}/{type}", method = {RequestMethod.GET, RequestMethod.POST})
    public void getTabulationAreaColumns(@PathVariable("fid1") String fid1, @PathVariable("fid2") String fid2, @PathVariable("type") String type,
            @RequestParam(value = "wkt", required = false, defaultValue = "") String wkt,
            HttpServletRequest req, HttpServletResponse resp) throws IOException {

        List<Tabulation> tabulations = tabulationDao.getTabulation(fid1, fid2, wkt);

        write(tabulations, resp, fid1, fid2, wkt, type,"area column%");
    }
    @RequestMapping(value = "/tabulation/area/total/{fid1}/{fid2}/{type}", method = {RequestMethod.GET, RequestMethod.POST})
    public void getTabulationAreaTotal(@PathVariable("fid1") String fid1, @PathVariable("fid2") String fid2, @PathVariable("type") String type,
            @RequestParam(value = "wkt", required = false, defaultValue = "") String wkt,
            HttpServletRequest req, HttpServletResponse resp) throws IOException {

        List<Tabulation> tabulations = tabulationDao.getTabulation(fid1, fid2, wkt);

        write(tabulations, resp, fid1, fid2, wkt, type,"area total%");
    }
    @RequestMapping(value = "/tabulation/occurrences/{fid1}/{fid2}/{type}", method = {RequestMethod.GET, RequestMethod.POST})
    public void getTabulationOccurrences(@PathVariable("fid1") String fid1, @PathVariable("fid2") String fid2, @PathVariable("type") String type,
            @RequestParam(value = "wkt", required = false, defaultValue = "") String wkt,
            HttpServletRequest req, HttpServletResponse resp) throws IOException {
        List<Tabulation> tabulations = tabulationDao.getTabulation(fid1, fid2, wkt);
        write(tabulations, resp, fid1, fid2, wkt, type,"occurrences");
    }    
    @RequestMapping(value = "/tabulation/occurrences/rows/{fid1}/{fid2}/{type}", method = {RequestMethod.GET, RequestMethod.POST})
    public void getTabulationOccurrencesRows(@PathVariable("fid1") String fid1, @PathVariable("fid2") String fid2, @PathVariable("type") String type,
            @RequestParam(value = "wkt", required = false, defaultValue = "") String wkt,
            HttpServletRequest req, HttpServletResponse resp) throws IOException {
        List<Tabulation> tabulations = tabulationDao.getTabulation(fid1, fid2, wkt);
        write(tabulations, resp, fid1, fid2, wkt, type,"occurrences row%");
    }
    @RequestMapping(value = "/tabulation/occurrences/columns/{fid1}/{fid2}/{type}", method = {RequestMethod.GET, RequestMethod.POST})
    public void getTabulationOccurrencesColumns(@PathVariable("fid1") String fid1, @PathVariable("fid2") String fid2, @PathVariable("type") String type,
            @RequestParam(value = "wkt", required = false, defaultValue = "") String wkt,
            HttpServletRequest req, HttpServletResponse resp) throws IOException {
        List<Tabulation> tabulations = tabulationDao.getTabulation(fid1, fid2, wkt);
        write(tabulations, resp, fid1, fid2, wkt, type,"occurrences column%");
    }
    @RequestMapping(value = "/tabulation/occurrences/total/{fid1}/{fid2}/{type}", method = {RequestMethod.GET, RequestMethod.POST})
    public void getTabulationOccurrencesTotal(@PathVariable("fid1") String fid1, @PathVariable("fid2") String fid2, @PathVariable("type") String type,
            @RequestParam(value = "wkt", required = false, defaultValue = "") String wkt,
            HttpServletRequest req, HttpServletResponse resp) throws IOException {

        List<Tabulation> tabulations = tabulationDao.getTabulation(fid1, fid2, wkt);

        write(tabulations, resp, fid1, fid2, wkt, type,"occurrences total%");
    }
    @RequestMapping(value = "/tabulation/species/{fid1}/{fid2}/{type}", method = {RequestMethod.GET, RequestMethod.POST})
    public void getTabulationSpecies(@PathVariable("fid1") String fid1, @PathVariable("fid2") String fid2, @PathVariable("type") String type,
            @RequestParam(value = "wkt", required = false, defaultValue = "") String wkt,
            HttpServletRequest req, HttpServletResponse resp) throws IOException {
        List<Tabulation> tabulations = tabulationDao.getTabulation(fid1, fid2, wkt);
        write(tabulations, resp, fid1, fid2, wkt, type,"species");
    }
    @RequestMapping(value = "/tabulation/species/rows/{fid1}/{fid2}/{type}", method = {RequestMethod.GET, RequestMethod.POST})
    public void getTabulationSpeciesRows(@PathVariable("fid1") String fid1, @PathVariable("fid2") String fid2, @PathVariable("type") String type,
            @RequestParam(value = "wkt", required = false, defaultValue = "") String wkt,
            HttpServletRequest req, HttpServletResponse resp) throws IOException {
        List<Tabulation> tabulations = tabulationDao.getTabulation(fid1, fid2, wkt);
        write(tabulations, resp, fid1, fid2, wkt, type,"species row%");
    }
    @RequestMapping(value = "/tabulation/species/columns/{fid1}/{fid2}/{type}", method = {RequestMethod.GET, RequestMethod.POST})
    public void getTabulationSpeciesColumns(@PathVariable("fid1") String fid1, @PathVariable("fid2") String fid2, @PathVariable("type") String type,
            @RequestParam(value = "wkt", required = false, defaultValue = "") String wkt,
            HttpServletRequest req, HttpServletResponse resp) throws IOException {
        List<Tabulation> tabulations = tabulationDao.getTabulation(fid1, fid2, wkt);
        write(tabulations, resp, fid1, fid2, wkt, type,"species column%");
    }
    @RequestMapping(value = "/tabulation/species/total/{fid1}/{fid2}/{type}", method = {RequestMethod.GET, RequestMethod.POST})
    public void getTabulationSpeciesTotal(@PathVariable("fid1") String fid1, @PathVariable("fid2") String fid2, @PathVariable("type") String type,
            @RequestParam(value = "wkt", required = false, defaultValue = "") String wkt,
            HttpServletRequest req, HttpServletResponse resp) throws IOException {

        List<Tabulation> tabulations = tabulationDao.getTabulation(fid1, fid2, wkt);

        write(tabulations, resp, fid1, fid2, wkt, type,"species total%");
    }
    private void write(List<Tabulation> tabulations, HttpServletResponse resp, String fid1, String fid2, String wkt, String type, String func) throws IOException {
        //determine x & y field names
        TreeMap<String, String> objects1 = new TreeMap<String, String>();
        TreeMap<String, String> objects2 = new TreeMap<String, String>();

        for (Tabulation t : tabulations) {
            objects1.put(t.getPid1(), t.getName1());
            objects2.put(t.getPid2(), t.getName2());
        }
        
        int rows = Math.max(objects1.size(),objects2.size());
        int columns = Math.min(objects1.size(),objects2.size());

        String[][] grid = new String[rows + 1][columns + 1];
        
        //populate grid
        if (objects1.size() <= objects2.size()) {

            //row and column sort order and labels
            TreeMap<String, Integer> order1 = new TreeMap<String, Integer>();
            TreeMap<String, Integer> order2 = new TreeMap<String, Integer>();
            int pos = 0;
            for (String s : objects1.keySet()) {
                order1.put(s, pos++);
                grid[0][pos] = objects1.get(s);
            }
            pos = 0;
            for (String s : objects2.keySet()) {
                order2.put(s, pos++);
                grid[pos][0] = objects2.get(s);
            }

            //grid
            for (Tabulation t : tabulations) {
                if (func.equals("area") || func.equals("area row%") || func.equals("area column%")|| func.equals("area total%")){
                    grid[order2.get(t.getPid2()) + 1][order1.get(t.getPid1()) + 1] = String.valueOf(t.getArea());
                }
                else if (func.equals("occurrences") || func.equals("occurrences row%") || func.equals("occurrences column%") || func.equals("occurrences total%")){
                    grid[order2.get(t.getPid2()) + 1][order1.get(t.getPid1()) + 1] = String.valueOf(t.getOccurrences());
                }
                else if (func.equals("species") || func.equals("species row%") || func.equals("species column%")|| func.equals("species total%")){
                    grid[order2.get(t.getPid2()) + 1][order1.get(t.getPid1()) + 1] = String.valueOf(t.getSpecies());
                }
            }
        } else {
            //row and column sort order and labels
            TreeMap<String, Integer> order1 = new TreeMap<String, Integer>();
            TreeMap<String, Integer> order2 = new TreeMap<String, Integer>();
            int pos = 0;
            for (String s : objects1.keySet()) {
                order1.put(s, pos++);
                grid[pos][0] = objects1.get(s);
            }
            pos = 0;
            for (String s : objects2.keySet()) {
                order2.put(s, pos++);
                grid[0][pos] = objects2.get(s);
            }

            //grid
            for (Tabulation t : tabulations) {                
                if (func.equals("area") || func.equals("area row%") || func.equals("area column%")|| func.equals("area total%")){
                    grid[order1.get(t.getPid1()) + 1][order2.get(t.getPid2()) + 1] = String.valueOf(t.getArea());
                }
                else if (func.equals("occurrences") || func.equals("occurrences row%") || func.equals("occurrences column%")|| func.equals("occurrences total%")){
                    grid[order1.get(t.getPid1()) + 1][order2.get(t.getPid2()) + 1] = String.valueOf(t.getOccurrences());
                }
                else if (func.equals("species") || func.equals("species row%") || func.equals("species column%")|| func.equals("species total%")){
                    grid[order1.get(t.getPid1()) + 1][order2.get(t.getPid2()) + 1] = String.valueOf(t.getSpecies());
                }
            }
        }

        //define row totals
        double[] sumofcolumns = new double[grid.length - 1];

        for (int k = 1; k < grid.length; k++) {
            //sum of rows
            for (int j = 1; j < grid[0].length; j++) {
                //not row and column headers
                if (grid[k][j] != null) {
                    if (func.equals("area") || func.equals("area row%") || func.equals("area column%")|| func.equals("area total%")){
                        sumofcolumns[k - 1] = sumofcolumns[k - 1] + Double.parseDouble(grid[k][j])/1000000.00;
                    }
                    else if (func.equals("occurrences") || func.equals("occurrences row%") || func.equals("occurrences column%")|| func.equals("occurrences total%")|| func.equals("species")|| func.equals("species row%") || func.equals("species column%")|| func.equals("species total%")){
                        sumofcolumns[k - 1] = sumofcolumns[k - 1] + Double.parseDouble(grid[k][j]);
                    }
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
                    if (func.equals("area")|| func.equals("area row%") || func.equals("area column%")|| func.equals("area total%")){
                        sumofrows[j - 1] = sumofrows[j - 1] + Double.parseDouble(grid[k][j])/1000000.00;
                    }
                    else if (func.equals("occurrences") || func.equals("occurrences row%") || func.equals("occurrences column%")|| func.equals("occurrences total%")|| func.equals("species")|| func.equals("species row%") || func.equals("species column%")|| func.equals("species total%")) {
                        sumofrows[j - 1] = sumofrows[j - 1] + Double.parseDouble(grid[k][j]);
                    }
                }
            }
        }

        double[][] gridRowPercentage = new double[rows][columns];
        if (func.equals("area row%") || func.equals("occurrences row%") || func.equals("species row%")){
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < columns; j++) {
                    if (grid[i+1][j+1] != null && sumofcolumns[i] != 0.0) {
                        if (func.equals("area row%")){
                            gridRowPercentage[i][j] = Double.parseDouble(grid[i+1][j+1]) / 1000000.00 / sumofcolumns[i]* 100.00;
                        }
                        else if (func.equals("occurrences row%") || func.equals("species row%") ){
                            gridRowPercentage[i][j] = Double.parseDouble(grid[i+1][j+1]) / sumofcolumns[i]* 100.00;
                        }
                    
                    }
                    else {
                        gridRowPercentage[i][j] = 0;
                    }
                }
            }
        }
        double[][] gridColumnPercentage = new double[rows][columns];
        if (func.equals("area column%") || func.equals("occurrences column%") || func.equals("species column%")){
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < columns; j++) {
                    if (grid[i+1][j+1] != null && sumofrows[j] != 0.0) {
                        if (func.equals("area column%")){
                            gridColumnPercentage[i][j] = Double.parseDouble(grid[i+1][j+1]) / 1000000.00 / sumofrows[j]* 100.00;
                        }
                        else if (func.equals("occurrences column%") || func.equals("species column%") ){
                            gridColumnPercentage[i][j] = Double.parseDouble(grid[i+1][j+1]) / sumofrows[j]* 100.00;
                        }                    
                    }
                    else {
                        gridColumnPercentage[i][j] = 0.0;
                    }
                }
            }
        }

        double[][] gridTotalPercentage = new double[rows][columns];
        double total = 0.0;
        for (int i = 0; i < rows; i++) {
            total += sumofcolumns[i];
        }
        if (func.equals("area total%")|| func.equals("occurrences total%")|| func.equals("species total%")){            
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < columns; j++) {
                    if (grid[i+1][j+1] != null && total != 0.0) {
                        if (func.equals("area total%")){
                            gridTotalPercentage[i][j] = Double.parseDouble(grid[i+1][j+1]) / 1000000.00 / total* 100.00;
                        }
                        else if (func.equals("occurrences total%")|| func.equals("species total%")){
                            gridTotalPercentage[i][j] = Double.parseDouble(grid[i+1][j+1]) / total* 100.00;
                        }
                    }
                    else {
                        gridTotalPercentage[i][j] = 0.0;
                    }
                }
            }
        }
        
        double[] sumofrowsGridRowPercentage = new double[columns];
        double[] sumofrowsGridColumnPercentage = new double[columns];
        double[] sumofrowsGridTotalPercentage = new double[columns];

        for (int j = 0; j < columns; j++) {
            //sum of rows
            for (int i = 0; i < rows; i++) {
                //not row and column headers
                if (grid[i+1][j+1] != null) {
                    if (func.equals("area row%") || func.equals("occurrences row%") || func.equals("species row%") ) {
                        sumofrowsGridRowPercentage[j] = sumofrowsGridRowPercentage[j] + gridRowPercentage[i][j];                        
                    }
                    else if (func.equals("area column%") || func.equals("occurrences column%") || func.equals("species column%") ) {
                        sumofrowsGridColumnPercentage[j] = sumofrowsGridColumnPercentage[j] + gridColumnPercentage[i][j];
                    }
                    else if (func.equals("area total%")|| func.equals("occurrences total%")|| func.equals("species total%")){
                        sumofrowsGridTotalPercentage[j] = sumofrowsGridTotalPercentage[j] + gridTotalPercentage[i][j];
                    }
                }
            }
            //System.out.println("sumofrowsGridRowPercentage["+j+"]:"+sumofrowsGridRowPercentage[j]);
        }
        //define row totals
        
        double[] sumofcolumnsGridRowPercentage = new double[rows];
        double[] sumofcolumnsGridColumnPercentage = new double[rows];
        double[] sumofcolumnsGridTotalPercentage = new double[rows];

        for (int i = 0; i < rows; i++) {
            //sum of rows
            for (int j = 0; j < columns; j++) {
                //not row and column headers
                if (grid[i+1][j+1] != null) {
                    if (func.equals("area row%") || func.equals("occurrences row%") || func.equals("species row%") ) {
                        sumofcolumnsGridRowPercentage[i] = sumofcolumnsGridRowPercentage[i] + gridRowPercentage[i][j];
                    }
                    else if (func.equals("area column%") || func.equals("occurrences column%") || func.equals("species column%") ) {
                        sumofcolumnsGridColumnPercentage[i] = sumofcolumnsGridColumnPercentage[i] + gridColumnPercentage[i][j];
                    }
                    else if (func.equals("area total%")|| func.equals("occurrences total%")|| func.equals("species total%")){
                        System.out.println("sumofcolumnsGridTotalPercentage["+i+"]:"+sumofcolumnsGridTotalPercentage[i]+";gridTotalPercentage:"+gridTotalPercentage[i][j]);
                        sumofcolumnsGridTotalPercentage[i] += gridTotalPercentage[i][j];
                        
                    }
                }
            }
        }
        
        DecimalFormat df0 = new DecimalFormat("###");
        DecimalFormat df1 = new DecimalFormat("##0.0");
        DecimalFormat df2 = new DecimalFormat("##0.00");
        
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
                            if (func.equals("area")) {
                                sb.append("\"Area (square kilometres)\"");
                            }
                            else if (func.equals("area row%")) {
                                sb.append("\"Area: Row%\"");
                            }
                            else if (func.equals("area column%")) {
                                sb.append("\"Area: Column%\"");
                            }
                            else if (func.equals("area total%")) {
                                sb.append("\"Area: Total%\"");
                            }
                            else if (func.equals("occurrences")) {
                                sb.append("\"Number of occurrences\"");
                            }
                            else if (func.equals("occurrences row%")) {
                                sb.append("\"Occurrences: Row%\"");
                            }
                            else if (func.equals("occurrences column%")) {
                                sb.append("\"Occurrences: Column%\"");
                            }
                            else if (func.equals("occurrences total%")) {
                                sb.append("\"Occurrences: Total%\"");
                            }
                            else if (func.equals("species")) {
                                sb.append("\"Number of species\"");
                            }
                            else if (func.equals("species row%")) {
                                sb.append("\"Species: Row%\"");
                            }
                            else if (func.equals("species column%")) {
                                sb.append("\"Species: Column%\"");
                            }
                            else if (func.equals("species total%")) {
                                sb.append("\"Species: Total%\"");
                            }
                            
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
                            if (func.equals("area")||func.equals("occurrences")||func.equals("species")){
                                sb.append(grid[i][j]);
                            }
                            else if (func.equals("area row%")|| func.equals("occurrences row%") || func.equals("species row%")){
                                sb.append(gridRowPercentage[i-1][j-1]/100.00);
                            }
                            else if (func.equals("area column%")|| func.equals("occurrences column%") || func.equals("species column%")){
                                sb.append(gridColumnPercentage[i-1][j-1]/100.00);
                            }
                            else if (func.equals("area total%")|| func.equals("occurrences total%") || func.equals("species total%")){
                                sb.append(gridTotalPercentage[i-1][j-1]/100.00);
                            }
                        }
                    }
                }
                if (i == 0) {
                    if (func.equals("area")){
                        sb.append(",\"Total area\"");
                    }
                    else if (func.equals("occurrences")){
                        sb.append(",\"Total occurrences\"");
                    }
                    else if (func.equals("species")){
                        sb.append(",\"Total species (non-unique)\"");
                    }
                    else if (func.equals("area row%")||func.equals("occurrences row%")||func.equals("species row%")||func.equals("area total%")||func.equals("occurrences total%")||func.equals("species total%")){
                        sb.append(",\"%\"");
                    }
                    else if (func.equals("area column%")||func.equals("occurrences column%")||func.equals("species column%")){
                        sb.append(",\"Average\"");
                    }
                } 
                else {
                    if (func.equals("area")||func.equals("occurrences")||func.equals("species")){
                        sb.append("," + sumofcolumns[i - 1]);
                    }
                    else if (func.equals("area row%")|| func.equals("occurrences row%") || func.equals("species row%")){
                        sb.append(","+ sumofcolumnsGridRowPercentage[i - 1]);
                    }
                    else if (func.equals("area column%")|| func.equals("occurrences column%") || func.equals("species column%")){
                        int numOfNonzeroClasses = 0;
                        for (int k = 1;k < grid[0].length; k++){
                            if (grid[i][k] != null) {
                                numOfNonzeroClasses = numOfNonzeroClasses + 1;
                            }                            
                        }
                        sb.append(","+ sumofcolumnsGridColumnPercentage[i - 1]/numOfNonzeroClasses);
                    }
                    else if (func.equals("area total%")|| func.equals("occurrences total%") || func.equals("species total%")){
                        sb.append(","+ sumofcolumnsGridTotalPercentage[i - 1]);
                    }
                }
            }
            sb.append("\n");
            if (func.equals("area")){
                sb.append("\"Total area\"");
            }
            else if (func.equals("occurrences")){
                sb.append("\"Total occurrences\"");
            }
            else if (func.equals("species")){
                sb.append("\"Total species (non-unique)\"");
            }
            else if (func.equals("area row%")||func.equals("occurrences row%")||func.equals("species row%")||func.equals("area total%")||func.equals("occurrences total%")||func.equals("species total%")){
                sb.append("\"Average\"");
            }
            else if (func.equals("area column%")||func.equals("occurrences column%")||func.equals("species column%")){
                sb.append("\"%\"");
            }
            for (int j = 1; j < grid[0].length; j++) {
                if (func.equals("area")||func.equals("occurrences")||func.equals("species")){
                    sb.append("," + sumofrows[j - 1]);
                }
                else if (func.equals("area row%")|| func.equals("occurrences row%") || func.equals("species row%")){
                    double numOfNonzeroClasses = 0.0;
                    for (int k = 1;k < grid.length; k++){
                        if (grid[k][j] != null) {
                            numOfNonzeroClasses = numOfNonzeroClasses + 1;
                        }                            
                    }
                    sb.append(","+ sumofrowsGridRowPercentage[j - 1]/numOfNonzeroClasses);
                }
                else if (func.equals("area column%")|| func.equals("occurrences column%") || func.equals("species column%")){                    
                    sb.append(","+ sumofrowsGridColumnPercentage[j - 1]);
                }
                else if (func.equals("area total%")|| func.equals("occurrences total%") || func.equals("species total%")){
                        sb.append(","+ sumofrowsGridTotalPercentage[j - 1]);
                }                
            }
            if (func.equals("area total%")|| func.equals("occurrences total%")|| func.equals("species total%")) {
                double temp = 0.0;
                for (int j = 1; j < grid[0].length; j++) {
                    temp += sumofrowsGridTotalPercentage[j - 1];
                }
                sb.append(","+ temp);
            }
            else if (func.equals("area") || func.equals("occurrences") ||func.equals("species")){
                double temp = 0.0;
                for (int j = 1; j < grid[0].length; j++) {
                    temp += sumofrows[j - 1];
                }
                if (func.equals("area")){
                    sb.append(","+ temp);
                }
                else if (func.equals("occurrences") ||func.equals("species")){
                    sb.append(","+ temp);
                }
            }
            else {
                sb.append(",");
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
                        sb.append(grid[i][j]);
                        sb.append(",");
                    }
                }
                if (func.equals("area")){
                    sb.append("\"Total area\":"+sumofcolumns[i - 1]);
                }
                else if (func.equals("occurrences")){
                    sb.append("\"Total occurrences\":"+sumofcolumns[i - 1]);
                }
                else if (func.equals("species")){
                    sb.append("\"Total species (non-unique)\":"+sumofcolumns[i - 1]);
                }
                else if (func.equals("area row%")||func.equals("occurrences row%")||func.equals("species row%")){
                    sb.append("\"%\":"+sumofcolumnsGridRowPercentage[i - 1]);
                }
                else if (func.equals("area column%")||func.equals("occurrences column%")||func.equals("species column%")){
                    int numOfNonzeroClasses = 0;
                        for (int k = 1;k < grid[0].length; k++){
                            if (grid[i][k] != null) {
                                numOfNonzeroClasses = numOfNonzeroClasses + 1;
                            }                            
                        }
                    sb.append("\"Average\":"+sumofcolumnsGridColumnPercentage[i - 1]/numOfNonzeroClasses);
                }
                else if (func.equals("area total%")||func.equals("occurrences total%")||func.equals("species total%")){
                    sb.append("\"%\":"+sumofcolumnsGridTotalPercentage[i - 1]);
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
                }
                else if (func.equals("occurrences")){
                    sb.append(",\"Total occurrences\":");
                }
                else if (func.equals("species")){
                    sb.append(",\"Total species (non-unique)\":");
                }
                else if (func.equals("area row%")||func.equals("occurrences row%")||func.equals("species row%")){
                    sb.append(",\"Average\":");
                }
                else if (func.equals("area column%")||func.equals("occurrences column%")||func.equals("species column%")){
                    sb.append(",\"%\":");
                }
                else if (func.equals("area total%")||func.equals("occurrences total%")||func.equals("species total%")){
                    sb.append(",\"%\":");
                }
                        
            sb.append("{");
            for (int j = 1; j < grid[0].length; j++) {
                sb.append("\"").append(grid[0][j].replace("\"", "\"\"")).append("\":");
                if (func.equals("area")||func.equals("occurrences")||func.equals("species")) {
                    sb.append(sumofrows[j - 1]+",");
                }
                else if (func.equals("area row%") || func.equals("occurrences row%") || func.equals("species row%")) {
                    double numOfNonzeroClasses = 0.0;
                    for (int k = 1;k < grid.length; k++){
                        if (grid[k][j] != null) {
                            numOfNonzeroClasses = numOfNonzeroClasses + 1;
                        }                            
                    }
                sb.append(sumofrowsGridRowPercentage[j - 1]/numOfNonzeroClasses+",");
                }
                else if (func.equals("area column%") || func.equals("occurrences column%") || func.equals("species column%")) {
                    sb.append(sumofrowsGridColumnPercentage[j - 1]+",");
                }
                else if (func.equals("area total%")|| func.equals("occurrences total%")|| func.equals("species total%")) {
                    sb.append(sumofrowsGridTotalPercentage[j - 1]+","); 
                }
            }
            if (func.equals("area total%")|| func.equals("occurrences total%")|| func.equals("species total%")) {
                double temp = 0.0;
                for (int j = 1; j < grid[0].length; j++) {
                    temp += sumofrowsGridTotalPercentage[j - 1];
                }
                sb.append("\"%\":"+temp+",");
            }
            else if (func.equals("area") || func.equals("occurrences") ||func.equals("species")){
                double temp = 0.0;
                for (int j = 1; j < grid[0].length; j++) {
                    temp += sumofrows[j - 1];
                }
                if (func.equals("area")){
                    sb.append("\"Total area\":"+temp+",");
                }
                else if (func.equals("occurrences") ){
                    sb.append("\"Total occurrences\":"+temp+",");
                }
                else if (func.equals("species") ){
                    sb.append("\"Total species (non-unique)\":"+temp+",");
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
            sb.append(title);
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
                        else {
                            if (func.equals("area")) {
                                sb.append("<b>Area (square kilometres)</b>");
                            }
                            else if (func.equals("area row%")) {
                                sb.append("<b>Area: Row%</b>");
                            }
                            else if (func.equals("area column%")) {
                                sb.append("<b>Area: Column%</b>");
                            }
                            else if (func.equals("area total%")) {
                                sb.append("<b>Area: Total%</b>");
                            }
                            else if (func.equals("occurrences")) {
                                sb.append("<b>Number of occurrences</b>");
                            }
                            else if (func.equals("occurrences row%")) {
                                sb.append("<b>Occurrences: Row%</b>");
                            }
                            else if (func.equals("occurrences column%")) {
                                sb.append("<b>Occurrences: Column%</b>");
                            }
                            else if (func.equals("occurrences total%")) {
                                sb.append("<b>Occurrences: Total%</b>");
                            }
                            else if (func.equals("species")) {
                                sb.append("<b>Number of species</b>");
                            }
                            else if (func.equals("species row%")) {
                                sb.append("<b>Species: Row%</b>");
                            }
                            else if (func.equals("species column%")) {
                                sb.append("<b>Species: Column%</b>");
                            }
                            else if (func.equals("species total%")) {
                                sb.append("<b>Species: Total%</b>");
                            }
                        }
                        sb.append("</td>");
                    } else {
                        //data
                        if (grid[i][j] != null) {
                            if (func.equals("area")){
                                //sb.append("<td align=\"right\">"+String.format("%.2f",Double.parseDouble(grid[i][j]) / 1000000.0)+"</td>");
                                sb.append("<td align=\"right\">"+df0.format(Double.parseDouble(grid[i][j]) / 1000000.0)+"</td>");
                            }
                            else if (func.equals("occurrences") || func.equals("species")){
                                sb.append("<td align=\"right\">"+grid[i][j]+"</td>");
                            }
                            else if (func.equals("area row%") || func.equals("occurrences row%") || func.equals("species row%")) {
                                //sb.append("<td align=\"right\">"+String.format("%.1f",gridRowPercentage[i-1][j-1])+"%"+"</td>");
                                sb.append("<td align=\"right\">"+df0.format(gridRowPercentage[i-1][j-1])+"%"+"</td>");
                            }
                            else if (func.equals("area column%") || func.equals("occurrences column%") || func.equals("species column%")){
                                //sb.append("<td align=\"right\">"+String.format("%.1f",gridColumnPercentage[i-1][j-1])+"%"+"</td>");
                                sb.append("<td align=\"right\">"+df0.format(gridColumnPercentage[i-1][j-1])+"%"+"</td>");
                            }
                            else if (func.equals("area total%")|| func.equals("occurrences total%") || func.equals("species total%")){
                                //sb.append("<td align=\"right\">"+String.format("%.1f",gridTotalPercentage[i-1][j-1])+"%"+"</td>");
                                sb.append("<td align=\"right\">"+df1.format(gridTotalPercentage[i-1][j-1])+"%"+"</td>");
                            }
                        }
                        else{
                            sb.append("<td></td>");
                        }
                    }
                    
                }
                if (i == 0) {
                    sb.append("<td border: 5pt>");
                    if (func.equals("area")) {
                        sb.append("<b>Total Area</b>");
                    }
                    else if (func.equals("area row%") || func.equals("occurrences row%") || func.equals("species row%") ) {
                        sb.append("<b>%</b>");
                    }
                    else if (func.equals("area total%")|| func.equals("occurrences total%")|| func.equals("species total%")) {
                        sb.append("<b>Total</b>");
                    }
                    else if (func.equals("area column%") || func.equals("occurrences column%") || func.equals("species column%")) {
                        sb.append("<b>Average</b>");
                    }
                    else if (func.equals("occurrences")) {
                        sb.append("<b>Total occurrences</b>");
                    }                                        
                    else if (func.equals("species")) {
                        sb.append("<b>Total species (non-unique)</b>");
                    }
                    
                    
                    sb.append("</td>");
                } else {
                    sb.append("<td align=\"right\" border: 5pt>");
                    if (func.equals("area") ){
                        //sb.append(String.format("%.2f",sumofcolumns[i - 1]));
                        sb.append(df0.format(sumofcolumns[i - 1]));
                    }
                    else if (func.equals("occurrences") || func.equals("species")){
                        //sb.append((int)sumofcolumns[i - 1]);
                        sb.append(df0.format(sumofcolumns[i - 1]));
                    }
                    else if (func.equals("area row%") || func.equals("occurrences row%") || func.equals("species row%")) {
                        double temp = sumofcolumnsGridRowPercentage[i - 1];
                        //sb.append((int)temp+"%");
                        sb.append(df0.format(temp)+"%");
                    }
                    else if (func.equals("area column%") || func.equals("occurrences column%") || func.equals("species column%")) {
                        int numOfNonzeroClasses = 0;
                        for (int k = 1;k < grid[0].length; k++){
                            if (grid[i][k] != null) {
                                numOfNonzeroClasses = numOfNonzeroClasses + 1;
                            }                            
                        }
                        double temp = sumofcolumnsGridColumnPercentage[i - 1]/numOfNonzeroClasses;
                        //sb.append((int)temp+"%");
                        sb.append(df0.format(temp)+"%");
                    }
                    else if (func.equals("area total%")|| func.equals("occurrences total%")|| func.equals("species total%")) {
                        double temp = sumofcolumnsGridTotalPercentage[i - 1];
                        
                        //sb.append(String.format("%.1f",temp)+"%");
                        sb.append(df1.format(temp)+"%");
                    }
                    sb.append("</td>");
                }
                sb.append("</tr>");
            }
            sb.append("<tr >");
            sb.append("<td>");
            if (func.equals("area")) {
                sb.append("<b>Total Area</b>");
            }
            else if (func.equals("area row%") || func.equals("occurrences row%") || func.equals("species row%")) {
                sb.append("<b>Average</b>");
            }
            else if (func.equals("area column%") || func.equals("occurrences column%") || func.equals("species column%") ) {
                sb.append("<b>％</b>");
            }
            else if (func.equals("area total%")|| func.equals("occurrences total%")|| func.equals("species total%")){
                sb.append("<b>Total</b>");
            }
            else if (func.equals("occurrences")) {
                sb.append("<b>Total occurrences</b>");
            }
            else if (func.equals("species")) {
                sb.append("<b>Total species (non-unique)</b>");
            }
            sb.append("</td>");
            for (int j = 1; j < grid[0].length; j++) {
                sb.append("<td align='right' >");
                if (func.equals("area")){
                    //sb.append(String.format("%.2f",sumofrows[j - 1]));
                    sb.append(df0.format(sumofrows[j - 1]));
                }
                else if (func.equals("occurrences") || func.equals("species")){
                    //sb.append(String.format("%.1f",sumofrows[j - 1]));
                    sb.append(df0.format(sumofrows[j - 1]));
                }
                else if (func.equals("area row%") || func.equals("occurrences row%") || func.equals("species row%")) {
                    double numOfNonzeroClasses = 0.0;
                    for (int k = 1;k < grid.length; k++){
                        if (grid[k][j] != null) {
                            numOfNonzeroClasses = numOfNonzeroClasses + 1;
                        }                            
                    }
                    double temp = sumofrowsGridRowPercentage[j - 1]/numOfNonzeroClasses;                    
                    //sb.append(String.format("%.1f",temp)+"%");
                    sb.append(df0.format(temp)+"%");
                }
                else if (func.equals("area column%") || func.equals("occurrences column%") || func.equals("species column%")) {
                    double temp = sumofrowsGridColumnPercentage[j - 1];                    
                    //sb.append(String.format("%.1f",temp)+"%");
                    sb.append(df0.format(temp)+"%");
                        //sb.append(sumofrowsGridColumnPercentage[j - 1]+"%");
                }
                else if (func.equals("area total%")|| func.equals("occurrences total%")|| func.equals("species total%")) {
                    double temp = sumofrowsGridTotalPercentage[j - 1];                    
                    //sb.append(String.format("%.1f",temp)+"%");
                    sb.append(df1.format(temp)+"%");
                }
                sb.append("</td>");
            }
            if (func.equals("area total%")|| func.equals("occurrences total%")|| func.equals("species total%")) {
                double temp = 0.0;
                for (int j = 1; j < grid[0].length; j++) {
                    temp += sumofrowsGridTotalPercentage[j - 1];
                }
                //sb.append("<td align='right'>"+String.format("%.1f",temp)+"%</td>");
                sb.append("<td align='right'>"+df1.format(temp)+"%</td>");
            }
            else if (func.equals("area") || func.equals("occurrences") ||func.equals("species")){
                double temp = 0.0;
                for (int j = 1; j < grid[0].length; j++) {
                    temp += sumofrows[j - 1];
                }
                if (func.equals("area")){
                    //sb.append("<td align='right'>"+String.format("%.2f",temp)+"</td>");
                    sb.append("<td align='right'>"+df0.format(temp)+"</td>");
                }
                else if (func.equals("occurrences") ||func.equals("species")){
                    //sb.append("<td align='right'>"+String.format("%.1f",temp)+"</td>");
                    sb.append("<td align='right'>"+df0.format(temp)+"</td>");
                }
            }
            else {
                sb.append("<td></td>");
            }
            sb.append("</tr>");                
            
            sb.append("</table>");
            sb.append("<p>");
            sb.append("Blanks = no intersection");
            sb.append("</p>");
            sb.append("<p>");
            sb.append("0 = no records in intersection");
            sb.append("</p>");
            sb.append("</body></html>");
        }

        OutputStream os = resp.getOutputStream();
        os.write(sb.toString().getBytes("UTF-8"));
        os.close();
    }
    
    
    
    @RequestMapping(value = WS_TABULATION_LIST, method = RequestMethod.GET)
    /*public void listAvailableTabulationsHtml(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        List<Tabulation> tabulations = tabulationDao.listTabulations();
        
        // write to string
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
    }
    * 
    */

    public ModelAndView listAvailableTabulationsHtml(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        List<Tabulation> tabulations = tabulationDao.listTabulations();
    
        ModelMap m = new ModelMap();
        m.addAttribute("tabulations", tabulations);
        return new ModelAndView("tabulations/list", m);

    }
    /*
     * list distribution table records, GET
     */
    @RequestMapping(value = "/tabulation/area/{fid1}/{type}", method = {RequestMethod.GET, RequestMethod.POST})
    public void getTabulationAreaSingleHtml(@PathVariable("fid1") String fid1,@PathVariable("type") String type,
            @RequestParam(value = "wkt", required = false, defaultValue = "") String wkt,
            HttpServletRequest req, HttpServletResponse resp) throws IOException {
        List<Tabulation> tabulations = tabulationDao.getTabulationSingle(fid1,wkt);
        write(tabulations, resp, fid1,null,wkt,type,"area");        
    }
    /*@RequestMapping(value = "/tabulation/occurrences/{fid1}/{type}", method = {RequestMethod.GET, RequestMethod.POST})
    public void getTabulationOccurrencesSingleHtml(@PathVariable("fid1") String fid1,@PathVariable("type") String type,
            @RequestParam(value = "wkt", required = false, defaultValue = "") String wkt,
            HttpServletRequest req, HttpServletResponse resp) throws IOException {
        List<Tabulation> tabulations = tabulationDao.getTabulationSingle(fid1,wkt);
        write(tabulations, resp, fid1,null,wkt,type,"occurrences");        
    }
    @RequestMapping(value = "/tabulation/species/{fid1}/{type}", method = {RequestMethod.GET, RequestMethod.POST})
    public void getTabulationSpeciesSingleHtml(@PathVariable("fid1") String fid1,@PathVariable("type") String type,
            @RequestParam(value = "wkt", required = false, defaultValue = "") String wkt,
            HttpServletRequest req, HttpServletResponse resp) throws IOException {
        List<Tabulation> tabulations = tabulationDao.getTabulationSingle(fid1,wkt);
        write(tabulations, resp, fid1,null,wkt,type,"species");        
    }
    * 
    */

}