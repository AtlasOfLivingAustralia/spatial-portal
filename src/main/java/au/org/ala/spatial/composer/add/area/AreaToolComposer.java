/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.ala.spatial.composer.add.area;

import au.org.ala.legend.Facet;
import au.org.ala.spatial.StringConstants;
import au.org.ala.spatial.composer.add.AddFacetController;
import au.org.ala.spatial.composer.tool.ToolComposer;
import au.org.ala.spatial.logger.RemoteLogger;
import au.org.ala.spatial.util.*;
import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.menu.SelectedArea;
import au.org.emii.portal.util.LayerUtilitiesImpl;
import org.apache.log4j.Logger;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;

import java.util.List;
import java.util.Map;

/**
 * @author ajay
 */
public class AreaToolComposer extends UtilityComposer {

    private static final Logger LOGGER = Logger.getLogger(AreaToolComposer.class);
    protected boolean ok = false;
    protected String layerName;
    private boolean isAnalysisChild = false;
    private boolean isFacetChild = false;
    private RemoteLogger remoteLogger;
    private ToolComposer analysisParent = null;
    private AddFacetController facetParent = null;
    private Map winProps = null;

    @Override
    public void afterCompose() {
        super.afterCompose();

        Component parent = this.getParent();

        winProps = Executions.getCurrent().getArg();

        if (StringConstants.ADDTOOLWINDOW.equals(parent.getId())) {
            analysisParent = (ToolComposer) this.getParent();
            isAnalysisChild = true;
        } else if (StringConstants.ADDFACETWINDOW.equals(parent.getId())) {
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
        String parentname = (String) winProps.get(StringConstants.PARENTNAME);
        String areatype = (String) getMapComposer().getAttribute(StringConstants.ADDAREAWINDOW);
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

        //always map the user defined area
        MapLayer ml = getMapComposer().getMapLayer(layerName);
        if (ok && ml != null) {
            String displayName = ml.getDisplayName();
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
            remoteLogger.logMapArea(layerName + ((!layerName.equalsIgnoreCase(displayName)) ? " (" + displayName + ")" : ""), areatype, ml.testWKT(), activeLayerName, fromLayer);

            //warn user when reduced WKT may be used for analysis
            getMapComposer().warnForLargeWKT(ml);

            //upload this area and replace with WMS
            getMapComposer().replaceWKTwithWMS(ml);
        }

        if (isAnalysisChild) {
            analysisParent.resetWindow(ok ? layerName : null);
            try {
                remoteLogger.logMapArea(layerName, areatype, getMapComposer().getMapLayer(layerName).testWKT());
            } catch (Exception e) {
                LOGGER.error("error with remote logging", e);
            }
        } else if (isFacetChild) {
            facetParent.resetWindow(ok ? layerName : null);
            try {
                remoteLogger.logMapArea(layerName, areatype, getMapComposer().getMapLayer(layerName).testWKT());
            } catch (Exception e) {
                LOGGER.error("error with remote logging", e);
            }
        } else if (parentname != null && "AddSpeciesInArea".equals(parentname)) {
            //was OK clicked?
            if (ok) {
                //map
//                String wkt = null;
//                try {
//                    wkt = getMapComposer().getMapLayer(layerName).getWKT();
//                } catch (Exception e) {
//                    LOGGER.error("failed to get WKT for layer: " + layerName, e);
//                }
                MapLayer mapLayer = getMapComposer().getMapLayer(layerName);

                Query q = null;
                if (winProps.get(StringConstants.QUERY) != null) {
                    q = ((Query) winProps.get(StringConstants.QUERY));

                    List<Facet> facets = mapLayer.getFacets();
                    String wkt = facets == null ? mapLayer.getWKT() : null;
                    SelectedArea sa = new SelectedArea(getMapComposer().getMapLayer(layerName), wkt);

                    q = QueryUtil.queryFromSelectedArea(q, sa, true, null);
                }
                if (winProps.get(StringConstants.QUERY) == null) {
                    mapSpeciesInArea();
                } else if (winProps.get(StringConstants.FILTER) != null && (Boolean) winProps.get(StringConstants.FILTER)) {
                    ml = getMapComposer().mapSpecies(
                            q, (String) winProps.get(StringConstants.NAME), (String) winProps.get("s"), (Integer) winProps.get(StringConstants.FEATURE_COUNT),
                            (Integer) winProps.get(StringConstants.TYPE), null, -1, MapComposer.DEFAULT_POINT_SIZE, MapComposer.DEFAULT_POINT_OPACITY,
                            Util.nextColour(), false);
                    ml.getMapLayerMetadata().setMoreInfo((String) winProps.get("metadata"));

                } else if (winProps.get("filterGrid") != null && (Boolean) winProps.get("filterGrid")) {
                    ml = getMapComposer().mapSpecies(
                            q, (String) winProps.get(StringConstants.NAME), (String) winProps.get("s"), (Integer) winProps.get(StringConstants.FEATURE_COUNT),
                            (Integer) winProps.get(StringConstants.TYPE), null, -1, MapComposer.DEFAULT_POINT_SIZE, MapComposer.DEFAULT_POINT_OPACITY,
                            Util.nextColour(), false);
                    ml.getMapLayerMetadata().setMoreInfo((String) winProps.get("metadata"));

                } else if (winProps.get("byLsid") != null && (Boolean) winProps.get("byLsid")) {
                    ml = getMapComposer().mapSpecies(
                            q, (String) winProps.get(StringConstants.NAME), (String) winProps.get("s"), (Integer) winProps.get(StringConstants.FEATURE_COUNT),
                            (Integer) winProps.get(StringConstants.TYPE), null, -1, MapComposer.DEFAULT_POINT_SIZE, MapComposer.DEFAULT_POINT_OPACITY,
                            Util.nextColour(), false);
                    ml.getMapLayerMetadata().setMoreInfo((String) winProps.get("metadata"));
                } else {
                    getMapComposer().mapSpecies(
                            q,
                            (String) winProps.get(StringConstants.TAXON),
                            (String) winProps.get(StringConstants.RANK),
                            0, LayerUtilitiesImpl.SPECIES, null, -1, MapComposer.DEFAULT_POINT_SIZE, MapComposer.DEFAULT_POINT_OPACITY,
                            Util.nextColour(), false);
                }
                if (getMapComposer().getMapLayer(layerName) != null) {
                    String displayName = getMapComposer().getMapLayer(layerName).getDisplayName();

                    remoteLogger.logMapArea(layerName + ((!layerName.equalsIgnoreCase(displayName)) ? " (" + displayName + ")" : ""), areatype, null);
                }
            }
        }
    }

    /**
     * maps species in area for the topmost polygon layer
     */
    void mapSpeciesInArea() {
        List<MapLayer> layers = getMapComposer().getPolygonLayers();
        if (layers == null || layers.isEmpty()) {
            return;
        }
        try {
            BiocacheQuery sq;
            if (layers.get(0).getFacets() != null) {
                sq = new BiocacheQuery(null, null, null, layers.get(0).getFacets(), true, null);
            } else {
                String wkt = layers.get(0).getWKT();
                sq = new BiocacheQuery(null, wkt, null, null, true, null);
            }
            
            int resultsCountOccurrences = sq.getOccurrenceCount();

            //test limit
            if (resultsCountOccurrences > 0 && resultsCountOccurrences <= Integer.parseInt(CommonData.getSettings().getProperty(StringConstants.MAX_RECORD_COUNT_MAP))) {
                String activeAreaLayerName = layers.get(0).getDisplayName();
                getMapComposer().mapSpecies(sq, CommonData.lang("occurrences_in_area_prefix") + " " + activeAreaLayerName, StringConstants.SPECIES, resultsCountOccurrences, LayerUtilitiesImpl.SPECIES, null,
                        -1, MapComposer.DEFAULT_POINT_SIZE, MapComposer.DEFAULT_POINT_OPACITY, Util.nextColour(), false);
            } else {
                getMapComposer().showMessage(
                        CommonData.lang("error_too_many_occurrences_for_mapping")
                                .replace("<counted_occurrences>", resultsCountOccurrences + "")
                                .replace("<max_occurrences>", CommonData.getSettings().getProperty(StringConstants.MAX_RECORD_COUNT_MAP)));
            }
        } catch (NumberFormatException e) {
            LOGGER.error("error mapping species in an area", e);
        }
    }
}
