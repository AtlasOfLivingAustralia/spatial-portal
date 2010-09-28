/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.analysis.index;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.ObjectInputStream;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import org.ala.spatial.util.TabulationSettings;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;

/**
 *
 * @author Adam
 */
public class CommonNamesHarvesting {

    public static void main(String[] args) {
        TabulationSettings.load();

        String[] lsids = getLSIDs();

        LinkedBlockingQueue<String> lbq = new LinkedBlockingQueue<String>();

        for (String s : lsids) {
            lbq.add(s);
        }

        CountDownLatch cdl = new CountDownLatch(lsids.length);

        ConcurrentHashMap<String, String> results = new ConcurrentHashMap<String, String>();

        String url_base = "http://bie.ala.org.au/species/_lsid_.json";

        int threadCount = 30;//TabulationSettings.analysis_threads;

        CommonNamesHarvester[] harvesters = new CommonNamesHarvester[threadCount];

        for (int i = 0; i < harvesters.length; i++) {
            harvesters[i] = new CommonNamesHarvester(lbq, cdl, results, url_base);
            harvesters[i].start();
        }

        try {
            cdl.await();
        } catch (Exception e) {
            e.printStackTrace();
        }

        for(int i=0;i<harvesters.length;i++){
            harvesters[i].interrupt();
        }

        System.out.println("exporting lsid vs common names");

        try {

            FileWriter fw = new FileWriter(
                    TabulationSettings.index_path + "commonnames.txt");

            for (Entry<String, String> es : results.entrySet()) {
                String k = es.getKey();
                String v = es.getValue();
                if(k != null && k.length() > 2 && v != null && v.length() > 2
                        && k.indexOf(v) > 2){
                    k = k.substring(0, k.indexOf(v) - 1).trim();
                   fw.append(k).append('\t').append(v).append("\r\n");
                }
            }

            fw.close();
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }

    }

    static String[] getLSIDs() {
        try {
            FileInputStream fis = new FileInputStream(
                    TabulationSettings.index_path
                    + OccurrencesIndex.SORTED_CONCEPTID_NAMES);
            BufferedInputStream bis = new BufferedInputStream(fis);
            ObjectInputStream ois = new ObjectInputStream(bis);
            String[] lsids = (String[]) ois.readObject();

            ois.close();
            return lsids;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}

class CommonNamesHarvester extends Thread {

    LinkedBlockingQueue<String> lbq;
    CountDownLatch cdl;
    ConcurrentHashMap<String, String> results;
    String base_url;
    FileWriter outputFilePiece;

    CommonNamesHarvester(LinkedBlockingQueue<String> lbq_, CountDownLatch cdl_, ConcurrentHashMap<String, String> results_, String base_url_) {
        lbq = lbq_;
        cdl = cdl_;
        results = results_;
        base_url = base_url_;

        try {
            File tmpFile = File.createTempFile("commonnames", ".txt");
            outputFilePiece = new FileWriter(tmpFile);
            System.out.println(tmpFile.getPath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            while (true) {
                String lsid = lbq.take();

                nextCommonName(lsid);

                this.outputFilePiece.flush();

                if (lbq.size() % 100 == 0) {
                    System.out.println(lbq.size());
                }
            }
        } catch(InterruptedException e){
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("closing harvester");
    }

    private void nextCommonName(String lsid) {

        int count = 0;

        try {
            HttpClient client = new HttpClient();
            String url = base_url.replace("_lsid_", lsid);
            GetMethod get = new GetMethod(url);
            get.addRequestHeader("Accept", "text/plain");

            client.executeMethod(get);
            String slist = get.getResponseBodyAsString();

            JsonFactory f = new JsonFactory();
            JsonParser jp = f.createJsonParser(slist);

            jp.nextToken(); // will return JsonToken.START_OBJECT (verify?)

            while (jp.nextToken() != JsonToken.NOT_AVAILABLE) {
                if (jp.getCurrentToken() == JsonToken.END_OBJECT) {
                    continue;
                }
                String fieldname = jp.getCurrentName();
                JsonToken jt = jp.nextToken(); // move to value, or START_OBJECT/START_ARRAY

                if (fieldname != null && "commonNames".equals(fieldname)) { // commonNames
                    jt = jp.nextToken();
                    while (jt != JsonToken.END_ARRAY) {
                        fieldname = jp.getCurrentName();
                        jt = jp.nextToken(); // move to value, or START_OBJECT/START_ARRAY

                        if (fieldname != null && "nameString".equals(fieldname)) {
                            String commonName = jp.getText().trim();

                            if (!commonName.equals("preferred")) {
                                results.put(lsid + " " + commonName, commonName);

                                //output to piece file as well
                                outputFilePiece.append(lsid).append("\t").append(commonName).append("\r\n");
                                count++;
                            }
                        }
                    }
                    break;
                }

                if (jt == JsonToken.START_ARRAY) {
                    while (jp.nextToken() != JsonToken.END_ARRAY);
                }
            }

            cdl.countDown();
            
        } catch (Exception e) {
            //e.printStackTrace();
            System.out.println("retry:" + lsid);

            //put it back if there was an error
            try {
                lbq.put(lsid);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        if (count == 0) {
            results.put(lsid, "");
        }
    }
}
