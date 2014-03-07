/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.ala.spatial.data;

import au.com.bytecode.opencsv.CSVReader;
import au.org.ala.spatial.util.CommonData;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;

import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.util.HashMap;

/**
 * @author Adam
 */
public class LsidCounts {

    Long[] lft;
    Long[] count;

    public LsidCounts() {
        HttpClient client = new HttpClient();

        CSVReader csv = null;
        try {
            String url = CommonData.biocacheServer
                    + "/webportal/legend?cm=lft&q="
                    + URLEncoder.encode("geospatial_kosher:*", "UTF-8")
                    + CommonData.biocacheQc;
            System.out.println(url);
            GetMethod get = new GetMethod(url);
            HashMap<Long, Long> map = new HashMap<Long, Long>();
            client.getHttpConnectionManager().getParams().setSoTimeout(30000);

            int result = client.executeMethod(get);

            csv = new CSVReader(new InputStreamReader(get.getResponseBodyAsStream()));

            String[] row;
            while ((row = csv.readNext()) != null) {
                //parse name and count
                try {
                    long name = Long.parseLong(row[0]);
                    long count = Long.parseLong(row[4]);
                    map.put(name, count);
                } catch (Exception e) {
                }
            }

            //sort keys
            lft = new Long[map.size()];
            map.keySet().toArray(lft);
            java.util.Arrays.sort(lft);

            //get sorted values
            count = new Long[map.size()];
            for (int i = 0; i < lft.length; i++) {
                count[i] = map.get(lft[i]);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (csv != null) {
                    csv.close();
                }
            } catch (Exception e) {
            }
        }
    }

    public long getCount(long left, long right) {
        if (lft == null) {
            return 0;
        }
        int pos = java.util.Arrays.binarySearch(lft, left);
        if (pos < 0) {
            pos = -1 * pos - 1;
        }

        long sum = 0;
        while (lft[pos] < right) {
            sum += count[pos++];
        }
        return sum;
    }

    public int getSize() {
        return (lft == null) ? 0 : lft.length;
    }
}
