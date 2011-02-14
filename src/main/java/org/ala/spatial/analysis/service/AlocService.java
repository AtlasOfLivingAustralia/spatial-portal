package org.ala.spatial.analysis.service;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import javax.imageio.ImageIO;
import org.ala.spatial.analysis.index.LayerFilter;
import org.ala.spatial.analysis.method.Aloc;
import org.ala.spatial.analysis.method.Pca;
import org.ala.spatial.util.AnalysisJobAloc;
import org.ala.spatial.util.GridCutter;
import org.ala.spatial.util.Layer;
import org.ala.spatial.util.SimpleRegion;
import org.ala.spatial.util.SpatialLogger;
import org.ala.spatial.util.TabulationSettings;

/**
 * entry into running Aloc
 * 
 * use run
 * 
 * @author adam
 *
 */
public class AlocService {

    /**
     * runs an ALOC classification
     * @param filename output filename for image out
     * @param layers list of layers to include in ALOC as Layer[]
     * @param numberofgroups number groups to generate as int
     * @param region option restrictive region as SimpleRegion
     * @param envelope option restrictive envelope of LayerFilter[]
     * @param id session id as String
     * @return groups as int[]
     */
    public static int[] run(String filename, Layer[] layers, int numberofgroups, SimpleRegion region, LayerFilter[] envelope, String id) {
        return run(filename, layers, numberofgroups, region, envelope, id, null);
    }

    /**
     * exports means and colours of a classification (ALOC) into
     * a csv
     *
     * @param filename csv filename to export into
     * @param means mean values for each legend record as [n][m]
     * where 
     * 	n is number of records
     *  m is number of layers used to generate the classification (ALOC)
     * @param colours RGB colours of legend records as [n][3] 
     * where 
     *  n is number of records
     *  [][0] is red
     *  [][1] is green
     *  [][2] is blue
     * @param layers layers used to generate the classification as Layer[]
     */
    static void exportMeansColours(String filename, double[][] means, int[][] colours, Layer[] layers) {
        try {
            FileWriter fw = new FileWriter(filename);
            int i, j;

            /* header */
            fw.append("group number");
            fw.append(",red");
            fw.append(",green");
            fw.append(",blue");
            for (i = 0; i < layers.length; i++) {
                fw.append(",");
                fw.append(layers[i].display_name);
            }
            fw.append("\r\n");

            /* outputs */
            for (i = 0; i < means.length; i++) {
                fw.append(String.valueOf(i + 1));
                fw.append(",");
                fw.append(String.valueOf(colours[i][0]));
                fw.append(",");
                fw.append(String.valueOf(colours[i][1]));
                fw.append(",");
                fw.append(String.valueOf(colours[i][2]));

                for (j = 0; j < means[i].length; j++) {
                    fw.append(",");
                    fw.append(String.valueOf(means[i][j]));
                }

                fw.append("\r\n");
            }

            fw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void exportMetadata(String filename, int numberOfGroups, Layer[] layers, String pid, String coloursAndMeansUrl, String area, int width, int height, double minx, double miny, double maxx, double maxy) {
        try {
            FileWriter fw = new FileWriter(filename);
            int i;

            fw.append("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\"> <html> <head> <meta http-equiv=\"Content-Type\" content=\"text/html; charset=MacRoman\"> <title>Layer information</title> <link rel=\"stylesheet\" href=\"/alaspatial/styles/style.css\" type=\"text/css\" media=\"all\" /> </head> ");

            fw.append("<body>");
            fw.append("<h1>").append("Classification").append("</h1>");

            fw.append("<p> <span class=\"title\">Reference number:</span> <br /> ");
            fw.append(pid);
            fw.append("</p>");

            fw.append("<p> <span class=\"title\">Number of groups:</span> <br /> ");
            fw.append(String.valueOf(numberOfGroups));
            fw.append("</p>");

            fw.append("<p> <span class=\"title\">Layers:</span> <br /> ");
            for (i = 0; i < layers.length; i++) {
                fw.append(layers[i].display_name);
                if (i < layers.length - 1) {
                    fw.append(", ");
                }
            }
            fw.append("</p>");

            fw.append("<p> <a href=\"" + TabulationSettings.alaspatial_path + "layers/analysis/inter_layer_association.pdf\" >");
            fw.append("<span class=\"title\">Inter-layer dissimilarity matrix (pdf)</span>  ");
            fw.append("</a>");
            fw.append("</p>");

            //fw.append("<p> <span class=\"title\">Extent:</span> <br /> ");
            //fw.append("width=").append(String.valueOf(width)).append("<br />");
            //fw.append("height=").append(String.valueOf(height)).append("<br />");
            //fw.append(String.valueOf(minx)).append(",").append(String.valueOf(miny)).append(";");
            //fw.append(String.valueOf(maxx)).append(",").append(String.valueOf(maxy));
            //fw.append("</p>");

            fw.append("<p> <span class=\"title\">Area:</span> <br /> ");
            fw.append(area);
            fw.append("</p>");

            fw.append("<p> <a href=\"" + coloursAndMeansUrl + "\">");
            fw.append("<span class=\"title\">Group means and colours (csv)</span>  ");
            fw.append("</a>");
            fw.append("</p>");

            fw.append("</body> </html> ");

            fw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * exports a geoserver sld file for legend generation
     * 
     * TODO: find out why it reports error when attached layer is called
     * 
     * @param filename sld filename to export into
     * @param means mean values for each legend record as [n][m]
     * where 
     * 	n is number of records
     *  m is number of layers used to generate the classification (ALOC)
     * @param colours RGB colours of legend records as [n][3] 
     * where 
     *  n is number of records
     *  [][0] is red
     *  [][1] is green
     *  [][2] is blue
     * @param layers layers used to generate the classification as Layer[]
     * @param id unique id (likely to be session_id) as String
     */
    static void exportSLD(String filename, double[][] means, int[][] colours, Layer[] layers, String id) {
        try {
            StringBuffer sld = new StringBuffer();
            sld.append("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>");

            /* header */
            sld.append("<StyledLayerDescriptor version=\"1.0.0\" xmlns=\"http://www.opengis.net/sld\" xmlns:ogc=\"http://www.opengis.net/ogc\"");
            sld.append(" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
            sld.append(" xsi:schemaLocation=\"http://www.opengis.net/sld http://schemas.opengis.net/sld/1.0.0/StyledLayerDescriptor.xsd\">");
            sld.append(" <NamedLayer>");
            sld.append(" <Name>aloc_" + id + "</Name>");
            sld.append(" <UserStyle>");
            sld.append(" <Name>aloc_" + id + "</Name>");
            sld.append(" <Title>ALA ALOC distribution</Title>");
            sld.append(" <FeatureTypeStyle>");
            sld.append(" <Rule>");
            sld.append(" <RasterSymbolizer>");
            sld.append(" <ColorMap type=\"values\" >");

            int i, j;
            String s;

            /* outputs */
            for (i = 0; i < colours.length; i++) {
                j = 0x00000000 | ((colours[i][0] << 16) | (colours[i][1] << 8) | colours[i][2]);
                s = Integer.toHexString(j).toUpperCase();
                while (s.length() < 6) {
                    s = "0" + s;
                }
                sld.append("<ColorMapEntry color=\"#" + s + "\" quantity=\"" + (i + 1) + ".0\" label=\"group " + (i + 1) + "\" opacity=\"1\"/>\r\n");
            }

            /* footer */
            sld.append("</ColorMap></RasterSymbolizer></Rule></FeatureTypeStyle></UserStyle></NamedLayer></StyledLayerDescriptor>");

            /* write */
            FileWriter fw = new FileWriter(filename);
            fw.append(sld.toString());
            fw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void exportExtents(String filename, int width, int height, double minx, double miny, double maxx, double maxy) {
        try {
            FileWriter fw = new FileWriter(filename);
            fw.append(String.valueOf(width)).append("\n");
            fw.append(String.valueOf(height)).append("\n");
            fw.append(String.valueOf(minx)).append("\n");
            fw.append(String.valueOf(miny)).append("\n");
            fw.append(String.valueOf(maxx)).append("\n");
            fw.append(String.valueOf(maxy)).append("\n");
            fw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static int[] run(String filename, Layer[] layers, int numberOfGroups, SimpleRegion region, LayerFilter[] envelope, String name, AnalysisJobAloc job) {
        TabulationSettings.load();

        if (job != null) {
            job.log("start ALOC");
        }

        /* get data, remove missing values, restrict by optional region */
        int i, j;
        j = 0;
        int width = 0, height = 0;
        String layerPath = TabulationSettings.environmental_data_path;
        if (layerPath == null) {
            layerPath = "";
        }
        //TODO: # piecies decided by memory available / memory required / threadcount
        int pieces = TabulationSettings.analysis_threads * 4;
        ArrayList<Object> data_pieces = GridCutter.cut(layers, region, pieces, filename + "extents.txt", envelope, job);

        //number of pieces may have changed
        pieces = data_pieces.size() - 2;

        if (job != null) {
            job.setCells(((int[][]) data_pieces.get(data_pieces.size() - 2)).length);
        }

        if (job != null) {
            job.log("set cells length");
        }

        double[] extents = (double[]) data_pieces.get(data_pieces.size() - 1);
        width = (int) extents[0];
        height = (int) extents[1];


        /* run aloc
         * Note: requested number of groups may not always equal request
         */
        //long start = System.currentTimeMillis();
        //int[] groups0 = Aloc.runGowerMetricThreaded(data_pieces, numberOfGroups, layers.length, pieces, job);
        //long middle = System.currentTimeMillis();
        //long one = middle - start;

        //reset data
        //data_pieces = GridCutter.cut(layers, region, pieces, filename + "extents.txt", envelope,job);
        //start = System.currentTimeMillis();
        //int[] groups1 = Aloc.runGowerMetricThreadedSpeedup1(data_pieces, numberOfGroups, layers.length, pieces, job);
        //middle = System.currentTimeMillis();
        //long two = middle -start;

        //reset data
        //data_pieces = GridCutter.cut(layers, region, pieces, filename + "extents.txt", envelope,job);
        //start  = System.currentTimeMillis();
        int[] groups = Aloc.runGowerMetricThreadedMemory(data_pieces, numberOfGroups, layers.length, pieces, job);
        if (job != null && job.isCancelled()) {
            return null;
        }
        //middle = System.currentTimeMillis();
        //long three = middle -start;

        /* pieces = 1;
        data_pieces = GridCutter.cut(layers, region, pieces, filename + "extents.txt", envelope,job);
        int[] groups3 = Aloc.runGowerMetricThreadedMemory(data_pieces, numberOfGroups, layers.length, pieces, job);

        //System.out.println("ALOC TIMINGS: (A)" + (one) + " (B)" + (two) + " (C)" + (three));

        //System.out.println("start check: " + groups.length + " == " + groups0.length);
        int count = 0;
        int count2 = 0;
        int count3 = 0;
        for(i=0;i<groups.length;i++){
        //if(groups[i] != groups1[i]){
        //                count++;
        //          }
        if(groups[i] != groups3[i]){
        count3++;
        }
        //  if(groups[i] != groups0[i]){
        //      count2++;
        //  }
        }
        System.out.println("end check: (a-b)" + (count / (double)groups.length) + " (a-c)" + (count2 / (double)groups.length)  + " (c-d)" + (count3) + " of " + groups.length);
         */
        if (job != null) {
            job.log("identified groups");
        }

        SpatialLogger.log("done gower metric");

        /* recalculate group counts */
        int newNumberOfGroups = 0;
        for (i = 0; i < groups.length; i++) {
            if (groups[i] > newNumberOfGroups) {
                newNumberOfGroups = groups[i];
            }
        }
        newNumberOfGroups++; //group number is 0..n-1
        numberOfGroups = newNumberOfGroups;

        /* calculate group means */
        double[][] group_means = new double[numberOfGroups][layers.length];
        int[][] group_counts = new int[numberOfGroups][layers.length];

        /* determine group means */
        int row = 0;
        for (int k = 0; k < pieces; k++) {
            float[] d = (float[]) data_pieces.get(k);
            for (i = 0; i < d.length; i += layers.length, row++) {
                for (j = 0; j < layers.length; j++) {
                    if (!Float.isNaN(d[i + j])) {
                        group_counts[groups[row]][j]++;
                        group_means[groups[row]][j] += d[i + j];
                    }
                }
            }
        }

        double[][] group_means_copy = new double[group_means.length][group_means[0].length];
        for (i = 0; i < group_means.length; i++) {
            for (j = 0; j < group_means[i].length; j++) {
                if (group_counts[i][j] > 0) {
                    group_means[i][j] /= group_counts[i][j];
                    group_means_copy[i][j] = group_means[i][j];
                }
            }
        }

        if (job != null) {
            job.log("determined group means");
        }

        /* get RGB for colouring group means via PCA */
        int[][] colours = Pca.getColours(group_means_copy);
        if (job != null) {
            job.log("determined group colours");
        }


        /* export means + colours */
        exportMeansColours(filename + ".csv", group_means, colours, layers);
        if (job != null) {
            job.log("exported group means and colours");
        }

        /* export metadata html */
        String pth = "output" + File.separator + "aloc" + File.separator;
        int pos = filename.indexOf(pth);
        String f = filename.substring(pos + pth.length());
        String urlpth = TabulationSettings.alaspatial_path + "output/aloc/" + f.replace("\\", "/");
        exportMetadata(filename + ".html", numberOfGroups, layers,
                (job != null) ? job.getName() : "",
                urlpth + ".csv",
                (job != null) ? job.area : "",
                width, height, extents[2], extents[3], extents[4], extents[5]);

        /* export geoserver sld file for legend */
        //exportSLD(filename + ".sld", group_means, colours, layers, id);

        /* map back as colours, grey scale for now */
        BufferedImage image = new BufferedImage(width, height,
                BufferedImage.TYPE_INT_ARGB);
        int[] image_bytes;

        image_bytes = image.getRGB(0, 0, image.getWidth(), image.getHeight(),
                null, 0, image.getWidth());

        /* try transparency as missing value */
        for (i = 0; i < image_bytes.length; i++) {
            image_bytes[i] = 0x00000000;
        }

        int[][] cells = (int[][]) data_pieces.get(data_pieces.size() - 2);
        int[] colour = new int[3];
        for (i = 0; i < groups.length; i++) {
            for (j = 0; j < colour.length; j++) {
                colour[j] = (int) (colours[groups[i]][j]);
            }

            //set up rgb colour for this group (upside down)
            image_bytes[cells[i][0] + (height - cells[i][1] - 1) * width] = 0xff000000 | ((colour[0] << 16) | (colour[1] << 8) | colour[2]);
        }

        /* write bytes to image */
        image.setRGB(0, 0, image.getWidth(), image.getHeight(),
                image_bytes, 0, image.getWidth());

        /* save image */
        try {
            ImageIO.write(image, "png",
                    new File(filename));
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (job != null) {
            job.log("saved image");
        }

        if (job != null) {
            job.log("finished ALOC");
        }

        return groups;
    }
}