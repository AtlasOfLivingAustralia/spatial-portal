package au.org.emii.portal.composer.legend;

import au.com.bytecode.opencsv.CSVReader;
import au.org.ala.legend.Facet;
import au.org.ala.legend.Legend;
import au.org.ala.legend.LegendObject;
import au.org.ala.legend.QueryField;
import au.org.ala.spatial.StringConstants;
import au.org.ala.spatial.util.BiocacheQuery;
import au.org.ala.spatial.util.CommonData;
import au.org.ala.spatial.util.Query;
import au.org.ala.spatial.util.Util;
import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.util.LayerUtilitiesImpl;
import org.apache.log4j.Logger;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.CheckEvent;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.*;

import java.io.IOException;
import java.io.StringReader;
import java.util.*;

public class ClassificationLegend extends UtilityComposer {

    private static final Logger LOGGER = Logger.getLogger(ClassificationLegend.class);
    private Listbox legend;
    private int sliderMax = 500;
    private Query query;
    private String pid;
    private String colourmode;
    private MapLayer mapLayer;
    private String imagePath = "";
    private Button createInGroup;
    private Button createOutGroup;
    private Button clearSelection;
    private List<String[]> legendLines;
    private boolean readonly = false;
    private boolean checkmarks = false;
    private Doublebox dmin;
    private Doublebox dmax;
    private double minValue;
    private double maxValue;
    private double gMinValue;
    private double gMaxValue;
    private Label dlabel;
    private Div divContinous;
    private Checkbox dunknown;
    private Button dbutton;
    private boolean intContinous = false;
    private boolean isMonth = false;
    private boolean isNumber = false;
    private boolean disableselection = false;
    private Map<String, String> legendFacets = null;
    private Facet facet;
    private Div dCreateButtons;
    private Listheader lhFirstColumn;
    private Listheader lhSecondColumn;
    private Listheader lhThirdColumn;
    private Listheader lhFourthColumn;
    private Label lblSelectedCount;
    private Set selectedList = new HashSet();

    @Override
    public void afterCompose() {
        super.afterCompose();

        query = (Query) (Executions.getCurrent().getArg().get(StringConstants.QUERY));
        LOGGER.debug("Query q:" + query.getQ());
        mapLayer = (MapLayer) (Executions.getCurrent().getArg().get(StringConstants.LAYER));

        readonly = (Executions.getCurrent().getArg().get(StringConstants.READONLY)) != null;
        colourmode = (String) (Executions.getCurrent().getArg().get(StringConstants.COLOURMODE));
        pid = (String) (Executions.getCurrent().getArg().get(StringConstants.PID));

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

        getFellow("btnSearch").addEventListener(StringConstants.ONCLICK, new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                List<String[]> legendLinesFiltered = new ArrayList<String[]>();
                String txt = ((Textbox) getFellow("txtSearch")).getValue().toLowerCase();
                if (txt.length() > 0) {
                    for (int i = 0; i < legendLines.size(); i++) {
                        if (legendLines.get(i)[0].toLowerCase().contains(txt)) {
                            legendLinesFiltered.add(legendLines.get(i));
                        }
                    }
                    ((Button) getFellow("btnClear")).setDisabled(false);
                    legend.setModel(new SimpleListModel(legendLinesFiltered));
                    legend.setActivePage(0);
                }
            }
        });

        getFellow("btnClear").addEventListener(StringConstants.ONCLICK, new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                ((Textbox) getFellow("txtSearch")).setValue("");
                ((Button) getFellow("btnClear")).setDisabled(true);
                legend.setModel(new SimpleListModel(legendLines));
                legend.setActivePage(0);
            }
        });

        //setup sorting
        Comparator labelComparatorAsc = new Comparator() {

            @Override
            public int compare(Object o1, Object o2) {
                String[] s1 = (String[]) o1;
                String[] s2 = (String[]) o2;

                return s1[0].compareTo(s2[0]);
            }
        };

        Comparator labelComparatorDesc = new Comparator() {

            @Override
            public int compare(Object o1, Object o2) {
                String[] s1 = (String[]) o1;
                String[] s2 = (String[]) o2;

                return s2[0].compareTo(s1[0]);
            }
        };

        Comparator countComparatorAsc = new Comparator() {

            @Override
            public int compare(Object o1, Object o2) {
                Long l1 = null;
                Long l2 = null;
                try {
                    l1 = Long.parseLong(((String[]) o1)[4]);
                    l2 = Long.parseLong(((String[]) o2)[4]);

                    return l1.compareTo(l2);
                } catch (Exception e) {
                    if (l2 == null && l1 == null) {
                        return 0;
                    } else if (l2 == null) {
                        return 1;
                    } else {
                        return -1;
                    }
                }
            }
        };

        Comparator countComparatorDesc = new Comparator() {

            @Override
            public int compare(Object o1, Object o2) {
                Long l1 = null;
                Long l2 = null;
                try {
                    l1 = Long.parseLong(((String[]) o1)[4]);
                    l2 = Long.parseLong(((String[]) o2)[4]);

                    return l2.compareTo(l1);
                } catch (Exception e) {
                    if (l2 == null && l1 == null) {
                        return 0;
                    } else if (l2 == null) {
                        return -1;
                    } else {
                        return 1;
                    }
                }
            }
        };

        lhSecondColumn.setSortAscending(labelComparatorAsc);
        lhSecondColumn.setSortDescending(labelComparatorDesc);
        lhFourthColumn.setSortAscending(countComparatorAsc);
        lhFourthColumn.setSortDescending(countComparatorDesc);
    }

    private void rebuildSelectedList() {
        for (Listitem li : legend.getItems()) {
            if (!li.getFirstChild().getChildren().isEmpty()) {
                String v = ((Listcell) li.getChildren().get(1)).getLabel();

                if (((Checkbox) li.getFirstChild().getFirstChild()).isChecked()) {
                    //add if checked
                    selectedList.add(v);
                } else {
                    //remove it not checked
                    selectedList.remove(v);
                }
            }
        }

        lblSelectedCount.setValue(selectedList.size() + " selected");

        if (selectedList.size() > 0) {
            createInGroup.setVisible(true);
            if (createOutGroup != null) createOutGroup.setVisible(true);
            clearSelection.setVisible(true);
        } else {
            createInGroup.setVisible(false);
            if (createOutGroup != null) createOutGroup.setVisible(false);
            clearSelection.setVisible(false);
        }
    }

    public void onClick$createInGroup(Event e) {
        getMapComposer().mapSpecies(query.newFacet(facet, true),
                "Facet of " + mapLayer.getDisplayName(), StringConstants.SPECIES, -1, LayerUtilitiesImpl.SPECIES, null, -1, MapComposer.DEFAULT_POINT_SIZE,
                MapComposer.DEFAULT_POINT_OPACITY, Util.nextColour(), false);
    }

    public void onClick$createOutGroup(Event e) {
        getMapComposer().mapSpecies(query.newFacet(Facet.parseFacet("-(" + mapLayer.getHighlight() + ")"), false),
                "Facet of " + mapLayer.getDisplayName(), StringConstants.SPECIES, -1, LayerUtilitiesImpl.SPECIES, null, -1, MapComposer.DEFAULT_POINT_SIZE,
                MapComposer.DEFAULT_POINT_OPACITY, Util.nextColour(), false);
    }

    int[] getState() {
        int countChecked = 0;
        boolean unknownChecked = false;
        for (Listitem li : legend.getItems()) {
            if (!li.getFirstChild().getChildren().isEmpty()
                    && ((Checkbox) li.getFirstChild().getFirstChild()).isChecked()) {
                String v = ((Listcell) li.getChildren().get(1)).getLabel();
                v = displayToActualLabel(v);
                if (((query instanceof BiocacheQuery || divContinous.isVisible()) && StringConstants.UNKNOWN.equals(v))
                        || v.length() == 0) {
                    unknownChecked = true;
                } else {
                    countChecked++;
                }
            }
        }
        int[] state = new int[2];
        state[0] = selectedList.size();
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
            StringBuilder slist = new StringBuilder();

            if (query != null) {
                if (StringConstants.GRID.equals(colourmode)) {
                    slist.append("name,red,green,blue,count");
                    for (int i = 0; i < 600; i += 100) {
                        slist.append(">");

                        slist.append("\n").append(i).append(",").append(LegendObject.getRGB(Legend.getLinearColour(i, 0, 500, 0xFFFFFF00, 0xFFFF0000))).append(",");
                    }
                } else {
                    slist.append(query.getLegend(colourmode).getTable());
                }
            } else {
                return;
            }

            try {
                legendLines = new CSVReader(new StringReader(slist.toString())).readAll();

                //reset selection when legendLines is rebuilt
                uncheckAll();
            } catch (IOException e) {
                LOGGER.error("failed to read legend list as csv", e);
            }

            legendLines.remove(0);

            String h = mapLayer.getHighlight();
            facet = Facet.parseFacet(h);

            divContinous.setVisible(false);

            //test for range (user upload)
            if (legendLines.size() > 1) {
                String first = legendLines.get(0)[0];
                if (first == null || first.length() == 0 || first.startsWith(StringConstants.UNKNOWN)) {
                    first = legendLines.get(1)[0];
                }
                if (!checkmarks && query.getLegend(colourmode) != null
                        && query.getLegend(colourmode).getNumericLegend() != null) {
                    setupForNumericalList(h);
                    //test for manual range (solr query)
                } else if (StringConstants.OCCURRENCE_YEAR.equals(colourmode)) {
                    setupForBiocacheNumber(h, colourmode, true);
                } else if (StringConstants.OCCURRENCE_YEAR_DECADE.equals(colourmode) || StringConstants.DECADE.equals(colourmode)) {
                    setupForBiocacheDecade();
                } else if (StringConstants.COORDINATE_UNCERTAINTY.equals(colourmode) || StringConstants.UNCERTAINTY.equals(colourmode)) {
                    setupForBiocacheNumber(h, colourmode, false);
                } else if (StringConstants.MONTH.equals(colourmode)) {
                    setupForBiocacheMonth();
                }
            }

            /* apply something to line onclick in lb */
            legend.setItemRenderer(new ListitemRenderer() {

                @Override
                public void render(Listitem li, Object data, int itemIdx) {
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
                                String v = ((Listcell) event.getTarget().getParent().getParent().getChildren().get(1)).getLabel();
                                if (((CheckEvent) event).isChecked()) {
                                    selectedList.add(v);
                                } else {
                                    selectedList.remove(v);
                                }

                                lblSelectedCount.setValue(selectedList.size() + " selected");

                                if (selectedList.size() > 0) {
                                    createInGroup.setVisible(true);
                                    if (createOutGroup != null) createOutGroup.setVisible(true);
                                    clearSelection.setVisible(true);
                                } else {
                                    createInGroup.setVisible(false);
                                    if (createOutGroup != null) createOutGroup.setVisible(false);
                                    clearSelection.setVisible(false);
                                }

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

                    int red = 0, green = 0, blue = 0;
                    try {
                        red = Integer.parseInt(ss[1]);
                        green = Integer.parseInt(ss[2]);
                        blue = Integer.parseInt(ss[3]);
                    } catch (Exception e) {
                        LOGGER.error("error parsing colours : " + ss[0], e);
                    }

                    lc = new Listcell("   ");
                    lc.setStyle("background-color: rgb(" + red + "," + green
                            + "," + blue + "); color: rgb(" + red + "," + green
                            + "," + blue + ")");
                    lc.setParent(li);

                    //count
                    try {
                        //don't display if it is not a number
                        Long.parseLong(ss[4]);
                        lhFourthColumn.setVisible(true);
                        lc = new Listcell(ss[4]);
                        lc.setParent(li);
                    } catch (NumberFormatException e) {
                        lhFourthColumn.setVisible(false);
                        lhThirdColumn.setWidth("100%");
                        dCreateButtons.setVisible(!readonly);
                    }
                }
            });


            legend.setModel(new SimpleListModel(legendLines));

            createInGroup.setVisible(!disableselection && mapLayer.getHighlight() != null && mapLayer.getHighlight().length() > 0);
            if (createOutGroup != null) createOutGroup.setVisible(!disableselection && mapLayer.getHighlight() != null && mapLayer.getHighlight().length() > 0);
            clearSelection.setVisible(!disableselection && mapLayer.getHighlight() != null && mapLayer.getHighlight().length() > 0);

            if (divContinous.isVisible()) {
                int[] state = getState();
                if (state[0] > 1) {
                    setEnableContinousControls(false);
                } else {
                    updateD();
                }

                getFellow("txtSearch").setVisible(false);
            } else {
                getFellow("txtSearch").setVisible(true);
            }
        } catch (Exception e) {
            LOGGER.error("error building classification legend, pid: " + pid, e);
        }
    }

    String getSelectionFacet() {
        StringBuilder values = new StringBuilder();

        boolean unknown = false;
        for (Object selectedItem : selectedList) {
            String v = (String) selectedItem;
            v = displayToActualLabel(v);
            if (legendFacets != null && !StringConstants.UNKNOWN.equals(v)
                    && !v.contains(StringConstants.OCCURRENCE_YEAR)
                    && !v.contains(StringConstants.UNCERTAINTY)
                    && !v.contains(StringConstants.COORDINATE_UNCERTAINTY)) {

                v = legendFacets.get(v);
            }
            if (v.length() == 0 || ((query instanceof BiocacheQuery || divContinous.isVisible()) && StringConstants.UNKNOWN.equals(v))) {
                unknown = true;
            } else {
                if (values.length() > 0) {
                    values.append(" OR ");
                }
                if (legendFacets != null) {
                    values.append(v);
                } else {
                    values.append(colourmode).append(":\"");
                    values.append(v).append("\"");
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

        if (values.length() == 0
                && (minValue != gMinValue || maxValue != gMaxValue || !dunknown.isChecked())) {
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
        if (values.length() > 0) {
            facet = Facet.parseFacet(values.toString());
            return facet.toString();
        } else {
            facet = null;
            return "";
        }
    }

    public void onScroll$dslider(Event event) {
        //slider cannot be disabled
        if (!dmax.isDisabled()) {
            uncheckAll();
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
        for (Listitem li : legend.getItems()) {
            if (!li.getFirstChild().getChildren().isEmpty()
                    && ((Checkbox) li.getFirstChild().getFirstChild()).isChecked()) {
                ((Checkbox) li.getFirstChild().getFirstChild()).setChecked(false);
            }
        }

        selectedList = new HashSet();
        lblSelectedCount.setValue(selectedList.size() + " selected");
    }

    void updateD() {
        if (!dmin.isDisabled()) {
            double range = gMaxValue - gMinValue;
            if (range > 0) {
                dmin.setValue(minValue);
                dmax.setValue(maxValue);

                mapLayer.setHighlight(getSelectionFacet());
                Facet f = Facet.parseFacet(getSelectionFacet());
                int occurrencesCount = query.getOccurrenceCount();
                if (f != null) {
                    occurrencesCount = query.newFacet(f, false).getOccurrenceCount();
                }
                String minText = minValue == (int) minValue ? String.valueOf((int) minValue) : String.format("%f", minValue);
                String maxText = maxValue == (int) maxValue ? String.valueOf((int) maxValue) : String.format("%f", maxValue);
                String unknownText = dunknown.isChecked() ? " +Unknown" : " ";
                String selectedText;
                if (occurrencesCount == query.getOccurrenceCount()) {
                    selectedText = String.format(" (all of %d records selected)", occurrencesCount);
                } else {
                    selectedText = String.format(" (%d of %d records selected)", occurrencesCount, query.getOccurrenceCount());
                }

                createInGroup.setVisible(!disableselection && occurrencesCount != 0 && occurrencesCount != query.getOccurrenceCount());
                if (createOutGroup != null) createOutGroup.setVisible(!disableselection && occurrencesCount != 0 && occurrencesCount != query.getOccurrenceCount());
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

        if (!enable) {
            dlabel.setValue("list selection underway, range selection disabled");
        }
        dunknown.setDisabled(!enable);
    }

    private void setupForNumericalList(String facetString) {
        String h = facetString;
        legendFacets = new HashMap<String, String>();
        try {
            divContinous.setVisible(!disableselection);
            LegendObject lo = mapLayer.getLegendObject();
            if (lo.getFieldType() == QueryField.FieldType.INT
                    || lo.getFieldType() == QueryField.FieldType.LONG) {
                intContinous = true;
            }

            gMinValue = minValue = lo.getMinMax()[0];
            gMaxValue = maxValue = lo.getMinMax()[1];
            double rng = gMaxValue - gMinValue;
            if (intContinous && rng < sliderMax && rng > 0) {
                sliderMax = (int) rng;
            }

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
                LOGGER.error("error setting up the numerical listing", e);
            }
            for (int j = 0; j < legendLines.size(); j++) {
                String label = legendLines.get(j)[0];
                if (label.charAt(0) == '-') {
                    legendFacets.put(StringConstants.UNKNOWN, legendLines.get(j)[0]);
                    legendLines.get(j)[0] = StringConstants.UNKNOWN;
                } else {
                    String s = legendLines.get(j)[0];

                    String[] ss = s.substring(s.indexOf('[')).replace("[", "").replace("]", "").replace(StringConstants.DATE_TIME_END_OF_YEAR, "").replace(StringConstants.DATE_TIME_BEGINNING_OF_YEAR, "").split(" TO ");

                    float[] cutoffs = lo.getNumericLegend().getCutoffFloats();
                    float[] cutoffMins = lo.getNumericLegend().getCutoffMinFloats();
                    String range, strFacet;
                    double min;
                    double max;
                    double nextmin;
                    if (ss.length > 1) {
                        if ("*".equals(ss[0])) {
                            min = cutoffMins[0];
                            max = cutoffs[0];
                            nextmin = cutoffMins.length > 1 ? cutoffMins[1] : cutoffMins[0];
                        } else if ("*".equals(ss[1])) {
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
                        legendLines.get(j)[0] = range;
                        legendFacets.put(range, strFacet);
                    }
                }
            }
            checkmarks = true;
        } catch (Exception e) {
            divContinous.setVisible(false);
            LOGGER.error("error setting up continous legend listing", e);
        }
    }

    private void setupForBiocacheNumber(String facetString, String facetName, boolean integer) {
        String h = facetString;
        isNumber = true;
        intContinous = integer;
        legendFacets = new HashMap<String, String>();

        LegendObject lo = mapLayer.getLegendObject();
        if (lo != null) {
            //enable sliders
            divContinous.setVisible(!disableselection);
            gMinValue = minValue = lo.getMinMax()[0];
            gMaxValue = maxValue = lo.getMinMax()[1];
            if (intContinous) {
                sliderMax = Math.min(sliderMax, (int) (gMaxValue - gMinValue));
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
                LOGGER.error("error seting up numerical listing", e);
            }

            //update text in legend lines
            for (int j = 0; j < legendLines.size(); j++) {
                String label = legendLines.get(j)[0];
                if (label.charAt(0) == '-') {
                    legendFacets.put(StringConstants.UNKNOWN, legendLines.get(j)[0]);
                    legendLines.get(j)[0] = StringConstants.UNKNOWN;
                } else {
                    String s = legendLines.get(j)[0];

                    String[] ss = s.substring(s.indexOf('[')).replace("[", "").replace("]", "").replace(StringConstants.DATE_TIME_END_OF_YEAR, "").replace(StringConstants.DATE_TIME_BEGINNING_OF_YEAR, "").split(" TO ");

                    float[] cutoffs = lo.getNumericLegend().getCutoffFloats();
                    float[] cutoffMins = lo.getNumericLegend().getCutoffMinFloats();
                    String range, strFacet;
                    if (ss.length > 1) {
                        if ("*".equals(ss[0])) {
                            if (intContinous) {
                                if (cutoffs.length > 1) {
                                    range = String.format(">= %d and < %d", (int) cutoffMins[0], (int) cutoffMins[1]);
                                } else {
                                    range = String.format(">= %d and <= %d", (int) cutoffMins[0], (int) cutoffs[0]);
                                }
                                if (StringConstants.OCCURRENCE_YEAR.equals(colourmode) || StringConstants.OCCURRENCE_YEAR_DECADE.equals(colourmode) || StringConstants.DECADE.equals(colourmode)) {
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
                        } else if ("*".equals(ss[1])) {
                            if (intContinous) {

                                range = String.format("<= %d", (int) gMaxValue);

                                if (StringConstants.OCCURRENCE_YEAR.equals(colourmode) || StringConstants.OCCURRENCE_YEAR_DECADE.equals(colourmode) || StringConstants.DECADE.equals(colourmode)) {
                                    String minyear = String.valueOf((int) cutoffMins[cutoffMins.length - 1]);
                                    while (minyear.length() < 4) {
                                        minyear = "0" + minyear;
                                    }
                                    strFacet = "" + facetName + ":[" + minyear + "-01-01T00:00:00Z TO " + (int) gMaxValue + "-12-31T00:00:00Z]";
                                } else {
                                    strFacet = "" + facetName + ":[" + (int) cutoffMins[cutoffMins.length - 1] + " TO " + (int) gMaxValue + "]";
                                }
                            } else {

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

                                    range = String.format("<= %d", (int) cutoffs[pos]);
                                }
                                if (StringConstants.OCCURRENCE_YEAR.equals(colourmode) || StringConstants.OCCURRENCE_YEAR_DECADE.equals(colourmode) || StringConstants.DECADE.equals(colourmode)) {
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
                        legendLines.get(j)[0] = range;
                        legendFacets.put(range, strFacet);
                    }
                }
            }
        }
    }

    private void setupForBiocacheDecade() {
        isNumber = false;
        intContinous = true;
        legendFacets = new HashMap<String, String>();

        LegendObject lo = mapLayer.getLegendObject();
        if (lo != null) {
            //update text in legend lines
            for (int j = 0; j < legendLines.size(); j++) {
                String label = legendLines.get(j)[0];
                if (label.charAt(0) == '-') {
                    legendFacets.put(StringConstants.UNKNOWN, legendLines.get(j)[0]);
                    legendLines.get(j)[0] = StringConstants.UNKNOWN;
                } else {
                    String[] ss = legendLines.get(j)[0].split(" TO ");

                    String range, strFacet;
                    if (ss.length > 1) {
                        String yr = ss[1].substring(0, ss[1].length() - 2);

                        range = String.format("%s0s", yr);
                        strFacet = "occurrence_year:[" + yr + "0-01-01T00:00:00Z TO " + yr + "9-12-31T00:00:00Z]";

                        legendLines.get(j)[0] = range;
                        legendFacets.put(range, strFacet);
                    }
                }
            }
        }
    }

    private void setupForBiocacheMonth() {
        isMonth = true;
        legendFacets = new HashMap<String, String>();
        //update text in legend lines
        for (int j = 0; j < legendLines.size(); j++) {
            String label = legendLines.get(j)[0];
            if (label.charAt(0) == '-') {
                legendFacets.put(StringConstants.UNKNOWN, legendLines.get(j)[0]);
                legendLines.get(j)[0] = StringConstants.UNKNOWN;
            } else {
                String s = legendLines.get(j)[0];
                s = s.substring(s.indexOf('[') + 1, s.indexOf(']'));
                String[] ss = s.split(" TO ");
                if (ss.length > 1) {
                    if ("01".equals(ss[1])) {
                        s = StringConstants.JANUARY;
                    } else if ("02".equals(ss[1])) {
                        s = StringConstants.FEBRUARY;
                    } else if ("03".equals(ss[1])) {
                        s = StringConstants.MARCH;
                    } else if ("04".equals(ss[1])) {
                        s = StringConstants.APRIL;
                    } else if ("05".equals(ss[1])) {
                        s = StringConstants.MAY;
                    } else if ("06".equals(ss[1])) {
                        s = StringConstants.JUNE;
                    } else if ("07".equals(ss[1])) {
                        s = StringConstants.JULY;
                    } else if ("08".equals(ss[1])) {
                        s = StringConstants.AUGUST;
                    } else if ("09".equals(ss[1])) {
                        s = StringConstants.SEPTEMBER;
                    } else if ("10".equals(ss[1])) {
                        s = StringConstants.OCTOBER;
                    } else if ("11".equals(ss[1])) {
                        s = StringConstants.NOVEMBER;
                    } else if ("12".equals(ss[1])) {
                        s = StringConstants.DECEMBER;
                    }
                }
                legendFacets.put(s, "month:[" + ss[1] + " TO " + ss[1] + "]");
                legendLines.get(j)[0] = s;
            }

        }
    }


    public void checkboxClick(Event event) {
        //reload the map layer with this highlight.
        if (mapLayer != null) {
            mapLayer.setHighlight(getSelectionFacet());
            createInGroup.setVisible(!disableselection && mapLayer.getHighlight() != null && mapLayer.getHighlight().length() > 0);
            if (createOutGroup != null) createOutGroup.setVisible(!disableselection && mapLayer.getHighlight() != null && mapLayer.getHighlight().length() > 0);
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
                if (value.startsWith(StringConstants.UNKNOWN)) {
                    found = f.startsWith("-");
                } else if (StringConstants.JANUARY.equals(value)) {
                    found = f.contains(" 01");
                } else if (StringConstants.FEBRUARY.equals(value)) {
                    found = f.contains(" 02");
                } else if (StringConstants.MARCH.equals(value)) {
                    found = f.contains(" 03");
                } else if (StringConstants.APRIL.equals(value)) {
                    found = f.contains(" 04");
                } else if (StringConstants.MAY.equals(value)) {
                    found = f.contains(" 05");
                } else if (StringConstants.JUNE.equals(value)) {
                    found = f.contains(" 06");
                } else if (StringConstants.JULY.equals(value)) {
                    found = f.contains(" 07");
                } else if (StringConstants.AUGUST.equals(value)) {
                    found = f.contains(" 08");
                } else if (StringConstants.SEPTEMBER.equals(value)) {
                    found = f.contains(" 09");
                } else if (StringConstants.OCTOBER.equals(value)) {
                    found = f.contains(" 10");
                } else if (StringConstants.NOVEMBER.equals(value)) {
                    found = f.contains(" 11");
                } else if (StringConstants.DECEMBER.equals(value)) {
                    found = f.contains("11 TO *");
                }
                if (found) {
                    cb.setChecked(true);
                }
            } else if (!f.contains(":\"") && !f.contains(":*")) {
                //must be a continous selection
                try {
                    if (facet != null) {
                        if (value == null || value.length() == 0
                                || ((query instanceof BiocacheQuery || divContinous.isVisible())
                                && StringConstants.UNKNOWN.equals(value))) {
                            cb.setChecked(facet.isValid(""));
                        } else {
                            cb.setChecked(f.contains(legendFacets.get(value)));
                        }
                    } else {
                        LOGGER.debug("Error parsing: " + mapLayer.getHighlight());
                    }
                } catch (Exception e) {
                    LOGGER.error("error with continous values", e);
                }
            } else {
                Facet fct = Facet.parseFacet(f);
                if (fct != null) {
                    if (value == null || value.length() == 0
                            || ((query instanceof BiocacheQuery || divContinous.isVisible())
                            && StringConstants.UNKNOWN.equals(value))) {
                        cb.setChecked(fct.isValid(""));
                    } else {
                        cb.setChecked(fct.isValid(value));
                    }
                    if (cb.isChecked()) {
                        selectedList.add(value);
                        lblSelectedCount.setValue(selectedList.size() + " selected");
                    }
                } else {
                    LOGGER.debug("Error parsing: " + mapLayer.getHighlight());
                }
            }
        }
    }

    public void onClick$selectAll(Event event) {
        if (legend != null) {
            for (Listitem li : legend.getItems()) {
                if (!li.getFirstChild().getChildren().isEmpty()
                        && !((Checkbox) li.getFirstChild().getFirstChild()).isChecked()) {
                    ((Checkbox) li.getFirstChild().getFirstChild()).setChecked(true);

                    String v = ((Listcell) li.getChildren().get(1)).getLabel();
                    selectedList.add(v);
                }
            }
            updateD();
            mapLayer.setHighlight(getSelectionFacet());
            getMapComposer().applyChange(mapLayer);
            dCreateButtons.setVisible(true);
            clearSelection.setVisible(true);
            createInGroup.setVisible(true);
            if (createOutGroup != null) createOutGroup.setVisible(true);

            lblSelectedCount.setValue(selectedList.size() + " selected");
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
}
