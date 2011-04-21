/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.ala.spatial.analysis.web;

import java.net.URL;
import java.net.URLEncoder;
import org.ala.spatial.util.CommonData;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.zkoss.zhtml.Filedownload;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Row;

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
        this.loadGridLayers(true);
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

        //this.detach();
        //runsampling();
        download(null);

    }

    public void runsampling() {
        try {
            SamplingResultsWCController window = (SamplingResultsWCController) Executions.createComponents("WEB-INF/zul/AnalysisSamplingResults.zul", this, null);
            window.parent = this;

            String taxon = getSelectedSpecies();
            String sbenvsel = getSelectedLayers();
            String area = getSelectedArea(); 

            StringBuffer sbProcessUrl = new StringBuffer();
            sbProcessUrl.append(CommonData.satServer + "/alaspatial/ws/sampling/process/preview?");
            if(taxon == null) {
                sbProcessUrl.append("taxonid=null");
            } else {
                sbProcessUrl.append("taxonid=" + URLEncoder.encode(taxon.replace(".", "__"), "UTF-8"));
            }
            sbProcessUrl.append("&envlist=" + URLEncoder.encode(sbenvsel.toString(), "UTF-8"));

            HttpClient client = new HttpClient();
            PostMethod get = new PostMethod(sbProcessUrl.toString());
            get.addParameter("area", area);
            get.addRequestHeader("Accept", "text/plain");

            int result = client.executeMethod(get);
            String slist = get.getResponseBodyAsString();

            System.out.println("Got response from SamplingWSController: " + result + "\n" + slist);

            //error condition, for example, when no combobox item is selected
            if (result != 200) {
                getMapComposer().showMessage("no records available");
                window.detach();
                return;
            }

            String[] aslist = slist.split(";");
            System.out.println("Result count: " + aslist.length);
            int count = 0;
            for (int i = 0; i < aslist.length; i++) {
                String[] rec = aslist[i].split("~");
                if (rec.length > 0) {
                    count++;
                }
            }
            count--; //don't include header in count

            if (slist.trim().length() == 0 || count == 0) {
                getMapComposer().showMessage("No records available for selected criteria.");

                window.detach();
                return;
            }

            //don't count header
            window.doModal();

            if (count == 1) {
                window.samplingresultslabel.setValue("preview: 1 record");
            } else {
                window.samplingresultslabel.setValue("preview: " + count + " records");
            }

            // load into the results popup
            // add rows
            String[] top_row = null;
            for (int i = 0; i < aslist.length; i++) {
                if (i == 0) {
                    top_row = aslist[i].split("~");

                    /*for (int k = 0; k < top_row.length; k++) {
                        if (isContextual(top_row[k])) {
                            try {
                                String layername = top_row[k].trim().replaceAll(" ", "_");
                                client = new HttpClient();
                                GetMethod getmethod = new GetMethod(satServer + "/alaspatial/ws/spatial/settings/layer/" + layername + "/extents"); // testurl
                                getmethod.addRequestHeader("Accept", "text/plain");

                                result = client.executeMethod(getmethod);
                                String[] salist = getmethod.getResponseBodyAsString().split("<br>");
                                contextualLists.put(new Integer(k), salist);

                                System.out.println("# records=" + salist.length);
                            } catch (Exception e) {
                                e.printStackTrace(System.out);
                            }
                        }
                    }*/
                }
                String[] rec = aslist[i].split("~");

                System.out.println("Column Count: " + rec.length);

                Row r = new Row();
                r.setParent(window.results_rows);
                // set the value
                for (int k = 0; k < rec.length && k < top_row.length; k++) {
                    Label label = new Label(rec[k]);
                    label.setParent(r);

                    /* onclick event for popup content update
                     * TODO: open metadata window
                    boolean iscontextual = isContextual(top_row[k]);
                    boolean isenvironmental = isEnvironmental(top_row[k]);

                    if (iscontextual || isenvironmental) {
                        if (i == 0) {
                            label.addEventListener("onClick", new EventListener() {

                                public void onEvent(Event event) throws Exception {
                                    showLayerExtentsLabel(event.getTarget());
                                }
                            });
                        }
                    }*/
                }
            }
        } catch (Exception e) {
            System.out.println("Exception calling sampling.preview:");
            e.printStackTrace(System.out);
        }
    }

    public void downloadWithProgressBar(Event event) {
        try {
            String taxon = getSelectedSpecies();
            String sbenvsel = getSelectedLayers();
            String area = getSelectedArea();

            StringBuffer sbProcessUrl = new StringBuffer();
            sbProcessUrl.append(CommonData.satServer + "/alaspatial/ws/sampling/processq/download?");
            sbProcessUrl.append("taxonid=" + URLEncoder.encode(taxon, "UTF-8"));
            sbProcessUrl.append("&envlist=" + URLEncoder.encode(sbenvsel.toString(), "UTF-8"));

            HttpClient client = new HttpClient();
                      PostMethod get = new PostMethod(sbProcessUrl.toString());
            get.addParameter("area", area);

            get.addRequestHeader("Accept", "text/plain");

            int result = client.executeMethod(get);
            pid = get.getResponseBodyAsString();

            System.out.println("Got response from SamplingWSController: \n" + pid);

            SamplingProgressWCController window = (SamplingProgressWCController) Executions.createComponents("WEB-INF/zul/AnalysisSamplingProgress.zul", this, null);
            window.parent = this;
            window.start(pid);
            window.doModal();
            getMapComposer().updateUserLogAnalysis("Sampling", "Species: " + taxon + ";area: " + area, sbenvsel.toString(), sbProcessUrl.toString(), pid, "Sampling starting file download for pid: " + pid);
        } catch (Exception e) {
            System.out.println("Exception calling sampling.download:");
            e.printStackTrace(System.out);
        }
    }

    public void download(Event event) {
        try {
            String taxon = getSelectedSpecies();
            String sbenvsel = getSelectedLayers();
            String area = getSelectedArea();

            StringBuffer sbProcessUrl = new StringBuffer();
            sbProcessUrl.append(CommonData.satServer + "/alaspatial/ws/sampling/process/download?");
            sbProcessUrl.append("taxonid=" + URLEncoder.encode(taxon, "UTF-8"));
            sbProcessUrl.append("&envlist=" + URLEncoder.encode(sbenvsel.toString(), "UTF-8"));

            HttpClient client = new HttpClient();
            PostMethod get = new PostMethod(sbProcessUrl.toString());
            get.addParameter("area", area);

            get.addRequestHeader("Accept", "text/plain");

            int result = client.executeMethod(get);
            String slist = get.getResponseBodyAsString();

            System.out.println("Got response from SamplingWSController: \n" + slist);

            if (slist.equalsIgnoreCase("")) {
                Messagebox.show("Unable to download sample file. Please try again", "ALA Spatial Analysis Toolkit - Sampling", Messagebox.OK, Messagebox.ERROR);
            } else {
                System.out.println("Sending file to user: " + CommonData.satServer + "/alaspatial" + slist);

                URL url = new URL(CommonData.satServer + "/alaspatial" + slist);
                Filedownload.save(url.openStream(), "application/zip",url.getFile());
                getMapComposer().updateUserLogAnalysis("Sampling", "species: " + taxon + "; area: " + area, sbenvsel.toString(), CommonData.satServer + "/alaspatial" + slist, pid, "Sampling results for species: " + taxon);
            }

            this.detach(); 

        } catch (Exception e) {
            System.out.println("Exception calling sampling.download:");
            e.printStackTrace(System.out);
        }
    }


}
