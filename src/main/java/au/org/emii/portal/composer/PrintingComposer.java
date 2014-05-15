/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.emii.portal.composer;

import au.org.emii.portal.util.SessionPrint;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.*;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author Adam
 */
public class PrintingComposer extends UtilityComposer {

    private static Logger logger = Logger.getLogger(PrintingComposer.class);

    Button btnExport;
    Button btnPreview;
    Textbox txtHeader;
    Checkbox chkGrid;
    Combobox cbFormat;
    Combobox cbResolution;

    @Override
    public void afterCompose() {
        super.afterCompose();

        cbFormat.setSelectedIndex(0);
        cbResolution.setSelectedIndex(0);

        txtHeader.setValue((new SimpleDateFormat("dd/MM/yyyy")).format(new Date()));
    }

    public void onClick$btnExport(Event event) {
        //header text
        String header = txtHeader.getValue();

        //grid
        double grid = 0; //zero is none
        if (chkGrid.isChecked()) {
            grid = 1;
        }

        //format (pdf, png, jpg)
        String format = cbFormat.getValue();

        //resolution (current == 0, print == 1)
        int resolution = cbResolution.getSelectedIndex();

        getMapComposer().print(header, grid, format, resolution, false);

        this.detach();  //close this window
    }

    public void onClick$btnPreview(Event event) {
        //header text
        String header = null;
        try {
            header = StringEscapeUtils.escapeHtml(txtHeader.getValue());
        } catch (Exception e) {
            header = "";
        }

        //grid
        double grid = 0; //zero is none
        if (chkGrid.isChecked()) {
            grid = 1;
        }

        //format (pdf, png, jpg)
        String format = cbFormat.getValue();

        //resolution (current == 0, print == 1)
        int resolution = cbResolution.getSelectedIndex();

        SessionPrint sp = getMapComposer().print(header, grid, format, resolution, true);

        String previewUrl = sp.getPreviewUrl();

        logger.debug("PREVIEW URL: " + previewUrl);

        //popup another window
        Window w = new Window("Export preview", "normal", false);
        w.setClosable(true);
        w.setSizable(true);
        w.setWidth("640px");
        w.setHeight("480px");
        Iframe iframe = new Iframe();
        iframe.setWidth("100%");
        iframe.setHeight("100%");
        iframe.setSrc(previewUrl);
        iframe.setParent(w);
        iframe.setId("iframe");
        w.setParent(this.getParent());

        //fix for closing preview closing the session
        w.addEventListener("onClose", new EventListener() {

            @Override
            public void onEvent(Event event) throws Exception {
                Iframe frame = ((Iframe) event.getTarget().getFellow("iframe"));
                frame.detach();
                frame.setSrc("");
            }
        });
        try {
            w.doModal();
        } catch (Exception e) {
            logger.error("Error opening map print options window", e);
        }
    }

}
