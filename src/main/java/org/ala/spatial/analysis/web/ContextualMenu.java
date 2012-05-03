package org.ala.spatial.analysis.web;

import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.settings.SettingsSupplementary;
import au.org.emii.portal.util.LayerUtilities;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import org.ala.spatial.data.Query;
import org.ala.spatial.util.ScatterplotData;
import org.zkoss.zhtml.Li;
import org.zkoss.zhtml.Ul;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.A;
import org.zkoss.zul.Vbox;
import org.zkoss.zk.ui.util.Clients;

/**
 *
 * @author ajay
 */
public class ContextualMenu extends UtilityComposer {

    SettingsSupplementary settingsSupplementary;
    Vbox contents;

    @Override
    public void afterCompose() {
        super.afterCompose();
    }

    public void refresh() {
        for (int i = contents.getChildren().size() - 1; i >= 0; i--) {
            ((Component) contents.getChildren().get(i)).detach();
        }

        ArrayList<Action> actions = getActions();

        Ul ul = new Ul();
        ul.setStyle("margin:0px");
        ul.setParent(contents);

        for (int i = 0; i < actions.size(); i++) {
            Li li = new Li();
            li.setParent(ul);
            A a = new A(actions.get(i).label);
            a.addEventListener("onClick", actions.get(i).eventListener);
            a.setParent(li);
        }
    }

    ArrayList<Action> getActions() {
        ArrayList<Action> actions = new ArrayList<Action>();
        ArrayList<MapLayer> layers = getVisibleLayers();

        MapLayer speciesLayer = null;
        MapLayer polygonLayer = null;
        MapLayer gridLayer = null;

        MapLayer firstLayer = null;
        for (int i = 0; i < layers.size() /*&& actions.size() < 5*/; i++) {
            if (layers.get(i).getData("query") != null
                    && layers.get(i).getSubType() != LayerUtilities.SCATTERPLOT) {
                if (speciesLayer == null) {
                    speciesLayer = layers.get(i);
                }
                if (firstLayer == null) {
                    firstLayer = layers.get(i);
                }
            } else if (layers.get(i).isPolygonLayer()
                    && layers.get(i).getSubType() != LayerUtilities.ALOC) {
                if (polygonLayer == null) {
                    polygonLayer = layers.get(i);
                }
                if (firstLayer == null) {
                    firstLayer = layers.get(i);
                }
            } else if (layers.get(i).isGridLayer()
                    && layers.get(i).getSubType() != LayerUtilities.MAXENT
                    && layers.get(i).getSubType() != LayerUtilities.GDM
                    && layers.get(i).getSubType() != LayerUtilities.ALOC) {
                //TODO: grid test
                if (gridLayer == null) {
                    gridLayer = layers.get(i);
                }
                if (firstLayer == null) {
                    firstLayer = layers.get(i);
                }
            } else if (layers.get(i).getMapLayerMetadata() != null
                    && layers.get(i).getSubType() != LayerUtilities.SCATTERPLOT) {
                if (firstLayer == null) {
                    firstLayer = layers.get(i);
                }
            }
        }

        //actions rules
        if (polygonLayer != null) {
            actions.add(new Action("View area report for \"" + polygonLayer.getDisplayName() + "\"",
                    new AreaReportEvent(getMapComposer(), polygonLayer.getName())));
        }
        if (firstLayer != null) {
            actions.add(new Action("View metadata for \"" + firstLayer.getDisplayName() + "\"",
                    new MetadataEvent(getMapComposer(), firstLayer.getName())));
        }
        if (polygonLayer != null) {
            actions.add(new Action("Download species list for \"" + polygonLayer.getDisplayName() + "\"",
                    new SpeciesListEvent(getMapComposer(), polygonLayer.getName())));
        }
        if (speciesLayer != null) {
            actions.add(new Action("Download all records for \"" + speciesLayer.getDisplayName() + "\""
                    + ((polygonLayer != null) ? " in \"" + polygonLayer.getDisplayName() + "\"" : ""),
                    new SamplingEvent(getMapComposer(), speciesLayer.getName(),
                    (polygonLayer != null) ? polygonLayer.getName() : null, null)));
        } else if (polygonLayer != null) {
            actions.add(new Action("Download all records "
                    + ((polygonLayer != null) ? " for " + "\"" + polygonLayer.getDisplayName() + "\"" : ""),
                    new SamplingEvent(getMapComposer(), null, polygonLayer.getName(), null)));
        }
        if (polygonLayer != null) {
            actions.add(new Action("Generate classification for \"" + polygonLayer.getDisplayName() + "\"",
                    new ClassificationEvent(getMapComposer(), polygonLayer.getName(), null)));
        }
        if (speciesLayer != null) {
            actions.add(new Action("Produce scatterplot for \"" + speciesLayer.getDisplayName() + "\""
                    + ((polygonLayer != null) ? " in \"" + polygonLayer.getDisplayName() + "\"" : ""),
                    new ScatterplotEvent(getMapComposer(), speciesLayer.getName(),
                    (polygonLayer != null) ? polygonLayer.getName() : null, null)));
        }
        if (speciesLayer != null) {
            actions.add(new Action("Generate prediction for \"" + speciesLayer.getDisplayName() + "\""
                    + ((polygonLayer != null) ? " in \"" + polygonLayer.getDisplayName() + "\"" : ""),
                    new PredictionEvent(getMapComposer(), speciesLayer.getName(),
                    (polygonLayer != null) ? polygonLayer.getName() : null, null)));
        }
        if (speciesLayer != null) {
            actions.add(new Action("Produce points to grid for \"" + speciesLayer.getDisplayName() + "\""
                    + ((polygonLayer != null) ? " in \"" + polygonLayer.getDisplayName() + "\"" : ""),
                    new SitesBySpeciesEvent(getMapComposer(), speciesLayer.getName(),
                    (polygonLayer != null) ? polygonLayer.getName() : null, null)));
        }
        if (speciesLayer != null) {
            actions.add(new Action("Produce GDM using species \"" + speciesLayer.getDisplayName() + "\""
                    + ((polygonLayer != null) ? " in \"" + polygonLayer.getDisplayName() + "\"" : ""),
                    new GDMEvent(getMapComposer(), speciesLayer.getName(),
                    (polygonLayer != null) ? polygonLayer.getName() : null, null)));
        }
        if (polygonLayer != null) {
            actions.add(new Action("Export area \"" + polygonLayer.getDisplayName() + "\"",
                    new ExportAreaEvent(getMapComposer(), polygonLayer.getName())));
        }
        if (speciesLayer != null && speciesLayer == getMapComposer().getActiveLayersSelection(false)) {
            actions.add(new Action("Display facet of \"" + speciesLayer.getDisplayName() + "\"",
                    new OpenFacetsEvent(getMapComposer(), speciesLayer.getName())));
        }

//        if (gridLayer != null) {
//            //actions.add(new Action("Browse environmental point values for " + gridLayer.getDisplayName(), new GridLayerHoverEvent(getMapComposer(), gridLayer.getName())));
//            actions.add(new Action("Browse environmental point values", new GridLayerHoverEvent(getMapComposer(), gridLayer.getName())));
//        }
//        if (polygonLayer != null) {
//            //actions.add(new Action("Browse environmental point values for " + gridLayer.getDisplayName(), new GridLayerHoverEvent(getMapComposer(), gridLayer.getName())));
//            actions.add(new Action("Browse contextual point values", new GridLayerHoverEvent(getMapComposer(), polygonLayer.getName())));
//        }

        //default actions
        if (actions.size() == 0) {
            actions.add(new Action("Map species occurrences", new AddToMap(getMapComposer(), "species")));
            actions.add(new Action("Map area", new AddToMap(getMapComposer(), "area")));
            actions.add(new Action("Map layer", new AddToMap(getMapComposer(), "layer")));
            actions.add(new Action("Map facet", new AddToMap(getMapComposer(), "facet")));
        }

        return actions;
    }

    ArrayList<MapLayer> getVisibleLayers() {
        ArrayList<MapLayer> list = new ArrayList<MapLayer>();
        List<MapLayer> allLayers = getPortalSession().getActiveLayers();
        for (int i = 0; i < allLayers.size(); i++) {
            if (allLayers.get(i).isDisplayed()) {
                list.add(allLayers.get(i));
            }
        }

        return list;
    }
}

class Action {

    public String label;
    public EventListener eventListener;

    //defaults
    public Action(String label, EventListener eventListener) {
        this.label = label;
        this.eventListener = eventListener;
    }
}

class SamplingEvent implements EventListener {

    String speciesLayerName;
    String polygonLayerName;
    String environmentalLayerName;
    MapComposer mc;
    int steps_to_skip;
    boolean[] geospatialKosher;

    public SamplingEvent(MapComposer mc, String speciesLayerName, String polygonLayerName, String environmentalLayerName) {
        this.mc = mc;
        this.speciesLayerName = speciesLayerName;
        this.polygonLayerName = polygonLayerName;
        this.environmentalLayerName = environmentalLayerName;
        this.steps_to_skip = 0;
        this.geospatialKosher = null;
    }

    public SamplingEvent(MapComposer mc, String speciesLayerName, String polygonLayerName, String environmentalLayerName, int steps_to_skip, boolean[] geospatialKosher) {
        this.mc = mc;
        this.speciesLayerName = speciesLayerName;
        this.polygonLayerName = polygonLayerName;
        this.environmentalLayerName = environmentalLayerName;
        this.steps_to_skip = steps_to_skip;
        this.geospatialKosher = geospatialKosher;
    }

    @Override
    public void onEvent(Event event) throws Exception {
        Hashtable<String, Object> params = new Hashtable<String, Object>();
        if (speciesLayerName != null) {
            params.put("speciesLayerName", speciesLayerName);
        } else {
            params.put("speciesLayerName", "none");
        }
        if (polygonLayerName != null) {
            params.put("polygonLayerName", polygonLayerName);
        } else {
            params.put("polygonLayerName", "none");
        }
        if (environmentalLayerName != null) {
            params.put("environmentalLayerName", environmentalLayerName);
        } else {
            params.put("environmentalLayerName", "none");
        }
        AddToolSamplingComposer window = (AddToolSamplingComposer) mc.openModal("WEB-INF/zul/AddToolSampling.zul", params, "addtoolwindow");

        window.setGeospatialKosherCheckboxes(geospatialKosher);

        int skip = steps_to_skip;
        while (skip > 0) {
            window.onClick$btnOk(event);
            skip--;
        }
        //window.onClick$btnOk(event);
        //window.onClick$btnOk(event);
    }
}

class PredictionEvent implements EventListener {

    String speciesLayerName;
    String polygonLayerName;
    String environmentalLayerName;
    MapComposer mc;

    public PredictionEvent(MapComposer mc, String speciesLayerName, String polygonLayerName, String environmentalLayerName) {
        this.mc = mc;
        this.speciesLayerName = speciesLayerName;
        this.polygonLayerName = polygonLayerName;
        this.environmentalLayerName = environmentalLayerName;
    }

    @Override
    public void onEvent(Event event) throws Exception {
        Hashtable<String, Object> params = new Hashtable<String, Object>();
        if (speciesLayerName != null) {
            params.put("speciesLayerName", speciesLayerName);
        } else {
            params.put("speciesLayerName", "none");
        }
        if (polygonLayerName != null) {
            params.put("polygonLayerName", polygonLayerName);
        } else {
            params.put("polygonLayerName", "none");
        }
        if (environmentalLayerName != null) {
            params.put("environmentalLayerName", environmentalLayerName);
        } else {
            params.put("environmentalLayerName", "none");
        }
        AddToolMaxentComposer window = (AddToolMaxentComposer) mc.openModal("WEB-INF/zul/AddToolMaxent.zul", params, "addtoolwindow");
        //window.onClick$btnOk(event);
        //window.onClick$btnOk(event);
    }
}

class GDMEvent implements EventListener {

    String speciesLayerName;
    String polygonLayerName;
    String environmentalLayerName;
    MapComposer mc;

    public GDMEvent(MapComposer mc, String speciesLayerName, String polygonLayerName, String environmentalLayerName) {
        this.mc = mc;
        this.speciesLayerName = speciesLayerName;
        this.polygonLayerName = polygonLayerName;
        this.environmentalLayerName = environmentalLayerName;
    }

    @Override
    public void onEvent(Event event) throws Exception {
        Hashtable<String, Object> params = new Hashtable<String, Object>();
        if (speciesLayerName != null) {
            params.put("speciesLayerName", speciesLayerName);
        } else {
            params.put("speciesLayerName", "none");
        }
        if (polygonLayerName != null) {
            params.put("polygonLayerName", polygonLayerName);
        } else {
            params.put("polygonLayerName", "none");
        }
        if (environmentalLayerName != null) {
            params.put("environmentalLayerName", environmentalLayerName);
        } else {
            params.put("environmentalLayerName", "none");
        }
        AddToolGDMComposer window = (AddToolGDMComposer) mc.openModal("WEB-INF/zul/AddToolGDM.zul", params, "addtoolwindow");
        //window.onClick$btnOk(event);
        //window.onClick$btnOk(event);
    }
}

class ClassificationEvent implements EventListener {

    String polygonLayerName;
    String environmentalLayerName;
    MapComposer mc;

    public ClassificationEvent(MapComposer mc, String polygonLayerName, String environmentalLayerName) {
        this.mc = mc;
        this.polygonLayerName = polygonLayerName;
        this.environmentalLayerName = environmentalLayerName;
    }

    @Override
    public void onEvent(Event event) throws Exception {
        Hashtable<String, Object> params = new Hashtable<String, Object>();
        if (polygonLayerName != null) {
            params.put("polygonLayerName", polygonLayerName);
        } else {
            params.put("polygonLayerName", "none");
        }
        if (environmentalLayerName != null) {
            params.put("environmentalLayerName", environmentalLayerName);
        } else {
            params.put("environmentalLayerName", "none");
        }
        AddToolALOCComposer window = (AddToolALOCComposer) mc.openModal("WEB-INF/zul/AddToolALOC.zul", params, "addtoolwindow");
        //window.onClick$btnOk(event);
    }
}

class ScatterplotEvent implements EventListener {

    String speciesLayerName;
    String polygonLayerName;
    String environmentalLayerName;
    MapComposer mc;

    public ScatterplotEvent(MapComposer mc, String speciesLayerName, String polygonLayerName, String environmentalLayerName) {
        this.mc = mc;
        this.speciesLayerName = speciesLayerName;
        this.polygonLayerName = polygonLayerName;
        this.environmentalLayerName = environmentalLayerName;
    }

    @Override
    public void onEvent(Event event) throws Exception {
        Hashtable<String, Object> params = new Hashtable<String, Object>();
        if (speciesLayerName != null) {
            params.put("speciesLayerName", speciesLayerName);
        } else {
            params.put("speciesLayerName", "none");
        }
        if (polygonLayerName != null) {
            params.put("polygonLayerName", polygonLayerName);
        } else {
            params.put("polygonLayerName", "none");
        }
        if (environmentalLayerName != null) {
            params.put("environmentalLayerName", environmentalLayerName);
        } else {
            params.put("environmentalLayerName", "default");
        }
        AddToolComposer window = (AddToolComposer) mc.openModal("WEB-INF/zul/AddToolScatterplot.zul", params, "addtoolwindow");
        //window.onClick$btnOk(event);
        //window.onClick$btnOk(event);
    }
}

class SitesBySpeciesEvent implements EventListener {

    String speciesLayerName;
    String polygonLayerName;
    String environmentalLayerName;
    MapComposer mc;

    public SitesBySpeciesEvent(MapComposer mc, String speciesLayerName, String polygonLayerName, String environmentalLayerName) {
        this.mc = mc;
        this.speciesLayerName = speciesLayerName;
        this.polygonLayerName = polygonLayerName;
        this.environmentalLayerName = environmentalLayerName;
    }

    @Override
    public void onEvent(Event event) throws Exception {
        Hashtable<String, Object> params = new Hashtable<String, Object>();
        if (speciesLayerName != null) {
            params.put("speciesLayerName", speciesLayerName);
        } else {
            params.put("speciesLayerName", "none");
        }
        if (polygonLayerName != null) {
            params.put("polygonLayerName", polygonLayerName);
        } else {
            params.put("polygonLayerName", "none");
        }
        if (environmentalLayerName != null) {
            params.put("environmentalLayerName", environmentalLayerName);
        } else {
            params.put("environmentalLayerName", "default");
        }
        AddToolComposer window = (AddToolComposer) mc.openModal("WEB-INF/zul/AddToolSitesBySpecies.zul", params, "addtoolwindow");
        //window.onClick$btnOk(event);
        //window.onClick$btnOk(event);
    }
}

class SpeciesListEvent implements EventListener {

    String polygonLayerName;
    MapComposer mc;
    int steps_to_skip;
    boolean[] geospatialKosher;

    public SpeciesListEvent(MapComposer mc, String polygonLayerName) {
        this.mc = mc;
        this.polygonLayerName = polygonLayerName;
        this.steps_to_skip = 0;
        this.geospatialKosher = null;
    }

    public SpeciesListEvent(MapComposer mc, String polygonLayerName, int steps_to_skip, boolean[] geospatialKosher) {
        this.mc = mc;
        this.polygonLayerName = polygonLayerName;
        this.steps_to_skip = steps_to_skip;
        this.geospatialKosher = geospatialKosher;
    }

    @Override
    public void onEvent(Event event) throws Exception {
        Hashtable<String, Object> params = new Hashtable<String, Object>();
        if (polygonLayerName != null) {
            params.put("polygonLayerName", polygonLayerName);
        } else {
            params.put("polygonLayerName", "none");
        }
        AddToolSpeciesListComposer window = (AddToolSpeciesListComposer) mc.openModal("WEB-INF/zul/AddToolSpeciesList.zul", params, "addtoolwindow");
        window.setGeospatialKosherCheckboxes(geospatialKosher);

        int skip = steps_to_skip;
        while (skip > 0) {
            window.onClick$btnOk(event);
            skip--;
        }
    }
}

class MetadataEvent implements EventListener {

    String layerName;
    MapComposer mc;

    public MetadataEvent(MapComposer mc, String layerName) {
        this.mc = mc;
        this.layerName = layerName;
    }

    @Override
    public void onEvent(Event event) throws Exception {
        MapLayer mapLayer = mc.getMapLayer(layerName);
        if (mapLayer != null) {
            if (mapLayer.getData("query") != null) {
                //TODO: update for scatterplot layers
                Query q = (Query) mapLayer.getData("query");
                Events.echoEvent("openHTML", mc, q.getMetadataHtml());
            } else if (mapLayer.getMapLayerMetadata() != null
                    && mapLayer.getMapLayerMetadata().getMoreInfo() != null
                    && mapLayer.getMapLayerMetadata().getMoreInfo().startsWith("http://")) {
                String infourl = mapLayer.getMapLayerMetadata().getMoreInfo().replace("__", ".");
                if (mapLayer.getSubType() == LayerUtilities.SCATTERPLOT) {
                    ScatterplotData data = (ScatterplotData) mapLayer.getData("scatterplotData");
                    infourl += "?dparam=X-Layer:" + data.getLayer1Name();
                    infourl += "&dparam=Y-Layer:" + data.getLayer2Name();
                }
                // send the user to the BIE page for the species
                Events.echoEvent("openUrl", mc, infourl);

            } else if (mapLayer.getMapLayerMetadata() != null
                    && mapLayer.getMapLayerMetadata().getMoreInfo() != null
                    && mapLayer.getMapLayerMetadata().getMoreInfo().length() > 0) {
                //logger.debug("performing a MapComposer.showMessage for following content " + activeLayer.getMapLayerMetadata().getMoreInfo());
                Events.echoEvent("openHTML", mc, mapLayer.getMapLayerMetadata().getMoreInfo());
            } else {
                //logger.debug("no metadata is available for current layer");
                mc.showMessage("Metadata currently unavailable");
            }
        }
    }
}

class OpenFacetsEvent implements EventListener {

    String layerName;
    MapComposer mc;

    public OpenFacetsEvent(MapComposer mc, String layerName) {
        this.mc = mc;
        this.layerName = layerName;
    }

    @Override
    public void onEvent(Event event) throws Exception {
        MapLayer mapLayer = mc.getMapLayer(layerName);
        if (mapLayer != null) {
            if(mapLayer.getColourMode().equals("grid")) {
                mapLayer.setColourMode("-1");
                mc.updateLayerControls();
            }
            Events.echoEvent("openFacets", mc, null);
        }
    }
}

class AreaReportEvent implements EventListener {

    String polygonLayerName;
    MapComposer mc;

    public AreaReportEvent(MapComposer mc, String polygonLayerName) {
        this.mc = mc;
        this.polygonLayerName = polygonLayerName;
    }

    @Override
    public void onEvent(Event event) throws Exception {
        Hashtable<String, Object> params = new Hashtable<String, Object>();
        if (polygonLayerName != null) {
            params.put("polygonLayerName", polygonLayerName);
        } else {
            params.put("polygonLayerName", "none");
        }
        AddToolAreaReportComposer window = (AddToolAreaReportComposer) mc.openModal("WEB-INF/zul/AddToolAreaReport.zul", params, "addtoolwindow");

//        MapLayer ml = mc.getMapLayer(polygonLayerName);
//        Window w = (Window) mc.getPage().getFellowIfAny("popup_results");
//        if (w != null) {
//            w.detach();
//        }
//        double[] bbox = null;
//        if (ml != null && ml.getMapLayerMetadata() != null
//                && ml.getMapLayerMetadata().getBbox() != null
//                && ml.getMapLayerMetadata().getBbox().size() == 4) {
//            bbox = new double[4];
//            bbox[0] = ml.getMapLayerMetadata().getBbox().get(0);
//            bbox[1] = ml.getMapLayerMetadata().getBbox().get(1);
//            bbox[2] = ml.getMapLayerMetadata().getBbox().get(2);
//            bbox[3] = ml.getMapLayerMetadata().getBbox().get(3);
//        }
//        FilteringResultsWCController.open(ml.getWKT(), ml.getName(), ml.getDisplayName(), (String) ml.getData("area"), bbox);
    }
}

class ExportAreaEvent implements EventListener {

    String polygonLayerName;
    MapComposer mc;

    public ExportAreaEvent(MapComposer mc, String polygonLayerName) {
        this.mc = mc;
        this.polygonLayerName = polygonLayerName;
    }

    @Override
    public void onEvent(Event event) throws Exception {
        Hashtable<String, Object> params = new Hashtable<String, Object>();
        if (polygonLayerName != null) {
            params.put("polygonLayerName", polygonLayerName);
        } else {
            params.put("polygonLayerName", "none");
        }
        ExportLayerComposer window = (ExportLayerComposer) mc.openModal("WEB-INF/zul/ExportLayer.zul", params, "addtoolwindow");

//        MapLayer ml = mc.getMapLayer(polygonLayerName);
//        Window w = (Window) mc.getPage().getFellowIfAny("popup_results");
//        if (w != null) {
//            w.detach();
//        }
//        double[] bbox = null;
//        if (ml != null && ml.getMapLayerMetadata() != null
//                && ml.getMapLayerMetadata().getBbox() != null
//                && ml.getMapLayerMetadata().getBbox().size() == 4) {
//            bbox = new double[4];
//            bbox[0] = ml.getMapLayerMetadata().getBbox().get(0);
//            bbox[1] = ml.getMapLayerMetadata().getBbox().get(1);
//            bbox[2] = ml.getMapLayerMetadata().getBbox().get(2);
//            bbox[3] = ml.getMapLayerMetadata().getBbox().get(3);
//        }
//        FilteringResultsWCController.open(ml.getWKT(), ml.getName(), ml.getDisplayName(), (String) ml.getData("area"), bbox);
    }
}

class GridLayerHoverEvent implements EventListener {

    MapComposer mc;
    String layerName;

    public GridLayerHoverEvent(MapComposer mc, String layerName) {
        this.mc = mc;
        this.layerName = layerName;
    }

    @Override
    public void onEvent(Event event) throws Exception {
        Clients.evalJavaScript("mapFrame.toggleActiveHover();");
    }
}

class AddToMap implements EventListener {

    String type;
    MapComposer mc;

    public AddToMap(MapComposer mc, String type) {
        this.mc = mc;
        this.type = type;
    }

    @Override
    public void onEvent(Event event) throws Exception {
        if (type.equals("species")) {
            mc.onClick$btnAddSpecies(null);
        } else if (type.equals("area")) {
            mc.onClick$btnAddArea(null);
        } else if (type.equals("layer")) {
            mc.onClick$btnAddLayer(null);
        } else if (type.equals("facet")) {
            mc.onClick$btnAddFacet(null);
        }
    }
}
