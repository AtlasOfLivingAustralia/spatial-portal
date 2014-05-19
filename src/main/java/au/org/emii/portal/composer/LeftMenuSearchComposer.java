package au.org.emii.portal.composer;

import au.org.emii.portal.value.BoundingBox;
import org.apache.log4j.Logger;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;

import java.util.HashMap;

public class LeftMenuSearchComposer extends UtilityComposer {

    private static Logger logger = Logger.getLogger(LeftMenuSearchComposer.class);

    private static final long serialVersionUID = 2540820748110129339L;

    private double north;
    private double south;
    private double east;
    private double west;
    private int zoom;

    //map of event listeners for viewport changes (west Doublebox onchange)
    HashMap<String, EventListener> viewportChangeEvents = new HashMap<String, EventListener>();

    @Override
    public void afterCompose() {
        super.afterCompose();
    }

    public BoundingBox getViewportBoundingBox() {
        BoundingBox bbox = new BoundingBox();
        bbox.setMaxLatitude((float) north);
        bbox.setMinLatitude((float) south);
        bbox.setMinLongitude((float) west);
        bbox.setMaxLongitude((float) east);
        return bbox;
    }

    public int getZoom() {
        return zoom;
    }

    ;

    public void setExtents(Event event) {
        String[] extents = ((String) event.getData()).split(",");
        west = Double.parseDouble(extents[0]);
        north = Double.parseDouble(extents[1]);
        east = Double.parseDouble(extents[2]);
        south = Double.parseDouble(extents[3]);

        zoom = Integer.parseInt(extents[4]);

        Events.echoEvent("triggerViewportChange", this, null);
    }

    public void triggerViewportChange(Event e) throws Exception {
        //update bounding box for this session
        BoundingBox bb = new BoundingBox();
        bb.setMinLatitude((float) south);
        bb.setMaxLatitude((float) north);
        bb.setMinLongitude((float) west);
        bb.setMaxLongitude((float) east);
        getMapComposer().getPortalSession().setDefaultBoundingbox(bb);

        for (EventListener el : viewportChangeEvents.values()) {
            try {
                el.onEvent(null);
            } catch (Exception ex) {
                logger.error("error running viewport change listener", ex);
            }
        }
    }

    public void addViewportEventListener(String eventName, EventListener eventListener) {
        viewportChangeEvents.put(eventName, eventListener);
    }

    public void removeViewportEventListener(String eventName) {
        viewportChangeEvents.remove(eventName);
    }
}
