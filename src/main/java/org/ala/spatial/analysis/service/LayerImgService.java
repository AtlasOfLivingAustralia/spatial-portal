package org.ala.spatial.analysis.service;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import javax.imageio.ImageIO;
import org.apache.commons.io.FileUtils;

/**
 * service setting up layer image for webservice
 *
 * path from pid exists in output/layer
 *
 * original image: srcimg.png
 * current image: img.png
 *
 * original legend: srclegend.txt
 * legend: legend.txt
 * (header) name, red, green, blue CR
 * (records) string, 0-255, 0-255, 0-255 CR
 *
 * extents: extents.txt
 * width in px
 * height in px
 * min longitude
 * min latitude
 * max longitude
 * max latitude
 *
 * uses output/layers/<pid>/ directory
 *
 * @author adam
 */
public class LayerImgService {

    public static boolean registerLayerImage(String outputlayerdir, String pid, String srcimagepath, String extents, String legend, String metadata) {
        /* check directory exists at outputlayerdir */
        String path = outputlayerdir + "output" + File.separator + "layers" + File.separator + pid + File.separator;
        System.out.println("about to create directory: " + outputlayerdir + "output" + File.separator + "layers" + File.separator + pid + " : "
                + " :FOR IMG>" + srcimagepath);
        try {
            File workingDir = new File(outputlayerdir + "output" + File.separator + "layers" + File.separator + pid);
            System.out.println("createdir=" + workingDir.mkdir());
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            /* copy srcimg */
            FileUtils.copyFile(new File(srcimagepath), new File(path + "srcimg.png"));

            /* copy img */
            FileUtils.copyFile(new File(srcimagepath), new File(path + "img.png"));

            /* write extents */
            FileWriter fw = new FileWriter(path + "extents.txt");
            fw.append(extents);
            fw.close();

            /* write srclegend */
            fw = new FileWriter(path + "srclegend.txt");
            fw.append(legend);
            fw.close();

            /* write legend */
            fw = new FileWriter(path + "legend.txt");
            fw.append(legend);
            fw.close();

            /* write metadata */
            fw = new FileWriter(path + "metadata.html");
            fw.append(metadata);
            fw.close();

            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * changes the colour of the legend idx'ed image at the specified path
     * @param path full local path eg (/output/layers/<pid>/) 
     * @param idx 
     * @param red
     * @param green
     * @param blue
     */
    public static void changeColour(String path, int idx, int red, int green, int blue) {
        try {
            /* load source and destination image */
            System.out.println("read: " + path + "srcimg.png");
            BufferedImage srcimg = ImageIO.read(new File(path + "srcimg.png"));
            BufferedImage img = ImageIO.read(new File(path + "img.png"));
            System.out.println("done");

            /* get bytes structure */
            int[] src_image_bytes = srcimg.getRGB(0, 0, srcimg.getWidth(), srcimg.getHeight(),
                    null, 0, srcimg.getWidth());
            int[] image_bytes = img.getRGB(0, 0, img.getWidth(), img.getHeight(),
                    null, 0, img.getWidth());

            /* get previous colour to replace from source legend
             *
             * rebuild output legend at the same time
             */
            BufferedReader srclegend = new BufferedReader(new FileReader(path + "srclegend.txt"));
            BufferedReader legend = new BufferedReader(new FileReader(path + "legend.txt"));
            FileWriter legendout = new FileWriter(path + "legendcopy.txt");

            //header
            String srcline = null;
            String line = null;
            srclegend.readLine();
            line = legend.readLine();
            legendout.append(line);
            legendout.append("\r\n");

            //read/write
            int i = 0;
            while (i <= idx && (srcline = srclegend.readLine()) != null
                    && (line = legend.readLine()) != null) {
                if (i < idx) {					//do not write out line that is changing here
                    legendout.append(line);
                    legendout.append("\r\n");
                }
                i++;
            }
            if (line == null) {
                //error, idx too high
                return;
            }
            //split target src legend line into records; name, r,g,b,...
            String[] record = srcline.split(",");
            if (record.length < 4) {
                //error with srclegend, not enough columns
                return;
            }
            //convert to new legend line
            line = record[0] + "," + red + "," + green + "," + blue;
            i = 4;
            while (i < record.length) {			//appending any remainder
                line += "," + record[i];
                i++;
            }

            //write out new line
            legendout.append(line);
            legendout.append("\r\n");
            //write rest of legend out
            while ((line = legend.readLine()) != null) {
                legendout.append(line);
                legendout.append("\r\n");
            }
            srclegend.close();
            legend.close();
            legendout.close();
            //replace old legend with new
            FileUtils.copyFile(new File(path + "legendcopy.txt"), new File(path + "legend.txt"));

            //parse rgb
            int srcred = Integer.parseInt(record[1]);
            int srcgreen = Integer.parseInt(record[2]);
            int srcblue = Integer.parseInt(record[3]);

            //aggregate into image value
            int srcrgb = 0xff000000 | ((srcred << 16) | (srcgreen << 8) | srcblue);
            int newrgb = 0xff000000 | ((red << 16) | (green << 8) | blue);

            /* iterate across srcimg replacing old rgb to new */
            int count_changed = 0;
            for (i = 0; i < image_bytes.length; i++) {
                if (src_image_bytes[i] == srcrgb) {		//do src to src colour comparison
                    count_changed++;
                    image_bytes[i] = newrgb;
                }
            }

            /* output new image */
            System.out.println("changes: " + count_changed + " writing: " + path + "img.png");
            System.out.println("old: " + Integer.toHexString(srcrgb) + " new: " + Integer.toHexString(newrgb));
            img.setRGB(0, 0, img.getWidth(), img.getHeight(),
                    image_bytes, 0, img.getWidth());
            ImageIO.write(img, "png",
                    new File(path + "img.png"));
            System.out.println("done");

        } catch (Exception e) {
            System.out.println(path);
            e.printStackTrace();
        }
    }

    public static boolean registerLayerLegend(String outputlayerdir, String pid, String legend) {
        /* check directory exists at outputlayerdir */
        String path = outputlayerdir + "output" + File.separator + "layers" + File.separator + pid + File.separator;
        System.out.println("about to create directory: " + outputlayerdir + "output" + File.separator + "layers" + File.separator + pid + " : "
                + " :FOR LEGEND.");
        try {
            File workingDir = new File(outputlayerdir + "output" + File.separator + "layers" + File.separator + pid);
            System.out.println("createdir=" + workingDir.mkdir());
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            /* write srclegend */
            FileWriter fw = new FileWriter(path + "srclegend.txt");
            fw.append(legend);
            fw.close();

            /* write legend */
            fw = new FileWriter(path + "legend.txt");
            fw.append(legend);
            fw.close();

            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }
}
