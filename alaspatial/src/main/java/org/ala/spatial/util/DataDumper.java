package org.ala.spatial.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Map;
import org.ala.spatial.analysis.index.OccurrencesCollection;
import org.ala.spatial.analysis.index.OccurrencesFilter;
import org.ala.spatial.analysis.index.OccurrencesSpeciesList;
import org.ala.spatial.analysis.service.ShapeIntersectionService;

/**
 * Backend utility to generate data dumps for special request
 * to bypass any limits
 *
 * -a specieslist generates a species list
 * 
 * -shp shapefile get the shape from the shapefile
 * -wkt wkt use the wkt as the shape
 *
 * -o outputfile output file, defaults to screen
 *
 * @author ajay
 */
public class DataDumper {

    private static String[] cmdargs;

    public static String generateSpeciesList(String[] args) {
        cmdargs = args;
        return generateSpeciesList();
    }

    /**
     * From org.ala.spatial.analysis.service.FilteringService.getSpeciesList 
     * 
     * @return
     */
    public static String generateSpeciesList() {
        String area = getArgsValue("-wkt");

        if (!area.equals("")) {
            area = getShapefileArea();
            if (area.equals("")) {
                System.out.println("Please provide either a valid Shapefile or WKT");
                return usage();
            }
        }

        generateSpeciesList(area, getArgsValue("-o")); 

        return "";

    }

    public static String generateSpeciesList(File shapefile, String outfile) {
        Map shape = ShapefileUtils.loadShapefile(shapefile);
        if (shape.containsKey("wkt")) {
            System.out.println("got wkt: " + (String)shape.get("wkt"));
            return generateSpeciesList((String) shape.get("wkt"), outfile);
        } else {
            return ""; 
        }
        
    }

    public static String generateSpeciesList(String area, String outfile) {
        System.out.println("parsing area...");
        SimpleRegion region = SimpleShapeFile.parseWKT(area);
        System.out.println("applying filter...");
        OccurrencesFilter occurrencesFilter = new OccurrencesFilter(region, 9000000);

        String[] output = null;
        System.out.print("getting species list...");
        ArrayList<OccurrencesSpeciesList> osl = OccurrencesCollection.getSpeciesList(occurrencesFilter);
        System.out.println(" done. ");
        if (osl != null && osl.size() > 0) {
            System.out.println("got " + osl.size() + " records");
            StringBuffer sb = new StringBuffer();
            for (String s : osl.get(0).getSpeciesList()) {
                sb.append(s).append("|");
            }

            //any species distributions?
            if (region != null) {
                int[] r = ShapeIntersectionService.getIntersections(region);
                if (r != null) {
                    String[] lsids = ShapeIntersectionService.convertToLsids(r);
                    String str = sb.toString();
                    for (int i = 0; i < lsids.length; i++) {
                        if (!str.contains(lsids[i])) {
                            //append the missing entry
                            System.out.println("getting species list from distribution...");
                            sb.append(OccurrencesSpeciesList.getSpeciesListEntryFromADistribution(lsids[i])).append("|");
                        }
                    }
                }
            }
            System.out.println("done.");
            System.out.println("processing species list");
            output = sb.toString().split("\\|");
            java.util.Arrays.sort(output);
        }

        System.out.println("prettifying...");
        StringBuffer sb = new StringBuffer();
        sb.append("Family Name,Scientific Name,Common name/s,Taxon rank,Scientific Name LSID,Number of Occurrences\r\n");
        for (String s : output) {
            sb.append("\"");
            sb.append(s.replaceAll("\\*", "\",\""));
            sb.append("\"");
            sb.append("\r\n");
        }

        // write to file if available
        if (outfile != null && !outfile.equals("")) {
            System.out.println("writing to " + outfile + " ...");
            try {
                PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(outfile)));
                out.write(sb.toString());
                out.close();
            } catch (Exception e) {
                System.out.println("Unable to write to file");
                e.printStackTrace(System.out);
            }
        }

        System.out.println("completed");

        return "";
    }

    private static String getShapefileArea() {
        String shp = getArgsValue("-shp");

        if (!shp.equals("")) {
            Map shape = ShapefileUtils.loadShapefile(new File(shp));
            if (shape.containsKey("wkt")) {
                return (String) shape.get("wkt");
            }
        }




        return "";
    }

    private static String getArgsValue(String key) {
        for (int i = 0; i < cmdargs.length; i++) {
            if (cmdargs[i].trim().toLowerCase().equals(key)) {
                if ((i + 1) < cmdargs.length - 1) {
                    return cmdargs[i + 1].trim();
                } else {
                    return usage();
                }
            }
        }

        return usage();
    }

    private static String usage() {
        System.out.println("Usage: ");
        System.out.println("\tDataDumper -a specieslist -shp /data/shapefiles/region.shp");
        System.out.println("\tDataDumper -a specieslist -wkt \"POLYGON((128.153 -29.349,128.153 -28.172,130.971 -28.172,130.971 -29.349,128.153 -29.349))\"");

        return "";
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            usage();
            return;
        }

        cmdargs = args;

        if (getArgsValue("-a").toLowerCase().equals("specieslist")) {
            generateSpeciesList();
        } else {
            usage();
        }


//        for (int a=0; a<args.length; a++) {
//            String v = args[a];
//            if (v.trim().equals("-a")) {
//                if ((a+1) < args.length-1) {
//                    if ("specieslist".equals(args[a+1].trim().toLowerCase())) {
//                        generateSpeciesList();
//                    }
//                } else {
//                    usage();
//                }
//            }
//        }

    }
}
