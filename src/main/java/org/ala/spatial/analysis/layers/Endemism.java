package org.ala.spatial.analysis.layers;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.codec.net.URLCodec;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.math3.util.Pair;

public class Endemism {

    private Map<String, Integer> _speciesCellCounts;
    private Map<Pair<BigDecimal, BigDecimal>, List<String>> _cellSpecies;

    public static void main(String[] args) throws Exception {
        File speciesCellCountFile = new File(args[0]);
        File cellSpeciesFile = new File(args[2]);
        File gridOutputFile = new File(args[3]);
        
        new Endemism(speciesCellCountFile, cellSpeciesFile).writeGrid(gridOutputFile);
    }

    public Endemism(File speciesCellCountFile, File cellSpeciesFile) throws IOException {
        _speciesCellCounts = new HashMap<String, Integer>();
        _cellSpecies = new HashMap<Pair<BigDecimal, BigDecimal>, List<String>>();
        
        List<String> speciesCellCountLines = FileUtils.readLines(speciesCellCountFile); 
        for (String line: speciesCellCountLines) {
            String[] tokens = line.split(",");
            String speciesLsid = tokens[0];
            int cellCount = Integer.parseInt(tokens[1]);
            _speciesCellCounts.put(speciesLsid, cellCount);
        }
        
        List<String> cellSpeciesLines = FileUtils.readLines(cellSpeciesFile);
        for (String line: speciesCellCountLines) {
            String[] tokens = line.split(",");
            BigDecimal latitude = new BigDecimal(tokens[0]);
            BigDecimal longitude = new BigDecimal(tokens[1]);
            List<String> speciesLsids = Arrays.asList(Arrays.copyOfRange(tokens, 2, tokens.length));
            _cellSpecies.put(new Pair(latitude, longitude), speciesLsids);
        }
    }
    
    

    public void writeGrid(File gridOutputFile) {

        try {
            FileWriter fw = new FileWriter(gridOutputFile);
            PrintWriter pw = new PrintWriter(fw);

            pw.println("ncols 36000");
            pw.println("nrows 18000");
            pw.println("xllcorner -180.0");
            pw.println("yllcorner -90.0");
            pw.println("cellsize 0.01");
            pw.println("NODATA_value -9999");

            BigDecimal gridSize = new BigDecimal("0.01");
            BigDecimal maxLatitude = new BigDecimal("90.00");
            BigDecimal minLatitude = new BigDecimal("-90.00");
            BigDecimal minLongitude = new BigDecimal("-180.00");
            BigDecimal maxLongitude = new BigDecimal("180.00");
            
            for (BigDecimal lat = maxLatitude; lat.compareTo(minLatitude) == 1; lat = lat.subtract(gridSize)) {
                // a row for each 0.01 latitude
                System.out.println(lat.doubleValue());
                for (BigDecimal lon = minLongitude; lon.compareTo(maxLongitude) == -1; lon = lon.add(gridSize)) {
                    // a column for each 0.01 longitude
                    // System.out.println(lon.doubleValue());

                    Pair<BigDecimal, BigDecimal> coordPair = new Pair<BigDecimal, BigDecimal>(lat, lon);

                    if (_cellSpecies.containsKey(coordPair)) {
                        double endemicityValue = 0; 
                        List<String> speciesLsids = _cellSpecies.get(coordPair);
                        for (String lsid : speciesLsids) {
                            int speciesCellCount = _speciesCellCounts.get(lsid);
                            endemicityValue += 1.0 / speciesCellCount;
                        }
                        pw.print(endemicityValue);
                    } else {
                        pw.print("-9999"); // No data value = -9999
                    }
                    
                    if (lon.compareTo(maxLongitude) != 0) {
                        pw.print(" ");
                    }
                }
                pw.println();
            }
            
            pw.flush();
            pw.close();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}
