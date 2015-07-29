/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.ala.spatial.composer.tool;

import au.org.ala.legend.Facet;
import au.org.ala.spatial.StringConstants;
import au.org.ala.spatial.composer.layer.ContextualLayersAutoComplete;
import au.org.ala.spatial.composer.results.PhylogeneticDiversityListResults;
import au.org.ala.spatial.util.CommonData;
import au.org.ala.spatial.util.Query;
import au.org.ala.spatial.util.Util;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.menu.SelectedArea;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.*;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author ajay
 */
public class PhylogeneticDiversityComposer extends ToolComposer {
    private static final Logger LOGGER = Logger.getLogger(PhylogeneticDiversityComposer.class);

    private Object[] trees;
    private Listbox treesList;
    private List<String> header;
    private Checkbox cAreasFromLayer;
    private Div divContextualLayer;
    private ContextualLayersAutoComplete autoCompleteLayers;
    private String autoCompleteLayerSelection;
    private List<SelectedArea> autoCompleteLayerAreas;
    private Query speciesLayerAsFilter;

    @Override
    public void afterCompose() {
        super.afterCompose();

        this.selectedMethod = "Phylogenetic Diversity";
        this.totalSteps = 2;

        this.loadAreaLayers(true);
        this.updateWindowTitle();

        fillPDTreeList();

        Map m = Executions.getCurrent().getArg();
        if (m != null) {
            for (Object o : m.entrySet()) {
                //apply preselected trees
                if (((Map.Entry) o).getKey() instanceof String
                        && "selectedTrees".equals(((Map.Entry) o).getKey())) {
                    String selectedTrees = (String) ((Map.Entry) o).getValue();
                    for (String tree : selectedTrees.split(",")) {
                        for (int i = 0; i < trees.length; i++) {
                            if (((Map<String, String>) trees[i]).containsValue(tree)) {
                                treesList.getItemAtIndex(i).setSelected(true);
                            }
                        }
                    }

                }

                //apply preselected areas
                if (((Map.Entry) o).getKey() instanceof String
                        && "selectedAreas".equals(((Map.Entry) o).getKey())) {
                    String selectedAreas = (String) ((Map.Entry) o).getValue();
                    for (String area : selectedAreas.split(",")) {
                        List checkboxes = getFellow("vboxArea").getChildren();
                        for (int i = 0; i < checkboxes.size(); i++) {
                            if (checkboxes.get(i) instanceof Checkbox &&
                                    ((Checkbox) checkboxes.get(i)).getLabel().equals(area)) {
                                ((Checkbox) checkboxes.get(i)).setChecked(true);
                            }
                        }
                    }
                }

                //apply query filter from mapped species occurrences layer
                if (((Map.Entry) o).getKey() instanceof String
                        && "query".equals(((Map.Entry) o).getKey())) {
                    String layerName = (String) ((Map.Entry) o).getValue();
                    for (MapLayer ml : getMapComposer().getSpeciesLayers()) {
                        if (ml.getSpeciesQuery() != null) {
                            //TODO: review matching criteria
                            if (ml.getName().equals(layerName)) {
                                speciesLayerAsFilter = ml.getSpeciesQuery();
                                
                                //set label on step 1
                                getFellow("divQueryFilter").setVisible(true);
                                ((Label) getFellow("lblQueryFilter")).setValue("Limited to species that appear in the layer: " + ml.getDisplayName());
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    private void fillPDTreeList() {
        JSONArray ja = null;
        String url = CommonData.getSettings().getProperty(CommonData.PHYLOLIST_URL) + "/phylo/getExpertTrees";
        JSONParser jp = new JSONParser();
        try {
            ja = (JSONArray) jp.parse(Util.readUrl(url));
        } catch (ParseException e) {
            LOGGER.error("failed to parse getExpertTrees");
        }

        if (ja == null || ja.size() == 0) {
            Events.echoEvent("onClose", this, null);

            getMapComposer().showMessage("Phylogenetic diversity tool is currently unavailable.");
            return;
        }

        trees = new Object[ja.size()];
        header = new ArrayList<String>();

        //restrict header to what is in the zul
        for (Component c : getFellow(StringConstants.TREES_HEADER).getChildren()) {
            header.add(c.getId().substring(3));
        }

        int row = 0;
        for (int i = 0; i < ja.size(); i++) {
            JSONObject j = (JSONObject) ja.get(i);

            Map<String, String> pdrow = new HashMap<String, String>();

            for (Object o : j.keySet()) {
                String key = (String) o;
                if (j.containsKey(key) && j.get(key) != null) {
                    pdrow.put(key, j.get(key).toString());
                } else {
                    pdrow.put(key, null);
                }
            }

            trees[row] = pdrow;

            row++;
        }

        treesList.setModel(new

                        ListModelArray(trees, false)

        );
        treesList.setItemRenderer(
                new ListitemRenderer() {

                    public void render(Listitem li, Object data, int itemIdx) {
                        Map<String, String> map = (Map<String, String>) data;

                        for (int i = 0; i < header.size(); i++) {
                            String value = map.get(header.get(i));
                            if (value == null) {
                                value = "";
                            }

                            if ("treeViewUrl".equalsIgnoreCase(header.get(i))) {
                                Html img = new Html("<i class='icon-info-sign'></i>");
                                img.setAttribute("link", value.isEmpty() ? CommonData.getSettings().getProperty(CommonData.PHYLOLIST_URL) : value);

                                Listcell lc = new Listcell();
                                lc.setParent(li);
                                img.setParent(lc);
                                img.addEventListener(StringConstants.ONCLICK, new EventListener() {

                                    @Override
                                    public void onEvent(Event event) throws Exception {
                                        //re-toggle the checked flag
                                        Listitem li = (Listitem) event.getTarget().getParent().getParent();
                                        li.getListbox().toggleItemSelection(li);

                                        String metadata = (String) event.getTarget().getAttribute("link");
                                        getMapComposer().activateLink(metadata, "Metadata", false);

                                    }
                                });


                            } else {
                                Listcell lc = new Listcell(value);
                                lc.setParent(li);
                            }
                        }
                    }
                }

        );

        treesList.setMultiple(true);
    }

    @Override
    public boolean onFinish() {

        List<SelectedArea> sa = getSelectedAreas();
        Map<String, Object> hm = new HashMap<String, Object>();
        hm.put("selectedareas", sa);

        List<Object> st = new ArrayList<Object>();
        for (Object o : treesList.getSelectedItems()) {
            st.add(trees[((Listitem) o).getIndex()]);
        }

        hm.put("selectedtrees", st);
        
        hm.put("query", speciesLayerAsFilter);

        PhylogeneticDiversityListResults window = (PhylogeneticDiversityListResults) Executions.createComponents("WEB-INF/zul/results/PhylogeneticDiversityResults.zul", getMapComposer(), hm);
        try {
            window.setParent(getMapComposer());
            window.doModal();
        } catch (Exception e) {
            LOGGER.error("error opening PhylogeneticDiversityResults.zul", e);
        }

        detach();
        return true;
    }

    public List<SelectedArea> getSelectedAreas() {
        List<SelectedArea> selectedAreas = new ArrayList<SelectedArea>();

        Vbox vboxArea = (Vbox) getFellowIfAny("vboxArea");

        for (Component c : vboxArea.getChildren()) {
            if ((c instanceof Checkbox) && ((Checkbox) c).isChecked()) {
                SelectedArea sa = null;
                String area = ((Checkbox) c).getValue();
                try {
                    if (StringConstants.CURRENT.equals(area)) {
                        sa = new SelectedArea(null, getMapComposer().getViewArea());
                    } else if (StringConstants.AUSTRALIA.equals(area)) {
                        sa = new SelectedArea(null, CommonData.getSettings().getProperty(CommonData.AUSTRALIA_WKT));
                    } else if (StringConstants.WORLD.equals(area)) {
                        sa = new SelectedArea(null, CommonData.WORLD_WKT);
                    } else {
                        List<MapLayer> layers = getMapComposer().getPolygonLayers();
                        for (MapLayer ml : layers) {
                            if (area.equals(ml.getWKT())) {
                                sa = new SelectedArea(ml, null);
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
                    LOGGER.warn("Unable to retrieve selected area", e);
                }
                if (sa != null) {
                    selectedAreas.add(sa);
                }
            }

        }
        
        //add all areas from a selection
        if (autoCompleteLayerSelection != null && cAreasFromLayer.isChecked()) {
            if (autoCompleteLayerAreas == null) {
                String fieldId = CommonData.getLayerFacetNameDefault(autoCompleteLayerSelection);

                JSONParser jp = new JSONParser();
                JSONObject objJson = null;
                try {
                    objJson = (JSONObject) jp.parse(Util.readUrl(CommonData.getLayersServer() + "/field/" + fieldId));
                } catch (ParseException e) {
                    LOGGER.error("failed to parse for: " + fieldId);
                }
                JSONArray objects = (JSONArray) objJson.get("objects");

                autoCompleteLayerAreas = new ArrayList();
                for (int i=0;i<objects.size();i++) {
                    MapLayer ml = createMapLayerForObject((JSONObject) objects.get(i));
                    SelectedArea sa = new SelectedArea(ml, null);

                    autoCompleteLayerAreas.add(sa);
                }
            }
            selectedAreas.addAll(autoCompleteLayerAreas);
        }
        return selectedAreas;
    }

    @Override
    public void onClick$btnOk(Event event) {
        if (currentStep == 1 && getSelectedAreas().isEmpty() && !((Radio) getFellow("rAreaCustom")).isSelected()) {
            //must have 1 or more areas selected
            Messagebox.show("Select one or more areas.");
        } else if (currentStep == 2 && treesList.getSelectedItems().isEmpty()) {
            //must have 1 or more tree selected
            Messagebox.show("Select one or more Phylogenetic Trees.");
        } else {
            super.onClick$btnOk(event);
        }
    }
    
    public void onCheck$cAreasFromLayer(Event event) {
        divContextualLayer.setVisible(cAreasFromLayer.isChecked());
    }

    public void onChange$autoCompleteLayers(Event event) {

        autoCompleteLayerSelection = null;
        autoCompleteLayerAreas = null;
        
        if (autoCompleteLayers.getItemCount() > 0 && autoCompleteLayers.getSelectedItem() != null) {
            JSONObject jo = autoCompleteLayers.getSelectedItem().getValue();
            
            autoCompleteLayerSelection = (String) jo.get(StringConstants.NAME);
            
        } else {

            // if the autocomplete has been type, but before selecting an option,
            // the focus is lost (eg, clicking on the next button or on tree)
            // it generates an error. This should fix it. 
//            if (llc.tree.getSelectedItem() == null) {
//                return;
//            }
//
//            JSONParser jp = new JSONParser();
//            JSONObject joLayer = null;
//            try {
//                joLayer = (JSONObject) jp.parse(llc.tree.getSelectedItem().getTreerow().getAttribute("lyr").toString());
//            } catch (ParseException e) {
//
//            }
//            if (!StringConstants.CLASS.equals(joLayer.get(StringConstants.TYPE))) {
//
//                String metadata = CommonData.getLayersServer() + "/layers/view/more/" + joLayer.get(StringConstants.ID);
//
//                setLayer(joLayer.get(StringConstants.DISPLAYNAME).toString(), joLayer.get("displaypath").toString(), metadata,
//                        StringConstants.ENVIRONMENTAL.equalsIgnoreCase(joLayer.get(StringConstants.TYPE).toString()) ? LayerUtilitiesImpl.GRID : LayerUtilitiesImpl.CONTEXTUAL);
//            } else {
//                String classValue = joLayer.get(StringConstants.DISPLAYNAME).toString();
//                String layer = joLayer.get(StringConstants.LAYERNAME).toString();
//                String displaypath = CommonData.getGeoServer()
//                        + "/wms?service=WMS&version=1.1.0&request=GetMap&layers=ALA:Objects&format=image/png&viewparams=s:"
//                        + joLayer.get("displaypath");
//
//                displaypath = displaypath.replace("gwc/service/", "");
//
//                String metadata = CommonData.getLayersServer() + "/layers/view/more/" + joLayer.get(StringConstants.ID);
//
//                setLayer(layer + " - " + classValue, displaypath, metadata,
//                        StringConstants.ENVIRONMENTAL.equalsIgnoreCase(joLayer.get(StringConstants.TYPE).toString()) ? LayerUtilitiesImpl.GRID : LayerUtilitiesImpl.CONTEXTUAL);
//            }
//
//            //close parent if it is 'addlayerwindow'
//            if (getRoot().hasFellow("addlayerwindow")) {
//                getRoot().getFellow("addlayerwindow").detach();
//            }
        }
    }
    
    private MapLayer createMapLayerForObject(JSONObject objJson) {
        MapLayer mapLayer = new MapLayer();
        
        mapLayer.setPolygonLayer(true);

        Facet facet = null;
        
        //TODO: make it work with contextual layers not yet indexed
        List<Facet> facets = new ArrayList<Facet>();
        facets.add(new Facet((String) objJson.get(StringConstants.FID), "\"" + objJson.get(StringConstants.NAME) + "\"", true));
        mapLayer.setFacets(facets);

        DecimalFormat df = new DecimalFormat("###,###.##");
        mapLayer.setAreaSqKm(String.valueOf(df.format(objJson.get(StringConstants.AREA_KM))));

        mapLayer.setDisplayName((String) objJson.get(StringConstants.NAME));
        
        return mapLayer;
    }
}
