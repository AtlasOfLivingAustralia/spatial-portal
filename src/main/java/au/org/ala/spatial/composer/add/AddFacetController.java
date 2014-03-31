package au.org.ala.spatial.composer.add;

import au.com.bytecode.opencsv.CSVReader;
import au.org.ala.spatial.data.BiocacheQuery;
import au.org.ala.spatial.data.Query;
import au.org.ala.spatial.data.QueryUtil;
import au.org.ala.spatial.util.CommonData;
import au.org.ala.spatial.util.SelectedArea;
import au.org.ala.spatial.util.Util;
import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.settings.SettingsSupplementary;
import au.org.emii.portal.util.LayerUtilities;
import org.ala.layers.legend.Facet;
import org.ala.layers.legend.Legend;
import org.ala.layers.legend.LegendObject;
import org.ala.layers.legend.QueryField;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.SuspendNotAllowedException;
import org.zkoss.zk.ui.event.CheckEvent;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.ForwardEvent;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.*;

import java.io.StringReader;
import java.util.*;

public class AddFacetController extends UtilityComposer {

    private static Logger logger = Logger.getLogger(AddFacetController.class);
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
    List<String[]> legend_lines;
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
    Checkbox cbContinousRange;
    Label lblOccurrencesSelected;

    @Override
    public void afterCompose() {
        super.afterCompose();
        winTop = this.getTop();
        winLeft = this.getLeft();
        setupDefaultParams();
        setParams(Executions.getCurrent().getAttributes());
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
        hasCustomArea = false;
        rAreaSelected = rgArea.getSelectedItem();
        try {
            rAreaSelected = (Radio) ((org.zkoss.zk.ui.event.ForwardEvent) event).getOrigin().getTarget();
        } catch (Exception e) {
        }
        if (rAreaSelected == rAreaCustom) {
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
        colourmode = cbColour.getSelectedItem().getValue();
        try {
            lo = query.getLegend(colourmode);
        } catch (Exception e) {
            logger.error("error getting legend: " + query.getFullQ(false) + " colourmode:" + colourmode, e);
        }

        map.put("colourmode", colourmode);

        pid = (String) (Executions.getCurrent().getArg().get("pid"));

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


            try {
                legend_lines = new CSVReader(new StringReader(slist)).readAll();
            } catch (Exception e) {
                logger.error("failed to read legend list as csv", e);
            }
            String[] first_line = legend_lines.get(0);
            legend_lines.remove(0);

            String h = "";
            facet = null;

            divContinous.setVisible(false);

            //test for range (user upload)
            if (legend_lines.size() > 1) {
                String first = legend_lines.get(0)[0];
                if (first == null || first.length() == 0 || first.startsWith("Unknown")) {
                    first = legend_lines.get(1)[0];
                }
                if (!checkmarks && query.getLegend(colourmode) != null
                        && query.getLegend(colourmode).getNumericLegend() != null) {
                    setupForNumericalList(first, h);
                    //test for manual range (solr query)
                } else if (colourmode.equals("occurrence_year")) {
                    setupForBiocacheNumber(h, colourmode, true);
                } else if (colourmode.equals("occurrence_year_decade") || colourmode.equals("decade")) {
                    setupForBiocacheDecade();
                } else if (colourmode.equals("coordinate_uncertainty") || colourmode.equals("uncertainty")) {
                    setupForBiocacheNumber(h, colourmode, false);
                } else if (colourmode.equals("month")) {
                    setupForBiocacheMonth();
                }
            }

            /* apply something to line onclick in lb */
            legend.setItemRenderer(new ListitemRenderer() {

                @Override
                public void render(Listitem li, Object data, int item_idx) {
                    String[] ss = (String[]) data;

                    if (ss == null) {
                        return;
                    }

                    Checkbox cb = null;

                    if (checkmarks) {
                        cb = new Checkbox();
                        cb.addEventListener("onCheck", new EventListener() {

                            @Override
                            public void onEvent(Event event) throws Exception {
                                btnOk.setDisabled(false);
                            }
                        });
                        cb.setVisible(!cbContinousRange.isChecked());
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

                    int red = 0, green = 0, blue = 0;
                    try {
                        red = Integer.parseInt(ss[1]);
                        green = Integer.parseInt(ss[2]);
                        blue = Integer.parseInt(ss[3]);
                    } catch (Exception e) {
                        logger.error("error parsing colours : " + ss[0], e);
                    }

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

        } catch (Exception e) {
            logger.error("error rendering legend", e);
        }
    }

    String getSelectionFacet() {
        StringBuilder values = new StringBuilder();

        boolean unknown = false;
        if (!divContinous.isVisible() || !cbContinousRange.isChecked()) {
            for (Listitem li : legend.getItems()) {
                if (li.getFirstChild().getChildren().size() > 0
                        && ((Checkbox) li.getFirstChild().getFirstChild()).isChecked()) {
                    String v = ((Listcell) li.getChildren().get(1)).getLabel();
                    v = displayToActualLabel(v);
                    if (legend_facets != null) {
                        if (v.equals("Unknown") || v.contains("occurrence_year") || v.contains("uncertainty") || v.contains("coordinate_uncertainty")) {
                            //keep unchanged
                            divContinous.setVisible(true);
                            onCheck$cbContinousRange(null);
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
        } else {
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

    public void onOK$dmin(Event event) {
        uncheckAll();
        minValue = dmin.getValue();
    }

    public void onChange$dmin(Event event) {
        uncheckAll();
        minValue = dmin.getValue();
    }

    public void onOK$dmax(Event event) {
        uncheckAll();
        maxValue = dmax.getValue();
    }

    public void onChange$dmax(Event event) {
        uncheckAll();
        maxValue = dmax.getValue();
    }

    public void onClick$dbutton(Event event) {
        uncheckAll();
        minValue = dmin.getValue();
        maxValue = dmax.getValue();

        //get count and display
        Facet f = Facet.parseFacet(getSelectionFacet());
        Query querynew = query.newFacet(f, true);
        lblOccurrencesSelected.setValue(querynew.getOccurrenceCount() + " occurrences selected");
    }

    public void onCheck$dunknown(Event event) {
        uncheckAll();
    }

    void uncheckAll() {
        for (Listitem li : legend.getItems()) {
            if (li.getFirstChild().getChildren().size() > 0
                    && ((Checkbox) li.getFirstChild().getFirstChild()).isChecked()) {
                ((Checkbox) li.getFirstChild().getFirstChild()).setChecked(false);
            }
        }
    }

    private void setupForNumericalList(String first, String facetString) {
        String h = facetString;
        legend_facets = new HashMap<String, String>();
        try {
            divContinous.setVisible(!disableselection);
            onCheck$cbContinousRange(null);
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
            dmin.setValue(gMinValue);
            dmax.setValue(gMaxValue);

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
                logger.error("error with numerical legend", e);
            }
            for (int j = 0; j < legend_lines.size(); j++) {

                String label = legend_lines.get(j)[0];
                if (label.charAt(0) == '-') {
                    legend_facets.put("Unknown", legend_lines.get(j)[0]);
                    legend_lines.get(j)[0] = "Unknown";
                } else {
                    String[] ss = legend_lines.get(j);

                    float[] cutoffs = lo.getNumericLegend().getCutoffFloats();
                    float[] cutoffMins = lo.getNumericLegend().getCutoffMinFloats();
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
                        legend_lines.get(j)[0] = range;
                        legend_facets.put(range, strFacet);
                    }
                }
            }
            checkmarks = true;
        } catch (Exception e) {
            divContinous.setVisible(false);
            onCheck$cbContinousRange(null);
            logger.error("error building numerical legend", e);
        }
    }

    private void setupForBiocacheNumber(String facetString, String facetName, boolean integer) {
        String h = facetString;
        isNumber = true;
        intContinous = integer;
        legend_facets = new HashMap<String, String>();

        if (lo != null) {
            //enable sliders
            divContinous.setVisible(!disableselection);
            onCheck$cbContinousRange(null);
            gMinValue = minValue = lo.getMinMax()[0];
            gMaxValue = maxValue = lo.getMinMax()[1];

            dmin.setValue(gMinValue);
            dmax.setValue(gMaxValue);

            if (intContinous) {
                SLIDER_MAX = Math.min(SLIDER_MAX, (int) (gMaxValue - gMinValue));
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
                logger.error("error building numberical legend", e);
            }

            //update text in legend lines
            for (int j = 0; j < legend_lines.size(); j++) {

                String label = legend_lines.get(j)[0];
                if (label.charAt(0) == '-') {
                    legend_facets.put("Unknown", legend_lines.get(j)[0]);
                    legend_lines.get(j)[0] = "Unknown";
                } else {
                    String s = legend_lines.get(j)[0];

                    String[] ss = s.replace("[", "").replace("]", "").replace("-12-31T00:00:00Z", "").replace("-01-01T00:00:00Z", "").split(" TO ");

                    float[] cutoffs = lo.getNumericLegend().getCutoffFloats();
                    float[] cutoffMins = lo.getNumericLegend().getCutoffMinFloats();
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
                                if (colourmode.equals("occurrence_year") || colourmode.equals("occurrence_year_decade") || colourmode.equals("decade")) {
                                    String minyear = String.valueOf((int) cutoffMins[0]);
                                    while (minyear.length() < 4) {
                                        minyear = "0" + minyear;
                                    }
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

                                if (colourmode.equals("occurrence_year") || colourmode.equals("occurrence_year_decade") || colourmode.equals("decade")) {
                                    String minyear = String.valueOf((int) cutoffMins[cutoffMins.length - 1]);
                                    while (minyear.length() < 4) {
                                        minyear = "0" + minyear;
                                    }
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
                                if (colourmode.equals("occurrence_year") || colourmode.equals("occurrence_year_decade") || colourmode.equals("decade")) {
                                    String minyear = String.valueOf((int) cutoffMins[pos]);
                                    while (minyear.length() < 4) {
                                        minyear = "0" + minyear;
                                    }
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
                        legend_lines.get(j)[0] = range;
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
                String label = legend_lines.get(j)[0];
                if (label.charAt(0) == '-') {
                    legend_facets.put("Unknown", legend_lines.get(j)[0]);
                    legend_lines.get(j)[0] = "Unknown";
                } else {
                    //String[] ss = legend_lines.get(j);
                    //s = s.substring(s.indexOf('[') + 1, s.indexOf(']'));
                    //s = s.replace("-12-31T00:00:00Z", "").replace("-01-01T00:00:00Z", "");
                    String[] ss = legend_lines.get(j)[0].split(" TO ");

                    String range, strFacet;
                    if (ss.length > 1) {
                        String yr = ss[1].substring(0, ss[1].length() - 2);

                        range = String.format("%s0s", yr);
                        strFacet = "occurrence_year:[" + yr + "0-01-01T00:00:00Z TO " + yr + "9-12-31T00:00:00Z]";

                        legend_lines.get(j)[0] = range;
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
            String value = legend_lines.get(j)[0];
            String s;
            if (value.equals("true")) {
                s = "Spatially valid";
            } else if (value.equals("false")) {
                s = "Spatially suspect";
            } else {
                s = "Coordinates not supplied";
            }
            legend_facets.put(s, "geospatial_kosher:" + value);
            legend_lines.get(j)[0] = s;
        }
    }

    private void setupForBiocacheMonth() {
        isMonth = true;
        legend_facets = new HashMap<String, String>();
        //update text in legend lines
        for (int j = 0; j < legend_lines.size(); j++) {
            String label = legend_lines.get(j)[0];
            if (label.charAt(0) == '-') {
                legend_facets.put("Unknown", legend_lines.get(j)[0]);
                legend_lines.get(j)[0] = "Unknown";
            } else {
                String s = legend_lines.get(j)[0];
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
                legend_lines.get(j)[0] = s;
            }

        }
    }

    int[] getState() {
        int countChecked = 0;
        boolean unknownChecked = false;
        for (Listitem li : legend.getItems()) {
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

    public void onClick$clearSelection(Event e) {

        uncheckAll();
        btnOk.setDisabled(true);
    }

    public void onClick$selectAll(Event event) {
        if (legend != null && legend.getItemCount() > 0) {
            for (Listitem li : legend.getItems()) {
                if (li.getFirstChild().getChildren().size() > 0
                        && !((Checkbox) li.getFirstChild().getFirstChild()).isChecked()) {
                    ((Checkbox) li.getFirstChild().getFirstChild()).setChecked(true);
                }
            }
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
                winProps.put("parentname", "Tool");
                winProps.put("selectedMethod", selectedMethod);

                List<MapLayer> layers = getMapComposer().getPolygonLayers();
                if (layers != null && layers.size() > 0) {
                    prevTopArea = layers.get(0);
                } else {
                    prevTopArea = null;
                }

                Window window = (Window) Executions.createComponents("WEB-INF/zul/add/AddArea.zul", this, winProps);
                window.setAttribute("winProps", winProps, true);
                window.doModal();

                return;
            }

            Div currentDiv = (Div) getFellowIfAny("atstep" + currentStep);
            Div nextDiv = (Div) getFellowIfAny("atstep" + (currentStep + 1));

            if (!currentDiv.getZclass().contains("last")) {
                currentDiv.setVisible(false);
                nextDiv.setVisible(true);

                Image previousStepCompletedImg = (Image) getFellowIfAny("imgCompletedStep" + (currentStep));
                previousStepCompletedImg.setVisible(true);

                Label previousStepLabel = (Label) getFellowIfAny("lblStep" + (currentStep));
                previousStepLabel.setStyle("font-weight:normal");

                Label currentStepLabel = (Label) getFellowIfAny("lblStep" + (currentStep + 1));
                currentStepLabel.setStyle("font-weight:bold");

                currentStep++;
                updateWindowTitle();
                fixFocus();
                sa = getSelectedArea();
                query = QueryUtil.queryFromSelectedArea(query, sa, false, getGeospatialKosher());
                if (query != null) {
                    ArrayList<QueryField> fields = query.getFacetFieldList();
                    Collections.sort(fields, new QueryField.QueryFieldComparator());
                    Comboitem seperator = new Comboitem("seperator");
                    String lastGroup = null;

                    for (QueryField field : fields) {
                        String newGroup = field.getGroup().getName();
                        if (!newGroup.equals(lastGroup)) {
                            Comboitem sep = new Comboitem("seperator");
                            sep.setLabel("---------------" + StringUtils.center(newGroup, 19) + "---------------");
                            sep.setParent(cbColour);
                            sep.setDisabled(true);
                            lastGroup = newGroup;
                        }
                        Comboitem ci = new Comboitem(field.getDisplayName());
                        ci.setValue(field.getName());
                        ci.setParent(cbColour);
                    }

                }
                btnBack.setDisabled(false);
                btnOk.setDisabled(true);
            } else {
                int[] state = getState();
                if (state[0] == 0) {
                    btnOk.setDisabled(true);
                }

                Facet f = Facet.parseFacet(getSelectionFacet());
                Query querynew = query.newFacet(f, true);
                if (querynew.getOccurrenceCount() <= 0) {
                    getMapComposer().showMessage("no occurrences in this selection");
                } else {
                    getMapComposer().mapSpecies(querynew,
                            "My facet", "species", -1, LayerUtilities.SPECIES, null, 0, MapComposer.DEFAULT_POINT_SIZE,
                            MapComposer.DEFAULT_POINT_OPACITY, Util.nextColour());
                    this.detach();
                }
            }

        } catch (Exception ex) {
            logger.error("error adding facet", ex);
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
                btnBack.setDisabled(((previousDiv.getZclass().contains("first"))));
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
        if (rgAreaHighlight.getSelectedItem().getId().equals("rAreaCustomHighlight")) {
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
            logger.error("Unable to retrieve selected area", e);
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
                String id = rgArealocal.getItems().get(i).getId();
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
                        logger.debug("2.resetting indexToSelect = " + i);
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
                rAreaSelected = rAreaWorld;
                rgArealocal.setSelectedItem(rAreaSelected);

            }
            Clients.evalJavaScript("jq('#" + rAreaSelected.getUuid() + "-real').attr('checked', true);");

        } catch (Exception e) {
            logger.error("Unable to load active area layers:", e);
        }
    }

    public void loadAreaHighlightLayers(String selectedAreaName) {
        try {
            Radiogroup rgArealocal = (Radiogroup) getFellowIfAny("rgAreaHighlight");
            //remove all radio buttons that don't have an id
            for (int i = rgArealocal.getItemCount() - 1; i >= 0; i--) {
                String id = rgArealocal.getItems().get(i).getId();
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
                        logger.debug("2.resetting indexToSelect = " + i);
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
                        rgArealocal.setSelectedItem(rgArealocal.getItemAtIndex(i));
                        break;
                    }
                }
            }
            Clients.evalJavaScript("jq('#" + rgArealocal.getSelectedItem().getUuid() + "-real').attr('checked', true);");

        } catch (Exception e) {
            logger.error("Unable to load active area layers:", e);
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
            logger.error("Unable to load active area layers:", e);
        }
    }

    public void resetWindow(String selectedArea) {
        try {

            if (selectedArea == null || selectedArea.trim().isEmpty()) {
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

        } catch (SuspendNotAllowedException ex) {
            logger.error("Exception when resetting analysis window", ex);
        }
    }

    /**
     * TODO NC 2013-08-15: Remove the need for "false" as the third item in the
     * array.
     *
     * @return
     */
    public boolean[] getGeospatialKosher() {
        return new boolean[]{chkGeoKosherTrue.isChecked(), chkGeoKosherFalse.isChecked(), false};
    }

    public void onCheck$chkGeoKosherTrue(Event event) {
        event = ((ForwardEvent) event).getOrigin();
        if (!((CheckEvent) event).isChecked() && !chkGeoKosherFalse.isChecked()) {
            chkGeoKosherFalse.setChecked(true);
        }
    }

    public void onCheck$chkGeoKosherFalse(Event event) {
        event = ((ForwardEvent) event).getOrigin();
        if (!((CheckEvent) event).isChecked() && !chkGeoKosherTrue.isChecked()) {
            chkGeoKosherTrue.setChecked(true);
        }
    }

    private String displayToActualLabel(String v) {
        String actual = null;
        for (String key : CommonData.getI18nPropertiesList(colourmode)) {
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

    public void onCheck$cbContinousRange(Event c) {
        dmin.setDisabled(!cbContinousRange.isChecked());
        dmax.setDisabled(!cbContinousRange.isChecked());
        dbutton.setDisabled(!cbContinousRange.isChecked());
        dunknown.setDisabled(!cbContinousRange.isChecked());

        for (Listitem li : legend.getItems()) {
            if (li.getFirstChild().getChildren().size() > 0) {
                ((Checkbox) li.getFirstChild().getFirstChild()).setVisible(!cbContinousRange.isChecked());
            }
        }

        if (!cbContinousRange.isChecked()) {
            dlabel.setValue("list selection underway, range selection disabled");
        } else {
            dlabel.setValue("range selection underway, list selection disabled");
        }
    }
}
