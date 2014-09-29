/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.emii.portal.composer;

import au.org.ala.spatial.StringConstants;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.util.PrintMapComposer;
import au.org.emii.portal.value.BoundingBox;
import org.apache.log4j.Logger;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Textbox;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author Adam
 */
public class PrintingComposer extends UtilityComposer {

    private static final Logger LOGGER = Logger.getLogger(PrintingComposer.class);

    private Textbox txtHeader;
    private Checkbox chkGrid;
    private Combobox cbFormat;
    private Combobox cbResolution;
    private String tbxPrintHack;
    private String baseMap;
    private BoundingBox boundingBox;
    private List<MapLayer> mapLayers;

    @Override
    public void afterCompose() {
        super.afterCompose();

        Map m = Executions.getCurrent().getArg();
        if (m != null) {
            for (Object o : m.entrySet()) {
                Object key = ((Map.Entry) o).getKey();
                if (key instanceof String) {
                    if (key.equals(StringConstants.PRINT_PARAMS)) {
                        tbxPrintHack = (String) ((Map.Entry) o).getValue();
                    } else if (key.equals(StringConstants.BASE_LAYER)) {
                        baseMap = (String) ((Map.Entry) o).getValue();
                    } else if (key.equals(StringConstants.BOUNDING_BOX)) {
                        boundingBox = (BoundingBox) ((Map.Entry) o).getValue();
                    } else if (key.equals(StringConstants.LAYERS)) {
                        mapLayers = (List<MapLayer>) ((Map.Entry) o).getValue();
                    }
                }
            }
        }


        cbFormat.setSelectedIndex(0);
        cbResolution.setSelectedIndex(0);

        if (!"outline".equalsIgnoreCase(getMapComposer().getBaseMap())) {
            getFellow("hboxImageResolution").setVisible(false);
        }

        txtHeader.setValue((new SimpleDateFormat("dd/MM/yyyy")).format(new Date()));
    }

    public void onClick$btnExport(Event event) {
        //header text
        String header = txtHeader.getValue();

        //grid
        double grid = 0;
        if (chkGrid.isChecked()) {
            grid = 1;
        }

        //format (pdf, png, jpg)
        String format = cbFormat.getValue();

        //resolution (current == 0, print == 1)
        int resolution = cbResolution.getSelectedIndex();

        print(header, grid, format, resolution, false);

        this.detach();
    }

    void print(String header, double grid, String format, int resolution, boolean preview) {
        //tbxPrintHack is 'screen width, screen height, map extents'
        LOGGER.debug("tbxPrintHack:" + tbxPrintHack);
        String[] ps = tbxPrintHack.split(",");

        double[] extents = new double[4];
        extents[0] = Double.parseDouble(ps[2]);
        extents[1] = Double.parseDouble(ps[3]);
        extents[2] = Double.parseDouble(ps[4]);
        extents[3] = Double.parseDouble(ps[5]);

        int[] windowSize = new int[2];
        windowSize[0] = Integer.parseInt(ps[0]);
        windowSize[1] = Integer.parseInt(ps[1]);

        if ("png".equalsIgnoreCase(format)) {
            Filedownload.save(new PrintMapComposer(baseMap, mapLayers, boundingBox, extents, windowSize, header, "png", resolution).get(), StringConstants.IMAGE_PNG, "map_export.png");
        } else if ("pdf".equalsIgnoreCase(format)) {
            Filedownload.save(new PrintMapComposer(baseMap, mapLayers, boundingBox, extents, windowSize, header, "pdf", resolution).get(), "application/pdf", "map_export.pdf");
        } else {
            Filedownload.save(new PrintMapComposer(baseMap, mapLayers, boundingBox, extents, windowSize, header, "jpg", resolution).get(), "image/jpeg", "map_export.jpg");
        }
    }

}
