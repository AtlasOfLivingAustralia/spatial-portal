/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.analysis.web;

import au.com.bytecode.opencsv.CSVReader;
import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.menu.MapLayerMetadata;
import au.org.emii.portal.util.LayerUtilities;
import java.io.StringReader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.ala.spatial.data.Facet;
import org.ala.spatial.data.Query;
import org.ala.spatial.util.CommonData;
import org.ala.spatial.data.QueryField;
import org.ala.spatial.data.QueryUtil;
import org.ala.spatial.data.BiocacheQuery;
import org.ala.spatial.data.UploadQuery;
import org.ala.spatial.util.SelectedArea;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Textbox;

/**
 *
 * @author ajay
 */
public class AddToolMaxentComposer extends AddToolComposer {

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

        Query query = getSelectedSpecies();
        if(query == null) {
            getMapComposer().showMessage("There is a problem selecting the species.  Try to select the species again", this);
            return false;
        }
        if (searchSpeciesAuto.getSelectedItem() != null) {
            getMapComposer().mapSpeciesFromAutocomplete(searchSpeciesAuto, getSelectedArea());
        } else if(query != null && rgSpecies.getSelectedItem() != null && rgSpecies.getSelectedItem().getValue().equals("multiple")) {
            getMapComposer().mapSpecies(query, "Species assemblage", "species", 0, LayerUtilities.SPECIES, null, -1, MapComposer.DEFAULT_POINT_SIZE, MapComposer.DEFAULT_POINT_OPACITY, MapComposer.nextColour());
        }

        System.out.println("Maxent Selected layers:");
        System.out.println(getSelectedLayers());

        return runmaxent();
    }

    public boolean runmaxent() {
        try {
            SelectedArea sa = getSelectedArea();
            Query query = QueryUtil.queryFromSelectedArea(getSelectedSpecies(), sa, false);

            String sbenvsel = getSelectedLayers();

            //String area = getSelectedArea();
            //String taxonlsid = taxon;
            if (searchSpeciesAuto.getSelectedItem() == null
                    || searchSpeciesAuto.getSelectedItem().getValue() == null) {
                //MapLayer ml = getMapComposer().getMapLayerSpeciesLSID(taxon);
                // taxonlsid = ml.getMapLayerMetadata().getSpeciesDisplayLsid();
            }

//            if (isSensitiveSpecies(taxon)) {
//                return;
//            }

            System.out.println("Selected species: " + query.getName());
            System.out.println("Selected species query: " + query.getQ());
            System.out.println("Selected env vars");
            System.out.println(sbenvsel.toString());
            System.out.println("Selected options: ");
            System.out.println("Jackknife: " + chkJackknife.isChecked());
            System.out.println("Response curves: " + chkRCurves.isChecked());
            System.out.println("Test per: " + txtTestPercentage.getValue());

            StringBuffer sbProcessUrl = new StringBuffer();
            sbProcessUrl.append(CommonData.satServer + "/ws/maxent?");
            sbProcessUrl.append("taxonid=" + URLEncoder.encode(query.getName(), "UTF-8"));
            sbProcessUrl.append("&taxonlsid=" + URLEncoder.encode(query.getQ(), "UTF-8"));
            sbProcessUrl.append("&envlist=" + URLEncoder.encode(sbenvsel.toString(), "UTF-8"));
            if (chkJackknife.isChecked()) {
                sbProcessUrl.append("&chkJackknife=on");
            }
            if (chkRCurves.isChecked()) {
                sbProcessUrl.append("&chkResponseCurves=on");
            }
            sbProcessUrl.append("&txtTestPercentage=" + txtTestPercentage.getValue());

            // System.out.println("Calling Maxent: " + sbProcessUrl.toString() + "\narea: " + area);

            HttpClient client = new HttpClient();
            PostMethod get = new PostMethod(sbProcessUrl.toString());

            String area = null;
            if (sa.getMapLayer() != null && sa.getMapLayer().getData("envelope") != null) {
                area = "ENVELOPE(" + (String) sa.getMapLayer().getData("envelope") + ")";
            } else {
                area = sa.getWkt();
            }
            if (getSelectedArea() != null) {
                get.addParameter("area", area);
            }

            System.out.println("Getting species data");
            String[] speciesData = getSpeciesData(query);
            System.out.print("checking for species data...");
            //check for no data
            if (speciesData[0] == null || speciesData[0].trim().equals("")) {
                System.out.println("none available");
                if (speciesData[1] == null) {
                    getMapComposer().showMessage("No records available for Prediction", this);
                } else {
                    getMapComposer().showMessage("All species and records selected are marked as sensitive", this);
                }
                return false;
            } else {
                System.out.println("available");
            }
//            System.out.println("displaying species data: '");
//            System.out.println(speciesData[0]);
//            System.out.println("'");

            get.addParameter("species", speciesData[0]);
            if (speciesData[1] != null) {
                get.addParameter("removedspecies", speciesData[1]);
            }

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

                Map attrs = new HashMap();
                attrs.put("actionby", "user");
                attrs.put("actiontype", "analysis");
                //attrs.put("lsid", taxonlsid);
                attrs.put("useremail", "spatialuser");
                attrs.put("processid", pid);
                attrs.put("sessionid", "");
                attrs.put("layers", sbenvsel.toString());
                attrs.put("method", "maxent");
                attrs.put("params", sbParams.toString());
                attrs.put("downloadfile", "");
                getMapComposer().updateUserLog(attrs, "analysis result: " + CommonData.satServer + "/output/maxent/" + pid + "/species.html");
                String options = "";
                options += "Jackknife: " + chkJackknife.isChecked();
                options += ";Response curves: " + chkRCurves.isChecked();
                options += ";Test per: " + txtTestPercentage.getValue();
                if (query instanceof BiocacheQuery) {
                    BiocacheQuery bq = (BiocacheQuery) query;
                    options = bq.getWS() + "|" + bq.getBS() + "|" + bq.getFullQ(false) + "|" + options;
                    remoteLogger.logMapAnalysis(tToolName.getValue(), "Tool - Prediction", area, bq.getLsids(), sbenvsel.toString(), pid, options, "STARTED");
                } else {
                    remoteLogger.logMapAnalysis(tToolName.getValue(), "Tool - Prediction", area, query.getName()+"__"+query.getQ(), sbenvsel.toString(), pid, options, "STARTED");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            this.setVisible(false);

            return true;
        } catch (Exception e) {
            System.out.println("Maxent error: ");
            e.printStackTrace(System.out);
            getMapComposer().showMessage("Unknown error.", this);
        }
        return false;
    }

    void openProgressBar() {
        MaxentProgressWCController window = (MaxentProgressWCController) Executions.createComponents("WEB-INF/zul/AnalysisMaxentProgress.zul", null, null);
        window.parent = this;
        window.start(pid);
        try {
            window.doModal();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadMap(Event event) {

        String mapurl = CommonData.geoServer + "/wms?service=WMS&version=1.1.0&request=GetMap&layers=ALA:species_" + pid + "&styles=alastyles&FORMAT=image%2Fpng";

        String legendurl = CommonData.geoServer
                + "/wms?REQUEST=GetLegendGraphic&VERSION=1.0.0&FORMAT=image/png&WIDTH=10&HEIGHT=1"
                + "&LAYER=ALA:species_" + pid
                + "&STYLE=alastyles";

        System.out.println(legendurl);

        //get job inputs
        String speciesName = "";
        try {
            for (String s : getJob("inputs").split(";")) {
                if (s.startsWith("scientificName")) {
                    speciesName = s.split(":")[1];
                    if (speciesName != null && speciesName.length() > 1) {
                        speciesName = speciesName.substring(0, 1).toUpperCase() + speciesName.substring(1);
                    }
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        String layername = tToolName.getValue();
        getMapComposer().addWMSLayer(pid, layername, mapurl, (float) 0.5, null, legendurl, LayerUtilities.MAXENT, null, null);
        MapLayer ml = getMapComposer().getMapLayer(pid);
        ml.setData("pid", pid);
        String infoUrl = CommonData.satServer + "/output/maxent/" + pid + "/species.html";
        MapLayerMetadata md = ml.getMapLayerMetadata();
        if (md == null) {
            md = new MapLayerMetadata();
            ml.setMapLayerMetadata(md);
        }
        md.setMoreInfo(infoUrl + "\nMaxent Output\npid:" + pid);
        md.setId(Long.valueOf(pid));

        try {
            // set off the download as well
            String fileUrl = CommonData.satServer + "/ws/download/" + pid;
            Filedownload.save(new URL(fileUrl).openStream(), "application/zip", tToolName.getValue().replaceAll(" ", "_") + ".zip"); // "ALA_Prediction_"+pid+".zip"
        } catch (Exception ex) {
            System.out.println("Error generating download for prediction model:");
            ex.printStackTrace(System.out);
        }

        this.detach();

        //getMapComposer().showMessage("Reference number to retrieve results: " + pid);

        //showInfoWindow("/output/maxent/" + pid + "/species.html");
    }

    String getJob(String type) {
        try {
            StringBuffer sbProcessUrl = new StringBuffer();
            sbProcessUrl.append(CommonData.satServer + "/ws/jobs/").append(type).append("?pid=").append(pid);

            System.out.println(sbProcessUrl.toString());
            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(sbProcessUrl.toString());

            get.addRequestHeader("Accept", "text/plain");

            int result = client.executeMethod(get);
            String slist = get.getResponseBodyAsString();
            System.out.println(slist);
            return slist;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * get CSV of speciesName, longitude, latitude in [0] and
     *
     * @param selectedSpecies
     * @param area
     * @return
     */
    private String[] getSpeciesData(Query query) {
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
                String sensitiveSpeciesRaw = new BiocacheQuery(null, null, "sensitive:[* TO *]", null, false).speciesList();
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
                if(rSpeciesSearch.isChecked()) {
                    searchSpeciesAuto.setFocus(true);
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
