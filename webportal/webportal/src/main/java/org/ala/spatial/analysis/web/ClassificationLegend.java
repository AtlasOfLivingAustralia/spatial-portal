package org.ala.spatial.analysis.web;

import au.com.bytecode.opencsv.CSVReader;
import au.org.emii.portal.composer.MapComposer;

import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.util.LayerUtilities;
import java.io.IOException;
import java.util.List;
import java.io.StringReader;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.ListitemRenderer;
import org.zkoss.zul.SimpleListModel;
import java.util.ArrayList;
import java.util.HashMap;
import org.ala.spatial.data.Facet;
import org.ala.spatial.data.Legend;
import org.ala.spatial.data.LegendObject;
import org.ala.spatial.data.Query;
import org.ala.spatial.data.BiocacheQuery;
import org.ala.spatial.data.QueryField;
import org.ala.spatial.util.CommonData;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Button;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Doublebox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listheader;
import org.zkoss.zul.Rangeslider;

public class ClassificationLegend extends UtilityComposer {

    int SLIDER_MAX = 500;
    Query query;
    String pid;
    String colourmode;
    MapLayer mapLayer;
    String imagePath = "";
    public Listbox legend;
    Button createInGroup;
    Button clearSelection;
    ArrayList<String> legend_lines;
    boolean readonly = false;
    boolean checkmarks = false;
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

    @Override
    public void afterCompose() {
        super.afterCompose();

        query = (Query) (Executions.getCurrent().getArg().get("query"));
        System.out.println("Query q:" + query.getQ());
        mapLayer = (MapLayer) (Executions.getCurrent().getArg().get("layer"));

        readonly = (Executions.getCurrent().getArg().get("readonly")) != null;
        colourmode = (String) (Executions.getCurrent().getArg().get("colourmode"));
        pid = (String) (Executions.getCurrent().getArg().get("pid"));

        checkmarks = (Executions.getCurrent().getArg().get("checkmarks")) != null;

        disableselection = (Executions.getCurrent().getArg().get("disableselection")) != null;
        if (disableselection) {
            dCreateButtons.setVisible(false);
            lhFirstColumn.setWidth("0px");
            lhSecondColumn.setWidth("190px");
            lhThirdColumn.setWidth("15px");
            lhThirdColumn.setLabel("");
            lhFourthColumn.setWidth("50px");
            legend.setWidth("280px");
        }

        buildLegend();
    }

    public void onClick$createInGroup(Event e) {
        getMapComposer().mapSpecies(query.newFacet(facet, true),
                "Facet of " + mapLayer.getDisplayName(), "species", -1, LayerUtilities.SPECIES, null, -1, MapComposer.DEFAULT_POINT_SIZE, MapComposer.DEFAULT_POINT_OPACITY, MapComposer.nextColour());
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

    public void onClick$clearSelection(Event e) {

        uncheckAll();

        if (divContinous.isVisible()) {
            setEnableContinousControls(true);
            minValue = gMinValue;
            maxValue = gMaxValue;
            dunknown.setChecked(true);
            updateD();
        } else {
            mapLayer.setHighlight(getSelectionFacet());
            getMapComposer().applyChange(mapLayer);
        }
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

            String h = mapLayer.getHighlight();
            facet = Facet.parseFacet(h);

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
                            }
                        });
                        determineCheckboxState(cb, ss[0]);
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
                        long count = Long.parseLong(ss[4]); //don't display if it is not a number
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

            createInGroup.setVisible(!disableselection && mapLayer.getHighlight() != null && mapLayer.getHighlight().length() > 0);
            clearSelection.setVisible(!disableselection && mapLayer.getHighlight() != null && mapLayer.getHighlight().length() > 0);

            if (divContinous.isVisible()) {
                int[] state = getState();
                if (state[0] > 1) {
                    setEnableContinousControls(false);
                } else {
                    updateD();
                }
            }
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
                    if (v.equals("Unknown")) {
                        //keep unchanged
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
            updateD();
        }
    }

    public void onOK$dmin(Event event) {
        uncheckAll();
        minValue = dmin.getValue();
        updateD();
    }

    public void onChange$dmin(Event event) {
        uncheckAll();
        minValue = dmin.getValue();
        updateD();
    }

    public void onOK$dmax(Event event) {
        uncheckAll();
        maxValue = dmax.getValue();
        updateD();
    }

    public void onChange$dmax(Event event) {
        uncheckAll();
        maxValue = dmax.getValue();
        updateD();
    }

    public void onClick$dbutton(Event event) {
        uncheckAll();
        minValue = dmin.getValue();
        maxValue = dmax.getValue();
        updateD();
    }

    public void onCheck$dunknown(Event event) {
        uncheckAll();
        updateD();
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
        if (!dmin.isDisabled()) {
            double range = gMaxValue - gMinValue;
            if (range > 0) {
                if (intContinous) {
                    minValue = (int) minValue;
                    maxValue = (int) maxValue;
                }
                dslider.setCurpos((int) (((minValue - gMinValue) / range) * SLIDER_MAX));
                dslider.setCurmaxpos((int) (((maxValue - gMinValue) / range) * SLIDER_MAX));
                dslider.setCurpos((int) (((minValue - gMinValue) / range) * SLIDER_MAX));   //repeat instead of testing for correct setting order
                dmin.setValue(minValue);
                dmax.setValue(maxValue);

                mapLayer.setHighlight(getSelectionFacet());
                Facet f = Facet.parseFacet(getSelectionFacet());
                int occurrencesCount = query.getOccurrenceCount();
                if (f != null) {
                    occurrencesCount = query.newFacet(f, false).getOccurrenceCount();
                }
                String minText = (minValue == (int) minValue ? String.valueOf((int) minValue) : String.format("%f", minValue));
                String maxText = (maxValue == (int) maxValue ? String.valueOf((int) maxValue) : String.format("%f", maxValue));
                String unknownText = (dunknown.isChecked() ? " +Unknown" : " ");
                String selectedText;
                if (occurrencesCount == query.getOccurrenceCount()) {
                    selectedText = String.format(" (all of %d records selected)", occurrencesCount, query.getOccurrenceCount());
                } else {
                    selectedText = String.format(" (%d of %d records selected)", occurrencesCount, query.getOccurrenceCount());
                }

                createInGroup.setVisible(!disableselection && occurrencesCount != 0 && occurrencesCount != query.getOccurrenceCount());
                clearSelection.setVisible(!disableselection && occurrencesCount != 0 && occurrencesCount != query.getOccurrenceCount());

                dlabel.setValue(minText + " to " + maxText + unknownText + selectedText);
                getMapComposer().applyChange(mapLayer);

            }
        } else {
            dlabel.setValue("list selection underway, range selection disabled");
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
            LegendObject lo = (LegendObject) mapLayer.getData("legendobject");
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
        LegendObject lo = (LegendObject) mapLayer.getData("legendobject");
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
        LegendObject lo = (LegendObject) mapLayer.getData("legendobject");
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

    public void checkboxClick(Event event) {
        //reload the map layer with this highlight.
        if (mapLayer != null) {
            mapLayer.setHighlight(getSelectionFacet());
            createInGroup.setVisible(!disableselection && mapLayer.getHighlight() != null && mapLayer.getHighlight().length() > 0);
            clearSelection.setVisible(!disableselection && mapLayer.getHighlight() != null && mapLayer.getHighlight().length() > 0);

            if (divContinous.isVisible()) {
                int[] state = getState();
                dunknown.setChecked(state[1] > 0);

                setEnableContinousControls(state[0] <= 1);

                if (state[0] > 0 || state[1] > 0) {
                    minValue = facet.getMin();
                    maxValue = facet.getMax();
                    if (Double.isInfinite(minValue)) {
                        minValue = gMinValue;
                    }
                    if (Double.isInfinite(maxValue)) {
                        maxValue = gMaxValue;
                    }
                } else {
                    minValue = gMinValue;
                    maxValue = gMaxValue;

                    if (state[1] == 0) {
                        dunknown.setChecked(true);
                    }
                }

                if (state[0] <= 1) {
                    updateD();
                }
            }
        }
        getMapComposer().applyChange(mapLayer);
    }

    private void determineCheckboxState(Checkbox cb, String value) {
        if (!disableselection && mapLayer.getHighlight() != null && mapLayer.getHighlight().length() > 0) {
            String f = mapLayer.getHighlight();
            if (isMonth) {
                //convert this string to date number
                boolean found = false;
                if (value.startsWith("Unknown")) {
                    found = f.startsWith("-");
                } else if (value.equals("January")) {
                    found = f.contains(" 01");
                } else if (value.equals("February")) {
                    found = f.contains(" 02");
                } else if (value.equals("March")) {
                    found = f.contains(" 03");
                } else if (value.equals("April")) {
                    found = f.contains(" 04");
                } else if (value.equals("May")) {
                    found = f.contains(" 05");
                } else if (value.equals("June")) {
                    found = f.contains(" 06");
                } else if (value.equals("July")) {
                    found = f.contains(" 07");
                } else if (value.equals("August")) {
                    found = f.contains(" 08");
                } else if (value.equals("September")) {
                    found = f.contains(" 09");
                } else if (value.equals("October")) {
                    found = f.contains(" 10");
                } else if (value.equals("November")) {
                    found = f.contains(" 11");
                } else if (value.equals("December")) {
                    found = f.contains("11 TO *");
                }
                if (found) {
                    cb.setChecked(found);
                }
            } else if (!f.contains(":\"") && !f.contains(":*")) {
                //must be a continous selection
                try {
                    if (facet != null) {
                        if (value == null || value.length() == 0 || ((query instanceof BiocacheQuery || divContinous.isVisible()) && value.equals("Unknown"))) {
                            cb.setChecked(facet.isValid(""));
                        } else {
                            cb.setChecked(f.contains(legend_facets.get(value)));
                        }
                    } else {
                        System.out.println("Error parsing: " + mapLayer.getHighlight());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                Facet facet = Facet.parseFacet(f);
                if (facet != null) {
                    if (value == null || value.length() == 0 || ((query instanceof BiocacheQuery || divContinous.isVisible()) && value.equals("Unknown"))) {
                        cb.setChecked(facet.isValid(""));
                    } else {
                        cb.setChecked(facet.isValid(value));
                    }
                } else {
                    System.out.println("Error parsing: " + mapLayer.getHighlight());
                }
            }
        }
    }

    public void onClick$selectAll(Event event) {
        if (legend != null) {
            for (Listitem li : (List<Listitem>) legend.getItems()) {
                if (li.getFirstChild().getChildren().size() > 0
                        && !((Checkbox) li.getFirstChild().getFirstChild()).isChecked()) {
                    ((Checkbox) li.getFirstChild().getFirstChild()).setChecked(true);
                }
            }
            updateD();
            mapLayer.setHighlight(getSelectionFacet());
            getMapComposer().applyChange(mapLayer);
            dCreateButtons.setVisible(true);
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
