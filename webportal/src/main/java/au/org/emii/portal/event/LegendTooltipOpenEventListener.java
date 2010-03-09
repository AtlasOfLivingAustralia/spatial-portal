package au.org.emii.portal.event;

import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.composer.LegendComposer;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.OpenEvent;

public class LegendTooltipOpenEventListener extends LegendEventListener {

    @Override
    public void onEvent(Event event) throws Exception {
        // do nothing if the popup is already open
        if (event instanceof OpenEvent) {
            OpenEvent openEvent = (OpenEvent) event;
            if (openEvent.isOpen()) {
                logger.debug("OpenEvent requests popup open");
                /* we want to both reposition any displaying legend window
                 * AND show the popup
                 */
                LegendComposer window = legendDisplaying(event);
                createComponents(event);
                if (window != null) {
                    legendAlreadyDisplayed(window, event);
                }


            } else {
                /* tooltip has been closed - show the sticky icon again
                 * because the tooltip will be reused until the list is
                 * in active layers gets rerendered.  This way, when (if)
                 * it gets shown again, we can see it again
                 */
                showStickyIcon(event, true);
            }
        }
        else {
            logger.info("event is not instance of OpenEvent - not processing: " + event.getClass().getName());
        }
    }

    public LegendTooltipOpenEventListener(MapLayer mapLayer) {
        super(mapLayer);
    }

    private void showStickyIcon(Event event, boolean show) {
        event.getTarget().getFirstChild().getFellow("stickyIcon").setVisible(show);
    }

    @Override
    protected void legendAlreadyDisplayed(LegendComposer window, Event event) {
        /* careful - the windows contained in window and event are different
         * objects ;-)
         */
        showStickyIcon(event, false);
        window.reposition();
    }

    @Override
    protected void createComponents(Event event) {
        LegendComposer window = (LegendComposer) event.getTarget().getFirstChild();
        window.setMapLayer(mapLayer);
        /* when the window is opened, it does an update() call for us so we don't
         * need to do it here as well
         */
    }
}
