package au.org.emii.portal.databinding;

import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.util.LayerUtilities;
import au.org.emii.portal.event.ActiveLayerDNDEventListener;
import au.org.emii.portal.event.ActiveLayersInfoEventListener;
import au.org.emii.portal.event.ActiveLayersLegendEventListener;
import au.org.emii.portal.event.ActiveLayersRemoveEventListener;
import au.org.emii.portal.event.ActiveLayersZoomExtentEventListener;
import au.org.emii.portal.event.LegendTooltipOpenEventListener;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.event.VisibilityToggleEventListener;
import au.org.emii.portal.lang.LanguagePack;
import org.springframework.beans.factory.annotation.Required;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.ListitemRenderer;
import org.zkoss.zul.Popup;

public class ActiveLayerRenderer implements ListitemRenderer {

    private LanguagePack languagePack = null;
    private LayerUtilities layerUtilities = null;
    private VisibilityToggleEventListener visibilityToggleEventListener = null;

    @Override
    public void render(Listitem item, Object data) throws Exception {
        final MapLayer layer = (MapLayer) data;
        Listcell listcell = new Listcell();
        Checkbox checkbox = null;
        /*
         * In the past it was assumed that we just set true here - this is not
         * the case because this method is called to re-render the list after
         * the user has been fiddling with the checkboxes in some cases (dnd
         * events)
         */
        checkbox = new Checkbox();
        checkbox.setChecked(layer.isDisplayed());
        checkbox.setParent(listcell);
        checkbox.setTooltiptext("Hide");
        if (layer.getType() != LayerUtilities.MAP) {
            checkbox.addEventListener("onCheck", visibilityToggleEventListener);
        } else {
            checkbox.setChecked(true);
            checkbox.addEventListener("onCheck", new EventListener() {

                @Override
                public void onEvent(Event event) throws Exception {
                    MapComposer mapComposer = (MapComposer) event.getPage().getFellow("mapPortalPage");
                    mapComposer.toggleLayers(event);
                }
            });
        }
        if(!layer.isRemoveable()){
            checkbox.setStyle("float:left; visibility:hidden; ");
            checkbox.setDisabled(true);
        }

        Label label = new Label(layerUtilities.chompLayerName(layer.getDisplayName()));
        //do after legend
        //label.setParent(listcell);
        listcell.setParent(item);
        listcell.setStyle("padding:5px;");

        // dnd list reordering support
        item.addEventListener("onDrop", new ActiveLayerDNDEventListener());
        item.setDraggable("true");
        item.setDroppable("true");

        // bind to the ActiveLayer instance (we readback later)
        item.setValue(layer);

        // simple description for tooltip
        label.setTooltiptext(layerUtilities.getTooltip(layer.getDisplayName(), layer.getDescription()));

        //label.addEventListener("onClick", new ActiveLayersInfoEventListener());
//        label.setStyle("float:left;");

        if(layer.isRemoveable()){
            checkbox.setStyle("float:left;");
        }

        /*
         * show the legend graphic when the user hovers over the palette icon
         */
        if(layer.isRemoveable()){
            Image remove = new Image(languagePack.getLang("layer_remove_icon"));
            remove.addEventListener("onClick", new ActiveLayersRemoveEventListener());
            remove.setParent(listcell);
            remove.setStyle("float:right;");
            remove.setTooltiptext("remove layer");
        }

        if(layer.isHasMetadata()){
            Image info = new Image(languagePack.getLang("layer_info_icon"));
            info.setParent(listcell);
            info.setStyle("float:right;");
            info.setTooltiptext("metadata");
            info.addEventListener("onClick", new ActiveLayersInfoEventListener());
        }

        if(layer.isHasExtent()){
            Image zoomextent = new Image(languagePack.getLang("layer_zoomextent_icon"));
            zoomextent.setParent(listcell);
            zoomextent.setStyle("float:right");
            zoomextent.setTooltiptext("zoom to extent");
            zoomextent.addEventListener("onClick", new ActiveLayersZoomExtentEventListener());
        }

        //Set the legend graphic based on the layer type
        Image legend = new Image();
        if (layer.isGridLayer()) {
            legend = new Image(languagePack.getLang("icon_grid"));
        } else if (layer.isSpeciesLayer()) {
            legend = new Image(languagePack.getLang("icon_species"));
        } else if (layer.isPolygonLayer()) {
            legend = new Image(languagePack.getLang("icon_polygon"));
        } else if (layer.isContextualLayer()) {
            legend = new Image(languagePack.getLang("icon_contextual"));
        } else {
            //just a plain layer
            legend = new Image(languagePack.getLang("layer_legend_icon"));
        }

        //todo: support analysis layers (languagePack.getLang("icon_analysis"))
        legend.setStyle("float:left;");
        legend.setParent(listcell);
        legend.setTooltiptext("View/edit the legend");
        legend.addEventListener("onClick", new ActiveLayersLegendEventListener());
        label.setParent(listcell);
    }

    public LanguagePack getLanguagePack() {
        return languagePack;
    }

    @Required
    public void setLanguagePack(LanguagePack languagePack) {
        this.languagePack = languagePack;
    }

    public LayerUtilities getLayerUtilities() {
        return layerUtilities;
    }

    @Required
    public void setLayerUtilities(LayerUtilities layerUtilities) {
        this.layerUtilities = layerUtilities;
    }

    public VisibilityToggleEventListener getVisibilityToggleEventListener() {
        return visibilityToggleEventListener;
    }

    @Required
    public void setVisibilityToggleEventListener(VisibilityToggleEventListener visibilityToggleEventListener) {
        this.visibilityToggleEventListener = visibilityToggleEventListener;
    }
}
