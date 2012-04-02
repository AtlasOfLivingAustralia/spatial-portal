package org.ala.spatial.analysis.web;

import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.util.LayerUtilities;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Set;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
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
    float[] threasholds = {0.1f, 0.3f, 1.0f};
    SimpleListModel listModel;
    MapComposer mapComposer;
    boolean includeAnalysisLayers;
    boolean disableContextualLayers;
    boolean singleDomain;

    public void init(MapComposer mc, boolean includeAnalysisLayers, boolean disableContextualLayers, boolean singleDomain) {
        mapComposer = mc;
        this.includeAnalysisLayers = includeAnalysisLayers;
        this.disableContextualLayers = disableContextualLayers;
        this.singleDomain = singleDomain;

        try {
            setupListEntries();

            setupList();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void setupListEntries() {
        listEntries = new ArrayList<ListEntry>();
        JSONArray ja = CommonData.getLayerListJSONArray();
        for (int i = 0; i < ja.size(); i++) {
            JSONObject jo = ja.getJSONObject(i);
            listEntries.add(
                    new ListEntry(jo.getString("name"),
                    (jo.containsKey("displayname") ? jo.getString("displayname") : jo.getString("name")),
                    (jo.containsKey("classification1") ? jo.getString("classification1") : ""),
                    (jo.containsKey("classification2") ? jo.getString("classification2") : ""),
                    (jo.containsKey("type") ? jo.getString("type") : ""),
                    (jo.containsKey("domain") ? jo.getString("domain") : ""),
                    jo));
        }

        if (includeAnalysisLayers) {         //add
            for (MapLayer ml : mapComposer.getAnalysisLayers()) {
                ListEntry le = null;
                if (ml.getSubType() == LayerUtilities.ALOC) {
                    le = new ListEntry((String) ml.getData("pid"), ml.getDisplayName(), "Analysis", "Classification", "Contextual", null, null);
                } else if (ml.getSubType() == LayerUtilities.MAXENT) {
                    le = new ListEntry((String) ml.getData("pid"), ml.getDisplayName(), "Analysis", "Prediction", "Environmental", null, null);
                } else if (ml.getSubType() == LayerUtilities.GDM) {
                    le = new ListEntry((String) ml.getData("pid"), ml.getDisplayName(), "Analysis", "GDM", "Environmental", null, null);
                } else if (ml.getSubType() == LayerUtilities.ODENSITY) {
                    le = new ListEntry((String) ml.getData("pid"), ml.getDisplayName(), "Analysis", "Occurrence Density", "Environmental", null, null);
                } else if (ml.getSubType() == LayerUtilities.SRICHNESS) {
                    le = new ListEntry((String) ml.getData("pid"), ml.getDisplayName(), "Analysis", "Species Richness", "Environmental", null, null);
                }
                if (le != null) {
                    listEntries.add(le);
                }
            }
        }

        java.util.Collections.sort(listEntries, new Comparator<ListEntry>() {

            @Override
            public int compare(ListEntry e1, ListEntry e2) {
                return (e1.catagory1 + " " + e1.catagory2).compareTo(e2.catagory1 + " " + e2.catagory2);
            }
        });
    }

    public void setupList() {
        try {
            setItemRenderer(new ListitemRenderer() {

                @Override
                public void render(Listitem li, Object data) {
                    new Listcell(((ListEntry) data).catagoryNames()).setParent(li);
                    new Listcell(((ListEntry) data).displayname).setParent(li);

                    Listcell lc = new Listcell();
                    lc.setParent(li);
                    lc.setValue(((ListEntry) data));

                    String type = ((ListEntry) data).type;
                    if (disableContextualLayers && type.equalsIgnoreCase("contextual")) {
                        li.setDisabled(true);
                    }

                    Image img = new Image();
                    img.setSrc("/img/information.png");

                    img.addEventListener("onClick", new EventListener() {

                        @Override
                        public void onEvent(Event event) throws Exception {
                            //re-toggle the checked flag (issue 572)
                            Listitem li = (Listitem) event.getTarget().getParent().getParent();
                            li.getListbox().toggleItemSelection(li);
                            EnvironmentalList el = (EnvironmentalList) li.getParent();
                            el.updateDistances();

                            String s = ((ListEntry) ((Listcell) event.getTarget().getParent()).getValue()).name;
                            String metadata = CommonData.layersServer + "/layer/" + s;
                            mapComposer.activateLink(metadata, "Metadata", false);

                        }
                    });
                    img.setParent(lc);

                    //String type = ((ListEntry) data).type;

                    if (type.equalsIgnoreCase("environmental")) {
                        float value = ((ListEntry) data).value;
                        lc = new Listcell(" ");
                        if (threasholds[0] > value) {
                            lc.setSclass("lcRed");//setStyle("background: #bb2222;");
                        } else if (threasholds[1] > value) {
                            lc.setSclass("lcYellow");//lc.setStyle("background: #ffff22;");
                        } else if (1 >= value) {
                            lc.setSclass("lcGreen");//lc.setStyle("background: #22aa22;");
                        } else {
                            lc.setSclass("lcWhite");//setStyle("background: #ffffff;");
                        }
                        lc.setParent(li);
                    }
                }

                ;
            });

            listModel = new SimpleListModel(listEntries);
            setModel(listModel);

            renderAll();

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
        if (listEntries == null) {
            return;
        }

        String fieldId;

        for (ListEntry le : listEntries) {
            //set to 2 for contextual and 'no association distance'
            if (le.type.equalsIgnoreCase("contextual")
                    || le.layerObject == null
                    || !le.layerObject.containsKey("fields")
                    || (fieldId = getFieldId(le.layerObject)) == null
                    || CommonData.getDistancesMap().get(fieldId) == null) {
                le.value = 2;
            } else {
                le.value = 1;
            }
        }

        for (Object o : getSelectedItems()) {
            ListEntry l = listEntries.get(((Listitem) o).getIndex());
            l.value = 0;
            String[] domain;
            if (l.type.equalsIgnoreCase("environmental")
                    && l.layerObject != null && l.layerObject.containsKey("fields")
                    && (fieldId = getFieldId(l.layerObject)) != null
                    && CommonData.getDistancesMap().get(fieldId) != null
                    && (domain = getDomain(l.layerObject)) != null) {
                for (ListEntry le : listEntries) {
                    if (le.layerObject != null && le.layerObject.containsKey("fields")
                            && isSameDomain(getDomain(le.layerObject), domain)) {
                        String fieldId2 = getFieldId(le.layerObject);

                        Double d = CommonData.getDistancesMap().get(fieldId).get(fieldId2);
                        if (d != null) {
                            le.value = (float) Math.min(le.value, d.doubleValue());
                        }
                    }
                }
            }
        }

        for (int i = 0; i < listEntries.size(); i++) {
            float value = listEntries.get(i).value;
            String type = listEntries.get(i).type;
            Listcell lc = (Listcell) (getItemAtIndex(i).getLastChild());
            if (type.equalsIgnoreCase("environmental")) {
                if (threasholds[0] > value) {
                    lc.setSclass("lcRed");//setStyle("background: #bb2222;");
                } else if (threasholds[1] > value) {
                    lc.setSclass("lcYellow");//lc.setStyle("background: #ffff22;");
                } else if (1 >= value) {
                    lc.setSclass("lcGreen");//lc.setStyle("background: #22aa22;");
                } else {
                    lc.setSclass("lcWhite");//lc.setStyle("background: #ffffff;");
                }
            }
        }

        forceDomain();
    }

    void forceDomain() {
        String[] firstDomain = getFirstDomain();
        String[] thisDomain;

        if (!singleDomain || firstDomain == null) {
            for (int i = 0; i < listEntries.size(); i++) {
                boolean defaultDisable = disableContextualLayers && listEntries.get(i).type.equalsIgnoreCase("contextual");
                getItemAtIndex(i).setDisabled(defaultDisable);
            }
            return;
        }

        for (int i = 0; i < listEntries.size(); i++) {
            ListEntry l = listEntries.get(i);
            if (l.layerObject != null
                    && (thisDomain = getDomain(l.layerObject)) != null) {
                boolean defaultDisable = disableContextualLayers && listEntries.get(i).type.equalsIgnoreCase("contextual");
                boolean match = false;
                for (String d1 : firstDomain) {
                    for (String d2 : thisDomain) {
                        if (d1.equalsIgnoreCase(d2)) {
                            match = true;
                        }
                    }
                }
                getItemAtIndex(i).setDisabled(defaultDisable || !match);
                if (!match && getItemAtIndex(i).isSelected()) {
                    toggleItemSelection(getItemAtIndex(i));
                }
            }
        }
    }

    String[] getFirstDomain() {
        String[] domain = null;

        for (Object o : getSelectedItems()) {
            ListEntry l = listEntries.get(((Listitem) o).getIndex());
            if (l.type.equalsIgnoreCase("environmental")
                    && l.layerObject != null) {
                domain = getDomain(l.layerObject);
                if (domain != null) {
                    break;
                }
            }
        }

        return domain;
    }

    String[] getDomain(JSONObject layerObject) {
        if (!layerObject.containsKey("domain")) {
            return null;
        }
        String[] d = layerObject.getString("domain").split(",");
        for (int i = 0; i < d.length; i++) {
            d[i] = d[i].trim();
        }
        return d;
    }

    String getFieldId(JSONObject layerObject) {
        String fieldId = null;
        try {
            JSONArray ja = (JSONArray) layerObject.get("fields");
            for (int i = 0; i < ja.size(); i++) {
                JSONObject jo = (JSONObject) ja.get(i);
                if (true) { //jo.getString("analysis").equalsIgnoreCase("true")) {
                    fieldId = jo.getString("id");
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return fieldId;
    }

    public void onSelect(Event event) {
        updateDistances();
    }

    public String[] getSelectedLayers() {
        Set selectedItems = getSelectedItems();
        String[] selected = new String[selectedItems.size()];
        int i = 0;
        System.out.print("getSelectedLayers: ");
        for (Object o : selectedItems) {
            selected[i] = listEntries.get(((Listitem) o).getIndex()).name;
            i++;
            System.out.print(listEntries.get(((Listitem) o).getIndex()).displayname + ", " + listEntries.get(((Listitem) o).getIndex()).name);
        }
        System.out.println("");
        return selected;
    }

    public void selectLayers(String[] layers) {
        String[] firstDomain = getFirstDomain();

        for (int i = 0; i < listEntries.size(); i++) {
            for (int j = 0; j < layers.length; j++) {
                if (listEntries.get(i).displayname.equalsIgnoreCase(layers[j])
                        || listEntries.get(i).name.equalsIgnoreCase(layers[j])) {
                    if (!getItemAtIndex(i).isSelected() && isSameDomain(firstDomain, getDomain(listEntries.get(i).layerObject))) {
                        toggleItemSelection(getItemAtIndex(i));
                    }
                    break;
                }
            }
        }

        updateDistances();
    }

    @Override
    public void clearSelection() {
        updateDistances();
        super.clearSelection();
    }

    public boolean getIncludeAnalysisLayers() {
        return includeAnalysisLayers;
    }

    private boolean isSameDomain(String[] domain1, String[] domain2) {
        if (domain1 == null || domain2 == null) {
            return true;
        }

        for (String s1 : domain1) {
            for (String s2 : domain2) {
                if (s1.equalsIgnoreCase(s2)) {
                    return true;
                }
            }
        }

        return false;
    }
}
