package org.ala.spatial.analysis.web;

import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.settings.SettingsSupplementary;
import au.org.emii.portal.util.LayerUtilities;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
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
        for(int i=contents.getChildren().size()-1;i>=0;i--) {
            ((Component)contents.getChildren().get(i)).detach();
        }
        
        ArrayList<Action> actions = getActions();

        Ul ul = new Ul();
        ul.setStyle("margin:0px");
        ul.setParent(contents);

        for(int i=0;i<actions.size() && i < 5;i++) {
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
        MapLayer firstLayer = null;
        for(int i=0;i<layers.size() && actions.size() < 5;i++) {
            if(layers.get(i).getMapLayerMetadata() != null
                    && layers.get(i).getMapLayerMetadata().getSpeciesLsid() != null
                    && layers.get(i).getSubType() != LayerUtilities.SCATTERPLOT) {
                if(speciesLayer == null) {
                    speciesLayer = layers.get(i);
                }
                if (firstLayer == null) {
                    firstLayer = layers.get(i);
                }
            } else if(layers.get(i).isPolygonLayer()
                    && layers.get(i).getSubType() != LayerUtilities.ALOC) {
                if(polygonLayer == null) {
                    polygonLayer = layers.get(i);
                }
                if (firstLayer == null) {
                    firstLayer = layers.get(i);
                }
            } else if (layers.get(i).isGridLayer()
                    && layers.get(i).getSubType() != LayerUtilities.MAXENT) {
                //TODO: grid test
                if(gridLayer == null) {
                    gridLayer = layers.get(i);
                }
                if (firstLayer == null) {
                    firstLayer = layers.get(i);
                }
            }
        }
        
        //actions rules
        if(polygonLayer != null) {
            actions.add(new Action("View area report for " + polygonLayer.getDisplayName(),
                        new AreaReportEvent(getMapComposer(), polygonLayer.getName())));
        }
        if(firstLayer != null) {
            actions.add(new Action("View metadata " + firstLayer.getDisplayName(),
                            new MetadataEvent(getMapComposer(), firstLayer.getName())));
        }
        if(speciesLayer != null) {
            actions.add(new Action("Download all records for " + speciesLayer.getDisplayName()
                    + ((polygonLayer != null)?" for " + polygonLayer.getDisplayName():""),
                            new SamplingEvent(getMapComposer(), speciesLayer.getMapLayerMetadata().getSpeciesLsid(),
                                (polygonLayer != null)?polygonLayer.getName():null, null)));
        } else if(polygonLayer != null) {
            actions.add(new Action("Download all records " +
                    ((polygonLayer != null)?" for " + polygonLayer.getDisplayName():""),
                            new SamplingEvent(getMapComposer(), null, polygonLayer.getName(), null)));
        }
        if (polygonLayer != null) {
                actions.add(new Action("Download species list for " + polygonLayer.getDisplayName(),
                        new SpeciesListEvent(getMapComposer(), polygonLayer.getName())));
        }
        if (polygonLayer != null) {
            actions.add(new Action("Produce classification for " + polygonLayer.getDisplayName(),
                        new ClassificationEvent(getMapComposer(), polygonLayer.getName(), null)));
        }
        if (speciesLayer != null) {
            actions.add(new Action("Produce scatterplot for " + speciesLayer.getDisplayName()
                    + ((polygonLayer != null)?" for " + polygonLayer.getDisplayName():""),
                        new ScatterplotEvent(getMapComposer(), speciesLayer.getMapLayerMetadata().getSpeciesLsid(),
                            (polygonLayer != null)?polygonLayer.getName():null, null)));
        }
        if (speciesLayer != null) {
            actions.add(new Action("Produce prediction for " + speciesLayer.getDisplayName()
                    + ((polygonLayer != null)?" for " + polygonLayer.getDisplayName():""),
                        new PredictionEvent(getMapComposer(), speciesLayer.getMapLayerMetadata().getSpeciesLsid(),
                            (polygonLayer != null)?polygonLayer.getName():null, null)));
        }

//        if(gridLayer != null){
//            actions.add(new Action("Browse environmental point values for " + gridLayer.getDisplayName(), new GridLayerHoverEvent(getMapComposer(), gridLayer.getName())));
//        }

      
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
    String environmentalLayerName;
    MapComposer mc;

    public SamplingEvent(MapComposer mc, String lsid, String polygonLayerName, String environmentalLayerName) {
        this.mc = mc;
        this.lsid = lsid;
        this.polygonLayerName = polygonLayerName;
        this.environmentalLayerName = environmentalLayerName;
    }

    @Override
    public void onEvent(Event event) throws Exception {
        Hashtable<String, Object> params = new Hashtable<String, Object>();
        if(lsid != null) {
            params.put("lsid", lsid);
        } else {
            params.put("lsid", "none");
        }
        if(polygonLayerName != null) {
            params.put("polygonLayerName", polygonLayerName);
        } else {
            params.put("polygonLayerName", "none");
        }
        if(environmentalLayerName != null) {
            params.put("environmentalLayerName", environmentalLayerName);
        } else {
            params.put("environmentalLayerName", "none");
        }
        AddToolSamplingComposer window = (AddToolSamplingComposer) mc.openModal("WEB-INF/zul/AddToolSampling.zul", params);
    }
}

class PredictionEvent implements EventListener {
    String lsid;
    String polygonLayerName;
    String environmentalLayerName;
    MapComposer mc;

    public PredictionEvent(MapComposer mc, String lsid, String polygonLayerName, String environmentalLayerName) {
        this.mc = mc;
        this.lsid = lsid;
        this.polygonLayerName = polygonLayerName;
        this.environmentalLayerName = environmentalLayerName;
    }

    @Override
    public void onEvent(Event event) throws Exception {
        Hashtable<String, Object> params = new Hashtable<String, Object>();
        if(lsid != null) {
            params.put("lsid", lsid);
        } else {
            params.put("lsid", "none");
        }
        if(polygonLayerName != null) {
            params.put("polygonLayerName", polygonLayerName);
        } else {
            params.put("polygonLayerName", "none");
        }
        if(environmentalLayerName != null) {
            params.put("environmentalLayerName", environmentalLayerName);
        } else {
            params.put("environmentalLayerName", "none");
        }
        AddToolMaxentComposer window = (AddToolMaxentComposer) mc.openModal("WEB-INF/zul/AddToolMaxent.zul", params);
    }
}

class ClassificationEvent implements EventListener {
    String polygonLayerName;
    String environmentalLayerName;
    MapComposer mc;

    public ClassificationEvent(MapComposer mc,String polygonLayerName, String environmentalLayerName) {
        this.mc = mc;
        this.polygonLayerName = polygonLayerName;
        this.environmentalLayerName = environmentalLayerName;
    }

    @Override
    public void onEvent(Event event) throws Exception {
        Hashtable<String, Object> params = new Hashtable<String, Object>();
        if(polygonLayerName != null) {
            params.put("polygonLayerName", polygonLayerName);
        } else {
            params.put("polygonLayerName", "none");
        }
        if(environmentalLayerName != null) {
            params.put("environmentalLayerName", environmentalLayerName);
        } else {
            params.put("environmentalLayerName", "none");
        }
        AddToolALOCComposer window = (AddToolALOCComposer) mc.openModal("WEB-INF/zul/AddToolALOC.zul", params);
    }
}

class ScatterplotEvent implements EventListener {
    String lsid;
    String polygonLayerName;
    String environmentalLayerName;
    MapComposer mc;

    public ScatterplotEvent(MapComposer mc, String lsid, String polygonLayerName, String environmentalLayerName) {
        this.mc = mc;
        this.lsid = lsid;
        this.polygonLayerName = polygonLayerName;
        this.environmentalLayerName = environmentalLayerName;
    }

    @Override
    public void onEvent(Event event) throws Exception {
        Hashtable<String, Object> params = new Hashtable<String, Object>();
        if(lsid != null) {
            params.put("lsid", lsid);
        } else {
            params.put("lsid", "none");
        }
        if(polygonLayerName != null) {
            params.put("polygonLayerName", polygonLayerName);
        } else {
            params.put("polygonLayerName", "none");
        }
        if(environmentalLayerName != null) {
            params.put("environmentalLayerName", environmentalLayerName);
        } else {
            params.put("environmentalLayerName", "default");
        }
        mc.openModal("WEB-INF/zul/AddToolScatterplot.zul", params);
    }
}

class SpeciesListEvent implements EventListener {
    String polygonLayerName;
    MapComposer mc;

    public SpeciesListEvent (MapComposer mc, String polygonLayerName) {
        this.mc = mc;
        this.polygonLayerName = polygonLayerName;
    }

    @Override
    public void onEvent(Event event) throws Exception {
        Hashtable<String, Object> params = new Hashtable<String, Object>();
        if(polygonLayerName != null) {
            params.put("polygonLayerName", polygonLayerName);
        } else {
            params.put("polygonLayerName", "none");
        }
        AddToolSpeciesListComposer window = (AddToolSpeciesListComposer) mc.openModal("WEB-INF/zul/AddToolSpeciesList.zul", params);

//        SpeciesListResults window = (SpeciesListResults) Executions.createComponents("WEB-INF/zul/AnalysisSpeciesListResults.zul", mc, null);
//        MapLayer ml = mc.getMapLayer(polygonLayerName);
//        if(ml != null) {
//            window.wkt = ml.getWKT();
//        } else {
//            window.wkt = mc.getViewArea();
//        }
//        try {
//            window.doModal();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }
}

class MetadataEvent implements EventListener {
    String layerName;
    MapComposer mc;

    public MetadataEvent (MapComposer mc, String layerName) {
        this.mc = mc;
        this.layerName = layerName;
    }

    @Override
    public void onEvent(Event event) throws Exception {
        MapLayer mapLayer = mc.getMapLayer(layerName);
        if(mapLayer != null) {
            if (mapLayer.getMapLayerMetadata() != null
                    && mapLayer.getMapLayerMetadata().getMoreInfo() != null
                    && mapLayer.getMapLayerMetadata().getMoreInfo().startsWith("http://")) {
                // send the user to the BIE page for the species
                //logger.debug("opening the following url " + activeLayer.getMapLayerMetadata().getMoreInfo().replace("__", "."));
                Events.echoEvent("openUrl", mc, mapLayer.getMapLayerMetadata().getMoreInfo().replace("__", "."));

            } else if (mapLayer.getMapLayerMetadata() != null
                    && mapLayer.getMapLayerMetadata().getMoreInfo() != null
                    && mapLayer.getMapLayerMetadata().getMoreInfo().length() > 0) {
                //logger.debug("performing a MapComposer.showMessage for following content " + activeLayer.getMapLayerMetadata().getMoreInfo());
                mc.showMessage(mapLayer.getMapLayerMetadata().getMoreInfo());
            } else {
                //logger.debug("no metadata is available for current layer");
                mc.showMessage("Metadata currently unavailable");
            }
        }
    }
}

class AreaReportEvent implements EventListener {
    String polygonLayerName;
    MapComposer mc;

    public AreaReportEvent (MapComposer mc, String polygonLayerName) {
        this.mc = mc;
        this.polygonLayerName = polygonLayerName;
    }

    @Override
    public void onEvent(Event event) throws Exception {
        Hashtable<String, Object> params = new Hashtable<String, Object>();
        if(polygonLayerName != null) {
            params.put("polygonLayerName", polygonLayerName);
        } else {
            params.put("polygonLayerName", "none");
        }
        AddToolAreaReportComposer window = (AddToolAreaReportComposer) mc.openModal("WEB-INF/zul/AddToolAreaReport.zul", params);

//        MapLayer ml = mc.getMapLayer(polygonLayerName);
//        if(ml != null) {
//            FilteringResultsWCController.open(ml.getWKT());
//        } else {
//            FilteringResultsWCController.open(null);
//        }
    }
}

class GridLayerHoverEvent implements EventListener {
    MapComposer mc;
    String layerName;

    public GridLayerHoverEvent (MapComposer mc, String layerName) {
        this.mc = mc;
        this.layerName = layerName;
    }

    @Override
    public void onEvent(Event event) throws Exception {
        Clients.evalJavaScript("mapFrame.toggleActiveHover();");
    }
}