/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.ala.spatial.analysis.web;

import au.com.bytecode.opencsv.CSVReader;
import au.org.emii.portal.util.LayerUtilities;
import java.io.StringReader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ala.spatial.data.BiocacheQuery;
import org.ala.spatial.data.Facet;
import org.ala.spatial.data.Query;
import org.ala.spatial.data.QueryField;
import org.ala.spatial.data.QueryUtil;
import org.ala.spatial.data.UploadQuery;
import org.ala.spatial.util.CommonData;
import org.ala.spatial.util.SelectedArea;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zul.Button;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;

/**
 *
 * @author ajay
 */
public class AddToolGDMComposer extends AddToolComposer {

    Listbox lbenvlayers;
    Button btnClearlbenvlayers;
    
    
    @Override
    public void afterCompose() {
        super.afterCompose();

        this.selectedMethod = "GDM";
        this.totalSteps = 5;

        this.loadAreaLayers();
        this.loadSpeciesLayers();
        this.loadGridLayers(true, true);
        this.updateWindowTitle();

    }

    public void onClick$btnClearlbenvlayers(Event event) {
        lbenvlayers.clearSelection();

        // check if lbListLayers is empty as well,
        // if so, then disable the next button
        if (lbenvlayers.getSelectedCount() == 0) {
            btnOk.setDisabled(true);
        }
    }

    public void onSelect$lbenvlayers(Event event) {
        btnOk.setDisabled(lbenvlayers.getSelectedCount() < 1);
    }

    @Override
    public void onLastPanel() {
        super.onLastPanel();
        this.updateName(getMapComposer().getNextAreaLayerName("My GDM"));

    }

    @Override
    public boolean onFinish() {
        Query query = getSelectedSpecies();
        if(query == null) {
            getMapComposer().showMessage("There is a problem selecting the species.  Try to select the species again", this);
            return false;
        }
        if (searchSpeciesAuto.getSelectedItem() != null) {
            getMapComposer().mapSpeciesFromAutocomplete(searchSpeciesAuto, getSelectedArea());
        } else if(query != null && rgSpecies.getSelectedItem() != null && rgSpecies.getSelectedItem().getValue().equals("multiple")) {
            getMapComposer().mapSpecies(query, "Species assemblage", "species", 0, LayerUtilities.SPECIES, null, -1);
        }

        System.out.println("GDM Selected layers:");
        System.out.println(getSelectedLayers());

        return rungdm();
    }
    
    public boolean rungdm() {
        try {
            SelectedArea sa = getSelectedArea();
            Query query = QueryUtil.queryFromSelectedArea(getSelectedSpecies(), sa, false);

            String sbenvsel = "p01:p05:p09:p12:p18:p23:p30:p35";
            if (lbenvlayers.getSelectedCount() > 0) {
                sbenvsel = ""; 
                Iterator<Listitem> it = lbenvlayers.getSelectedItems().iterator();
                sbenvsel += ""; 
                while(it.hasNext()) {
                    Listitem li = it.next();
                    sbenvsel += li.getValue();
                    if (it.hasNext()) sbenvsel += ":";
                }
            }

            String[] speciesData = getSpeciesData(query);

            System.out.println("speciesData:\n");
            System.out.println(speciesData);

            StringBuffer sbProcessUrl = new StringBuffer();
            sbProcessUrl.append(CommonData.satServer + "/ws/gdm/process2?");
            //sbProcessUrl.append("http://localhost:8080/alaspatial/ws/gdm/process2?");
            sbProcessUrl.append("&envlist=" + URLEncoder.encode(sbenvsel, "UTF-8"));

            HttpClient client = new HttpClient();
            PostMethod get = new PostMethod(sbProcessUrl.toString());

//            String area = null;
//            if (sa.getMapLayer() != null && sa.getMapLayer().getData("envelope") != null) {
//                area = "ENVELOPE(" + (String) sa.getMapLayer().getData("envelope") + ")";
//            } else {
//                area = sa.getWkt();
//            }
//            if (getSelectedArea() != null) {
//                get.addParameter("area", area);
//            }

            //check for no data
            if (speciesData[0] == null) {
                if (speciesData[1] == null) {
                    getMapComposer().showMessage("No records available for Modelling", this);
                } else {
                    getMapComposer().showMessage("All species and records selected are marked as sensitive", this);
                }
                return false;
            }
            get.addParameter("speciesdata", speciesData[0]);
            if (speciesData[1] != null) {
                get.addParameter("removedspecies", speciesData[1]);
            }

            get.addRequestHeader("Accept", "text/plain");

            System.out.println("calling gdm ws");
            int result = client.executeMethod(get);

            pid = get.getResponseBodyAsString();

            this.setVisible(false);

            getMapComposer().showMessage("Output for the GDM is available at http://spatial-dev.ala.org.au/output/gdm/" + pid + "/", getMapComposer());

            String fileUrl = CommonData.satServer + "/ws/download/" + pid;
            Filedownload.save(new URL(fileUrl).openStream(), "application/zip", tToolName.getValue().replaceAll(" ", "_") + ".zip"); // "ALA_Prediction_"+pid+".zip"

            return true;
        } catch (Exception ex) {
            Logger.getLogger(AddToolGDMComposer.class.getName()).log(Level.SEVERE, null, ex);
        }

        return false;
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
                    String lsid = "species";
                    String spname = "";
                    if (qf != null) {
                        lsid = qf.getAsString(i / 2);
                        isSensitive = sensitiveLsids.contains(lsid);
                    } 
                    if (!isSensitive) {
                        if (sb.length() == 0) {
                            //header
                            sb.append("\"X\",\"Y\",\"Code\"");
                        }
                        sb.append("\n").append(points[i]).append(",").append(points[i + 1]).append(",").append(getLsidIndex(lsid)).append("");
                        //sb.append("\n").append(points[i]).append(",").append(points[i + 1]).append(",\"").append(lsid).append("\"");
                    }
                }
                sb.append("\n"); 
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

    Vector<String> lsidList = new Vector<String>();
    private int getLsidIndex(String lsid) {
        int i=0;
        for (Listitem li : (List<Listitem>) lMultiple.getItems()) {
            i++;
            Listcell lc = (Listcell) li.getLastChild();
            if (lc.getLabel().equals(lsid)) {
                return i;
            }
        }

        System.out.print("i: " + i);
        return i;
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
            case 4:
                //chkJackknife.setFocus(true);
                break;
            case 5:
                tToolName.setFocus(true);
                break;
        }
    }
}
