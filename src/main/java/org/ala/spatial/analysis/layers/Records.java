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

/**
 *
 * @author Adam
 */
public class Records {

    ArrayList<Double> points;
    ArrayList<Integer> lsidIdx;
    String[] lsids;
    int speciesSize;

    public Records(String biocache_service_url, String q, double[] bbox, String filename) throws IOException {
        int speciesEstimate = 250000;
        int recordsEstimate = 26000000;
        int pageSize = 1000000;

        String bboxTerm = String.format("longitude:%%5B%f%%20TO%%20%f%%5D%%20AND%%20latitude:%%5B%f%%20TO%%20%f%%5D", bbox[0], bbox[2], bbox[1], bbox[3]);

        points = new ArrayList<Double>(recordsEstimate);
        lsidIdx = new ArrayList<Integer>(recordsEstimate);
        HashMap<String, Integer> lsidMap = new HashMap<String, Integer>(speciesEstimate);

        int start = 0;

        RandomAccessFile raf = null;
        if (filename != null) {
            raf = new RandomAccessFile(filename, "rw");
        }

        while (true && start < 300000000) {
            String url = biocache_service_url + "/webportal/occurrences.gz?q=" + q.replace(" ", "%20") + "&fq=" + bboxTerm + "&pageSize=" + pageSize + "&start=" + start + "&fl=longitude,latitude,names_and_lsid";
            InputStream is = getUrlStream(url);

            CSVReader csv = new CSVReader(new InputStreamReader(new GZIPInputStream(is)));
            if (start != 0) {
                csv.readNext(); //discard header
            }
            String[] line;
            int[] header = new int[3]; //to contain [0]=lsid, [1]=longitude, [2]=latitude
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
                if (row == 0) {
                    //determine header
                    for (int i = 0; i < line.length; i++) {
                        if (line[i].equals("names_and_lsid")) {
                            header[0] = i;
                        }
                        if (line[i].equals("longitude")) {
                            header[1] = i;
                        }
                        if (line[i].equals("latitude")) {
                            header[2] = i;
                        }
                    }
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

            csv.close();
            is.close();

            if (currentCount == 0) {
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

    Records(String filename) throws IOException {
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
        int[] header = new int[3]; //to contain [0]=lsid, [1]=longitude, [2]=latitude
        int row = start;
        int currentCount = 0;
        String lat, lng, sp;
        int p1, p2;
        line = new String[3];
        //while((line = csv.readNext()) != null) {
        while ((rawline = br.readLine()) != null) {
            currentCount++;

            p1 = rawline.indexOf(',');
            p2 = rawline.indexOf(',', p1 + 1);
            if (p1 < 0 || p2 < 0) {
                continue;
            }
            line[0] = rawline.substring(0, p1);
            line[1] = rawline.substring(p1 + 1, p2);
            line[2] = rawline.substring(p2 + 1, rawline.length());

            if (currentCount % 100000 == 0) {
                System.out.print("\rreading row " + currentCount);
            }

            if (row == 0) {
                //determine header
                for (int i = 0; i < line.length; i++) {
                    if (line[i].equals("names_and_lsid")) {
                        header[0] = i;
                    }
                    if (line[i].equals("longitude")) {
                        header[1] = i;
                    }
                    if (line[i].equals("latitude")) {
                        header[2] = i;
                    }
                }
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

        System.out.println("Got " + getRecordsSize() + " records of " + getSpeciesSize() + " species");
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
    int soHeight;
    double soResolution;

    public int[] sortedRowStarts(double minLat, int height, double resolution) {
        if (sortOrder != null && soMinLat == minLat
                && soHeight == height && soResolution == resolution) {
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
        return rowStarts;
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
