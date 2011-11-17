package org.ala.spatial.analysis.web;

import au.com.bytecode.opencsv.CSVReader;
import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.util.LayerUtilities;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Comparator;
import org.ala.spatial.util.CommonData;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.A;
import org.zkoss.zul.Button;
import org.zkoss.zul.Doublebox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listhead;
import org.zkoss.zul.Listheader;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.ListitemRenderer;

/**
 *
 * @author ajay
 */
public class DistributionsWCController extends UtilityComposer {

    Label distributionLabel;
    Listbox distributionListbox;
    EventListener el;
    String[] text;
    ArrayList<String[]> original_data;
    String type;
    String original_count;
    Doublebox minDepth;
    Doublebox maxDepth;

    public void onClick$btnApplyDepthFilter() {
        ArrayList<String[]> new_data = new ArrayList<String[]>();
        for (int i = 0; i < original_data.size(); i++) {
            try {
                double min = Double.parseDouble(original_data.get(i)[7]);
                double max = Double.parseDouble(original_data.get(i)[8]);
                if ((minDepth.getValue() == null || minDepth.getValue() <= min)
                        && (maxDepth.getValue() == null || maxDepth.getValue() >= max)) {
                    new_data.add(original_data.get(i));
                }
            } catch (Exception e) {
            }
        }
        update(new_data, String.valueOf(new_data.size()));
    }

    public void onClick$btnClearDepthFilter() {
        minDepth.setValue(null);
        maxDepth.setValue(null);

        update(original_data, original_count);
    }

    public void onClick$btnDownload(Event event) {
        try {
            el.onEvent(event);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void init(String[] text, String type, String count, EventListener el) {
        this.el = el;
        this.original_count = count;
        this.type = type;
        this.text = text;
        if (text != null && text.length > 1) {
            ArrayList<String[]> data = new ArrayList<String[]>();
            for (int i = 1; i < text.length; i++) {
                if (text[i] != null && text[i].length() > 0) {
                    try {
                        StringReader sreader = new StringReader(text[i]);
                        CSVReader reader = new CSVReader(sreader);
                        data.add(reader.readNext());
                        reader.close();
                        sreader.close();
                    } catch (Exception e) {
                    }
                }
            }

            if (this.original_data == null) {
                this.original_data = data;
            }

            update(data, original_count);
        }
    }

    void update(ArrayList<String[]> data, String count) {
        distributionLabel.setValue("found " + count + " " + type + " in the Area");
        distributionListbox.setItemRenderer(new ListitemRenderer() {

            @Override
            public void render(Listitem li, Object data) {
                try {
                    String[] cells = (String[]) data;
                    if (cells.length > 0) {
                        Listcell lc = new Listcell();
                        if (!cells[0].equals("SPCODE")) {
                            Button b = new Button("map");
                            b.setSclass("goButton");
                            b.addEventListener("onClick", new EventListener() {

                                @Override
                                public void onEvent(Event event) throws Exception {
                                    //get spcode
                                    Listcell lc = (Listcell) event.getTarget().getParent().getNextSibling();
                                    String spcode = lc.getLabel();

                                    //map it
                                    String[] mapping = CommonData.getSpeciesDistributionWMSFromSpcode(spcode);
                                    MapLayer ml = getMapComposer().addWMSLayer(getMapComposer().getNextAreaLayerName(mapping[0] + " area"),mapping[0] + " area", mapping[1], 0.8f, null, mapping[2], LayerUtilities.WKT, null, null);
                                    MapComposer.setupMapLayerAsDistributionArea(ml);

                                    //disable this button
                                    ((Button) event.getTarget()).setDisabled(true);
                                }
                            });
                            b.setParent(lc);
                        }

                        lc.setParent(li);

                        for (int i = 0; i < 10; i++) { //exclude LSID
                            if (i == 9) { //metadata url
                                lc = new Listcell();
                                if (cells[i] != null && cells[i].length() > 0) {
                                    A a = new A("link");
                                    a.setHref(cells[i]);
                                    a.setTarget("_blank");
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
                }
            }
        });

        ListModelList lme = new ListModelList(data);
        distributionListbox.setModel(lme);

        Listhead head = distributionListbox.getListhead();

        for (int i = 0; i < head.getChildren().size(); i++) {
            Listheader lh = (Listheader) head.getChildren().get(i);

            //-1 for first column containing buttons.
            if (i == 8 || i == 9) {       //min depth, max depth
                lh.setSortAscending(new DListComparator(true, true, i - 1));
                lh.setSortDescending(new DListComparator(false, true, i - 1));
            } else if (i > 0 && i < 9) { //exclude 'metadata link' and 'map button' headers
                lh.setSortAscending(new DListComparator(true, false, i - 1));
                lh.setSortDescending(new DListComparator(false, false, i - 1));
            }
        }
    }
}

class DListComparator implements Comparator {

    boolean ascending;
    boolean number;
    int index;

    public DListComparator(boolean ascending, boolean number, int index) {
        this.ascending = ascending;
        this.number = number;
        this.index = index;
    }

    public int compare(Object o1, Object o2) {
        String[] s1 = (String[]) o1;
        String[] s2 = (String[]) o2;
        int sort = 0;

        if (number) {
            Double d1 = null, d2 = null;
            try {
                d1 = Double.parseDouble(s1[index]);
            } catch (Exception e) {
            }
            try {
                d2 = Double.parseDouble(s2[index]);
            } catch (Exception e) {
            }
            if (d1 == null || d2 == null) {
                sort = (d1 == null ? 1 : 0) + (d2 == null ? -1 : 0);
            } else {
                sort = d1.compareTo(d2);
            }
        } else {
            String t1 = s1[index];
            String t2 = s2[index];
            if (t1 == null || t2 == null) {
                sort = (t1 == null ? 1 : 0) + (t2 == null ? -1 : 0);
            } else {
                sort = t1.compareTo(t2);
            }
        }

        return ascending ? sort : -sort;
    }
}
