package au.org.ala.spatial.composer.add;

import au.com.bytecode.opencsv.CSVReader;
import au.org.ala.legend.Facet;
import au.org.ala.legend.Legend;
import au.org.ala.legend.LegendObject;
import au.org.ala.legend.QueryField;
import au.org.ala.spatial.StringConstants;
import au.org.ala.spatial.util.*;
import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.menu.SelectedArea;
import au.org.emii.portal.util.LayerUtilitiesImpl;
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

    private static final Logger LOGGER = Logger.getLogger(AddFacetController.class);
    private double minValue;
    private double maxValue;
    private double gMinValue;
    private double gMaxValue;
    private Listbox legend;
    private String winTop = "300px";
    private String winLeft = "500px";
    private Map<String, Object> params;
    private int currentStep = 1, totalSteps = 2;
    private String selectedMethod = "";
    private Button btnCancel, btnOk, btnBack, clearSelection;
    private int sliderMax = 500;
    private Button createInGroup;
    private List<String[]> legendLines;
    private Doublebox dmin;
    private Doublebox dmax;
    private Label dlabel;
    private Div divContinous;
    private Checkbox dunknown;
    private Button dbutton;
    private boolean intContinous = false;
    private boolean disableselection = false;
    private Map<String, String> legendFacets = null;
    private Facet facet;
    private Div dCreateButtons;
    private Listheader lhFirstColumn;
    private Listheader lhSecondColumn;
    private Listheader lhThirdColumn;
    private Listheader lhFourthColumn;
    private Combobox cbColour;
    private boolean hasCustomArea = false;
    private Radiogroup rgArea, rgAreaHighlight, rgSpecies, rgSpeciesBk;
    private SelectedArea sa;
    private Query query;
    private Map map = new HashMap();
    private String colourmode;
    private boolean readonly = true;
    private boolean checkmarks = true;
    private MapComposer mc = getMapComposer();
    private Radio rAreaWorld, rAreaCustom, rAreaSelected, rAreaAustralia;
    private MapLayer prevTopArea = null;
    private Checkbox chkGeoKosherTrue, chkGeoKosherFalse;
    private LegendObject lo = null;
    private Checkbox cbContinousRange;
    private Label lblOccurrencesSelected;
    private Textbox txtSearch;
    private Label lblSelectedCount;
    private Set selectedList = new HashSet();

    @Override
    public void afterCompose() {
        super.afterCompose();
        winTop = this.getTop();
        winLeft = this.getLeft();
        setupDefaultParams();
        Map<String, Object> tmp = new HashMap<String, Object>();
        Map m = Executions.getCurrent().getArg();
        if (m != null) {
            for (Object entry : m.entrySet()) {
                if (((Map.Entry) entry).getKey() instanceof String) {
                    tmp.put((String) ((Map.Entry) entry).getKey(), ((Map.Entry) entry).getValue());
                }
            }
        }
        setParams(tmp);

        selectedMethod = "Add facet";
        updateWindowTitle();
        fixFocus();
        loadAreaLayers();
        if (rgArea.getSelectedItem() != null) {
            btnOk.setDisabled(false);
        } else {
            btnOk.setDisabled(true);
        }

        getFellow("btnSearch").addEventListener(StringConstants.ONCLICK, new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                List<String[]> legendLinesFiltered = new ArrayList<String[]>();
                String txt = txtSearch.getValue().toLowerCase();
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

    public void onSelect$legend(Event event) {
        rebuildSelectedList();
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

        lblSelectedCount.setValue(selectedList.size() + " facets selected");
    }


    private void setupDefaultParams() {
        btnOk.setDisabled(true);
    }

    public void setParams(Map<String, Object> params) {
        // iterate thru' the passed params and load them into the
        // existing default params
        if (this.params == null) {
            this.params = new HashMap<String, Object>();
        }
        if (params != null) {
            this.params.putAll(params);
        }
    }

    public void updateWindowTitle() {
        ((Caption) getFellow(StringConstants.CTITLE)).setLabel("Step " + currentStep + " of " + totalSteps + " - " + selectedMethod);
    }

    void fixFocus() {
        if (currentStep == 1) {
            rgArea.setFocus(true);
            rgArea.setSelectedItem(rAreaWorld);
        } else if (currentStep == 2) {
            cbColour.setFocus(true);
        }
    }

    public void onCheck$rgArea(Event event) {
        if (rgArea == null) {
            return;
        }
        hasCustomArea = false;
        rAreaSelected = rgArea.getSelectedItem();

        if (rAreaSelected == rAreaCustom) {
            hasCustomArea = false;
        }
        btnOk.setDisabled(false);
    }

    public SelectedArea getSelectedArea() {
        String area = rAreaSelected.getValue();
        SelectedArea selectedarea = null;
        try {
            if (StringConstants.CURRENT.equals(area)) {
                selectedarea = new SelectedArea(null, getMapComposer().getViewArea());
            } else if (StringConstants.AUSTRALIA.equals(area)) {
                selectedarea = new SelectedArea(null, CommonData.getSettings().getProperty(CommonData.AUSTRALIA_WKT));
            } else if (StringConstants.WORLD.equals(area)) {
                selectedarea = new SelectedArea(null, CommonData.WORLD_WKT);
            } else {
                List<MapLayer> layers = getMapComposer().getPolygonLayers();
                for (MapLayer ml : layers) {
                    if (area.equals(ml.getName())) {
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
            LOGGER.error("Unable to retrieve selected area", e);
        }

        return selectedarea;
    }

    public void onSelect$cbColour(Event event) {
        colourmode = cbColour.getSelectedItem().getValue();
        try {
            lo = query.getLegend(colourmode);
        } catch (Exception e) {
            LOGGER.error("error getting legend: " + query.getFullQ(false) + " colourmode:" + colourmode, e);
        }

        map.put(StringConstants.COLOURMODE, colourmode);

        buildLegend();
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
            } catch (Exception e) {
                LOGGER.error("failed to read legend list as csv", e);
            }

            legendLines.remove(0);

            String h = "";
            facet = null;

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
                                btnOk.setDisabled(false);
                                rebuildSelectedList();
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

                    if (cb != null) {
                        cb.setChecked(selectedList.contains(lc.getLabel()));
                    }

                    int red = 0, green = 0, blue = 0;
                    try {
                        red = Integer.parseInt(ss[1]);
                        green = Integer.parseInt(ss[2]);
                        blue = Integer.parseInt(ss[3]);
                    } catch (NumberFormatException e) {
                        LOGGER.error("error parsing colours : " + ss[0], e);
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

            legend.setModel(new SimpleListModel(legendLines));

        } catch (Exception e) {
            LOGGER.error("error rendering legend", e);
        }
    }

    String getSelectionFacet() {
        StringBuilder values = new StringBuilder();

        boolean unknown = false;
        if (!divContinous.isVisible() || !cbContinousRange.isChecked()) {
            for (Object selectedItem : selectedList) {
                String v = (String) selectedItem;
                v = displayToActualLabel(v);
                if (legendFacets != null) {
                    if (StringConstants.UNKNOWN.equals(v) || v.contains(StringConstants.OCCURRENCE_YEAR) || v.contains(StringConstants.UNCERTAINTY) || v.contains(StringConstants.COORDINATE_UNCERTAINTY)) {
                        //keep unchanged
                        divContinous.setVisible(true);
                        onCheck$cbContinousRange(null);
                    } else {
                        v = legendFacets.get(v);
                    }
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
        } else {
            if (minValue != gMinValue || maxValue != gMaxValue || !dunknown.isChecked()) {
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
            if (!li.getFirstChild().getChildren().isEmpty()
                    && ((Checkbox) li.getFirstChild().getFirstChild()).isChecked()) {
                ((Checkbox) li.getFirstChild().getFirstChild()).setChecked(false);
            }
        }

        selectedList = new HashSet();
        lblSelectedCount.setValue(selectedList.size() + " facets selected");
    }

    private void setupForNumericalList(String facetString) {
        String h = facetString;
        legendFacets = new HashMap<String, String>();
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
            if (intContinous && rng < sliderMax && rng > 0) {
                sliderMax = (int) rng;
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
                LOGGER.error("error with numerical legend", e);
            }
            for (int j = 0; j < legendLines.size(); j++) {

                String label = legendLines.get(j)[0];
                if (label.charAt(0) == '-') {
                    legendFacets.put(StringConstants.UNKNOWN, legendLines.get(j)[0]);
                    legendLines.get(j)[0] = StringConstants.UNKNOWN;
                } else {
                    String s = legendLines.get(j)[0];

                    String[] ss = s.replace("[", "").replace("]", "").replace(StringConstants.DATE_TIME_END_OF_YEAR, "").replace(StringConstants.DATE_TIME_BEGINNING_OF_YEAR, "").split(" TO ");

                    float[] cutoffs = lo.getNumericLegend().getCutoffFloats();
                    float[] cutoffMins = lo.getNumericLegend().getCutoffMinFloats();
                    String range, strFacet;
                    double min;
                    double max;
                    double nextmin;
                    if (ss.length > 1) {
                        if ("*".equals(ss[1])) {
                            min = cutoffMins[0];
                            max = cutoffs[0];
                            nextmin = cutoffMins.length > 1 ? cutoffMins[1] : cutoffMins[0];
                        } else if ("*".equals(ss[3])) {
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
            onCheck$cbContinousRange(null);
            LOGGER.error("error building numerical legend", e);
        }
    }

    private void setupForBiocacheNumber(String facetString, String facetName, boolean integer) {
        intContinous = integer;
        legendFacets = new HashMap<String, String>();

        if (lo != null) {
            //enable sliders
            divContinous.setVisible(!disableselection);
            onCheck$cbContinousRange(null);
            gMinValue = minValue = lo.getMinMax()[0];
            gMaxValue = maxValue = lo.getMinMax()[1];

            dmin.setValue(gMinValue);
            dmax.setValue(gMaxValue);

            if (intContinous) {
                sliderMax = Math.min(sliderMax, (int) (gMaxValue - gMinValue));
            }
            try {
                if (facet != null) {
                    //count OR and test for null
                    boolean nulls = facetString.contains("-(" + facetName + ":") || facetString.contains("-" + facetName + ":");
                    int countOr = facetString.split(" OR ").length;
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
                LOGGER.error("error building numberical legend", e);
            }

            //update text in legend lines
            for (int j = 0; j < legendLines.size(); j++) {

                String label = legendLines.get(j)[0];
                if (label.charAt(0) == '-') {
                    legendFacets.put(StringConstants.UNKNOWN, legendLines.get(j)[0]);
                    legendLines.get(j)[0] = StringConstants.UNKNOWN;
                } else {
                    String s = legendLines.get(j)[0];

                    String[] ss = s.replace("[", "").replace("]", "").replace(StringConstants.DATE_TIME_END_OF_YEAR, "").replace(StringConstants.DATE_TIME_BEGINNING_OF_YEAR, "").split(" TO ");

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
                                if (StringConstants.OCCURRENCE_YEAR.equals(colourmode)
                                        || StringConstants.OCCURRENCE_YEAR_DECADE.equals(colourmode)
                                        || StringConstants.DECADE.equals(colourmode)) {
                                    String minyear = String.valueOf((int) cutoffMins[0]);
                                    while (minyear.length() < 4) {
                                        minyear = "0" + minyear;
                                    }
                                    strFacet = "" + facetName + ":[" + minyear + StringConstants.DATE_TIME_END_OF_YEAR
                                            + " TO " + (int) cutoffs[0] + StringConstants.DATE_TIME_END_OF_YEAR + "]";
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

                                if (StringConstants.OCCURRENCE_YEAR.equals(colourmode)
                                        || StringConstants.OCCURRENCE_YEAR_DECADE.equals(colourmode)
                                        || StringConstants.DECADE.equals(colourmode)) {
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
                                if (StringConstants.OCCURRENCE_YEAR.equals(colourmode)
                                        || StringConstants.OCCURRENCE_YEAR_DECADE.equals(colourmode)
                                        || StringConstants.DECADE.equals(colourmode)) {
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
        intContinous = true;
        legendFacets = new HashMap<String, String>();

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

    public void onClick$clearSelection(Event e) {

        uncheckAll();
        btnOk.setDisabled(true);
    }

    public void onClick$selectAll(Event event) {
        if (legend != null && legend.getItemCount() > 0) {
            for (Listitem li : legend.getItems()) {
                if (!li.getFirstChild().getChildren().isEmpty()
                        && !((Checkbox) li.getFirstChild().getFirstChild()).isChecked()) {
                    ((Checkbox) li.getFirstChild().getFirstChild()).setChecked(true);

                    String v = ((Listcell) li.getChildren().get(1)).getLabel();
                    selectedList.add(v);
                }
            }
            btnOk.setDisabled(false);
        }

        lblSelectedCount.setValue(selectedList.size() + " facets selected");
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
                winProps.put(StringConstants.PARENT, this);
                winProps.put(StringConstants.PARENTNAME, "Tool");
                winProps.put(StringConstants.SELECTEDMETHOD, selectedMethod);

                List<MapLayer> layers = getMapComposer().getPolygonLayers();
                if (layers != null && !layers.isEmpty()) {
                    prevTopArea = layers.get(0);
                } else {
                    prevTopArea = null;
                }

                Window window = (Window) Executions.createComponents("WEB-INF/zul/add/AddArea.zul", this, winProps);
                window.setAttribute("winProps", winProps, true);
                window.setParent(this);
                window.doModal();

                return;
            }

            Div currentDiv = (Div) getFellowIfAny(StringConstants.ATSTEP + currentStep);
            Div nextDiv = (Div) getFellowIfAny(StringConstants.ATSTEP + (currentStep + 1));

            if (!currentDiv.getZclass().contains(StringConstants.LAST)) {
                currentDiv.setVisible(false);
                nextDiv.setVisible(true);

                Html previousStepCompletedImg = (Html) getFellowIfAny(StringConstants.IMG_COMPLETED_STEP + (currentStep));
                previousStepCompletedImg.setVisible(true);

                Label previousStepLabel = (Label) getFellowIfAny(StringConstants.LBLSTEP + (currentStep));
                previousStepLabel.setStyle(StringConstants.FONT_WEIGHT_NORMAL);

                Label currentStepLabel = (Label) getFellowIfAny(StringConstants.LBLSTEP + (currentStep + 1));
                currentStepLabel.setStyle(StringConstants.FONT_WEIGHT_BOLD);

                currentStep++;
                updateWindowTitle();
                fixFocus();
                sa = getSelectedArea();
                query = QueryUtil.queryFromSelectedArea(query, sa, false, getGeospatialKosher());
                if (query != null) {
                    List<QueryField> fields = query.getFacetFieldList();
                    Collections.sort(fields, new QueryField.QueryFieldComparator());

                    String lastGroup = null;

                    for (QueryField field : fields) {
                        String newGroup = field.getGroup().getName();
                        if (!newGroup.equals(lastGroup)) {
                            Comboitem sep = new Comboitem(StringConstants.SEPERATOR);
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
                if (selectedList.size() == 0) {
                    btnOk.setDisabled(true);
                }

                Facet f = Facet.parseFacet(getSelectionFacet());
                Query querynew = query.newFacet(f, true);
                if (querynew.getOccurrenceCount() <= 0) {
                    getMapComposer().showMessage(CommonData.lang(StringConstants.NO_OCCURRENCES_SELECTED));
                } else {
                    getMapComposer().mapSpecies(querynew,
                            StringConstants.MY_FACET, StringConstants.SPECIES, -1, LayerUtilitiesImpl.SPECIES, null, 0, MapComposer.DEFAULT_POINT_SIZE,
                            MapComposer.DEFAULT_POINT_OPACITY, Util.nextColour(), false);
                    this.detach();
                }
            }

        } catch (Exception ex) {
            LOGGER.error("error adding facet", ex);
        }

        fixFocus();
    }

    public void onClick$btnBack(Event event) {

        Div currentDiv = (Div) getFellowIfAny(StringConstants.ATSTEP + currentStep);

        Div previousDiv = currentStep > 1 ? (Div) getFellowIfAny(StringConstants.ATSTEP + (currentStep - 1)) : null;

        if (currentDiv.getZclass().contains(StringConstants.FIRST)) {
            btnBack.setDisabled(true);
        } else {
            currentDiv.setVisible(false);
            previousDiv.setVisible(true);

            Html currentStepCompletedImg = (Html) getFellowIfAny(StringConstants.IMG_COMPLETED_STEP + (currentStep - 1));
            currentStepCompletedImg.setVisible(false);

            Label nextStepLabel = (Label) getFellowIfAny(StringConstants.LBLSTEP + (currentStep));
            nextStepLabel.setStyle(StringConstants.FONT_WEIGHT_NORMAL);

            Label currentStepLabel = (Label) getFellowIfAny(StringConstants.LBLSTEP + (currentStep - 1));
            currentStepLabel.setStyle(StringConstants.FONT_WEIGHT_BOLD);

            currentStep--;

            btnBack.setDisabled(previousDiv.getZclass().contains(StringConstants.FIRST));
        }

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
                && StringConstants.RAREACUSTOMHIGHLIGHT.equals(rgAreaHighlight.getSelectedItem().getId());
    }

    public void onCheck$rgAreaHighlight(Event event) {
        if (rgAreaHighlight == null) {
            return;
        }
        if (StringConstants.RAREACUSTOMHIGHLIGHT.equals(rgAreaHighlight.getSelectedItem().getId())) {
            hasCustomArea = false;
        }
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

            String selectedLayerName = (String) params.get(StringConstants.POLYGON_LAYER_NAME);
            Radio rSelectedLayer = null;

            List<MapLayer> layers = getMapComposer().getPolygonLayers();
            for (int i = 0; i < layers.size(); i++) {
                MapLayer lyr = layers.get(i);
                Radio rAr = new Radio(lyr.getDisplayName());
                rAr.setValue(lyr.getName());

                rAr.setParent(rgArealocal);
                rgArealocal.insertBefore(rAr, rAreaCurrent);

                if (lyr.getName().equals(selectedLayerName)) {
                    rSelectedLayer = rAr;
                    rAreaSelected = rAr;
                }
            }

            if (selectedAreaName != null && !selectedAreaName.isEmpty()) {
                for (int i = 0; i < rgArealocal.getItemCount(); i++) {
                    if (rgArealocal.getItemAtIndex(i).isVisible() && rgArealocal.getItemAtIndex(i).getLabel().equals(selectedAreaName)) {
                        rAreaSelected = rgArealocal.getItemAtIndex(i);
                        LOGGER.debug("2.resetting indexToSelect = " + i);
                        rgArealocal.setSelectedItem(rAreaSelected);
                        break;
                    }
                }
            } else if (rSelectedLayer != null) {
                rAreaSelected = rSelectedLayer;
                rgArealocal.setSelectedItem(rAreaSelected);
            } else if (StringConstants.NONE.equals(selectedLayerName)) {
                rgArealocal.setSelectedItem(rAreaWorld);
                rAreaSelected = rAreaWorld;
                rgArealocal.setSelectedItem(rAreaSelected);
            } else {
                rAreaSelected = rAreaWorld;
                rgArealocal.setSelectedItem(rAreaSelected);

            }
            Clients.evalJavaScript("jq('#" + rAreaSelected.getUuid() + "-real').attr('checked', true);");

        } catch (Exception e) {
            LOGGER.error(StringConstants.UNABLE_TO_LOAD_ACTIVE_AREA_LAYERS, e);
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

            String selectedLayerName = (String) params.get(StringConstants.POLYGON_LAYER_NAME);
            Radio rSelectedLayer = null;

            List<MapLayer> layers = getMapComposer().getPolygonLayers();
            for (int i = 0; i < layers.size(); i++) {
                MapLayer lyr = layers.get(i);
                Radio rAr = new Radio(lyr.getDisplayName());
                rAr.setValue(lyr.getName());

                rAr.setParent(rgArealocal);
                rgArealocal.insertBefore(rAr, rAreaCurrentHighlight);

                if (lyr.getName().equals(selectedLayerName)) {
                    rSelectedLayer = rAr;
                }
            }

            if (selectedAreaName != null && !selectedAreaName.isEmpty()) {
                for (int i = 0; i < rgArealocal.getItemCount(); i++) {
                    if (rgArealocal.getItemAtIndex(i).isVisible() && rgArealocal.getItemAtIndex(i).getLabel().equals(selectedAreaName)) {
                        LOGGER.debug("2.resetting indexToSelect = " + i);
                        rgArealocal.setSelectedItem(rgArealocal.getItemAtIndex(i));
                        break;
                    }
                }
            } else if (rSelectedLayer != null) {
                rgArealocal.setSelectedItem(rAreaSelected);
            } else if (StringConstants.NONE.equals(selectedLayerName)) {
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
            LOGGER.error(StringConstants.UNABLE_TO_LOAD_ACTIVE_AREA_LAYERS, e);
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
                rAr.setValue(lyr.getName());
                rAr.setParent(rgArealocal);
                rgArealocal.insertBefore(rAr, rAreaCurrent);
            }

            rAreaNone.setSelected(true);
        } catch (Exception e) {
            LOGGER.error(StringConstants.UNABLE_TO_LOAD_ACTIVE_AREA_LAYERS, e);
        }
    }

    public void resetWindow(String selectedArea) {
        try {

            hasCustomArea = !(selectedArea == null || selectedArea.trim().isEmpty());

            boolean ok = false;
            if (hasCustomArea) {
                MapLayer curTopArea;
                List<MapLayer> layers = getMapComposer().getPolygonLayers();
                if (layers != null && !layers.isEmpty()) {
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
            LOGGER.error("Exception when resetting analysis window", ex);
        }
    }

    /**
     * TODO NC 2013-08-15: Remove the need for Constants.FALSE as the third item in the
     * array.
     *
     * @return
     */
    public boolean[] getGeospatialKosher() {
        return new boolean[]{chkGeoKosherTrue.isChecked(), chkGeoKosherFalse.isChecked(), false};
    }

    public void onCheck$chkGeoKosherTrue(Event event) {
        Event evt = ((ForwardEvent) event).getOrigin();
        if (!((CheckEvent) evt).isChecked() && !chkGeoKosherFalse.isChecked()) {
            chkGeoKosherFalse.setChecked(true);
        }
    }

    public void onCheck$chkGeoKosherFalse(Event event) {
        Event evt = ((ForwardEvent) event).getOrigin();
        if (!((CheckEvent) evt).isChecked() && !chkGeoKosherTrue.isChecked()) {
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
            if (!li.getFirstChild().getChildren().isEmpty()) {
                li.getFirstChild().getFirstChild().setVisible(!cbContinousRange.isChecked());
            }
        }

        if (!cbContinousRange.isChecked()) {
            dlabel.setValue(CommonData.lang(StringConstants.MSG_LIST_SELECTION_ENABLED));
        } else {
            dlabel.setValue(CommonData.lang(StringConstants.MSG_RANGE_SELECTION_ENABLED));
        }
    }
}
