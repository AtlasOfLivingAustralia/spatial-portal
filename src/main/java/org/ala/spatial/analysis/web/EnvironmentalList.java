package org.ala.spatial.analysis.web;

import au.org.emii.portal.composer.MapComposer;
import java.util.ArrayList;
import java.util.Set;
import org.ala.spatial.util.CommonData;
import org.ala.spatial.util.ListEntry;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Image;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.ListitemRenderer;
import org.zkoss.zul.SimpleListModel;

/**
 *
 * @author ajay
 */
public class EnvironmentalList extends Listbox {

    ArrayList<ListEntry> listEntries;
    float[][] distances;
    String[] layerNames;
    float[] threasholds = {0.1f, 0.3f, 1.0f};
    SimpleListModel listModel;
    MapComposer mapComposer;
    String satServer;
    boolean environmentalOnly;

    public void init(MapComposer mc, String sat_url, boolean environmental_only) {
        mapComposer = mc;
        satServer = sat_url;
        environmentalOnly = environmental_only;

        try {

            if (environmentalOnly) {
                listEntries = CommonData.getListEntriesEnv();
                distances = CommonData.getDistances();
                layerNames = CommonData.getLayerNamesEnv();
            } else {
                distances = null;
                listEntries = CommonData.getListEntriesAll();
                layerNames = CommonData.getLayerNamesAll();
            }

            setupEnvironmentalLayers(layerNames, distances);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void setupEnvironmentalLayers(String[] aslist, float[][] assocMx) {
        try {
            if (aslist.length > 0) {

                setItemRenderer(new ListitemRenderer() {

                    @Override
                    public void render(Listitem li, Object data) {
                        new Listcell(((ListEntry) data).catagoryNames()).setParent(li);
                        new Listcell(((ListEntry) data).displayname).setParent(li);

                        Listcell lc = new Listcell();
                        lc.setParent(li);
                        lc.setValue(((ListEntry) data).uid);
                        Image img = new Image();
                        img.setSrc("/img/information.png");
                        img.addEventListener("onClick", new EventListener() {

                            @Override
                            public void onEvent(Event event) throws Exception {
                                String s = (String) ((Listcell) event.getTarget().getParent()).getValue();
                                String metadata = satServer + "/layers/" + s;
                                mapComposer.activateLink(metadata, "Metadata", false);
                            }
                        });
                        img.setParent(lc);

                        if (environmentalOnly) {
                            float value = ((ListEntry) data).value;
                            lc = new Listcell(" ");
                            if (threasholds[0] > value) {
                                lc.setSclass("lcRed");//setStyle("background: #bb2222;");
                            } else if (threasholds[1] > value) {
                                lc.setSclass("lcYellow");//lc.setStyle("background: #ffff22;");
                            } else {
                                lc.setSclass("lcGreen");//lc.setStyle("background: #22aa22;");
                            }
                            lc.setParent(li);
                        }
                    }
                });

                listModel = new SimpleListModel(listEntries);
                setModel(listModel);

                //renderAll();
            }
        } catch (Exception e) {
            System.out.println("error setting up env list");
            e.printStackTrace(System.out);
        }
    }

    @Override
    public void toggleItemSelection(Listitem item) {
        super.toggleItemSelection(item);
        //update minimum distances here
    }

    public void updateDistances() {
        if(distances == null)
            return;
        
        for (ListEntry le : listEntries) {
            le.value = 1;
        }

        for (Object o : getSelectedItems()) {
            int row = listEntries.get(((Listitem) o).getIndex()).row_in_distances;

            for (ListEntry le : listEntries) {
                float d = getDistance(le.row_in_distances, row);
                le.value = Math.min(le.value, d);
            }
        }

        for (int i = 0; i < listEntries.size(); i++) {
            float value = listEntries.get(i).value;
            Listcell lc = (Listcell) (getItemAtIndex(i).getLastChild());
            if (threasholds[0] > value) {
                lc.setSclass("lcRed");//setStyle("background: #bb2222;");
            } else if (threasholds[1] > value) {
                lc.setSclass("lcYellow");//lc.setStyle("background: #ffff22;");
            } else {
                lc.setSclass("lcGreen");//lc.setStyle("background: #22aa22;");
            }
        }
    }

    public void onSelect(Event event) {
        if (environmentalOnly) {
            updateDistances();
        }
    }

    private float getDistance(int row, int row0) {
        //diagonal
        if (row == row0) {
            return 0;
        }

        //lower right matrix only
        int minrow, maxrow;
        if (row < row0) {
            minrow = row;
            maxrow = row0;
        } else {
            minrow = row0;
            maxrow = row;
        }

        //rows are 1-n, columns are 0-(n-1)
        if (maxrow - 1 < distances.length && minrow < distances[maxrow - 1].length) {
            return distances[maxrow - 1][minrow];
        }
        return 1;
    }

    public String[] getSelectedLayers() {
        Set selectedItems = getSelectedItems();
        String[] selected = new String[selectedItems.size()];
        int i = 0;
        System.out.print("getSelectedLayers: ");
        for (Object o : selectedItems) {
            selected[i] = listEntries.get(((Listitem) o).getIndex()).name;
            i++;
            System.out.print(listEntries.get(((Listitem) o).getIndex()).displayname + ", ");
        }
        System.out.println("");
        return selected;
    }

    void selectLayers(String[] layers) {
        for (int i = 0; i < listEntries.size(); i++) {
            for (int j = 0; j < layers.length; j++) {
                if (listEntries.get(i).displayname.equalsIgnoreCase(layers[j])
	                        || listEntries.get(i).name.equalsIgnoreCase(layers[j])) {
	                    toggleItemSelection(getItemAtIndex(i));
		                    break;
		                }
            }
        }
        updateDistances();
    }

    @Override
    public void clearSelection() {
        super.clearSelection();
        updateDistances();
    }
}