/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.emii.portal.composer;

import org.apache.log4j.Logger;
import org.zkoss.zk.ui.event.Event;
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

}
