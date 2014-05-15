package org.ala.layers.util;

import com.sun.tools.javac.util.Paths;
import org.ala.layers.client.Client;
import org.ala.layers.dto.Field;
import org.ala.layers.dto.Layer;
import org.ala.layers.grid.GridCacheReader;
import org.ala.layers.intersect.Grid;
import org.ala.layers.intersect.IntersectConfig;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.List;
import java.util.Vector;

/**
 * Created by a on 15/05/2014.
 */
public class LayerTests {
    private static Logger logger = Logger.getLogger(LayerTests.class);

    /* test database and filesystem files used by layers-store
    *
    * outputs errors and warnings
    *
    */
    public static void main(String[] args) {
        //init
        Client.getLayerIntersectDao().getConfig().getIntersectionFile(null);

        //tests
        TestLayers();
//        TestFields();
//        TestObjects();
//        TestDistributions();
//        TestDistributionData();
//        TestDistributionShapes();
//        TestObjNames();
    }

    private static void testError(boolean expectTrue, String errorMsg) {
        if (!expectTrue) {
            System.out.println("ERROR> " + errorMsg);
        }
    }

    private static void testWarning(boolean expectTrue, String errorMsg) {
        if (!expectTrue) {
            System.out.println("WARN> " + errorMsg);
        }
    }

    private static void TestLayers() {
        List<Layer> layers = Client.getLayerDao().getLayersForAdmin();

        //test layers
        for(Layer l : layers) {
            TestLayer(l);
        }
    }

    private static void msg(String msg) {
        System.out.println("*************> " + msg);
    }

    private static void TestLayer(Layer l) {
        String id = ", id=" + l.getId() + ", ";

        msg("Start testing layersdb.layers:" + id + " name:" + l.getName());

        //mandatory fields
        testError(l.getId() != null, "layerdb.layers: record found with null id. (some tests skipped)");
        testError(l.getName() != null, "layersdb.layers:" + id + "missing name.");
        testError(l.getDisplayname() != null, "layersdb.layers:" + id + "missing displayname.");
        testWarning(l.getDescription() != null, "layersdb.layers:" + id + "missing description.");
        testError(l.getType() != null && (l.getType().equals("Environmental") || l.getType().equals("Contextual")),
                "layersdb.layers:" + id + "type is not 'Environmental' or 'Contextual'. (some tests skipped)");
        testError(l.getMaxlatitude() != null && l.getMaxlatitude() >= -90 && l.getMaxlatitude() <= 90 && (l.getMinlatitude() == null || l.getMaxlatitude() > l.getMinlatitude()),
                "layersdb.layers:" + id + "maxlatitude is invalid.");
        testError(l.getMinlatitude() != null && l.getMinlatitude() >= -90 && l.getMinlatitude() <= 90 && (l.getMaxlatitude() == null || l.getMaxlatitude() > l.getMinlatitude()),
                "layersdb.layers:" + id + "minlatitude is invalid.");
        testError(l.getMaxlongitude() != null && l.getMaxlongitude() >= -180 && l.getMaxlongitude() <= 180 && (l.getMinlongitude() == null || l.getMaxlongitude() > l.getMinlongitude()),
                "layersdb.layers:" + id + "maxlongitude is invalid.");
        testError(l.getMinlongitude() != null && l.getMinlongitude() >= -180 && l.getMinlongitude() <= 180 && (l.getMaxlongitude() == null || l.getMaxlongitude() > l.getMinlongitude()),
                "layersdb.layers:" + id + "minlongitude is invalid.");
        testError(l.getDisplaypath() != null && getUrlResponseCode(l.getDisplaypath()) == 400,
                "layersdb.layers:" + id + "computed displaypath: '" + l.getDisplaypath() + "' is invalid.");
        testError(l.getPath_orig() != null && isValidFilePrefix(l.getPath_orig()),
                "layersdb.layers:" + id + "path_orig does not exist. (some tests skipped)");
        if(l.getId() != null) {
            testError(l.getUid() != null && l.getUid().equals(String.valueOf(l.getId())),
                    "layersdb.layers:" + id + "uid is not the same as the id.");
        }

        //domain
        testError(l.getdomain() != null && (l.getdomain().equals("Marine") || l.getdomain().equals("Terrestrial") || l.getdomain().equals("Terrestrial,Marine") || l.getdomain().equals("Marine,Terrestrial")),
                "layersdb.layers:" + id + "domain must be 'Marine' or 'Terrestrial' or 'Terrestrial,Marine'");

        testWarning(l.getKeywords() != null && l.getKeywords().length() > 0,
                "layersdb.layers:" + id + "no keywords.");

        testError(l.getClassification1() != null && l.getClassification1().length() > 0,
                "layersdb.layers:" + id + "no classification1.");

        testWarning(l.getMetadatapath() != null && l.getMetadatapath().length() > 0,
                "layersdb.layers:" + id + "no metadatapath.");
        testWarning(l.getLicence_level() != null && l.getLicence_level().length() > 0,
                "layersdb.layers:" + id + "no licence level.");

        //environmental only fields
        if (l.getType() != null && l.getType().equals("Environmental")) {
            testError(l.getScale() != null && l.getScale().length() > 0,
                    "layersdb.layers:" + id + "missing scale.");

            testError(isDouble(l.getEnvironmentalvaluemax()) && (!isDouble(l.getEnvironmentalvaluemin()) || Double.parseDouble(l.getEnvironmentalvaluemin()) > Double.parseDouble(l.getEnvironmentalvaluemax())),
                    "layersdb.layers:" + id + "invalid environmentalvaluemax.");

            testError(isDouble(l.getEnvironmentalvaluemin()) && (!isDouble(l.getEnvironmentalvaluemax()) || Double.parseDouble(l.getEnvironmentalvaluemin()) > Double.parseDouble(l.getEnvironmentalvaluemax())),
                    "layersdb.layers:" + id + "invalid environmentalvaluemin.");

            testError(l.getEnvironmentalvalueunits() != null && l.getEnvironmentalvalueunits().length() > 0,
                    "layersdb.layers:" + id + "missing environmentalvaluemax.");
        }

        //does it have an associated field?
        boolean valid = false;
        Field f = null;
        try {
            f = Client.getFieldDao().getFieldById(Client.getLayerIntersectDao().getConfig().getIntersectionFile(l.getName()).getFieldId());
            valid = (f != null);
        } catch (Exception e) {}
        testError(valid, "layersdb.layers:" + id + "missing an associated field in the fields table");

        //test analysis grids, only need one valid grid file
        valid = false;
        if (f != null) {
            String r = "";
            for (Double d : IntersectConfig.getAnalysisResolutions()) {
                try {
                    if(isValidFilePrefix(IntersectConfig.getAnalysisLayerFilesPath() + d + File.separator + f.getId())){
                        valid = true;
                        break;
                    }
                } catch (Exception e) {
                }
                r +=  d + ", ";
            }
            testError(valid,"layersdb.layers:" + id + "error using analysis transformed grid file at ANALYSIS_LAYER_FILES_PATH: " + IntersectConfig.getAnalysisLayerFilesPath()
                    + " for any of the ANALYSIS_RESOLUTIONS: " + r);
        }

        //test grid cache has indexed a field with this layer
        valid = false;
        try {
            GridCacheReader gcr = new GridCacheReader(Client.getLayerIntersectDao().getConfig().getGridCachePath());
            valid = gcr.getFileNames().contains(l.getName());
        } catch (Exception e) {
            logger.error("layersdb.layers:" + id + "initializing grid cache:" + Client.getLayerIntersectDao().getConfig().getGridCachePath(), e);
        }
        testError(valid, "layersdb.layers:" + id + "not in grid cache:" + Client.getLayerIntersectDao().getConfig().getGridCachePath());

        //test file system related files by performing an intersection without an error
        //(already did path_orig test)
        /*
        if(l.getId() != null && l.getPath_orig() != null && l.getType() != null && isValidFilePrefix(l.getPath_orig())) {
            valid = false;
            try {
                Vector v = Client.getLayerIntersectDao().samplingFull(l.getName(), 140, -30);
                valid = (v != null && v.size() == 1);
            } catch (Exception e) {
            }
            testError(valid, "layersdb.layers:" + id + "failed sampling check path_orig files: " + l.getPath_orig());
        }*/
    }

    private static boolean isValidFilePrefix(String filePrefix) {
        String path = filePrefix.substring(0,filePrefix.lastIndexOf(File.separator));
        String prefix = filePrefix.substring(path.length() + 1);

        File f = new File(IntersectConfig.getLayerFilesPath() + path);

        if(f.exists() && f.isDirectory()) {
            for (File c : f.listFiles()) {
                if(c.getName().startsWith(prefix)) {
                    return true;
                }
            }
        }

        return false;
    }

    private static int getUrlResponseCode(String displaypath) {
        try {
            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(displaypath);
            return client.executeMethod(get);
        } catch (Exception e) {
            return -1;
        }
    }

    private static boolean isDouble(String s) {
        try {
            Double d = Double.parseDouble(s);
            return d != null;
        } catch (Exception e) {
        }
        return false;
    }
}
