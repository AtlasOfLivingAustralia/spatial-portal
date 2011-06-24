/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.analysis.index;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.OutputStreamWriter;
import java.util.HashMap;
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

    static final int RETRY_MAXIMUM = 3;
    static boolean save_json = false;
    static HashMap<String, Integer> retry_count;

    public static boolean isSavingJson() {
        return save_json;
    }

    public static void main(String[] args) {
        TabulationSettings.load();

        retry_count = new HashMap<String, Integer>();

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
            harvesters[i] = new CommonNamesHarvester(lbq, cdl, results, url_base, retry_count);
            harvesters[i].start();
        }

        try {
            cdl.await();
        } catch (Exception e) {
            e.printStackTrace();
        }

        for (int i = 0; i < harvesters.length; i++) {
            harvesters[i].interrupt();
        }

        System.out.println("exporting lsid vs common names");

        try {
            OutputStreamWriter fw = new OutputStreamWriter(new FileOutputStream(
                    TabulationSettings.index_path
                    + "commonnames.txt"), "UTF-8");

            for (Entry<String, String> es : results.entrySet()) {
                String k = es.getKey();
                String v = es.getValue();
                if (k != null && k.length() > 2 && v != null && v.length() > 2
                        && k.indexOf(v) > 2) {
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

            if (lsids != null) {
                System.out.println("got lsids: " + lsids.length);
            } else {
                System.out.println("lsids empty");
            }
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
    OutputStreamWriter outputFilePiece;
    HashMap<String, Integer> retry_count;

    CommonNamesHarvester(LinkedBlockingQueue<String> lbq_, CountDownLatch cdl_, ConcurrentHashMap<String, String> results_, String base_url_, HashMap<String, Integer> retry_count_) {
        lbq = lbq_;
        cdl = cdl_;
        results = results_;
        base_url = base_url_;
        retry_count = retry_count_;

        try {
            File tmpFile = File.createTempFile("commonnames", ".txt");
            outputFilePiece = new OutputStreamWriter(new FileOutputStream(
                    tmpFile), "UTF-8");
            System.out.println(tmpFile.getPath());
        } catch (Exception e) {
            e.printStackTrace();
        }

        setPriority(MIN_PRIORITY);
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
        } catch (InterruptedException e) {
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            outputFilePiece.close();
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

            if (CommonNamesHarvesting.isSavingJson()) {
                save(lsid, slist);
            }

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

                            jt = jp.nextToken(); // move to next

                            //if (!commonName.equals("preferred")) {
                            results.put(lsid + " " + commonName, commonName);

                            //output to piece file as well
                            outputFilePiece.append(lsid).append("\t").append(commonName).append("\r\n");
                            count++;

                            System.out.println(lsid + " " + commonName);
                            //}
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
            //only retry if under retry_count limit
            synchronized (retry_count) {
                //increment count
                Integer c = retry_count.get(lsid);
                if (c == null) {
                    c = new Integer(1);
                } else {
                    c = c + 1;
                }
                retry_count.put(lsid, c);

                if (c >= CommonNamesHarvesting.RETRY_MAXIMUM) {
                    //report failure
                    System.out.println("retry limit reached for:" + lsid);
                } else {
                    //put it back into the queue
                    try {
                        lbq.put(lsid);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
        if (count == 0) {
            results.put(lsid, "");
        }
    }

    void save(String lsid, String json) {
        try {
            File tmpFile = File.createTempFile("json" + lsid.replace(":", "_"), ".txt");
            OutputStreamWriter fw = new OutputStreamWriter(new FileOutputStream(
                    tmpFile), "UTF-8");
            fw.append(json);
            fw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
