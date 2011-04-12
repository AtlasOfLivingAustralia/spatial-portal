package org.ala.spatial.analysis.web;

import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.settings.SettingsSupplementary;
import au.org.emii.portal.util.LayerUtilities;
import java.util.ArrayList;
import java.util.List;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.A;
import org.zkoss.zul.Radio;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.Window;

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
        for(int i=contents.getChildren().size()-1;i>=0;i--) {
            ((Component)contents.getChildren().get(i)).detach();
        }
        
        ArrayList<Action> actions = getActions();

        for(int i=0;i<actions.size() && i < 5;i++) {
            A a = new A(actions.get(i).label);
            a.addEventListener("onClick", actions.get(i).eventListener);
            a.setParent(contents);
        }
    }

    ArrayList<Action> getActions() {
        ArrayList<Action> actions = new ArrayList<Action>();
        ArrayList<MapLayer> layers = getVisibleLayers();

        MapLayer speciesLayer = null;
        MapLayer polygonLayer = null;
        MapLayer gridLayer = null;

//        for(int i=0;i<layers.size() && actions.size() < 5;i++) {
//            if(layers.get(i).getMapLayerMetadata() != null
//                    && layers.get(i).getMapLayerMetadata().getSpeciesLsid() != null) {
//                if(speciesLayer != null) {
//                    speciesLayer = layers.get(i);
//                }
//                String lsid = layers.get(i).getMapLayerMetadata().getSpeciesLsid();
//                actions.add(new Action("Download all records for " + layers.get(i).getName(), new SamplingEvent(getMapComposer(), lsid, null)));
//                if(polygonLayer != null) {
//                    actions.add(new Action("Download all records for " + layers.get(i).getName() + " for " + polygonLayer.getName(),
//                            new SamplingEvent(getMapComposer(), lsid, polygonLayer.getName())));
//                }
//                actions.add(new Action("Produce environmental scatterplot for " + layers.get(i).getName(),
//                        new ScatterplotEvent(getMapComposer(), lsid)));
//                actions.add(new Action("Produce Maxent model for " + layers.get(i).getName(),
//                        new PredictionEvent(getMapComposer(),  lsid, null)));
//                if(polygonLayer != null) {
//                    actions.add(new Action("Download Maxent model for " + layers.get(i).getName() + " for " + polygonLayer.getName(),
//                            new PredictionEvent(getMapComposer(), lsid, polygonLayer.getName())));
//                }
//            } else if(layers.get(i).isPolygonLayer()) {
//                if(polygonLayer != null) {
//                    polygonLayer = layers.get(i);
//                }
//                if(speciesLayer != null) {
//                    actions.add(new Action("Download all records for " + layers.get(i).getName() + " for " + layers.get(i).getName(),
//                            new SamplingEvent(getMapComposer(), speciesLayer.getMapLayerMetadata().getSpeciesLsid(), layers.get(i).getName())));
//                }
//                actions.add(new Action("Download species list for " + layers.get(i).getName(),
//                        new SpeciesListEvent(getMapComposer(), layers.get(i).getName())));
//                actions.add(new Action("Download records for all species for " + layers.get(i).getName(),
//                        new SamplingEvent(getMapComposer(), null, layers.get(i).getName())));
//                actions.add(new Action("View area report for " + layers.get(i).getName(),
//                        new AreaReportEvent(getMapComposer(), layers.get(i).getName())));
//            } else if(layers.get(i).getType() == LayerUtilities.MAXENT) {
//                actions.add(new Action("View analysis report for " + layers.get(i).getName(),
//                        new MetadataEvent(getMapComposer(), layers.get(i).getMapLayerMetadata().getMoreInfo())));
//            } else if(layers.get(i).getType() == LayerUtilities.ALOC) {
//                actions.add(new Action("View analysis report for " + layers.get(i).getName(),
//                        new MetadataEvent(getMapComposer(), layers.get(i).getMapLayerMetadata().getMoreInfo())));
//            } else {
//                //WMS layer, 'could' be a grid
//                actions.add(new Action("Produce classification model for " + layers.get(i).getName(),
//                        new ClassificationEvent(getMapComposer(),
//                        layers.get(i).getMapLayerMetadata().getMoreInfo(),
//                        polygonLayer==null?null:polygonLayer.getName())));
//            }
//        }
        
        for(int i=0;i<layers.size() && actions.size() < 5;i++) {
            if(layers.get(i).getMapLayerMetadata() != null
                    && layers.get(i).getMapLayerMetadata().getSpeciesLsid() != null) {
                if(speciesLayer == null) {
                    speciesLayer = layers.get(i);
                }
            } else if(layers.get(i).isPolygonLayer()) {
                if(polygonLayer == null) {
                    polygonLayer = layers.get(i);
                }
            } else {
                //TODO: grid test
                if(gridLayer == null) {
                    gridLayer = layers.get(i);
                }
            }
        }
        
        //actions rules
        if(speciesLayer != null && polygonLayer != null) {
            actions.add(new Action("Download all records for " + speciesLayer.getDisplayName() + " for " + polygonLayer.getDisplayName(),
                            new SamplingEvent(getMapComposer(), speciesLayer.getMapLayerMetadata().getSpeciesLsid(), polygonLayer.getName())));
            actions.add(new Action("Produce prediction for " + speciesLayer.getDisplayName() + " for " + polygonLayer.getDisplayName(),
                            new PredictionEvent(getMapComposer(), speciesLayer.getMapLayerMetadata().getSpeciesLsid(), polygonLayer.getName())));
            actions.add(new Action("Produce scatterplot for " + speciesLayer.getDisplayName() + " for " + polygonLayer.getDisplayName(),
                            new ScatterplotEvent(getMapComposer(), speciesLayer.getMapLayerMetadata().getSpeciesLsid(), polygonLayer.getName())));
        }
        if (speciesLayer != null) {
            actions.add(new Action("View metadata " + speciesLayer.getDisplayName(),
                            new MetadataEvent(getMapComposer(), speciesLayer.getMapLayerMetadata().getSpeciesLsid())));
            actions.add(new Action("Download all records for " + speciesLayer.getDisplayName(),
                            new SamplingEvent(getMapComposer(), speciesLayer.getMapLayerMetadata().getSpeciesLsid(),null)));
            actions.add(new Action("Produce prediction for " + speciesLayer.getDisplayName(),
                            new PredictionEvent(getMapComposer(), speciesLayer.getMapLayerMetadata().getSpeciesLsid(), null)));
            actions.add(new Action("Produce scatterplot for " + speciesLayer.getDisplayName(),
                            new ScatterplotEvent(getMapComposer(), speciesLayer.getMapLayerMetadata().getSpeciesLsid(), null)));
        }
        if(polygonLayer != null) {
            actions.add(new Action("View metadata " + speciesLayer.getDisplayName(),
                            new MetadataEvent(getMapComposer(), polygonLayer.getName())));
            actions.add(new Action("Download species list for " + polygonLayer.getDisplayName(),
                            new SpeciesListEvent(getMapComposer(), polygonLayer.getName())));
            actions.add(new Action("Download all records for " + polygonLayer.getDisplayName(),
                            new SamplingEvent(getMapComposer(), null, polygonLayer.getName())));
            actions.add(new Action("Produce classification for " + polygonLayer.getDisplayName(),
                            new ClassificationEvent(getMapComposer(), polygonLayer.getName())));
        }

        if(speciesLayer != null && gridLayer != null) {
            actions.add(new Action("View metadata " + gridLayer.getDisplayName(),
                            new MetadataEvent(getMapComposer(), gridLayer.getName())));
            actions.add(new Action("Produce scatterplot for " + speciesLayer.getDisplayName() + " for " + gridLayer.getDisplayName(),
                            new ScatterplotEvent(getMapComposer(), speciesLayer.getMapLayerMetadata().getSpeciesLsid(), gridLayer.getName())));
            actions.add(new Action("Produce prediction for " + speciesLayer.getDisplayName() + " for " + gridLayer.getDisplayName(),
                            new PredictionEvent(getMapComposer(), speciesLayer.getMapLayerMetadata().getSpeciesLsid(), gridLayer.getName())));
        } else if(gridLayer != null) {
            actions.add(new Action("View metadata " + gridLayer.getDisplayName(),
                            new MetadataEvent(getMapComposer(), gridLayer.getName())));
            actions.add(new Action("Produce prediction for " + gridLayer.getDisplayName(),
                            new PredictionEvent(getMapComposer(), null, gridLayer.getName())));
        }

        return actions;
    }

    ArrayList<MapLayer> getVisibleLayers() {
        ArrayList<MapLayer> list = new ArrayList<MapLayer>();
        List<MapLayer> allLayers = getPortalSession().getActiveLayers();
        for(int i=0;i<allLayers.size();i++) {
            if(allLayers.get(i).isDisplayed()) {
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
    String lsid;
    String polygonLayerName;
    MapComposer mc;

    public SamplingEvent(MapComposer mc, String lsid, String polygonLayerName) {
        this.mc = mc;
        this.lsid = lsid;
        this.polygonLayerName = polygonLayerName;
    }

    @Override
    public void onEvent(Event event) throws Exception {

    }
}

class PredictionEvent implements EventListener {
    String lsid;
    String polygonLayerName;
    MapComposer mc;

    public PredictionEvent(MapComposer mc, String lsid, String polygonLayerName) {
        this.mc = mc;
        this.lsid = lsid;
        this.polygonLayerName = polygonLayerName;
    }

    @Override
    public void onEvent(Event event) throws Exception {

    }
}

class ClassificationEvent implements EventListener {
    String polygonLayerName;
    MapComposer mc;

    public ClassificationEvent(MapComposer mc,String polygonLayerName) {
        this.mc = mc;
        this.polygonLayerName = polygonLayerName;
    }

    @Override
    public void onEvent(Event event) throws Exception {

    }
}

class ScatterplotEvent implements EventListener {
    String lsid;
    String polygonLayerName;
    MapComposer mc;

    public ScatterplotEvent(MapComposer mc, String lsid, String polygonLayerName) {
        this.mc = mc;
        this.lsid = lsid;
        this.polygonLayerName = polygonLayerName;
    }

    @Override
    public void onEvent(Event event) throws Exception {

    }
}

class SpeciesListEvent implements EventListener {
    String lsid;
    MapComposer mc;

    public SpeciesListEvent (MapComposer mc, String lsid) {
        this.mc = mc;
        this.lsid = lsid;
    }

    @Override
    public void onEvent(Event event) throws Exception {

    }
}

class MetadataEvent implements EventListener {
    String lsid;
    MapComposer mc;

    public MetadataEvent (MapComposer mc, String lsid) {
        this.mc = mc;
        this.lsid = lsid;
    }

    @Override
    public void onEvent(Event event) throws Exception {

    }
}

class AreaReportEvent implements EventListener {
    String lsid;
    MapComposer mc;

    public AreaReportEvent (MapComposer mc, String lsid) {
        this.mc = mc;
        this.lsid = lsid;
    }

    @Override
    public void onEvent(Event event) throws Exception {

    }
}