package org.ala.spatial.analysis.layers;

import au.com.bytecode.opencsv.CSVReader;
import org.ala.layers.intersect.SimpleRegion;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.zip.GZIPInputStream;

/**
 * @author Adam
 */
public class LayersServiceRecords extends Records {

    public LayersServiceRecords(String biocache_service_url, String q, double[] bbox, String filename, SimpleRegion region) throws IOException {
        super(biocache_service_url, q, bbox, filename, region);
    }

    public LayersServiceRecords(String biocache_service_url, String q, double[] bbox, String filename, SimpleRegion region, String facetField) throws IOException {
        super(biocache_service_url, q, bbox, filename, region, facetField);
    }

    @Override
    void init(String biocache_service_url, String q, double[] bbox, String filename, SimpleRegion region, String facetField) throws IOException {
        int speciesEstimate = 250000;
        int recordsEstimate = 26000000;
        int pageSize = 1000000;

        String bboxTerm = String.format("longitude:%%5B%f%%20TO%%20%f%%5D%%20AND%%20latitude:%%5B%f%%20TO%%20%f%%5D", bbox[0], bbox[2], bbox[1], bbox[3]);

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
            String url = biocache_service_url + "/userdata/sample?id=" + q;

            int tryCount = 0;
            InputStream is = null;
            CSVReader csv = null;
            int maxTrys = 4;
            while (tryCount < maxTrys && csv == null) {
                tryCount++;
                try {
                    is = getUrlStream(url);
                    csv = new CSVReader(new InputStreamReader(is));
                } catch (Exception e) {
                    System.out.println("failed try " + tryCount + " of " + maxTrys + ": " + url);
                    e.printStackTrace();
                }
            }

            if (csv == null) {
                throw new IOException("failed to get records from layers-service.");
            }

            String[] line;
            int[] header = new int[2]; //to contain [0]=longitude, [1]=latitude
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
                        if (line[i].equals("longitude")) {
                            header[0] = i;
                        }
                        if (line[i].equals("latitude")) {
                            header[1] = i;
                        }
                    }
                    System.out.println("header info:" + header[0] + "," + header[1]);
                } else {
                    if (line.length >= 3) {
                        try {
                            double longitude = Double.parseDouble(line[header[0]]);
                            double latitude = Double.parseDouble(line[header[1]]);
                            if (region == null || region.isWithin_EPSG900913(longitude, latitude)) {
                                points.add(longitude);
                                points.add(latitude);
                                String species = "default";
                                Integer idx = lsidMap.get(species);
                                if (idx == null) {
                                    idx = lsidMap.size();
                                    lsidMap.put(species, idx);
                                }
                                lsidIdx.add(idx);
                                years.add((short) 0);   //default
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
}
