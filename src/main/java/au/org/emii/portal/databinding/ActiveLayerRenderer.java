package au.org.emii.portal.databinding;

import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.event.*;
import au.org.emii.portal.lang.LanguagePack;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.util.LayerUtilities;
import org.springframework.beans.factory.annotation.Required;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.*;

public class ActiveLayerRenderer implements ListitemRenderer {

    private LanguagePack languagePack = null;
    private LayerUtilities layerUtilities = null;
    private VisibilityToggleEventListener visibilityToggleEventListener = null;

    @Override
    public void render(Listitem item, Object data, int item_idx) throws Exception {
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
        if (!layer.isRemoveable()) {
            checkbox.setStyle("float:left; visibility:hidden; ");
            checkbox.setDisabled(true);
        }

        Label label = new Label(layer.getDisplayName());
        //do after legend
        //label.setParent(listcell);
        listcell.setParent(item);

        // dnd list reordering support
        item.addEventListener("onDrop", new ActiveLayerDNDEventListener());
        item.setDraggable("true");
        item.setDroppable("true");

        // bind to the ActiveLayer instance (we readback later)
        item.setValue(layer);

        // simple description for tooltip
        label.setTooltiptext(layer.getDescription());

        //label.addEventListener("onClick", new ActiveLayersInfoEventListener());
//        label.setStyle("float:left;");

        if (layer.isRemoveable()) {
            checkbox.setStyle("float:left;");
        }

        /*
         * show the legend graphic when the user hovers over the palette icon
         */
        if (layer.isRemoveable()) {
            Html remove = new Html(languagePack.getLang("layer_remove_icon_html");
            remove.addEventListener("onClick", new ActiveLayersRemoveEventListener());
            remove.setParent(listcell);
            remove.setStyle("float:right;");
            remove.setTooltiptext("remove layer");
        }

        Html info = new Html(languagePack.getLang("layer_info_icon_html"));
        info.setParent(listcell);
        info.setStyle("float:right;");
        info.setTooltiptext("metadata");
        info.addEventListener("onClick", new ActiveLayersInfoEventListener());

        if (layer.getType() != LayerUtilities.MAP) {
            Html zoomextent = new Html(languagePack.getLang("layer_zoomextent_icon_html");
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

        legend.setStyle("float:left;");
        legend.setParent(listcell);
        legend.setTooltiptext("View/edit the legend");
        legend.addEventListener("onClick", new ActiveLayersLegendEventListener());
        label.setParent(listcell);

        // adding buttons to basemap in layer list
        if (layer.getType() == LayerUtilities.MAP) {
            //add select all or unselect all button or delete all buttons
            //NOTE: Objects created here are referenced by relative location in the listcell.
            //      Changes made here must also be made in MapComposer.adjustActiveLayersList

            Div div = new Div();
            div.setClass("btn-group");
            div.setParent(listcell);
            div.setStyle("float:right;margin-right:30px");

            Button b = new Button("Delete all");
            b.setClass("btn-mini");
            b.setParent(div);
            b.setVisible(false);
            b.addEventListener("onClick", new EventListener() {

                @Override
                public void onEvent(Event event) throws Exception {
                    Messagebox.show("All layers will be deleted, are you sure?", "Warning", Messagebox.OK | Messagebox.CANCEL, Messagebox.EXCLAMATION
                            , new EventListener() {
                        public void onEvent(Event evt) {
                            switch (((Integer) evt.getData()).intValue()) {
                                case Messagebox.OK:
                                    ((MapComposer) evt.getPage().getFellow("mapPortalPage")).onClick$removeAllLayers();
                                    break;
                            }
                        }
                    });
                }
            });

            b = new Button("Show all");
            b.setClass("btn-mini");
            b.setParent(div);
            b.setVisible(false);
            b.addEventListener("onClick", new EventListener() {

                @Override
                public void onEvent(Event event) throws Exception {
                    ((MapComposer) event.getPage().getFellow("mapPortalPage")).setLayersVisible(true);

                }
            });
            b = new Button("Hide all");
            b.setClass("btn-mini");
            b.setParent(div);
            b.setVisible(false);
            b.addEventListener("onClick", new EventListener() {

                @Override
                public void onEvent(Event event) throws Exception {
                    ((MapComposer) event.getPage().getFellow("mapPortalPage")).setLayersVisible(false);

                }
            });

            ((MapComposer) listcell.getPage().getFellow("mapPortalPage")).adjustActiveLayersList();
        }
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
