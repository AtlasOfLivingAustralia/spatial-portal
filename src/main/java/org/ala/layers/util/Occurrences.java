package org.ala.layers.util;

import au.com.bytecode.opencsv.CSVReader;
import org.ala.layers.intersect.SimpleRegion;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import java.util.zip.GZIPInputStream;

/**
 * Created by a on 10/03/2014.
 */
public class Occurrences {
    private static Logger logger = Logger.getLogger(Occurrences.class);

    /**
     * get csv with occurrence data for the specified fields.
     *
     * @param q
     * @param server
     * @param fields
     * @return
     */
    public static String getOccurrences(String q, String server, String fields) throws IOException {
        int pageSize = 1000000;

        int start = 0;

        StringBuilder output = new StringBuilder();

        while (true && start < 300000000) {
            String url = server + "/webportal/occurrences.gz?q=" + q + "&pageSize=" + pageSize + "&start=" + start + "&fl=" + fields;
            logger.info("retrieving from biocache : " + url);
            int tryCount = 0;
            InputStream is = null;
            String csv = null;
            int maxTrys = 4;
            while (tryCount < maxTrys && csv == null) {
                tryCount++;
                try {
                    is = getUrlStream(url);
                    csv = IOUtils.toString(new GZIPInputStream(is));
                } catch (Exception e) {
                    logger.warn("failed try " + tryCount + " of " + maxTrys + ": " + url, e);
                }
            }

            if (csv == null) {
                throw new IOException("failed to get records from biocache.");
            }

            output.append(csv);

            int currentCount = 0;
            int i = -1;
            while ((i = csv.indexOf("\n", i + 1)) > 0) {
                currentCount++;
            }

            if (currentCount == 0 || currentCount < pageSize) {
                break;
            }
        }

        return output.toString();
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
