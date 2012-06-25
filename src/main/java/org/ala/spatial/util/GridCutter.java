/**
 * ************************************************************************
 * Copyright (C) 2010 Atlas of Living Australia All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 * *************************************************************************
 */
package org.ala.spatial.util;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.TreeMap;
import org.ala.layers.client.Client;
import org.ala.layers.intersect.Grid;
import org.ala.layers.intersect.SimpleRegion;
import org.ala.layers.util.SpatialUtil;
import org.ala.spatial.analysis.index.LayerFilter;

/**
 * Class for region cutting test data grids
 *
 * @author adam
 */
public class GridCutter {

    public static ArrayList<Object> loadCutGridsForAloc(File[] files, String extentsFilename, int pieces, AnalysisJob job) {
        ArrayList<Object> data = new ArrayList<Object>();

        if (job != null) {
            job.setProgress(0);
        }

        //determine outer bounds of layers
        double xmin = Double.MAX_VALUE;
        double ymin = Double.MAX_VALUE;
        double xmax = Double.MAX_VALUE * -1;
        double ymax = Double.MAX_VALUE * -1;
        double xres = 0.01;
        double yres = 0.01;
        for (File f : files) {
            String gridFilename = f.getPath().substring(0, f.getPath().length() - 4);
            Grid g = new Grid(gridFilename);
            xres = g.xres;
            yres = g.xres;
            if (xmin > g.xmin) {
                xmin = g.xmin;
            }
            if (xmax < g.xmax) {
                xmax = g.xmax;
            }
            if (ymin > g.ymin) {
                ymin = g.ymin;
            }
            if (ymax < g.ymax) {
                ymax = g.ymax;
            }
        }

        if (files.length < 2) {
            if (job != null) {
                job.setCurrentState(AnalysisJob.FAILED);
                job.log("Fewer than two layers with postive range.");
            } else {
                SpatialLogger.log("Fewer than two layers with postive range.");
            }
            return null;
        }


        //determine range and width's
        double xrange = xmax - xmin;
        double yrange = ymax - ymin;
        int width = (int) Math.ceil(xrange / xres);
        int height = (int) Math.ceil(yrange / yres);

        //write extents into a file now
        if (extentsFilename != null) {
            try {
                FileWriter fw = new FileWriter(extentsFilename);
                fw.append(String.valueOf(width)).append("\n");
                fw.append(String.valueOf(height)).append("\n");
                fw.append(String.valueOf(xmin)).append("\n");
                fw.append(String.valueOf(ymin)).append("\n");
                fw.append(String.valueOf(xmax)).append("\n");
                fw.append(String.valueOf(ymax));
                fw.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (job != null) {
            job.setProgress(0.1, "exported extents");
        }

        //make cells list for outer bounds
        int th = height;
        int tw = width;
        int tp = 0;
        int[][] cells = new int[tw * th][2];
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                cells[tp][0] = j;
                cells[tp][1] = i;
                tp++;
            }
        }

        if (job != null) {
            job.setProgress(0.2, "determined target cells");
        }

        if (job != null) {
            job.log("Cut cells count: " + cells.length);
        } else {
            System.out.println("Cut cells count: " + cells.length);
        }

        //transform cells numbers to long/lat numbers
        double[][] points = new double[cells.length][2];
        for (int i = 0; i < cells.length; i++) {
            points[i][0] = xmin + cells[i][0] * xres;
            points[i][1] = ymin + cells[i][1] * yres;
        }

        //initialize data structure to hold everything
        // each data piece: row1[col1, col2, ...] row2[col1, col2, ...] row3...
        int remainingLength = cells.length;
        int step = (int) Math.floor(remainingLength / (double) pieces);
        for (int i = 0; i < pieces; i++) {
            if (i == pieces - 1) {
                data.add(new float[remainingLength * files.length]);
            } else {
                data.add(new float[step * files.length]);
                remainingLength -= step;
            }
        }

        //iterate for layers
        double[] layerExtents = new double[files.length * 2];
        for (int j = 0; j < files.length; j++) {
            String gridFilename = files[j].getPath().substring(0, files[j].getPath().length() - 4);
            Grid g = new Grid(gridFilename);
            float[] v = g.getValues2(points);

            //row range standardization
            float minv = Float.MAX_VALUE;
            float maxv = Float.MAX_VALUE * -1;
            for (int i = 0; i < v.length; i++) {
                if (v[i] < minv) {
                    minv = v[i];
                }
                if (v[i] > maxv) {
                    maxv = v[i];
                }
            }
            float range = maxv - minv;
            if (range > 0) {
                for (int i = 0; i < v.length; i++) {
                    v[i] = (v[i] - minv) / range;
                }
            } else {
                for (int i = 0; i < v.length; i++) {
                    v[i] = 0;
                }
            }
            layerExtents[j * 2] = minv;
            layerExtents[j * 2 + 1] = maxv;

            //iterate for pieces
            for (int i = 0; i < pieces; i++) {
                float[] d = (float[]) data.get(i);
                for (int k = j, n = i * step; k < d.length; k += files.length, n++) {
                    d[k] = v[n];
                }
            }

            if (job != null) {
                job.setProgress(0.2 + j / (double) files.length * 7 / 10.0, "opened grid: " + files[j].getName());
            }
        }

        if (job != null) {
            job.log("finished opening grids");
        }

        //remove null rows from data and cells
        int newCellPos = 0;
        int currentCellPos = 0;
        for (int i = 0; i < pieces; i++) {
            float[] d = (float[]) data.get(i);
            int newPos = 0;
            for (int k = 0; k < d.length; k += files.length) {
                int nMissing = 0;
                for (int j = 0; j < files.length; j++) {
                    if (Float.isNaN(d[k + j])) {
                        nMissing++;
                    }
                }
                //if (nMissing < files.length) {
                if (nMissing == 0) {
                    if (newPos < k) {
                        for (int j = 0; j < files.length; j++) {
                            d[newPos + j] = d[k + j];
                        }
                    }
                    newPos += files.length;
                    if (newCellPos < currentCellPos) {
                        cells[newCellPos][0] = cells[currentCellPos][0];
                        cells[newCellPos][1] = cells[currentCellPos][1];
                    }
                    newCellPos++;
                }
                currentCellPos++;
            }
            if (newPos < d.length) {
                d = java.util.Arrays.copyOf(d, newPos);
                data.set(i, d);
            }
        }

        //remove zero length data pieces
        for (int i = pieces - 1; i >= 0; i--) {
            float[] d = (float[]) data.get(i);
            if (d.length == 0) {
                data.remove(i);
            }
        }

        //add cells reference to output
        data.add(cells);

        //add extents to output
        double[] extents = new double[6 + layerExtents.length];
        extents[0] = width;
        extents[1] = height;
        extents[2] = xmin;
        extents[3] = ymin;
        extents[4] = xmax;
        extents[5] = ymax;
        for (int i = 0; i < layerExtents.length; i++) {
            extents[6 + i] = layerExtents[i];
        }
        data.add(extents);

        if (job != null) {
            job.setProgress(1, "cleaned data");
        }

        return data;
    }

    /**
     * exports a list of layers cut against a region
     *
     * Cut layer files generated are input layers with grid cells outside of
     * region set as missing.
     *
     * @param layers list of layer fieldIds to be cut as String[].
     * @param resolution target resolution as String
     * @param region null or region to cut against as SimpleRegion. Cannot be
     * used with envelopes.
     * @param envelopes nul or region to cut against as LayerFilter[]. Cannot be
     * used with region.
     * @param extentsFilename output filename and path for writing output
     * extents.
     * @return directory containing the cut grid files.
     */
    public static String cut2(String[] layers, String resolution, SimpleRegion region, LayerFilter[] envelopes, String extentsFilename) {
        //check if resolution needs changing
        resolution = confirmResolution(layers, resolution);

        //get extents for all layers
        double[][] extents = getLayerExtents(resolution, layers[0]);
        for (int i = 1; i < layers.length; i++) {
            extents = internalExtents(extents, getLayerExtents(resolution, layers[i]));
            if (!isValidExtents(extents)) {
                return null;
            }
        }

        //get mask and adjust extents for filter
        byte[][] mask;
        int w = 0, h = 0;
        double res = Double.parseDouble(resolution);
        if (region != null) {
            extents = internalExtents(extents, region.getBoundingBox());

            if (!isValidExtents(extents)) {
                return null;
            }

            h = (int) Math.ceil((extents[1][1] - extents[0][1]) / res);
            w = (int) Math.ceil((extents[1][0] - extents[0][0]) / res);
            mask = getRegionMask(res, extents, w, h, region);
        } else if (envelopes != null) {
            h = (int) Math.ceil((extents[1][1] - extents[0][1]) / res);
            w = (int) Math.ceil((extents[1][0] - extents[0][0]) / res);
            mask = getEnvelopeMaskAndUpdateExtents(resolution, res, extents, h, w, envelopes);
            h = (int) Math.ceil((extents[1][1] - extents[0][1]) / res);
            w = (int) Math.ceil((extents[1][0] - extents[0][0]) / res);
        } else {
            h = (int) Math.ceil((extents[1][1] - extents[0][1]) / res);
            w = (int) Math.ceil((extents[1][0] - extents[0][0]) / res);
            mask = getMask(res, extents, w, h);
        }

        //mkdir in index location
        String newPath = null;
        try {
            newPath = AlaspatialProperties.getAnalysisWorkingDir() + System.currentTimeMillis() + java.io.File.separator;
            File directory = new File(newPath);
            directory.mkdir();
        } catch (Exception e) {
            e.printStackTrace();
        }

        //apply mask
        for (int i = 0; i < layers.length; i++) {
            applyMask(newPath, resolution, extents, w, h, mask, layers[i]);
        }

        //write extents file
        writeExtents(extentsFilename, extents, w, h);

        return newPath;
    }

    static double[][] internalExtents(double[][] e1, double[][] e2) {
        double[][] internalExtents = new double[2][2];

        internalExtents[0][0] = Math.max(e1[0][0], e2[0][0]);
        internalExtents[0][1] = Math.max(e1[0][1], e2[0][1]);
        internalExtents[1][0] = Math.min(e1[1][0], e2[1][0]);
        internalExtents[1][1] = Math.min(e1[1][1], e2[1][1]);

        return internalExtents;
    }

    static boolean isValidExtents(double[][] e) {
        return e[0][0] < e[1][0] && e[0][1] < e[1][1];
    }

    static double[][] getLayerExtents(String resolution, String layer) {
        double[][] extents = new double[2][2];
        Grid g = Grid.getGrid(getLayerPath(resolution, layer));

        extents[0][0] = g.xmin;
        extents[0][1] = g.ymin;
        extents[1][0] = g.xmax;
        extents[1][1] = g.ymax;

        return extents;
    }

    public static String getLayerPath(String resolution, String layer) {
        String field = Layers.getFieldId(layer);

        File file = new File(AlaspatialProperties.getAnalysisLayersDir() + File.separator + resolution + File.separator + field + ".grd");

        //move up a resolution when the file does not exist at the target resolution
        try {
            while (!file.exists()) {
                TreeMap<Double, String> resolutionDirs = new TreeMap<Double, String>();
                for (File dir : new File(AlaspatialProperties.getAnalysisLayersDir()).listFiles()) {
                    if (dir.isDirectory()) {
                        try {
                            resolutionDirs.put(Double.parseDouble(dir.getName()), dir.getName());
                        } catch (Exception e) {
                        }
                    }
                }

                String newResolution = resolutionDirs.higherEntry(Double.parseDouble(resolution)).getValue();

                if (newResolution.equals(resolution)) {
                    break;
                } else {
                    resolution = newResolution;
                    file = new File(AlaspatialProperties.getAnalysisLayersDir() + File.separator + resolution + File.separator + field + ".grd");
                }
            }
        } catch (Exception e) {
        }

        String layerPath = AlaspatialProperties.getAnalysisLayersDir() + File.separator + resolution + File.separator + field;

        if (new File(layerPath + ".grd").exists()) {
            return layerPath;
        } else {
            //look for an analysis layer
            String[] info = Client.getLayerIntersectDao().getConfig().getAnalysisLayerInfo(layer);
            if (info != null) {
                return info[1];
            } else {
                System.out.println("getLayerPath, cannot find for: " + layer + ", " + resolution);
                return null;
            }
        }
    }

    static void applyMask(String dir, String resolution, double[][] extents, int w, int h, byte[][] mask, String layer) {
        //layer output container
        double[] dfiltered = new double[w * h];

        //open grid and get all data
        Grid grid = Grid.getGrid(getLayerPath(resolution, layer));
        float[] d = grid.getGrid(); //get whole layer

        //set all as missing values
        for (int i = 0; i < dfiltered.length; i++) {
            dfiltered[i] = Double.NaN;
        }

        double res = Double.parseDouble(resolution);

        for (int i = 0; i < mask.length; i++) {
            for (int j = 0; j < mask[0].length; j++) {
                if (mask[i][j] > 0) {
                    dfiltered[j + (h - i - 1) * w] = grid.getValues2(new double[][]{{j * res + extents[0][0], i * res + extents[0][1]}})[0];
                }
            }
        }

        grid.writeGrid(dir + layer, dfiltered,
                extents[0][0],
                extents[0][1],
                extents[1][0],
                extents[1][1],
                res, res, h, w);
    }

    static void writeExtents(String filename, double[][] extents, int w, int h) {
        if (filename != null) {
            try {
                FileWriter fw = new FileWriter(filename);
                fw.append(String.valueOf(w)).append("\n");
                fw.append(String.valueOf(h)).append("\n");
                fw.append(String.valueOf(extents[0][0])).append("\n");
                fw.append(String.valueOf(extents[0][1])).append("\n");
                fw.append(String.valueOf(extents[1][0])).append("\n");
                fw.append(String.valueOf(extents[1][1]));
                fw.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Get a region mask.
     *
     * Note: using decimal degree grid, probably should be EPSG900913 grid.
     *
     * @param res resolution as double
     * @param extents extents as double[][] with [0][0]=xmin, [0][1]=ymin,
     * [1][0]=xmax, [1][1]=ymax.
     * @param h height as int.
     * @param w width as int.
     * @param region area for the mask as SimpleRegion.
     * @return
     */
    private static byte[][] getRegionMask(double res, double[][] extents, int w, int h, SimpleRegion region) {
        byte[][] mask = new byte[h][w];

        //can also use region.getOverlapGridCells_EPSG900913
        region.getOverlapGridCells(extents[0][0], extents[0][1], extents[1][0], extents[1][1], w, h, mask);
        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                //double tx = (j + 0.5) * res + extents[0][0];
                //double ty = (i + 0.5) * res + extents[0][1];
                //if (region.isWithin_EPSG900913(tx, ty)) {
                //    mask[i][j] = 1;
                //}
                if (mask[i][j] > 0) {
                    mask[i][j] = 1;
                }
            }
        }
        return mask;
    }

    private static byte[][] getMask(double res, double[][] extents, int w, int h) {
        byte[][] mask = new byte[h][w];
        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                mask[i][j] = 1;
            }
        }
        return mask;
    }

    /**
     * Get a mask, 0=absence, 1=presence, for a given envelope and extents.
     *
     * @param resolution resolution as String.
     * @param res resultions as double.
     * @param extents extents as double[][] with [0][0]=xmin, [0][1]=ymin,
     * [1][0]=xmax, [1][1]=ymax.
     * @param h height as int.
     * @param w width as int.
     * @param envelopes
     * @return mask as byte[][]
     */
    private static byte[][] getEnvelopeMaskAndUpdateExtents(String resolution, double res, double[][] extents, int h, int w, LayerFilter[] envelopes) {
        byte[][] mask = new byte[h][w];

        double[][] points = new double[h * w][2];
        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                points[i + j * w][0] = (double) (extents[0][0] + (i + 0.5) * res);
                points[i + j * w][1] = (double) (extents[0][1] + (j + 0.5) * res);
                //mask[j][i] = 0;
            }
        }

        for (int k = 0; k < envelopes.length; k++) {
            LayerFilter lf = envelopes[k];

            Grid grid = Grid.getGrid(getLayerPath(resolution, lf.getLayername()));

            float[] d = grid.getValues2(points);

            for (int i = 0; i < d.length; i++) {
                if (lf.isValid(d[i])) {
                    mask[i / w][i % w]++;
                }
            }
        }

        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                if (mask[j][i] == envelopes.length) {
                    mask[j][i] = 1;
                } else {
                    mask[j][i] = 0;
                }
            }
        }

        //find internal extents
        int minx = w;
        int maxx = -1;
        int miny = h;
        int maxy = -1;
        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                if (mask[j][i] > 0) {
                    if (minx > i) {
                        minx = i;
                    }
                    if (maxx < i) {
                        maxx = i;
                    }
                    if (miny > j) {
                        miny = j;
                    }
                    if (maxy < j) {
                        maxy = j;
                    }
                }
            }
        }

        //reduce the size of the mask
        int nw = maxx - minx + 1;
        int nh = maxy - miny + 1;
        byte[][] smallerMask = new byte[nh][nw];
        for (int i = minx; i < maxx; i++) {
            for (int j = miny; j < maxy; j++) {
                smallerMask[j - miny][i - minx] = mask[j][i];
            }
        }

        //update extents
        extents[0][0] += minx * res;
        extents[1][0] -= (w - maxx - 1) * res;
        extents[0][1] += miny * res;
        extents[1][1] -= (h - maxy - 1) * res;

        return smallerMask;
    }

    /**
     * Write a diva grid to disk for the envelope, 0 = absence, 1 = presence.
     *
     * @param filename output filename for the grid as String.
     * @param resolution target resolution in decimal degrees as String.
     * @param envelopes envelope specification as LayerFilter[].
     * @return area in sq km as double.
     */
    public static double makeEnvelope(String filename, String resolution, LayerFilter[] envelopes) {

        //get extents for all layers
        double[][] extents = getLayerExtents(resolution, envelopes[0].getLayername());
        for (int i = 1; i < envelopes.length; i++) {
            extents = internalExtents(extents, getLayerExtents(resolution, envelopes[i].getLayername()));
            if (!isValidExtents(extents)) {
                return -1;
            }
        }

        //get mask and adjust extents for filter
        byte[][] mask;
        int w, h;
        double res = Double.parseDouble(resolution);
        h = (int) Math.ceil((extents[1][1] - extents[0][1]) / res);
        w = (int) Math.ceil((extents[1][0] - extents[0][0]) / res);
        mask = getEnvelopeMaskAndUpdateExtents(resolution, res, extents, h, w, envelopes);
        h = (int) Math.ceil((extents[1][1] - extents[0][1]) / res);
        w = (int) Math.ceil((extents[1][0] - extents[0][0]) / res);

        float[] values = new float[w * h];
        int pos = 0;
        double areaSqKm = 0;
        for (int i = h - 1; i >= 0; i--) {
            for (int j = 0; j < w; j++) {
                values[pos] = mask[i][j];
                pos++;

                if (mask[i][j] > 0) {
                    areaSqKm += SpatialUtil.cellArea(res, extents[0][1] + res * i);
                }
            }
        }

        Grid grid = new Grid(getLayerPath(resolution, envelopes[0].getLayername()));

        grid.writeGrid(filename, values,
                extents[0][0],
                extents[0][1],
                extents[1][0],
                extents[1][1],
                grid.xres, grid.yres, h, w);

        return areaSqKm;
    }

    /**
     * Test if the layer filter is valid.
     *
     * The common problem is that a filter may refer to a layer that is not
     * available.
     *
     * @param resolution target resolution as String.
     * @param filter layer filter as LayerFilter[].
     * @return true iff valid filter.
     */
    public static boolean isValidLayerFilter(String resolution, LayerFilter[] filter) {
        for (LayerFilter lf : filter) {
            if (GridCutter.getLayerPath(resolution, lf.getLayername()) == null) {
                return false;
            }
        }
        return true;
    }

    /**
     * Determine the grid resolution that will be in use.
     *
     * @param layers list of layers to be used as String []
     * @param resolution target resolution as String
     * @return resolution that will be used
     */
    private static String confirmResolution(String[] layers, String resolution) {
        try {
            TreeMap<Double, String> resolutions = new TreeMap<Double, String>();
            for (String layer : layers) {
                String path = GridCutter.getLayerPath(resolution, layer);
                int end, start;
                if (path != null
                        && ((end = path.lastIndexOf(File.separator)) > 0)
                        && ((start = path.lastIndexOf(File.separator, end - 1)) > 0)) {
                    String res = path.substring(start + 1, end);
                    Double d = Double.parseDouble(res);
                    if (d < 1) {
                        resolutions.put(d, res);
                    }
                }
            }
            if (resolutions.size() > 0) {
                resolution = resolutions.firstEntry().getValue();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return resolution;
    }
}
