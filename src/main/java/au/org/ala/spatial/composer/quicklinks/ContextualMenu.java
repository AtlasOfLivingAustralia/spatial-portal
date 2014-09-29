package au.org.ala.spatial.composer.quicklinks;

import au.org.ala.spatial.StringConstants;
import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.util.LayerUtilitiesImpl;
import org.zkoss.zhtml.Li;
import org.zkoss.zhtml.Ul;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.A;
import org.zkoss.zul.Vbox;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ajay
 */
public class ContextualMenu extends UtilityComposer {

    private Vbox contents;

    @Override
    public void afterCompose() {
        super.afterCompose();

        getMapComposer().setContextualMenuRefreshListener(new EventListener() {

            @Override
            public void onEvent(Event event) throws Exception {
                refresh();
            }
        });
    }

    public void refresh() {
        for (int i = contents.getChildren().size() - 1; i >= 0; i--) {
            contents.getChildren().get(i).detach();
        }

        List<Action> actions = getActions();

        Ul ul = new Ul();
        ul.setStyle("margin:0px");
        ul.setParent(contents);

        for (int i = 0; i < actions.size(); i++) {
            Li li = new Li();
            li.setParent(ul);
            A a = new A(actions.get(i).label);
            a.addEventListener(StringConstants.ONCLICK, actions.get(i).eventListener);
            a.setParent(li);
        }
    }

    List<Action> getActions() {
        List<Action> actions = new ArrayList<Action>();
        List<MapLayer> layers = getVisibleLayers();

        MapLayer speciesLayer = null;
        MapLayer polygonLayer = null;
        MapLayer gridLayer = null;

        MapLayer firstLayer = null;
        for (int i = 0; i < layers.size() /*&& actions.size() < 5*/; i++) {
            if (layers.get(i).getSpeciesQuery() != null
                    && layers.get(i).getSubType() != LayerUtilitiesImpl.SCATTERPLOT) {
                if (speciesLayer == null) {
                    speciesLayer = layers.get(i);
                }
                if (firstLayer == null) {
                    firstLayer = layers.get(i);
                }
            } else if (layers.get(i).isPolygonLayer()
                    && layers.get(i).getSubType() != LayerUtilitiesImpl.ALOC) {
                if (polygonLayer == null) {
                    polygonLayer = layers.get(i);
                }
                if (firstLayer == null) {
                    firstLayer = layers.get(i);
                }
            } else if (layers.get(i).isGridLayer()
                    && layers.get(i).getSubType() != LayerUtilitiesImpl.MAXENT
                    && layers.get(i).getSubType() != LayerUtilitiesImpl.GDM
                    && layers.get(i).getSubType() != LayerUtilitiesImpl.ALOC) {
                //TODO: grid test
                if (gridLayer == null) {
                    gridLayer = layers.get(i);
                }
                if (firstLayer == null) {
                    firstLayer = layers.get(i);
                }
            } else if (layers.get(i).getSubType() != LayerUtilitiesImpl.SCATTERPLOT
                    && firstLayer == null) {
                firstLayer = layers.get(i);
            }
        }

        //actions rules
        if (polygonLayer != null) {
            actions.add(new Action("View area report for \"" + polygonLayer.getDisplayName() + "\"",
                    new AreaReportEvent(polygonLayer.getName())));
        }
        if (firstLayer != null) {
            actions.add(new Action("View metadata for \"" + firstLayer.getDisplayName() + "\"",
                    new MetadataEvent(firstLayer.getName())));
        }
        if (polygonLayer != null) {
            actions.add(new Action("Download species list for \"" + polygonLayer.getDisplayName() + "\"",
                    new SpeciesListEvent(polygonLayer.getName())));
        }
        if (speciesLayer != null) {
            actions.add(new Action("Download all records for \"" + speciesLayer.getDisplayName() + "\""
                    + formatLayerName(polygonLayer),
                    new SamplingEvent(speciesLayer.getName(),
                            (polygonLayer != null) ? polygonLayer.getName() : null, null)));
        } else if (polygonLayer != null) {
            actions.add(new Action("Download all records "
                    + (" for " + "\"" + polygonLayer.getDisplayName() + "\""),
                    new SamplingEvent(null, polygonLayer.getName(), null)));
        }
        if (polygonLayer != null) {
            actions.add(new Action("Generate classification for \"" + polygonLayer.getDisplayName() + "\"",
                    new ClassificationEvent(polygonLayer.getName(), null)));
        }
        if (speciesLayer != null) {
            actions.add(new Action("Produce scatterplot for \"" + speciesLayer.getDisplayName() + "\""
                    + formatLayerName(polygonLayer),
                    new ScatterplotEvent(speciesLayer.getName(),
                            (polygonLayer != null) ? polygonLayer.getName() : null, null)));
        }
        if (speciesLayer != null) {
            actions.add(new Action("Generate prediction for \"" + speciesLayer.getDisplayName() + "\""
                    + formatLayerName(polygonLayer),
                    new PredictionEvent(speciesLayer.getName(),
                            (polygonLayer != null) ? polygonLayer.getName() : null, null)));
        }
        if (speciesLayer != null) {
            actions.add(new Action("Produce points to grid for \"" + speciesLayer.getDisplayName() + "\""
                    + formatLayerName(polygonLayer),
                    new SitesBySpeciesEvent(speciesLayer.getName(),
                            (polygonLayer != null) ? polygonLayer.getName() : null, null)));
        }
        if (speciesLayer != null) {
            actions.add(new Action("Produce GDM using species \"" + speciesLayer.getDisplayName() + "\""
                    + formatLayerName(polygonLayer),
                    new GDMEvent(speciesLayer.getName(),
                            (polygonLayer != null) ? polygonLayer.getName() : null, null)));
        }
        if (polygonLayer != null) {
            actions.add(new Action("Export area \"" + polygonLayer.getDisplayName() + "\"",
                    new ExportAreaEvent(polygonLayer.getName())));
        }
        if (speciesLayer != null && speciesLayer == layers.get(0)) {
            actions.add(new Action("Display facet of \"" + speciesLayer.getDisplayName() + "\"",
                    new OpenFacetsEvent(speciesLayer.getName())));
        }

        //default actions
        if (actions.isEmpty()) {
            actions.add(new Action("Map species occurrences", new AddToMapEvent(StringConstants.SPECIES)));
            actions.add(new Action("Map area", new AddToMapEvent(StringConstants.AREA)));
            actions.add(new Action("Map layer", new AddToMapEvent(StringConstants.LAYER)));
            actions.add(new Action("Map facet", new AddToMapEvent(StringConstants.FACET)));
        }

        return actions;
    }

    private String formatLayerName(MapLayer polygonLayer) {
        return (polygonLayer != null) ? " in \"" + polygonLayer.getDisplayName() + "\"" : "";
    }

    List<MapLayer> getVisibleLayers() {
        List<MapLayer> list = new ArrayList<MapLayer>();
        List<MapLayer> allLayers = getPortalSession().getActiveLayers();
        for (int i = 0; i < allLayers.size(); i++) {
            if (allLayers.get(i).isDisplayed()) {
                list.add(allLayers.get(i));
            }
        }

        return list;
    }
}
