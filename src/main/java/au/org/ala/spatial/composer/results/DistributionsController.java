package au.org.ala.spatial.composer.results;

import au.com.bytecode.opencsv.CSVReader;
import au.org.ala.spatial.StringConstants;
import au.org.ala.spatial.util.CommonData;
import au.org.ala.spatial.util.Util;
import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.util.LayerUtilitiesImpl;
import org.apache.log4j.Logger;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.*;

import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author ajay
 */
public class DistributionsController extends UtilityComposer {
    private static final String EXPERT_DISTRIBUTION_AREA_NAME = "Expert distribution";
    private static final Logger LOGGER = Logger.getLogger(DistributionsController.class);
    private Label distributionLabel;
    private Listbox distributionListbox;
    private String[] text;
    private List<String[]> originalData;
    private List<String[]> currentData;
    private String type;
    private String originalCount;
    private Doublebox minDepth;
    private Doublebox maxDepth;

    @Override
    public void afterCompose() {
        super.afterCompose();

        String title = null;
        String size = null;
        String[] table = null;

        Map m = Executions.getCurrent().getArg();
        if (m != null) {
            for (Object o : m.entrySet()) {
                if (((Map.Entry) o).getKey() instanceof String) {
                    if (StringConstants.TITLE.equals(((Map.Entry) o).getKey())) {
                        title = (String) ((Map.Entry) o).getValue();
                    }
                    if (StringConstants.SIZE.equals(((Map.Entry) o).getKey())) {
                        size = (String) ((Map.Entry) o).getValue();
                    }
                    if (StringConstants.TABLE.equals(((Map.Entry) o).getKey())) {
                        table = (String[]) ((Map.Entry) o).getValue();
                    }
                }
            }
        }

        //sometimes init is called directly
        if (table != null) {
            init(table, title, size);
        }
    }

    public void onClick$btnApplyDepthFilter() {
        List<String[]> newData = new ArrayList<String[]>();
        for (int i = 0; i < originalData.size(); i++) {
            try {
                double min = Double.parseDouble(originalData.get(i)[7]);
                double max = Double.parseDouble(originalData.get(i)[8]);
                if ((minDepth.getValue() == null || minDepth.getValue() <= min) && (maxDepth.getValue() == null || maxDepth.getValue() >= max)) {
                    newData.add(originalData.get(i));
                }
            } catch (Exception e) {
                LOGGER.error("error applying depth filter", e);
            }
        }
        update(newData, String.valueOf(newData.size()));
    }

    public void onClick$btnClearDepthFilter() {
        minDepth.setValue(null);
        maxDepth.setValue(null);

        update(originalData, originalCount);
    }

    public void onClick$btnDownload(Event event) {
        try {
            String spid = String.valueOf(System.currentTimeMillis());

            SimpleDateFormat date = new SimpleDateFormat(StringConstants.DATE);
            String sdate = date.format(new Date());

            StringBuilder sb = new StringBuilder();
            for (String s : text) {
                if (sb.length() > 0) {
                    sb.append("\n");
                }
                sb.append(s);
            }

            Filedownload.save(sb.toString(), "text/plain;charset=UTF-8", type + "_" + sdate + "_" + spid + ".csv");
        } catch (Exception e) {
            LOGGER.error("error downloading distributions area data", e);
        }
    }

    public void init(String[] text, String type, String count) {
        ((Caption) getFellow(StringConstants.CTITLE)).setLabel(type);

        this.originalCount = count;
        this.type = type;
        this.text = text.clone();
        if (text != null && text.length > 1) {
            List<String[]> data = new ArrayList<String[]>();
            for (int i = 1; i < text.length; i++) {
                if (text[i] != null && text[i].length() > 0) {
                    try {
                        StringReader sreader = new StringReader(text[i]);
                        CSVReader reader = new CSVReader(sreader);

                        // last row indicates if it is mapped or not
                        String[] row = reader.readNext();
                        String[] newrow = java.util.Arrays.copyOf(row, 15);
                        try {
                            String niceAreaSqKm = String.format("%.1f", (float) Double.parseDouble(row[12]));
                            newrow[12] = niceAreaSqKm;
                        } catch (Exception e) {
                            LOGGER.error("error parsing area from: " + ((row != null && row.length > 12) ? row[12] : "bad data"));
                        }
                        data.add(newrow);

                        reader.close();
                        sreader.close();
                    } catch (Exception e) {
                        LOGGER.error("error reading distributions row", e);
                    }
                }
            }

            if (this.originalData == null) {
                this.originalData = data;
            }

            update(data, originalCount);
        }
    }

    void update(List<String[]> data, String count) {
        currentData = data;

        distributionLabel.setValue("found " + count + " " + type + " in the Area");
        distributionListbox.setItemRenderer(new ListitemRenderer() {

            @Override
            public void render(Listitem li, Object data, int itemIdx) {
                try {
                    String[] cells = (String[]) data;
                    li.setValue(cells);
                    if (cells.length > 0) {
                        Listcell lc = new Listcell();
                        if (!"SPCODE".equals(cells[0])) {
                            Button b = new Button("map");
                            b.setSclass("goButton");
                            //NC 2013-08-15: The commented out section of code below is causing http://code.google.com/p/ala/issues/detail?id=237
                            // But I am not sure if this is going to cause an regression issues.
                            if ((CommonData.getSpeciesChecklistWMSFromSpcode(cells[0]).length > 0 && getMapComposer().getMapLayerWMS(CommonData.getSpeciesChecklistWMSFromSpcode(cells[0])[1]) != null)
                                    || (CommonData.getSpeciesDistributionWMSFromSpcode(cells[0]).length > 0 && getMapComposer()
                                    .getMapLayerWMS(CommonData.getSpeciesDistributionWMSFromSpcode(cells[0])[1]) != null)) {
                                b.setDisabled(true);
                            } else {
                                b.addEventListener(StringConstants.ONCLICK, new EventListener() {

                                    @Override
                                    public void onEvent(Event event) throws Exception {
                                        // get spcode
                                        Listcell lc = (Listcell) event.getTarget().getParent().getNextSibling();
                                        String spcode = lc.getLabel();

                                        // row as metadata
                                        Listitem li = (Listitem) lc.getParent();
                                        String[] row = li.getValue();
                                        String layerName = getMapComposer().getNextAreaLayerName(row[0] + " area");
                                        String html = Util.getMetadataHtmlForDistributionOrChecklist(row[0], row, layerName);

                                        // map it
                                        String[] mapping = CommonData.getSpeciesDistributionWMSFromSpcode(spcode);
                                        if (mapping.length == 0) {
                                            mapping = CommonData.getSpeciesChecklistWMSFromSpcode(spcode);
                                        }
                                        String displayName = mapping[0] + " area";
                                        if (row[11] != null && row[11].length() > 0) {
                                            displayName = row[11];
                                        }
                                        MapLayer ml = getMapComposer().addWMSLayer(layerName, displayName, mapping[1], 0.6f, html, null, LayerUtilitiesImpl.WKT, null, null);
                                        ml.setSPCode(row[0]);
                                        getMapComposer().setupMapLayerAsDistributionArea(ml);
                                        getMapComposer().updateLayerControls();

                                        // disable this button
                                        ((Button) event.getTarget()).setDisabled(true);

                                        // flag as mapped by area_name or spcode
                                        for (int i = 0; i < originalData.size(); i++) {
                                            if (originalData.get(i)[0].length() > 0
                                                    && (originalData.get(i)[0].equals(row[0]) || (originalData.get(i)[11] != null && originalData.get(i)[11].length() > 0
                                                    && !originalData.get(i)[11].startsWith(EXPERT_DISTRIBUTION_AREA_NAME) && originalData.get(i)[11].equals(row[11])))) {
                                                originalData.get(i)[14] = "1";
                                            }
                                        }
                                        for (int i = 0; i < currentData.size(); i++) {
                                            if (currentData.get(i)[0].length() > 0
                                                    && (currentData.get(i)[0].equals(row[0]) || (currentData.get(i)[11] != null && currentData.get(i)[11].length() > 0
                                                    && !currentData.get(i)[11].startsWith(EXPERT_DISTRIBUTION_AREA_NAME) && currentData.get(i)[11].equals(row[11])))) {
                                                currentData.get(i)[14] = "1";
                                            }
                                        }
                                        for (int i = 0; i < distributionListbox.getItemCount(); i++) {
                                            String[] data = distributionListbox.getItemAtIndex(i).getValue();
                                            if (data != null && data[14] != null && data[14].length() > 0) {
                                                ((Button) distributionListbox.getItemAtIndex(i).getFirstChild().getFirstChild()).setDisabled(true);
                                            }
                                        }
                                    }
                                });
                            }
                            b.setParent(lc);
                        }

                        lc.setParent(li);

                        for (int i = 0; i < 14; i++) {
                            if (i == 9) {
                                // metadata url
                                lc = new Listcell();
                                if (cells[i] != null && cells[i].length() > 0) {
                                    A a = new A("link");
                                    a.setHref(cells[i]);
                                    a.setTarget(StringConstants.BLANK);
                                    a.setParent(lc);
                                }
                                lc.setParent(li);
                            } else if (i == 10) {
                                // lsid
                                lc = new Listcell();
                                if (cells[i] != null && cells[i].length() > 0) {
                                    A a = new A("more...");
                                    a.setHref(CommonData.getBieServer() + "/species/" + cells[i]);
                                    a.setTarget(StringConstants.BLANK);
                                    a.setParent(lc);
                                }
                                lc.setParent(li);
                            } else {
                                lc = new Listcell(cells[i]);
                                lc.setParent(li);
                            }
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error("error updating distributions list data", e);
                }
            }
        });

        ListModelList lme = new ListModelList(data);
        distributionListbox.setModel(lme);

        Listhead head = distributionListbox.getListhead();

        for (int i = 0; i < head.getChildren().size(); i++) {
            Listheader lh = (Listheader) head.getChildren().get(i);

            // -1 for first column containing buttons.
            if (i == 8 || i == 9 || i == 13) {
                // min depth, max depth, area_km
                lh.setSortAscending(new DListComparator(true, true, i - 1));
                lh.setSortDescending(new DListComparator(false, true, i - 1));
            } else if (i > 0 && i != 10 && i != 11) {
                // exclude 'map button',
                // 'metadata link', 'BIE
                // link'
                lh.setSortAscending(new DListComparator(true, false, i - 1));
                lh.setSortDescending(new DListComparator(false, false, i - 1));
            }
        }
    }
}

