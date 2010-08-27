package au.org.emii.portal.event;

import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.util.LayerUtilitiesImpl;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Listitem;

public class ActiveLayersInfoEventListener extends PortalEvent implements EventListener {

    public void onEvent(Event event) throws Exception {
        MapComposer mapComposer = getMapComposer(event);
        if (mapComposer != null && mapComposer.safeToPerformMapAction()) {
            // get reference to the label/image the user clicked on
            Component target = event.getTarget();
            Listitem listItem = (Listitem) target.getParent().getParent();
            MapLayer activeLayer = (MapLayer) listItem.getValue();

            if (activeLayer != null) {
                //mapComposer.deactiveLayer(activeLayer, true, false);

                //System.out.println("activeLayer.metdata: " + activeLayer.getMapLayerMetadata().getMoreInfo());
                if (activeLayer.getMapLayerMetadata() != null && activeLayer.getMapLayerMetadata().getMoreInfo().startsWith("http://")) {
                    // send the user to the BIE page for the species
                    Clients.evalJavaScript("window.open('"
                            + activeLayer.getMapLayerMetadata().getMoreInfo()
                            + "', 'metadataWindow');");
                } else if (activeLayer.getMapLayerMetadata() != null && activeLayer.getMapLayerMetadata().getMoreInfo().length() > 0) {
                    //mapComposer.showMessage("Metadata",activeLayer.getMapLayerMetadata().getMoreInfo(),"");
                    mapComposer.showMessage(activeLayer.getMapLayerMetadata().getMoreInfo());
                } else {
                    mapComposer.showMessage("Metadata currently unavailable");
                }
            } else {
                //logger.debug("nothing selected in active layers list will do nothing");
            }
        } else {
            logger.debug("MapController reports unsafe to perform action");
        }
    }
}
