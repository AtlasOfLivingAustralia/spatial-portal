package org.ala.spatial.analysis.layers;

import au.com.bytecode.opencsv.CSVReader;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.zip.GZIPInputStream;

import org.ala.layers.intersect.SimpleRegion;

/**
 * @author Adam
 */
public class Records {

    ArrayList<Double> points;
    ArrayList<Integer> lsidIdx;
    ArrayList<Short> years;
    String[] lsids;
    int speciesSize;

    public Records(String biocache_service_url, String q, double[] bbox, String filename, SimpleRegion region) throws IOException {
        init(biocache_service_url, q, bbox, filename, region, "names_and_lsid");
    }

    public Records(String biocache_service_url, String q, double[] bbox, String filename, SimpleRegion region, String facetField) throws IOException {
        init(biocache_service_url, q, bbox, filename, region, facetField);
    }

    void init(String biocache_service_url, String q, double[] bbox, String filename, SimpleRegion region, String facetField) throws IOException {
        int speciesEstimate = 250000;
        int recordsEstimate = 26000000;
        int pageSize = 1000000;

        String bboxTerm = null;
        if (bbox != null) {
            bboxTerm = String.format("&fq=longitude:%%5B%f%%20TO%%20%f%%5D%%20AND%%20latitude:%%5B%f%%20TO%%20%f%%5D", bbox[0], bbox[2], bbox[1], bbox[3]);
        } else {
            bboxTerm = "";
        }

        points = new ArrayList<Double>(recordsEstimate);
        lsidIdx = new ArrayList<Integer>(recordsEstimate);
        years = new ArrayList<Short>(recordsEstimate);
        HashMap<String, Integer> lsidMap = new HashMap<String, Integer>(speciesEstimate);

        int start = 0;

        RandomAccessFile raf = null;
        if (filename != null) {
            raf = new RandomAccessFile(filename, "rw");
        }

        while (true && start < 300000000) {
            String url = biocache_service_url + "/webportal/occurrences.gz?q=" + q.replace(" ", "%20") + bboxTerm + "&pageSize=" + pageSize + "&start=" + start + "&fl=longitude,latitude," + facetField + ",year";

            int tryCount = 0;
            InputStream is = null;
            CSVReader csv = null;
            int maxTrys = 4;
            while (tryCount < maxTrys && csv == null) {
                tryCount++;
                try {
                    is = getUrlStream(url);
                    csv = new CSVReader(new InputStreamReader(new GZIPInputStream(is)));
                } catch (Exception e) {
                    System.out.println("failed try " + tryCount + " of " + maxTrys + ": " + url);
                    e.printStackTrace();
                }
            }

            if (csv == null) {
                throw new IOException("failed to get records from biocache.");
            }

            String[] line;
            int[] header = new int[4]; //to contain [0]=lsid, [1]=longitude, [2]=latitude, [3]=year
            int row = start;
            int currentCount = 0;
            while ((line = csv.readNext()) != null) {
                if (raf != null) {
                    for (int i = 0; i < line.length; i++) {
                        if (i > 0) {
                            raf.write(",".getBytes());
                        }
                        raf.write(line[i].getBytes());
                    }
                    raf.write("\n".getBytes());
                }
                currentCount++;
                if (currentCount == 1) {
                    //determine header
                    for (int i = 0; i < line.length; i++) {
                        if (line[i].equals(facetField)) {
                            header[0] = i;
                        }
                        if (line[i].equals("longitude")) {
                            header[1] = i;
                        }
                        if (line[i].equals("latitude")) {
                            header[2] = i;
                        }
                        if (line[i].equals("year")) {
                            header[3] = i;
                        }
                    }
                    System.out.println("header info:" + header[0] + "," + header[1] + "," + header[2] + "," + header[3]);
                } else {
                    if (line.length >= 3) {
                        try {
                            double longitude = Double.parseDouble(line[header[1]]);
                            double latitude = Double.parseDouble(line[header[2]]);
                            if (region == null || region.isWithin_EPSG900913(longitude, latitude)) {
                                points.add(longitude);
                                points.add(latitude);
                                String species = line[header[0]];
                                Integer idx = lsidMap.get(species);
                                if (idx == null) {
                                    idx = lsidMap.size();
                                    lsidMap.put(species, idx);
                                }
                                lsidIdx.add(idx);
                                years.add(Short.parseShort(line[header[3]]));
                            }
                        } catch (Exception e) {
                        } finally {
                            if (lsidIdx.size() * 2 < points.size()) {
                                points.remove(points.size() - 1);
                                points.remove(points.size() - 1);
                            } else if (years.size() < lsidIdx.size()) {
                                years.add((short) 0);
                            }
                        }
                    }
                }
                row++;
            }
            if (start == 0) {
                start = row - 1; //offset for header
            } else {
                start = row;
            }

            csv.close();
            is.close();

            if (currentCount == 0 || currentCount < pageSize) {
                break;
            }
        }

        if (raf != null) {
            raf.close();
        }

        //make lsid list
        lsids = new String[lsidMap.size()];
        for (Entry<String, Integer> e : lsidMap.entrySet()) {
            lsids[e.getValue()] = e.getKey();
        }

        System.out.println("Got " + getRecordsSize() + " records of " + getSpeciesSize() + " species");
    }

    public Records(String filename) throws IOException {
        int speciesEstimate = 250000;
        int recordsEstimate = 26000000;

        points = new ArrayList<Double>(recordsEstimate);
        lsidIdx = new ArrayList<Integer>(recordsEstimate);
        HashMap<String, Integer> lsidMap = new HashMap<String, Integer>(speciesEstimate);

        int start = 0;

        BufferedReader br = new BufferedReader(new FileReader(filename));
        //CSVReader csv = new CSVReader(new FileReader(filename));

        String[] line;
        String rawline;
        int[] header = new int[4]; //to contain [0]=lsid, [1]=longitude, [2]=latitude
        int row = start;
        int currentCount = 0;
        String lat, lng, sp;
        int p1, p2, p3;
        line = new String[4];
        //while((line = csv.readNext()) != null) {
        while ((rawline = br.readLine()) != null) {
            currentCount++;

            p1 = rawline.indexOf(',');
            p2 = rawline.indexOf(',', p1 + 1);
            p3 = rawline.indexOf(',', p2 + 1);
            if (p1 < 0 || p2 < 0 || p3 < 0) {
                continue;
            }
            line[0] = rawline.substring(0, p1);
            line[1] = rawline.substring(p1 + 1, p2);
            line[2] = rawline.substring(p2 + 1, p3);
            line[3] = rawline.substring(p3 + 1, rawline.length());

            if (currentCount % 100000 == 0) {
                System.out.print("\rreading row: " + currentCount);
            }

            String facetName = "names_and_lsid";
            if (row == 0) {
                //determine header
                for (int i = 0; i < line.length; i++) {
                    if (line[i].equals(facetName)) {
                        header[0] = i;
                    }
                    if (line[i].equals("longitude")) {
                        header[1] = i;
                    }
                    if (line[i].equals("latitude")) {
                        header[2] = i;
                    }
                    if (line[i].equals("year")) {
                        header[3] = i;
                    }
                }
                System.out.println("line: " + line[0] + "," + line[1] + "," + line[2] + "," + line[3]);
                System.out.println("header: " + header[0] + "," + header[1] + "," + header[2] + "," + header[3]);
                boolean notZero = header[1] == 0 || header[2] == 0 || (header[3] == 0 && line.length > 3); //'year' may be absent
                boolean notOne = line.length < 1 || header[1] == 1 || header[2] == 1 || header[3] == 1;
                boolean notTwo = line.length < 2 || header[1] == 2 || header[2] == 2 || header[3] == 2;
                boolean notThree = line.length < 3 || header[1] == 3 || header[2] == 3 || header[3] == 3;
                if (!notZero) header[0] = 0;
                if (!notOne) header[0] = 1;
                if (!notTwo) header[0] = 2;
                if (!notThree) header[0] = 3;
                System.out.println("header: " + header[0] + "," + header[1] + "," + header[2] + "," + header[3]);
            } else {
                if (line.length >= 3) {
                    try {
                        double longitude = Double.parseDouble(line[header[1]]);
                        double latitude = Double.parseDouble(line[header[2]]);
                        points.add(longitude);
                        points.add(latitude);
                        String species = line[header[0]];
                        Integer idx = lsidMap.get(species);
                        if (idx == null) {
                            idx = lsidMap.size();
                            lsidMap.put(species, idx);
                        }
                        lsidIdx.add(idx);
                    } catch (Exception e) {
                    }
                }
            }
            row++;
        }
        if (start == 0) {
            start = row - 1; //offset for header
        } else {
            start = row;
        }

        //csv.close();
        br.close();

        //make lsid list
        lsids = new String[lsidMap.size()];
        for (Entry<String, Integer> e : lsidMap.entrySet()) {
            lsids[e.getValue()] = e.getKey();
        }

        System.out.println("\nGot " + getRecordsSize() + " records of " + getSpeciesSize() + " species");
    }

    public Records(String filename, SimpleRegion region) throws IOException {
        int speciesEstimate = 250000;
        int recordsEstimate = 26000000;

        points = new ArrayList<Double>(recordsEstimate);
        lsidIdx = new ArrayList<Integer>(recordsEstimate);
        HashMap<String, Integer> lsidMap = new HashMap<String, Integer>(speciesEstimate);

        int start = 0;

        BufferedReader br = new BufferedReader(new FileReader(filename));
        //CSVReader csv = new CSVReader(new FileReader(filename));

        String[] line;
        String rawline;
        int[] header = new int[4]; //to contain [0]=lsid, [1]=longitude, [2]=latitude
        int row = start;
        int currentCount = 0;
        String lat, lng, sp;
        int p1, p2, p3;
        line = new String[4];
        //while((line = csv.readNext()) != null) {
        while ((rawline = br.readLine()) != null) {
            currentCount++;

            p1 = rawline.indexOf(',');
            p2 = rawline.indexOf(',', p1 + 1);
            p3 = rawline.indexOf(',', p2 + 1);
            if (p1 < 0 || p2 < 0 || p3 < 0) {
                continue;
            }
            line[0] = rawline.substring(0, p1);
            line[1] = rawline.substring(p1 + 1, p2);
            line[2] = rawline.substring(p2 + 1, p3);
            line[3] = rawline.substring(p3 + 1, rawline.length());

            if (currentCount % 100000 == 0) {
                System.out.print("\rreading row: " + currentCount);
            }

            String facetName = "names_and_lsid";
            if (row == 0) {
                //determine header
                for (int i = 0; i < line.length; i++) {
                    if (line[i].equals(facetName)) {
                        header[0] = i;
                    }
                    if (line[i].equals("longitude")) {
                        header[1] = i;
                    }
                    if (line[i].equals("latitude")) {
                        header[2] = i;
                    }
                    if (line[i].equals("year")) {
                        header[3] = i;
                    }
                }
                System.out.println("line: " + line[0] + "," + line[1] + "," + line[2] + "," + line[3]);
                System.out.println("header: " + header[0] + "," + header[1] + "," + header[2] + "," + header[3]);
                boolean notZero = header[1] == 0 || header[2] == 0 || (header[3] == 0 && line.length > 3); //'year' may be absent
                boolean notOne = line.length < 1 || header[1] == 1 || header[2] == 1 || header[3] == 1;
                boolean notTwo = line.length < 2 || header[1] == 2 || header[2] == 2 || header[3] == 2;
                boolean notThree = line.length < 3 || header[1] == 3 || header[2] == 3 || header[3] == 3;
                if (!notZero) header[0] = 0;
                if (!notOne) header[0] = 1;
                if (!notTwo) header[0] = 2;
                if (!notThree) header[0] = 3;
                System.out.println("header: " + header[0] + "," + header[1] + "," + header[2] + "," + header[3]);
            } else {
                if (line.length >= 3) {
                    try {
                        double longitude = Double.parseDouble(line[header[1]]);
                        double latitude = Double.parseDouble(line[header[2]]);
                        if (region != null && !region.isWithin(longitude, latitude)) {
                            continue;
                        }
                        points.add(longitude);
                        points.add(latitude);
                        String species = line[header[0]];
                        Integer idx = lsidMap.get(species);
                        if (idx == null) {
                            idx = lsidMap.size();
                            lsidMap.put(species, idx);
                        }
                        lsidIdx.add(idx);
                    } catch (Exception e) {
                    }
                }
            }
            row++;
        }
        if (start == 0) {
            start = row - 1; //offset for header
        } else {
            start = row;
        }

        //csv.close();
        br.close();

        //make lsid list
        lsids = new String[lsidMap.size()];
        for (Entry<String, Integer> e : lsidMap.entrySet()) {
            lsids[e.getValue()] = e.getKey();
        }

        System.out.println("\nGot " + getRecordsSize() + " records of " + getSpeciesSize() + " species");
    }

    public String getSpecies(int pos) {
        return lsids[lsidIdx.get(pos)];
    }

    public int getSpeciesNumber(int pos) {
        return lsidIdx.get(pos);
    }

    public double getLongitude(int pos) {
        return points.get(pos * 2);
    }

    public double getLatitude(int pos) {
        return points.get(pos * 2 + 1);
    }

    public short getYear(int pos) {
        return years.get(pos);
    }

    public int getRecordsSize() {
        return lsidIdx.size();
    }

    public int getSpeciesSize() {
        if (lsids == null) {
            return speciesSize;
        } else {
            return lsids.length;
        }
    }

    public void removeSpeciesNames() {
        speciesSize = lsids.length;
        lsids = null;
    }

    Integer[] sortOrder;
    int[] sortOrderRowStarts;
    double soMinLat;
    double soMinLong;
    int soHeight;
    double soResolution;
    boolean soSortedStarts;
    boolean soSortedRowStarts;

    public int[] sortedRowStarts(double minLat, int height, double resolution) {
        if (sortOrder != null && soMinLat == minLat
                && soHeight == height && soResolution == resolution
                && soSortedRowStarts) {
            return sortOrderRowStarts;
        }
        //init
        sortOrder = new Integer[points.size() / 2];
        for (int i = 0; i < sortOrder.length; i++) {
            sortOrder[i] = i * 2 + 1;
        }

        final double mLat = minLat;
        final double res = resolution;
        final int h = height;

        //sort
        java.util.Arrays.sort(sortOrder, new Comparator<Integer>() {

            @Override
            public int compare(Integer o1, Integer o2) {
                return (h - 1 - ((int) ((points.get(o1) - mLat) / res)))
                        - (h - 1 - ((int) ((points.get(o2) - mLat) / res)));
            }
        });

        //get row starts
        int[] rowStarts = new int[height];
        int row = 0;
        for (int i = 0; i < sortOrder.length; i++) {
            int thisRow = (h - 1 - (int) ((points.get(sortOrder[i]) - mLat) / res));

            //handle overflow
            if (thisRow >= height) {
                for (int j = row + 1; j < height; j++) {
                    rowStarts[j] = rowStarts[j - 1];
                }
                break;
            }

            //apply next row start
            if (thisRow > row) {
                for (int j = row + 1; j < thisRow; j++) {
                    rowStarts[j] = i;
                }
                rowStarts[thisRow] = i;
                row = thisRow;
            }
        }
        for (int j = row + 1; j < height; j++) {
            rowStarts[j] = sortOrder.length;
        }

        //translate sortOrder values from latitude to idx
        for (int i = 0; i < sortOrder.length; i++) {
            sortOrder[i] = (sortOrder[i] - 1) / 2;
        }

        sortOrderRowStarts = rowStarts;
        soMinLat = minLat;
        soHeight = height;
        soResolution = resolution;
        soSortedStarts = false;
        soSortedRowStarts = true;
        return rowStarts;
    }

    public void sortedStarts(double minLat, double minLong, double resolution) {
        if (sortOrder != null && soMinLat == minLat
                && soMinLong == minLong
                && soResolution == resolution
                && soSortedStarts) {
            return;
        }
        //init
        sortOrder = new Integer[points.size() / 2];
        for (int i = 0; i < sortOrder.length; i++) {
            sortOrder[i] = i * 2 + 1;
        }

        final double mLat = minLat;
        final double mLong = minLong;
        final double res = resolution;

        //sort
        java.util.Arrays.sort(sortOrder, new Comparator<Integer>() {

            @Override
            public int compare(Integer o1, Integer o2) {
                int v = ((int) ((points.get(o1) - mLat) / res))
                        - ((int) ((points.get(o2) - mLat) / res));

                if (v == 0) {
                    return ((int) ((points.get(o1 - 1) - mLong) / res))
                            - ((int) ((points.get(o2 - 1) - mLong) / res));
                } else {
                    return v;
                }
            }
        });

        soMinLat = minLat;
        soMinLong = minLong;
        soResolution = resolution;
        soSortedStarts = true;
        soSortedRowStarts = false;
    }

    public String getSortedSpecies(int pos) {
        return lsids[lsidIdx.get(sortOrder[pos])];
    }

    public int getSortedSpeciesNumber(int pos) {
        return lsidIdx.get(sortOrder[pos]);
    }

    public double getSortedLongitude(int pos) {
        return points.get(sortOrder[pos] * 2);
    }

    public double getSortedLatitude(int pos) {
        return points.get(sortOrder[pos] * 2 + 1);
    }

    static InputStream getUrlStream(String url) throws IOException {
        System.out.print("getting : " + url + " ... ");
        long start = System.currentTimeMillis();
        URLConnection c = new URL(url).openConnection();
        InputStream is = c.getInputStream();
        System.out.print((System.currentTimeMillis() - start) + "ms\n");
        return is;
    }
}
