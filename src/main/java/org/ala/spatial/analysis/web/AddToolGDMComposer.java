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
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
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
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.ScrollEvent;
import org.zkoss.zul.Button;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Slider;
import org.zkoss.zul.Textbox;

/**
 *
 * @author ajay
 */
public class AddToolGDMComposer extends AddToolComposer {

    Listbox lbenvlayers;
    Button btnClearlbenvlayers;
    Listbox cutpoint;
    Radiogroup rgdistance;
    Combobox weighting;
    Checkbox useSubSample;
    Textbox sitePairsSize;
    Slider sitesslider;
    Label sitesslidermin, sitesslidermax, sitessliderdef;
    Hbox sliderbox;
    int MAX_SCROLL = 100;

    @Override
    public void afterCompose() {
        super.afterCompose();

        this.selectedMethod = "GDM";
        this.totalSteps = 5;

        this.loadAreaLayers();
        this.loadSpeciesLayers();
        this.loadGridLayers(true, false, true);
        this.updateWindowTitle();

        try {

            sitesslider.addEventListener("onScrolling", new EventListener() {

                @Override
                public void onEvent(Event event) throws Exception {
                    if (event instanceof ScrollEvent) {
//                        ScrollEvent se = (ScrollEvent) event;
//                        System.out.println("Checking for max scroll...");
//                        if (se.getPos() > MAX_SCROLL) {
//                            sitesslider.setCurpos(MAX_SCROLL);
//                        }
                    }
                }
            });

        } catch (Exception e) {
            System.out.println("Error in slider");
            System.out.println(e.getMessage());
        }

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
        this.updateName(getMapComposer().getNextAreaLayerName("My GDM : " + cutpoint.getSelectedItem().getValue()));

    }

    @Override
    public boolean onFinish() {
        Query query = getSelectedSpecies();
        if (query == null) {
            getMapComposer().showMessage("There is a problem selecting the species.  Try to select the species again", this);
            return false;
        }
        if (searchSpeciesAuto.getSelectedItem() != null) {
            getMapComposer().mapSpeciesFromAutocomplete(searchSpeciesAuto, getSelectedArea(), getGeospatialKosher());
        } else if (query != null && rgSpecies.getSelectedItem() != null && rgSpecies.getSelectedItem().getValue().equals("multiple")) {
            getMapComposer().mapSpecies(query, "Species assemblage", "species", 0, LayerUtilities.SPECIES, null, -1, MapComposer.DEFAULT_POINT_SIZE, MapComposer.DEFAULT_POINT_OPACITY, MapComposer.nextColour());
        }

        return rungdm();
    }
    Vector<String> vSp = new Vector<String>();

    private int getSpLoc(String sp) {
        if (vSp.indexOf(sp) == -1) {
            vSp.add(sp);
        }

        return vSp.indexOf(sp);
    }

    public void onClick$btnOk(Event event) {
        System.out.println("Completing step " + currentStep + " for GDM");
        if (currentStep == 3) {
            System.out.println("checking with server for step 1");
            runGDMStep1();
        }

        super.onClick$btnOk(event);

    }

    public boolean runGDMStep1() {
        try {
            SelectedArea sa = getSelectedArea();
            Query query = QueryUtil.queryFromSelectedArea(getSelectedSpecies(), sa, false, getGeospatialKosher());
            String sbenvsel = getSelectedLayers();
            String[] speciesData = getSpeciesData(query);

            StringBuffer sbProcessUrl = new StringBuffer();
            sbProcessUrl.append(CommonData.satServer + "/ws/gdm/step1?");
            //sbProcessUrl.append("http://localhost:8080/alaspatial/ws/gdm/process2?");
            sbProcessUrl.append("&envlist=" + URLEncoder.encode(sbenvsel, "UTF-8"));

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

            System.out.println("calling gdm ws step 1");
            int result = client.executeMethod(get);

            String step1resp = get.getResponseBodyAsString();
            System.out.println("==========step1resp==========");
            System.out.println(step1resp);
            System.out.println("==========step1resp==========");

            //Scanner s = new Scanner(new File("/Users/ajay/projects/ala/code/other/gdm/testdata/Cutpoint.csv"));
            Scanner s = new Scanner(step1resp);

            pid = s.nextLine(); // pid

            s.nextLine(); // ignore the header

            if (cutpoint.getItems().size() > 0) {
                cutpoint.getItems().clear();

                Listitem li = new Listitem();
                Listcell lc;

                lc = new Listcell("0");
                lc.setParent(li);

                lc = new Listcell("All records");
                lc.setParent(li);

                lc = new Listcell("All records");
                lc.setParent(li);

                li.setValue("0");
                li.setParent(cutpoint);


            }

            while (s.hasNext()) {
                Listitem li = new Listitem();
                Listcell lc;
                String[] sxs = s.nextLine().split(",");

                lc = new Listcell(sxs[0]);
                lc.setParent(li);

                lc = new Listcell(sxs[1]);
                lc.setParent(li);

                lc = new Listcell(sxs[2]);
                lc.setParent(li);

                li.setValue(sxs[0]);
                li.setParent(cutpoint);
            }

            cutpoint.setSelectedIndex(0);


            // setup the range slider for the sub samples

            int maxBytes = 524288000; // 500 * 1024 * 1024 bytes

            int maxScroll = maxBytes / ((lbListLayers.getSelectedCount() * 3) + 1) / 8;
            int minScroll = (int) (maxScroll * 0.01);  // 1% of maxScroll

            sitesslider.setCurpos(minScroll);
            sitesslider.setMaxpos(maxScroll);
            sitePairsSize.setValue(Integer.toString(minScroll));
            sitessliderdef.setValue(Integer.toString(minScroll));
            sitesslidermax.setValue(Integer.toString(maxScroll));


            return true;
        } catch (Exception ex) {
            Logger.getLogger(AddToolGDMComposer.class.getName()).log(Level.SEVERE, null, ex);
        }

        return false;
    }

    public void onScroll$sitesslider(Event event) {
        sitePairsSize.setValue(Integer.toString(sitesslider.getCurpos()));
    }

    public void onCheck$useSubSample(Event event) {
        if (useSubSample.isChecked()) {
            sitePairsSize.setDisabled(true);
            sliderbox.setVisible(false);
        } else {
            sitePairsSize.setDisabled(false);
            sliderbox.setVisible(true);
        }
    }

    public boolean rungdm() {
        try {
            StringBuffer sbProcessUrl = new StringBuffer();
            sbProcessUrl.append(CommonData.satServer + "/ws/gdm/step2?");
            sbProcessUrl.append("&pid=" + pid);
            sbProcessUrl.append("&cutpoint=" + cutpoint.getSelectedItem().getValue());
            sbProcessUrl.append("&useDistance=" + rgdistance.getSelectedItem().getValue());
            sbProcessUrl.append("&weighting=" + weighting.getSelectedItem().getValue());
            sbProcessUrl.append("&useSubSample=" + (useSubSample.isChecked() ? "1" : "0"));
            sbProcessUrl.append("&sitePairsSize=" + sitePairsSize.getValue());

            HttpClient client = new HttpClient();
            PostMethod get = new PostMethod(sbProcessUrl.toString());


            get.addRequestHeader("Accept", "text/plain");

            System.out.println("calling gdm ws: " + sbProcessUrl.toString());
            int result = client.executeMethod(get);

            pid = get.getResponseBodyAsString();

            loadMap(null);

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

    public boolean rungdmfull() {
        try {
            SelectedArea sa = getSelectedArea();
            Query query = QueryUtil.queryFromSelectedArea(getSelectedSpecies(), sa, false, getGeospatialKosher());
            String sbenvsel = getSelectedLayers();
            String[] speciesData = getSpeciesData(query);

            StringBuffer sbProcessUrl = new StringBuffer();
            sbProcessUrl.append(CommonData.satServer + "/ws/gdm/process2?");
            //sbProcessUrl.append("http://localhost:8080/alaspatial/ws/gdm/process2?");
            sbProcessUrl.append("&envlist=" + URLEncoder.encode(sbenvsel, "UTF-8"));

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

    public void loadMap(Event event) {

        String[] envlist = getSelectedLayers().split(":");

        for (String env : envlist) {
            String mapurl = CommonData.geoServer + "/wms?service=WMS&version=1.1.0&request=GetMap&layers=ALA:gdm_" + env + "Tran_" + pid + "&styles=alastyles&FORMAT=image%2Fpng";

            String legendurl = CommonData.geoServer
                    + "/wms?REQUEST=GetLegendGraphic&VERSION=1.0.0&FORMAT=image/png&WIDTH=10&HEIGHT=1"
                    + "&LAYER=ALA:gdm_" + env + "Tran_" + pid
                    + "&STYLE=alastyles";

            System.out.println(legendurl);

            String layername = "Tranformed " + CommonData.getFacetLayerDisplayName(env);
            System.out.println("Converting " + env + " to " + CommonData.getFacetLayerDisplayName(env));
            getMapComposer().addWMSLayer(pid + "_" + env, layername, mapurl, (float) 0.5, null, legendurl, LayerUtilities.GDM, null, null);
            MapLayer ml = getMapComposer().getMapLayer(pid + "_" + env);
            ml.setData("pid", pid + "_" + env);
            String infoUrl = CommonData.satServer + "/output/gdm/" + pid + "/gdm.html";
            MapLayerMetadata md = ml.getMapLayerMetadata();
            if (md == null) {
                md = new MapLayerMetadata();
                ml.setMapLayerMetadata(md);
            }
            md.setMoreInfo(infoUrl + "\nGDM Output\npid:" + pid);
            md.setId(Long.valueOf(pid));
        }

        //this.detach();

        //getMapComposer().showMessage("Reference number to retrieve results: " + pid);

        //showInfoWindow("/output/maxent/" + pid + "/species.html");
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
            QueryField qf2 = new QueryField("taxon_name");
            qf2.setStored(true);
            fields.add(qf2);
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
                    String taxonname = "taxon_name";
                    if (qf2 != null) {
                        taxonname = qf2.getAsString(i / 2);
                    }
                    if (!isSensitive) {
                        if (sb.length() == 0) {
                            //header
                            sb.append("\"X\",\"Y\",\"Code\"");
                        }
                        //sb.append("\n").append(points[i]).append(",").append(points[i + 1]).append(",").append(getLsidIndex(lsid)).append("");
                        sb.append("\n").append(points[i]).append(",").append(points[i + 1]).append(",\"").append(getSpLoc(lsid)).append("\"");
                        //sb.append("\n").append(points[i]).append(",").append(points[i + 1]).append(",\"").append(lsid).append(",\"").append(taxonname).append("\"");
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
        int i = 0;
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
