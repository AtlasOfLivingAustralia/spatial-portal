/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.analysis.web;

import java.util.ArrayList;
import java.util.Set;
import org.ala.spatial.data.BiocacheQuery;
import org.ala.spatial.data.Query;
import org.ala.spatial.data.QueryUtil;
import org.ala.spatial.util.CommonData;
import org.ala.spatial.util.SelectedArea;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.Events;

/**
 *
 * @author ajay
 */
public class AddToolSamplingComposer extends AddToolComposer {

    int generation_count = 1;

    @Override
    public void afterCompose() {
        super.afterCompose();

        this.selectedMethod = "Sampling";
        this.totalSteps = 3;

        this.setIncludeAnalysisLayersForUploadQuery(false);
        this.setIncludeAnalysisLayersForAnyQuery(false);

        this.loadAreaLayers();
        this.loadSpeciesLayers();
        this.loadGridLayers(false, true, false);
        this.updateWindowTitle();

    }

    @Override
    public void onLastPanel() {
        super.onLastPanel();
        //this.updateName("Sampling #" + generation_count);
        this.updateName(getMapComposer().getNextAreaLayerName("My Sampling"));
    }

    @Override
    public boolean onFinish() {
        //super.onFinish();

        Query query = getSelectedSpecies();
        if (query == null) {
            getMapComposer().showMessage("There is a problem selecting the species.  Try to select the species again", this);
            return false;
        }

        return download(null);
    }

    public boolean download(Event event) {

        try {
            SelectedArea sa = getSelectedArea();
            Query query = QueryUtil.queryFromSelectedArea(getSelectedSpecies(), sa, false, getGeospatialKosher());
            //test size        
            if (query.getOccurrenceCount() <= 0) {
                getMapComposer().showMessage("No occurrences selected. Please try again", this);
                return false;
            }

            //translate layer names
            String[] layers = null;
            String envlayers = getSelectedLayers();
            if (envlayers.length() > 0) {
                layers = envlayers.split(":");
                for (int i = 0; i < layers.length; i++) {
                    String l = layers[i];
                    String new_name = CommonData.getLayerFacetName(layers[i]);
                    if (layers[i] == null || new_name == null || layers[i].equals(new_name)) {
                        System.out.println("failed to getLayerFacetName for " + l);
                    } else {
                        layers[i] = new_name;
                    }
                }
            }

            if (query instanceof BiocacheQuery) {
                String[] inBiocache = null;
                String[] outBiocache = null;

                //split layers into 'in biocache' and 'out of biocache'
                Set<String> biocacheLayers = CommonData.biocacheLayerList;
                ArrayList<String> aInBiocache = new ArrayList<String>();
                ArrayList<String> aOutBiocache = new ArrayList<String>();

                if (envlayers.length() > 0) {
                    for (String s : layers) {
                        if (biocacheLayers.contains(s)) {
                            aInBiocache.add(s);
                        } else {
                            aOutBiocache.add(s);
                        }
                    }
                }
                if (aInBiocache.size() > 0) {
                    inBiocache = new String[aInBiocache.size()];
                    aInBiocache.toArray(inBiocache);
                }
                if (aOutBiocache.size() > 0) {
                    outBiocache = new String[aOutBiocache.size()];
                    aOutBiocache.toArray(outBiocache);
                    getMapComposer().downloadSecondQuery = query;
                    getMapComposer().downloadSecondLayers = outBiocache;
                    SamplingAnalysisDownloadWCController c = (SamplingAnalysisDownloadWCController) Executions.createComponents("/WEB-INF/zul/SamplingAnalysisDownload.zul", getMapComposer(), null);
                    c.doModal();
                } else {
                    getMapComposer().downloadSecondQuery = null;
                    getMapComposer().downloadSecondLayers = null;
                }

                //test for URL download
                String url = query.getDownloadUrl(inBiocache);
                System.out.println("Sending file to user: " + url);

                Events.echoEvent("openHTML", getMapComposer(), "Download\n" + url);

                try {
                    remoteLogger.logMapAnalysis("species sampling", "Export - Species Sampling", sa.getWkt(), query.getName(), envlayers, pid, "", "download");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                this.detach();
            } else {

                //download byte data.  Requires a progress bar to prevent timeout issues.
                Component c = getMapComposer().getFellowIfAny("samplingprogress");
                if (c != null) {
                    c.detach();
                }
                SamplingProgressWCController window = (SamplingProgressWCController) Executions.createComponents("WEB-INF/zul/AnalysisSamplingProgress.zul", getMapComposer(), null);
                window.parent = this;
                window.start(query, layers);
                try {
                    window.doModal();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                try {
                    remoteLogger.logMapAnalysis("species sampling", "Export - Species Sampling", sa.getWkt(), query.getName(), envlayers, pid, "", "");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                this.detach();
            }

            return true;

        } catch (Exception e) {
            System.out.println("Exception calling sampling.download:");
            e.printStackTrace(System.out);
            getMapComposer().showMessage("Unknown error.", this);
        }

        return false;
    }

    @Override
    void fixFocus() {
        switch (currentStep) {
            case 1:
                rgArea.setFocus(true);
                break;
            case 2:
                if (rSpeciesSearch.isChecked()) {
                    searchSpeciesAuto.setFocus(true);
                } else {
                    rgSpecies.setFocus(true);
                }
                break;
            case 3:
                lbListLayers.setFocus(true);
                break;
        }
    }
}
