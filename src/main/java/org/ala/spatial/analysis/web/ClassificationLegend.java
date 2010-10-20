package org.ala.spatial.analysis.web;

import au.org.emii.portal.composer.MapComposer;

import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.settings.SettingsSupplementary;
import java.net.URLEncoder;
import java.util.List;
import java.awt.Color;
import java.net.URLDecoder;
import org.ala.spatial.util.LegendMaker;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Page;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.ListitemRenderer;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Image;
import org.zkoss.zul.Button;
import org.zkoss.zul.Div;
import org.zkoss.zul.Slider;
import java.util.ArrayList;
import org.ala.spatial.util.CommonData;

import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;

public class ClassificationLegend extends UtilityComposer {

    private SettingsSupplementary settingsSupplementary = null;
    String pid = "";
    String layerLabel = "";
    String imagePath = "";
    MapComposer mc;
    private String satServer = "";
    public Slider redSlider;
    public Slider greenSlider;
    public Slider blueSlider;
    public int colours_index;
    public Button applyChange;
    public Listcell legend_cell;
    public Image sampleColour;
    public int legend_counter = 0;
    public Div colourChooser;
    public Listbox legend;
    ArrayList<String> legend_lines;

    @Override
    public void afterCompose() {
        super.afterCompose();

        mc = getThisMapComposer();
        if (settingsSupplementary != null) {
            satServer = settingsSupplementary.getValue(CommonData.SAT_URL);
        }

        pid = (String) (Executions.getCurrent().getArg().get("pid"));
        System.out.println("PID:" + pid);
        layerLabel = (String) (Executions.getCurrent().getArg().get("layer"));
        layerLabel = URLDecoder.decode(layerLabel);
        System.out.println("layer:" + layerLabel);

        buildLegend();
    }

    void buildLegend() {
        try {
            // call get
            StringBuffer sbProcessUrl = new StringBuffer();
            sbProcessUrl.append(satServer + "/alaspatial/ws/layer/get?");
            sbProcessUrl.append("pid=" + URLEncoder.encode(pid, "UTF-8"));

            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(sbProcessUrl.toString());

            get.addRequestHeader("Accept", "text/plain");

            int result = client.executeMethod(get);
            String slist = get.getResponseBodyAsString();

            // retrieve legend file
            String[] slista = slist.split("\n");

            client = new HttpClient();
            get = new GetMethod(satServer + "/alaspatial/" + slista[1]);
            get.addRequestHeader("Accept", "text/plain");
            result = client.executeMethod(get);
            slist = get.getResponseBodyAsString();

            String[] lines = slist.split("\r\n");
            legend_lines = new ArrayList<String>();
            int i = 0;
            for (i = 1; i < lines.length; i++) {
                legend_lines.add(lines[i]);
            }
  
            /* apply something to line onclick in lb */
            legend.setItemRenderer(new ListitemRenderer() {

                public void render(Listitem li, Object data) {
                    String s = (String) data;
                    String[] ss = s.split(",");
                    Listcell lc = new Listcell("group " + ss[0]);
                    lc.setParent(li);

                    int red = Integer.parseInt(ss[1]);
                    int green = Integer.parseInt(ss[2]);
                    int blue = Integer.parseInt(ss[3]);

                    lc = new Listcell(ss[0]);
                    lc.setStyle("background-color: rgb(" + red + "," + green
                            + "," + blue + "); color: rgb(" + red + "," + green
                            + "," + blue + ")");
                    lc.setParent(li);
                    lc.addEventListener("onClick", new EventListener() {

                        public void onEvent(Event event) throws Exception {
                            // open colours selector
                            openColours((Listcell) event.getTarget());
                        }
                    });
                }
            });

            legend.setModel(new SimpleListModel(legend_lines));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void openColours(Listcell lc) {
        String name = lc.getLabel();
        legend_cell = lc;

        // find in legend list
        int i = 0;
        for (i = 0; i < legend_lines.size(); i++) {
            if (legend_lines.get(i).startsWith(name + ",")) {
                break;
            }
        }
        String[] la = legend_lines.get(i).split(",");
        int red = Integer.parseInt(la[1]);
        int green = Integer.parseInt(la[2]);
        int blue = Integer.parseInt(la[3]);

        colours_index = i;

        redSlider.setCurpos(red);
        greenSlider.setCurpos(green);
        blueSlider.setCurpos(blue);

        sliderChange();

        colourChooser.setVisible(true);
    }

    public void onClick$applyChange() {
        System.out.println("applyChange");
        int red = redSlider.getCurpos();
        int green = greenSlider.getCurpos();
        int blue = blueSlider.getCurpos();
        String[] la = legend_lines.get(colours_index).split(",");
        la[1] = "" + red;
        la[2] = "" + green;
        la[3] = "" + blue;
        String las = la[0];
        for (int i = 1; i < la.length; i++) {
            las += "," + la[i];
        }
        legend_lines.set(colours_index, las);
        legend_cell.setValue(las);
        legend_cell.setStyle("background-color: rgb(" + red + "," + green
                + "," + blue + "); color: rgb(" + red + "," + green
                + "," + blue + ")");

        // service call to change map layer
        // pid
        // colours_index
        // red, green blue
        //
        System.out.println("changing colours: " + red + " " + green + " "
                + blue + " @" + colours_index);
        try {
            // call get
            StringBuffer sbProcessUrl = new StringBuffer();
            sbProcessUrl.append(satServer + "/alaspatial/ws/layer/set?");
            sbProcessUrl.append("pid=" + URLEncoder.encode(pid, "UTF-8"));
            sbProcessUrl.append("&idx="
                    + URLEncoder.encode("" + colours_index, "UTF-8"));
            sbProcessUrl.append("&red=" + URLEncoder.encode("" + red, "UTF-8"));
            sbProcessUrl.append("&green="
                    + URLEncoder.encode("" + green, "UTF-8"));
            sbProcessUrl.append("&blue="
                    + URLEncoder.encode("" + blue, "UTF-8"));

            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(sbProcessUrl.toString());

            get.addRequestHeader("Accept", "text/plain");

            int result = client.executeMethod(get);
            String slist = get.getResponseBodyAsString();
            System.out.println("updated layer image:" + slist);
            imagePath = satServer + "/alaspatial/" + slist.split("\r\n")[0];
            loadMap();
        } catch (Exception e) {
            e.printStackTrace();
        }

        colourChooser.setVisible(false);
    }

    public void onScroll$redSlider(Event event) {
        sliderChange();
    }

    public void onScroll$greenSlider(Event event) {
        sliderChange();
    }

    public void onScroll$blueSlider(Event event) {
        sliderChange();
    }

    void sliderChange() {
        int red = redSlider.getCurpos();
        int green = greenSlider.getCurpos();
        int blue = blueSlider.getCurpos();
        LegendMaker lm = new LegendMaker();
        Color c = new Color(red, green, blue);
        sampleColour.setContent(lm.singleRectImage(c, 50, 50, 45, 45));
    }

    /**
     * Gets the main pages controller so we can add a
     * layer to the map
     * @return MapComposer = map controller class
     */
    private MapComposer getThisMapComposer() {
        MapComposer mapComposer = null;
        Page page = getPage();
        mapComposer = (MapComposer) page.getFellow("mapPortalPage");

        return mapComposer;
    }

    private void loadMap() {
        float opacity = Float.parseFloat("0.75");

        List<Double> bbox = new ArrayList<Double>();
        bbox.add(112.0);
        bbox.add(-44.0000000007);
        bbox.add(154.00000000084);
        bbox.add(-9.0);

        mc.addImageLayer(pid, layerLabel, imagePath, opacity, bbox);

    }
}
