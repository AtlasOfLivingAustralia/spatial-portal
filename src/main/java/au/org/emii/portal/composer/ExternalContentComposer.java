package au.org.emii.portal.composer;

import au.org.ala.spatial.StringConstants;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Iframe;
import org.zkoss.zul.Window;

public class ExternalContentComposer extends Window {

    private static final long serialVersionUID = 1L;
    private String src;

    @Override
    public void doOverlapped() {
        super.doOverlapped();

        this.getFellow("hide").addEventListener(StringConstants.ONCLICK, new EventListener() {

            public void onEvent(Event event) throws Exception {
                close();
            }
        });

        this.getFellow(StringConstants.RESET).addEventListener(StringConstants.ONCLICK, new EventListener() {

            public void onEvent(Event event) throws Exception {
                Events.echoEvent(StringConstants.SET_SRC, event.getTarget().getParent().getParent(), null);
            }
        });

        this.getFellow(StringConstants.BREAKOUT).addEventListener(StringConstants.ONCLICK, new EventListener() {

            public void onEvent(Event event) throws Exception {
                close();
            }
        });
    }

    @Override
    public void doModal() {
        super.doModal();

        this.getFellow("hide").addEventListener(StringConstants.ONCLICK, new EventListener() {

            public void onEvent(Event event) throws Exception {
                close();
            }
        });

        this.getFellow(StringConstants.RESET).addEventListener(StringConstants.ONCLICK, new EventListener() {

            public void onEvent(Event event) throws Exception {
                Events.echoEvent(StringConstants.SET_SRC, event.getTarget().getParent().getParent(), null);
            }
        });

        this.getFellow(StringConstants.BREAKOUT).addEventListener(StringConstants.ONCLICK, new EventListener() {

            public void onEvent(Event event) throws Exception {
                close();
            }
        });
    }

    public void setSrc(Event event) {
        if (event.getData() == null) {
            ((Iframe) getFellow("externalContentIframe")).setSrc("");
            Events.echoEvent(StringConstants.SET_SRC, event.getTarget(), src);
        } else {
            ((Iframe) getFellow("externalContentIframe")).setSrc(src);
        }
    }

    void close() {
        detach();
    }

    public void setSrc(String newUri) {
        this.src = newUri;
    }
}
