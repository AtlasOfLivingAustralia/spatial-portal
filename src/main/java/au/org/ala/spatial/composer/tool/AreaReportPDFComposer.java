/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.ala.spatial.composer.tool;

import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.menu.SelectedArea;
import au.org.emii.portal.util.AreaReportPDF;
import org.apache.log4j.Logger;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zul.*;

import java.util.Map;
import java.util.concurrent.*;

/**
 * @author ajay
 */
public class AreaReportPDFComposer extends ToolComposer {

    private static final Logger LOGGER = Logger.getLogger(AreaReportPDFComposer.class);
    private ExecutorService pool;
    private Future<AreaReportPDF> future;
    private Map progress;
    private Label progressLabel;
    private Div divProgress;
    private Progressmeter jobprogress;

    @Override
    public void afterCompose() {
        super.afterCompose();

        this.selectedMethod = "Detailed Area Report (PDF)";
        this.totalSteps = 1;

        this.loadAreaLayers();
        this.updateWindowTitle();
    }

    @Override
    public boolean onFinish() {
        if (pool != null) {
            return true;
        }
        //close any existing area report
        Window w = (Window) getPage().getFellowIfAny("popupResults");
        if (w != null) {
            w.detach();
        }
        SelectedArea sa = getSelectedArea();
        String areaName = getSelectedAreaName();
        String areaDisplayName = getSelectedAreaDisplayName();
        MapLayer ml = getMapComposer().getMapLayer(areaName);
        double[] bbox = null;
        if (ml != null
                && ml.getMapLayerMetadata().getBbox() != null
                && ml.getMapLayerMetadata().getBbox().size() == 4) {
            bbox = new double[4];
            bbox[0] = ml.getMapLayerMetadata().getBbox().get(0);
            bbox[1] = ml.getMapLayerMetadata().getBbox().get(1);
            bbox[2] = ml.getMapLayerMetadata().getBbox().get(2);
            bbox[3] = ml.getMapLayerMetadata().getBbox().get(3);
        }

        final String area = areaDisplayName;
        final String wkt = (ml == null ? sa.getWkt() : ml.getWKT());
        progress = new ConcurrentHashMap();
        progress.put("label", "Starting");
        progress.put("percent", 0.0);

        Callable pdfAreaReport = new Callable<AreaReportPDF>() {
            @Override
            public AreaReportPDF call() {
                return new AreaReportPDF(wkt, area, progress);
            }
        };

        pool = Executors.newFixedThreadPool(1);
        future = pool.submit(pdfAreaReport);

        getMapComposer().getOpenLayersJavascript().execute("setTimeout('checkProgress()', 2000);");

        divProgress.setVisible(true);

        return true;
    }

    public void checkProgress(Event event) {
        if (future.isDone()) {
            progressLabel.setValue("Finished. Starting download.");
            jobprogress.setValue(100);
            try {
                Filedownload.save(future.get().getPDF(), "application/pdf", "areaReport.pdf");

                detach();
            } catch (Exception e) {
                LOGGER.error("failed to download finished PDF report", e);
            }
        } else {
            progressLabel.setValue("Running> " + (String) progress.get("label"));
            jobprogress.setValue((int) Math.min(100, (int) (((Double) progress.get("percent")) * 100)));

            getMapComposer().getOpenLayersJavascript().execute("setTimeout('checkProgress()', 2000);");
        }
    }

    @Override
    void fixFocus() {
        rgArea.setFocus(true);
    }

    @Override
    public void onClick$btnCancel(Event event) {
        try {
            progress.put("cancel", true);
            pool.shutdownNow();
        } catch (Exception e) {
            LOGGER.error("failed to shutdown pdf area report when cancelled.", e);
        }

        super.onClick$btnCancel(event);
    }
}
