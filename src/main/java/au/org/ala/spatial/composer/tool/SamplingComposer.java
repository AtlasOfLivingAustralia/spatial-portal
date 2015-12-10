/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.ala.spatial.composer.tool;

import au.org.ala.spatial.StringConstants;
import au.org.ala.spatial.composer.output.SamplingAnalysisDownloadController;
import au.org.ala.spatial.util.BiocacheQuery;
import au.org.ala.spatial.util.CommonData;
import au.org.ala.spatial.util.Query;
import au.org.ala.spatial.util.QueryUtil;
import au.org.emii.portal.menu.SelectedArea;
import org.apache.log4j.Logger;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Filedownload;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author ajay
 */
public class SamplingComposer extends ToolComposer {
    private static final Logger LOGGER = Logger.getLogger(SamplingComposer.class);
    private int generationCount = 1;

    @Override
    public void afterCompose() {
        super.afterCompose();
        this.selectedMethod = StringConstants.SAMPLING;
        this.totalSteps = 3;

        this.setIncludeAnalysisLayersForUploadQuery(true);
        this.setIncludeAnalysisLayersForAnyQuery(true);

        this.loadAreaLayers();
        this.loadSpeciesLayers();
        this.loadGridLayers(false, true, false);
        this.updateWindowTitle();

    }

    @Override
    public void onLastPanel() {
        super.onLastPanel();

        this.updateName(getMapComposer().getNextAreaLayerName("My Sampling"));
    }

    @Override
    public boolean onFinish() {
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
            String[] layersDisplaynames = null;
            String envlayers = getSelectedLayersWithDisplayNames();
            if (envlayers.length() > 0) {
                layers = envlayers.split(":");
                layersDisplaynames = new String[layers.length];
                for (int i = 0; i < layers.length; i++) {
                    String[] l = layers[i].split("\\|");
                    String newName = CommonData.getLayerFacetName(l[0]);

                    layers[i] = newName;
                    layersDisplaynames[i] = l[1];
                }
            }

            if (query instanceof BiocacheQuery) {
                String[] inBiocache = null;
                String[] outBiocache;
                String[] outBiocacheDN;

                //split layers into 'in biocache' and 'out of biocache'
                Set<String> biocacheLayers = CommonData.getBiocacheLayerList();
                List<String> aInBiocache = new ArrayList<String>();
                List<String> aOutBiocache = new ArrayList<String>();
                List<String> aInBiocacheDN = new ArrayList<String>();
                List<String> aOutBiocacheDN = new ArrayList<String>();

                if (layers != null) {
                    for (int i = 0; i < layers.length; i++) {
                        String s = layers[i];
                        if (biocacheLayers.contains(s)) {
                            aInBiocache.add(s);
                            aInBiocacheDN.add(layersDisplaynames[i]);
                        } else {
                            aOutBiocache.add(s);
                            aOutBiocacheDN.add(layersDisplaynames[i]);
                        }
                    }
                }
                if (!aInBiocache.isEmpty()) {
                    inBiocache = new String[aInBiocache.size()];
                    aInBiocache.toArray(inBiocache);
                }
                if (!aOutBiocache.isEmpty()) {
                    outBiocache = new String[aOutBiocache.size()];
                    aOutBiocache.toArray(outBiocache);
                    outBiocacheDN = new String[aOutBiocacheDN.size()];
                    aOutBiocacheDN.toArray(outBiocacheDN);
                    getMapComposer().setDownloadSecondQuery(query);
                    getMapComposer().setDownloadSecondLayers(outBiocache, outBiocacheDN);
                    SamplingAnalysisDownloadController c = (SamplingAnalysisDownloadController) Executions.createComponents("/WEB-INF/zul/output/SamplingAnalysisDownload.zul", getMapComposer(), null);
                    c.setParent(getMapComposer());
                    c.doModal();
                } else {
                    getMapComposer().setDownloadSecondQuery(null);
                    getMapComposer().setDownloadSecondLayers(null, null);
                }

                //test for URL download
                String url = query.getDownloadUrl(inBiocache);
                LOGGER.debug("Sending file to user: " + url);

                Events.echoEvent(StringConstants.OPEN_HTML, getMapComposer(), "Download\n" + url);

                try {
                    remoteLogger.logMapAnalysis("species sampling", "Export - Species Sampling", sa.getWkt(), query.getName(), envlayers, pid, "", "download");
                } catch (Exception e) {
                    LOGGER.error("remote logger error", e);
                }
                this.detach();
            } else {

                String fileUrl = query.getDownloadUrl(layers);

                Filedownload.save(new URL(fileUrl).openStream(), "application/zip", query.getName().replaceAll(" ", "_") + "_sample_" + ".zip");

                try {
                    remoteLogger.logMapAnalysis("species sampling", "Export - Species Sampling", sa.getWkt(), query.getName(), envlayers, pid, "", "");
                } catch (Exception e) {
                    LOGGER.error("remote logger error", e);
                }
                this.detach();
            }

            return true;

        } catch (Exception e) {
            LOGGER.error("Exception calling sampling.download:", e);
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
                    searchSpeciesACComp.getAutoComplete().setFocus(true);
                } else {
                    rgSpecies.setFocus(true);
                }
                break;
            case 3:
                lbListLayers.setFocus(true);

                //tick and disable all 'DEFAULT' sampled layers
                if (getSelectedSpecies() != null) {
                    Query q = getSelectedSpecies();
                    String[] defaultFields = q.getDefaultDownloadFields();

                    if (defaultFields.length > 0) {
                        lbListLayers.selectLayersAndDisable(defaultFields);
                    }
                }

                updateLayerSelectionCount();

                break;
            default:
                LOGGER.error("invalid step for SamplingComposer: " + currentStep);
        }
    }
}
