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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.List;
import org.ala.layers.client.Client;
import org.ala.layers.dao.AnalysisLayerDAO;
import org.ala.layers.dao.LayerDAO;
import org.ala.layers.dao.LayerIntersectDAO;
import org.ala.layers.dto.Layer;
import org.ala.layers.intersect.Grid;

/**
 *
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
                + "args[0] = 'all', prepare all layers for analysis\n");

        args = new String[]{"auto"};

        if (args.length == 1 && args[0].equals("auto")) {
            LayerIntersectDAO liDao = Client.getLayerIntersectDao();
            LayerDAO layerDao = Client.getLayerDao();
            AnalysisLayerDAO alDao = Client.getAnalysisLayerDao();

            List<Double> resolutions = liDao.getConfig().getAnalysisResolutions();

            List<Layer> layers = layerDao.getLayersByEnvironment();
            for (Layer l : layers) {
                //determine best resolution
                try {
                    Grid g = new Grid(liDao.getConfig().getLayerFilesPath() + l.getPath_orig());
                    double minRes = Math.min(g.xres, g.yres);
                    int i = 0;
                    for (; i < resolutions.size(); i++) {
                        if (resolutions.get(i) == minRes) {
                            break;
                        } else if (i > 0 && resolutions.get(i) > minRes) {
                            i--;
                            break;
                        }
                    }
                    while (i < resolutions.size()) {
                        System.out.println("processing: " + l.getPath_orig());
                        if (diva2Analysis(liDao.getConfig().getLayerFilesPath() + l.getPath_orig(),
                                liDao.getConfig().getAnalysisLayerFilesPath() + resolutions.get(i) + File.separator + l.getPath_orig(),
                                resolutions.get(i),
                                liDao.getConfig().getGdalPath(),
                                false)) {
                            System.out.println("successful");
                        } else {
                            System.out.println("unsuccessful");
                        }
                        i++;
                    }
                } catch (Exception e) {
                    System.out.println("failed for layer: " + l.getName() + ", " + l.getPath_orig());
                    e.printStackTrace();
                }
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
                    Diva2bil.diva2bil(srcFilepath, tmpBil.getPath());

                    //gdalwarp bil to target resolution
                    File tmpxBil = File.createTempFile("tmpxbil", "");
                    gdal_warp(gdalPath, tmpBil.getPath() + ".bil", tmpxBil.getPath() + ".bil", resolution, minx, miny, maxx, maxy, g.nodatavalue);

                    //bil 2 diva
                    Bil2diva.bil2diva(tmpxBil.getPath(), dstFilepath, "");

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
}
