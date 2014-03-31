package au.org.ala.spatial.composer.quicklinks;

import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.settings.SettingsSupplementary;
import au.org.emii.portal.util.LayerUtilities;
import org.apache.log4j.Logger;
import org.zkoss.zhtml.Li;
import org.zkoss.zhtml.Ul;
import org.zkoss.zul.A;
import org.zkoss.zul.Vbox;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ajay
 */
public class ContextualMenu extends UtilityComposer {

    private static Logger logger = Logger.getLogger(ContextualMenu.class);

    SettingsSupplementary settingsSupplementary;
    Vbox contents;

    public void refresh() {
        for (int i = contents.getChildren().size() - 1; i >= 0; i--) {
            contents.getChildren().get(i).detach();
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
            if (layers.get(i).getSpeciesQuery() != null
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
            } else if (layers.get(i).getSubType() != LayerUtilities.SCATTERPLOT) {
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
        if (speciesLayer != null
                && (speciesLayer == getMapComposer().getActiveLayersSelection(false)
                || (getMapComposer().getActiveLayersSelection(false) == null && layers != null && speciesLayer == layers.get(0)))) {
            actions.add(new Action("Display facet of \"" + speciesLayer.getDisplayName() + "\"",
                    new OpenFacetsEvent(getMapComposer(), speciesLayer.getName())));
        }

        //default actions
        if (actions.size() == 0) {
            actions.add(new Action("Map species occurrences", new AddToMapEvent(getMapComposer(), "species")));
            actions.add(new Action("Map area", new AddToMapEvent(getMapComposer(), "area")));
            actions.add(new Action("Map layer", new AddToMapEvent(getMapComposer(), "layer")));
            actions.add(new Action("Map facet", new AddToMapEvent(getMapComposer(), "facet")));
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
