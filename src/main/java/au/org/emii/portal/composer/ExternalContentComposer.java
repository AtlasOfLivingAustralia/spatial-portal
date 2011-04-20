package au.org.emii.portal.composer;

import org.zkoss.zk.ui.SuspendNotAllowedException;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Iframe;
import org.zkoss.zul.Window;

public class ExternalContentComposer extends Window {

    private static final long serialVersionUID = 1L;
    String src;

    @Override
    public void doOverlapped() {
        super.doOverlapped();

        this.getFellow("hide").addEventListener("onClick", new EventListener() {

            public void onEvent(Event event) throws Exception {
                ((Iframe)event.getTarget() //toolbarbutton
                        .getParent() //control
                        .getParent().getFellow("externalContentIframe")).setSrc("/img/loading_small.gif");

                event.getTarget() //toolbarbutton
                        .getParent() //control
                        .getParent().setVisible(false); //window
            }
        });
        
        this.getFellow("reset").addEventListener("onClick", new EventListener() {

            public void onEvent(Event event) throws Exception {
                Events.echoEvent("setSrc", event.getTarget().getParent().getParent(), null);
            }
        });
    }

    @Override
    public void doModal() throws InterruptedException, SuspendNotAllowedException  {
        super.doModal();

        this.getFellow("hide").addEventListener("onClick", new EventListener() {

            public void onEvent(Event event) throws Exception {
                ((Iframe)event.getTarget() //toolbarbutton
                        .getParent() //control
                        .getParent().getFellow("externalContentIframe")).setSrc("/img/loading_small.gif");

                event.getTarget() //toolbarbutton
                        .getParent() //control
                        .getParent().setVisible(false); //window
            }
        });

        this.getFellow("reset").addEventListener("onClick", new EventListener() {

            public void onEvent(Event event) throws Exception {
                Events.echoEvent("setSrc", event.getTarget().getParent().getParent(), null);
            }
        });
    }

    public void setSrc(Event event) {
        if(event.getData() == null) {
            ((Iframe)getFellow("externalContentIframe")).setSrc("");
            Events.echoEvent("setSrc", event.getTarget(), src);
        } else {
            ((Iframe)getFellow("externalContentIframe")).setSrc(src);
        }
    }
}
