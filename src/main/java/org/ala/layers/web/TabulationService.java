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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ala.layers.dao.FieldDAO;
import org.ala.layers.dao.TabulationDAO;
import org.ala.layers.dto.Field;
import org.ala.layers.dto.Tabulation;
import org.apache.log4j.Logger;
import org.springframework.ui.ModelMap;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

/**
 * @author Adam
 */
@Controller
public class TabulationService {

    private final String WS_TABULATION_LIST = "/tabulations";
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
    @RequestMapping(value = WS_TABULATION_LIST, method = RequestMethod.GET)
    public ModelAndView listAvailableTabulationsHtml(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        List<Tabulation> tabulations = tabulationDao.listTabulations();

        ModelMap m = new ModelMap();
        m.addAttribute("tabulations", tabulations);
        return new ModelAndView("tabulations/list", m);

    }

    /*
     * list distribution table records, GET
     */
    @RequestMapping(value = "/tabulation/{func1}/{func2}/{fid1}/{fid2}/tabulation.html", method = {RequestMethod.GET, RequestMethod.POST})
    public ModelAndView displayTabulation(@PathVariable("func1") String func1, @PathVariable("func2") String func2, @PathVariable("fid1") String fid1, @PathVariable("fid2") String fid2,
                                          @RequestParam(value = "wkt", required = false, defaultValue = "") String wkt,
                                          HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String func = func1 + func2;
        List<Tabulation> tabulations = tabulationDao.getTabulation(fid1, fid2, wkt);
        return generateTabulation(tabulations, func, fid1, fid2, wkt);
    }

    @RequestMapping(value = "/tabulation/{func1}/{fid1}/{fid2}/tabulation.html", method = {RequestMethod.GET, RequestMethod.POST})
    public ModelAndView displayTabulation(@PathVariable("func1") String func1, @PathVariable("fid1") String fid1, @PathVariable("fid2") String fid2,
                                          @RequestParam(value = "wkt", required = false, defaultValue = "") String wkt,
                                          HttpServletRequest req, HttpServletResponse resp) throws IOException {
        List<Tabulation> tabulations = tabulationDao.getTabulation(fid1, fid2, wkt);
        return generateTabulation(tabulations, func1, fid1, fid2, wkt);
    }

    public ModelAndView generateTabulation(List<Tabulation> tabulations, String func, String fid1, String fid2,
                                           String wkt) throws IOException {

        ModelMap m = new ModelMap();

        Field field1 = fieldDao.getFieldById(fid1);
        Field field2 = fieldDao.getFieldById(fid2);
        String title = "Tabulation for \"";
        if (field1 != null) {
            title += field1.getName();
        } else {
            title += fid1;
        }
        title += "\" and \"";
        if (field2 != null) {
            title += field2.getName();
        } else {
            title += fid2;
        }
        if (func.equals("area")) {
            title += "\"(sq km)";
        } else {
            title += "\"";
        }
        m.addAttribute("title", title);

        String[][] grid = tabulationGridGenerator(tabulations, fid1, fid2, wkt, func);
        double[] sumOfColumns = tabulationSumOfColumnsGenerator(grid, func);
        double[] sumOfRows = tabulationSumOfRowsGenerator(grid, func);
        double[][] gridRowPercentage = tabulationGridRowPercentageGenerator(grid, sumOfColumns, func);
        double[][] gridColumnPercentage = tabulationGridColumnPercentageGenerator(grid, sumOfRows, func);
        double[][] gridTotalPercentage = tabulationGridTotalPercentageGenerator(grid, sumOfColumns, func);
        double[] sumOfRowsGridPercentage = tabulationSumOfRowsGridPercentageGenerator(grid, gridRowPercentage, gridColumnPercentage, gridTotalPercentage, sumOfColumns, func);
        double[] sumOfColumnsGridPercentage = tabulationSumOfColumnsGridPercentageGenerator(grid, gridRowPercentage, gridColumnPercentage, gridTotalPercentage, sumOfColumns, func);

        double Total = 0.0;
        for (int j = 1; j < grid[0].length; j++) {
            Total += sumOfRows[j - 1];
        }

        double TotalPercentage = 0.0;
        for (int j = 1; j < grid[0].length; j++) {
            TotalPercentage += sumOfRowsGridPercentage[j - 1];
        }


        double[] AveragePercentageOverRows = new double[grid[0].length - 1];
        for (int j = 1; j < grid[0].length; j++) {
            double NumOfNonzeroRows = 0.0;
            for (int k = 1; k < grid.length; k++) {
                if (grid[k][j] != null) {
                    NumOfNonzeroRows = NumOfNonzeroRows + 1;
                }
            }
            if (NumOfNonzeroRows != 0.0) {
                AveragePercentageOverRows[j - 1] = sumOfRowsGridPercentage[j - 1] / NumOfNonzeroRows;
            }
        }
        double[] AveragePercentageOverColumns = new double[grid.length - 1];
        for (int i = 1; i < grid.length; i++) {
            double NumOfNonzeroColumns = 0.0;
            for (int k = 1; k < grid[0].length; k++) {
                if (grid[i][k] != null) {
                    NumOfNonzeroColumns = NumOfNonzeroColumns + 1;
                }
            }
            if (NumOfNonzeroColumns != 0.0) {
                AveragePercentageOverColumns[i - 1] = sumOfColumnsGridPercentage[i - 1] / NumOfNonzeroColumns;
            }
        }

        if (func.equals("area") || func.equals("occurrences") || func.equals("species")) {
            m.addAttribute("grid", grid);
            m.addAttribute("sumofcolumns", sumOfColumns);
            m.addAttribute("sumofrows", sumOfRows);
            m.addAttribute("total", Total);
        } else if (func.equals("arearow") || func.equals("occurrencesrow") || func.equals("speciesrow")) {
            m.addAttribute("grid", grid);
            m.addAttribute("sumofcolumns", sumOfColumns);
            m.addAttribute("sumofrows", sumOfRows);
            m.addAttribute("gridpercentage", gridRowPercentage);
            m.addAttribute("sumofrowsgridpercentage", sumOfRowsGridPercentage);
            m.addAttribute("sumofcolumnsgridpercentage", sumOfColumnsGridPercentage);
            m.addAttribute("AveragePercentageOverRows", AveragePercentageOverRows);
        } else if (func.equals("areacolumn") || func.equals("occurrencescolumn") || func.equals("speciescolumn")) {
            m.addAttribute("grid", grid);
            m.addAttribute("sumofcolumns", sumOfColumns);
            m.addAttribute("sumofrows", sumOfRows);
            m.addAttribute("gridpercentage", gridColumnPercentage);
            m.addAttribute("sumofrowsgridpercentage", sumOfRowsGridPercentage);
            m.addAttribute("sumofcolumnsgridpercentage", sumOfColumnsGridPercentage);
            m.addAttribute("AveragePercentageOverColumns", AveragePercentageOverColumns);
        } else if (func.equals("areatotal") || func.equals("occurrencestotal") || func.equals("speciestotal")) {
            m.addAttribute("grid", grid);
            m.addAttribute("sumofcolumns", sumOfColumns);
            m.addAttribute("sumofrows", sumOfRows);
            m.addAttribute("gridpercentage", gridTotalPercentage);
            m.addAttribute("sumofrowsgridpercentage", sumOfRowsGridPercentage);
            m.addAttribute("sumofcolumnsgridpercentage", sumOfColumnsGridPercentage);
            m.addAttribute("totalpercentage", TotalPercentage);
        }

        if (func.equals("area")) {
            m.addAttribute("tabulationDescription", "Area (square kilometres)");
            m.addAttribute("tabulationRowMargin", "Total area");
            m.addAttribute("tabulationColumnMargin", "Total area");
            m.addAttribute("id", 1);
        } else if (func.equals("occurrences")) {
            m.addAttribute("tabulationDescription", "Number of occurrences");
            m.addAttribute("tabulationRowMargin", "Total occurrences");
            m.addAttribute("tabulationColumnMargin", "Total occurrences");
            m.addAttribute("id", 2);
        } else if (func.equals("species")) {
            m.addAttribute("tabulationDescription", "Number of species");
            m.addAttribute("tabulationRowMargin", "Total species (non unique)");
            m.addAttribute("tabulationColumnMargin", "Total species (non unique)");
            m.addAttribute("id", 2);
        } else if (func.equals("arearow")) {
            m.addAttribute("tabulationDescription", "Area: Row%");
            m.addAttribute("tabulationRowMargin", "Total");
            m.addAttribute("tabulationColumnMargin", "Average");
            m.addAttribute("id", 4);
        } else if (func.equals("occurrencesrow")) {
            m.addAttribute("tabulationDescription", "Occurrences: Row%");
            m.addAttribute("tabulationRowMargin", "Total");
            m.addAttribute("tabulationColumnMargin", "Average");
            m.addAttribute("id", 4);
        } else if (func.equals("speciesrow")) {
            m.addAttribute("tabulationDescription", "Species: Row%");
            m.addAttribute("tabulationRowMargin", "Total");
            m.addAttribute("tabulationColumnMargin", "Average");
            m.addAttribute("id", 4);
        } else if (func.equals("areacolumn")) {
            m.addAttribute("tabulationDescription", "Area: Column%");
            m.addAttribute("tabulationRowMargin", "Average");
            m.addAttribute("tabulationColumnMargin", "Total");
            m.addAttribute("id", 3);
        } else if (func.equals("occurrencescolumn")) {
            m.addAttribute("tabulationDescription", "Occurrences: Column%");
            m.addAttribute("tabulationRowMargin", "Average");
            m.addAttribute("tabulationColumnMargin", "Total");
            m.addAttribute("id", 3);
        } else if (func.equals("speciescolumn")) {
            m.addAttribute("tabulationDescription", "Species: Column%");
            m.addAttribute("tabulationRowMargin", "Average");
            m.addAttribute("tabulationColumnMargin", "Total");
            m.addAttribute("id", 3);
        } else if (func.equals("areatotal")) {
            m.addAttribute("tabulationDescription", "Area: Total%");
            m.addAttribute("tabulationRowMargin", "Total");
            m.addAttribute("tabulationColumnMargin", "Total");
            m.addAttribute("id", 5);
        } else if (func.equals("occurrencestotal")) {
            m.addAttribute("tabulationDescription", "Occurrences: Total%");
            m.addAttribute("tabulationRowMargin", "Total");
            m.addAttribute("tabulationColumnMargin", "Total");
            m.addAttribute("id", 5);
        } else if (func.equals("speciestotal")) {
            m.addAttribute("tabulationDescription", "Species: Total%");
            m.addAttribute("tabulationRowMargin", "Total");
            m.addAttribute("tabulationColumnMargin", "Total");
            m.addAttribute("id", 5);

        }
        return new ModelAndView("tabulations/tabulation", m);
    }

    private String[][] tabulationGridGenerator(List<Tabulation> tabulations, String fid1, String fid2, String wkt, String func) throws IOException {
        //determine x & y field names
        TreeMap<String, String> objects1 = new TreeMap<String, String>();
        TreeMap<String, String> objects2 = new TreeMap<String, String>();

        for (Tabulation t : tabulations) {
            objects1.put(t.getPid1(), t.getName1());
            objects2.put(t.getPid2(), t.getName2());
        }

        int rows = Math.max(objects1.size(), objects2.size());
        int columns = Math.min(objects1.size(), objects2.size());

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
                if (func.equals("area") || func.equals("arearow") || func.equals("areacolumn") || func.equals("areatotal")) {
                    grid[order2.get(t.getPid2()) + 1][order1.get(t.getPid1()) + 1] = String.format("%.1f", t.getArea() / 1000000.0); //convert sqm to sqkm
                } else if (func.equals("occurrences") || func.equals("occurrencesrow") || func.equals("occurrencescolumn") || func.equals("occurrencestotal")) {
                    grid[order2.get(t.getPid2()) + 1][order1.get(t.getPid1()) + 1] = String.valueOf(t.getOccurrences());
                } else if (func.equals("species") || func.equals("speciesrow") || func.equals("speciescolumn") || func.equals("speciestotal")) {
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
                if (func.equals("area") || func.equals("arearow") || func.equals("areacolumn") || func.equals("areatotal")) {
                    grid[order1.get(t.getPid1()) + 1][order2.get(t.getPid2()) + 1] = String.format("%.1f", t.getArea() / 1000000.0); //convert sqm to sqkm
                } else if (func.equals("occurrences") || func.equals("occurrencesrow") || func.equals("occurrencescolumn") || func.equals("occurrencestotal")) {
                    grid[order1.get(t.getPid1()) + 1][order2.get(t.getPid2()) + 1] = String.valueOf(t.getOccurrences());
                } else if (func.equals("species") || func.equals("speciesrow") || func.equals("speciescolumn") || func.equals("speciestotal")) {
                    grid[order1.get(t.getPid1()) + 1][order2.get(t.getPid2()) + 1] = String.valueOf(t.getSpecies());
                }
            }
        }
        return grid;
    }

    private double[] tabulationSumOfColumnsGenerator(String[][] grid, String func) throws IOException {

        //define row totals
        double[] sumofcolumns = new double[grid.length - 1];

        for (int k = 1; k < grid.length; k++) {
            //sum of rows
            for (int j = 1; j < grid[0].length; j++) {
                //not row and column headers
                if (grid[k][j] != null) {
                    if (func.equals("area") || func.equals("arearow") || func.equals("areacolumn") || func.equals("areatotal")) {
                        sumofcolumns[k - 1] = sumofcolumns[k - 1] + Double.parseDouble(grid[k][j]);
                    } else if (func.equals("occurrences") || func.equals("occurrencesrow") || func.equals("occurrencescolumn") || func.equals("occurrencestotal") || func.equals("species") || func.equals("speciesrow") || func.equals("speciescolumn") || func.equals("speciestotal")) {
                        sumofcolumns[k - 1] = sumofcolumns[k - 1] + Double.parseDouble(grid[k][j]);
                    }
                }
            }
        }
        return sumofcolumns;
    }

    private double[] tabulationSumOfRowsGenerator(String[][] grid, String func) throws IOException {
        //define column totals
        double[] sumofrows = new double[grid[0].length - 1];
        for (int j = 1; j < grid[0].length; j++) {
            //sum of rows
            for (int k = 1; k < grid.length; k++) {
                //not row and column headers
                if (grid[k][j] != null) {
                    if (func.equals("area") || func.equals("arearow") || func.equals("areacolumn") || func.equals("areatotal")) {
                        sumofrows[j - 1] = sumofrows[j - 1] + Double.parseDouble(grid[k][j]);
                    } else if (func.equals("occurrences") || func.equals("occurrencesrow") || func.equals("occurrencescolumn") || func.equals("occurrencestotal") || func.equals("species") || func.equals("speciesrow") || func.equals("speciescolumn") || func.equals("speciestotal")) {
                        sumofrows[j - 1] = sumofrows[j - 1] + Double.parseDouble(grid[k][j]);
                    }
                }
            }
        }
        return sumofrows;
    }

    private double[][] tabulationGridRowPercentageGenerator(String[][] grid, double[] sumofcolumns, String func) throws IOException {
        double[][] gridRowPercentage = new double[grid.length - 1][grid[0].length - 1];
        if (func.equals("arearow") || func.equals("occurrencesrow") || func.equals("speciesrow")) {
            for (int i = 0; i < grid.length - 1; i++) {
                for (int j = 0; j < grid[0].length - 1; j++) {
                    if (grid[i + 1][j + 1] != null && sumofcolumns[i] != 0.0) {
                        if (func.equals("arearow")) {
                            gridRowPercentage[i][j] = Double.parseDouble(grid[i + 1][j + 1]) / sumofcolumns[i] * 100.00;
                        } else if (func.equals("occurrencesrow") || func.equals("speciesrow")) {
                            gridRowPercentage[i][j] = Double.parseDouble(grid[i + 1][j + 1]) / sumofcolumns[i] * 100.00;
                        }

                    } else {
                        gridRowPercentage[i][j] = 0;
                    }
                }
            }
        }
        return gridRowPercentage;
    }

    private double[][] tabulationGridColumnPercentageGenerator(String[][] grid, double[] sumofrows, String func) throws IOException {
        double[][] gridColumnPercentage = new double[grid.length - 1][grid[0].length - 1];
        if (func.equals("areacolumn") || func.equals("occurrencescolumn") || func.equals("speciescolumn")) {
            for (int i = 0; i < grid.length - 1; i++) {
                for (int j = 0; j < grid[0].length - 1; j++) {
                    if (grid[i + 1][j + 1] != null && sumofrows[j] != 0.0) {
                        if (func.equals("areacolumn")) {
                            gridColumnPercentage[i][j] = Double.parseDouble(grid[i + 1][j + 1]) / sumofrows[j] * 100.00;
                        } else if (func.equals("occurrencescolumn") || func.equals("speciescolumn")) {
                            gridColumnPercentage[i][j] = Double.parseDouble(grid[i + 1][j + 1]) / sumofrows[j] * 100.00;
                        }
                    } else {
                        gridColumnPercentage[i][j] = 0.0;
                    }
                }
            }
        }
        return gridColumnPercentage;
    }

    private double[][] tabulationGridTotalPercentageGenerator(String[][] grid, double[] sumofcolumns, String func) throws IOException {
        double[][] gridTotalPercentage = new double[grid.length - 1][grid[0].length - 1];
        double total = 0.0;
        for (int i = 0; i < grid.length - 1; i++) {
            total += sumofcolumns[i];
        }
        if (func.equals("areatotal") || func.equals("occurrencestotal") || func.equals("speciestotal")) {
            for (int i = 0; i < grid.length - 1; i++) {
                for (int j = 0; j < grid[0].length - 1; j++) {
                    if (grid[i + 1][j + 1] != null && total != 0.0) {
                        if (func.equals("areatotal")) {
                            gridTotalPercentage[i][j] = Double.parseDouble(grid[i + 1][j + 1]) / total * 100.00;
                        } else if (func.equals("occurrencestotal") || func.equals("speciestotal")) {
                            gridTotalPercentage[i][j] = Double.parseDouble(grid[i + 1][j + 1]) / total * 100.00;
                        }
                    } else {
                        gridTotalPercentage[i][j] = 0.0;
                    }
                }
            }
        }
        return gridTotalPercentage;
    }

    private double[] tabulationSumOfRowsGridPercentageGenerator(String[][] grid, double[][] gridRowPercentage, double[][] gridColumnPercentage, double[][] gridTotalPercentage, double[] sumofcolumns, String func) throws IOException {
        double[] sumofrowsGridRowPercentage = new double[grid[0].length - 1];
        double[] sumofrowsGridColumnPercentage = new double[grid[0].length - 1];
        double[] sumofrowsGridTotalPercentage = new double[grid[0].length - 1];
        double[] result = new double[grid[0].length - 1];

        for (int j = 0; j < grid[0].length - 1; j++) {
            //sum of rows
            for (int i = 0; i < grid.length - 1; i++) {
                //not row and column headers
                if (grid[i + 1][j + 1] != null) {
                    if (func.equals("arearow") || func.equals("occurrencesrow") || func.equals("speciesrow")) {
                        sumofrowsGridRowPercentage[j] = sumofrowsGridRowPercentage[j] + gridRowPercentage[i][j];
                    } else if (func.equals("areacolumn") || func.equals("occurrencescolumn") || func.equals("speciescolumn")) {
                        sumofrowsGridColumnPercentage[j] = sumofrowsGridColumnPercentage[j] + gridColumnPercentage[i][j];
                    } else if (func.equals("areatotal") || func.equals("occurrencestotal") || func.equals("speciestotal")) {
                        sumofrowsGridTotalPercentage[j] = sumofrowsGridTotalPercentage[j] + gridTotalPercentage[i][j];
                    }
                }
            }
            //System.out.println("sumofrowsGridRowPercentage["+j+"]:"+sumofrowsGridRowPercentage[j]);
        }
        if (func.equals("arearow") || func.equals("occurrencesrow") || func.equals("speciesrow")) {
            result = sumofrowsGridRowPercentage;
        } else if (func.equals("areacolumn") || func.equals("occurrencescolumn") || func.equals("speciescolumn")) {
            result = sumofrowsGridColumnPercentage;
        } else if (func.equals("areatotal") || func.equals("occurrencestotal") || func.equals("speciestotal")) {
            result = sumofrowsGridTotalPercentage;
        }
        return result;
    }

    //define row totals
    private double[] tabulationSumOfColumnsGridPercentageGenerator(String[][] grid, double[][] gridRowPercentage, double[][] gridColumnPercentage, double[][] gridTotalPercentage, double[] sumofcolumns, String func) throws IOException {
        double[] sumofcolumnsGridRowPercentage = new double[grid.length - 1];
        double[] sumofcolumnsGridColumnPercentage = new double[grid.length - 1];
        double[] sumofcolumnsGridTotalPercentage = new double[grid.length - 1];
        double[] result = new double[grid.length - 1];

        for (int i = 0; i < grid.length - 1; i++) {
            //sum of rows
            for (int j = 0; j < grid[0].length - 1; j++) {
                //not row and column headers
                if (grid[i + 1][j + 1] != null) {
                    if (func.equals("arearow") || func.equals("occurrencesrow") || func.equals("speciesrow")) {
                        sumofcolumnsGridRowPercentage[i] = sumofcolumnsGridRowPercentage[i] + gridRowPercentage[i][j];
                    } else if (func.equals("areacolumn") || func.equals("occurrencescolumn") || func.equals("speciescolumn")) {
                        sumofcolumnsGridColumnPercentage[i] = sumofcolumnsGridColumnPercentage[i] + gridColumnPercentage[i][j];
                    } else if (func.equals("areatotal") || func.equals("occurrencestotal") || func.equals("speciestotal")) {
                        System.out.println("sumofcolumnsGridTotalPercentage[" + i + "]:" + sumofcolumnsGridTotalPercentage[i] + ";gridTotalPercentage:" + gridTotalPercentage[i][j]);
                        sumofcolumnsGridTotalPercentage[i] += gridTotalPercentage[i][j];

                    }
                }
            }
        }
        if (func.equals("arearow") || func.equals("occurrencesrow") || func.equals("speciesrow")) {
            result = sumofcolumnsGridRowPercentage;
        } else if (func.equals("areacolumn") || func.equals("occurrencescolumn") || func.equals("speciescolumn")) {
            result = sumofcolumnsGridColumnPercentage;
        } else if (func.equals("areatotal") || func.equals("occurrencestotal") || func.equals("speciestotal")) {
            result = sumofcolumnsGridTotalPercentage;
        }
        return result;
    }

    /*
     * list distribution table records, GET
     */
    @RequestMapping(value = "/tabulation/{func1}/{func2}/{fid1}/{fid2}/tabulation.{type}", method = {RequestMethod.GET, RequestMethod.POST})
    public void displayTabulationCSVHTML(@PathVariable("func1") String func1, @PathVariable("func2") String func2, @PathVariable("fid1") String fid1, @PathVariable("fid2") String fid2, @PathVariable("type") String type,
                                         @RequestParam(value = "wkt", required = false, defaultValue = "") String wkt,
                                         HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String func = func1 + func2;
        List<Tabulation> tabulations = tabulationDao.getTabulation(fid1, fid2, wkt);
        generateTabulationCSVHTML(tabulations, resp, func, fid1, fid2, wkt, type);
    }

    @RequestMapping(value = "/tabulation/{func1}/{fid1}/{fid2}/tabulation.{type}", method = {RequestMethod.GET, RequestMethod.POST})
    public void displayTabulationCSVHTML(@PathVariable("func1") String func1, @PathVariable("fid1") String fid1, @PathVariable("fid2") String fid2, @PathVariable("type") String type,
                                         @RequestParam(value = "wkt", required = false, defaultValue = "") String wkt,
                                         HttpServletRequest req, HttpServletResponse resp) throws IOException {
        List<Tabulation> tabulations = tabulationDao.getTabulation(fid1, fid2, wkt);
        generateTabulationCSVHTML(tabulations, resp, func1, fid1, fid2, wkt, type);
    }

    public void generateTabulationCSVHTML(List<Tabulation> tabulations, HttpServletResponse resp, String func, String fid1, String fid2,
                                          String wkt, String type) throws IOException {
        String[][] grid = tabulationGridGenerator(tabulations, fid1, fid2, wkt, func);
        double[] sumOfColumns = tabulationSumOfColumnsGenerator(grid, func);
        double[] sumOfRows = tabulationSumOfRowsGenerator(grid, func);
        double[][] gridRowPercentage = tabulationGridRowPercentageGenerator(grid, sumOfColumns, func);
        double[][] gridColumnPercentage = tabulationGridColumnPercentageGenerator(grid, sumOfRows, func);
        double[][] gridTotalPercentage = tabulationGridTotalPercentageGenerator(grid, sumOfColumns, func);
        double[] sumOfRowsGridPercentage = tabulationSumOfRowsGridPercentageGenerator(grid, gridRowPercentage, gridColumnPercentage, gridTotalPercentage, sumOfColumns, func);
        double[] sumOfColumnsGridPercentage = tabulationSumOfColumnsGridPercentageGenerator(grid, gridRowPercentage, gridColumnPercentage, gridTotalPercentage, sumOfColumns, func);

        double Total = 0.0;
        for (int j = 1; j < grid[0].length; j++) {
            Total += sumOfRows[j - 1];
        }

        double TotalPercentage = 0.0;
        for (int j = 1; j < grid[0].length; j++) {
            TotalPercentage += sumOfRowsGridPercentage[j - 1];
        }

        double[] AveragePercentageOverRows = new double[grid[0].length - 1];
        for (int j = 1; j < grid[0].length; j++) {
            double NumOfNonzeroRows = 0.0;
            for (int k = 1; k < grid.length; k++) {
                if (grid[k][j] != null) {
                    NumOfNonzeroRows = NumOfNonzeroRows + 1;
                }
            }
            if (NumOfNonzeroRows != 0.0) {
                AveragePercentageOverRows[j - 1] = sumOfRowsGridPercentage[j - 1] / NumOfNonzeroRows;
            }
        }
        double[] AveragePercentageOverColumns = new double[grid.length - 1];
        for (int i = 1; i < grid.length; i++) {
            double NumOfNonzeroColumns = 0.0;
            for (int k = 1; k < grid[0].length; k++) {
                if (grid[i][k] != null) {
                    NumOfNonzeroColumns = NumOfNonzeroColumns + 1;
                }
            }
            if (NumOfNonzeroColumns != 0.0) {
                AveragePercentageOverColumns[i - 1] = sumOfColumnsGridPercentage[i - 1] / NumOfNonzeroColumns;
            }
        }

        //write to csv or json
        StringBuilder sb = new StringBuilder();
        if (type.equals("csv")) {
            //resp.setContentType("text/plain");
            resp.setContentType("text/comma-separated-values");
            for (int i = 0; i < grid.length; i++) {
                if (i > 0) {
                    sb.append("\n");
                }
                for (int j = 0; j < grid[i].length; j++) {
                    if (i == 0 || j == 0) {
                        if (i == 0 && j == 0) {
                            if (func.equals("area")) {
                                sb.append("\"Area (square kilometres)\"");
                            } else if (func.equals("arearow")) {
                                sb.append("\"Area: Row%\"");
                            } else if (func.equals("areacolumn")) {
                                sb.append("\"Area: Column%\"");
                            } else if (func.equals("areatotal")) {
                                sb.append("\"Area: Total%\"");
                            } else if (func.equals("occurrences")) {
                                sb.append("\"Number of occurrences\"");
                            } else if (func.equals("occurrencesrow")) {
                                sb.append("\"Occurrences: Row%\"");
                            } else if (func.equals("occurrencescolumn")) {
                                sb.append("\"Occurrences: Column%\"");
                            } else if (func.equals("occurrencestotal")) {
                                sb.append("\"Occurrences: Total%\"");
                            } else if (func.equals("species")) {
                                sb.append("\"Number of species (non unique)\"");
                            } else if (func.equals("speciesrow")) {
                                sb.append("\"Species: Row%\"");
                            } else if (func.equals("speciescolumn")) {
                                sb.append("\"Species: Column%\"");
                            } else if (func.equals("speciestotal")) {
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
                            if (func.equals("area") || func.equals("occurrences") || func.equals("species")) {
                                sb.append(grid[i][j]);
                            } else if (func.equals("arearow") || func.equals("occurrencesrow") || func.equals("speciesrow")) {
                                //sb.append(gridRowPercentage[i-1][j-1]/100.00);
                                sb.append(gridRowPercentage[i - 1][j - 1] + "%");
                            } else if (func.equals("areacolumn") || func.equals("occurrencescolumn") || func.equals("speciescolumn")) {
                                //sb.append(gridColumnPercentage[i-1][j-1]/100.00);
                                sb.append(gridColumnPercentage[i - 1][j - 1] + "%");
                            } else if (func.equals("areatotal") || func.equals("occurrencestotal") || func.equals("speciestotal")) {
                                //sb.append(gridTotalPercentage[i-1][j-1]/100.00);
                                sb.append(gridTotalPercentage[i - 1][j - 1] + "%");
                            }
                        }
                    }
                }
                if (i == 0) {
                    if (func.equals("area")) {
                        sb.append(",\"Total area\"");
                    } else if (func.equals("occurrences")) {
                        sb.append(",\"Total occurrences\"");
                    } else if (func.equals("species")) {
                        sb.append(",\"Total species (non unique)\"");
                    } else if (func.equals("arearow") || func.equals("occurrencesrow") || func.equals("speciesrow") || func.equals("areatotal") || func.equals("occurrencestotal") || func.equals("speciestotal")) {
                        sb.append(",\"Total\"");
                    } else if (func.equals("areacolumn") || func.equals("occurrencescolumn") || func.equals("speciescolumn")) {
                        sb.append(",\"Average\"");
                    } else if (func.equals("areatotal") || func.equals("occurrencestotal") || func.equals("speciestotal")) {
                        sb.append(",\"Total\"");
                    }
                } else {
                    if (func.equals("area")) {
                        sb.append("," + sumOfColumns[i - 1]);
                    } else if (func.equals("occurrences") || func.equals("species")) {
                        sb.append("," + sumOfColumns[i - 1]);
                    } else if (func.equals("arearow") || func.equals("occurrencesrow") || func.equals("speciesrow")) {
                        //sb.append(","+ sumOfColumnsGridPercentage[i - 1]/100.00);
                        sb.append("," + sumOfColumnsGridPercentage[i - 1] + "%");
                    } else if (func.equals("areacolumn") || func.equals("occurrencescolumn") || func.equals("speciescolumn")) {
                        int numOfNonzeroClasses = 0;
                        for (int k = 1; k < grid[0].length; k++) {
                            if (grid[i][k] != null) {
                                numOfNonzeroClasses = numOfNonzeroClasses + 1;
                            }
                        }
                        //sb.append(","+ AveragePercentageOverColumns[i - 1]/100.00);
                        sb.append("," + AveragePercentageOverColumns[i - 1] + "%");
                    } else if (func.equals("areatotal") || func.equals("occurrencestotal") || func.equals("speciestotal")) {
                        //sb.append(","+ sumOfColumnsGridPercentage[i - 1]/100.00);
                        sb.append("," + sumOfColumnsGridPercentage[i - 1] + "%");
                    }
                }
            }
            sb.append("\n");
            if (func.equals("area")) {
                sb.append("\"Total area\"");
            } else if (func.equals("occurrences")) {
                sb.append("\"Total occurrences\"");
            } else if (func.equals("species")) {
                sb.append("\"Total species (non-unique)\"");
            } else if (func.equals("arearow") || func.equals("occurrencesrow") || func.equals("speciesrow") || func.equals("areatotal") || func.equals("occurrencestotal") || func.equals("speciestotal")) {
                sb.append("\"Average\"");
            } else if (func.equals("areacolumn") || func.equals("occurrencescolumn") || func.equals("speciescolumn")) {
                sb.append("\"Total\"");
            } else if (func.equals("areatotal") || func.equals("occurrencestotal") || func.equals("speciestotal")) {
                sb.append("\"Total\"");
            }
            for (int j = 1; j < grid[0].length; j++) {
                if (func.equals("area")) {
                    sb.append("," + sumOfRows[j - 1]);
                } else if (func.equals("occurrences") || func.equals("species")) {
                    sb.append("," + sumOfRows[j - 1]);
                } else if (func.equals("arearow") || func.equals("occurrencesrow") || func.equals("speciesrow")) {
                    //sb.append(","+ AveragePercentageOverRows[j - 1]/100.00);
                    sb.append("," + AveragePercentageOverRows[j - 1] + "%");
                } else if (func.equals("areacolumn") || func.equals("occurrencescolumn") || func.equals("speciescolumn")) {
                    //sb.append(","+ sumOfRowsGridPercentage[j - 1]/100.00);
                    sb.append("," + sumOfRowsGridPercentage[j - 1] + "%");
                } else if (func.equals("areatotal") || func.equals("occurrencestotal") || func.equals("speciestotal")) {
                    //sb.append(","+ sumOfRowsGridPercentage[j - 1]/100.00);
                    sb.append("," + sumOfRowsGridPercentage[j - 1] + "%");
                }
            }
            if (func.equals("areatotal") || func.equals("occurrencestotal") || func.equals("speciestotal")) {

                //sb.append(","+ TotalPercentage/100.00);
                sb.append("," + TotalPercentage + "%");
            } else if (func.equals("area")) {
                sb.append("," + Total);
            } else if (func.equals("occurrences") || func.equals("species")) {
                sb.append("," + Total);
            } else {
                sb.append(",");
            }
            sb.append("\n\n");
            sb.append("Blanks = no intersection\n");
            sb.append("0 = no records in intersection\n");
        } else if (type.equals("json")) {
            resp.setContentType("application/json");
            sb.append("{");
            for (int i = 1; i < grid.length; i++) {
                sb.append("\"").append(grid[i][0].replace("\"", "\"\"")).append("\":");
                sb.append("{");
                for (int j = 1; j < grid[i].length; j++) {
                    if (grid[i][j] != null) {
                        sb.append("\"").append(grid[0][j].replace("\"", "\"\"")).append("\":");
                        //sb.append(grid[i][j]);
                        if (func.equals("area") || func.equals("occurrences") || func.equals("species")) {
                            sb.append(grid[i][j]);
                        } else if (func.equals("arearow") || func.equals("occurrencesrow") || func.equals("speciesrow")) {
                            sb.append(gridRowPercentage[i - 1][j - 1] / 100.00);
                        } else if (func.equals("areacolumn") || func.equals("occurrencescolumn") || func.equals("speciescolumn")) {
                            sb.append(gridColumnPercentage[i - 1][j - 1] / 100.00);
                        } else if (func.equals("areatotal") || func.equals("occurrencestotal") || func.equals("speciestotal")) {
                            sb.append(gridTotalPercentage[i - 1][j - 1] / 100.00);
                        }
                        sb.append(",");
                    }
                }
                if (func.equals("area")) {
                    sb.append("\"Total area\":" + sumOfColumns[i - 1]);
                } else if (func.equals("occurrences")) {
                    sb.append("\"Total occurrences\":" + sumOfColumns[i - 1]);
                } else if (func.equals("species")) {
                    sb.append("\"Total species (non unique)\":" + sumOfColumns[i - 1]);
                } else if (func.equals("arearow") || func.equals("occurrencesrow") || func.equals("speciesrow")) {
                    sb.append("\"Total\":" + sumOfColumnsGridPercentage[i - 1] / 100);
                } else if (func.equals("areacolumn") || func.equals("occurrencescolumn") || func.equals("speciescolumn")) {
                    sb.append("\"Average\":" + AveragePercentageOverColumns[i - 1] / 100);
                } else if (func.equals("areatotal") || func.equals("occurrencestotal") || func.equals("speciestotal")) {
                    sb.append("\"Total\":" + sumOfColumnsGridPercentage[i - 1] / 100);
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

            if (func.equals("area")) {
                sb.append(",\"Total area\":");
            } else if (func.equals("occurrences")) {
                sb.append(",\"Total occurrences\":");
            } else if (func.equals("species")) {
                sb.append(",\"Total species (non-unique)\":");
            } else if (func.equals("arearow") || func.equals("occurrencesrow") || func.equals("speciesrow")) {
                sb.append(",\"Average\":");
            } else if (func.equals("areacolumn") || func.equals("occurrencescolumn") || func.equals("speciescolumn")) {
                sb.append(",\"Total\":");
            } else if (func.equals("areatotal") || func.equals("occurrencestotal") || func.equals("speciestotal")) {
                sb.append(",\"Total\":");
            }

            sb.append("{");
            for (int j = 1; j < grid[0].length; j++) {
                sb.append("\"").append(grid[0][j].replace("\"", "\"\"")).append("\":");
                if (func.equals("area")) {
                    sb.append(sumOfRows[j - 1] + ",");
                } else if (func.equals("occurrences") || func.equals("species")) {
                    sb.append(sumOfRows[j - 1] + ",");
                } else if (func.equals("arearow") || func.equals("occurrencesrow") || func.equals("speciesrow")) {
                    sb.append(AveragePercentageOverRows[j - 1] / 100 + ",");
                } else if (func.equals("areacolumn") || func.equals("occurrencescolumn") || func.equals("speciescolumn")) {
                    sb.append(sumOfRowsGridPercentage[j - 1] / 100 + ",");
                } else if (func.equals("areatotal") || func.equals("occurrencestotal") || func.equals("speciestotal")) {
                    sb.append(sumOfRowsGridPercentage[j - 1] / 100 + ",");
                }
            }
            if (func.equals("areatotal") || func.equals("occurrencestotal") || func.equals("speciestotal")) {
                sb.append("\"%\":" + TotalPercentage / 100 + ",");
            } else if (func.equals("area") || func.equals("occurrences") || func.equals("species")) {
                if (func.equals("area")) {
                    sb.append("\"Total area\":" + Total + ",");
                } else if (func.equals("occurrences")) {
                    sb.append("\"Total occurrences\":" + Total + ",");
                } else if (func.equals("species")) {
                    sb.append("\"Total species (non unique)\":" + Total + ",");
                }
            }
            if (sb.toString().endsWith(",")) {
                sb.deleteCharAt(sb.toString().length() - 1);
            }
            sb.append("\n");

            sb.append("}");
            sb.append("}");
        }
        OutputStream os = resp.getOutputStream();
        os.write(sb.toString().getBytes("UTF-8"));
        os.close();
    }

    /*
     * list distribution table records, GET
     */
    @RequestMapping(value = "/tabulation/{func1}/{func2}/{fid1}/tabulation.html", method = {RequestMethod.GET, RequestMethod.POST})
    public ModelAndView displayTabulationSingle(@PathVariable("func1") String func1, @PathVariable("func2") String func2, @PathVariable("fid1") String fid1,
                                                @RequestParam(value = "wkt", required = false, defaultValue = "") String wkt,
                                                HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String func = func1 + func2;
        List<Tabulation> tabulations = tabulationDao.getTabulationSingle(fid1, wkt);
        return generateTabulation(tabulations, func, fid1, null, wkt);
    }

    @RequestMapping(value = "/tabulation/{func1}/{fid1}/tabulation.html", method = {RequestMethod.GET, RequestMethod.POST})
    public ModelAndView displayTabulationSingle(@PathVariable("func1") String func1, @PathVariable("fid1") String fid1,
                                                @RequestParam(value = "wkt", required = false, defaultValue = "") String wkt,
                                                HttpServletRequest req, HttpServletResponse resp) throws IOException {
        List<Tabulation> tabulations = tabulationDao.getTabulationSingle(fid1, wkt);
        return generateTabulation(tabulations, func1, fid1, null, wkt);
    }
    /*
     * list distribution table records, GET
     */

    @RequestMapping(value = "/tabulation/{func1}/{func2}/{fid1}/tabulation.{type}", method = {RequestMethod.GET, RequestMethod.POST})
    public void displayTabulationSingleCSVHTML(@PathVariable("func1") String func1, @PathVariable("func2") String func2, @PathVariable("fid1") String fid1, @PathVariable("type") String type,
                                               @RequestParam(value = "wkt", required = false, defaultValue = "") String wkt,
                                               HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String func = func1 + func2;
        List<Tabulation> tabulations = tabulationDao.getTabulationSingle(fid1, wkt);
        generateTabulationCSVHTML(tabulations, resp, func, fid1, null, wkt, type);
    }

    @RequestMapping(value = "/tabulation/{func1}/{fid1}/tabulation.{type}", method = {RequestMethod.GET, RequestMethod.POST})
    public void displayTabulationSingleCSVHTML(@PathVariable("func1") String func1, @PathVariable("fid1") String fid1, @PathVariable("type") String type,
                                               @RequestParam(value = "wkt", required = false, defaultValue = "") String wkt,
                                               HttpServletRequest req, HttpServletResponse resp) throws IOException {
        List<Tabulation> tabulations = tabulationDao.getTabulationSingle(fid1, wkt);
        generateTabulationCSVHTML(tabulations, resp, func1, fid1, null, wkt, type);
    }
}
