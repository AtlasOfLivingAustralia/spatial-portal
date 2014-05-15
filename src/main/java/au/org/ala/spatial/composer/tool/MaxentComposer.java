/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.ala.spatial.composer.tool;

import au.org.ala.spatial.composer.progress.ProgressController;
import au.org.ala.spatial.data.BiocacheQuery;
import au.org.ala.spatial.data.Query;
import au.org.ala.spatial.data.QueryUtil;
import au.org.ala.spatial.util.CommonData;
import au.org.ala.spatial.util.SelectedArea;
import au.org.ala.spatial.util.Util;
import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.menu.MapLayerMetadata;
import au.org.emii.portal.util.LayerUtilities;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.log4j.Logger;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Textbox;

import java.net.URL;
import java.net.URLEncoder;

/**
 * @author ajay
 */
public class MaxentComposer extends ToolComposer {
    private static Logger logger = Logger.getLogger(MaxentComposer.class);
    int generation_count = 1;
    private Checkbox chkJackknife;
    private Checkbox chkRCurves;
    private Textbox txtTestPercentage;
//    private String taxon = "";

    @Override
    public void afterCompose() {
        super.afterCompose();

        this.selectedMethod = "Prediction";
        this.totalSteps = 5;

        this.setIncludeAnalysisLayersForAnyQuery(true);

        this.loadAreaLayers();
        this.loadSpeciesLayers();
        this.loadGridLayers(true, true, true);
        this.updateWindowTitle();

    }

    public void onClick$btnClearSelectionCtx(Event event) {
        // check if lbListLayers is empty as well,
        // if so, then disable the next button
        if (lbListLayers.getSelectedCount() == 0) {
            btnOk.setDisabled(true);
        }
    }

    @Override
    public void onLastPanel() {
        super.onLastPanel();
        //this.updateName("My Prediction model for " + rgSpecies.getSelectedItem().getLabel());
        this.updateName(getMapComposer().getNextAreaLayerName("My Prediction"));
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
        if (searchSpeciesACComp.getAutoComplete().getSelectedItem() != null) {
            getMapComposer().mapSpeciesFromAutocompleteComponent(searchSpeciesACComp, getSelectedArea(), getGeospatialKosher());
        } else if (query != null && rgSpecies.getSelectedItem() != null && rgSpecies.getSelectedItem().getValue().equals("multiple")) {
            getMapComposer().mapSpecies(query, "Species assemblage", "species", 0, LayerUtilities.SPECIES, null, -1, MapComposer.DEFAULT_POINT_SIZE, MapComposer.DEFAULT_POINT_OPACITY, Util.nextColour());
        }

        logger.debug("Maxent Selected layers:");
        logger.debug(getSelectedLayers());

        return runmaxent();
    }

    SelectedArea sa = null;
    Query query = null;
    String sbenvsel = "";
    //String[] speciesData = null;

    private void setupData() throws Exception {
        if (query == null) {
            sa = getSelectedArea();
            query = QueryUtil.queryFromSelectedArea(getSelectedSpecies(), sa, false, getGeospatialKosher());

            sbenvsel = getSelectedLayers();
        }
    }

    @Override
    public long getEstimate() {
        try {
            setupData();

            StringBuffer sbProcessUrl = new StringBuffer();
            sbProcessUrl.append(CommonData.satServer + "/ws/maxent/estimate?");
            sbProcessUrl.append("taxonid=" + URLEncoder.encode(query.getName(), "UTF-8"));
            sbProcessUrl.append("&taxonlsid=" + URLEncoder.encode(query.getQ(), "UTF-8"));
            sbProcessUrl.append("&envlist=" + URLEncoder.encode(sbenvsel.toString(), "UTF-8"));

            sbProcessUrl.append("&speciesq=").append(URLEncoder.encode(QueryUtil.queryFromSelectedArea(query, sa, false, getGeospatialKosher()).getQ(), "UTF-8"));
            sbProcessUrl.append("&bs=" + URLEncoder.encode(((BiocacheQuery) query).getBS(), "UTF-8"));

            if (chkJackknife.isChecked()) {
                sbProcessUrl.append("&chkJackknife=on");
            }
            if (chkRCurves.isChecked()) {
                sbProcessUrl.append("&chkResponseCurves=on");
            }
            sbProcessUrl.append("&txtTestPercentage=" + txtTestPercentage.getValue());

            // logger.debug("Calling Maxent: " + sbProcessUrl.toString() + "\narea: " + area);

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

//            logger.debug("Getting species data");
////            speciesData = getSpeciesData(query);
//            System.out.print("checking for species data...");
//            //check for no data
//            if (speciesData[0] == null || speciesData[0].trim().equals("")) {
//                logger.debug("none available");
//                if (speciesData[1] == null) {
//                    getMapComposer().showMessage("No records available for Prediction", this);
//                } else {
//                    getMapComposer().showMessage("All species and records selected are marked as sensitive", this);
//                }
//                return -1;
//            } else {
//                logger.debug("available");
//            }
//
//            get.addParameter("species", speciesData[0]);
//            if (speciesData[1] != null) {
//                get.addParameter("removedspecies", speciesData[1]);
//            }

            get.addRequestHeader("Accept", "text/plain");

            int result = client.executeMethod(get);
            String estimate = get.getResponseBodyAsString();

            return Long.valueOf(estimate);

        } catch (Exception e) {
            logger.error("Unable to get estimates", e);
        }
        return -1;
    }

    public boolean runmaxent() {
        try {

            setupData();


            //SelectedArea sa = getSelectedArea();
            //Query query = QueryUtil.queryFromSelectedArea(getSelectedSpecies(), sa, false, getGeospatialKosher());

            //String sbenvsel = getSelectedLayers();

            //String area = getSelectedArea();
            //String taxonlsid = taxon;
            if (!searchSpeciesACComp.hasValidItemSelected()) {
                //MapLayer ml = getMapComposer().getMapLayerSpeciesLSID(taxon);
                // taxonlsid = ml.getMapLayerMetadata().getSpeciesDisplayLsid();
            }

//            if (isSensitiveSpecies(taxon)) {
//                return;
//            }

            logger.debug("Selected species: " + query.getName());
            logger.debug("Selected species query: " + query.getQ());
            logger.debug("Selected env vars");
            logger.debug(sbenvsel.toString());
            logger.debug("Selected options: ");
            logger.debug("Jackknife: " + chkJackknife.isChecked());
            logger.debug("Response curves: " + chkRCurves.isChecked());
            logger.debug("Test per: " + txtTestPercentage.getValue());

            StringBuffer sbProcessUrl = new StringBuffer();
            sbProcessUrl.append(CommonData.satServer + "/ws/maxent?");
            sbProcessUrl.append("taxonid=" + URLEncoder.encode(query.getName(), "UTF-8"));
            sbProcessUrl.append("&taxonlsid=" + URLEncoder.encode(query.getQ(), "UTF-8"));
            sbProcessUrl.append("&envlist=" + URLEncoder.encode(sbenvsel.toString(), "UTF-8"));
            sbProcessUrl.append("&speciesq=").append(URLEncoder.encode(QueryUtil.queryFromSelectedArea(query, sa, false, getGeospatialKosher()).getQ(), "UTF-8"));
            sbProcessUrl.append("&bs=" + URLEncoder.encode(((BiocacheQuery) query).getBS(), "UTF-8"));

            if (chkJackknife.isChecked()) {
                sbProcessUrl.append("&chkJackknife=on");
            }
            if (chkRCurves.isChecked()) {
                sbProcessUrl.append("&chkResponseCurves=on");
            }
            sbProcessUrl.append("&txtTestPercentage=" + txtTestPercentage.getValue());

            // logger.debug("Calling Maxent: " + sbProcessUrl.toString() + "\narea: " + area);

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

            logger.debug("Getting species data");
//            //String[] speciesData = getSpeciesData(query);
//            System.out.print("checking for species data...");
//            //check for no data
//            if (speciesData[0] == null || speciesData[0].trim().equals("")) {
//                logger.debug("none available");
//                if (speciesData[1] == null) {
//                    getMapComposer().showMessage("No records available for Prediction", this);
//                } else {
//                    getMapComposer().showMessage("All species and records selected are marked as sensitive", this);
//                }
//                return false;
//            } else {
//                logger.debug("available");
//            }
////            logger.debug("displaying species data: '");
////            logger.debug(speciesData[0]);
////            logger.debug("'");
//
//            get.addParameter("species", speciesData[0]);
//            if (speciesData[1] != null) {
//                get.addParameter("removedspecies", speciesData[1]);
//            }

            get.addRequestHeader("Accept", "text/plain");

            int result = client.executeMethod(get);
            pid = get.getResponseBodyAsString();
//            this.taxon = taxon;

            openProgressBar();

            try {
                StringBuffer sbParams = new StringBuffer();
                sbParams.append("Species: " + query.getName());
                sbParams.append(";Query: " + query.getFullQ(false));
                sbParams.append(";Jackknife: " + chkJackknife.isChecked());
                sbParams.append(";Response curves: " + chkRCurves.isChecked());
                sbParams.append(";Test per: " + txtTestPercentage.getValue());

                String options = "";
                options += "Jackknife: " + chkJackknife.isChecked();
                options += ";Response curves: " + chkRCurves.isChecked();
                options += ";Test per: " + txtTestPercentage.getValue();
                if (query instanceof BiocacheQuery) {
                    BiocacheQuery bq = (BiocacheQuery) query;
                    options = bq.getWS() + "|" + bq.getBS() + "|" + bq.getFullQ(false) + "|" + options;
                    remoteLogger.logMapAnalysis(tToolName.getValue(), "Tool - Prediction", area, bq.getLsids(), sbenvsel.toString(), pid, options, "STARTED");
                } else {
                    remoteLogger.logMapAnalysis(tToolName.getValue(), "Tool - Prediction", area, query.getName() + "__" + query.getQ(), sbenvsel.toString(), pid, options, "STARTED");
                }
            } catch (Exception e) {
                logger.error("error requesting maxent", e);
            }

            this.setVisible(false);

            return true;
//        } catch (NoSpeciesFoundException e) {
//            logger.debug("Maxent error: NoSpeciesFoundException");
//            e.printStackTrace(System.out);
//            getMapComposer().showMessage("No species occurrences found in the current area. \nPlease select a larger area and re-run the analysis", this);
        } catch (Exception e) {
            logger.error("Maxent error: ", e);
            getMapComposer().showMessage("Unknown error.", this);
        }
        return false;
    }

    void openProgressBar() {
        ProgressController window = (ProgressController) Executions.createComponents("WEB-INF/zul/progress/AnalysisProgress.zul", getMapComposer(), null);
        window.parent = this;
        window.start(pid, "Prediction", isBackgroundProcess);
        try {
            window.doModal();
        } catch (Exception e) {
            logger.error("error opening AnaysisProgress.zul for Prediction", e);
        }
    }

    public void loadMap(Event event) {

        String mapurl = CommonData.geoServer + "/wms?service=WMS&version=1.1.0&request=GetMap&layers=ALA:species_" + pid + "&styles=alastyles&FORMAT=image%2Fpng";

        String legendurl = CommonData.geoServer
                + "/wms?REQUEST=GetLegendGraphic&VERSION=1.0.0&FORMAT=image/png&WIDTH=10&HEIGHT=1"
                + "&LAYER=ALA:species_" + pid
                + "&STYLE=alastyles";

        logger.debug(legendurl);

        //get job inputs
        String speciesName = "";
        try {
            for (String s : getJob().split(";")) {
                if (s.startsWith("scientificName")) {
                    speciesName = s.split(":")[1];
                    if (speciesName != null && speciesName.length() > 1) {
                        speciesName = speciesName.substring(0, 1).toUpperCase() + speciesName.substring(1);
                    }
                    break;
                }
            }
        } catch (Exception e) {
            logger.error("error getting scientificName", e);
        }

        String layername = tToolName.getValue();
        getMapComposer().addWMSLayer("species_" + pid, layername, mapurl, (float) 0.5, null, legendurl, LayerUtilities.MAXENT, null, null);
        MapLayer ml = getMapComposer().getMapLayer("species_" + pid);
        ml.setPid(pid);
        String infoUrl = CommonData.satServer + "/output/maxent/" + pid + "/species.html";
        MapLayerMetadata md = ml.getMapLayerMetadata();

        md.setMoreInfo(infoUrl + "\nMaxent Output\npid:" + pid);
        md.setId(Long.valueOf(pid));

        try {
            // set off the download as well
            String fileUrl = CommonData.satServer + "/ws/download/" + pid;
            Filedownload.save(new URL(fileUrl).openStream(), "application/zip", tToolName.getValue().replaceAll(" ", "_") + ".zip"); // "ALA_Prediction_"+pid+".zip"
        } catch (Exception e) {
            logger.error("Error generating download for prediction model:", e);
        }

        this.detach();
    }

    String getJob() {
        try {
            StringBuffer sbProcessUrl = new StringBuffer();
            sbProcessUrl.append(CommonData.satServer + "/ws/jobs/").append("inputs").append("?pid=").append(pid);

            logger.debug(sbProcessUrl.toString());
            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(sbProcessUrl.toString());

            get.addRequestHeader("Accept", "text/plain");

            int result = client.executeMethod(get);
            String slist = get.getResponseBodyAsString();
            logger.debug(slist);
            return slist;
        } catch (Exception e) {
            logger.error("error getting job info pid=" + pid, e);
        }
        return "";
    }

//    /**
//     * get CSV of speciesName, longitude, latitude in [0] and
//     *
//     * @param selectedSpecies
//     * @param area
//     * @return
//     */
//    private String[] getSpeciesData(Query query) throws NoSpeciesFoundException {
//        if (query instanceof UploadQuery) {
//            //no sensitive records in upload
//            ArrayList<QueryField> fields = new ArrayList<QueryField>();
//            String lsidFieldName = query.getSpeciesIdFieldName();
//            QueryField qf = null;
//            if (lsidFieldName != null) {
//                qf = new QueryField(query.getSpeciesIdFieldName());
//                qf.setStored(true);
//                fields.add(qf);
//            }
//            double[] points = query.getPoints(fields);
//            StringBuilder sb = null;
//            if (points != null) {
//                sb = new StringBuilder();
//                for (int i = 0; i < points.length; i += 2) {
//                    if (sb.length() == 0) {
//                        //header
//                        sb.append("species,longitude,latitude");
//                    }
//                    sb.append("\nspecies,").append(points[i]).append(",").append(points[i + 1]);
//                }
//            }
//
//            String[] out = {((sb == null) ? null : sb.toString()), null};
//            return out;
//        } else {
//            //identify sensitive species records
//            List<String[]> sensitiveSpecies = null;
//            try {
//                String sensitiveSpeciesRaw = new BiocacheQuery(null, null, "sensitive:[* TO *]", null, false, null).speciesList();
//                CSVReader csv = new CSVReader(new StringReader(sensitiveSpeciesRaw));
//                sensitiveSpecies = csv.readAll();
//                csv.close();
//            } catch (Exception e) {
//                logger.error("error getting sensitive species list", e);
//            }
//            HashSet<String> sensitiveSpeciesFound = new HashSet<String>();
//            HashSet<String> sensitiveLsids = new HashSet<String>();
//
//            //add to 'identified' sensitive list
//            try {
//                CSVReader csv = new CSVReader(new StringReader(query.speciesList()));
//                List<String[]> fullSpeciesList = csv.readAll();
//                csv.close();
//                for (int i = 0; i < fullSpeciesList.size(); i++) {
//                    String[] sa = fullSpeciesList.get(i);
//                    for (String[] ss : sensitiveSpecies) {
//                        if (sa != null && sa.length > 4
//                                && ss != null && ss.length > 4
//                                && sa[4].equals(ss[4])) {
//                            sensitiveSpeciesFound.add(ss[4] + "," + ss[1] + "," + ss[3]);
//                            sensitiveLsids.add(ss[4]);
//                            break;
//                        }
//                    }
//                }
//            } catch (Exception e) {
//                logger.error("error matching sensitive species list", e);
//            }
//
//            //remove sensitive records that will not be LSID matched
//            Query maxentQuery = query.newFacet(new Facet("sensitive", "[* TO *]", false), false);
//            ArrayList<QueryField> fields = new ArrayList<QueryField>();
//            String lsidFieldName = maxentQuery.getSpeciesIdFieldName();
//            QueryField qf = null;
//            if (lsidFieldName != null) {
//                qf = new QueryField(maxentQuery.getSpeciesIdFieldName());
//                qf.setStored(true);
//                fields.add(qf);
//            }
//            double[] points = maxentQuery.getPoints(fields);
//            StringBuilder sb = null;
//            if (points != null) {
//                sb = new StringBuilder();
//                for (int i = 0; i < points.length; i += 2) {
//                    boolean isSensitive = false;
//                    if (qf != null) {
//                        String lsid = qf.getAsString(i / 2);
//                        isSensitive = sensitiveLsids.contains(lsid);
//                    }
//                    if (!isSensitive) {
//                        if (sb.length() == 0) {
//                            //header
//                            sb.append("species,longitude,latitude");
//                        }
//                        sb.append("\nspecies,").append(points[i]).append(",").append(points[i + 1]);
//                    }
//                }
//            }
//
//            //collate sensitive species found, no header
//            StringBuilder sen = new StringBuilder();
//            for (String s : sensitiveSpeciesFound) {
//                sen.append(s).append("\n");
//            }
//
//            String[] out = {((sb == null) ? null : sb.toString()), (sen.length() == 0) ? null : sen.toString()};
//            return out;
//        }
//    }

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
                break;
            case 4:
                chkJackknife.setFocus(true);
                break;
            case 5:
                tToolName.setFocus(true);
                break;
        }
    }
}
