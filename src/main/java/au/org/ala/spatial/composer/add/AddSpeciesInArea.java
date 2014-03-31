package au.org.ala.spatial.composer.add;

import au.org.ala.spatial.data.BiocacheQuery;
import au.org.ala.spatial.data.Query;
import au.org.ala.spatial.data.QueryUtil;
import au.org.ala.spatial.logger.RemoteLogger;
import au.org.ala.spatial.util.CommonData;
import au.org.ala.spatial.util.SelectedArea;
import au.org.ala.spatial.util.Util;
import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.settings.SettingsSupplementary;
import au.org.emii.portal.util.LayerUtilities;
import org.apache.log4j.Logger;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author ajay
 */
public class AddSpeciesInArea extends UtilityComposer {

    private static Logger logger = Logger.getLogger(AddSpeciesInArea.class);
    SettingsSupplementary settingsSupplementary;
    RemoteLogger remoteLogger;
    String selectedMethod = "";
    String pid = "";
    Radiogroup rgArea;
    Radio rAreaWorld, rAreaCustom, rAreaSelected, rAreaAustralia;
    Button btnCancel, btnOk;
    Textbox tToolName;
    boolean hasCustomArea = false;
    Query query;
    String rank;
    String taxon;
    private String name;
    private String s;
    private int type;
    private int featureCount;
    boolean filterGrid = false;
    boolean filter = false;
    boolean byLsid = false;
    private String metadata;
    private boolean allSpecies = false;
    private boolean[] geospatialKosher = null;
    String multipleSpeciesUploadName = null;
    Radio rAreaCurrent;

    @Override
    public void afterCompose() {
        super.afterCompose();
    }

    public void loadAreaLayers() {
        try {
            List<MapLayer> layers = getMapComposer().getPolygonLayers();
            for (int i = 0; i < layers.size(); i++) {
                MapLayer lyr = layers.get(i);
                Radio rAr = new Radio(lyr.getDisplayName());
                rAr.setId(lyr.getDisplayName().replaceAll(" ", ""));
                rAr.setValue(lyr.getWKT());
                rAr.setParent(rgArea);
                rgArea.insertBefore(rAr, rAreaCurrent);
            }

            if (!allSpecies) {
                rAreaSelected = rAreaWorld; //set as default in the zul
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
            logger.error("Unable to load active area layers:", e);
        }
    }

    public void onCheck$rgArea(Event event) {
        if (rgArea == null) {
            return;
        }
        rAreaSelected = rgArea.getSelectedItem();
        try {
            rAreaSelected = (Radio) ((org.zkoss.zk.ui.event.ForwardEvent) event).getOrigin().getTarget();
        } catch (Exception e) {
        }
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
                winProps.put("parent", this);
                winProps.put("parentname", "AddSpeciesInArea");
                winProps.put("query", query);
                winProps.put("rank", rank);
                winProps.put("taxon", taxon);
                winProps.put("name", name);
                winProps.put("s", s);
                winProps.put("featureCount", featureCount);
                winProps.put("filter", filter);
                winProps.put("filterGrid", filterGrid);
                winProps.put("byLsid", byLsid);
                winProps.put("metadata", metadata);
                winProps.put("type", type);

                Window window = (Window) Executions.createComponents("WEB-INF/zul/add/AddArea.zul", getMapComposer(), winProps);
                window.setAttribute("winProps", winProps, true);
                window.doModal();
            } else {
                onFinish();
            }
        } catch (Exception e) {
            logger.error("error finishing Add species in area request", e);
        }

        this.detach();
    }

    public void onFinish() {
        try {
            SelectedArea sa = getSelectedArea();

            //String spname = name + ";" + lsid;
            boolean setupMetadata = true;

            MapLayer ml = null;

            Query q = null;
            q = QueryUtil.queryFromSelectedArea(query, sa, true, geospatialKosher);

            if (byLsid) {
                ml = getMapComposer().mapSpecies(q, name, s, featureCount, type, sa.getWkt(), -1, MapComposer.DEFAULT_POINT_SIZE, MapComposer.DEFAULT_POINT_OPACITY,
                        Util.nextColour());
            } else if (filter) {
                ml = getMapComposer().mapSpecies(q, name, s, featureCount, type, sa.getWkt(), -1, MapComposer.DEFAULT_POINT_SIZE, MapComposer.DEFAULT_POINT_OPACITY,
                        Util.nextColour());
            } else if (filterGrid) {
                ml = getMapComposer().mapSpecies(q, name, s, featureCount, type, sa.getWkt(), -1, MapComposer.DEFAULT_POINT_SIZE, MapComposer.DEFAULT_POINT_OPACITY,
                        Util.nextColour());
            } else if (rank != null && taxon != null && q != null) {
                //String sptaxon = taxon+";"+lsid;
                ml = getMapComposer().mapSpecies(q, taxon, rank, -1, LayerUtilities.SPECIES, sa.getWkt(), -1, MapComposer.DEFAULT_POINT_SIZE,
                        MapComposer.DEFAULT_POINT_OPACITY, Util.nextColour());
                setupMetadata = false;
            } else {
                int results_count_occurrences = q.getOccurrenceCount();

                //test limit
                if (results_count_occurrences > 0
                        && results_count_occurrences <= settingsSupplementary.getValueAsInt("max_record_count_map")) {

                    String activeAreaLayerName = getSelectedAreaDisplayName();
                    String layerName = "Occurrences in " + activeAreaLayerName;

                    if (multipleSpeciesUploadName != null) {
                        layerName = multipleSpeciesUploadName;
                    } else {
                        if (q instanceof BiocacheQuery) {
                            String lsids = ((BiocacheQuery) q).getLsids();
                            if (lsids != null && lsids.length() > 0
                                    && lsids.split(",").length > 1) {
                                layerName = "Species assemblage";
                            }
                        }
                    }
                    ml = getMapComposer().mapSpecies(q, layerName, "species", results_count_occurrences, LayerUtilities.SPECIES, sa.getWkt(), -1,
                            MapComposer.DEFAULT_POINT_SIZE, MapComposer.DEFAULT_POINT_OPACITY, Util.nextColour());
                } else {
                    getMapComposer().showMessage(results_count_occurrences
                            + " occurrences in this area.\r\nSelect an area with fewer than "
                            + settingsSupplementary.getValueAsInt("max_record_count_map")
                            + " occurrences");
                }

                setupMetadata = false;
            }

            if (setupMetadata) {
                ml.getMapLayerMetadata().setMoreInfo(metadata);
            }

            logger.debug("metadata: " + metadata);

        } catch (Exception e) {
            logger.error("error adding species in area to map", e);
        }
    }

    public SelectedArea getSelectedArea() {
        String area = rAreaSelected.getValue();

        SelectedArea sa = null;
        try {
            if (area.equals("current")) {
                sa = new SelectedArea(null, getMapComposer().getViewArea());
            } else if (area.equals("australia")) {
                sa = new SelectedArea(null, CommonData.AUSTRALIA_WKT);
            } else if (area.equals("world")) {
                sa = new SelectedArea(null, CommonData.WORLD_WKT);
            } else {
                List<MapLayer> layers = getMapComposer().getPolygonLayers();
                for (MapLayer ml : layers) {
                    if (area.equals(ml.getWKT())) {
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
            logger.error("Unable to retrieve selected area", e);
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
        this.geospatialKosher = geospatialKosher;
    }

    void setMultipleSpeciesUploadName(String multipleSpeciesUploadName) {
        this.multipleSpeciesUploadName = multipleSpeciesUploadName;
    }
}
