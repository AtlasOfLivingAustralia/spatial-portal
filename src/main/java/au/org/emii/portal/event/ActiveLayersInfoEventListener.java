package au.org.emii.portal.event;

import au.org.ala.spatial.StringConstants;
import au.org.ala.spatial.dto.ScatterplotDataDTO;
import au.org.ala.spatial.util.Query;
import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.util.LayerUtilitiesImpl;
import org.apache.log4j.Logger;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Listitem;

public class ActiveLayersInfoEventListener extends PortalEvent implements EventListener {

    private static final Logger LOGGER = Logger.getLogger(PortalEvent.class);

    @Override
    public void onEvent(Event event) throws Exception {
        MapComposer mapComposer = getMapComposer(event);
        if (mapComposer != null && mapComposer.safeToPerformMapAction()) {
            // get reference to the label/image the user clicked on
            Component target = event.getTarget();
            Listitem listItem = (Listitem) target.getParent().getParent();
            MapLayer activeLayer = listItem.getValue();

            if (activeLayer != null) {
                if (activeLayer.getSpeciesQuery() != null) {

                    Query q = activeLayer.getSpeciesQuery();
                    Events.echoEvent(StringConstants.OPEN_HTML, mapComposer, q.getMetadataHtml());
                } else if (activeLayer.getMapLayerMetadata().getMoreInfo() != null
                        && activeLayer.getMapLayerMetadata().getMoreInfo().startsWith("http://")) {
                    String infourl = activeLayer.getMapLayerMetadata().getMoreInfo().replace("__", ".");
                    if (activeLayer.getSubType() == LayerUtilitiesImpl.SCATTERPLOT) {
                        ScatterplotDataDTO data = activeLayer.getScatterplotDataDTO();
                        infourl += "?dparam=X-Layer:" + data.getLayer1Name();
                        infourl += "&dparam=Y-Layer:" + data.getLayer2Name();
                    }
                    // send the user to the BIE page for the species
                    LOGGER.debug("opening the following url " + infourl);
                    Events.echoEvent(StringConstants.OPEN_URL, mapComposer, activeLayer.getMapLayerMetadata().getMoreInfo().replace("__", "."));

                } else if (activeLayer.getMapLayerMetadata().getMoreInfo() != null
                        && activeLayer.getMapLayerMetadata().getMoreInfo().length() > 0) {
                    LOGGER.debug("performing a MapComposer.showMessage for following content " + activeLayer.getMapLayerMetadata().getMoreInfo());

                    String metadata = activeLayer.getMapLayerMetadata().getMoreInfo();

                    Events.echoEvent(StringConstants.OPEN_HTML, mapComposer, metadata);
                } else if (activeLayer.getType() == LayerUtilitiesImpl.MAP) {
                    String metaurl = "https://www.google.com/intl/en_au/help/terms_maps.html";
                    if ("outline".equalsIgnoreCase(mapComposer.getPortalSession().getBaseLayer())) {
                        metaurl = "openstreetmap_metadata.html";
                    } else if ("minimal".equalsIgnoreCase(mapComposer.getPortalSession().getBaseLayer())) {
                        metaurl = "http://www.naturalearthdata.com/about/terms-of-use";
                    }

                    LOGGER.debug("opening base map metadata for: " + mapComposer.getPortalSession().getBaseLayer() + ", url:" + metaurl);
                    Events.echoEvent(StringConstants.OPEN_URL, mapComposer, metaurl);
                } else {
                    LOGGER.debug("no metadata is available for current layer");
                    mapComposer.showMessage("Metadata currently unavailable");
                }
            }
        } else {
            LOGGER.debug("MapController reports unsafe to perform action");
        }
    }
}
