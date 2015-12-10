package au.org.ala.spatial.composer.layer;

import au.org.ala.spatial.StringConstants;
import au.org.ala.spatial.dto.ListEntryDTO;
import au.org.ala.spatial.util.CommonData;
import au.org.ala.spatial.util.Util;
import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.util.LayerUtilitiesImpl;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * @author ajay
 */
public class EnvironmentalList extends Listbox {

    private static final Logger LOGGER = Logger.getLogger(EnvironmentalList.class);
    private List<ListEntryDTO> listEntries;
    private float[] threasholds = {0.1f, 0.3f, 1.0f};
    private MapComposer mapComposer;
    private boolean includeAnalysisLayers;
    private boolean disableContextualLayers;
    private boolean singleDomain;

    public void init(MapComposer mc, boolean includeAnalysisLayers, boolean disableContextualLayers, boolean singleDomain) {
        mapComposer = mc;
        this.includeAnalysisLayers = includeAnalysisLayers;
        this.disableContextualLayers = disableContextualLayers;
        this.singleDomain = singleDomain;

        try {
            setupListEntries();

            setupList();

            this.setMultiple(true);

        } catch (Exception e) {
            LOGGER.error("error with initial setip of Environmental List", e);
        }
    }

    void setupListEntries() {
        listEntries = new ArrayList<ListEntryDTO>();
        JSONArray ja = CommonData.getLayerListJSONArray();
        for (int i = 0; i < ja.size(); i++) {
            JSONObject field = (JSONObject) ja.get(i);
            JSONObject layer = (JSONObject) field.get("layer");
            listEntries.add(
                    new ListEntryDTO(field.get(StringConstants.ID).toString(),
                            field.containsKey(StringConstants.NAME) ? field.get(StringConstants.NAME).toString() : field.get(StringConstants.ID).toString(),
                            layer.containsKey(StringConstants.CLASSIFICATION1) ? layer.get(StringConstants.CLASSIFICATION1).toString() : "",
                            layer.containsKey(StringConstants.CLASSIFICATION2) ? layer.get(StringConstants.CLASSIFICATION2).toString() : "",
                            layer.containsKey(StringConstants.TYPE) ? layer.get(StringConstants.TYPE).toString() : "",
                            layer.containsKey(StringConstants.DOMAIN) ? layer.get(StringConstants.DOMAIN).toString() : "",
                            layer));
        }

        if (includeAnalysisLayers) {
            for (MapLayer ml : mapComposer.getAnalysisLayers()) {
                ListEntryDTO le = null;
                if (ml.getSubType() == LayerUtilitiesImpl.ALOC) {
                    le = new ListEntryDTO(ml.getName(), ml.getDisplayName(), StringConstants.ANALYSIS, StringConstants.CLASSIFICATION, "Contextual", null, null);
                } else if (ml.getSubType() == LayerUtilitiesImpl.MAXENT) {
                    le = new ListEntryDTO(ml.getName(), ml.getDisplayName(), StringConstants.ANALYSIS, StringConstants.PREDICTION, StringConstants.ENVIRONMENTAL, null, null);
                } else if (ml.getSubType() == LayerUtilitiesImpl.GDM) {
                    le = new ListEntryDTO(ml.getName(), ml.getDisplayName(), StringConstants.ANALYSIS, StringConstants.GDM, StringConstants.ENVIRONMENTAL, null, null);
                } else if (ml.getSubType() == LayerUtilitiesImpl.ODENSITY) {
                    le = new ListEntryDTO(ml.getName(), ml.getDisplayName(), StringConstants.ANALYSIS, StringConstants.OCCURRENCE_DENSITY, StringConstants.ENVIRONMENTAL, null, null);
                } else if (ml.getSubType() == LayerUtilitiesImpl.SRICHNESS) {
                    le = new ListEntryDTO(ml.getName(), ml.getDisplayName(), StringConstants.ANALYSIS, StringConstants.SPECIES_RICHNESS, StringConstants.ENVIRONMENTAL, null, null);
                }
                if (le != null) {
                    listEntries.add(le);
                }
            }
        }

        java.util.Collections.sort(listEntries, new Comparator<ListEntryDTO>() {

            @Override
            public int compare(ListEntryDTO e1, ListEntryDTO e2) {
                return (e1.getCatagory1() + " " + e1.getCatagory2() + " " + e1.getDisplayName()).compareTo(e2.getCatagory1() + " " + e2.getCatagory2() + " " + e2.getDisplayName());
            }
        });
    }

    public void setupList() {
        try {
            setItemRenderer(new ListitemRenderer() {

                @Override
                public void render(Listitem li, Object data, int itemIdx) {
                    String type = ((ListEntryDTO) data).getType();

                    Image imgType = new Image();
                    if (StringConstants.ENVIRONMENTAL.equalsIgnoreCase(type)) {
                        imgType.setSrc("/img/icon_grid-layer.png");
                    } else {
                        imgType.setSrc("/img/icon_contextual-layer.png");
                    }
                    Listcell tc = new Listcell();
                    tc.setParent(li);
                    imgType.setParent(tc);

                    Listcell n = new Listcell(((ListEntryDTO) data).catagoryNames());

                    n.setParent(li);
                    n = new Listcell(((ListEntryDTO) data).getDisplayName());

                    n.setParent(li);

                    Listcell lc = new Listcell();
                    lc.setParent(li);
                    lc.setValue(data);

                    if (disableContextualLayers && StringConstants.CONTEXTUAL.equalsIgnoreCase(type)) {
                        li.setDisabled(true);
                    }

                    Html img = new Html("<i class='icon-info-sign'></i>");

                    img.addEventListener(StringConstants.ONCLICK, new EventListener() {

                        @Override
                        public void onEvent(Event event) throws Exception {
                            //re-toggle the checked flag (issue 572)
                            Listitem li = (Listitem) event.getTarget().getParent().getParent();
                            li.getListbox().toggleItemSelection(li);
                            EnvironmentalList el = (EnvironmentalList) li.getParent();
                            el.updateDistances();

                            String s = ((ListEntryDTO) ((Listcell) event.getTarget().getParent()).getValue()).getName();
                            String metadata = CommonData.getLayersServer() + "/layers/view/more/" + s;
                            mapComposer.activateLink(metadata, "Metadata", false);

                        }
                    });
                    img.setParent(lc);


                    if (StringConstants.ENVIRONMENTAL.equalsIgnoreCase(type)) {
                        float value = ((ListEntryDTO) data).getValue();
                        lc = new Listcell(" ");
                        if (threasholds[0] > value) {
                            lc.setSclass(StringConstants.LCRED);
                        } else if (threasholds[1] > value) {
                            lc.setSclass(StringConstants.LCYELLOW);
                        } else if (1 >= value) {
                            lc.setSclass(StringConstants.LCGREEN);
                        } else {
                            lc.setSclass(StringConstants.LCWHITE);
                        }
                        lc.setParent(li);
                    }
                }

            });

            SimpleListModel listModel = new SimpleListModel(listEntries);
            setModel(listModel);

            setMultiple(true);
        } catch (Exception e) {
            LOGGER.debug("error setting up env list", e);
        }
    }

    @Override
    public boolean isMultiple() {
        return true;
    }

    @Override
    public void toggleItemSelection(Listitem item) {
        super.toggleItemSelection(item);
        //update minimum distances here
        this.setMultiple(true);
    }

    public void updateDistances() {
        this.setMultiple(true);

        if (listEntries == null) {
            return;
        }

        String fieldId;

        for (ListEntryDTO le : listEntries) {
            //set to 2 for contextual and 'no association distance'
            if (StringConstants.CONTEXTUAL.equalsIgnoreCase(le.getType())
                    || le.getLayerObject() == null
                    || !le.getLayerObject().containsKey(StringConstants.FIELDS)
                    || (fieldId = getFieldId(le.getLayerObject())) == null
                    || CommonData.getDistancesMap().get(fieldId) == null) {
                le.setValue(2);
            } else {
                le.setValue(1);
            }
        }

        for (Object o : getSelectedItems()) {
            ListEntryDTO l = listEntries.get(((Listitem) o).getIndex());
            l.setValue(0);
            String[] domain;
            if (StringConstants.ENVIRONMENTAL.equalsIgnoreCase(l.getType())
                    && l.getLayerObject() != null && l.getLayerObject().containsKey(StringConstants.FIELDS)
                    && (fieldId = getFieldId(l.getLayerObject())) != null
                    && CommonData.getDistancesMap().get(fieldId) != null
                    && (domain = Util.getDomain(l.getLayerObject())).length > 0) {
                for (ListEntryDTO le : listEntries) {
                    if (le.getLayerObject() != null && le.getLayerObject().containsKey(StringConstants.FIELDS)
                            && (!singleDomain || Util.isSameDomain(Util.getDomain(le.getLayerObject()), domain))) {
                        String fieldId2 = getFieldId(le.getLayerObject());

                        Double d = CommonData.getDistancesMap().get(fieldId).get(fieldId2);
                        if (d != null) {
                            le.setValue((float) Math.min(le.getValue(), d));
                        }
                    }
                }
            }
        }

        for (int i = 0; i < listEntries.size(); i++) {
            float value = listEntries.get(i).getValue();
            String type = listEntries.get(i).getType();
            Listcell lc = (Listcell) (getItemAtIndex(i).getLastChild());
            if (StringConstants.ENVIRONMENTAL.equalsIgnoreCase(type)) {
                if (threasholds[0] > value) {
                    lc.setSclass(StringConstants.LCRED);
                } else if (threasholds[1] > value) {
                    lc.setSclass(StringConstants.LCYELLOW);
                } else if (1 >= value) {
                    lc.setSclass(StringConstants.LCGREEN);
                } else {
                    lc.setSclass(StringConstants.LCWHITE);
                }
            }
        }

        forceDomain();
    }

    void forceDomain() {
        String[] firstDomain = getFirstDomain();
        String[] thisDomain;

        if (!singleDomain || firstDomain.length == 0) {
            for (int i = 0; i < listEntries.size(); i++) {
                boolean defaultDisable = disableContextualLayers && StringConstants.CONTEXTUAL.equalsIgnoreCase(listEntries.get(i).getType());
                getItemAtIndex(i).setDisabled(defaultDisable);
            }
            return;
        }

        for (int i = 0; i < listEntries.size(); i++) {
            ListEntryDTO l = listEntries.get(i);
            if (l.getLayerObject() != null) {
                thisDomain = Util.getDomain(l.getLayerObject());

                if (thisDomain.length > 0) {
                    boolean defaultDisable = disableContextualLayers && StringConstants.CONTEXTUAL.equalsIgnoreCase(listEntries.get(i).getType());
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
    }

    String[] getFirstDomain() {
        String[] domain = {};

        for (Object o : getSelectedItems()) {
            ListEntryDTO l = listEntries.get(((Listitem) o).getIndex());
            if (StringConstants.ENVIRONMENTAL.equalsIgnoreCase(l.getType())
                    && l.getLayerObject() != null) {
                domain = Util.getDomain(l.getLayerObject());
                if (domain.length > 0) {
                    break;
                }
            }
        }

        return domain;
    }

    String getFieldId(JSONObject layerObject) {
        String fieldId = null;
        try {
            JSONArray ja = (JSONArray) layerObject.get(StringConstants.FIELDS);
            for (int i = 0; i < ja.size(); i++) {
                JSONObject jo = (JSONObject) ja.get(i);
                if (StringConstants.TRUE.equalsIgnoreCase(jo.get("analysis").toString())) {
                    fieldId = jo.get(StringConstants.ID).toString();
                    break;
                }
            }
        } catch (Exception e) {
            LOGGER.error("error getting field id from layer JSON object: " + layerObject, e);
        }
        return fieldId;
    }

    public void onSelect(Event event) {
        this.setMultiple(true);
        updateDistances();
    }

    public String[] getSelectedLayers() {
        this.setMultiple(true);

        Set selectedItems = getSelectedItems();
        String[] selected = new String[selectedItems.size()];
        int i = 0;
        LOGGER.debug("getSelectedLayers: ");
        for (Object o : selectedItems) {
            selected[i] = listEntries.get(((Listitem) o).getIndex()).getName();
            i++;
            LOGGER.debug(listEntries.get(((Listitem) o).getIndex()).getDisplayName() + ", " + listEntries.get(((Listitem) o).getIndex()).getName());
        }
        LOGGER.debug("");
        return selected;
    }

    public String[] getSelectedLayersWithDisplayNames() {
        this.setMultiple(true);

        Set selectedItems = getSelectedItems();
        String[] selected = new String[selectedItems.size()];
        int i = 0;
        LOGGER.debug("getSelectedLayers: ");
        for (Object o : selectedItems) {
            selected[i] = listEntries.get(((Listitem) o).getIndex()).getName() + "|" + listEntries.get(((Listitem) o).getIndex()).getDisplayName().replace(":", ";").replace('|', ';');
            i++;
            LOGGER.debug(listEntries.get(((Listitem) o).getIndex()).getDisplayName() + ", " + listEntries.get(((Listitem) o).getIndex()).getName());
        }
        LOGGER.debug("");
        return selected;
    }

    public void selectLayers(String[] layers) {
        this.setMultiple(true);

        String[] firstDomain = getFirstDomain();

        for (int i = 0; i < listEntries.size(); i++) {
            for (int j = 0; j < layers.length; j++) {
                if (listEntries.get(i).getDisplayName().equalsIgnoreCase(layers[j])
                        || listEntries.get(i).getName().equalsIgnoreCase(layers[j])) {
                    if (!getItemAtIndex(i).isSelected() && (!singleDomain || Util.isSameDomain(firstDomain, Util.getDomain(listEntries.get(i).getLayerObject())))) {
                        toggleItemSelection(getItemAtIndex(i));
                    }
                    break;
                }
            }
        }

        updateDistances();
    }

    public void selectLayersAndDisable(String[] layers) {
        this.setMultiple(true);

        if (layers == null) {
            return;
        }

        for (int i = 0; i < listEntries.size(); i++) {
            String f = CommonData.getLayerFacetName(listEntries.get(i).getName());

            for (int j = 0; j < layers.length; j++) {
                if (f != null && f.equalsIgnoreCase(layers[j])) {
                    toggleItemSelection(getItemAtIndex(i));
                    getItemAtIndex(i).setDisabled(true);

                    getItemAtIndex(i).setCheckable(false);
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
}
