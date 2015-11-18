package au.org.ala.spatial.composer.add;

import au.org.ala.spatial.StringConstants;
import au.org.ala.spatial.util.*;
import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.menu.SelectedArea;
import au.org.emii.portal.util.LayerUtilitiesImpl;
import org.apache.log4j.Logger;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Button;
import org.zkoss.zul.Radio;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Window;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author ajay
 */
public class AddSpeciesInArea extends UtilityComposer {

    private static final Logger LOGGER = Logger.getLogger(AddSpeciesInArea.class);
    private Radiogroup rgArea;
    private Radio rAreaWorld, rAreaCustom, rAreaSelected, rAreaAustralia;
    private Button btnOk;
    private Query query;
    private String rank;
    private String taxon;
    private boolean filterGrid = false;
    private boolean filter = false;
    private boolean byLsid = false;
    private String multipleSpeciesUploadName = null;
    private Radio rAreaCurrent;
    private String name;
    private String s;
    private int type;
    private int featureCount;
    private String metadata;
    private boolean allSpecies = false;
    private boolean[] geospatialKosher = null;
    private boolean expertDistributions;

    public void loadAreaLayers() {
        try {
            List<MapLayer> layers = null;
            Map m = args;
            if (m != null) {
                for (Object o : m.entrySet()) {
                    if (((Map.Entry) o).getKey() instanceof String
                            && ((Map.Entry) o).getKey().equals(StringConstants.POLYGON_LAYERS)) {
                        layers = (List<MapLayer>) ((Map.Entry) o).getValue();
                    }
                }
            }

            for (int i = 0; layers != null && i < layers.size(); i++) {
                MapLayer lyr = layers.get(i);
                Radio rAr = new Radio(lyr.getDisplayName());
                rAr.setId(lyr.getDisplayName().replaceAll(" ", ""));
                rAr.setValue(lyr.getName());
                rAr.setParent(rgArea);
                rgArea.insertBefore(rAr, rAreaCurrent);
            }

            if (!allSpecies) {
                //set as default in the zul
                rAreaSelected = rAreaWorld;
            } else {
                rAreaWorld.setVisible(false);
                rAreaAustralia.setVisible(false);

                for (int i = 0; i < rgArea.getItemCount(); i++) {
                    if (rgArea.getItemAtIndex(i).isVisible()) {
                        rAreaSelected = rgArea.getItemAtIndex(i);
                        rgArea.setSelectedItem(rAreaSelected);
                        Clients.evalJavaScript("jq('#" + rAreaSelected.getUuid() + "-real').attr('checked', true);");
                        break;
                    }
                }
            }

        } catch (Exception e) {
            LOGGER.error(StringConstants.UNABLE_TO_LOAD_ACTIVE_AREA_LAYERS, e);
        }
    }

    public void onCheck$rgArea(Event event) {
        if (rgArea == null) {
            return;
        }

        rAreaSelected = (Radio) ((org.zkoss.zk.ui.event.ForwardEvent) event).getOrigin().getTarget();
    }

    public void onClick$btnCancel(Event event) {
        this.detach();
    }

    public void onClick$btnOk(Event event) {
        if (btnOk.isDisabled()) {
            return;
        }

        try {
            if (rAreaSelected == rAreaCustom) {
                Map<String, Object> winProps = new HashMap<String, Object>();
                winProps.put(StringConstants.PARENT, this);
                winProps.put(StringConstants.PARENTNAME, "AddSpeciesInArea");
                winProps.put(StringConstants.QUERY, query);
                winProps.put(StringConstants.RANK, rank);
                winProps.put(StringConstants.TAXON, taxon);
                winProps.put(StringConstants.NAME, name);
                winProps.put("s", s);
                winProps.put(StringConstants.FEATURE_COUNT, featureCount);
                winProps.put(StringConstants.FILTER, filter);
                winProps.put("filterGrid", filterGrid);
                winProps.put("byLsid", byLsid);
                winProps.put("metadata", metadata);
                winProps.put(StringConstants.TYPE, type);

                Window window = (Window) Executions.createComponents("WEB-INF/zul/add/AddArea.zul", getParent(), winProps);
                window.setAttribute("winProps", winProps, true);
                window.setParent(getParent());
                window.doModal();
            } else {
                onFinish();
            }
        } catch (Exception e) {
            LOGGER.error("error finishing Add species in area request", e);
        }

        this.detach();
    }

    public void onFinish() {
        try {
            SelectedArea sa = getSelectedArea();

            boolean setupMetadata = true;

            MapLayer ml = null;

            Query q = QueryUtil.queryFromSelectedArea(query, sa, true, geospatialKosher);

            if (byLsid) {
                ml = getMapComposer().mapSpecies(q, name, s, featureCount, type, null, -1, MapComposer.DEFAULT_POINT_SIZE, MapComposer.DEFAULT_POINT_OPACITY,
                        Util.nextColour(), expertDistributions);
            } else if (filter) {
                ml = getMapComposer().mapSpecies(q, name, s, featureCount, type, null, -1, MapComposer.DEFAULT_POINT_SIZE, MapComposer.DEFAULT_POINT_OPACITY,
                        Util.nextColour(), expertDistributions);
            } else if (filterGrid) {
                ml = getMapComposer().mapSpecies(q, name, s, featureCount, type, null, -1, MapComposer.DEFAULT_POINT_SIZE, MapComposer.DEFAULT_POINT_OPACITY,
                        Util.nextColour(), expertDistributions);
            } else if (rank != null && taxon != null && q != null) {
                ml = getMapComposer().mapSpecies(q, taxon, rank, -1, LayerUtilitiesImpl.SPECIES, null, -1, MapComposer.DEFAULT_POINT_SIZE,
                        MapComposer.DEFAULT_POINT_OPACITY, Util.nextColour(), expertDistributions);
                setupMetadata = false;
            } else {
                int resultsCountOccurrences = q.getOccurrenceCount();

                //test limit
                if (resultsCountOccurrences > 0
                        && resultsCountOccurrences <= Integer.parseInt(CommonData.getSettings().getProperty(StringConstants.MAX_RECORD_COUNT_MAP))) {

                    String activeAreaLayerName = getSelectedAreaDisplayName();
                    String layerName = CommonData.lang("occurrences_in_area_prefix") + " " + activeAreaLayerName;

                    if (multipleSpeciesUploadName != null) {
                        layerName = multipleSpeciesUploadName;
                    } else {
                        if (q instanceof BiocacheQuery) {
                            String lsids = ((BiocacheQuery) q).getLsids();
                            if (lsids != null && lsids.length() > 0
                                    && lsids.split(",").length > 1) {
                                layerName = CommonData.lang("species_assemblage_layer_name");
                            }
                        }
                    }

                    ml = getMapComposer().mapSpecies(q, layerName, StringConstants.SPECIES, resultsCountOccurrences, LayerUtilitiesImpl.SPECIES, null, -1,
                            MapComposer.DEFAULT_POINT_SIZE, MapComposer.DEFAULT_POINT_OPACITY, Util.nextColour(), expertDistributions);
                } else {
                    getMapComposer().showMessage(
                            CommonData.lang("error_too_many_occurrences_for_mapping")
                                    .replace("<counted_occurrences>", resultsCountOccurrences + "")
                                    .replace("<max_occurrences>", CommonData.getSettings().getProperty(StringConstants.MAX_RECORD_COUNT_MAP)));
                }

                setupMetadata = false;
            }

            if (setupMetadata) {
                ml.getMapLayerMetadata().setMoreInfo(metadata);
            }

            LOGGER.debug("metadata: " + metadata);

        } catch (Exception e) {
            LOGGER.error("error adding species in area to map", e);
        }
    }

    public SelectedArea getSelectedArea() {
        String area = rAreaSelected.getValue();

        SelectedArea sa = null;
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
                    if (area.equals(ml.getName())) {
                        sa = new SelectedArea(ml, null);
                        break;
                    }
                }
                if (sa == null) {
                    //for 'all areas'
                    sa = new SelectedArea(null, area);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Unable to retrieve selected area", e);
        }

        return sa;
    }

    public String getSelectedAreaName() {
        String area = rAreaSelected.getLabel();
        List<MapLayer> layers = getMapComposer().getPolygonLayers();
        for (MapLayer ml : layers) {
            if (area.equals(ml.getDisplayName())) {
                area = ml.getName();
                break;
            }
        }

        return area;
    }

    public String getSelectedAreaDisplayName() {
        return rAreaSelected.getLabel();
    }

    void setSpeciesParams(Query q, String rank, String taxon) {
        this.query = q;
        this.rank = rank;
        this.taxon = taxon;
    }

    public void setSpeciesFilterGridParams(Query q, String name, String s, int featureCount, int type, String metadata, String rank) {
        this.query = q;
        this.name = name;
        this.s = s;
        this.featureCount = featureCount;
        this.type = type;
        this.filterGrid = true;
        this.metadata = metadata;
        this.rank = rank;
    }

    public void setSpeciesFilterParams(Query q, String name, String s, int featureCount, int type, String metadata, String rank) {
        this.query = q;
        this.name = name;
        this.s = s;
        this.featureCount = featureCount;
        this.type = type;
        this.filter = true;
        this.metadata = metadata;
        this.rank = rank;
    }

    void setSpeciesByLsidParams(Query q, String name, String s, int featureCount, int type, String metadata) {
        this.query = q;
        this.name = name;
        this.s = s;
        this.featureCount = featureCount;
        this.type = type;
        this.byLsid = true;
        this.metadata = metadata;
    }

    void setAllSpecies(boolean isAllSpecies) {
        this.allSpecies = isAllSpecies;
    }

    void setGeospatialKosher(boolean[] geospatialKosher) {
        this.geospatialKosher = (geospatialKosher == null) ? null : geospatialKosher.clone();
    }

    void setMultipleSpeciesUploadName(String multipleSpeciesUploadName) {
        this.multipleSpeciesUploadName = multipleSpeciesUploadName;
    }

    public void setExpertDistributions(boolean expertDistributions) {
        this.expertDistributions = expertDistributions;
    }
}
