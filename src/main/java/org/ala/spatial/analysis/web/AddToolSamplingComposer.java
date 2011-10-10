/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.analysis.web;

import org.ala.spatial.data.Query;
import org.ala.spatial.data.QueryUtil;
import org.ala.spatial.util.CommonData;
import org.ala.spatial.util.SelectedArea;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Messagebox;

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

        this.loadAreaLayers();
        this.loadSpeciesLayers();
        this.loadGridLayers(false, true);
        this.updateWindowTitle();

    }

    @Override
    public void onLastPanel() {
        super.onLastPanel();
        //this.updateName("Sampling #" + generation_count);
        this.updateName(getMapComposer().getNextAreaLayerName("My Sampling"));
    }

    @Override
    public void onFinish() {
        //super.onFinish();

        System.out.println("Area: " + getSelectedArea());
        System.out.println("Species: " + getSelectedSpecies());
        System.out.println("Layers: " + getSelectedLayers());

        download(null);

    }

    public void download(Event event) {

        try {
            SelectedArea sa = getSelectedArea();
            Query query = QueryUtil.queryFromSelectedArea(getSelectedSpecies(), sa, false);
            //test size        
            if (query.getOccurrenceCount() <= 0) {
                Messagebox.show("No occurrences selected. Please try again", "ALA Spatial Analysis Toolkit - Sampling", Messagebox.OK, Messagebox.ERROR);
                return;
            }

            //translate layer names
            String[] layers = null;
            String envlayers = getSelectedLayers();
            if (envlayers.length() > 0) {
                layers = envlayers.split(":");
                for (int i = 0; i < layers.length; i++) {
                    layers[i] = CommonData.getLayerFacetName(layers[i]);
                }
            }

            //test for URL download
            String url = query.getDownloadUrl(layers);
            if (url != null) {
                System.out.println("Sending file to user: " + url);

                //TODO: find some way to do this nicely.
                //Events.echoEvent("openHTML", getMapComposer(), "Download\n<a href='" + url + "' >click to start download</a>");
                Events.echoEvent("openHTML", getMapComposer(), "Download\n" + url);

                //Would clients still treat this as a popup if it were on prod?
                //getMapComposer().openHTML(event)Clients.evalJavaScript("window.open('" + url + "','download','')");

                //TODO: fix logging
                //getMapComposer().updateUserLogAnalysis("Sampling", "species: " + taxon + "; area: " + area, sbenvsel.toString(), CommonData.satServer + slist, pid, "Sampling results for species: " + taxon);

                this.detach();
            } else {
//                byte [] b = query.getDownloadBytes(layers);
//                if(b != null) {
//                    Filedownload.save(new ByteArrayInputStream(b), "application/zip", query.getName() + ".zip");
//                } else {
//                    Messagebox.show("Unable to download sample file. Please try again", "ALA Spatial Analysis Toolkit - Sampling", Messagebox.OK, Messagebox.ERROR);
//                }

                //download byte data.  Requires a progress bar to prevent timeout issues.
                SamplingProgressWCController window = (SamplingProgressWCController) Executions.createComponents("WEB-INF/zul/AnalysisSamplingProgress.zul", getMapComposer(), null);
                window.parent = this;
                window.start(query, layers);
                try {
                    window.doModal();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                this.detach();
            }

        } catch (Exception e) {
            System.out.println("Exception calling sampling.download:");
            e.printStackTrace(System.out);
        }
    }

    @Override
    void fixFocus() {
        switch (currentStep) {
            case 1:
                rgArea.setFocus(true);
                break;
            case 2:
                if(rSpeciesSearch.isChecked()) {
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
