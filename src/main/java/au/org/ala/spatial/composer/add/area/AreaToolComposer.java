/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.ala.spatial.composer.add.area;

import au.org.ala.spatial.composer.add.AddFacetController;
import au.org.ala.spatial.composer.tool.ToolComposer;
import au.org.ala.spatial.data.BiocacheQuery;
import au.org.ala.spatial.data.Query;
import au.org.ala.spatial.data.QueryUtil;
import au.org.ala.spatial.logger.RemoteLogger;
import au.org.ala.spatial.util.SelectedArea;
import au.org.ala.spatial.util.Util;
import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.settings.SettingsSupplementary;
import au.org.emii.portal.util.LayerUtilities;
import org.apache.log4j.Logger;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;

import java.util.List;
import java.util.Map;

/**
 * @author ajay
 */
public class AreaToolComposer extends UtilityComposer {

    private static Logger logger = Logger.getLogger(AreaToolComposer.class);
    String layerName;
    SettingsSupplementary settingsSupplementary;
    RemoteLogger remoteLogger;
    boolean isAnalysisChild = false;
    boolean isFacetChild = false;
    ToolComposer analysisParent = null;
    AddFacetController facetParent = null;
    Map winProps = null;
    public boolean ok = false;

    @Override
    public void afterCompose() {
        super.afterCompose();
        //txtLayerName.setValue(getMapComposer().getNextAreaLayerName("My Area"));

        Component parent = this.getParent();
        //      logger.debug("Parent: " + parent.getId() + " - " + parent.getWidgetClass());

        winProps = Executions.getCurrent().getArg();

        if (parent.getId().equals("addtoolwindow")) {
            analysisParent = (ToolComposer) this.getParent();
            isAnalysisChild = true;
        } else if (parent.getId().equals("addfacetwindow")) {
            facetParent = (AddFacetController) this.getParent();
            isFacetChild = true;
        } else {
            isAnalysisChild = false;
            isFacetChild = false;
        }
    }

    @Override
    public void detach() {
        super.detach();
        String parentname = (String) winProps.get("parentname");
        String areatype = (String) getMapComposer().getAttribute("addareawindow");
        if (areatype == null) {
            areatype = "";
        } else {
            if (areatype.startsWith("ci")) {
                areatype = areatype.substring(2);
            }

            if (areatype.startsWith("Upload")) {
                areatype = "Import - Area " + areatype.substring(6);
            } else if (areatype.startsWith("WKT")) {
                areatype = "Import - Area WKT";
            } else {
                areatype = "Area - " + areatype;
            }
        }

        if (isAnalysisChild) {
            //analysisParent.hasCustomArea = true;
            analysisParent.resetWindow(ok ? layerName : null);
            try {
                remoteLogger.logMapArea(layerName, areatype, getMapComposer().getMapLayer(layerName).getWKT());
            } catch (Exception e) {
                logger.error("error with remote logging", e);
            }
        } else if (isFacetChild) {
            //analysisParent.hasCustomArea = true;
            facetParent.resetWindow(ok ? layerName : null);
            try {
                remoteLogger.logMapArea(layerName, areatype, getMapComposer().getMapLayer(layerName).getWKT());
            } catch (Exception e) {
                logger.error("error with remote logging", e);
            }
        } else if (parentname != null && parentname.equals("AddSpeciesInArea")) {
            //was OK clicked?
            if (ok) {
                //map
                String wkt = null;
                try {
                    wkt = getMapComposer().getMapLayer(layerName).getWKT();
                } catch (Exception e) {
                }

                Query q = null;
                if (winProps.get("query") != null) {
                    q = ((Query) winProps.get("query"));

                    SelectedArea sa = new SelectedArea(getMapComposer().getMapLayer(layerName),
                            (getMapComposer().getMapLayer(layerName).getFacets() == null ? wkt : null));

                    q = QueryUtil.queryFromSelectedArea(q, sa, true, null);
                }
                if (winProps.get("query") == null) {
                    mapSpeciesInArea();
                } else if (winProps.get("filter") != null && (Boolean) winProps.get("filter")) {
                    MapLayer ml = getMapComposer().mapSpecies(
                            q, (String) winProps.get("name"), (String) winProps.get("s"), (Integer) winProps.get("featureCount"),
                            (Integer) winProps.get("type"), wkt, -1, MapComposer.DEFAULT_POINT_SIZE, MapComposer.DEFAULT_POINT_OPACITY,
                            Util.nextColour());
                    ml.getMapLayerMetadata().setMoreInfo((String) winProps.get("metadata"));

                } else if (winProps.get("filterGrid") != null && (Boolean) winProps.get("filterGrid")) {
                    MapLayer ml = getMapComposer().mapSpecies(
                            q, (String) winProps.get("name"), (String) winProps.get("s"), (Integer) winProps.get("featureCount"),
                            (Integer) winProps.get("type"), wkt, -1, MapComposer.DEFAULT_POINT_SIZE, MapComposer.DEFAULT_POINT_OPACITY,
                            Util.nextColour());
                    ml.getMapLayerMetadata().setMoreInfo((String) winProps.get("metadata"));

                } else if (winProps.get("byLsid") != null && (Boolean) winProps.get("byLsid")) {
                    MapLayer ml = getMapComposer().mapSpecies(
                            q, (String) winProps.get("name"), (String) winProps.get("s"), (Integer) winProps.get("featureCount"),
                            (Integer) winProps.get("type"), wkt, -1, MapComposer.DEFAULT_POINT_SIZE, MapComposer.DEFAULT_POINT_OPACITY,
                            Util.nextColour());
                    ml.getMapLayerMetadata().setMoreInfo((String) winProps.get("metadata"));
                } else {
                    MapLayer ml = getMapComposer().mapSpecies(
                            q,
                            (String) winProps.get("taxon"),
                            (String) winProps.get("rank"),
                            0, LayerUtilities.SPECIES, wkt, -1, MapComposer.DEFAULT_POINT_SIZE, MapComposer.DEFAULT_POINT_OPACITY,
                            Util.nextColour());
                }
                if (getMapComposer().getMapLayer(layerName) != null) {
                    String displayName = getMapComposer().getMapLayer(layerName).getDisplayName();
                    remoteLogger.logMapArea(layerName + ((!layerName.equalsIgnoreCase(displayName)) ? " (" + displayName + ")" : ""), areatype, wkt);
                }
            }
        } else {
            if (ok && getMapComposer().getMapLayer(layerName) != null) {
                String displayName = getMapComposer().getMapLayer(layerName).getDisplayName();
                String fromLayer = (String) getMapComposer().getAttribute("mappolygonlayer");
                String activeLayerName = (String) getMapComposer().getAttribute("activeLayerName");
                if (fromLayer == null) {
                    fromLayer = "";
                } else {
                    getMapComposer().removeAttribute("mappolygonlayer");
                }
                if (activeLayerName == null) {
                    activeLayerName = "";
                } else {
                    getMapComposer().removeAttribute("activeLayerName");
                }
                remoteLogger.logMapArea(layerName + ((!layerName.equalsIgnoreCase(displayName)) ? " (" + displayName + ")" : ""), areatype, getMapComposer().getMapLayer(layerName).getWKT(), activeLayerName, fromLayer);
            }
        }
    }

    /**
     * maps species in area for the topmost polygon layer
     */
    void mapSpeciesInArea() {
        List<MapLayer> layers = getMapComposer().getPolygonLayers();
        if (layers == null || layers.size() == 0) {
            return;
        }
        try {
            String wkt = layers.get(0).getWKT();

            BiocacheQuery sq = new BiocacheQuery(null, wkt, null, null, true, null);
            int results_count_occurrences = sq.getOccurrenceCount();

            //test limit
            if (results_count_occurrences > 0 && results_count_occurrences <= getMapComposer().getSettingsSupplementary().getValueAsInt("max_record_count_map")) {
                String activeAreaLayerName = layers.get(0).getDisplayName();
                getMapComposer().mapSpecies(sq, "Occurrences in " + activeAreaLayerName, "species", results_count_occurrences, LayerUtilities.SPECIES, wkt,
                        -1, MapComposer.DEFAULT_POINT_SIZE, MapComposer.DEFAULT_POINT_OPACITY, Util.nextColour());
            } else {
                getMapComposer().showMessage(results_count_occurrences
                        + " occurrences in this area.\r\nSelect an area with fewer than "
                        + settingsSupplementary.getValueAsInt("max_record_count_map")
                        + " occurrences");
            }
        } catch (Exception e) {
            logger.error("error mapping species in an area", e);
        }
    }
}
