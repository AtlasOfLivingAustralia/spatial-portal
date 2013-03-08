package org.ala.spatial.analysis.layers;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.math3.util.Pair;

/**
 * Generates an endemism layer using data generated from the biocache-store. The
 * layer is generated for the entire globe (latitude -90 to 90, longitude -180
 * to 180)
 * 
 * Layer data is output in both ESRI ASCII grid format (.asc) and in DIVA grid
 * format (.gri/.grd). -9999 is used as the no data value.
 * 
 * @author ChrisF
 * 
 */
public class Endemism {

    private Map<String, Integer> _speciesCellCounts;
    private Map<Pair<BigDecimal, BigDecimal>, List<String>> _cellSpecies;
    private BigDecimal _resolution;

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("args[0]=Resolution in degrees, e.g. 0.1 for 0.1 by 0.1 degree cells\n"
                    + "args[1]=Path to species cell count file (should be generated by biocache store via jenkins)\n"
                    + "args[2]=Path to cell species list file (should be generated by biocache store via jenkins)\n"
                    + "args[3]=Path of directory in which to write output files\n"
                    + "args[4]=Prefix to use for names of output files.\n");

            return;
        }

        BigDecimal resolution = new BigDecimal(args[0]).setScale(2);
        File speciesCellCountFile = new File(args[1]);
        File cellSpeciesFile = new File(args[2]);
        File outputFileDirectory = new File(args[3]);
        String outputFileNamePrefix = args[4];

        new Endemism(resolution, speciesCellCountFile, cellSpeciesFile).writeGrid(outputFileDirectory, outputFileNamePrefix);
    }

    public Endemism(BigDecimal resolution, File speciesCellCountFile, File cellSpeciesFile) throws IOException {
        _resolution = resolution;

        _speciesCellCounts = new HashMap<String, Integer>();
        _cellSpecies = new HashMap<Pair<BigDecimal, BigDecimal>, List<String>>();

        // read species cell counts into memory
        List<String> speciesCellCountLines = FileUtils.readLines(speciesCellCountFile);
        for (String line : speciesCellCountLines) {
            String[] tokens = line.split(",");
            String speciesLsid = tokens[0];
            int cellCount = Integer.parseInt(tokens[1]);
            _speciesCellCounts.put(speciesLsid, cellCount);
        }

        // Read cell species lists into memory
        List<String> cellSpeciesLines = FileUtils.readLines(cellSpeciesFile);
        for (String line : cellSpeciesLines) {
            String[] tokens = line.split(",");
            BigDecimal latitude = new BigDecimal(tokens[0]).setScale(2);
            BigDecimal longitude = new BigDecimal(tokens[1]).setScale(2);
            List<String> speciesLsids = Arrays.asList(Arrays.copyOfRange(tokens, 2, tokens.length));
            _cellSpecies.put(new Pair(latitude, longitude), speciesLsids);
        }
    }

    private int calculateNumberOfRows() {
        return (int) (180 / _resolution.floatValue());
    }

    private int calculateNumberOfColumns() {
        return (int) (360 / _resolution.floatValue());
    }

    public void writeGrid(File outputFileDirectory, String outputFileNamePrefix) {

        try {
            // .asc output (ESRI ASCII grid)
            FileWriter ascFileWriter = new FileWriter(new File(outputFileDirectory, outputFileNamePrefix + ".asc"));
            PrintWriter ascPrintWriter = new PrintWriter(ascFileWriter);

            // DIVA output
            BufferedOutputStream divaOutputStream = new BufferedOutputStream(new FileOutputStream(new File(outputFileDirectory, outputFileNamePrefix + ".gri")));

            int numRows = calculateNumberOfRows();
            int numColumns = calculateNumberOfColumns();

            // Write header for .asc output
            ascPrintWriter.println("ncols " + Integer.toString(numColumns));
            ascPrintWriter.println("nrows " + Integer.toString(numRows));
            ascPrintWriter.println("xllcorner -180.0");
            ascPrintWriter.println("yllcorner -90.0");
            ascPrintWriter.println("cellsize " + _resolution.toString());
            ascPrintWriter.println("NODATA_value -9999");

            // Generate layer for the entire globe.
            BigDecimal maxLatitude = new BigDecimal("90.00");
            BigDecimal minLatitude = new BigDecimal("-90.00");
            BigDecimal minLongitude = new BigDecimal("-180.00");
            BigDecimal maxLongitude = new BigDecimal("180.00");

            float maxValue = Float.NEGATIVE_INFINITY;

            for (BigDecimal lat = maxLatitude; lat.compareTo(minLatitude) == 1; lat = lat.subtract(_resolution)) {
                // a row for each _resolution unit of latitude
                // System.out.println(lat.doubleValue());
                for (BigDecimal lon = minLongitude; lon.compareTo(maxLongitude) == -1; lon = lon.add(_resolution)) {
                    // a column for each _resolution unit of longitude
                    Pair<BigDecimal, BigDecimal> coordPair = new Pair<BigDecimal, BigDecimal>(lat, lon);

                    if (_cellSpecies.containsKey(coordPair)) {
                        // Calculate endemism value for the cell. Sum (1 / total
                        // species cell count) for each species that occurs in
                        // the cell. Then divide by the number of species that
                        // occur in the cell.

                        float endemicityValue = 0;
                        List<String> speciesLsids = _cellSpecies.get(coordPair);
                        for (String lsid : speciesLsids) {
                            int speciesCellCount = _speciesCellCounts.get(lsid);
                            endemicityValue += 1.0 / speciesCellCount;
                        }
                        endemicityValue = endemicityValue / speciesLsids.size();

                        if (maxValue < endemicityValue) {
                            maxValue = endemicityValue;
                        }

                        ascPrintWriter.print(endemicityValue);

                        ByteBuffer bb = ByteBuffer.wrap(new byte[Float.SIZE / Byte.SIZE]);
                        bb.order(ByteOrder.LITTLE_ENDIAN);
                        bb.putFloat(endemicityValue);
                        divaOutputStream.write(bb.array());
                    } else {
                        // No species occurrences in this cell. Endemism value
                        // is zero.
                        ascPrintWriter.print("0");

                        ByteBuffer bb = ByteBuffer.wrap(new byte[Float.SIZE / Byte.SIZE]);
                        bb.order(ByteOrder.LITTLE_ENDIAN);
                        bb.putFloat(0);
                        divaOutputStream.write(bb.array());
                    }

                    if (lon.compareTo(maxLongitude) != 0) {
                        ascPrintWriter.print(" ");
                    }
                }
                ascPrintWriter.println();
            }

            ascPrintWriter.flush();
            ascPrintWriter.close();

            divaOutputStream.flush();
            divaOutputStream.close();

            // Write header file for DIVA output
            DensityLayers.writeHeader(new File(outputFileDirectory, outputFileNamePrefix + ".grd").getAbsolutePath(), _resolution.doubleValue(), numRows, numColumns, -180, -90, 180, 90, 0, maxValue,
                    -9999);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}