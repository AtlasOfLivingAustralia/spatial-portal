package org.ala.spatial.analysis.service;

import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import javax.imageio.ImageIO;
import org.ala.layers.intersect.Grid;
import org.ala.spatial.analysis.method.Aloc;
import org.ala.spatial.analysis.method.Pca;
import org.ala.spatial.util.AnalysisJob;
import org.ala.spatial.util.AnalysisJobAloc;
import org.ala.spatial.util.GridCutter;
import org.ala.spatial.util.Layer;
import org.ala.spatial.util.AlaspatialProperties;

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

    static void exportMetadata(String filename, int numberOfGroups, Layer[] layers, String pid, String coloursAndMeansUrl, String area, int width, int height, double minx, double miny, double maxx, double maxy, int iterationCount) {
        try {
            FileWriter fw = new FileWriter(filename);
            int i;

            fw.append("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\"> <html> <head> <meta http-equiv=\"Content-Type\" content=\"text/html; charset=MacRoman\"> <title>Layer information</title> <link rel=\"stylesheet\" href=\"/alaspatial/styles/style.css\" type=\"text/css\" media=\"all\" /> </head> ");

            fw.append("<body>");
            fw.append("<h1>").append("Classification").append("</h1>");

            fw.append("<p> <span class=\"title\">Model reference number:</span> <br /> ");
            fw.append(pid);
            fw.append("</p>");

            fw.append("<p> <span class=\"title\">Number of groups:</span> <br /> ");
            fw.append(String.valueOf(numberOfGroups));
            fw.append("</p>");

            fw.append("<p> <span class=\"title\">Number of iterations:</span> <br /> ");
            fw.append(String.valueOf(iterationCount));
            fw.append("</p>");

            fw.append("<p> <span class=\"title\">Layers:</span> <br /> ");
            for (i = 0; i < layers.length; i++) {
                fw.append(layers[i].display_name);
                if (i < layers.length - 1) {
                    fw.append(", ");
                }
            }
            fw.append("</p>");

            fw.append("<p> <a href=\"" + AlaspatialProperties.getBaseOutputURL() + "/files/inter_layer_association.csv\" >");
            fw.append("<span class=\"title\">Inter-layer dissimilarity matrix (csv)</span>  ");
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
                sld.append("<ColorMapEntry color=\"#" + s + "\" quantity=\"" + i + ".0\" label=\"group " + (i + 1) + "\" opacity=\"1\"/>\r\n");
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

    private static int getGroupRange(int[] groups) {
        if (groups == null) {
            return 0;
        }
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (int i = 0; i < groups.length; i++) {
            if (groups[i] < min) {
                min = groups[i];
            }
            if (groups[i] > max) {
                max = groups[i];
            }
        }
        if (min > max) {
            min = 0;
            max = 0;
        }
        return max - min;
    }

    public static void main(String[] args) {
//        args = new String[]{
//                    "e:\\mnt\\ala\\data\\index\\tabulation\\1329792717628\\",
//                    "20",
//                    "4",
//                    "d:\\"
//                };
        System.out.println("args[0] = grid files directory\n"
                + "args[1] = number of groups\n"
                + "args[2] = number of threads\n"
                + "args[3] = output path\n");

        String gridfilepath = args[0];
        int numberOfGroups = Integer.parseInt(args[1]);
        int numberOfThreads = Integer.parseInt(args[2]);
        String outputpath = args[3];

        String filename = outputpath + File.separator + "aloc.png";

        String name = "aloc";

        AnalysisJob job = null;

        if (job != null) {
            job.log("start ALOC");
        }

        /* get data, remove missing values, restrict by optional region */
        int i, j;
        j = 0;
        int width = 0, height = 0;

        int pieces = numberOfThreads * 4;

        ArrayList<Object> data_pieces = GridCutter.loadCutGridsForAloc(gridfilepath, outputpath + "extents.txt", pieces, job);

        //number of pieces may have changed
        pieces = data_pieces.size() - 2;

        if (job != null) {
            job.log("set cells length");
        }

        double[] extents = (double[]) data_pieces.get(data_pieces.size() - 1);
        width = (int) extents[0];
        height = (int) extents[1];

        //identify grid files
        File[] files = new File(gridfilepath).listFiles(new FilenameFilter() {

            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".grd") || name.endsWith(".GRD");
            }
        });
        Layer[] layers = new Layer[files.length];
        for (i = 0; i < files.length; i++) {
            layers[i] = new Layer(files[i].getName(), files[i].getName(), "", "");
        }

        /* run aloc
         * Note: requested number of groups may not always equal request
         */
        int [] iterationCount = new int[1];
        int[] groups = Aloc.runGowerMetricThreadedMemory(data_pieces, numberOfGroups, layers.length, pieces, layers, null, numberOfThreads, iterationCount);
        if (groups == null || getGroupRange(groups) < 2) {
            if (job != null && job.getCurrentState() != null && !job.getCurrentState().equals(AnalysisJob.FAILED)) {
                job.setCurrentState(AnalysisJobAloc.FAILED);
                job.setMessage("Classification failed to generate groups");
            }
        }

        if (job != null) {
            job.log("identified groups");
        }

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
        exportMeansColours(filename.replace("aloc.png", "classification_means.csv"), group_means, colours, layers);
        if (job != null) {
            job.log("exported group means and colours");
        }

        /* export metadata html */
        String urlpth = AlaspatialProperties.getBaseOutputURL() + "aloc/<insert job number here>/classification_means.csv";
        exportMetadata(filename.replace("aloc.png", "classification") + ".html", numberOfGroups, layers,
                (job != null) ? job.getName() : "<insert job number here>",
                urlpth,
                "", //(job != null) ? job.area : "",
                width, height, extents[2], extents[3], extents[4], extents[5],
                iterationCount[0]);

        /* export geoserver sld file for legend */
        exportSLD(filename + ".sld", group_means, colours, layers, "");

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
            e.printStackTrace(System.out);
        }

        if (job != null) {
            job.log("saved image");
        }

        if (job != null) {
            job.log("finished ALOC");
        }

        //write grid file
        double[] grid_data = new double[height * width];
        for (i = 0; i < grid_data.length; i++) {
            grid_data[i] = Double.NaN;
        }
        for (i = 0; i < groups.length; i++) {
            for (j = 0; j < colour.length; j++) {
                colour[j] = (int) (colours[groups[i]][j]);
            }
            grid_data[cells[i][0] + (height - cells[i][1] - 1) * width] = groups[i];
        }
        Grid g = new Grid(null);
        float res = (float) ((extents[4] - extents[2]) / width);
        g.writeGrid(filename.replace("aloc.png", name), grid_data, extents[2], extents[3], extents[4], extents[5],
                res, res,
                height, width);

        //export sld
        exportSLD(filename.replace("aloc.png", name + ".sld"), group_means, colours, layers, "0");

        //export ASCGRID
        BufferedWriter fw = null;
        try {
            fw = new BufferedWriter(
                    new OutputStreamWriter(
                    new FileOutputStream(filename.replace("aloc.png", name + ".asc")), "US-ASCII"));
            fw.append("ncols ").append(String.valueOf(width)).append("\n");
            fw.append("nrows ").append(String.valueOf(height)).append("\n");
            fw.append("xllcorner ").append(String.valueOf(extents[2])).append("\n");
            fw.append("yllcorner ").append(String.valueOf(extents[3])).append("\n");
            fw.append("cellsize ").append(String.valueOf(res)).append("\n");

            fw.append("NODATA_value ").append(String.valueOf(-1));

            for (i = 0; i < height; i++) {
                fw.append("\n");
                for (j = 0; j < width; j++) {
                    if (j > 0) {
                        fw.append(" ");
                    }
                    if (Double.isNaN(grid_data[i * width + j])) {
                        fw.append("-1");
                    } else {
                        fw.append(String.valueOf(grid_data[i * width + j]));
                    }
                }
            }
            fw.append("\n");
        } catch (Exception e) {
            e.printStackTrace(System.out);
        } finally {
            if (fw != null) {
                try {
                    fw.close();
                } catch (Exception e) {
                    e.printStackTrace(System.out);
                }
            }
        }
    }

    static String readFile(String file) {
        String s = null;
        try {
            RandomAccessFile raf = new RandomAccessFile(file, "r");
            byte[] b = new byte[(int) raf.length()];
            raf.read(b);
            raf.close();
            s = new String(b, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return s;
    }

    private static void writeProjectionFile(String filename) {
        try {
            PrintWriter spWriter = new PrintWriter(new BufferedWriter(new FileWriter(filename)));

            StringBuffer sbProjection = new StringBuffer();
            sbProjection.append("GEOGCS[\"WGS 84\", ").append("\n");
            sbProjection.append("    DATUM[\"WGS_1984\", ").append("\n");
            sbProjection.append("        SPHEROID[\"WGS 84\",6378137,298.257223563, ").append("\n");
            sbProjection.append("            AUTHORITY[\"EPSG\",\"7030\"]], ").append("\n");
            sbProjection.append("        AUTHORITY[\"EPSG\",\"6326\"]], ").append("\n");
            sbProjection.append("    PRIMEM[\"Greenwich\",0, ").append("\n");
            sbProjection.append("        AUTHORITY[\"EPSG\",\"8901\"]], ").append("\n");
            sbProjection.append("    UNIT[\"degree\",0.01745329251994328, ").append("\n");
            sbProjection.append("        AUTHORITY[\"EPSG\",\"9122\"]], ").append("\n");
            sbProjection.append("    AUTHORITY[\"EPSG\",\"4326\"]] ").append("\n");

            spWriter.write(sbProjection.toString());
            spWriter.close();

        } catch (IOException ex) {            
            System.out.println("error writing species file:");
            ex.printStackTrace(System.out);
        }
    }
}
