package org.ala.spatial.web.zk;

import org.zkoss.zul.*;
import java.util.*;
import org.ala.spatial.util.*;
import org.ala.spatial.util.Grid;
import java.awt.image.*;

public class FilteringZK extends Window {

	final int WIDTH = 1008;
	final int HEIGHT = 840;

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
	float[] active_layer_grid;
	Layer this_layer = null;

	public FilteringZK() {
		int i;
		TabulationSettings.load();

		for (i = 0; i < TabulationSettings.environmental_data_files.length; i++) {
			_layers.add(TabulationSettings.environmental_data_files[i]);
			// get min/max
			Grid grid = new Grid(TabulationSettings.environmental_data_path
					+ TabulationSettings.environmental_data_files[i].name);

			System.out
					.println(TabulationSettings.environmental_data_files[i].name
							+ " (" + grid.minval + " to " + grid.maxval + ")");

			layer_base_minimums.add(new Double(grid.minval));
			layer_base_maximums.add(new Double(grid.maxval));
			layer_minimums.add(new Double(grid.minval));
			layer_maximums.add(new Double(grid.maxval));
		}

		System.out.println("done layer setup");

		// fixed img size 640x480
		base_filter = new int[HEIGHT * WIDTH];
/*
		try {
			image = javax.imageio.ImageIO.read(new java.io.File(
					"/media/2417-58C2/b1-4t.png"));
		} catch (Exception e) {
			System.out.println("err image load: " + e.toString());
		}*/

		// get the load image bytes
		image_bytes = image.getRGB(0, 0, image.getWidth(), image.getHeight(),
				null, 0, image.getWidth());

		// todo - get real images - but do this for now
		loadGrid(TabulationSettings.environmental_data_files[0].name);
		for (i = 0; i < image_bytes.length && i < active_layer_grid.length; i++) {
			if (Double.isNaN(active_layer_grid[i])) {
				image_bytes[i] = 0xff9999FF; // blue
			} else {
				image_bytes[i] = 0xff44ff44; // green
			}
		}

		image.setRGB(0, 0, image.getWidth(), image.getHeight(),
				image_bytes, 0, image.getWidth());

		System.out.println("img: h=" + image.getHeight() + " w="
				+ image.getWidth() + " byteslen=" + image_bytes.length);

		refreshUI();
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

	public void setLayerBounds(Layer layer, double new_min, double new_max) {
		System.out.println("entering setlayerbounds");

		loadGrid(layer.name);

		// get idx of layer
		int layer_idx = 0;
		int i;
		for (i = 0; i < _layers.size(); i++) {
			if (layer.name.equals(((Layer) _layers.get(i)).name)) {
				layer_idx = i;
				break;
			}
		}

		double old_min = ((Double) layer_minimums.get(layer_idx)).doubleValue();
		double old_max = ((Double) layer_maximums.get(layer_idx)).doubleValue();

		if (Double.isNaN(new_max))
			new_max = old_max;
		if (Double.isNaN(new_min))
			new_min = old_min;

		System.out.println("setLayerBounds: " + layer.name + " " + new_min
				+ " " + new_max + " from " + old_min + " " + old_max);

		// 4 cases:
		// 1. new_min lower
		// 2. new_min higher
		// 3. new_max lower
		// 4. new_max higher
		int length = WIDTH * HEIGHT; // do only a little so I can see it if is
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
						image_bytes[i] |= 0xff000000;

					}
				}
			}
		} else if (new_min > old_min) {// show less
			for (i = 0; i < active_layer_grid.length; i++) {
				if (active_layer_grid[i] < new_min
						&& active_layer_grid[i] >= old_min) {
					// reverse bit array
					base_filter[i] |= 0x00000001 << layer_idx;

					// hide image pixel
					image_bytes[i] &= 0x00FFFFFF;
				}
			}
		} else if (new_max < old_max) {// show less
			for (i = 0; i < active_layer_grid.length; i++) {
				if (active_layer_grid[i] > new_max
						&& active_layer_grid[i] <= old_max) {
					// reverse bit array
					base_filter[i] |= 0x00000001 << layer_idx;

					// hide image pixel
					image_bytes[i] &= 0x00FFFFFF;
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
						image_bytes[i] |= 0xff000000;
					}
				}
			}
		}

		// write back new min/max
		layer_minimums.set(layer_idx, new Double(new_min));
		layer_maximums.set(layer_idx, new Double(new_max));

		// write back image bytes
		image.setRGB(0, 0, image.getWidth(), image.getHeight(), image_bytes, 0,
				image.getWidth());

		refreshUI();
	}

	public void onChangeSliderMin() {
		try {
			// get idx of layer
			int layer_idx = 0;
			int i;
			for (i = 0; i < _layers.size(); i++) {
				if (this_layer.name.equals(((Layer) _layers.get(i)).name)) {
					layer_idx = i;
					break;
				}
			}
			double min_value = ((Double) layer_minimums.get(layer_idx))
					.doubleValue();
			double max_value = ((Double) layer_maximums.get(layer_idx))
					.doubleValue();

			double min_base_value = ((Double) layer_base_minimums
					.get(layer_idx)).doubleValue();
			double max_base_value = ((Double) layer_base_maximums
					.get(layer_idx)).doubleValue();

			double range = max_base_value - min_base_value;

			System.out.println("settings min slider");

			double new_min = ((Slider) getFellow("slider_min"))/*, true)) for zk 5.0.0*/
					.getCurpos()
					/ 100.0 * range + min_base_value;

			setLayerBounds(this_layer, new_min, Double.NaN);
		} catch (Exception e) {
			System.out.println("filteringzk: onchangeslidermin" + e.toString());
		}
	}

	public void onChangeSliderMax() {
		try {

			// get idx of layer
			int layer_idx = 0;
			int i;
			for (i = 0; i < _layers.size(); i++) {
				if (this_layer.name.equals(((Layer) _layers.get(i)).name)) {
					layer_idx = i;
					break;
				}
			}
			double min_value = ((Double) layer_minimums.get(layer_idx))
					.doubleValue();
			double max_value = ((Double) layer_maximums.get(layer_idx))
					.doubleValue();

			double min_base_value = ((Double) layer_base_minimums
					.get(layer_idx)).doubleValue();
			double max_base_value = ((Double) layer_base_maximums
					.get(layer_idx)).doubleValue();

			double range = max_base_value - min_base_value;

			double new_max = ((Slider) getFellow("slider_max"))/*,true)) for zk5.0.0*/.getCurpos()
					/ 100.0 * range + min_base_value;

			System.out.println("settings max slider");
			setLayerBounds(this_layer, Double.NaN, new_max);
		} catch (Exception e) {
			System.out.println("filteringzk: onchangeslidermax" + e.toString());
		}
	}

	public void onChangeTextMin() {
		try {

			// get idx of layer
			int layer_idx = 0;
			int i;
			for (i = 0; i < _layers.size(); i++) {
				if (this_layer.name.equals(((Layer) _layers.get(i)).name)) {
					layer_idx = i;
					break;
				}
			}

			double min_base_value = ((Double) layer_base_minimums
					.get(layer_idx)).doubleValue();
			double max_base_value = ((Double) layer_base_maximums
					.get(layer_idx)).doubleValue();

			double new_min = Double
					.parseDouble(((Textbox) getFellow("text_min"))/*,true))for zk 5.0.0*/.getValue());

			// cap
			if (new_min < min_base_value) {
				new_min = min_base_value;
			}

			System.out.println("settings max text");
			setLayerBounds(this_layer, new_min, Double.NaN);
		} catch (Exception e) {
			System.out.println("filteringzk: onchangetextmin" + e.toString());
		}
	}

	public void onChangeTextMax() {
		try {

			// get idx of layer
			int layer_idx = 0;
			int i;
			for (i = 0; i < _layers.size(); i++) {
				if (this_layer.name.equals(((Layer) _layers.get(i)).name)) {
					layer_idx = i;
					break;
				}
			}

			double min_base_value = ((Double) layer_base_minimums
					.get(layer_idx)).doubleValue();
			double max_base_value = ((Double) layer_base_maximums
					.get(layer_idx)).doubleValue();

			double new_max = Double
					.parseDouble(((Textbox) getFellow("text_max"))/*,true)) for zk 5.0.0*/.getValue());

			// cap
			if (new_max > max_base_value) {
				new_max = max_base_value;
			}

			System.out.println("settings max text");
			setLayerBounds(this_layer, Double.NaN, new_max);
		} catch (Exception e) {
			System.out.println("filteringzk: onchangetextmax" + e.toString());
		}
	}

	public void refreshUI() {
		try {
			Listbox lb = (Listbox) getFellow("lb");/*,true)for zk5.0.0*/
			this_layer = (Layer) lb.getSelectedItem().getValue();
			((Textbox) getFellow("name"))/*,true)) for zk5.0.0*/.setValue(this_layer.display_name);
			((Textbox) getFellow("desc"))/*,true)) for zk5.0.0*/.setValue(this_layer.description);
			// sliders upper & lower bounds

			// get idx of layer
			int layer_idx = 0;
			int i;
			for (i = 0; i < _layers.size(); i++) {
				if (this_layer.name.equals(((Layer) _layers.get(i)).name)) {
					layer_idx = i;
					break;
				}
			}
			double min_value = ((Double) layer_minimums.get(layer_idx)).doubleValue();
			double max_value = ((Double) layer_maximums.get(layer_idx)).doubleValue();

			double min_base_value = ((Double) layer_base_minimums.get(layer_idx)).doubleValue();
			double max_base_value = ((Double) layer_base_maximums.get(layer_idx)).doubleValue();

			double range = max_base_value - min_base_value;

			System.out.println("about to set sliders: range=" + range + " max="
					+ (int) ((max_value - min_base_value) / (range) * 100)
					+ " (" + max_value + ") ");
			System.out.println("min_base=" + min_base_value
					+ " max_base_value=" + max_base_value);
			System.out.println("min="
					+ (int) ((min_value - min_base_value) / (range) * 100)
					+ " (" + min_value + ") ");
			((Slider) getFellow("slider_max"))/*,true)) for zk5.0.0*/.setCurpos((int) ((max_value - min_base_value) / (range) * 100));
			((Slider) getFellow("slider_min"))/*,true)) for zk5.0.0*/.setCurpos((int) ((min_value - min_base_value) / (range) * 100));

			((Textbox) getFellow("text_max"))/*,true)) for zk5.0.0*/.setValue(String.format("%.2f",  max_value));
			((Textbox) getFellow("text_min"))/*,true)) for zk5.0.0*/.setValue(String.format("%.2f", min_value));

		} catch (Exception e) {
			System.out.println("get box stuff: " + e.toString());
		}

		// get all fellows
		/*
		 * try{ Object [] objects = getFellows().toArray(); for(int
		 * i=0;i<objects.length;i++){ System.out.println("fellow (" + i + ")=" +
		 * objects[i].toString()); } }catch(Exception e){
		 * System.out.println("fellows error: " + e.toString()); }
		 */

		// apply the image
		try {
			Image img = (Image) getFellow("im");/*, true); for zk5.0.0*/
			img.setContent(image);
		} catch (Exception e) {
			System.out.println("img set error: " + e.toString());
		}
	}

	public List getLayers() {
		return _layers;
	}

	public void setLayers(List layers) {
		_layers = layers;
	}

	public String getLayerRange(String layername){
		// get idx of layer
		int layer_idx = 0;
		int i;
		for (i = 0; i < _layers.size(); i++) {
			if (this_layer.name.equals(((Layer) _layers.get(i)).name)) {
				layer_idx = i;
				break;
			}
		}
		double min_value = ((Double) layer_minimums.get(layer_idx)).doubleValue();
		double max_value = ((Double) layer_maximums.get(layer_idx)).doubleValue();

		return String.format("%.2f to %.2f", min_value, max_value);
	}

}
