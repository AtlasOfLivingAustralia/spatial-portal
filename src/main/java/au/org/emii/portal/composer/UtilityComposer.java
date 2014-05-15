package au.org.emii.portal.composer;

import au.org.emii.portal.util.Validate;
import org.apache.log4j.Logger;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;

/**
 * Handy utilities to compose windows with - Currently, if you compose against
 * this class, you get: + autowire variables + autoforward variables + nudge
 * windows dragged offscreen back into the browser
 *
 * @author geoff
 */
public class UtilityComposer extends GenericAutowireAutoforwardComposer {

    private static Logger logger = Logger.getLogger(UtilityComposer.class);

    private static final long serialVersionUID = 1L;

    /**
     * Stop user dragging windows outsize viewport - if they try to do this,
     * nudge it back again!
     */
    public void onNudgeToView() {
        /*
         * We use getTop() to find where on the screen the item is - 
         * getTop doesn't return an integer or even a float, instead
         * it returns Strings like this:
         * "174px"
         * "77.5px"
         * "-94px"
         * 
         * So rather than parse a number, we just check to see if the
         * string starts with the '-' character.  If it does, we do
         * the nudge 
         */
        logger.debug("nudging window");
        String top = getTop();
        if ((!Validate.empty(top)) && (top.charAt(0) == '-')) {
            logger.debug("moving window from " + top + " to 0px");
            setTop("0px");
        }
    }

    /**
     * autowire a default action for id=close
     */
    public void onClick$close() {
        detach();
    }

    /**
     * autowire a default action for id=hide
     */
    public void onClick$hide() {
        setVisible(false);
    }

    @Override
    public void doAfterCompose(Component component) throws Exception {
    }

    @Override
    public void afterCompose() {
        super.afterCompose();
        logger.debug("registered UtilityComposer event listeners");
        addEventListener("onMove", new EventListener() {
            public void onEvent(Event event) throws Exception {
                onNudgeToView();
            }
        });
    }
}
