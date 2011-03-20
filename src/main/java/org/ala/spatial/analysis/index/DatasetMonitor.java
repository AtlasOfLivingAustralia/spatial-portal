/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.analysis.index;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import org.ala.spatial.analysis.service.ShapeIntersectionService;
import org.ala.spatial.util.TabulationSettings;

/**
 * monitor TabulationSettings.occurrences_config_path for new occurrences files
 *
 * Events:
 * - add dataset
 * - remove dataset
 * - update dataset
 * 
 * @author Adam
 */
public class DatasetMonitor extends Thread {

    ArrayList<String> dataset_files;
    File path;
    boolean end = false;

    public DatasetMonitor() {
        TabulationSettings.load();

        setPriority(MIN_PRIORITY);
    }

    /**
     * Get all dataset headers.
     *
     * Auto update, load and enable most recent versions.
     *
     */
    public void initDatasetFiles() {        
        dataset_files = new ArrayList<String>();
        path = new File(TabulationSettings.occurrences_config_path);

        HashMap<String, Dataset> names = new HashMap<String, Dataset>();

        //add DatasetLoadedPoints
        try {
            dataset_files.add("loaded points");
            Dataset d = new DatasetLoadedPoints("loaded points", "loaded points", new Date(0), false, false, false);
            names.put("loaded points", d);
            OccurrencesCollection.datasets.add(d);
        } catch (Exception e) {
            e.printStackTrace();
        }

        for (String f : path.list()) {
            dataset_files.add(path.getPath() + File.separator + f);
            Dataset d = loadDatasetFile(path.getPath() + File.separator + f, false, false, false);

            //identify most recent version of this dataset
            if (d != null) {
                Dataset ds = names.get(d.name);
                if (ds == null) {
                    names.put(d.name, d);
                    ds = d;
                }

                if (ds.date.before(d.date)) {
                    names.put(d.name, d);
                }
            }
        }

        for (Dataset d : names.values()) {
            //keep index up to date
            d.updateOccurrencesIndex(false);
            d.updateSamplingIndex(false);

            ///load index
            d.isEnabled = true;     //cannot load until this dataset is enabled
            if (d.load()) {
                System.out.println("Dataset loaded: " + d.getUniqueName());
            } else {
                System.out.println("Failed Dataset load: " + d.getUniqueName());
            }
        }
    }

    public void run() {
        initDatasetFiles();

        try {
            while (!end) {
                wait(30000);

                //TODO: insert checking
            }
        } catch (Exception e) {
        }
    }

    public void end() {
        end = true;
        this.interrupt();
    }

    /**
     * dataset files contain \n separated records
     *
     * line 1: dataset name
     * line 2: date/time as dd/mm/yyyy hh:mm:ss
     * line 3: path to occurrences
     *
     * @param filename
     * @return loaded Dataset, or null
     */
    private Dataset loadDatasetFile(String filename, boolean forceUpdate, boolean enabled, boolean load) {
        try {
            BufferedReader r = new BufferedReader(new FileReader(filename));

            String[] s = new String[3];
            int i = 0;
            while (i < 3 && (s[i] = r.readLine()) != null) {
                i = i + 1;
            }

            SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss");

            Dataset d = new Dataset(s[2], s[0], df.parse(s[1]), forceUpdate, enabled, load);

            OccurrencesCollection.datasets.add(d);

            r.close();

            return d;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
