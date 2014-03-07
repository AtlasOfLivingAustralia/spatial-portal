package au.org.ala.spatial.composer.tool;

import au.com.bytecode.opencsv.CSVReader;
import au.org.ala.spatial.composer.progress.ProgressController;
import au.org.ala.spatial.data.*;
import au.org.ala.spatial.exception.NoSpeciesFoundException;
import au.org.ala.spatial.userpoints.UserPointsUtil;
import au.org.ala.spatial.util.CommonData;
import au.org.ala.spatial.util.SelectedArea;
import au.org.ala.spatial.util.Util;
import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.menu.MapLayerMetadata;
import au.org.emii.portal.util.LayerUtilities;
import org.ala.layers.intersect.SimpleRegion;
import org.ala.layers.intersect.SimpleShapeFile;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.log4j.Logger;
import org.zkoss.zhtml.Filedownload;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Combobox;

import java.io.StringReader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * @author ajay
 */
public class SitesBySpeciesComposer extends ToolComposer {
    private static Logger logger = Logger.getLogger(SitesBySpeciesComposer.class);

    Checkbox chkOccurrenceDensity;
    Checkbox chkSpeciesDensity;
    Checkbox chkSitesBySpecies;
    Combobox cbMovingAverageSize;

    @Override
    public void afterCompose() {
        super.afterCompose();

        this.selectedMethod = "Points To Grid";
        this.totalSteps = 3;

        this.loadAreaLayers();
        this.loadSpeciesLayers(true);
        this.updateWindowTitle();

    }

    @Override
    public void onLastPanel() {
        logger.debug("**** On last step ****");
        super.onLastPanel();
        //this.updateName("My Prediction model for " + rgSpecies.getSelectedItem().getLabel());
        //this.updateName(getMapComposer().getNextAreaLayerName("Sites By Species"));
    }

    @Override
    public boolean onFinish() {
        //super.onFinish();

        if (!hasEstimated && !isUserLoggedIn()) {
            checkEstimate();
            return false;
        }

        Query query = getSelectedSpecies();
        if (query == null) {
            getMapComposer().showMessage("There is a problem selecting the species.  Try to select the species again", this);
            return false;
        }

        if (searchSpeciesACComp.hasValidItemSelected()) {
            getMapComposer().mapSpeciesFromAutocompleteComponent(searchSpeciesACComp, getSelectedArea(), getGeospatialKosher());
        } else if (query != null && rgSpecies.getSelectedItem() != null && rgSpecies.getSelectedItem().getValue().equals("multiple")) {
            getMapComposer().mapSpecies(query, "Species assemblage", "species", 0, LayerUtilities.SPECIES, null, -1, MapComposer.DEFAULT_POINT_SIZE, MapComposer.DEFAULT_POINT_OPACITY, Util.nextColour());
        }

        return runsitesbyspecies();
    }

    Query query = null;
    SelectedArea sa = null;

    private void setupData() throws Exception {
        if (query == null) {
            sa = getSelectedArea();
            query = QueryUtil.queryFromSelectedArea(getSelectedSpecies(), sa, false, getGeospatialKosher());
        }
    }

    @Override
    public long getEstimate() {
        try {

            setupData();

            String ma = "9";
            if (cbMovingAverageSize.getSelectedItem() == null) {
                String txt = cbMovingAverageSize.getValue();
                for (int i = 0; i < cbMovingAverageSize.getItemCount(); i++) {
                    if (txt != null && txt.equalsIgnoreCase(cbMovingAverageSize.getItemAtIndex(i).getLabel())) {
                        ma = (String) cbMovingAverageSize.getItemAtIndex(i).getValue();
                        break;
                    }
                }
            } else {
                ma = (String) cbMovingAverageSize.getSelectedItem().getValue();
            }
            int movingAverageSize = Integer.parseInt(ma);
            if (movingAverageSize % 2 == 0 || movingAverageSize <= 0 || movingAverageSize >= 16) {
                getMapComposer().showMessage("Moving average size " + movingAverageSize + " is not valid.  Must be odd and between 1 and 15.", this);
                return -1;
            }

            if (!chkOccurrenceDensity.isChecked()
                    && !chkSitesBySpecies.isChecked()
                    && !chkSpeciesDensity.isChecked()) {
                getMapComposer().showMessage("Must select at least one output; Sites by species, Occurrence density or Species richness.", this);
                return -1;
            }

            Double gridResolution = dResolution.getValue();
            //SelectedArea sa = getSelectedArea();
            SimpleRegion sr = SimpleShapeFile.parseWKT(sa.getWkt());
            //query = QueryUtil.queryFromSelectedArea(getSelectedSpecies(), sa, false, getGeospatialKosher());
            int occurrenceCount = query.getOccurrenceCount();
            int boundingboxcellcount = (int) ((sr.getBoundingBox()[1][0] - sr.getBoundingBox()[0][0])
                    * (sr.getBoundingBox()[1][1] - sr.getBoundingBox()[0][1])
                    / (gridResolution * gridResolution));

            logger.debug("SitesBySpecies for " + occurrenceCount + " occurrences in up to " + boundingboxcellcount + " grid cells.");

            if (boundingboxcellcount > settingsSupplementary.getValueAsInt("sitesbyspecies_maxbbcells")) {
                //getMapComposer().showMessage("Too many potential output grid cells.  Reduce by at least " + String.format("%.2f",100* (1-boundingboxcellcount / (double)settingsSupplementary.getValueAsInt("sitesbyspecies_maxbbcells"))) + "% by decreasing area or increasing resolution.", this);
                getMapComposer().showMessage("Too many output grid cells: Decrease area or increase grid size.", this);
                return -1;
            }

            if (occurrenceCount > settingsSupplementary.getValueAsInt("sitesbyspecies_maxoccurrences")) {
                getMapComposer().showMessage("Too many occurrences for the selected species in this area.  " + occurrenceCount + " occurrences found, must be less than " + settingsSupplementary.getValueAsInt("sitesbyspecies_maxoccurrences"), this);
                return -1;
            }

            StringBuilder sbProcessUrl = new StringBuilder();
            sbProcessUrl.append(CommonData.satServer + "/ws/sitesbyspecies/estimate?");

            sbProcessUrl.append("speciesq=").append(URLEncoder.encode(QueryUtil.queryFromSelectedArea(query, sa, false, getGeospatialKosher()).getQ(), "UTF-8"));

            sbProcessUrl.append("&gridsize=" + URLEncoder.encode(String.valueOf(gridResolution), "UTF-8"));
            sbProcessUrl.append("&bs=" + URLEncoder.encode(((BiocacheQuery) query).getBS(), "UTF-8"));

            if (chkOccurrenceDensity.isChecked()) {
                sbProcessUrl.append("&occurrencedensity=1");
            }
            if (chkSpeciesDensity.isChecked()) {
                sbProcessUrl.append("&speciesdensity=1");
            }
            if (chkSitesBySpecies.isChecked()) {
                sbProcessUrl.append("&sitesbyspecies=1");
            }

            sbProcessUrl.append("&movingaveragesize=" + ma);

            String areaSqKm = "0";
            if (sa.getMapLayer() != null && sa.getMapLayer().getAreaSqKm() != null) {
                areaSqKm = (String) sa.getMapLayer().getAreaSqKm();
            } else {
                areaSqKm = String.format("%,.2f", Util.calculateArea(sa.getWkt()) / 1000000.0);
            }
            sbProcessUrl.append("&areasqkm=" + areaSqKm);


            HttpClient client = new HttpClient();
            PostMethod get = new PostMethod(sbProcessUrl.toString());

            String area = null;
            if (sa.getMapLayer() != null && sa.getMapLayer().getEnvelope() != null) {
                area = "ENVELOPE(" + (String) sa.getMapLayer().getEnvelope() + ")";
            } else {
                area = sa.getWkt();
            }
            if (getSelectedArea() != null) {
                get.addParameter("area", area);
            }
            get.addParameter("qname", query.getName());

            get.addRequestHeader("Accept", "text/plain");

            int result = client.executeMethod(get);
            String estimate = get.getResponseBodyAsString();

            return Long.valueOf(estimate);

        } catch (Exception e) {
            logger.debug("Unable to get estimates");
            e.printStackTrace(System.out);
        }

        return -1;
    }

    public boolean runsitesbyspecies() {
        try {

            setupData();

            String ma = "9";
            if (cbMovingAverageSize.getSelectedItem() == null) {
                String txt = cbMovingAverageSize.getValue();
                for (int i = 0; i < cbMovingAverageSize.getItemCount(); i++) {
                    if (txt != null && txt.equalsIgnoreCase(cbMovingAverageSize.getItemAtIndex(i).getLabel())) {
                        ma = (String) cbMovingAverageSize.getItemAtIndex(i).getValue();
                        break;
                    }
                }
            } else {
                ma = (String) cbMovingAverageSize.getSelectedItem().getValue();
            }
            int movingAverageSize = Integer.parseInt(ma);
            if (movingAverageSize % 2 == 0 || movingAverageSize <= 0 || movingAverageSize >= 16) {
                getMapComposer().showMessage("Moving average size " + movingAverageSize + " is not valid.  Must be odd and between 1 and 15.", this);
                return false;
            }

            if (!chkOccurrenceDensity.isChecked()
                    && !chkSitesBySpecies.isChecked()
                    && !chkSpeciesDensity.isChecked()) {
                getMapComposer().showMessage("Must select at least one output; Sites by species, Occurrence density or Species richness.", this);
                return false;
            }

            Double gridResolution = dResolution.getValue();
            //SelectedArea sa = getSelectedArea();
            SimpleRegion sr = SimpleShapeFile.parseWKT(sa.getWkt());
            //Query query = QueryUtil.queryFromSelectedArea(getSelectedSpecies(), sa, false, getGeospatialKosher());
            int occurrenceCount = query.getOccurrenceCount();
            int boundingboxcellcount = (int) ((sr.getBoundingBox()[1][0] - sr.getBoundingBox()[0][0])
                    * (sr.getBoundingBox()[1][1] - sr.getBoundingBox()[0][1])
                    / (gridResolution * gridResolution));

            logger.debug("SitesBySpecies for " + occurrenceCount + " occurrences in up to " + boundingboxcellcount + " grid cells.");

            if (boundingboxcellcount > settingsSupplementary.getValueAsInt("sitesbyspecies_maxbbcells")) {
                //getMapComposer().showMessage("Too many potential output grid cells.  Reduce by at least " + String.format("%.2f",100* (1-boundingboxcellcount / (double)settingsSupplementary.getValueAsInt("sitesbyspecies_maxbbcells"))) + "% by decreasing area or increasing resolution.", this);
                getMapComposer().showMessage("Too many output grid cells: Decrease area or increase grid size.", this);
                return false;
            }

            if (occurrenceCount > settingsSupplementary.getValueAsInt("sitesbyspecies_maxoccurrences")) {
                getMapComposer().showMessage("Too many occurrences for the selected species in this area.  " + occurrenceCount + " occurrences found, must be less than " + settingsSupplementary.getValueAsInt("sitesbyspecies_maxoccurrences"), this);
                return false;
            }

            StringBuilder sbProcessUrl = new StringBuilder();
            sbProcessUrl.append(CommonData.satServer + "/ws/sitesbyspecies?");

            sbProcessUrl.append("speciesq=").append(URLEncoder.encode(QueryUtil.queryFromSelectedArea(query, sa, false, getGeospatialKosher()).getQ(), "UTF-8"));

            sbProcessUrl.append("&gridsize=" + URLEncoder.encode(String.valueOf(gridResolution), "UTF-8"));
            sbProcessUrl.append("&bs=" + URLEncoder.encode(((BiocacheQuery) query).getBS(), "UTF-8"));

            if (chkOccurrenceDensity.isChecked()) {
                sbProcessUrl.append("&occurrencedensity=1");
            }
            if (chkSpeciesDensity.isChecked()) {
                sbProcessUrl.append("&speciesdensity=1");
            }
            if (chkSitesBySpecies.isChecked()) {
                sbProcessUrl.append("&sitesbyspecies=1");
            }

            sbProcessUrl.append("&movingaveragesize=" + ma);

            String areaSqKm = "0";
            if (sa.getMapLayer() != null && sa.getMapLayer().getAreaSqKm() != null) {
                areaSqKm = (String) sa.getMapLayer().getAreaSqKm();
            } else {
                areaSqKm = String.format("%,.2f", Util.calculateArea(sa.getWkt()) / 1000000.0);
            }
            sbProcessUrl.append("&areasqkm=" + areaSqKm);


            HttpClient client = new HttpClient();
            PostMethod get = new PostMethod(sbProcessUrl.toString());

            String area = null;
            if (sa.getMapLayer() != null && sa.getMapLayer().getEnvelope() != null) {
                area = "ENVELOPE(" + (String) sa.getMapLayer().getEnvelope() + ")";
            } else {
                area = sa.getWkt();
            }
            if (getSelectedArea() != null) {
                get.addParameter("area", area);
            }
            get.addParameter("qname", query.getName());

            get.addRequestHeader("Accept", "text/plain");

            int result = client.executeMethod(get);
            pid = get.getResponseBodyAsString();

            openProgressBar();

            StringBuilder sbParams = new StringBuilder();

            this.setVisible(false);

            try {
                String extras = "";
                extras += "gridsize=" + String.valueOf(gridResolution);
                extras += "|occurrencedensity=1";
                extras += "|speciesdensity=1";
                extras += "|sitesbyspecies=1";
                extras += "|movingaveragesize=" + ma;

                if (query instanceof BiocacheQuery) {
                    BiocacheQuery bq = (BiocacheQuery) query;
                    extras = bq.getWS() + "|" + bq.getBS() + "|" + bq.getFullQ(false) + "|" + extras;
                    remoteLogger.logMapAnalysis("species to grid", "Tool - Species to Grid", area, bq.getLsids(), "", pid, extras, "STARTED");
                } else if (query instanceof UploadQuery) {
                    remoteLogger.logMapAnalysis("species to grid", "Tool - Species to Grid", area, ((UploadQuery) query).getQ(), "", pid, extras, "STARTED");
                } else {
                    remoteLogger.logMapAnalysis("species to grid", "Tool - Species to Grid", area, "", "", pid, extras, "STARTED");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            return true;
        } catch (Exception e) {
            logger.debug("SitesBySpecies error: ");
            e.printStackTrace(System.out);
            getMapComposer().showMessage("Unknown error.", this);
        }
        return false;
    }

    @Override
    public void loadMap(Event event) {
        try {
            if (chkOccurrenceDensity.isChecked()) {
                String mapurl = CommonData.geoServer + "/wms?service=WMS&version=1.1.0&request=GetMap&layers=ALA:odensity_" + pid + "&styles=odensity_" + pid + "&FORMAT=image%2Fpng";
                String legendurl = CommonData.geoServer
                        + "/wms?REQUEST=GetLegendGraphic&VERSION=1.0.0&FORMAT=image/png&WIDTH=10&HEIGHT=1"
                        + "&LAYER=ALA:odensity_" + pid
                        + "&STYLE=odensity_" + pid;

                logger.debug(legendurl);

//                String layername = tToolName.getValue();
                String layername = getMapComposer().getNextAreaLayerName("Occurrence Density");
                getMapComposer().addWMSLayer(pid + "_odensity", layername, mapurl, (float) 0.5, null, legendurl, LayerUtilities.ODENSITY, null, null);
                MapLayer ml = getMapComposer().getMapLayer(pid + "_odensity");
                ml.setPid(pid + "_odensity");
                String infoUrl = CommonData.satServer + "/output/sitesbyspecies/" + pid + "/odensity_metadata.html";
                MapLayerMetadata md = ml.getMapLayerMetadata();

                md.setMoreInfo(infoUrl + "\nOccurrence Density\npid:" + pid);
                md.setId(Long.valueOf(pid));

                //perform intersection on user uploaded layers so you can facet on this layer
                UserPointsUtil.addAnalysisLayerToUploadedCoordinates(getPortalSession(), pid + "_odensity", layername);
            }

            if (chkSpeciesDensity.isChecked()) {
                String mapurl = CommonData.geoServer + "/wms?service=WMS&version=1.1.0&request=GetMap&layers=ALA:srichness_" + pid + "&styles=srichness_" + pid + "&FORMAT=image%2Fpng";
                String legendurl = CommonData.geoServer
                        + "/wms?REQUEST=GetLegendGraphic&VERSION=1.0.0&FORMAT=image/png&WIDTH=10&HEIGHT=1"
                        + "&LAYER=ALA:srichness_" + pid
                        + "&STYLE=srichness_" + pid;

                logger.debug(legendurl);

//                String layername = tToolName.getValue();
                String layername = getMapComposer().getNextAreaLayerName("Species Richness");
                getMapComposer().addWMSLayer(pid + "_srichness", layername, mapurl, (float) 0.5, null, legendurl, LayerUtilities.SRICHNESS, null, null);
                MapLayer ml = getMapComposer().getMapLayer(pid + "_srichness");
                ml.setPid(pid + "_srichness");
                String infoUrl = CommonData.satServer + "/output/sitesbyspecies/" + pid + "/srichness_metadata.html";
                MapLayerMetadata md = ml.getMapLayerMetadata();

                md.setMoreInfo(infoUrl + "\nSpecies Richness\npid:" + pid);
                md.setId(Long.valueOf(pid));

                //perform intersection on user uploaded layers so you can facet on this layer
                UserPointsUtil.addAnalysisLayerToUploadedCoordinates(getPortalSession(), pid + "_srichness", layername);
            }

            // set off the download as well
            String fileUrl = CommonData.satServer + "/ws/download/" + pid;
            Filedownload.save(new URL(fileUrl).openStream(), "application/zip", "sites_by_species.zip");
        } catch (Exception ex) {
            logger.debug("Error generating download for prediction model:");
            ex.printStackTrace(System.out);
        }

        this.detach();
    }

    void openProgressBar() {
        ProgressController window = (ProgressController) Executions.createComponents("WEB-INF/zul/progress/AnalysisProgress.zul", getMapComposer(), null);
        window.parent = this;
        window.start(pid, "Points to Grid");
        try {
            window.doModal();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    String getJob(String type) {
        try {
            StringBuilder sbProcessUrl = new StringBuilder();
            sbProcessUrl.append(CommonData.satServer + "/ws/jobs/").append(type).append("?pid=").append(pid);

            logger.debug(sbProcessUrl.toString());
            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(sbProcessUrl.toString());

            get.addRequestHeader("Accept", "text/plain");

            int result = client.executeMethod(get);
            String slist = get.getResponseBodyAsString();
            logger.debug(slist);
            return slist;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * get CSV of speciesName, longitude, latitude in [0] and
     */
    private String[] getSpeciesData(Query query) throws NoSpeciesFoundException {
        if (query instanceof UploadQuery) {
            //no sensitive records in upload
            ArrayList<QueryField> fields = new ArrayList<QueryField>();
            String lsidFieldName = query.getSpeciesIdFieldName();
            QueryField qf = null;
            if (lsidFieldName != null) {
                qf = new QueryField(query.getSpeciesIdFieldName());
                qf.setStored(true);
                fields.add(qf);
            }
            double[] points = query.getPoints(fields);
            StringBuilder sb = null;
            if (points != null) {
                sb = new StringBuilder();
                for (int i = 0; i < points.length; i += 2) {
                    if (sb.length() == 0) {
                        //header
                        sb.append("species,longitude,latitude");
                    }
                    sb.append("\nspecies,").append(points[i]).append(",").append(points[i + 1]);
                }
            }

            String[] out = {((sb == null) ? null : sb.toString()), null};
            return out;
        } else {
            //identify sensitive species records
            List<String[]> sensitiveSpecies = null;
            try {
                String sensitiveSpeciesRaw = new BiocacheQuery(null, null, "sensitive:[* TO *]", null, false, getGeospatialKosher()).speciesList();
                CSVReader csv = new CSVReader(new StringReader(sensitiveSpeciesRaw));
                sensitiveSpecies = csv.readAll();
                csv.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            HashSet<String> sensitiveSpeciesFound = new HashSet<String>();
            HashSet<String> sensitiveLsids = new HashSet<String>();

            //add to 'identified' sensitive list
            try {
                CSVReader csv = new CSVReader(new StringReader(query.speciesList()));
                List<String[]> fullSpeciesList = csv.readAll();
                csv.close();
                for (int i = 0; i < fullSpeciesList.size(); i++) {
                    String[] sa = fullSpeciesList.get(i);
                    for (String[] ss : sensitiveSpecies) {
                        if (sa != null && sa.length > 4
                                && ss != null && ss.length > 4
                                && sa[4].equals(ss[4])) {
                            sensitiveSpeciesFound.add(ss[4] + "," + ss[1] + "," + ss[3]);
                            sensitiveLsids.add(ss[4]);
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            //remove sensitive records that will not be LSID matched
            Query maxentQuery = query.newFacet(new Facet("sensitive", "[* TO *]", false), false);
            ArrayList<QueryField> fields = new ArrayList<QueryField>();
            String lsidFieldName = maxentQuery.getSpeciesIdFieldName();
            QueryField qf = null;
            if (lsidFieldName != null) {
                qf = new QueryField(maxentQuery.getSpeciesIdFieldName());
                qf.setStored(true);
                fields.add(qf);
            }
            double[] points = maxentQuery.getPoints(fields);
            StringBuilder sb = null;
            if (points != null) {
                sb = new StringBuilder();
                for (int i = 0; i < points.length; i += 2) {
                    boolean isSensitive = false;
                    if (qf != null) {
                        String lsid = qf.getAsString(i / 2);
                        isSensitive = sensitiveLsids.contains(lsid);
                    }
                    if (!isSensitive) {
                        if (sb.length() == 0) {
                            //header
                            sb.append("species,longitude,latitude");
                        }
                        sb.append("\nspecies,").append(points[i]).append(",").append(points[i + 1]);
                    }
                }
            }

            //collate sensitive species found, no header
            StringBuilder sen = new StringBuilder();
            for (String s : sensitiveSpeciesFound) {
                sen.append(s).append("\n");
            }

            String[] out = {((sb == null) ? null : sb.toString()), (sen.length() == 0) ? null : sen.toString()};
            return out;
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
        }
    }
}
