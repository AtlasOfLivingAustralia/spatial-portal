/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.ala.spatial.analysis.legend;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import javax.imageio.ImageIO;

/**
 *
 * @author Adam
 */
public abstract class Legend {
    
    /*
     * Colours are all set as transparent for no reason
     *
     * There are groups+1 colours
     */
    final int [] colours = {0x00ffffff, 0x00009999, 0x0099FF66
          , 0x00FFFF66
          , 0x00FFFF00
          , 0x00FF9900
          , 0x00FF6600
          , 0x00FF6666
          , 0x00FF3300
          , 0x00CC33FF
          , 0x00FF33FF };

    /*
     * for determining the records that are equal to the maximum value
     */
    float [] cutoffs;

    /**
     * number of group members
     */
    int [] groupSizes;

    /*
     * min/max values
     */
    float min, max;

    /*
     * number of non-NaN values
     */
    int numberOfRecords;

    /*
     * number of unique values
     */
    int numberOfUniqueValues;

    /*
     * array position of last value
     */
    int lastValue;

    /**
     * generate the legend cutoff points.
     *
     * @param d asc sorted float []
     */
    abstract public void generate(float [] d);

    /**
     * return nice name for the method that this class uses to generate the
     * cutoff points
     *
     * @return name as String
     */
    abstract public String getTypeName();

    /**
     * some common values
     *
     * @param d as sorted float []
     */
    void init(float [] d) {
        //NaN sorted last.
        min = d[0];

        for(int i=0;i<d.length;i++) {
            if(!Float.isNaN(d[i])) {
                numberOfRecords++;

                if(i == 0 || d[i] != d[i-1]) {
                    numberOfUniqueValues++;
                }
            }
        }

        lastValue = numberOfRecords;

        max = d[numberOfRecords - 1];
    }

    /**
     * size is represented by number of unique values.
     *
     * @param d float [] sorted in ascending order
     */
    void determineGroupSizes(float [] d) {
        groupSizes = new int[cutoffs.length];

        int cutoffPos = 0;
        for(int i=0;i<d.length;i++) {
            if(Float.isNaN(d[i])) {
                continue;
            } else if(d[i] > cutoffs[cutoffPos]) {
                cutoffPos++;
            }
            if(i == 0 || d[i-1] != d[i]) {
                groupSizes[cutoffPos]++;    //max cutoff == max value
            }
        }
    }

    /**
     * lower is better
     *
     * @param d
     * @return
     */
    double evaluateStdDev(float [] d) {
        determineGroupSizes(d);

        float stdev = 0;
        float mean = numberOfUniqueValues / (float) groupSizes.length;
        for(int i=0;i<groupSizes.length;i++) {
            stdev += Math.pow(groupSizes[i] - mean, 2) / groupSizes.length;
        }

        return stdev;
    }

    /**
     * save to a file as a png.
     * 
     * @param d float [] of raster data to have legend applied
     * @param width row width
     * @param filename output filename
     */
    void exportImage(float [] d, int width, String filename) {
        try {
            /* make image */
            BufferedImage image = new BufferedImage(width, d.length / width,
                    BufferedImage.TYPE_4BYTE_ABGR);

            /* get bytes structure */
            int [] image_bytes = image.getRGB(0, 0, image.getWidth(), image.getHeight(),
                    null, 0, image.getWidth());

            //fill
            for (int i = 0; i < image_bytes.length; i++) {
                image_bytes[i] = getColour(d[i]);
            }

            /* write back image bytes */
            image.setRGB(0, 0, image.getWidth(), image.getHeight(),
                    image_bytes, 0, image.getWidth());

            /* write image */
            ImageIO.write(image, "png", new File(filename));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * @param d asc sorted float []
     * @return colour of d after applying cutoff's.
     */
    int getColour(float d) {
        if(Float.isNaN(d)) {
            return 0x00000000;
        }
        int pos = java.util.Arrays.binarySearch(cutoffs, d);
        if(pos < 0) {
            pos = (pos * -1) - 1;
        }
        if(pos >= cutoffs.length) {
            return 0x00000000;
        } else {
            double upper = cutoffs[pos];
            double lower;
            if(pos == 0) {
                lower = min;
            } else {
                lower = cutoffs[pos-1];
            }

            //translate value to 0-1 position between the colours
            double v = (d - lower) / (upper - lower);
            double vt = 1 - v;

            //there are groups+1 colours
            int red = (int) ((colours[pos] & 0x00FF0000) * vt + (colours[pos+1]  & 0x00FF0000) * v);
            int green = (int) ((colours[pos] & 0x0000FF00) * vt + (colours[pos+1]  & 0x0000FF00) * v);
            int blue = (int) ((colours[pos] & 0x00000FF) * vt + (colours[pos+1]  & 0x000000FF) * v);

            return (red & 0x00FF0000) | (green & 0x0000FF00) | (blue & 0x000000FF) | 0xFF000000;
        }
    }

    /**
     * get cutoff values as String
     *
     * includes group sizes if calculated
     *
     * @return String of cutoff values
     */
    public String getCutoffs() {
        StringBuffer sb = new StringBuffer();
        for(int i=0;i<cutoffs.length;i++) {
            if(groupSizes != null) {
                sb.append(String.valueOf(cutoffs[i])).append("\t").append(String.valueOf(groupSizes[i])).append("\n");
            } else {
                sb.append(String.valueOf(cutoffs[i])).append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * write the cutoff points to a file as text
     *
     * @param filename
     */
    void exportLegend(String filename) {
        try {
            FileWriter fw = new FileWriter(filename);
            fw.append(getCutoffs());
            fw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}