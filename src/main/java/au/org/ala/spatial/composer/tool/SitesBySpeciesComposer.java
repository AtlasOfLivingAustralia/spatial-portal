package au.org.ala.spatial.composer.tool;

import au.org.ala.spatial.StringConstants;
import au.org.ala.spatial.composer.progress.ProgressController;
import au.org.ala.spatial.util.*;
import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.menu.MapLayerMetadata;
import au.org.emii.portal.menu.SelectedArea;
import au.org.emii.portal.util.LayerUtilitiesImpl;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.log4j.Logger;
import org.zkoss.zhtml.Filedownload;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Combobox;

import java.net.URL;
import java.net.URLEncoder;
import java.util.List;

/**
 * @author ajay
 */
public class SitesBySpeciesComposer extends ToolComposer {
    private static final Logger LOGGER = Logger.getLogger(SitesBySpeciesComposer.class);


    private Checkbox chkOccurrenceDensity;
    private Checkbox chkSpeciesDensity;
    private Checkbox chkSitesBySpecies;
    private Combobox cbMovingAverageSize;
    private Query query = null;
    private SelectedArea sa = null;

    @Override
    public void afterCompose() {
        super.afterCompose();

        this.selectedMethod = "Points To Grid";
        this.totalSteps = 3;

        if (dResolution.getValue() == 0) {
            dResolution.setValue(0.05);
        }

        this.loadAreaLayers();
        this.loadSpeciesLayers(false);
        this.updateWindowTitle();
    }

    @Override
    public void onLastPanel() {
        LOGGER.debug("**** On last step ****");
        super.onLastPanel();
    }

    @Override
    public boolean onFinish() {
        if (!hasEstimated && !isUserLoggedIn()) {
            checkEstimate();
            return false;
        }

        Query q = getSelectedSpecies();
        if (q == null) {
            getMapComposer().showMessage("There is a problem selecting the species.  Try to select the species again", this);
            return false;
        }

        if (searchSpeciesACComp.hasValidItemSelected()) {
            getMapComposer().mapSpeciesFromAutocompleteComponent(searchSpeciesACComp, getSelectedArea(), getGeospatialKosher(), false);
        } else if (rgSpecies.getSelectedItem() != null && StringConstants.MULTIPLE.equals(rgSpecies.getSelectedItem().getValue())) {
            getMapComposer().mapSpecies(q, StringConstants.SPECIES_ASSEMBLAGE, StringConstants.SPECIES, 0, LayerUtilitiesImpl.SPECIES, null, -1, MapComposer.DEFAULT_POINT_SIZE, MapComposer.DEFAULT_POINT_OPACITY, Util.nextColour(), false);
        }

        return runsitesbyspecies();
    }

    private void setupData() throws Exception {
        if (query == null) {
            sa = getSelectedArea();
            query = QueryUtil.queryFromSelectedArea(getSelectedSpecies(), sa, false, getGeospatialKosher());
        }
    }

    String run(boolean isEstimate) throws Exception {
        setupData();

        String ma = "9";
        if (cbMovingAverageSize.getSelectedItem() == null) {
            String txt = cbMovingAverageSize.getValue();
            for (int i = 0; i < cbMovingAverageSize.getItemCount(); i++) {
                if (txt != null && txt.equalsIgnoreCase(cbMovingAverageSize.getItemAtIndex(i).getLabel())) {
                    ma = cbMovingAverageSize.getItemAtIndex(i).getValue();
                    break;
                }
            }
        } else {
            ma = cbMovingAverageSize.getSelectedItem().getValue();
        }
        int movingAverageSize = Integer.parseInt(ma);
        if (movingAverageSize % 2 == 0 || movingAverageSize <= 0 || movingAverageSize >= 16) {
            getMapComposer().showMessage("Moving average size " + movingAverageSize + " is not valid.  Must be odd and between 1 and 15.", this);
            return null;
        }

        if (!chkOccurrenceDensity.isChecked()
                && !chkSitesBySpecies.isChecked()
                && !chkSpeciesDensity.isChecked()) {
            getMapComposer().showMessage("Must select at least one output; Sites by species, Occurrence density or Species richness.", this);
            return null;
        }

        Double gridResolution = dResolution.getValue();

        //something is wrong with the Doublebox, it keeps setting to zero
        if (gridResolution == 0) {
            dResolution.setValue(0.05);
            gridResolution = 0.05;
        }
        List<Double> bbox = Util.getBoundingBox(sa.getWkt());

        int occurrenceCount = query.getOccurrenceCount();
        int boundingboxcellcount = (int) ((bbox.get(2) - bbox.get(0))
                * (bbox.get(3) - bbox.get(1))
                / (gridResolution * gridResolution));

        LOGGER.debug("SitesBySpecies for " + occurrenceCount + " occurrences in up to " + boundingboxcellcount + " grid cells.");

        if (boundingboxcellcount > Integer.parseInt(CommonData.getSettings().getProperty("sitesbyspecies_maxbbcells"))) {
            getMapComposer().showMessage("Too many output grid cells: Decrease area or increase grid size.", this);
            return null;
        }

        if (occurrenceCount > Integer.parseInt(CommonData.getSettings().getProperty(StringConstants.SITESBYSPECIES_MAXOCCURRENCES))) {
            getMapComposer().showMessage("Too many occurrences for the selected species in this area.  " + occurrenceCount + " occurrences found, must be less than " + CommonData.getSettings().getProperty(StringConstants.SITESBYSPECIES_MAXOCCURRENCES), this);
            return null;
        }

        StringBuilder sbProcessUrl = new StringBuilder();
        sbProcessUrl.append(CommonData.getSatServer()).append("/ws/sitesbyspecies").append(isEstimate ? "/estimate" : "").append("?");

        sbProcessUrl.append("speciesq=").append(URLEncoder.encode(QueryUtil.queryFromSelectedArea(query, sa, false, getGeospatialKosher()).getQ(), StringConstants.UTF_8));
        sbProcessUrl.append("&bs=").append(URLEncoder.encode(query.getBS(), StringConstants.UTF_8));

        sbProcessUrl.append("&gridsize=").append(URLEncoder.encode(String.valueOf(gridResolution), StringConstants.UTF_8));

        if (chkOccurrenceDensity.isChecked()) {
            sbProcessUrl.append("&occurrencedensity=1");
        }
        if (chkSpeciesDensity.isChecked()) {
            sbProcessUrl.append("&speciesdensity=1");
        }
        if (chkSitesBySpecies.isChecked()) {
            sbProcessUrl.append("&sitesbyspecies=1");
        }

        sbProcessUrl.append("&movingaveragesize=").append(ma);

        String areaSqKm;
        if (sa.getMapLayer() != null && sa.getMapLayer().getAreaSqKm() != null) {
            areaSqKm = sa.getMapLayer().getAreaSqKm();
        } else {
            areaSqKm = String.format("%,.2f", Util.calculateArea(sa.getWkt()) / 1000000.0);
        }
        sbProcessUrl.append("&areasqkm=").append(areaSqKm);


        HttpClient client = new HttpClient();
        PostMethod get = new PostMethod(sbProcessUrl.toString());

        String area;
        if (sa.getMapLayer() != null && sa.getMapLayer().getEnvelope() != null) {
            area = StringConstants.ENVELOPE + "(" + sa.getMapLayer().getEnvelope() + ")";
        } else {
            area = sa.getWkt();
        }
        if (getSelectedArea() != null) {
            get.addParameter(StringConstants.AREA, area);
        }
        get.addParameter("qname", query.getName());

        get.addRequestHeader(StringConstants.ACCEPT, StringConstants.TEXT_PLAIN);

        client.executeMethod(get);

        if (!isEstimate) {
            try {
                String extras = "";
                extras += "gridsize=" + gridResolution;
                extras += "|occurrencedensity=1";
                extras += "|speciesdensity=1";
                extras += "|sitesbyspecies=1";
                extras += "|movingaveragesize=" + ma;

                if (query instanceof BiocacheQuery) {
                    BiocacheQuery bq = (BiocacheQuery) query;
                    extras = bq.getWS() + "|" + bq.getBS() + "|" + bq.getFullQ(false) + "|" + extras;
                    remoteLogger.logMapAnalysis("species to grid", StringConstants.TOOL_SPECIES_TO_GRID, area, bq.getLsids(), "", pid, extras, StringConstants.STARTED);
                } else if (query instanceof UserDataQuery) {
                    remoteLogger.logMapAnalysis("species to grid", StringConstants.TOOL_SPECIES_TO_GRID, area, query.getQ(), "", pid, extras, StringConstants.STARTED);
                } else {
                    remoteLogger.logMapAnalysis("species to grid", StringConstants.TOOL_SPECIES_TO_GRID, area, "", "", pid, extras, StringConstants.STARTED);
                }
            } catch (Exception e) {
                LOGGER.error("error logging", e);
            }
        }

        return get.getResponseBodyAsString();
    }

    @Override
    public long getEstimate() {
        try {
            String estimate = run(true);

            if (estimate == null) {
                return -1;
            }

            return Long.valueOf(estimate);

        } catch (Exception e) {
            LOGGER.error("Unable to get estimates", e);
        }

        return -1;
    }

    public boolean runsitesbyspecies() {
        try {
            pid = run(false);

            if (pid == null) {
                return false;
            }

            openProgressBar();

            this.setVisible(false);

            return true;

        } catch (Exception e) {

            LOGGER.error("SitesBySpecies error: ", e);
            getMapComposer().showMessage("Unknown error.", this);
        }
        return false;
    }

    void loadLayer(String type, String name, int typeId) {
        String mapurl = CommonData.getGeoServer() + "/wms?service=WMS&version=1.1.0&request=GetMap&layers=ALA:" + type + "_" + pid
                + "&styles=" + type + "_" + pid + "&FORMAT=image%2Fpng";
        String legendurl = CommonData.getGeoServer()
                + "/wms?REQUEST=GetLegendGraphic&VERSION=1.0.0&FORMAT=image/png&WIDTH=10&HEIGHT=1"
                + "&LAYER=ALA:" + type + "_" + pid
                + "&STYLE=" + type + "_" + pid;

        LOGGER.debug(legendurl);

        String layername = getMapComposer().getNextAreaLayerName(name);
        getMapComposer().addWMSLayer(pid + "_" + type, layername, mapurl, (float) 0.5, null, legendurl, typeId, null, null);
        MapLayer ml = getMapComposer().getMapLayer(pid + "_" + type);
        ml.setPid(pid + "_" + type);
        String infoUrl = CommonData.getSatServer() + "/output/sitesbyspecies/" + pid + "/" + type + "_metadata.html";
        MapLayerMetadata md = ml.getMapLayerMetadata();

        md.setMoreInfo(infoUrl + "\n" + name + "\npid:" + pid);
        md.setId(Long.valueOf(pid));
    }

    public void loadMap(Event event) {
        try {
            if (chkOccurrenceDensity.isChecked()) {
                loadLayer("odensity", StringConstants.OCCURRENCE_DENSITY, LayerUtilitiesImpl.ODENSITY);
            }

            if (chkSpeciesDensity.isChecked()) {
                loadLayer("srichness", StringConstants.SPECIES_RICHNESS, LayerUtilitiesImpl.SRICHNESS);
            }

            // set off the download as well
            String fileUrl = CommonData.getSatServer() + "/ws/download/" + pid;
            Filedownload.save(new URL(fileUrl).openStream(), "application/zip", "sites_by_species.zip");
        } catch (Exception ex) {
            LOGGER.error("Error generating download for prediction model:", ex);
        }

        this.detach();
    }

    void openProgressBar() {
        ProgressController window = (ProgressController) Executions.createComponents("WEB-INF/zul/progress/AnalysisProgress.zul", getMapComposer(), null);
        window.setParentWindow(this);
        window.start(pid, "Points to Grid");
        try {
            window.setParent(getMapComposer());
            window.doModal();
        } catch (Exception e) {
            LOGGER.error("error opening AnalysisProgress.zul for points to grid: " + pid, e);
        }
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
                dResolution.setFocus(true);
                break;
            default:
                LOGGER.error("invalid step for SitesBySpeciesComposer: " + currentStep);
        }
    }
}
