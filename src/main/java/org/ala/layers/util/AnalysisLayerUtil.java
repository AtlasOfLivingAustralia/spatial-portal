/**************************************************************************
 *  Copyright (C) 2010 Atlas of Living Australia
 *  All Rights Reserved.
 *
 *  The contents of this file are subject to the Mozilla Public
 *  License Version 1.1 (the "License"); you may not use this file
 *  except in compliance with the License. You may obtain a copy of
 *  the License at http://www.mozilla.org/MPL/
 *
 *  Software distributed under the License is distributed on an "AS
 *  IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 *  implied. See the License for the specific language governing
 *  rights and limitations under the License.
 ***************************************************************************/
package org.ala.layers.util;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKTReader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ala.layers.client.Client;
import org.ala.layers.dao.FieldDAO;
import org.ala.layers.dao.LayerDAO;
import org.ala.layers.dao.LayerIntersectDAO;
import org.ala.layers.dao.ObjectDAO;
import org.ala.layers.dto.Field;
import org.ala.layers.dto.Layer;
import org.ala.layers.dto.Objects;
import org.ala.layers.intersect.Grid;
import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 * @author Adam
 */
public class AnalysisLayerUtil {

    public static void main(String[] args) {
        System.out.println(
                "prepare one grid file for analysis.\n"
                        + "args[0] = source diva grid filename (without .gri or .grd)\n"
                        + "args[1] = output diva grid filename (without .gri or .grd)\n"
                        + "args[2] = resolution in decimal degrees, e.g. 0.01\n"
                        + "args[3] = path to gdal\n\n"
                        + "prepare all grid files for analysis.\n"
                        + "args[0] = 'all', prepare all layers for analysis\n\n"
                        + "prepare all shape or grid files for analysis.\n"
                        + "args[0] = 'all', operate on all layers\n"
                        + "args[1] = 'shapes' or 'grids' to operate on only grids or shapes\n\n");

        if (args == null || args.length == 0) {
            args = new String[]{"all", "grids"};
        }

        if (args.length == 1 && (args[0].equals("auto") || args[0].equals("all"))) {
            processShapeFiles();
            processGridFiles();
        } else if (args.length == 2 && (args[0].equals("auto") || args[0].equals("all"))) {
            if (args[1].equals("shapes")) {
                processShapeFiles();
            } else {
                processGridFiles();
            }
        } else if (args.length == 4) {
            if (diva2Analysis(args[0], args[1], Double.parseDouble(args[2]), args[3], true)) {
                System.out.println("successful");
            } else {
                System.out.println("unsuccessful");
            }
        }
    }

    static boolean diva2Analysis(String srcFilepath, String dstFilepath, Double resolution, String gdalPath, boolean force) {
        try {
            File sgrd = new File(srcFilepath + ".grd");
            File sgri = new File(srcFilepath + ".gri");
            File dgrd = new File(dstFilepath + ".grd");
            File dgri = new File(dstFilepath + ".gri");
            if (force || !dgrd.exists() || !dgri.exists()
                    || (dgrd.lastModified() < sgrd.lastModified() || dgri.lastModified() < sgri.lastModified())) {
                //get new extents
                Grid g = new Grid(srcFilepath);
                double minx = (g.xmin == ((int) (g.xmin / resolution)) * resolution) ? g.xmin : ((int) (g.xmin / resolution)) * resolution + resolution;
                double maxx = (g.xmax == ((int) (g.xmax / resolution)) * resolution) ? g.xmax : ((int) (g.xmax / resolution)) * resolution;
                double miny = (g.ymin == ((int) (g.ymin / resolution)) * resolution) ? g.ymin : ((int) (g.ymin / resolution)) * resolution + resolution;
                double maxy = (g.ymax == ((int) (g.ymax / resolution)) * resolution) ? g.ymax : ((int) (g.ymax / resolution)) * resolution;

                if (maxx < minx + 2 * resolution) maxx = minx + 2 * resolution;
                if (maxy < miny + 2 * resolution) maxy = miny + 2 * resolution;

                new File(new File(dstFilepath).getParent()).mkdirs();

                if (minx == g.xmin && miny == g.ymin
                        && maxx == g.xmax && maxy == g.ymax
                        && resolution == g.xres && resolution == g.yres) {
                    //copy to target dir if ok
                    fileCopy(srcFilepath + ".gri", dstFilepath + ".gri");
                    fileCopy(srcFilepath + ".grd", dstFilepath + ".grd");
                } else {
                    //diva 2 bil
                    File tmpBil = File.createTempFile("tmpbil", "");
                    if (!Diva2bil.diva2bil(srcFilepath, tmpBil.getPath())) {
                        return false;
                    }

                    //gdalwarp bil to target resolution
                    File tmpxBil = File.createTempFile("tmpxbil", "");
                    if (!gdal_warp(gdalPath, tmpBil.getPath() + ".bil", tmpxBil.getPath() + ".bil", resolution, minx, miny, maxx, maxy, g.nodatavalue)) {
                        return false;
                    }

                    //bil 2 diva
                    if (!Bil2diva.bil2diva(tmpxBil.getPath(), dstFilepath, "")) {
                        return false;
                    }

                    //cleanup
                    //tmpbil, tmpbil + .bil, tmpbil + .hdr
                    //tmpxbil, tmpxbil + .bil, tmpxbil + .hdr
                    deleteFiles(new String[]{tmpBil.getPath(), tmpBil.getPath() + ".bil", tmpBil.getPath() + ".hdr",
                            tmpxBil.getPath(), tmpxBil.getPath() + ".bil", tmpxBil.getPath() + ".hdr", tmpxBil.getPath() + ".bil.aux.xml"});
                }
            }

            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private static boolean gdal_warp(String gdalPath, String srcFilename, String dstFilename, double resolution, double minx, double miny, double maxx, double maxy, double nodatavalue) {
        Runtime runtime = Runtime.getRuntime();
        try {

            System.out.println("Got gdal_path: " + gdalPath);

            //gdalwarp -te 109.51 -44.37 157.28 -8.19 -tr 0.01 -0.01
            //-s_srs '" + edlconfig.s_srs + "' -t_srs '" + edlconfig.t_srs + "'
            //-of EHdr -srcnodata -9999 -dstnodata -9999
            String base_command = gdalPath + "gdalwarp -te " + minx + " " + miny + " " + maxx + " " + maxy
                    + " -dstnodata " + String.valueOf(nodatavalue)
                    + " -tr " + resolution + " " + resolution + " -of EHdr ";

            String command = base_command + srcFilename + " " + dstFilename;

            System.out.println("Exec'ing " + command);
            Process proc = runtime.exec(command);

            System.out.println("Setting up output stream readers");
            InputStreamReader isr = new InputStreamReader(proc.getInputStream());
            InputStreamReader eisr = new InputStreamReader(proc.getErrorStream());
            BufferedReader br = new BufferedReader(isr);
            BufferedReader ebr = new BufferedReader(eisr);
            String line;

            System.out.printf("Output of running %s is:", command);

            while ((line = br.readLine()) != null) {
                System.out.println(line);
            }

            while ((line = ebr.readLine()) != null) {
                System.out.println(line);
            }

            int exitVal = proc.waitFor();

            System.out.println(exitVal);

            if (exitVal == 0) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
        return false;
    }

    private static void deleteFiles(String[] filesToDelete) {
        for (String s : filesToDelete) {
            try {
                new File(s).delete();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void fileCopy(String src, String dst) {
        try {
            FileInputStream fis = new FileInputStream(src);
            FileOutputStream fos = new FileOutputStream(dst);
            byte[] buf = new byte[1024 * 1024];
            int len;
            while ((len = fis.read(buf)) > 0) {
                fos.write(buf, 0, len);
            }
            fis.close();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void processShapeFiles() {
        LayerIntersectDAO liDao = Client.getLayerIntersectDao();
        LayerDAO layerDao = Client.getLayerDao();
        FieldDAO fieldDao = Client.getFieldDao();

        List<Double> resolutions = liDao.getConfig().getAnalysisResolutions();
        List<Field> fields = fieldDao.getFields();

        for (Field f : fields) {
            try {
                if (f.isAnalysis() && f.getType().equals("c")) {
                    System.out.println("processing: " + f.getId());

                    Layer l = layerDao.getLayerById(Integer.parseInt(f.getSpid()));

                    //create tmp shape file and lookup file name
                    File tmpShp = File.createTempFile("tmpshp", "");

                    for (Double d : resolutions) {
                        if (shp2Analysis(liDao.getConfig().getLayerFilesPath() + l.getPath_orig(),
                                tmpShp.getPath(),
                                f.getId(),
                                liDao.getConfig().getAnalysisLayerFilesPath() + d + File.separator + f.getId(),
                                d,
                                liDao.getConfig().getGdalPath(),
                                false)) {

                            System.out.println("successful for: " + f.getId() + " @ " + d);
                        } else {
                            System.out.println("unsuccessful for: " + f.getId() + " @ " + d);
                        }
                    }

                    //delete shape and lookup tmp files
                    deleteFiles(new String[]{tmpShp.getPath(), tmpShp.getPath() + ".shp", tmpShp.getPath() + ".shx"
                            , tmpShp.getPath() + ".dbf", tmpShp.getPath() + ".fix", tmpShp.getPath() + ".qix"
                            , tmpShp.getPath() + ".prj", tmpShp.getPath() + ".txt"});
                }
            } catch (Exception e) {
                System.out.println("Error processing: " + f.getId());
                e.printStackTrace();
            }
        }

    }

    private static void processGridFiles() {
        LayerIntersectDAO liDao = Client.getLayerIntersectDao();
        LayerDAO layerDao = Client.getLayerDao();
        FieldDAO fieldDao = Client.getFieldDao();

        List<Double> resolutions = liDao.getConfig().getAnalysisResolutions();
        List<Field> fields = fieldDao.getFields();

        for (Field f : fields) {
            try {
                if (f.isAnalysis() && (f.getType().equals("e") || f.getType().equals("a") || f.getType().equals("b"))) {
                    Layer l = layerDao.getLayerById(Integer.parseInt(f.getSpid()));

                    //determine best resolution
                    Grid g = new Grid(liDao.getConfig().getLayerFilesPath() + l.getPath_orig());
                    double minRes = Math.min(g.xres, g.yres);
                    int i = 0;
                    for (; i < resolutions.size(); i++) {
                        if (resolutions.get(i) == minRes) {
                            break;
                        } else if (resolutions.get(i) > minRes) {
                            if (i > 0) i--;
                            break;
                        }
                    }
                    while (i < resolutions.size()) {
                        if (resolutions.get(i) >= minRes) {
                            System.out.println("processing: " + l.getPath_orig());
                            if (diva2Analysis(liDao.getConfig().getLayerFilesPath() + l.getPath_orig(),
                                    liDao.getConfig().getAnalysisLayerFilesPath() + resolutions.get(i) + File.separator + f.getId(),
                                    resolutions.get(i),
                                    liDao.getConfig().getGdalPath(),
                                    false)) {

                                //copy the contextual value lookup if required
                                if (f.getType().equals("a") || f.getType().equals("b")) {
                                    copyFile(liDao.getConfig().getLayerFilesPath() + l.getPath_orig() + ".txt",
                                            liDao.getConfig().getAnalysisLayerFilesPath() + resolutions.get(i) + File.separator + f.getId() + ".txt");
                                }

                                System.out.println("successful for: " + f.getId() + " @ " + resolutions.get(i));
                            } else {
                                System.out.println("unsuccessful for: " + f.getId() + " @ " + resolutions.get(i));
                            }
                        }
                        i++;
                    }
                }
            } catch (Exception e) {
                System.out.println("error processing: " + f.getId());
                e.printStackTrace();
            }
        }
    }

    static boolean shp2Analysis(String srcOrigFilepath, String srcFilepath, String fieldId, String dstFilepath, Double resolution, String gdalPath, boolean force) {
        try {
            File sshp = new File(srcOrigFilepath + ".shp");
            File tmpShp = new File(srcFilepath + ".shp");
            File dgrd = new File(dstFilepath + ".grd");
            File dgri = new File(dstFilepath + ".gri");
            if (force || !dgrd.exists() || !dgri.exists()
                    || (dgrd.lastModified() < sshp.lastModified() || dgri.lastModified() < sshp.lastModified())) {

                new File(new File(dstFilepath).getParent()).mkdirs();

                //create tmp shape file if it does not exist
                if (!tmpShp.exists()) {
                    if (!fieldToShapeFile(fieldId, srcFilepath)) {
                        return false;
                    }
                }

                //determine the appropriate extents
                FileDataStore store = FileDataStoreFinder.getDataStore(tmpShp);
                ReferencedEnvelope re = store.getFeatureSource().getBounds();
                double minx = (re.getMinX() == ((int) (re.getMinX() / resolution)) * resolution) ? re.getMinX() : ((int) (re.getMinX() / resolution)) * resolution + resolution;
                double maxx = (re.getMaxX() == ((int) (re.getMaxX() / resolution)) * resolution) ? re.getMaxX() : ((int) (re.getMaxX() / resolution)) * resolution;
                double miny = (re.getMinY() == ((int) (re.getMinY() / resolution)) * resolution) ? re.getMinY() : ((int) (re.getMinY() / resolution)) * resolution + resolution;
                double maxy = (re.getMaxY() == ((int) (re.getMaxY() / resolution)) * resolution) ? re.getMaxY() : ((int) (re.getMaxY() / resolution)) * resolution;

                if (maxx < minx + 2 * resolution) maxx = minx + 2 * resolution;
                if (maxy < miny + 2 * resolution) maxy = miny + 2 * resolution;

                //shp 2 bil
                File tmpBil = File.createTempFile("tmpbil", "");
                if (!gdal_rasterize(gdalPath, tmpShp.getPath(), tmpBil.getPath() + ".bil", resolution, minx, miny, maxx, maxy)) {
                    return false;
                }

                //bil 2 diva
                if (!Bil2diva.bil2diva(tmpBil.getPath(), dstFilepath, "")) {
                    return false;
                }

                //copy lookup
                copyFile(srcFilepath + ".txt", dstFilepath + ".txt");

                //cleanup
                //tmpbil, tmpbil + .bil, tmpbil + .hdr
                deleteFiles(new String[]{tmpBil.getPath(), tmpBil.getPath() + ".bil", tmpBil.getPath() + ".hdr"});
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private static boolean gdal_rasterize(String gdalPath, String srcFilename, String dstFilename, double resolution, double minx, double miny, double maxx, double maxy) {
        Runtime runtime = Runtime.getRuntime();
        try {

            System.out.println("Got gdal_path: " + gdalPath);

            String layername = new File(srcFilename).getName().replace(".shp", "");

            //gdal_rasterize -ot Int16 -of EHdr -l aus1 -tr 0.01 0.01
            String base_command = gdalPath + "gdal_rasterize -ot Int16 -of EHdr"
                    + " -te " + minx + " " + miny + " " + maxx + " " + maxy
                    + " -l " + layername
                    + " -a id "
                    + " -tr " + resolution + " " + resolution + " ";

            String command = base_command + srcFilename + " " + dstFilename;

            System.out.println("Exec'ing " + command);
            Process proc = runtime.exec(command);

            System.out.println("Setting up output stream readers");
            InputStreamReader isr = new InputStreamReader(proc.getInputStream());
            InputStreamReader eisr = new InputStreamReader(proc.getErrorStream());
            BufferedReader br = new BufferedReader(isr);
            BufferedReader ebr = new BufferedReader(eisr);
            String line;

            System.out.printf("Output of running %s is:", command);

            while ((line = br.readLine()) != null) {
                System.out.println(line);
            }

            while ((line = ebr.readLine()) != null) {
                System.out.println(line);
            }

            int exitVal = proc.waitFor();

            System.out.println(exitVal);

            if (exitVal == 0) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
        return false;
    }

    private static boolean fieldToShapeFile(String fid, String path) {
        boolean ret = true;
        try {
            final SimpleFeatureType TYPE = DataUtilities.createType("tmpshp", "the_geom:MultiPolygon,id:int");

            ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
            Map<String, Serializable> params = new HashMap<String, Serializable>();
            params.put("url", new File(path + ".shp").toURI().toURL());
            params.put("create spatial index", Boolean.FALSE);
            ShapefileDataStore newDataStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
            newDataStore.createSchema(TYPE);
            newDataStore.forceSchemaCRS(DefaultGeographicCRS.WGS84);
            Transaction transaction = new DefaultTransaction("create");
            String typeName = newDataStore.getTypeNames()[0];
            SimpleFeatureSource featureSource = newDataStore.getFeatureSource(typeName);
            SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;
            featureStore.setTransaction(transaction);
            SimpleFeatureCollection collection = FeatureCollections.newCollection();

            FileWriter fw = null;
            try {
                fw = new FileWriter(path + ".txt");
                int count = 1;
                ObjectDAO objectDao = Client.getObjectDao();
                for (Objects o : objectDao.getObjectsById(fid)) {
                    //get WKT
                    String wkt = objectDao.getObjectsGeometryById(o.getPid(), "wkt");
                    WKTReader r = new WKTReader();
                    Geometry geom = r.read(wkt);

                    //create feature
                    SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(TYPE);
                    featureBuilder.add(geom);
                    featureBuilder.add(count);
                    SimpleFeature f = featureBuilder.buildFeature(String.valueOf(count));
                    collection.add(f);

                    //write for lookup file
                    if (count > 1) {
                        fw.write("\n");
                    }
                    fw.write(count + "=" + o.getId());
                    count++;
                }
                featureStore.addFeatures(collection);
                transaction.commit();
                transaction.close();
            } catch (Exception e) {
                e.printStackTrace();
                ret = false;
            } finally {
                if (fw != null) {
                    try {
                        fw.close();
                    } catch (Exception e) {
                        ret = false;
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            ret = false;
            e.printStackTrace();
        }

        return ret;
    }

    private static void copyFile(String src, String dst) {
        BufferedReader br = null;
        FileWriter fw = null;
        try {
            br = new BufferedReader(new FileReader(src));
            fw = new FileWriter(dst);
            char[] buffer = new char[1024];
            int n;
            while ((n = br.read(buffer)) > 0) {
                fw.write(buffer, 0, n);
            }
            br.close();
            fw.close();
        } catch (Exception e) {
            System.out.println("failure to copy: " + src + " to " + dst);
            e.printStackTrace();
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                if (fw != null) {
                    fw.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
