package au.org.emii.portal.composer;

import org.apache.log4j.Logger;

import java.util.Map;

/**
 * Composer to handle internal zk errors by logging and displaying a
 * popup error message.
 * <p/>
 * This gets triggered as a last resort when something is badly broken
 * - it's not used for 'normal' error messages, so in this case, we
 * hide the error from the user to prevent them seeing a stack trace
 * or NPE which is the sort of error that normally ends up getting
 * handled here.
 * <p/>
 * We display a catch all 'internal error' message to the user and
 * then go on to compose the ZkError.zul file.
 * <p/>
 * This composer gets triggered by registering ZkError.zul in zk.xml
 *
 * @author geoff
 */
public class ZkErrorComposer extends UtilityComposer {

    private static Logger logger = Logger.getLogger(ZkErrorComposer.class);


    private static final long serialVersionUID = 1L;

    /**
     * Kill our window and attempt to continue
     */
    public void onClick$continueButton() {
        detach();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void afterCompose() {
        super.afterCompose();
        Map map = getPage().getAttributes(REQUEST_SCOPE);
        Throwable exception = (Throwable) map.get("javax.servlet.error.exception");

        logger.error("***FIXME***! Unhandled error delivered to user: ", exception);
    }

}
