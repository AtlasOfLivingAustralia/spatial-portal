package org.ala.spatial.analysis.tabulation;

import java.util.*;
import org.ala.spatial.analysis.*;
import org.ala.spatial.util.*;

import java.awt.image.*;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import javax.imageio.ImageIO;

public class FilteringImage implements Serializable {

    private static final long serialVersionUID = -2431102028701859952L;

    final int WIDTH = 252;
    final int HEIGHT = 210;
    // List _resorts = new ArrayList();
    List _layers = new ArrayList();
    List layer_minimums = new ArrayList();
    List layer_maximums = new ArrayList();
    List layer_base_minimums = new ArrayList();
    List layer_base_maximums = new ArrayList();
    // base filter list
    int[] base_filter;
    // img, fixed size
    BufferedImage image = new BufferedImage(WIDTH, HEIGHT,
            BufferedImage.TYPE_4BYTE_ABGR);
    int[] image_bytes;
    // to remember last layer being worked on
    String active_layer_name = "";
    double[] active_layer_grid;
    Layer this_layer = null;
    String filename;

    public FilteringImage() {
    }

    

    public FilteringImage(String filename_) {
        filename = filename_;

        int i;
        TabulationSettings.load();

        for (i = 0; i < TabulationSettings.environmental_data_files.length; i++) {
            _layers.add(TabulationSettings.environmental_data_files[i]);
            // get min/max
            Grid grid = new Grid(TabulationSettings.environmental_data_path
                    + TabulationSettings.environmental_data_files[i].name);

            System.out.println(TabulationSettings.environmental_data_files[i].name
                    + " (" + grid.minval + " to " + grid.maxval + ")");

            layer_base_minimums.add(new Double(grid.minval));
            layer_base_maximums.add(new Double(grid.maxval));
            layer_minimums.add(new Double(grid.minval));
            layer_maximums.add(new Double(grid.maxval));
        }

        for (i = 0; i < TabulationSettings.geo_tables.length; i++) {
            _layers.add(TabulationSettings.geo_tables[i]);
        }

        base_filter = new int[HEIGHT * WIDTH];

        // get the load image bytes
        image_bytes = image.getRGB(0, 0, image.getWidth(), image.getHeight(),
                null, 0, image.getWidth());

        //use missing value (transparent, 0x00000000)
        for (i = 0; i < image_bytes.length; i++) {
            image_bytes[i] = 0xFF00FF00;
        }

        image.setRGB(0, 0, image.getWidth(), image.getHeight(),
                image_bytes, 0, image.getWidth());

        writeImage();

        //test colour change on half
        for (i = 0; i < image_bytes.length / 2; i++) {
            image_bytes[i] = 0xFF0000FF;
        }

        image.setRGB(0, 0, image.getWidth(), image.getHeight(),
                image_bytes, 0, image.getWidth());

        writeImage();
    }

    private void writeImage() {

        try {
            ImageIO.write(image, "png",
                    new File(filename));
            System.out.println("writing image");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void loadGrid(String layername) {
        if (active_layer_name != null && !active_layer_name.equals(layername)) {
            // load a grid
            System.out.println("loading a grid: " + layername);

            Grid grid = new Grid(TabulationSettings.environmental_data_path
                    + layername);
            active_layer_grid = null;
            if (grid.datatype == "FLOAT" || grid.datatype == "DOUBLE") {
                active_layer_grid = grid.getGrid();
            }

            active_layer_name = layername;
        }
    }

    public SPLFilter applyFilter(String layername, double new_min_, double new_max_) {
        Layer layer = null;
        Iterator it = _layers.iterator();
        int layer_idx = 0;
        while(it.hasNext()) {
            Layer tl = (Layer) it.next();
            if (tl.display_name.equalsIgnoreCase(layername)) {
                layer = tl;
                break;
            }
            layer_idx++;
        }
        applyFilter(layer_idx, new_min_, new_max_);
        return SpeciesListIndex.getLayerFilter(layer);
    }

    /* new min/max is 0 to 1, or negative for */
    public void applyFilter(int layer_idx, double new_min_, double new_max_) {
        System.out.println("applyFilter(" + layer_idx + "," + new_min_ + "," + new_max_);
        /* convert layer name to TabulationSettings.Layers
         *
         * name */
        Layer layer = (Layer) _layers.get(layer_idx);

        System.out.println("Applying layer to filter: " + layer.display_name); 
        loadGrid(layer.name);

        //rescale new_min/new_max to layer extents (or below & over if required)
        double lmin = ((Double) layer_base_minimums.get(layer_idx)).doubleValue();
        double lmax = ((Double) layer_base_maximums.get(layer_idx)).doubleValue();
        double new_min = new_min_ * (lmax - lmin) + lmin;
        double new_max = new_max_ * (lmax - lmin) + lmin;

        double old_min = ((Double) layer_minimums.get(layer_idx)).doubleValue();
        double old_max = ((Double) layer_maximums.get(layer_idx)).doubleValue();

        System.out.println(">" + new_min + "," + new_max + " old=" + old_min + "," + old_max);

        if (Double.isNaN(new_max)) {
            new_max = old_max;
        }
        if (Double.isNaN(new_min)) {
            new_min = old_min;
        }

        System.out.println("setLayerBounds: " + layer.name + " " + new_min
                + " " + new_max + " from " + old_min + " " + old_max);

        // 4 cases:
        // 1. new_min lower
        // 2. new_min higher
        // 3. new_max lower
        // 4. new_max higher
        int length = WIDTH * HEIGHT; // do only a little so I can see it if is

        int i;

        int count1 = 0;
        int count2 = 0;

        //handle 0/1 case
        if (new_min_ <= 0 && new_min_ >= -0.01 && new_max_ == 1) {
            System.out.println("0-1 case");
            for (i = 0; i < active_layer_grid.length; i++) {
                if (Double.isNaN(active_layer_grid[i])) {	//missing value, hide
                    // reverse bit array
                    base_filter[i] |= 0x00000001 << layer_idx;

                    // hide image pixel
                    image_bytes[i] = 0xFFFFFFFF; //white

                    count2++;
                } else { 									//show
                    base_filter[i] &= ~(0x00000001 << layer_idx);

                    if (base_filter[i] == 0) {
                        // unhide image pixel
                        image_bytes[i] = 0x00000000; //transparent
                    }
                    count1++;
                }
            }
        } else if (new_min_ < -0.01) {	//handle -?/? case
            System.out.println("- case");
            //show all
            for (i = 0; i < active_layer_grid.length; i++) {
                //show
                base_filter[i] &= ~(0x00000001 << layer_idx);

                if (base_filter[i] == 0) {
                    // unhide image pixel
                    image_bytes[i] = 0x00000000; //transparent
                }
                count1++;
            }
        } else {
            // working;
            if (new_min < old_min) {// show more
                for (i = 0; i < active_layer_grid.length; i++) {
                    if (active_layer_grid[i] >= new_min
                            && active_layer_grid[i] < old_min) {
                        // reverse bit array (hopefully it was correct in before
                        // here)
                        base_filter[i] &= ~(0x00000001 << layer_idx);

                        if (base_filter[i] == 0) {
                            // unhide image pixel
                            image_bytes[i] = 0x00000000; //transparent
                        }
                        count1++;
                    }
                }
            } else if (new_min > old_min) {// show less
                for (i = 0; i < active_layer_grid.length; i++) {
                    if (active_layer_grid[i] < new_min
                            && active_layer_grid[i] >= old_min) {
                        // reverse bit array
                        base_filter[i] |= 0x00000001 << layer_idx;

                        // hide image pixel
                        image_bytes[i] = 0xFFFFFFFF; //white
                        count2++;
                    }
                }
            } 
            if (new_max < old_max) {// show less
                for (i = 0; i < active_layer_grid.length; i++) {
                    if (active_layer_grid[i] > new_max
                            && active_layer_grid[i] <= old_max) {
                        // reverse bit array
                        base_filter[i] |= 0x00000001 << layer_idx;

                        // hide image pixel
                        image_bytes[i] = 0xFFFFFFFF; //white
                        count2++;
                    }
                }
            } else if (new_max > old_max) {// show more
                for (i = 0; i < active_layer_grid.length; i++) {
                    if (active_layer_grid[i] <= new_max
                            && active_layer_grid[i] > old_max) {
                        // reverse bit array (hopefully it was correct in before
                        // here)
                        base_filter[i] &= ~(0x00000001 << layer_idx);

                        if (base_filter[i] == 0) {
                            // unhide image pixel
                            image_bytes[i] = 0x00000000; //transparent
                            count1++;
                        }


                    }
                }
            }
        }

        System.out.println("#hide count=" + count2);
        System.out.println("#show count=" + count1);

        // write back new min/max
        layer_minimums.set(layer_idx, new Double(new_min));
        layer_maximums.set(layer_idx, new Double(new_max));

        // write back image bytes
        image.setRGB(0, 0, image.getWidth(), image.getHeight(), image_bytes, 0,
                image.getWidth());

        writeImage();
    }

    public SPLFilter applyFilterCtx(String layername, int value, boolean show) {
        Layer layer = null;
        Iterator it = _layers.iterator();
        int layer_idx = 0;
        while(it.hasNext()) {
            Layer tl = (Layer) it.next();
            if (tl.display_name.equalsIgnoreCase(layername)) {
                layer = tl;
                break;
            }
            layer_idx++;
        }
        applyFilterCtx(layer_idx, value, show);
        return SpeciesListIndex.getLayerFilter(layer);
    }

    /*
     * value = -1 for all values
     */
    public void applyFilterCtx(int layer_idx, int value, boolean show) {
        System.out.println("entering setLayerCtx(" + layer_idx + "," + value + ")");

        Layer layer = (Layer) _layers.get(layer_idx);

        // 4 cases:
        // 1. new_min lower
        // 2. new_min higher
        // 3. new_max lower
        // 4. new_max higher
        int length = WIDTH * HEIGHT; // do only a little so I can see it if is
        // working;

        int idx_start;
        int idx_end;
        int i;

        //get layer data: TODO replace the get grid bit
        ImageShort[] is = SpeciesListIndex.getImageData(layer, 112, 154, -9, -44, 252, 210);

        System.out.println("layerdatlen=" + is.length);

        if (value < 0) {
            idx_start = 0;
            idx_end = is.length;
        } else {
            //value adjust
            value += is[0].value;
            value++;

            for (idx_start = 0; idx_start < is.length && value != is[idx_start].value; idx_start++) {
                //seeking, make binary search
            }
            idx_end = idx_start;
            for (; idx_end < is.length && value == is[idx_end].value; idx_end++) {
                //seeking, make binary search
            }
        }
        System.out.println("idx_start=" + idx_start + " idx_end=" + idx_end);

        int count1 = 0;
        int count2 = 0;
        int p;

        if (show) {// show more
            for (p = idx_start; p < idx_end; p++) {
                i = WIDTH * (HEIGHT - is[p].y - 1) + is[p].x;

                base_filter[i] &= ~(0x00000001 << layer_idx);

                if (base_filter[i] == 0) {
                    // unhide image pixel
                    image_bytes[i] = 0x00000000; //transparent
                    count1++;
                }
            }
        } else {//hide more
            for (p = idx_start; p < idx_end; p++) {
                i = WIDTH * (HEIGHT - is[p].y - 1) + is[p].x;

                // reverse bit array
                base_filter[i] |= 0x00000001 << layer_idx;

                // hide image pixel
                image_bytes[i] = 0xFFFFFFFF; //white
                count2++;
            }
        }

        System.out.println("#hide count=" + count2);
        System.out.println("#show count=" + count1);

        // write back image bytes
        image.setRGB(0, 0, image.getWidth(), image.getHeight(), image_bytes, 0,
                image.getWidth());

        writeImage();
    }
}
