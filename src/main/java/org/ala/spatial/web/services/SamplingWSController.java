package org.ala.spatial.web.services;

import java.net.URLDecoder;
import javax.servlet.http.HttpServletRequest;
import org.ala.layers.intersect.Grid;
import org.ala.layers.util.SpatialUtil;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 *
 * @author ajay
 */
@Controller
@RequestMapping("/ws/sampling/")
public class SamplingWSController {

    final static double RESOLUTION = 0.01;

    /**
     * chart basis: occurrence or area
     * filter: [lsid] + [area]
     * series: [taxon level] or [string attribute] or [boolean attribute] or [shape file layer]
     * xaxis: [taxon level] or [attribute] or [layer]
     *        Optional ',' + min + ' ' max
     *        Optional ',' + categories delimited by '\t'
     * yaxis: [taxon level] or [attribute] or [layer] or [count type=countspecies, countoccurrence, size of area]
     *        Optional ',' + min + ' ' max
     *        Optional ',' + categories delimited by '\t'
     * zaxis: [count type=species, occurrence, size of area]
     *
     * Types of chart basis:
     * bA. Occurrence locations.
     * bB. Layer intersections.
     *
     * Types of variables:
     * tA. catagorical (e.g. STRING ATTRIBUTES[bA], SHAPE FILE INTERSECTION, TAXON NAMES[bA])
     * tB. continous (e.g. INT or DOUBLE ATTRIBUTES[bA] and GRID FILE INTERSECTION)
     * tC. presence (e.g. BOOLEAN ATTRIBUTES[bA], BOOLEAN=TRUE COUNT[bA], SPECIES COUNT[bA] and OCCURRENCE COUNT[bA], INTERSECTION AREA SIZE[bB])
     *
     * Valid variables for Filtering:
     * fZ. none.
     * fA. catagorical (e.g. STRING ATTRIBUTES[bA], SHAPE FILE INTERSECTION, TAXON NAMES[bA])
     * fB. continous (e.g. INT or DOUBLE ATTRIBUTES[bA] and GRID FILE INTERSECTION)
     * fC. presence (e.g. BOOLEAN ATTRIBUTES[bA])
     * fD. Active Area
     *
     * Valid variables for Series:
     * sZ. none
     * sA. catagorical (e.g. STRING ATTRIBUTES[bA], SHAPE FILE INTERSECTION, TAXON NAMES[bA])
     * sB. presence (e.g. BOOLEAN ATTRIBUTES[bA])
     *
     * Valid variables for Y-Axis:
     * yA. catagorical (e.g. STRING ATTRIBUTES[bA], SHAPE FILE INTERSECTION, TAXON NAMES[bA])
     * yB. continous (e.g. INT or DOUBLE ATTRIBUTES[bA] and GRID FILE INTERSECTION)
     * yC. presence (e.g. BOOLEAN=TRUE COUNT[bA], SPECIES COUNT[bA] and OCCURRENCE COUNT[bA], INTERSECTION AREA SIZE[bB])
     *
     * Valid variables for X-Axis:
     * tA. catagorical (e.g. STRING ATTRIBUTES[bA], SHAPE FILE INTERSECTION, TAXON NAMES[bA])
     * tB. continous (e.g. INT or DOUBLE ATTRIBUTES[bA] and GRID FILE INTERSECTION)
     *
     * Valid variables for Z-Axis:
     * zZ. none.
     * zA. presence (e.g. BOOLEAN=TRUE COUNT[bA], SPECIES COUNT[bA] and OCCURRENCE COUNT[bA], INTERSECTION AREA SIZE[bB])
     *
     *
     * Axis Combinations
     *
     * table for bA
     * yA			yB					yC
     * tA	(1) XYBlockChart	Box & Whisker				Histogram
     * tB	(2)			Scatterplot or (1) XYBlockChart		Histogram
     *
     * table for bB
     * yA			yB					yC
     * tA	(1) XYBlockChart	Box & Whisker				(2)
     * tB	(2)			(1) XYBlockChart			(2)
     *
     * (1) requires Z-Axis variable.
     * (2) not allowed
     *
     * 
     *
     * @param req
     * @return
     */
    @RequestMapping(value = "/chart", method = RequestMethod.GET)
    public
    @ResponseBody
    String chart(HttpServletRequest req) {
        try {
            String filter = URLDecoder.decode(req.getParameter("wkt"), "UTF-8");
            String xaxis = URLDecoder.decode(req.getParameter("xaxis"), "UTF-8");
            String yaxis = URLDecoder.decode(req.getParameter("yaxis"), "UTF-8");
            String sDivisions = URLDecoder.decode(req.getParameter("divisions"), "UTF-8");
            int divisions = 20;
            if(sDivisions != null) {
                divisions = Integer.parseInt(sDivisions);
            }

//            SimpleRegion region = null;
//            if (filterArea != null && wkt.startsWith("ENVELOPE")) {
//                records = FilteringService.getRecords(filterArea);
//            } else {
//                region = SimpleShapeFile.parseWKT(filterArea);
//            }
            
                //bB

                //TODO: more than env layers
                String[] layers = new String[2];
                double[][] extents = new double[2][2];
                String [] s = xaxis.split(",");
                layers[0] = s[0];
                extents[0][0] = Double.parseDouble(s[1]);
                extents[0][1] = Double.parseDouble(s[2]);
                s = yaxis.split(",");
                layers[1] = s[0];
                extents[1][0] = Double.parseDouble(s[1]);
                extents[1][1] = Double.parseDouble(s[2]);

                float[][] cutoffs = new float[2][divisions];

                //get linear cutoffs
                for (int i = 0; i < layers.length; i++) {
                    for(int j=0;j<divisions;j++){
                        cutoffs[i][j] = (float)(extents[i][0] + (extents[i][1] - extents[i][0]) * ((j + 1) / (float) divisions));
                    }
                    cutoffs[i][divisions - 1] = (float)extents[i][1];  //set max
                }

                //build
                float [][] data = new float[2][];
                Grid g = null;

                //TODO: get cut data

                int len = data[0].length;
                int divs = 20;  //same as number of cutpoints in Legend
                double[][] area = new double[divs][divs];

                for (int i = 0; i < len; i++) {
                    if (Float.isNaN(data[0][i]) || Float.isNaN(data[1][i])) {
                        continue;
                    }
                    int x = getPos(data[0][i], cutoffs[0]);
                    int y = getPos(data[1][i], cutoffs[1]);

                    area[x][y] += cellArea(RESOLUTION, (i / g.ncols) * g.yres);
                }

                //to csv
                StringBuilder sb = new StringBuilder();
                sb.append(",").append(layers[1]).append("\n").append(layers[0]);
                for (int j = 0; j < divs; j++) {
                    sb.append(",").append(cutoffs[1][j]);
                }
                for (int i = 0; i < divs; i++) {
                    sb.append("\n").append(cutoffs[0][i]);
                    for (int j = 0; j < divs; j++) {
                        sb.append(",").append(area[i][j]);
                    }
                }

                return sb.toString();
            
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    int getPos(float d, float[] cutoffs) {
        int pos = java.util.Arrays.binarySearch(cutoffs, d);
        if (pos < 0) {
            pos = (pos * -1) - 1;
        }

        return pos;
    }
    static double[] commonGridLatitudeArea = null;

    static double cellArea(double resolution, double latitude) {
        double minx = 0;
        double maxx = resolution;
        double miny = Math.floor(latitude / resolution) * resolution;
        double maxy = miny + resolution;

        return SpatialUtil.calculateArea(new double [][] {{minx, miny},{minx, maxy},{maxx, maxy}, {maxx, miny}, {minx, miny}});
    }

    /**
     * get values from string of the form:
     * 
     * name1:value2;name2;value2
     * 
     * ';' is not a valid value character and any recognised ';'+name+':'
     * must be absent from values
     * 
     * @param get   name to look for as String
     * @param list  String with name and value pairs
     * @return value as String for the name, or null
     */
    private String getParam(String get, String list) {
        //get name pos
        int p1 = 0;

        while (p1 >= 0 && p1 < list.length()) {
            p1 = list.indexOf(get + ":", p1);
            //failed to find it if -1
            if (p1 < 0) {
                return null;
            } else if (p1 == 0 || list.charAt(p1 - 1) == ';') {
                //found it, extract value
                int start = p1 + get.length() + 1;
                int end = list.indexOf(';', start);
                if (end < 0) {
                    end = list.length();
                }
                if (start >= 0 && start < list.length()
                        && end >= start && end <= list.length()) {
                    return list.substring(start, end);
                }
            } else {
                //contine after moving p1 forward
                p1 += get.length() + 1;
            }
        }

        return null;
    }
}
