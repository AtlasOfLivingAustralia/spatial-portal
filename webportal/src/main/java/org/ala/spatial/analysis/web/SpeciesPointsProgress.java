/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.analysis.web;

import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.composer.UtilityComposer;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Button;
import org.zkoss.zul.Label;
import org.zkoss.zul.Progressmeter;
import org.zkoss.zul.Timer;

/**
 *
 * @author ajay
 */
public class SpeciesPointsProgress extends UtilityComposer {

    Label jobstatus;
    Progressmeter jobprogress;
    Timer timer;
    String label;
    String url;
    String params;
    boolean forceReload;
    long start;
    String lsid;
    String urlname = "";
    GetGeojson getGeojson = null;
    int partsCount;
    boolean loaded = false;
    boolean layerReady = false;
    int parts_count = 0;

    Button btnCancel;

    @Override
    public void afterCompose() {
        super.afterCompose();

        timer.stop();
    }

    public void start(String label_, String url_, String params_, boolean forceReload_, int partsCount_, String lsid_) {
        label = label_;
        url = url_;
        params = params_;
        forceReload = forceReload_;
        partsCount = partsCount_;
        lsid = lsid_;

        EventListener el = new EventListener() {

            public void onEvent(Event event) throws Exception {
                // refresh count may be required if area is
                // not an envelope.
                String layer = getMapComposer().getLayerLoaded();
                if (layer.contains(url)) {
                    loaded = true;
                }
            }
        };
        getMapComposer().addLayerLoadedEventListener("progressbar:" + label, el);

        //getGeojson = new GetGeojson(url + "_" + parts_count, getMapComposer());
        parts_count++;        
        getMapComposer().addGeoJSONLayerProgressBarReady(label, url + "_0", params, forceReload, "", lsid, partsCount);

        //getGeojson.start();
        

        timer.start();

        jobstatus.setValue("getting part " + (parts_count) + " of " + (partsCount));

        start = System.currentTimeMillis();

        onTimer$timer(null);
    }

    public void onTimer$timer(Event e) {
        if (loaded) {
            //any more?
            if(parts_count < partsCount){
                parts_count++;

                getMapComposer().appendGeoJSONLayerProgressBarReady(label, url, parts_count-1, params, forceReload, "", lsid);

                jobstatus.setValue("getting part " + (parts_count) + " of " + (partsCount));

                loaded = false;
            } else {
                jobstatus.setValue("finishing");
                timer.stop();
                getMapComposer().removeLayerLoadedEventListener("progressbar:" + label);
                this.detach();
                return;
            }
        }

        //increment progress, 1ms per 1 records
        long duration = System.currentTimeMillis() - start;
        double progress = (parts_count) / (double) (partsCount);

        if (progress > 1) {
            progress = 1;
        }

        jobprogress.setValue((int) (progress * 95)); //limit of 95 in progress bar
    }

    public void onClick$btnCancel(Event e) {
        getMapComposer().removeLayerLoadedEventListener("progressbar:" + label);
        this.detach();
    }

    public void onClick$btnHide(Event e) {
        getMapComposer().removeLayerLoadedEventListener("progressbar:" + label);
        this.detach();
    }
}

class GetGeojson extends Thread {

    String url;
    String geojson;
    MapComposer mapComposer;

    public GetGeojson(String url_, MapComposer mapComposer_) {
        url = url_;
        mapComposer = mapComposer_;

        setPriority(Thread.MIN_PRIORITY);
    }

    public void run() {
        try {
            //geojson = mapComposer.getJson(url);
            //don't need geojson at server
            geojson = "";
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getGeojson() {
        return geojson;
    }
}
