package org.ala.spatial.analysis.layers;

import java.io.FileWriter;
import java.io.InputStream;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.codec.net.URLCodec;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.IOUtils;
import org.apache.commons.math3.util.Pair;
import org.apache.commons.math3.util.Precision;

public class Endemism {

    private static String FACET_DOWNLOAD_URL_TEMPLATE = "/occurrences/facets/download?q={0}&facets={1}";

    private String biocache_service_url = "http://biocache.ala.org.au/ws";
    //private String biocache_service_url = "http://ala-rufus.it.csiro.au/biocache-service/";

    private Map<String, Integer> _speciesCellCounts;
    private Map<String, Double> _speciesCellAreas;


    private Map<Pair<BigDecimal, BigDecimal>, Set<String>> cellSpecies;

    public static void main(String[] args) throws Exception {
        new Endemism().createEndemismLayer();
    }

    public Endemism() {
        _speciesCellCounts = new HashMap<String, Integer>();
        _speciesCellAreas = new HashMap<String, Double>();
        cellSpecies = new HashMap<Pair<BigDecimal, BigDecimal>, Set<String>>();
    }

    public void createEndemismLayer() throws Exception {
        // get list of species
        List<String> speciesLsids = doFacetDownload("taxon_concept_lsid:[* TO *]", "taxon_concept_lsid");
        System.out.println(speciesLsids.size());
        // List<String> speciesLsids = new ArrayList<String>();
        // speciesLsids.add("urn:lsid:biodiversity.org.au:afd.taxon:3b127a18-0d9e-408e-b622-6dadbdc9f2f3");

        // remove first line as this will contain the text "taxon_concept_id"
        speciesLsids.remove(0);

        for (String lsid : speciesLsids.subList(0, 5)) {

            List<String> occurrencePoints = doFacetDownload("taxon_concept_lsid:" + lsid + " AND geospatial_kosher:true", "point-0.001");
            System.out.println(lsid + ": " + occurrencePoints.size());

            // remove first line as this will contain the text
            // "taxon_concept_id"
            occurrencePoints.remove(0);

            Set<Pair<BigDecimal, BigDecimal>> roundedPointsSet = new HashSet<Pair<BigDecimal, BigDecimal>>();

            double minLatitude = Double.POSITIVE_INFINITY;
            double maxLatitude = Double.NEGATIVE_INFINITY;
            double minLongitude = Double.POSITIVE_INFINITY;
            double maxLongitude = Double.NEGATIVE_INFINITY;

            double plus360MinLongitude = Double.POSITIVE_INFINITY;
            double plus360MaxLongitude = Double.NEGATIVE_INFINITY;

            for (String point : occurrencePoints) {
                String[] splitPoint = point.split(",");
                String strLatitude = splitPoint[0];
                String strLongitude = splitPoint[1];

                double rawLatitude = Double.parseDouble(strLatitude);
                double rawLongitude = Double.parseDouble(strLongitude);

                if (rawLatitude < minLatitude) {
                    minLatitude = rawLatitude;
                }

                if (rawLatitude > maxLatitude) {
                    maxLatitude = rawLatitude;
                }

                if (rawLongitude < minLongitude) {
                    minLongitude = rawLongitude;
                }

                if (rawLongitude > maxLongitude) {
                    maxLongitude = rawLongitude;
                }

                if (rawLongitude < 0) {
                    double plus360Longitude = rawLongitude + 360;

                    if (plus360Longitude > plus360MaxLongitude) {
                        plus360MaxLongitude = plus360Longitude;
                    }
                } else {
                    if (rawLongitude < plus360MinLongitude) {
                        plus360MinLongitude = rawLongitude;
                    }
                }

                BigDecimal roundedLatitude = new BigDecimal(rawLatitude).setScale(2, BigDecimal.ROUND_CEILING);
                BigDecimal roundedLongitude = new BigDecimal(rawLongitude).setScale(2, BigDecimal.ROUND_FLOOR);

                Pair<BigDecimal, BigDecimal> cellCoordPair = new Pair<BigDecimal, BigDecimal>(roundedLatitude, roundedLongitude);
                Set<String> thisCellSpecies = cellSpecies.get(cellCoordPair);
                if (thisCellSpecies == null) {
                    thisCellSpecies = new HashSet<String>();
                    cellSpecies.put(cellCoordPair, thisCellSpecies);
                }
                thisCellSpecies.add(lsid);

                roundedPointsSet.add(cellCoordPair);
            }

            _speciesCellCounts.put(lsid, roundedPointsSet.size());

            double roundedMinLatitude = Precision.round(minLatitude, 2, BigDecimal.ROUND_FLOOR);
            double roundedMaxLatitude = Precision.round(maxLatitude, 2, BigDecimal.ROUND_CEILING);
            double roundedMinLongitude = Precision.round(minLongitude, 2, BigDecimal.ROUND_FLOOR);
            double roundedMaxLongitude = Precision.round(maxLongitude, 2, BigDecimal.ROUND_CEILING);

            double areaRegular = (roundedMaxLongitude - roundedMinLongitude) * (roundedMaxLatitude - roundedMinLatitude);
            _speciesCellAreas.put(lsid, areaRegular);


            if (plus360MinLongitude != Double.POSITIVE_INFINITY && plus360MaxLongitude != Double.NEGATIVE_INFINITY) {
                double roundedPlus360MinLongitude = Precision.round(plus360MinLongitude, 2, BigDecimal.ROUND_FLOOR);
                double roundedPlus360MaxLongitude = Precision.round(plus360MaxLongitude, 2, BigDecimal.ROUND_CEILING);
                double areaPlus360 = (roundedPlus360MaxLongitude - roundedPlus360MinLongitude) * (roundedMaxLatitude - roundedMinLatitude);
                if (areaPlus360 < areaRegular) {
                    _speciesCellAreas.put(lsid, areaPlus360);
                }

            }
        }

        // for each species,
        // get no of grid cells occurring in
        // get area of bounding box of records - rounded out to complete 1
        // degree cells.
        // record data

        writeGrid();
    }

    private void writeGrid() {

        try {
            FileWriter fw = new FileWriter("C:\\Users\\ChrisF\\eclipse-workspace\\alaspatial\\testgrid.asc");
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

                    if (cellSpecies.containsKey(coordPair)) {
                        double endemicityValue = 0; 
                        Set<String> speciesLsids = cellSpecies.get(coordPair);
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

    private List<String> doFacetDownload(String query, String facet) throws Exception {
        URLCodec urlCodec = new URLCodec();

        String url = MessageFormat.format(biocache_service_url + FACET_DOWNLOAD_URL_TEMPLATE, urlCodec.encode(query), urlCodec.encode(facet));

        HttpClient httpClient = new HttpClient();
        GetMethod get = new GetMethod(url);
        try {
            int responseCode = httpClient.executeMethod(get);
            if (responseCode == 200) {
                InputStream contentStream = get.getResponseBodyAsStream();

                List<String> lines = IOUtils.readLines(contentStream);
                return lines;
            } else {
                throw new Exception("facet download request failed (" + responseCode + ")");
            }
        } finally {
            get.releaseConnection();
        }
    }

}
