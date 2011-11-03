package org.ala.spatial.analysis.web;

import au.com.bytecode.opencsv.CSVReader;
import java.io.StringReader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
import org.zkoss.zhtml.Filedownload;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;

/**
 *
 * @author ajay
 */
public class AddToolSitesBySpeciesComposer extends AddToolComposer {

    @Override
    public void afterCompose() {
        super.afterCompose();

        this.selectedMethod = "Sites By Species";
        this.totalSteps = 3;

        this.loadAreaLayers();
        this.loadSpeciesLayers();
        this.updateWindowTitle();

    }


    @Override
    public void onLastPanel() {
        System.out.println("**** On last step ****");
        super.onLastPanel();
        //this.updateName("My Prediction model for " + rgSpecies.getSelectedItem().getLabel());
        //this.updateName(getMapComposer().getNextAreaLayerName("Sites By Species"));
    }

    @Override
    public boolean onFinish() {
        //super.onFinish();

//        if (searchSpeciesAuto.getSelectedItem() != null) {
//            getMapComposer().mapSpeciesFromAutocomplete(searchSpeciesAuto, getSelectedArea());
//        }

        return runsitesbyspecies();
    }

    public boolean runsitesbyspecies() {
        try {
            SelectedArea sa = getSelectedArea();
            Query query = QueryUtil.queryFromSelectedArea(getSelectedSpecies(), sa, false);
            Double gridResolution = dResolution.getValue();


            StringBuffer sbProcessUrl = new StringBuffer();
            sbProcessUrl.append(CommonData.satServer + "/ws/sitesbyspecies/processgeoq?");
            
            
            sbProcessUrl.append("speciesq=" + URLEncoder.encode(query.getQ(), "UTF-8"));
            
            sbProcessUrl.append("&gridsize=" + URLEncoder.encode(String.valueOf(gridResolution), "UTF-8"));



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

            get.addRequestHeader("Accept", "text/plain");

            int result = client.executeMethod(get);
            pid = get.getResponseBodyAsString();

            openProgressBar();

            StringBuffer sbParams = new StringBuffer();

            this.setVisible(false);

            return true;
        } catch (Exception e) {
            System.out.println("SitesBySpecies error: ");
            e.printStackTrace(System.out);
            getMapComposer().showMessage("Unknown error.", this);
        }
        return false;
    }
    
    public void loadMap(Event event) {
        try {
            // set off the download as well
            String fileUrl = CommonData.satServer + "/ws/download/" + pid;
            Filedownload.save(new URL(fileUrl).openStream(), "application/zip", "sites_by_species.zip");
        } catch (Exception ex) {
            System.out.println("Error generating download for prediction model:");
            ex.printStackTrace(System.out);
        }

        this.detach();
    }

    void openProgressBar() {
        SitesBySpeciesProgressWCController window = (SitesBySpeciesProgressWCController) Executions.createComponents("WEB-INF/zul/AnalysisSitesBySpeciesProgress.zul", null, null);
        window.parent = this;
        window.start(pid);
        try {
            window.doModal();
        } catch (Exception e) {
            e.printStackTrace();
        }
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
                dResolution.setFocus(true);
                break;
        }
    }
}
