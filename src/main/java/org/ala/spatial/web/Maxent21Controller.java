package org.ala.spatial.web;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import org.ala.spatial.analysis.maxent.MaxentServiceImpl;
import org.ala.spatial.analysis.maxent.MaxentSettings;
import org.ala.spatial.analysis.service.SamplingService;
import org.ala.spatial.dao.SpeciesDAO;
import org.ala.spatial.model.Species;
import org.ala.spatial.util.Layer;
import org.ala.spatial.util.SpatialSettings;
import org.ala.spatial.util.TabulationSettings;
import org.ala.spatial.util.UploadSpatialResource;
import org.ala.spatial.util.Zipper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Session;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.InputEvent;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zul.Button;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Iframe;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.ListitemRenderer;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Textbox;

/**
 *
 * @author ajay
 */
@Controller
public class Maxent21Controller extends GenericForwardComposer {

    private SpeciesDAO speciesDao;

    @Autowired
    public void setSpeciesDao(SpeciesDAO speciesDao) {
        System.out.println("setting species dao");
        this.speciesDao = speciesDao;
    }
    private Combobox sac;
    private Button getspinfo;
    private Label test;
    private Textbox tbenvfilter;
    private Button btenvfilterclear;
    private Listbox lbenvlayers;
    private Button startmaxent;
    private List<Layer> _layers;
    private Checkbox chkJackknife;
    private Checkbox chkRCurves;
    private Textbox txtTestPercentage;
    private Textbox trun;
    private SpatialSettings ssets;

    private Tabbox outputtab;
    private Iframe mapframe;
    private Iframe infoframe;

    /**
     * When the page is loaded, setup the various settings that are needed
     * throughtout the page action
     * 
     * @param comp The page component itself
     * @throws Exception 
     */
    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);

        // load layer list
        _layers = new ArrayList();

        ssets = new SpatialSettings();
        Layer[] _layerlist = ssets.getEnvironmentalLayers();

        for (int i = 0; i < _layerlist.length; i++) {
            _layers.add(_layerlist[i]);
            //System.out.println("Layer: " + _layerlist[i].name + " - " + _layerlist[i].display_name);

        }
        setupListbox();
        lbenvlayers.setModel(new SimpleListModel(_layers));


        trun.setValue(ssets.getMaxentCmd());

    }

    /**
     * Iterate thru' the layer list setup in the @doAfterCompose method
     * and setup the listbox
     */
    private void setupListbox() {
        lbenvlayers.setItemRenderer(new ListitemRenderer() {

            public void render(Listitem li, Object data) {
                li.setWidth(null);
                new Listcell(((Layer) data).display_name).setParent(li);
            }
        });
    }

    public void onChange$sac(Event event) {
        test.setValue("Selected species: " + sac.getValue());
    }

    public void onClick$getspinfo(Event event) {
        test.setValue("clicked new value selected: " + sac.getText() + " - " + sac.getValue());
        System.out.println("Looking up taxon names for " + sac.getValue());


        SamplingService ss = new SamplingService();
        //String csv = ss.sampleSpecies(sac.getValue(), null);
        String[] csvdata = ss.sampleSpecies(sac.getValue(), null).split("\n");

        //System.out.println("Species list for " + sac.getValue() + ":\n" + csv);

        for (int i = 0; i < csvdata.length; i++) {
            //String rec = csvdata[i];
            //String loc = rec.substring(rec.lastIndexOf(",", rec.lastIndexOf(",")));

            String[] recdata = csvdata[i].split(",");
            String spout = "species, " + recdata[recdata.length - 2] + ", " + recdata[recdata.length - 1];
            System.out.println(spout);

        }

    }

    /**
     * On changing the text box value, iterate thru' the listbox
     * and display only the matched options.
     * 
     * @param event The event attached to the component
     */
    public void onChanging$tbenvfilter(InputEvent event) {
        String filter = event.getValue().toLowerCase();
        System.out.println("checking for: " + filter);
        System.out.print("Number of list items to iterate thru: ");
        System.out.println(lbenvlayers.getItems().size());
        for (Listitem li : (List<Listitem>) lbenvlayers.getItems()) {
            if (li.getLabel().toLowerCase().contains(filter)) {
                if (!li.isVisible()) {
                    li.setVisible(true);
                }
            } else {
                if (li.isVisible()) {
                    li.setVisible(false);
                }
            }
        }
    }

    /**
     * Clear the filter text box
     * 
     * @param event The event attached to the component
     */
    public void onClick$btenvfilterclear(Event event) {
        tbenvfilter.setValue("");
        for (Listitem li : (List<Listitem>) lbenvlayers.getItems()) {
            li.setVisible(true);
        }
    }

    public void onClick$startmaxent(Event event) {
        try {
            String msg = "";
            String[] envsel = null;

            if (lbenvlayers.getSelectedCount() > 0) {
                envsel = new String[lbenvlayers.getSelectedCount()];
                msg = "Selected " + lbenvlayers.getSelectedCount() + " items \n ";
                Iterator it = lbenvlayers.getSelectedItems().iterator();
                int i = 0;
                while (it.hasNext()) {
                    Listitem li = (Listitem) it.next();
                    msg += li.getIndex() + " - " + li.getLabel();

                    int pidx = _layers.indexOf(lbenvlayers.getModel().getElementAt(li.getIndex()));
                    if (pidx > -1) {
                        msg += " - " + _layers.get(pidx).name + " \n ";
                        envsel[i] = _layers.get(pidx).name;
                        i++;
                    }

                }

                process(envsel);

            }
            //Messagebox.show(msg, "Maxent", Messagebox.OK, Messagebox.INFORMATION);
        } catch (Exception e) {
            System.out.println("Maxent error: ");
            e.printStackTrace(System.out);
        }


    }

    private void process(String[] envsel) {

        outputtab.setVisible(false);


        Session session = (Session) Sessions.getCurrent();
        long currTime = System.currentTimeMillis();

        System.out.println("init params: ");
        Iterator it = session.getWebApp().getInitParameterNames();
        while (it.hasNext()) {
            String s = (String) it.next();
            System.out.println(">> " + s);
        }

        String currentPath = Sessions.getCurrent().getWebApp().getRealPath("/");
        //String currentPath = TabulationSettings.base_output_dir;


        // dump the species data to a file
        SamplingService ss = new SamplingService();
        String[] csvdata = ss.sampleSpecies(sac.getValue(), null).split("\n");
        StringBuffer sbSpecies = new StringBuffer();
        for (int i = 0; i < csvdata.length; i++) {
            String[] recdata = csvdata[i].split(",");
            sbSpecies.append("species, " + recdata[recdata.length - 2] + ", " + recdata[recdata.length - 1]);
        }



        MaxentSettings msets = new MaxentSettings();
        //msets.setMaxentPath((String) session.getAttribute("maxentCmdPath"));
        msets.setMaxentPath(ssets.getMaxentCmd());
        msets.setEnvList(Arrays.asList(envsel));
        msets.setRandomTestPercentage(Integer.parseInt(txtTestPercentage.getValue()));
        //msets.setEnvPath((String) session.getAttribute("worldClimPresentVars") + "10minutes/");
        msets.setEnvPath(ssets.getEnvDataPath());
        msets.setEnvVarToggler("world");
        msets.setSpeciesFilepath(setupSpecies(sbSpecies.toString(), currentPath + "output/maxent/" + currTime + "/"));
        msets.setOutputPath(currentPath + "output/maxent/" + currTime + "/");
        if (chkJackknife.isChecked()) {
            msets.setDoJackknife(true);
        }
        if (chkRCurves.isChecked()) {
            msets.setDoResponsecurves(true);
        }

        test.setValue(msets.toString());


        MaxentServiceImpl maxent = new MaxentServiceImpl();
        maxent.setMaxentSettings(msets);
        int exitValue = maxent.process();

        System.out.println("Completed.");
        test.setValue(test.getValue() + " \n Completed: " + exitValue);

        String extraout = "";

        if (exitValue == 0) {
            // TODO: Should probably move this part an external "parent"
            // function so can be used by other functions
            //

            Hashtable htGeoserver = ssets.getGeoserverSettings();

            // if generated successfully, then add it to geoserver
            String url = (String)htGeoserver.get("geoserver_url") + "/rest/workspaces/ALA/coveragestores/maxent_" + currTime + "/file.arcgrid?coverageName=species_" + currTime;
            String extra = "";
            String username = (String)htGeoserver.get("geoserver_username");
            String password = (String)htGeoserver.get("geoserver_password");

            // first zip up the file as it's going to be sent as binary
            String ascZipFile = Zipper.zipFile(msets.getOutputPath() + "species.asc");

            // Upload the file to GeoServer using REST calls
            System.out.println("Uploading file: " + ascZipFile + " to \n" + url);
            UploadSpatialResource.loadResource(url, extra, username, password, ascZipFile);

            extraout += "status: success"; ///  \n <br />
            //extraout += "file: " + "output/maxent/" + currTime + "/species.asc \n <br />";
            //extraout += "info: " + "output/maxent/" + currTime + "/species.html \n <br />";
            //extraout += "map: " + "/wms?service=WMS&version=1.1.0&request=GetMap&layers=ALA:species_" + currTime + "&styles=alastyles&bbox=112.0,-44.0,154.0,-9.0&width=700&height=500&srs=EPSG:4326&format=application/openlayers";
            extraout += "";

            mapframe.setSrc((String)htGeoserver.get("geoserver_url") + "/wms?service=WMS&version=1.1.0&request=GetMap&layers=ALA:species_" + currTime + "&styles=alastyles&bbox=112.0,-44.0,154.0,-9.0&width=700&height=500&srs=EPSG:4326&format=application/openlayers");
            infoframe.setSrc("/output/maxent/" + currTime + "/species.html");
            outputtab.setVisible(true);

        } else {
            extraout += "Status: failure\n";
        }

        test.setValue(extraout);

    }

    public String setupSpecies(List speciesList, String outputpath) {
        try {
            File fDir = new File(outputpath);
            fDir.mkdir();

            File spFile = File.createTempFile("points_", ".csv", fDir);
            PrintWriter spWriter = new PrintWriter(new BufferedWriter(new FileWriter(spFile)));

            spWriter.write("spname, longitude, latitude \n");
            Iterator<Species> itr = speciesList.listIterator();
            while(itr.hasNext()) {
                Species sp = itr.next();
                spWriter.write("species, " + sp.getLongitude() + ", " + sp.getLatitude() + "\n");
            }
            spWriter.close();

            return spFile.getAbsolutePath();
        } catch (IOException ex) {
            //Logger.getLogger(MaxentServiceImpl.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("error writing species file:");
            ex.printStackTrace(System.out);
        }

        return null;
    }

    public String setupSpecies(String speciesList, String outputpath) {
        try {
            File fDir = new File(outputpath);
            fDir.mkdir();

            File spFile = File.createTempFile("points_", ".csv", fDir);
            PrintWriter spWriter = new PrintWriter(new BufferedWriter(new FileWriter(spFile)));

            //spWriter.write("spname, longitude, latitude \n");
            spWriter.write(speciesList);
            spWriter.close();

            return spFile.getAbsolutePath();
        } catch (IOException ex) {
            //Logger.getLogger(MaxentServiceImpl.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("error writing species file:");
            ex.printStackTrace(System.out);
        }

        return null;
    }

}
