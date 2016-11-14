package au.org.ala.spatial.composer.sandbox;

import au.org.ala.legend.Facet;
import au.org.ala.spatial.StringConstants;
import au.org.ala.spatial.util.BiocacheQuery;
import au.org.ala.spatial.util.CommonData;
import au.org.ala.spatial.util.Util;
import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.util.LayerUtilitiesImpl;
import org.apache.log4j.Logger;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.ForwardEvent;
import org.zkoss.zul.Div;
import org.zkoss.zul.Iframe;
import org.zkoss.zul.Label;

import java.util.ArrayList;
import java.util.List;

public class SandboxEmbeddedController extends UtilityComposer {

    private static final Logger LOGGER = Logger.getLogger(SandboxEmbeddedController.class);

    private String dataResourceUid;
    private Div sandboxReady;
    private Iframe sandboxFrame;
    private BiocacheQuery query;
    private boolean addtoMap;
    private EventListener callback;
    private String tag;

    @Override
    public void afterCompose() {
        super.afterCompose();

        tag = "SP" + System.currentTimeMillis();
        ((Label) getFellow("uploadTag")).setValue(tag);

        sandboxFrame.setSrc(CommonData.getSettings().getProperty("sandbox.url") + "?tag=" + tag);
    }

    public void setAddToMap(boolean addtoMap) {
        this.addtoMap = addtoMap;
    }

    public void gotDrUid(Event event) {
        dataResourceUid = ((String) event.getData());

        sandboxReady.setVisible(true);

        sandboxFrame.setVisible(false);

        List<Facet> facetList = new ArrayList<Facet>();
        facetList.add(new Facet("data_resource_uid", dataResourceUid, true));
        query = new BiocacheQuery(null, null, null, facetList, true, null,
                CommonData.getSettings().getProperty("sandbox.biocache.url"),
                CommonData.getSettings().getProperty("sandbox.biocache.ws.url"),
                true);

        if (addtoMap) {
            getMapComposer().mapSpecies(query, query.getSolrName(), StringConstants.SPECIES, query.getOccurrenceCount(),
                    LayerUtilitiesImpl.SPECIES, null,
                    0, MapComposer.DEFAULT_POINT_SIZE, MapComposer.DEFAULT_POINT_OPACITY, Util.nextColour(), false);
        }

        //call reset window on caller to perform refresh'
        if (callback != null) {
            try {
                callback.onEvent(new ForwardEvent("", null, null, new String[]{dataResourceUid, "normal"}));
            } catch (Exception e) {
                LOGGER.error("failed to cancel species points upload", e);
            }
        }

        onClick$btnOk(null);
    }

    public void onClick$btnOk(Event event) {
        this.detach();
    }

    public void onClick$btnCancel(Event event) {
        if (callback != null) {
            try {
                callback.onEvent(new ForwardEvent("", null, null, new String[]{"", "cancel"}));
            } catch (Exception e) {
                LOGGER.error("failed to cancel species points upload", e);
            }
        }
        this.detach();
    }
}
