package org.ala.spatial.analysis.scatterplot;

import com.thoughtworks.xstream.persistence.FilePersistenceStrategy;
import com.thoughtworks.xstream.persistence.PersistenceStrategy;
import com.thoughtworks.xstream.persistence.XmlArrayList;
import org.ala.spatial.util.AlaspatialProperties;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.*;

/**
 * Created by a on 26/03/2014.
 */
public class ScatterplotStore {

    /**
     * @author Adam
     */

    private static Logger logger = Logger.getLogger(ScatterplotStore.class);


    static final int MAX_SIZE = 500;
    final static String TEMP_FILE_PATH = System.getProperty("java.io.tmpdir");

    /**
     * store for ID and {last access time (Long) , cluster (Vector) }
     */
    static HashMap<String, Object[]> selections = new HashMap<String, Object[]>();

    public static void addData(String id, Scatterplot data) {
        Object[] o = selections.get(id);
        if (o == null) {
            freeMem();
            o = new Object[2];
        }
        o[0] = Long.valueOf(System.currentTimeMillis());
        o[1] = data;
        store(id, data);

        selections.put(id, o);
    }

    public static Scatterplot getData(String id) {
        Object[] o = selections.get(id);
        if (o == null) {
            o = retrieve(id);
        }
        if (o != null) {
            o[0] = Long.valueOf(System.currentTimeMillis());
            selections.put(id, o);
            return (Scatterplot) o[1];
        }

        return null;
    }

    private static void freeMem() {
        if (selections.size() > MAX_SIZE) {
            Long time_min = Long.valueOf(0);
            Long time_max = Long.valueOf(0);

            for (Map.Entry<String, Object[]> e : selections.entrySet()) {
                if (time_min == 0 || (Long) e.getValue()[0] < time_min) {
                    time_min = (Long) e.getValue()[0];
                }
                if (time_max == 0 || (Long) e.getValue()[0] < time_max) {
                    time_max = (Long) e.getValue()[0];
                }
            }
            Long time_mid = (time_max - time_min) / 2 + time_min;
            for (Map.Entry<String, Object[]> e : selections.entrySet()) {
                if ((Long) e.getValue()[0] < time_mid) {
                    selections.remove(e.getKey());
                }
            }
        }
    }

    static void store(String key, Scatterplot o) {
        try {

            String sfld = AlaspatialProperties.getAnalysisWorkingDir() + "scatterplot/" + o.getScatterplotDTO().getId();

            File sessfolder = new File(sfld + "/");
            if (!sessfolder.exists()) {
                sessfolder.mkdirs();
            } else {
                FileUtils.deleteDirectory(sessfolder);
                sessfolder.mkdirs();
            }

            PersistenceStrategy strategy = new FilePersistenceStrategy(new File(sfld));
            List list = new XmlArrayList(strategy);
            list.add(o.getScatterplotDTO()); //save ScatterplotDTO
            list.add(o.getScatterplotStyleDTO()); //save ScatterplotStyleDTO
            list.add(o.getScatterplotDataDTO()); //save ScatterplotDataDTO

        } catch (Exception e) {
            logger.error("error saving scatterplot", e);
        }
    }

    static Object[] retrieve(String key) {
        try {
            String sfld = AlaspatialProperties.getAnalysisWorkingDir() + "scatterplot/" + key;
            File f = new File(sfld);

            if (!f.exists()) {
                return null;
            }

            PersistenceStrategy strategy = new FilePersistenceStrategy(f);
            List list = new XmlArrayList(strategy);

            Iterator i = list.listIterator();

            Scatterplot scat = new Scatterplot((ScatterplotDTO) list.get(0), (ScatterplotStyleDTO) list.get(1),
                    (list.size() == 3) ? (ScatterplotDataDTO) list.get(2) : null);

            Object[] o = new Object[2];
            o[1] = scat;

            return o;

        } catch (Exception e) {
            logger.error("error reading scatterplot from disk", e);
        }
        return null;
    }
}

