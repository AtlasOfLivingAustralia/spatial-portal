package org.ala.spatial.analysis.web;

import au.com.bytecode.opencsv.CSVReader;
import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.settings.SettingsSupplementary;
import au.org.emii.portal.util.LayerUtilities;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ala.spatial.data.*;
import org.ala.spatial.util.CommonData;
import org.ala.spatial.util.SelectedArea;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.SuspendNotAllowedException;
import org.zkoss.zk.ui.event.CheckEvent;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.ForwardEvent;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.*;

public class AddFacetController extends UtilityComposer {

    String winTop = "300px";
    String winLeft = "500px";
    Map<String, Object> params;
    int currentStep = 1, totalSteps = 2;
    String selectedMethod = "";
    Button btnCancel, btnOk, btnBack, clearSelection;
    int SLIDER_MAX = 500;
    String pid;
    String imagePath = "";
    public Listbox legend;
    Button createInGroup;
    ArrayList<String> legend_lines;
    Rangeslider dslider;
    Doublebox dmin;
    Doublebox dmax;
    double minValue;
    double maxValue;
    double gMinValue;
    double gMaxValue;
    Label dlabel;
    Div divContinous;
    Checkbox dunknown;
    Button dbutton;
    boolean intContinous = false;
    boolean isMonth = false;
    boolean isNumber = false;
    boolean disableselection = false;
    HashMap<String, String> legend_facets = null;
    Facet facet;
    Div dCreateButtons;
    Listheader lhFirstColumn;
    Listheader lhSecondColumn;
    Listheader lhThirdColumn;
    Listheader lhFourthColumn;
    Combobox cbColour;
    SettingsSupplementary settingsSupplementary;
    boolean hasCustomArea = false;
    Radiogroup rgArea, rgAreaHighlight, rgSpecies, rgSpeciesBk;
    //SelectedArea sa = new SelectedArea(null, CommonData.AUSTRALIA_WKT);
    //SelectedArea sa = new SelectedArea(null,CommonData.WORLD_WKT);
    SelectedArea sa;
    Query query;
    Map map = new HashMap();
    String colourmode;
    boolean readonly = true;
    boolean checkmarks = true;
    MapComposer mc = getMapComposer();
    Radio rAreaWorld, rAreaCustom, rAreaSelected, rAreaAustralia;
    MapLayer prevTopArea = null;
    Checkbox chkGeoKosherTrue, chkGeoKosherFalse;
    LegendObject lo = null;

    @Override
    public void afterCompose() {
        super.afterCompose();
        winTop = this.getTop();
        winLeft = this.getLeft();
        setupDefaultParams();
        setParams(Executions.getCurrent().getArg());
        selectedMethod = "Add facet";
        updateWindowTitle();
        fixFocus();
        loadAreaLayers();
        if (rgArea.getSelectedItem() != null) {
            btnOk.setDisabled(false);
        } else {
            btnOk.setDisabled(true);
        }
    }

    private void setupDefaultParams() {
        Hashtable<String, Object> p = new Hashtable<String, Object>();
        p.put("step1", "Select area");
        p.put("step2", "Select facet");

        if (params == null) {
            params = p;
        } else {
            setParams(p);
        }

        Div currentDiv = (Div) getFellowIfAny("atstep" + currentStep);
        //btnOk.setLabel("Next >");
        btnOk.setDisabled(true);
    }

    public void setParams(Map<String, Object> params) {
        // iterate thru' the passed params and load them into the
        // existing default params
        if (params == null) {
            setupDefaultParams();
        }
        if (params != null && params.keySet() != null && params.keySet().iterator() != null) {
            Iterator<String> it = params.keySet().iterator();
            while (it.hasNext()) {
                String key = it.next();
                this.params.put(key, params.get(key));
            }
        } else {
            this.params = params;
        }
    }

    public void updateWindowTitle() {
        this.setTitle("Step " + currentStep + " of " + totalSteps + " - " + selectedMethod);
    }

    void fixFocus() {
        switch (currentStep) {
            case 1:
                rgArea.setFocus(true);
                rgArea.setSelectedItem(rAreaWorld);
                break;
            case 2:
                cbColour.setFocus(true);
                break;

        }
    }

    public void onCheck$rgArea(Event event) {
        if (rgArea == null) {
            return;
        }
        //setCustomArea = false;
        hasCustomArea = false;
        rAreaSelected = rgArea.getSelectedItem();
        try {
            rAreaSelected = (Radio) ((org.zkoss.zk.ui.event.ForwardEvent) event).getOrigin().getTarget();
        } catch (Exception e) {
        }
        if (rAreaSelected == rAreaCustom) {
            //setCustomArea = true;
            hasCustomArea = false;
        }
        btnOk.setDisabled(false);
    }

    public SelectedArea getSelectedArea() {
        String area = rAreaSelected.getValue();
        SelectedArea selectedarea = null;
        try {
            if (area.equals("current")) {
                selectedarea = new SelectedArea(null, getMapComposer().getViewArea());
            } else if (area.equals("australia")) {
                selectedarea = new SelectedArea(null, CommonData.AUSTRALIA_WKT);
            } else if (area.equals("world")) {
                selectedarea = new SelectedArea(null, CommonData.WORLD_WKT);
            } else {
                List<MapLayer> layers = getMapComposer().getPolygonLayers();
                for (MapLayer ml : layers) {
                    if (area.equals(ml.getWKT())) {
                        selectedarea = new SelectedArea(ml, null);
                        break;
                    }
                }

                //for 'all areas'
                if (selectedarea == null) {
                    selectedarea = new SelectedArea(null, area);
                }
            }
        } catch (Exception e) {
            logger.error("Unable to retrieve selected area", e);
        }

        return selectedarea;
    }

    public void onSelect$cbColour(Event event) {
        colourmode = (String) cbColour.getSelectedItem().getValue();
        try {
            lo = ((Query) query).getLegend(colourmode);
        } catch (Exception e) {
            e.printStackTrace();
        }

        map.put("colourmode", colourmode);

        pid = (String) (Executions.getCurrent().getArg().get("pid"));

        //disableselection = (Executions.getCurrent().getArg().get("disableselection")) != null;
        //if(disableselection) {
        //   dCreateButtons.setVisible(false);
        //    lhFirstColumn.setWidth("0px");
        //   lhSecondColumn.setWidth("190px");
        //    lhThirdColumn.setWidth("15px");
        //   lhThirdColumn.setLabel("");
        //   lhFourthColumn.setWidth("50px");
        //   legend.setWidth("280px");
        //}
        buildLegend();
    }

    void buildLegend() {
        try {
            String slist = null;

            if (query != null) {
                if (colourmode.equals("grid")) {
                    slist = "name,red,green,blue,count";
                    for (int i = 0; i < 600; i += 100) {
                        if (i == 1) {
                            slist += ">";
                        } else {
                            slist += ">";
                        }

                        slist += "\n" + i + "," + LegendObject.getRGB(Legend.getLinearColour(i, 0, 500, 0xFFFFFF00, 0xFFFF0000)) + ",";
                    }
                } else {
                    slist = query.getLegend(colourmode).getTable();
                }
            } else {
                return;
            }

            String[] lines = slist.split("\r\n");
            if (lines.length == 1) {
                lines = slist.split("\n");
            }
            legend_lines = new ArrayList<String>();
            int i = 0;
            for (i = 1; i < lines.length; i++) {
                if (lines[i].split(",").length > 3) {
                    legend_lines.add(lines[i]);
                }
            }

            String h = "";
            facet = null;

            //test for range (user upload)
            if (legend_lines.size() > 1) {
                String first = legend_lines.get(0);
                if (first == null || first.length() == 0 || first.startsWith("Unknown")) {
                    first = legend_lines.get(1);
                }
                if (!checkmarks && query.getLegend(colourmode) != null
                        && query.getLegend(colourmode).getNumericLegend() != null) {
                    setupForNumericalList(first, h);
                    //test for manual range (solr query)
                } else if (colourmode.equals("occurrence_year")) {
                    setupForBiocacheNumber(h, colourmode, true);
                } else if (colourmode.equals("occurrence_year_decade")) {
                    setupForBiocacheDecade();
                } else if (colourmode.equals("coordinate_uncertainty")) {
                    setupForBiocacheNumber(h, colourmode, false);
                } else if (colourmode.equals("month")) {
                    setupForBiocacheMonth();
                }
            }

            if("geospatial_kosher".equals(colourmode)){
                setupForBiocacheGeospatialKosher();
            }

            /* apply something to line onclick in lb */
            legend.setItemRenderer(new ListitemRenderer() {

                @Override
                public void render(Listitem li, Object data) {
                    String[] ss = null;
                    try {
                        CSVReader csv = new CSVReader(new StringReader((String) data));
                        ss = csv.readNext();
                        csv.close();
                    } catch (IOException ex) {
                        Logger.getLogger(ClassificationLegend.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    if (ss == null) {
                        return;
                    }

                    Checkbox cb = null;

                    if (checkmarks) {
                        cb = new Checkbox();
                        cb.addEventListener("onCheck", new EventListener() {

                            @Override
                            public void onEvent(Event event) throws Exception {
                                checkboxClick(event);
                                btnOk.setDisabled(false);
                            }
                        });
                        //determineCheckboxState(cb, ss[0]);
                    }

                    Listcell lc;
                    lc = new Listcell();
                    if (cb != null) {
                        cb.setParent(lc);
                        //Do not display checkboxes for facets that are not simple
                        cb.setVisible(!disableselection && !ss[0].endsWith(" more"));
                    }
                    lc.setParent(li);

                    if (readonly) {
                        lc = new Listcell(actualToDisplayLabel(ss[0]));
                    } else {
                        lc = new Listcell("group " + ss[0]);
                    }
                    lc.setParent(li);

                    int red = Integer.parseInt(ss[1]);
                    int green = Integer.parseInt(ss[2]);
                    int blue = Integer.parseInt(ss[3]);

                    lc = new Listcell("   ");
                    lc.setStyle("background-color: rgb(" + red + "," + green
                            + "," + blue + "); color: rgb(" + red + "," + green
                            + "," + blue + ")");
                    lc.setParent(li);

                    //count
                    try {
                        lhFourthColumn.setVisible(true);
                        lc = new Listcell(ss[4]);
                        lc.setParent(li);
                    } catch (Exception e) {
                        lhFourthColumn.setVisible(false);
                    }
                }
            });


            legend.setModel(new SimpleListModel(legend_lines));
            legend.renderAll();

            //if (divContinous.isVisible()) {
            //    int[] state = getState();
            //    if (state[0] > 1) {
            //        setEnableContinousControls(false);
            //    } else {
            //        updateD();
            //    }
            //}
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    String getSelectionFacet() {
        StringBuilder values = new StringBuilder();

        boolean unknown = false;
        for (Listitem li : (List<Listitem>) legend.getItems()) {
            if (li.getFirstChild().getChildren().size() > 0
                    && ((Checkbox) li.getFirstChild().getFirstChild()).isChecked()) {
                String v = ((Listcell) li.getChildren().get(1)).getLabel();
                v = displayToActualLabel(v);
                if (legend_facets != null) {
                    if (v.equals("Unknown") || v.contains("occurrence_year") || v.contains("uncertainty")) {
                        //keep unchanged
                        divContinous.setVisible(true);
                    } else {
                        v = legend_facets.get(v);
                    }
                }

                if (v.length() == 0 || ((query instanceof BiocacheQuery || divContinous.isVisible()) && v.equals("Unknown"))) {
                    unknown = true;
                } else {
                    if (values.length() > 0) {
                        values.append(" OR ");
                    }
                    if (legend_facets != null) {
                        values.append(v);
                    } else {
                        values.append(colourmode).append(":\"");
                        values.append(v).append("\"");
                    }
                }
            }
        }
        String unknownSearch = divContinous.isVisible() ? ":[* TO *]" : ":*";
        if (unknown) {
            if (values.length() > 0) {
                String newValues = "-(" + colourmode + unknownSearch + " AND -" + values.toString().replace(" OR ", " AND -") + ")";
                values = new StringBuilder();
                values.append(newValues);
            } else {
                values.append("-").append(colourmode).append(unknownSearch);
            }
        }

        if (values.length() == 0) {
            if (minValue == gMinValue && maxValue == gMaxValue && dunknown.isChecked()) {
                //no facet
            } else {
                if (dunknown.isChecked()) {
                    if (intContinous) {
                        values.append(String.format("-(%s:[* TO *] AND -%s:[%d TO %d])", colourmode, colourmode, (int) minValue, (int) maxValue));
                    } else {
                        values.append(String.format("-(%s:[* TO *] AND -%s:[%f TO %f])", colourmode, colourmode, minValue, maxValue));
                    }
                } else {
                    if (intContinous) {
                        values.append(String.format("%s:[%d TO %d]", colourmode, (int) minValue, (int) maxValue));
                    } else {
                        values.append(String.format("%s:[%f TO %f]", colourmode, minValue, maxValue));
                    }
                }
            }
        }
        if (values.length() > 0) {
            facet = Facet.parseFacet(values.toString());
            return values.toString();
        } else {
            facet = null;
            return "";
        }
    }

    public void onScroll$dslider(Event event) {
        if (!dmax.isDisabled()) {    //slider cannot be disabled
            uncheckAll();
            double range = gMaxValue - gMinValue;
            minValue = dslider.getCurpos() * range / (double) SLIDER_MAX + gMinValue;
            maxValue = dslider.getCurmaxpos() * range / (double) SLIDER_MAX + gMinValue;
            //updateD();
        }
    }

    public void onOK$dmin(Event event) {
        uncheckAll();
        minValue = dmin.getValue();
        //updateD();
    }

    public void onChange$dmin(Event event) {
        uncheckAll();
        minValue = dmin.getValue();
        //updateD();
    }

    public void onOK$dmax(Event event) {
        uncheckAll();
        maxValue = dmax.getValue();
        //updateD();
    }

    public void onChange$dmax(Event event) {
        uncheckAll();
        maxValue = dmax.getValue();
        //updateD();
    }

    public void onClick$dbutton(Event event) {
        uncheckAll();
        minValue = dmin.getValue();
        maxValue = dmax.getValue();
        //updateD();
    }

    public void onCheck$dunknown(Event event) {
        uncheckAll();
        //updateD();
    }

    void uncheckAll() {
        for (Listitem li : (List<Listitem>) legend.getItems()) {
            if (li.getFirstChild().getChildren().size() > 0
                    && ((Checkbox) li.getFirstChild().getFirstChild()).isChecked()) {
                ((Checkbox) li.getFirstChild().getFirstChild()).setChecked(false);
            }
        }
    }

    void updateD() {
        Facet f = Facet.parseFacet(getSelectionFacet());
        Query querynew = query.newFacet(f, true);
        //mc.mapSpecies(newq, "my layer", "species", q.getOccurrenceCount(), LayerUtilities.SPECIES, sa.getWkt(), 0);
        if(querynew.getOccurrenceCount() <= 0) {
            getMapComposer().showMessage("no occurrences in this selection");
        } else {
            getMapComposer().mapSpecies(querynew,
                    "My facet", "species", -1, LayerUtilities.SPECIES, null, 0, MapComposer.DEFAULT_POINT_SIZE, MapComposer.DEFAULT_POINT_OPACITY, MapComposer.nextColour());
            this.detach();
        }
    }

    void setEnableContinousControls(boolean enable) {
        dmax.setDisabled(!enable);
        dmin.setDisabled(!enable);
        dbutton.setDisabled(!enable);
        //dslider.setDisabled(!enable);
        if (!enable) {
            dlabel.setValue("list selection underway, range selection disabled");
        }
        dunknown.setDisabled(!enable);
    }

    private void setupForNumericalList(String first, String facetString) {
        String h = facetString;
        legend_facets = new HashMap<String, String>();
        try {
            divContinous.setVisible(!disableselection && true);            
            if (lo.getFieldType() == QueryField.FieldType.INT
                    || lo.getFieldType() == QueryField.FieldType.LONG) {
                intContinous = true;
            }

            gMinValue = minValue = lo.getMinMax()[0];
            gMaxValue = maxValue = lo.getMinMax()[1];
            double rng = gMaxValue - gMinValue;
            if (intContinous && rng < SLIDER_MAX && rng > 0) {
                SLIDER_MAX = (int) rng;
            }
            dslider.setMaxpos(SLIDER_MAX);
            try {
                if (facet != null) {
                    //count OR and test for null
                    boolean nulls = h.startsWith("-")
                            || (h.length() > 1 && h.charAt(1) == '-')
                            || (h.length() > 2 && h.charAt(2) == '-');
                    int countOr = h.split(" OR ").length;
                    if (countOr == 1 || (nulls && countOr == 2)) {
                        minValue = facet.getMin();
                        maxValue = facet.getMax();
                        if (Double.isInfinite(minValue)) {
                            minValue = gMinValue;
                        }
                        if (Double.isInfinite(maxValue)) {
                            maxValue = gMaxValue;
                        }
                    }
                    dunknown.setChecked(nulls);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            for (int j = 0; j < legend_lines.size(); j++) {
                String s = legend_lines.get(j);
                String back = s.substring(s.indexOf(','));
                String front = s.substring(0, s.indexOf(','));
                if (s.length() > 2 && s.charAt(0) == '-' || s.charAt(1) == '-') {
                    legend_lines.set(j, "Unknown" + back);
                    String v = front;
                    if (v.startsWith("\"") && v.endsWith("\"")) {
                        v = v.substring(1, v.length() - 1);
                    }
                    legend_facets.put("Unknown", v);
                } else {
                    String[] ss = s.split(" ");

                    double[] cutoffs = lo.getNumericLegend().getCutoffdoubles();
                    double[] cutoffMins = lo.getNumericLegend().getCutoffMindoubles();
                    String range, strFacet;
                    double min;
                    double max;
                    double nextmin;
                    if (ss.length > 1) {
                        if (ss[1].equals("*")) {
                            double v = Double.parseDouble(ss[3]);
                            min = cutoffMins[0];
                            max = cutoffs[0];
                            nextmin = cutoffMins.length > 1 ? cutoffMins[1] : cutoffMins[0];
                        } else if (ss[3].equals("*")) {
                            min = cutoffMins[cutoffMins.length - 1];
                            max = gMaxValue;
                            nextmin = gMaxValue;
                        } else {
                            double v = Double.parseDouble(ss[1]);
                            int pos = 0;
                            while (v > cutoffs[pos]) {
                                pos++;
                            }
                            min = cutoffMins[pos];
                            max = cutoffs[pos];
                            nextmin = cutoffMins.length > pos + 1 ? cutoffMins[pos + 1] : cutoffMins[pos];
                        }
                        if (intContinous) {
                            if (min == gMinValue && max == gMaxValue) {
                                range = String.format(">= %d and <= %d", (int) min, (int) max);
                            } else if (min == gMinValue) {
                                range = String.format(">= %d and < %d", (int) min, (int) nextmin);
                            } else if (max == gMaxValue) {
                                range = String.format("<= %d", (int) max);
                            } else {
                                range = String.format("< %d", (int) nextmin);
                            }
                            strFacet = colourmode + ":[" + (int) min + " TO " + (int) max + "]";
                        } else {
                            if (min == gMinValue && max == gMaxValue) {
                                range = String.format(">= %g and <= %g", min, max);
                            } else if (min == gMinValue) {
                                range = String.format(">= %g and < %g", min, nextmin);
                            } else if (max == gMaxValue) {
                                range = String.format("<= %g", max);
                            } else {
                                range = String.format("< %g", nextmin);
                            }

                            strFacet = colourmode + ":[" + min + " TO " + max + "]";
                        }
                        legend_lines.set(j, range + back);
                        legend_facets.put(range, strFacet);
                    }
                }
            }
            checkmarks = true;
        } catch (Exception e) {
            divContinous.setVisible(false);
            e.printStackTrace();
        }
    }

    private void setupForBiocacheNumber(String facetString, String facetName, boolean integer) {
        String h = facetString;
        isNumber = true;
        intContinous = integer;
        legend_facets = new HashMap<String, String>();

        //checkmarks = false;
        if (lo != null) {
            //enable sliders
            divContinous.setVisible(!disableselection && true);
            gMinValue = minValue = lo.getMinMax()[0];
            gMaxValue = maxValue = lo.getMinMax()[1];
            if (intContinous) {
                SLIDER_MAX = (int) Math.min(SLIDER_MAX, (int) (gMaxValue - gMinValue));
            }
            if(SLIDER_MAX > 0) {
                dslider.setMaxpos(SLIDER_MAX);
            }
            try {
                if (facet != null) {
                    //count OR and test for null
                    boolean nulls = h.contains("-(" + facetName + ":") || h.contains("-" + facetName + ":");
                    int countOr = h.split(" OR ").length;
                    if (countOr == 1 || (nulls && countOr == 2)) {
                        minValue = facet.getMin();
                        maxValue = facet.getMax();
                        if (Double.isInfinite(minValue)) {
                            minValue = gMinValue;
                        }
                        if (Double.isInfinite(maxValue)) {
                            maxValue = gMaxValue;
                        }
                    }
                    dunknown.setChecked(nulls);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            //update text in legend lines
            for (int j = 0; j < legend_lines.size(); j++) {
                String s = legend_lines.get(j);
                String back = s.substring(s.indexOf(','));
                String front = s.substring(0, s.indexOf(','));
                if (s.length() > 2 && s.charAt(0) == '-' || s.charAt(1) == '-') {
                    legend_lines.set(j, "Unknown" + back);
                    String v = front;
                    if (v.startsWith("\"") && v.endsWith("\"")) {
                        v = v.substring(1, v.length() - 1);
                    }
                    legend_facets.put("Unknown", v);
                } else {
                    s = s.substring(s.indexOf('[') + 1, s.indexOf(']'));
                    s = s.replace("-12-31T00:00:00Z","").replace("-01-01T00:00:00Z", "");
                    String[] ss = s.split(" TO ");

                    double[] cutoffs = lo.getNumericLegend().getCutoffdoubles();
                    double[] cutoffMins = lo.getNumericLegend().getCutoffMindoubles();
                    String range, strFacet;
                    if (ss.length > 1) {
                        if (ss[0].equals("*")) {
                            double v = Double.parseDouble(ss[1]);
                            if (intContinous) {
                                if (cutoffs.length > 1) {
                                    range = String.format(">= %d and < %d", (int) cutoffMins[0], (int) cutoffMins[1]);
                                } else {
                                    range = String.format(">= %d and <= %d", (int) cutoffMins[0], (int) cutoffs[0]);
                                }
                                if(colourmode.equals("occurrence_year") || colourmode.equals("occurrence_year_decade")) {
                                    String minyear = String.valueOf((int) cutoffMins[0]);
                                    while(minyear.length() < 4) minyear = "0" + minyear;
                                    strFacet = "" + facetName + ":[" + minyear + "-01-01T00:00:00Z TO " + (int) cutoffs[0] + "-12-31T00:00:00Z]";
                                } else {
                                    strFacet = "" + facetName + ":[" + (int) cutoffMins[0] + " TO " + (int) cutoffs[0] + "]";
                                }
                            } else {
                                if (cutoffs.length > 1) {
                                    range = String.format(">= %.2f and < %.2f", cutoffMins[0], cutoffMins[1]);
                                } else {
                                    range = String.format(">= %.2f and <= %.2f", cutoffMins[0], cutoffs[0]);
                                }
                                strFacet = "" + facetName + ":[" + cutoffMins[0] + " TO " + cutoffs[0] + "]";
                            }
                        } else if (ss[1].equals("*")) {
                            if (intContinous) {
                                //range = String.format(">= %d and <= %d", (int) cutoffMins[cutoffMins.length - 1], (int) gMaxValue);
                                range = String.format("<= %d", (int) gMaxValue);

                                if(colourmode.equals("occurrence_year") || colourmode.equals("occurrence_year_decade")) {
                                    String minyear = String.valueOf((int) cutoffMins[cutoffMins.length - 1]);
                                    while(minyear.length() < 4) minyear = "0" + minyear;
                                    strFacet = "" + facetName + ":[" + minyear + "-01-01T00:00:00Z TO " + (int) gMaxValue + "-12-31T00:00:00Z]";
                                } else {
                                    strFacet = "" + facetName + ":[" + (int) cutoffMins[cutoffMins.length - 1] + " TO " + (int) gMaxValue + "]";
                                }
                            } else {
                                //range = String.format(">= %.2f and <= %.2f", cutoffMins[cutoffMins.length - 1], gMaxValue);
                                range = String.format("<= %.2f", gMaxValue);
                                strFacet = "" + facetName + ":[" + cutoffMins[cutoffMins.length - 1] + " TO " + gMaxValue + "]";
                            }
                        } else {
                            double v = Double.parseDouble(ss[1]);
                            int pos = 0;
                            while (v > cutoffs[pos]) {
                                pos++;
                            }
                            if (intContinous) {
                                if (pos + 1 < cutoffs.length) {
                                    range = String.format("< %d", (int) cutoffMins[pos + 1]);
                                } else {
                                    //range = String.format(">= %d and <= %d", (int) cutoffMins[pos], (int) cutoffs[pos]);
                                    range = String.format("<= %d", (int) cutoffs[pos]);
                                }
                                if(colourmode.equals("occurrence_year") || colourmode.equals("occurrence_year_decade")) {
                                    String minyear = String.valueOf((int) cutoffMins[pos]);
                                    while(minyear.length() < 4) minyear = "0" + minyear;
                                    strFacet = "" + facetName + ":[" + minyear + "-01-01T00:00:00Z TO " + (int) cutoffs[pos] + "-12-31T00:00:00Z]";
                                } else {
                                    strFacet = "" + facetName + ":[" + (int) cutoffMins[pos] + " TO " + (int) cutoffs[pos] + "]";
                                }
                            } else {
                                if (pos + 1 < cutoffs.length) {
                                    range = String.format("< %.2f", cutoffMins[pos + 1]);
                                } else {
                                    range = String.format("<= %.2f", cutoffs[pos]);
                                }
                                strFacet = "" + facetName + ":[" + cutoffMins[pos] + " TO " + cutoffs[pos] + "]";
                            }
                        }
                        legend_lines.set(j, range + back);
                        legend_facets.put(range, strFacet);
                    }
                }
            }
        }
    }

    private void setupForBiocacheDecade() {
        String h = "occurrence_year";
        isNumber = false;
        intContinous = true;
        legend_facets = new HashMap<String, String>();

        //checkmarks = false;
        if (lo != null) {
            //update text in legend lines
            for (int j = 0; j < legend_lines.size(); j++) {
                String s = legend_lines.get(j);
                String back = s.substring(s.indexOf(','));
                String front = s.substring(0, s.indexOf(','));
                if (s.length() > 2 && s.charAt(0) == '-' || s.charAt(1) == '-') {
                    legend_lines.set(j, "Unknown" + back);
                    String v = front;
                    if (v.startsWith("\"") && v.endsWith("\"")) {
                        v = v.substring(1, v.length() - 1);
                    }
                    legend_facets.put("Unknown", v);
                } else {
                    s = s.substring(s.indexOf('[') + 1, s.indexOf(']'));
                    s = s.replace("-12-31T00:00:00Z","").replace("-01-01T00:00:00Z", "");
                    String[] ss = s.split(" TO ");

                    String range, strFacet;
                    if (ss.length > 1) {
                        String yr = ss[1].substring(0,ss[1].length()-1);

                        range = String.format("%s0s", yr);
                        strFacet = "occurrence_year:[" + yr + "0-01-01T00:00:00Z TO " + yr + "9-12-31T00:00:00Z]";

                        legend_lines.set(j, range + back);
                        legend_facets.put(range, strFacet);
                    }
                }
            }
        }
    }

    private void setupForBiocacheGeospatialKosher() {
        legend_facets = new HashMap<String, String>();
        //update text in legend lines
        for (int j = 0; j < legend_lines.size(); j++) {
            String s = legend_lines.get(j);
            int firstDelim = s.indexOf(',');
            String value = s.substring(0,firstDelim);
            String back = s.substring(firstDelim);
            if (value.equals("\"true\"")) {
                s = "Spatially valid";
            } else if (value.equals("\"false\"")) {
                s = "Spatially suspect";
            } else {
                s = "Coordinates not supplied";
            }
            legend_facets.put(s, "geospatial_kosher:" + value);
            legend_lines.set(j, s + back);
        }
    }

    private void setupForBiocacheMonth() {
        isMonth = true;
        legend_facets = new HashMap<String, String>();
        //update text in legend lines
        for (int j = 0; j < legend_lines.size(); j++) {
            String s = legend_lines.get(j);
            String back = s.substring(s.indexOf(','));
            if (s.length() > 2 && s.charAt(0) == '-' || s.charAt(1) == '-') {
                legend_lines.set(j, "Unknown" + back);
                legend_facets.put("Unknown", "Unknown");
            } else {
                s = s.substring(s.indexOf('[') + 1, s.indexOf(']'));
                String[] ss = s.split(" TO ");
                if (ss.length > 1) {
                    if (ss[1].equals("01")) {
                        s = "January";
                    } else if (ss[1].equals("02")) {
                        s = "February";
                    } else if (ss[1].equals("03")) {
                        s = "March";
                    } else if (ss[1].equals("04")) {
                        s = "April";
                    } else if (ss[1].equals("05")) {
                        s = "May";
                    } else if (ss[1].equals("06")) {
                        s = "June";
                    } else if (ss[1].equals("07")) {
                        s = "July";
                    } else if (ss[1].equals("08")) {
                        s = "August";
                    } else if (ss[1].equals("09")) {
                        s = "September";
                    } else if (ss[1].equals("10")) {
                        s = "October";
                    } else if (ss[1].equals("11")) {
                        s = "November";
                    } else if (ss[1].equals("12")) {
                        s = "December";
                    }
                }
                legend_facets.put(s, "month:[" + ss[1] + " TO " + ss[1] + "]");
                legend_lines.set(j, s + back);
            }

        }
    }

    int[] getState() {
        int countChecked = 0;
        boolean unknownChecked = false;
        for (Listitem li : (List<Listitem>) legend.getItems()) {
            if (li.getFirstChild().getChildren().size() > 0
                    && ((Checkbox) li.getFirstChild().getFirstChild()).isChecked()) {
                String v = ((Listcell) li.getChildren().get(1)).getLabel();
                v = displayToActualLabel(v);
                if (((query instanceof BiocacheQuery || divContinous.isVisible()) && v.equals("Unknown"))
                        || v.length() == 0) {
                    unknownChecked = true;
                } else {
                    countChecked++;
                }
            }
        }
        int[] state = new int[2];
        state[0] = countChecked;
        state[1] = (unknownChecked ? 1 : 0);

        return state;
    }

    public void checkboxClick(Event event) {
//        if (divContinous.isVisible()) {
//            int[] state = getState();
//            dunknown.setChecked(state[1] > 0);
//
//            setEnableContinousControls(state[0] <= 1);
//
//            if (state[0] > 0 || state[1] > 0) {
//                minValue = facet.getMin();
//                maxValue = facet.getMax();
//                if (Double.isInfinite(minValue)) {
//                    minValue = gMinValue;
//                }
//                if (Double.isInfinite(maxValue)) {
//                    maxValue = gMaxValue;
//                }
//            } else {
//                minValue = gMinValue;
//                maxValue = gMaxValue;
//
//                if (state[1] == 0) {
//                    dunknown.setChecked(true);
//                }
//            }
//
//            //if (state[0] <= 1) {
//            //    updateD();
//            //}
//        }
    }

    public void onClick$clearSelection(Event e) {

        uncheckAll();
        btnOk.setDisabled(true);

//        if (divContinous.isVisible()) {
//            setEnableContinousControls(true);
//            minValue = gMinValue;
//            maxValue = gMaxValue;
//            dunknown.setChecked(true);
//            //updateD();
//        } else {
//            //mapLayer.setHighlight(getSelectionFacet());
//            //getMapComposer().applyChange(mapLayer);
//        }
    }

    public void onClick$selectAll(Event event) {
        if (legend != null && legend.getItemCount() > 0) {
            for (Listitem li : (List<Listitem>) legend.getItems()) {
                if (li.getFirstChild().getChildren().size() > 0
                        && !((Checkbox) li.getFirstChild().getFirstChild()).isChecked()) {
                    ((Checkbox) li.getFirstChild().getFirstChild()).setChecked(true);
                }
            }
            //updateD();
            //mapLayer.setHighlight(getSelectionFacet());
            //getMapComposer().applyChange(mapLayer);
            //dCreateButtons.setVisible(true);

            checkboxClick(event);
            btnOk.setDisabled(false);
        }
    }

    public void onClick$btnCancel(Event event) {
        this.detach();
    }

    public void onClick$btnOk(Event event) {
        if (btnOk.isDisabled()) {
            return;
        }
        try {
            if (!hasCustomArea && (isAreaCustom() || isAreaHighlightCustom())) {
                this.doOverlapped();
                this.setTop("-9999px");
                this.setLeft("-9999px");

                Map<String, Object> winProps = new HashMap<String, Object>();
                winProps.put("parent", this);
                winProps.put("parentname", "AddTool");
                winProps.put("selectedMethod", selectedMethod);

                List<MapLayer> layers = getMapComposer().getPolygonLayers();
                if (layers != null && layers.size() > 0) {
                    prevTopArea = layers.get(0);
                } else {
                    prevTopArea = null;
                }

                Window window = (Window) Executions.createComponents("WEB-INF/zul/AddArea.zul", this, winProps);
                window.setAttribute("winProps", winProps, true);
                window.doModal();

                return;
            }

            Div currentDiv = (Div) getFellowIfAny("atstep" + currentStep);
            Div nextDiv = (Div) getFellowIfAny("atstep" + (currentStep + 1));
            Div previousDiv = (currentStep > 1) ? ((Div) getFellowIfAny("atstep" + (currentStep + 1))) : null;

            if (!currentDiv.getZclass().contains("last")) {
                currentDiv.setVisible(false);
                nextDiv.setVisible(true);

                Image previousStepCompletedImg = (Image) getFellowIfAny("imgCompletedStep" + (currentStep));
                previousStepCompletedImg.setVisible(true);

                Label previousStepLabel = (Label) getFellowIfAny("lblStep" + (currentStep));
                previousStepLabel.setStyle("font-weight:normal");

                Label currentStepLabel = (Label) getFellowIfAny("lblStep" + (currentStep + 1));
                currentStepLabel.setStyle("font-weight:bold");

                // now include the extra options for step 4
                //if (nextDiv != null) {
                //    btnOk.setLabel("Next >");
                //}

                currentStep++;
                updateWindowTitle();
                fixFocus();
                sa = getSelectedArea();
                query = QueryUtil.queryFromSelectedArea(query, sa, false, getGeospatialKosher());
                if (query != null) {
                    ArrayList<QueryField> fields = query.getFacetFieldList();
                    Comboitem seperator = new Comboitem("seperator");
                    seperator.setLabel("------------------    Taxonomic     ------------------");
                    seperator.setParent(cbColour);
                    seperator.setDisabled(true);

                    for (int i = 0; i < fields.size(); i++) {
                        Comboitem ci = new Comboitem(fields.get(i).getDisplayName());
                        ci.setValue(fields.get(i).getName());
                        ci.setParent(cbColour);

                        if (ci.getValue().equals("interaction")) {
                            Comboitem seperator1 = new Comboitem("seperator");
                            seperator1.setLabel("------------------    Geospatial     ------------------");
                            seperator1.setParent(cbColour);
                            seperator1.setDisabled(true);
                        }
                        if (ci.getValue().equals("geospatial_kosher")) {
                            Comboitem seperator2 = new Comboitem("seperator");
                            seperator2.setLabel("------------------     Temporal     ------------------");
                            seperator2.setParent(cbColour);
                            seperator2.setDisabled(true);
                        }
                        if (ci.getValue().equals("occurrence_year_decade")) {
                            Comboitem seperator3 = new Comboitem("seperator");
                            seperator3.setLabel("------------------  Record details  ------------------");
                            seperator3.setParent(cbColour);
                            seperator3.setDisabled(true);
                        }
                        if (ci.getValue().equals("collector")) {
                            Comboitem seperator4 = new Comboitem("seperator");
                            seperator4.setLabel("------------------    Attribution    ------------------");
                            seperator4.setParent(cbColour);
                            seperator4.setDisabled(true);
                        }
                        if (ci.getValue().equals("institution_name")) {
                            Comboitem seperator5 = new Comboitem("seperator");
                            seperator5.setLabel("------------------ Record assertions ------------------");
                            seperator5.setParent(cbColour);
                            seperator5.setDisabled(true);
                        }
                    }
                }
                btnBack.setDisabled(false);
                btnOk.setDisabled(true);
            } else {
                int[] state = getState();
                if (state[0] == 0) {
                    btnOk.setDisabled(true);
                }
                //if (state[0] > 1) {
                //    setEnableContinousControls(false);
                //} 
                updateD();
            }

        } catch (Exception ex) {
            Logger.getLogger(AddToolComposer.class.getName()).log(Level.SEVERE, null, ex);
        }

        fixFocus();
    }

    public void onClick$btnBack(Event event) {

        Div currentDiv = (Div) getFellowIfAny("atstep" + currentStep);
        Div nextDiv = (Div) getFellowIfAny("atstep" + (currentStep + 1));
        Div previousDiv = (currentStep > 1) ? ((Div) getFellowIfAny("atstep" + (currentStep - 1))) : null;



        if (currentDiv.getZclass().contains("first")) {
            btnBack.setDisabled(true);
        } else {
            currentDiv.setVisible(false);
            previousDiv.setVisible(true);

            Image currentStepCompletedImg = (Image) getFellowIfAny("imgCompletedStep" + (currentStep - 1));
            currentStepCompletedImg.setVisible(false);

            Label nextStepLabel = (Label) getFellowIfAny("lblStep" + (currentStep));
            nextStepLabel.setStyle("font-weight:normal");

            Label currentStepLabel = (Label) getFellowIfAny("lblStep" + (currentStep - 1));
            currentStepLabel.setStyle("font-weight:bold");

            currentStep--;

            if (previousDiv != null) {
                btnBack.setDisabled(((!previousDiv.getZclass().contains("first")) ? false : true));
            }
        }

        //btnOk.setLabel("Next >");
        btnOk.setDisabled(false);
        updateWindowTitle();

    }

    private boolean isAreaHighlightTab() {
        return rgAreaHighlight != null && rgAreaHighlight.getParent().isVisible();
    }

    boolean isAreaTab() {
        return rgArea != null && rgArea.getParent().isVisible();
    }

    boolean isAreaCustom() {
        return isAreaTab() && rAreaCustom != null && rAreaCustom.isSelected();
    }

    boolean isAreaHighlightCustom() {
        return isAreaHighlightTab() && rgAreaHighlight != null
                && rgAreaHighlight.getSelectedItem().getId().equals("rAreaCustomHighlight");
    }

    public void onCheck$rgAreaHighlight(Event event) {
        if (rgAreaHighlight == null) {
            return;
        }
        //setCustomArea = false;
        hasCustomArea = false;
        if (rgAreaHighlight.getSelectedItem().getId().equals("rAreaCustomHighlight")) {
            //setCustomArea = true;
            hasCustomArea = false;
        }
    }

    public SelectedArea getSelectedAreaHighlight() {
        String area = rgAreaHighlight.getSelectedItem().getValue();

        SelectedArea selectedarea = null;
        try {
            if (area.equals("none")) {
            } else if (area.equals("current")) {
                selectedarea = new SelectedArea(null, getMapComposer().getViewArea());
            } else if (area.equals("australia")) {
                selectedarea = new SelectedArea(null, CommonData.AUSTRALIA_WKT);
            } else if (area.equals("world")) {
                selectedarea = new SelectedArea(null, CommonData.WORLD_WKT);
            } else {
                List<MapLayer> layers = getMapComposer().getPolygonLayers();
                for (MapLayer ml : layers) {
                    if (area.equals(ml.getWKT())) {
                        selectedarea = new SelectedArea(ml, null);
                        break;
                    }
                }

                //for 'all areas'
                if (selectedarea == null) {
                    selectedarea = new SelectedArea(null, area);
                }
            }
        } catch (Exception e) {
            System.out.println("Unable to retrieve selected area");
            e.printStackTrace(System.out);
        }

        return selectedarea;
    }

    public void loadAreaLayers() {
        loadAreaLayers(null);
    }

    public void loadAreaLayers(String selectedAreaName) {
        try {
            Radiogroup rgArealocal = (Radiogroup) getFellowIfAny("rgArea");
            //remove all radio buttons that don't have an id
            for (int i = rgArealocal.getItemCount() - 1; i >= 0; i--) {
                String id = ((Radio) rgArealocal.getItems().get(i)).getId();
                if (id == null || id.length() == 0) {
                    rgArealocal.removeItemAt(i);
                } else {
                    rgArealocal.getItemAtIndex(i).setSelected(false);
                }
            }

            Radio rAreaCurrent = (Radio) getFellowIfAny("rAreaCurrent");

            String selectedLayerName = (String) params.get("polygonLayerName");
            Radio rSelectedLayer = null;

            StringBuilder allWKT = new StringBuilder();
            int count_not_envelopes = 0;
            List<MapLayer> layers = getMapComposer().getPolygonLayers();
            for (int i = 0; i < layers.size(); i++) {
                MapLayer lyr = layers.get(i);
                Radio rAr = new Radio(lyr.getDisplayName());
                rAr.setValue(lyr.getWKT());

                if (!lyr.getWKT().contains("ENVELOPE")) {
                    if (count_not_envelopes > 0) {
                        allWKT.append(',');
                    }
                    count_not_envelopes++;
                    String wkt = lyr.getWKT();
                    if (wkt.startsWith("GEOMETRYCOLLECTION(")) {
                        wkt = wkt.substring("GEOMETRYCOLLECTION(".length(), wkt.length() - 1);
                    }
                    allWKT.append(wkt);
                }

                rAr.setParent(rgArealocal);
                rgArealocal.insertBefore(rAr, rAreaCurrent);

                if (selectedLayerName != null && lyr.getName().equals(selectedLayerName)) {
                    rSelectedLayer = rAr;
                    rAreaSelected = rAr;
                }
            }

            if (!layers.isEmpty() && count_not_envelopes > 1) {
                Radio rAr = new Radio("All area layers"
                        + ((count_not_envelopes < layers.size()) ? " (excluding Environmental Envelopes)" : ""));
                rAr.setValue("GEOMETRYCOLLECTION(" + allWKT.toString() + ")");
                rAr.setParent(rgArealocal);
                rgArealocal.insertBefore(rAr, rAreaCurrent);
            }

            if (selectedAreaName != null && !selectedAreaName.equals("")) {
                for (int i = 0; i < rgArealocal.getItemCount(); i++) {
                    if (rgArealocal.getItemAtIndex(i).isVisible() && rgArealocal.getItemAtIndex(i).getLabel().equals(selectedAreaName)) {
                        rAreaSelected = rgArealocal.getItemAtIndex(i);
                        System.out.println("2.resetting indexToSelect = " + i);
                        rgArealocal.setSelectedItem(rAreaSelected);
                        break;
                    }
                }
            } else if (rSelectedLayer != null) {
                rAreaSelected = rSelectedLayer;
                rgArealocal.setSelectedItem(rAreaSelected);
            } else if (selectedLayerName != null && selectedLayerName.equals("none")) {
                rgArealocal.setSelectedItem(rAreaWorld);
                rAreaSelected = rAreaWorld;
                rgArealocal.setSelectedItem(rAreaSelected);
            } else {
//                for (int i = 0; i < rgArealocal.getItemCount(); i++) {
//                    if (rgArealocal.getItemAtIndex(i).isVisible()) {
//                        rAreaSelected = rgArealocal.getItemAtIndex(i);
//                        rgArealocal.setSelectedItem(rAreaSelected);
//                        break;
//                    }
//                }
                rAreaSelected = rAreaWorld;
                rgArealocal.setSelectedItem(rAreaSelected);

            }
            Clients.evalJavaScript("jq('#" + rAreaSelected.getUuid() + "-real').attr('checked', true);");

        } catch (Exception e) {
            System.out.println("Unable to load active area layers:");
            e.printStackTrace(System.out);
        }
    }

    public void loadAreaHighlightLayers(String selectedAreaName) {
        try {
            Radiogroup rgArealocal = (Radiogroup) getFellowIfAny("rgAreaHighlight");
            //remove all radio buttons that don't have an id
            for (int i = rgArealocal.getItemCount() - 1; i >= 0; i--) {
                String id = ((Radio) rgArealocal.getItems().get(i)).getId();
                if (id == null || id.length() == 0) {
                    rgArealocal.removeItemAt(i);
                } else {
                    rgArealocal.getItemAtIndex(i).setSelected(false);
                }
            }

            Radio rAreaCurrentHighlight = (Radio) getFellowIfAny("rAreaCurrentHighlight");

            String selectedLayerName = (String) params.get("polygonLayerName");
            Radio rSelectedLayer = null;

            StringBuilder allWKT = new StringBuilder();
            int count_not_envelopes = 0;
            List<MapLayer> layers = getMapComposer().getPolygonLayers();
            for (int i = 0; i < layers.size(); i++) {
                MapLayer lyr = layers.get(i);
                Radio rAr = new Radio(lyr.getDisplayName());
                rAr.setValue(lyr.getWKT());

                if (!lyr.getWKT().contains("ENVELOPE")) {
                    if (count_not_envelopes > 0) {
                        allWKT.append(',');
                    }
                    count_not_envelopes++;
                    String wkt = lyr.getWKT();
                    if (wkt.startsWith("GEOMETRYCOLLECTION(")) {
                        wkt = wkt.substring("GEOMETRYCOLLECTION(".length(), wkt.length() - 1);
                    }
                    allWKT.append(wkt);
                }


                rAr.setParent(rgArealocal);
                rgArealocal.insertBefore(rAr, rAreaCurrentHighlight);

                if (selectedLayerName != null && lyr.getName().equals(selectedLayerName)) {
                    rSelectedLayer = rAr;
                    //rAreaSelected = rAr;
                }
            }

            if (!layers.isEmpty() && count_not_envelopes > 1) {
                Radio rAr = new Radio("All area layers"
                        + ((count_not_envelopes < layers.size()) ? " (excluding Environmental Envelopes)" : ""));
                rAr.setValue("GEOMETRYCOLLECTION(" + allWKT.toString() + ")");
                rAr.setParent(rgArealocal);
                rgArealocal.insertBefore(rAr, rAreaCurrentHighlight);
            }

            if (selectedAreaName != null && !selectedAreaName.equals("")) {
                for (int i = 0; i < rgArealocal.getItemCount(); i++) {
                    if (rgArealocal.getItemAtIndex(i).isVisible() && rgArealocal.getItemAtIndex(i).getLabel().equals(selectedAreaName)) {
                        System.out.println("2.resetting indexToSelect = " + i);
                        rgArealocal.setSelectedItem(rgArealocal.getItemAtIndex(i));
                        break;
                    }
                }
            } else if (rSelectedLayer != null) {
                rgArealocal.setSelectedItem(rAreaSelected);
            } else if (selectedLayerName != null && selectedLayerName.equals("none")) {
                rgArealocal.setSelectedItem(rAreaWorld);
            } else {
                for (int i = 0; i < rgArealocal.getItemCount(); i++) {
                    if (rgArealocal.getItemAtIndex(i).isVisible()) {
                        //rAreaSelected = rgArea.getItemAtIndex(i);
                        rgArealocal.setSelectedItem(rgArealocal.getItemAtIndex(i));
                        break;
                    }
                }
            }
            Clients.evalJavaScript("jq('#" + rgArealocal.getSelectedItem().getUuid() + "-real').attr('checked', true);");

        } catch (Exception e) {
            System.out.println("Unable to load active area layers:");
            e.printStackTrace(System.out);
        }
    }

    public void loadAreaLayersHighlight() {
        try {

            Radiogroup rgArealocal = (Radiogroup) getFellowIfAny("rgAreaHighlight");
            Radio rAreaCurrent = (Radio) getFellowIfAny("rAreaCurrentHighlight");
            Radio rAreaNone = (Radio) getFellowIfAny("rAreaNoneHighlight");

            List<MapLayer> layers = getMapComposer().getPolygonLayers();
            for (int i = 0; i < layers.size(); i++) {
                MapLayer lyr = layers.get(i);
                Radio rAr = new Radio(lyr.getDisplayName());
                rAr.setId(lyr.getName().replaceAll(" ", ""));
                rAr.setValue(lyr.getWKT());
                rAr.setParent(rgArealocal);
                rgArealocal.insertBefore(rAr, rAreaCurrent);
            }

            rAreaNone.setSelected(true);
        } catch (Exception e) {
            System.out.println("Unable to load active area layers:");
            e.printStackTrace(System.out);
        }
    }

    public void resetWindow(String selectedArea) {
        try {

            if (selectedArea == null) {
                hasCustomArea = false;
            } else if (selectedArea.trim().equals("")) {
                hasCustomArea = false;
            } else {
                hasCustomArea = true;
            }

            boolean ok = false;
            if (hasCustomArea) {
                MapLayer curTopArea = null;
                List<MapLayer> layers = getMapComposer().getPolygonLayers();
                if (layers != null && layers.size() > 0) {
                    curTopArea = layers.get(0);
                } else {
                    curTopArea = null;
                }

                if (curTopArea != prevTopArea) {
                    if (isAreaHighlightTab()) {
                        loadAreaHighlightLayers(curTopArea.getDisplayName());
                    } else if (isAreaTab()) {
                        loadAreaLayers(curTopArea.getDisplayName());
                    }

                    ok = true;
                }
            }
            this.setTop(winTop);
            this.setLeft(winLeft);

            this.doModal();

            if (ok) {
                onClick$btnOk(null);
                hasCustomArea = false;
            }

            fixFocus();
        } catch (InterruptedException ex) {
            System.out.println("InterruptedException when resetting analysis window");
            ex.printStackTrace(System.out);
        } catch (SuspendNotAllowedException ex) {
            System.out.println("Exception when resetting analysis window");
            ex.printStackTrace(System.out);
        }
    }
    /**
     * TODO NC 2013-08-15: Remove the need for "false" as the third item in the array.
     * @return
     */
    public boolean[] getGeospatialKosher() {
        return new boolean[]{chkGeoKosherTrue.isChecked(), chkGeoKosherFalse.isChecked(), false};
    }

    public void onCheck$chkGeoKosherTrue(Event event) {
        event = ((ForwardEvent)event).getOrigin();
        if (!((CheckEvent)event).isChecked() && !chkGeoKosherFalse.isChecked() ) {
            chkGeoKosherFalse.setChecked(true);
        }
    }

    public void onCheck$chkGeoKosherFalse(Event event) {
        event = ((ForwardEvent)event).getOrigin();
        if (!((CheckEvent)event).isChecked() && !chkGeoKosherTrue.isChecked()) {
            chkGeoKosherTrue.setChecked(true);
        }
    }

    

    private String displayToActualLabel(String v) {
        String actual = null;
        for(String key : CommonData.getI18nPropertiesList(colourmode)) {
            String s = CommonData.getI18nProperty(key);
            if (s.equals(v)) {
                int pos = key.indexOf('.');
                if (pos > 0) {
                    actual = key.substring(pos + 1);
                } else {
                    actual = key;
                }
            }
        }
        if (actual == null) {
            actual = v;
        }
        return actual;
    }

    private String actualToDisplayLabel(String v) {
        String s = CommonData.getI18nProperty(colourmode + "." + v);
        if (s == null) {
            s = v;
        }
        return s;
    }
}
