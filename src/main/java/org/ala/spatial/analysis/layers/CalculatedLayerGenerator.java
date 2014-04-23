package org.ala.spatial.analysis.layers;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.math3.util.Pair;

/**
 * Generates a layer using species cell count and cell species list data
 * generated from the biocache-store. The layer is generated for the entire
 * globe (latitude -90 to 90, longitude -180 to 180)
 * <p/>
 * Layer data is output in both ESRI ASCII grid format (.asc) and in DIVA grid
 * format (.gri/.grd). -9999 is used as the no data value.
 *
 * @author ChrisF
 */
public abstract class CalculatedLayerGenerator {

    protected Map<String, Integer> _speciesCellCounts;
    protected Map<Pair<BigDecimal, BigDecimal>, List<String>> _cellSpecies;
    protected Map<Pair<BigDecimal, BigDecimal>, Long> _cellOccurrenceCounts;
    protected BigDecimal _resolution;

    public CalculatedLayerGenerator(BigDecimal resolution) throws IOException {
        _resolution = resolution;
    }

    // read species cell counts into memory
    protected void readSpeciesCellCounts(File speciesCellCountFile) throws IOException {
        _speciesCellCounts = new HashMap<String, Integer>();

        List<String> speciesCellCountLines = FileUtils.readLines(speciesCellCountFile);
        for (String line : speciesCellCountLines) {
            String[] tokens = line.split(",");
            String speciesLsid = tokens[0];
            int cellCount = Integer.parseInt(tokens[1]);
            _speciesCellCounts.put(speciesLsid, cellCount);
        }
    }

    // Read cell species lists into memory
    protected void readCellSpeciesLists(File cellSpeciesFile) throws IOException {
        _cellSpecies = new HashMap<Pair<BigDecimal, BigDecimal>, List<String>>();

        List<String> cellSpeciesLines = FileUtils.readLines(cellSpeciesFile);
        for (String line : cellSpeciesLines) {
            String[] tokens = line.split(",");
            BigDecimal latitude = new BigDecimal(tokens[0]).setScale(2);
            BigDecimal longitude = new BigDecimal(tokens[1]).setScale(2);
            List<String> speciesLsids = Arrays.asList(Arrays.copyOfRange(tokens, 2, tokens.length));
            _cellSpecies.put(new Pair(latitude, longitude), speciesLsids);
        }
    }

    // Read cell species lists into memory
    protected void readCellOccurrenceCounts(File cellOccurrenceCountsFile) throws IOException {
        _cellOccurrenceCounts = new HashMap<Pair<BigDecimal, BigDecimal>, Long>();

        List<String> cellOccurrenceCountsLines = FileUtils.readLines(cellOccurrenceCountsFile);
        for (String line : cellOccurrenceCountsLines) {
            String[] tokens = line.split(",");
            BigDecimal latitude = new BigDecimal(tokens[0]).setScale(2);
            BigDecimal longitude = new BigDecimal(tokens[1]).setScale(2);
            long cellOccurrencesCount = Long.parseLong(tokens[2]);
            _cellOccurrenceCounts.put(new Pair(latitude, longitude), cellOccurrencesCount);
        }
    }

    protected int calculateNumberOfRows() {
        return (int) (180 / _resolution.floatValue());
    }

    protected int calculateNumberOfColumns() {
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

                    maxValue = handleCell(coordPair, maxValue, ascPrintWriter, divaOutputStream);

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

    /**
     * Handles the cell as defined by the coordiate pair, writes necessary
     * changes to the ascPrintWriter and divaOutputStream
     *
     * @param coordPair        the coordinate pair that defines the cell
     * @param maxValue         the current maximum value for the generated raster layer
     * @param ascPrintWriter   PrintWriter for writing the asc grid
     * @param divaOutputStream output stream for writing the diva grid
     * @return the new maxValue, this will equal to or greater than the maxValue
     * supplied to the method.
     */
    protected abstract float handleCell(Pair<BigDecimal, BigDecimal> coordPair, float maxValue, PrintWriter ascPrintWriter, BufferedOutputStream divaOutputStream) throws IOException;

}
