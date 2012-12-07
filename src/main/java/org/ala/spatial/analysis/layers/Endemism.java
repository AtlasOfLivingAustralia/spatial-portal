package org.ala.spatial.analysis.layers;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;

public class Endemism {

    private static String FACET_DOWNLOAD_URL_TEMPLATE = "/occurrences/facets/download?q={0}&facets={1}";
    private static String SPECIES_BOUNDING_BOX_URL_TEMPLATE = "/mapping/bbox?q=taxon_concept_lsid:{0}";

    // private String biocache_service_url = "http://biocache.ala.org.au/ws";
    private String biocache_service_url = "http://ala-rufus.it.csiro.au/biocache-service/";

    private GeometryFactory _geometryFactory;

    private Map<String, Integer> speciesCellCounts;
    private Map<String, Double> speciesCellAreas;

    private Map<Pair<Double, Double>, Set<String>> cellSpecies;

    public static void main(String[] args) throws Exception {
        new Endemism().createEndemismLayer();
    }

    public Endemism() {
        _geometryFactory = new GeometryFactory();
        speciesCellCounts = new HashMap<String, Integer>();
        speciesCellAreas = new HashMap<String, Double>();
        cellSpecies = new HashMap<Pair<Double, Double>, Set<String>>();
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
            System.out.println(lsid);
            List<String> occurrencePoints = doFacetDownload("taxon_concept_lsid:" + lsid + " AND geospatial_kosher:true", "point-0.001");

            // remove first line as this will contain the text
            // "taxon_concept_id"
            occurrencePoints.remove(0);

            Set<String> roundedPointsSet = new HashSet<String>();

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

                double roundedLatitude = Precision.round(rawLatitude, 2, BigDecimal.ROUND_CEILING);
                double roundedLongitude = Precision.round(rawLongitude, 2, BigDecimal.ROUND_FLOOR);

                Pair<Double, Double> cellCoordPair = new Pair<Double, Double>(roundedLatitude, roundedLongitude);
                Set<String> thisCellSpecies = cellSpecies.get(cellCoordPair);
                if (thisCellSpecies == null) {
                    thisCellSpecies = new HashSet<String>();
                    cellSpecies.put(cellCoordPair, thisCellSpecies);
                }
                thisCellSpecies.add(lsid);

                roundedPointsSet.add(roundedLatitude + "," + roundedLongitude);
            }

            speciesCellCounts.put(lsid, roundedPointsSet.size());

            double roundedMinLatitude = Precision.round(minLatitude, 2, BigDecimal.ROUND_FLOOR);
            double roundedMaxLatitude = Precision.round(maxLatitude, 2, BigDecimal.ROUND_CEILING);
            double roundedMinLongitude = Precision.round(minLongitude, 2, BigDecimal.ROUND_FLOOR);
            double roundedMaxLongitude = Precision.round(maxLongitude, 2, BigDecimal.ROUND_CEILING);

            double areaRegular = (roundedMaxLongitude - roundedMinLongitude) * (roundedMaxLatitude - roundedMinLatitude);
            speciesCellAreas.put(lsid, areaRegular);
            // System.out.println("Regular area: " + areaRegular);
            // System.out.println("POLYGON((" + minLongitude + " " + maxLatitude
            // + ", " + maxLongitude + " " + maxLatitude + ", " + maxLongitude +
            // " " + minLatitude + ", " + minLongitude + " "
            // + minLatitude + ", " + minLongitude + " " + maxLatitude + "))");

            if (plus360MinLongitude != Double.POSITIVE_INFINITY && plus360MaxLongitude != Double.NEGATIVE_INFINITY) {
                double roundedPlus360MinLongitude = Precision.round(plus360MinLongitude, 2, BigDecimal.ROUND_FLOOR);
                double roundedPlus360MaxLongitude = Precision.round(plus360MaxLongitude, 2, BigDecimal.ROUND_CEILING);
                double areaPlus360 = (roundedPlus360MaxLongitude - roundedPlus360MinLongitude) * (roundedMaxLatitude - roundedMinLatitude);
                if (areaPlus360 < areaRegular) {
                    speciesCellAreas.put(lsid, areaPlus360);
                }
                // System.out.println("+360 area: " + areaPlus360);
                // System.out.println("POLYGON((" + plus360MinLongitude + " " +
                // maxLatitude + ", " + plus360MaxLongitude + " " + maxLatitude
                // + ", " + plus360MaxLongitude + " " + minLatitude + ", "
                // + plus360MinLongitude + " " + minLatitude + ", " +
                // plus360MinLongitude + " " + maxLatitude + "))");
            }

            // System.out.println(pointsSet.size());

            // System.out.println(getSpeciesArea(lsid));
            // speciesCellCounts.put(lsid, roundedPointsSet.size());
            // speciesCellAreas.put(lsid, getSpeciesArea(lsid));
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
            
            List<Pair<Double, Double>> sortedCoords = new ArrayList<Pair<Double,Double>>(cellSpecies.keySet());
            Collections.sort(sortedCoords, new PairComparator());
            
            for (double lat=90.0; lat >= -90.0; lat -= 0.01) {
                // a row for each 0.01 latitude    
                
                for (double lon=-180.0; lon <= 180.0; lon += 0.01) {
                    // a column for each 0.01 longitude
                    
                    Pair<Double, Double> coordPair = new Pair<Double, Double>(lat, lon);
                    
                    double endemicityValue = 0;
                    
                    if (cellSpecies.containsKey(coordPair)) {
                        Set<String> speciesLsids = cellSpecies.get(coordPair);
                        for (String lsid: speciesLsids) {
                            int speciesCellCount = speciesCellCounts.get(lsid);
                            endemicityValue += 1.0 / speciesCellCount;
                        }
                    }
                    
                    pw.println(endemicityValue + " ");
                }
                
            }
            
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private class PairComparator implements Comparator<Pair<Double, Double>> {

        @Override
        public int compare(Pair<Double, Double> o1, Pair<Double, Double> o2) {
            if (o1.getKey().equals(o2.getKey())) {
                return (o1.getValue().compareTo(o2.getValue()));
            } else {
                return (o1.getKey().compareTo(o2.getKey()));
            }
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

    // private double getSpeciesArea(String lsid) throws Exception {
    // String bboxString = getSpeciesBbox(lsid);
    // String[] bboxCoords = bboxString.split(",");
    // String strMinLong = bboxCoords[0];
    // String strMinLat = bboxCoords[1];
    // String strMaxLong = bboxCoords[2];
    // String strMaxLat = bboxCoords[3];
    //
    // // Round the bounding box corners to the nearest 0.01 degree as we are
    // // interested in the area of the 0.01 degree squares
    // // in which the occurrences fall
    // // val roundedMinLong =
    // // math.floor(java.lang.Double.parseDouble(strMinLong))
    // // val roundedMinLat =
    // // math.floor(java.lang.Double.parseDouble(strMinLat))
    // // val roundedMaxLong =
    // // math.ceil(java.lang.Double.parseDouble(strMaxLong))
    // // val roundedMaxLat =
    // // math.ceil(java.lang.Double.parseDouble(strMaxLat))
    //
    // double roundedMinLong = Precision.round(Double.parseDouble(strMinLong),
    // 2, BigDecimal.ROUND_FLOOR);
    // double roundedMinLat = Precision.round(Double.parseDouble(strMinLat), 2,
    // BigDecimal.ROUND_FLOOR);
    // double roundedMaxLong = Precision.round(Double.parseDouble(strMaxLong),
    // 2, BigDecimal.ROUND_CEILING);
    // double roundedMaxLat = Precision.round(Double.parseDouble(strMaxLat), 2,
    // BigDecimal.ROUND_CEILING);
    //
    // // val roundedMinLong = 75.759307861328
    // // val roundedMinLat = -56.214631594657
    // // val roundedMaxLong = 213.22024536133
    // // val roundedMaxLat = 16.222085414693
    //
    // Coordinate coord1 = new Coordinate(roundedMinLong, roundedMaxLat);
    // Coordinate coord2 = new Coordinate(roundedMaxLong, roundedMaxLat);
    // Coordinate coord3 = new Coordinate(roundedMaxLong, roundedMinLat);
    // Coordinate coord4 = new Coordinate(roundedMinLong, roundedMinLat);
    // Coordinate coord5 = new Coordinate(roundedMinLong, roundedMaxLat);
    //
    // // System.out.println("POLYGON((" + strMinLong + " " + strMaxLat + ", "
    // // + strMaxLong + " " + strMaxLat + ", " + strMaxLong + " " + strMinLat
    // // + ", " + strMinLong + " "
    // // + strMinLat + ", " + strMinLong + " " + strMaxLat + "))");
    //
    // System.out.println("POLYGON((" + roundedMinLong + " " + roundedMaxLat +
    // ", " + roundedMaxLong + " " + roundedMaxLat + ", " + roundedMaxLong + " "
    // + roundedMinLat + ", " + roundedMinLong + " "
    // + roundedMinLat + ", " + roundedMinLong + " " + roundedMaxLat + "))");
    //
    // Coordinate[] coordArray = new Coordinate[] { coord1, coord2, coord3,
    // coord4, coord5 };
    //
    // LinearRing polygonBoundary =
    // _geometryFactory.createLinearRing(coordArray);
    // Polygon polygon = _geometryFactory.createPolygon(polygonBoundary, null);
    //
    // return polygon.getArea();
    // }
    //
    // private String getSpeciesBbox(String lsid) throws Exception {
    // String url = MessageFormat.format(biocache_service_url +
    // SPECIES_BOUNDING_BOX_URL_TEMPLATE, lsid);
    //
    // HttpClient httpClient = new HttpClient();
    // GetMethod get = new GetMethod(url);
    // try {
    // int responseCode = httpClient.executeMethod(get);
    // if (responseCode == 200) {
    // InputStream contentStream = get.getResponseBodyAsStream();
    // String bbox = IOUtils.toString(contentStream);
    // return bbox;
    // } else {
    // throw new Exception("facet download request failed (" + responseCode +
    // ")");
    // }
    // } finally {
    // get.releaseConnection();
    // }
    // }

}
