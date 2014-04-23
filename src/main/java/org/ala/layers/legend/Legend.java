/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.layers.legend;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.Serializable;
import javax.imageio.ImageIO;

import org.ala.layers.intersect.Grid;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonSubTypes;
import org.codehaus.jackson.annotate.JsonTypeInfo;
import org.codehaus.jackson.map.annotate.JsonDeserialize;

/**
 * @author Adam
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "typeName")
@JsonSubTypes({
        @JsonSubTypes.Type(value = LegendDecade.class, name = "Decade Legend"),
        @JsonSubTypes.Type(value = LegendEqualArea.class, name = "Equal Area"),
        @JsonSubTypes.Type(value = LegendEqualSize.class, name = "Equal Size"),
        @JsonSubTypes.Type(value = LegendEvenInterval.class, name = "Even Interval"),
        @JsonSubTypes.Type(value = LegendEvenIntervalLog.class, name = "Even Interval Log"),
        @JsonSubTypes.Type(value = LegendEvenIntervalLog10.class, name = "Even Interval Log 10"),
})
@JsonIgnoreProperties({"minMax", "cutoffs"})
public abstract class Legend implements Serializable {

    final static String LEGEND_KEY = "/legend_key.png";

    /*
     * Colours are all set as transparent for no reason
     *
     * There are groups+1 colours
     */
    final public static int[] colours = {0x00002DD0, 0x00005BA2, 0x00008C73, 0x0000B944, 0x0000E716, 0x00A0FF00, 0x00FFFF00, 0x00FFC814, 0x00FFA000, 0x00FF5B00, 0x00FF0000};

    /*
         * for determining the records that are equal to the maximum value
         */
    float[] cutoffs;

    /*
     * for determining the records that are equal to the minimum value
     */
    float[] cutoffMins;

    /*
     * cutoffs.length may not match colours.length, this a translation
     */
    float[] cutoffsColours;
    /**
     * number of group members by Unique Value
     */
    int[] groupSizes;
    /**
     * number of group members by Area
     */
    int[] groupSizesArea;

    /*
     * number of NaN values from counts
     */
    int countOfNaN;

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

    /*
     * number of cut points
     */
    int divisions;

    /**
     * generate the legend cutoff points.
     * <p/>
     * default number of cutoffs = 10
     *
     * @param d asc sorted float []
     */
    public void generate(float[] d) {
        generate(d, 10);
    }

    /**
     * generate the legend cutoff points.
     *
     * @param d         asc sorted float []
     * @param divisions number of cut points
     */
    abstract public void generate(float[] d, int divisions);

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
     * @param d         as sorted float []
     * @param divisions number of cutpoints
     */
    void init(float[] d, int divisions) {
        this.divisions = divisions;

        //NaN sorted last.
        min = d[0];

        for (int i = 0; i < d.length; i++) {
            if (!Float.isNaN(d[i])) {
                numberOfRecords++;

                if (i == 0 || d[i] != d[i - 1]) {
                    numberOfUniqueValues++;
                }
            }
        }

        lastValue = numberOfRecords;
        if (numberOfRecords == 0) {
            max = Float.NaN;
        } else {
            max = d[numberOfRecords - 1];
        }

        cutoffsColours = null;
    }

    /**
     * size is represented by number of unique values.
     *
     * @param d float [] sorted in ascending order
     */
    public void determineGroupSizes(float[] d) {
        if (cutoffs == null) {
            return;
        }

        //fix cutoffs
        for (int i = 1; i < cutoffs.length; i++) {
            if (cutoffs[i] < cutoffs[i - 1]) {
                for (int j = i; j < cutoffs.length; j++) {
                    cutoffs[j] = cutoffs[i - 1];
                }
                break;
            }
        }

        groupSizes = new int[cutoffs.length];

        int cutoffPos = 0;
        countOfNaN = 0;
        for (int i = 0; i < d.length; i++) {
            if (Float.isNaN(d[i])) {
                countOfNaN++;
                continue;
            } else if (d[i] > cutoffs[cutoffPos]) {
                while (d[i] > cutoffs[cutoffPos]) {
                    cutoffPos++;
                }
            }
            if (i == 0 || d[i - 1] != d[i]) {
                groupSizes[cutoffPos]++;    //max cutoff == max value
            }
        }

        groupSizesArea = determineGroupSizesArea(d);
    }

    /**
     * range better on features
     * <p/>
     * lower is better
     *
     * @param d
     * @return
     */
    public double evaluateStdDev(float[] d) {
        if (Float.isNaN(max)) {
            return Double.NaN;
        }
        determineGroupSizes(d);

        float stdev = 0;
        float mean = numberOfUniqueValues / (float) groupSizes.length;
        for (int i = 0; i < groupSizes.length; i++) {
            stdev += Math.pow(groupSizes[i] - mean, 2) / (float) groupSizes.length;
        }

        stdev = (float) Math.sqrt(stdev);

        return stdev;
    }

    /**
     * size is represented by number of unique values.
     *
     * @param d float [] sorted in ascending order
     */
    int[] determineGroupSizesArea(float[] d) {
        if (cutoffs == null) {
            return null;
        }

        cutoffMins = new float[cutoffs.length];
        int[] grpSizes = new int[cutoffs.length];

        int cutoffPos = 0;
        for (int i = 0; i < d.length; i++) {
            if (Float.isNaN(d[i])) {
                continue;
            }
            while (d[i] > cutoffs[cutoffPos]) {
                while (d[i] > cutoffs[cutoffPos]) {
                    cutoffPos++;
                    cutoffMins[cutoffPos] = d[i];
                }
            }
            grpSizes[cutoffPos]++;
        }

        return grpSizes;
    }

    /**
     * range better on area
     * <p/>
     * lower is better
     *
     * @param d
     * @return
     */
    public double evaluateStdDevArea(float[] d) {
        if (Float.isNaN(max)) {
            return Double.NaN;
        }

        int[] grpSizes = determineGroupSizesArea(d);
        int sum = 0;
        for (int i = 0; i < grpSizes.length; i++) {
            sum += grpSizes[i];
        }

        double stdev = 0;
        double mean = sum / (double) grpSizes.length;
        for (int i = 0; i < grpSizes.length; i++) {
            stdev += Math.pow(grpSizes[i] - mean, 2) / (double) grpSizes.length;
        }

        stdev = (float) Math.sqrt(stdev);

        return stdev;
    }

    /**
     * save to a file as a type (filename extension).
     * <p/>
     * Option to scale down image size by discarding values/pixels
     *
     * @param d        float [] of raster data to have legend applied
     * @param width    row width
     * @param filename output filename
     */
    public void exportImage(float[] d, int width, String filename, int scaleDownBy, boolean minValueTransparent) {
        try {
            /* make image */
            BufferedImage image = null;
            if (minValueTransparent) {
                image = new BufferedImage(width / scaleDownBy, d.length / width / scaleDownBy,
                        BufferedImage.TYPE_4BYTE_ABGR);
            } else {
                image = new BufferedImage(width / scaleDownBy, d.length / width / scaleDownBy,
                        BufferedImage.TYPE_INT_BGR);
            }

            /* get bytes structure */
            int[] image_bytes = image.getRGB(0, 0, image.getWidth(), image.getHeight(),
                    null, 0, image.getWidth());

            //fill
            for (int i = 0; i < image_bytes.length; i++) {
                int x = i % (width / scaleDownBy);
                int y = i / (width / scaleDownBy);

                int dataX = x * scaleDownBy;
                int dataY = y * scaleDownBy;

                int dataI = dataX + dataY * width;

                if (minValueTransparent && d[dataI] == min) {
                    image_bytes[i] = 0x00000000;
                } else {
                    image_bytes[i] = getColour(d[dataI]);
                }
            }

            /* write back image bytes */
            image.setRGB(0, 0, image.getWidth(), image.getHeight(),
                    image_bytes, 0, image.getWidth());

            /* write image */
            String extension = filename.substring(filename.length() - 3);
            ImageIO.write(image, extension, new File(filename));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Only correct when Legend is created with 10 divisions.
     *
     * @param d asc sorted float []
     * @return colour of d after applying cutoff's.
     */
    public int getColour(float d) {
        if (Float.isNaN(d)) {
            return 0xFFFFFFFF; //white
        }
        int pos = java.util.Arrays.binarySearch(cutoffs, d);
        if (pos < 0) {
            pos = (pos * -1) - 1;
        } else {
            //get first instance of this cutoff value
            while (pos > 0 && cutoffs[pos] == cutoffs[pos - 1]) {
                pos--;
            }
        }
        if (divisions != 10) {
            //TODO: fix for mismatch with colours.length
        }
        if (pos >= cutoffs.length) {
            return 0xFFFFFFFF;
        } else {
            double upper = cutoffs[pos];
            int upperPos = pos + 1;
            double lower;
            int lowerPos;
            if (pos == 0) {
                lower = min;
                lowerPos = 0;
            } else {
                lower = cutoffs[pos - 1];
                lowerPos = pos;
            }

            //translate value to 0-1 position between the colours
            if (upper == lower) {
                while (pos > 0 && cutoffs[pos - 1] == lower) {
                    pos--;
                }
                return colours[pos];
            }
            double v = (d - lower) / (upper - lower);
            double vt = 1 - v;


            //there are groups+1 colours
            int red = (int) ((colours[lowerPos] & 0x00FF0000) * vt + (colours[upperPos] & 0x00FF0000) * v);
            int green = (int) ((colours[lowerPos] & 0x0000FF00) * vt + (colours[upperPos] & 0x0000FF00) * v);
            int blue = (int) ((colours[lowerPos] & 0x00000FF) * vt + (colours[upperPos] & 0x000000FF) * v);

            return (red & 0x00FF0000) | (green & 0x0000FF00) | (blue & 0x000000FF) | 0xFF000000;
        }
    }

    /**
     * colourize input between provided ranges.
     *
     * @param d   value to colourize as float
     * @param min minimum of range as float
     * @param max maximum of range as float
     * @return colour of d scaled between min and max as int ARGB with A == 0xFF.
     * Defaults to black.
     */
    public static int getColour(double d, double min, double max) {
        if (Double.isNaN(d) || d < min || d > max) {
            return 0xFFFFFFFF;
        }
        double range = max - min;
        double a = (d - min) / range;

        //10 colour steps
        int pos = (int) (a);  //fit 0 to 10
        if (pos == 10) {
            pos--;
        }
        double lower = (pos / 10.0) * range + min;
        double upper = ((pos + 1) / 10.0) * range + min;

        //translate value to 0-1 position between the colours
        double v = (d - lower) / (upper - lower);
        double vt = 1 - v;

        //there are groups+1 colours
        int red = (int) ((colours[pos] & 0x00FF0000) * vt + (colours[pos + 1] & 0x00FF0000) * v);
        int green = (int) ((colours[pos] & 0x0000FF00) * vt + (colours[pos + 1] & 0x0000FF00) * v);
        int blue = (int) ((colours[pos] & 0x00000FF) * vt + (colours[pos + 1] & 0x000000FF) * v);

        return (red & 0x00FF0000) | (green & 0x0000FF00) | (blue & 0x000000FF) | 0xFF000000;
    }

    public static int getLinearColour(double d, double min, double max, int startColour, int endColour) {
        //translate value to 0-1 position between the colours
        double v = (d - min) / (max - min);
        double vt = 1 - v;

        int red = (int) ((startColour & 0x00FF0000) * vt + (endColour & 0x00FF0000) * v);
        int green = (int) ((startColour & 0x0000FF00) * vt + (endColour & 0x0000FF00) * v);
        int blue = (int) ((startColour & 0x00000FF) * vt + (endColour & 0x000000FF) * v);

        return (red & 0x00FF0000) | (green & 0x0000FF00) | (blue & 0x000000FF) | 0xFF000000;
    }

    /**
     * get cutoff values as String
     * <p/>
     * includes group sizes if calculated
     *
     * @return String of cutoff values
     */
    public String getCutoffs() {
        StringBuffer sb = new StringBuffer();
        //System.out.println(getTypeName());
        for (int i = 0; i < cutoffs.length; i++) {
            if (groupSizes != null && groupSizesArea != null) {
                sb.append(String.valueOf(cutoffs[i])).append("\t").append(String.valueOf(groupSizes[i])).append("\t").append(String.valueOf(groupSizesArea[i])).append("\n");
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

    /**
     * get cutoff's
     *
     * @return cutoff upper segment values as float[] (missing min value)
     */
    public float[] getCutoffFloats() {
        return cutoffs;
    }

    /**
     * @return float[] of [min, max]
     */
    public float[] getMinMax() {
        float[] f = {min, max};
        return f;
    }

    public void exportSLD(Grid g, String outputfilename, String units, boolean hasNoDataValue, boolean minAsTransparent) {
        String header = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<sld:UserStyle xmlns=\"http://www.opengis.net/sld\" xmlns:sld="
                + "\"http://www.opengis.net/sld\" xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:gml=\"http://www.opengis.net/gml\">\n"
                + "  <sld:Name>raster</sld:Name>\n"
                + "<sld:Title>A very simple color map</sld:Title>\n"
                + "  <sld:Abstract>A very basic color map</sld:Abstract>\n"
                + "  <sld:FeatureTypeStyle>\n"
                + "    <sld:Name>name</sld:Name>\n"
                + "    <sld:FeatureTypeName>Feature</sld:FeatureTypeName>\n"
                + "    <sld:Rule>\n"
                + "      <sld:RasterSymbolizer>\n"
                + "        <sld:Geometry>\n"
                + "          <ogc:PropertyName>geom</ogc:PropertyName>\n"
                + "        </sld:Geometry>\n"
                + "        <sld:ChannelSelection>\n"
                + "          <sld:GrayChannel>\n"
                + "            <sld:SourceChannelName>1</sld:SourceChannelName>\n"
                + "          </sld:GrayChannel>\n"
                + "        </sld:ChannelSelection>\n"
                + "        <sld:ColorMap>\n";
        String footer = " </sld:ColorMap>\n"
                + "      </sld:RasterSymbolizer>\n"
                + "    </sld:Rule>\n"
                + "  </sld:FeatureTypeStyle>\n"
                + "</sld:UserStyle>";

        StringBuilder sb = new StringBuilder();

        sb.append(header);

        if (hasNoDataValue) {
            sb.append("\n<sld:ColorMapEntry color=\"#ffffff\" opacity=\"0\" quantity=\""
                    + g.nodatavalue + "\"/>\n");
        }

        String c = String.format("%6s", Integer.toHexString(colours[0])).replace(" ", "0");
        if (minAsTransparent) {
            sb.append("<sld:ColorMapEntry color=\"#" + c + "\" opacity=\"0\" quantity=\"" + min + "\" label=\"" + min + " " + units + "\"/>\n");
        } else {
            sb.append("<sld:ColorMapEntry color=\"#" + c + "\" quantity=\"" + min + "\" label=\"" + min + " " + units + "\"/>\n");
        }

        for (int i = 0; i < cutoffs.length - 1; i++) {
            if ((i == 0 && cutoffs[i] != min)
                    || (i > 0 && cutoffs[i] != cutoffs[i - 1])) {

                c = String.format("%6s", Integer.toHexString(colours[i + 1])).replace(" ", "0");
                sb.append("<sld:ColorMapEntry color=\"#" + c + "\" quantity=\"" + cutoffs[i] + "\" />\n");
            }
        }

        c = String.format("%6s", Integer.toHexString(colours[cutoffs.length])).replace(" ", "0");
        sb.append("<sld:ColorMapEntry color=\"#" + c + "\" quantity=\"" + cutoffs[cutoffs.length - 1] + "\" label=\"" + cutoffs[cutoffs.length - 1] + " " + units + "\"/>\n");

        sb.append(footer);

        try {
            FileWriter fw = new FileWriter(outputfilename);
            fw.append(sb.toString());
            fw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * Produce image legend
     *
     * @param filename output filename.  Must end with .png.
     */
    public void generateLegend(String filename) {
        try {
            String path = Legend.class.getResource(LEGEND_KEY).getFile();
            System.out.println("generating legend using key: " + path + " and producing output at " + filename);
            BufferedImage legendImage = ImageIO.read(new File(path));
            File ciOut = new File(filename);
            Graphics cg = legendImage.getGraphics();
            cg.setColor(Color.BLACK);
            cg.setFont(new Font("Arial", Font.PLAIN, 10));
            String sdata = "";
            int width = legendImage.getWidth();
            int height = legendImage.getHeight();
            int padding = 10; // 10px padding around the image
            int keyHeight = 30; // 30px key height
            int keyWidth = 25; // 30px key width

            width -= padding * 2;
            height -= padding * 2;

            int top = padding + (keyHeight / 2);
            int left = padding * 2 + keyWidth;

            for (int i = 0; i < cutoffs.length; i++) {
                String value = "<= " + cutoffs[i];
                cg.drawString(value, left, top);

                top += keyHeight;
            }

            ImageIO.write(legendImage, "png", ciOut);
        } catch (Exception e) {
            System.out.println("Unable to write legendImage:");
            e.printStackTrace(System.out);
        }

    }

    public float[] getCutoffMinFloats() {
        return this.cutoffMins;
    }

    public void setCutoffs(float[] cutoffs) {
        this.cutoffs = cutoffs;
    }

    public float[] getCutoffMins() {
        return cutoffMins;
    }

    public void setCutoffMins(float[] cutoffMins) {
        this.cutoffMins = cutoffMins;
    }

    public float[] getCutoffsColours() {
        return cutoffsColours;
    }

    public void setCutoffsColours(float[] cutoffsColours) {
        this.cutoffsColours = cutoffsColours;
    }

    public int[] getGroupSizes() {
        return groupSizes;
    }

    public void setGroupSizes(int[] groupSizes) {
        this.groupSizes = groupSizes;
    }

    public int[] getGroupSizesArea() {
        return groupSizesArea;
    }

    public void setGroupSizesArea(int[] groupSizesArea) {
        this.groupSizesArea = groupSizesArea;
    }

    public int getCountOfNaN() {
        return countOfNaN;
    }

    public void setCountOfNaN(int countOfNaN) {
        this.countOfNaN = countOfNaN;
    }

    public float getMin() {
        return min;
    }

    public void setMin(float min) {
        this.min = min;
    }

    public float getMax() {
        return max;
    }

    public void setMax(float max) {
        this.max = max;
    }

    public int getNumberOfRecords() {
        return numberOfRecords;
    }

    public void setNumberOfRecords(int numberOfRecords) {
        this.numberOfRecords = numberOfRecords;
    }

    public int getNumberOfUniqueValues() {
        return numberOfUniqueValues;
    }

    public void setNumberOfUniqueValues(int numberOfUniqueValues) {
        this.numberOfUniqueValues = numberOfUniqueValues;
    }

    public int getLastValue() {
        return lastValue;
    }

    public void setLastValue(int lastValue) {
        this.lastValue = lastValue;
    }

    public int getDivisions() {
        return divisions;
    }

    public void setDivisions(int divisions) {
        this.divisions = divisions;
    }

    public void setCutoffFloats(float[] cutoffs) {
        this.cutoffs = cutoffs;
    }

    public void setCutoffMinFloats(float[] cutoffMins) {
        this.cutoffMins = cutoffMins;
    }

    public void setTypeName(String typeName) {
        //does nothing
    }
}
