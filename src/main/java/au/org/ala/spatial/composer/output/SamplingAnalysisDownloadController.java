/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.ala.spatial.composer.output;

import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.composer.SamplingDownloadUtil;
import au.org.emii.portal.composer.UtilityComposer;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Button;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Label;

/**
 * @author ajay
 */
public class SamplingAnalysisDownloadController extends UtilityComposer {

    private String id;
    private long startTime;
    private byte[] bytes;
    private MapComposer mc;
    private Thread download = new Thread() {
        @Override
        public void run() {
            bytes = SamplingDownloadUtil.downloadSecond(mc, mc.getDownloadSecondQuery(),
                    mc.getDownloadSecondLayers(), mc.getDownloadSecondLayersDN());
        }
    };

    @Override
    public void afterCompose() {
        super.afterCompose();

        startTime = System.currentTimeMillis();
        mc = getMapComposer();

        String[] layers = mc.getDownloadSecondLayers();
        String[] layersDN = mc.getDownloadSecondLayersDN();
        String list = "";
        for (int i = 0; i < layers.length; i++) {
            if (list.length() > 0) list += ", ";
            list += layersDN[i] + " (" + layers[i] + ")";
        }
        ((Label) getFellow("layersForIndexing")).setValue(list);

        download.start();

        Events.echoEvent("checkDownloadStatus", this, null);
    }

    public void onClick$btnDownload(Event event) {
        Filedownload.save(bytes, "application/zip", "analysis_output_intersect.zip");

        this.detach();
    }

    public void onClick$btnCancel(Event event) {
        this.detach();
    }

    public void checkDownloadStatus(Event event) {
        boolean finished = !download.isAlive();
        if (finished && bytes != null) {
            ((Label) getFellow("statusLabel")).setValue("ready for download");
            ((Button) getFellow("btnDownload")).setDisabled(false);
        } else if (!finished) {
            ((Label) getFellow("statusLabel")).setValue("preparing... " + (System.currentTimeMillis() - startTime) / 1000 + "s");
        } else {
            //error
            ((Label) getFellow("statusLabel")).setValue("Unknown error. Unable to download.");
            return;
        }

        //repeat after wait
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Events.echoEvent("checkDownloadStatus", this, null);
    }
}
