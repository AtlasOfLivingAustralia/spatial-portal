/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.ala.spatial.composer.progress;

import au.org.ala.spatial.composer.tool.SamplingComposer;
import au.org.ala.spatial.data.Query;
import au.org.ala.spatial.util.CommonData;
import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.menu.MapLayer;
import org.apache.log4j.Logger;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zul.*;

import java.io.ByteArrayInputStream;

/**
 * @author ajay
 */
public class SamplingProgressController extends UtilityComposer {

    private static Logger logger = Logger.getLogger(SamplingProgressController.class);
    Thread samplingThread;
    byte[] download;
    Query query;
    String[] layers;
    String[] displayNames;

    long start;
    long estimate;

    Label jobstatus;
    Progressmeter jobprogress;
    Timer timer;
    Textbox tbPid;
    public String pid = null;
    public SamplingComposer parent = null;

    @Override
    public void afterCompose() {
        super.afterCompose();

        timer.stop();
    }

    public void onTimer$timer(Event e) {
        //is it running?
        boolean running = samplingThread.isAlive();

        //get status
        if (!running) {
            timer.stop();
            finish();
        } else {
            //update progress bar
            long now = System.currentTimeMillis();

            double pos = Math.min((now - start) / (double) estimate, 0.95);
            jobprogress.setValue((int) (pos * 100));
        }
    }

    public void onClick$btnCancel(Event e) {
        samplingThread.interrupt();
        this.detach();
    }

    void finish() {
        try {
            if (download != null) {
                Filedownload.save(new ByteArrayInputStream(download), "application/zip", query.getName() + ".zip");
            } else {
                Messagebox.show("Unable to download sample file.", "ALA Spatial Analysis Toolkit - Sampling", Messagebox.OK, Messagebox.ERROR);
            }
        } catch (Exception e) {
            logger.error("error downloading user points sample", e);
        }
        this.detach();
    }

    public void start(Query q, String[] l) {
        this.query = q;
        this.layers = l;

        //estimate
        double perGrid = getMapComposer().getSettingsSupplementary().getValueAsDouble("sampling_time_per_grid");
        double perShapeFile = getMapComposer().getSettingsSupplementary().getValueAsDouble("sampling_time_per_shape_file");
        int threadCount = getMapComposer().getSettingsSupplementary().getValueAsInt("sampling_thread_count");
        estimate = 0;
        for (int i = 0; i < l.length; i++) {
            if (l[i] == null) {
                continue;
            }
            if (l[i].startsWith("cl")) {
                estimate += perShapeFile * 1000 / threadCount;
            } else {
                estimate += perGrid * 1000 / threadCount;
            }
        }

        if (layers != null && layers.length > 0) {
            displayNames = new String[layers.length];
            for (int i = 0; i < layers.length; i++) {
                displayNames[i] = CommonData.getFacetLayerDisplayName(layers[i]);
                if (displayNames[i] == null || displayNames[i].equals(layers[i])) {
                    //attempt to get the name from a mapLayer, for analysis layers
                    MapLayer ml = getMapComposer().getMapLayer(layers[i]);
                    if (ml != null) {
                        displayNames[i] = ml.getDisplayName();
                    }
                }
                if (displayNames[i] == null || displayNames[i].equals(layers[i])) {
                    displayNames[i] = layers[i];
                }
            }
        }

        samplingThread = new Thread() {

            @Override
            public void run() {
                // download = query.getDownloadBytes(layers, displayNames);
            }
        };

        samplingThread.start();
        timer.start();
        start = System.currentTimeMillis();
        jobstatus.setValue("preparing...");

        onTimer$timer(null);
    }
}
