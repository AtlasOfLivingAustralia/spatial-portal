package org.ala.spatial.analysis.web;

import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.composer.UtilityComposer;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ala.spatial.util.Layer;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.zkoss.zhtml.Iframe;
import org.zkoss.zhtml.Messagebox;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.InputEvent;
import org.zkoss.zul.Button;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.ListitemRenderer;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Window;

/**
 *
 * @author ajay
 */
public class MaxentWCController extends UtilityComposer {

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
    //private SpatialSettings ssets;
    private Tabbox outputtab;
    private Iframe mapframe;
    private Iframe infoframe;
    private Window maxentWindow;
    private String geoServer = "http://ec2-184-73-34-104.compute-1.amazonaws.com";  // http://localhost:8080;

    @Override
    public void doAfterCompose(Component component) throws Exception {
        super.doAfterCompose(component);
    }

    /**
     * When the page is loaded, setup the various settings that are needed
     * throughtout the page action
     *
     * @param comp The page component itself
     * @throws Exception
     */
    @Override
    public void afterCompose() {
        super.afterCompose();

        try {
            //Messagebox.show("Hello world afterCompose!");
            setupEnvListbox();
        } catch (Exception e) {
            System.out.println("opps in after compose");
        }

    }

    /**
     * Iterate thru' the layer list setup in the @doAfterCompose method
     * and setup the listbox
     */
    private void setupEnvListbox() {
        try {

            String envurl = "http://localhost:8080/alaspatial/ws/spatial/settings/envlist/string";

            //Messagebox.show("Loading env data from: " + envurl);


            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(envurl);
            get.addRequestHeader("Content-type", "text/plain");

            int result = client.executeMethod(get);
            String slist = get.getResponseBodyAsString();

            //Messagebox.show("done loading data: " + slist);

            String[] aslist = slist.split("\n");

            if (aslist.length > 0) {

                lbenvlayers.setItemRenderer(new ListitemRenderer() {

                    public void render(Listitem li, Object data) {
                        li.setWidth(null);
                        new Listcell((String) data).setParent(li);
                    }
                });

                lbenvlayers.setModel(new SimpleListModel(aslist));
            }


        } catch (Exception e) {
            System.out.println("error setting up env list");
            e.printStackTrace(System.out);
        }
    }

    public void onChange$sac(Event event) {
        test.setValue("Selected species: " + sac.getValue());
    }

    public void onClick$getspinfo(Event event) {
        try {
            test.setValue("clicked new value selected: " + sac.getText() + " - " + sac.getValue());
            System.out.println("Looking up taxon names for " + sac.getValue());
            //Messagebox.show("Hello world!, i got clicked");
        } catch (Exception ex) {
            System.out.println("Got an error clicking button!!");
            ex.printStackTrace(System.out);
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
        try {
            tbenvfilter.setValue("");
            for (Listitem li : (List<Listitem>) lbenvlayers.getItems()) {
                li.setVisible(true);
            }
            //Messagebox.show("Cleared env list");
        } catch (Exception ex) {
            Logger.getLogger(MaxentWCController.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("Clicked on clear env list button");
        }
    }

    public void onClick$startmaxent(Event event) {
        try {
            String msg = "";
            String[] envsel = null;
            StringBuffer sbenvsel = new StringBuffer();

            if (lbenvlayers.getSelectedCount() > 0) {
                envsel = new String[lbenvlayers.getSelectedCount()];
                msg = "Selected " + lbenvlayers.getSelectedCount() + " items \n ";
                Iterator it = lbenvlayers.getSelectedItems().iterator();
                int i = 0;
                while (it.hasNext()) {
                    Listitem li = (Listitem) it.next();

                    sbenvsel.append(li.getLabel());
                    if (it.hasNext()) {
                        sbenvsel.append(":");
                    }

                }

                //process(envsel);

                System.out.println("Selected species: " + sac.getValue());
                System.out.println("Selected env vars");
                System.out.println(sbenvsel.toString());
                System.out.println("Selected options: ");
                System.out.println("Jackknife: " + chkJackknife.isChecked());
                System.out.println("Response curves: " + chkRCurves.isChecked());
                System.out.println("Test per: " + txtTestPercentage.getValue());


                StringBuffer sbProcessUrl = new StringBuffer();
                sbProcessUrl.append("http://localhost:8080/alaspatial/ws/maxent/process?");
                sbProcessUrl.append("taxonid=" + URLEncoder.encode(sac.getValue(), "UTF-8"));
                sbProcessUrl.append("&envlist=" + URLEncoder.encode(sbenvsel.toString(), "UTF-8"));
                if (chkJackknife.isChecked()) {
                    sbProcessUrl.append("&chkJackknife=on");
                }
                if (chkRCurves.isChecked()) {
                    sbProcessUrl.append("&chkResponseCurves=on");
                }
                sbProcessUrl.append("&txtTestPercentage=" + txtTestPercentage.getValue());


                HttpClient client = new HttpClient();
                GetMethod get = new GetMethod(sbProcessUrl.toString());
                get.addRequestHeader("Content-type", "application/json");

                int result = client.executeMethod(get);
                String slist = get.getResponseBodyAsString();

                System.out.println("Got response from MaxentWSController: \n" + slist);

                String[] maxentresponse = slist.split(";");
                String[] status = maxentresponse[0].split(":");
                String[] pid = maxentresponse[1].split(":");
                String[] info = maxentresponse[2].split(":");

                String mapurl = geoServer + "/geoserver/wms?service=WMS&version=1.1.0&request=GetMap&layers=ALA:species_"+pid[1]+"&styles=alastyles&srs=EPSG:4326&TRANSPARENT=true&FORMAT=image%2Fpng";


                //get the current MapComposer instance
                MapComposer mc = getThisMapComposer();

                mc.addWMSLayer("Species Distribution Model - Maxent", mapurl ,(float) 0.5);



            }
            //Messagebox.show(msg, "Maxent", Messagebox.OK, Messagebox.INFORMATION);
        } catch (Exception e) {
            System.out.println("Maxent error: ");
            e.printStackTrace(System.out);
        }


    }

    /**
     * Gets the main pages controller so we can add a
     * layer to the map
     * @return MapComposer = map controller class
     */
    private MapComposer getThisMapComposer() {

        MapComposer mapComposer = null;
        //Page page = maxentWindow.getPage();
        Page page = getPage();
        mapComposer = (MapComposer) page.getFellow("mapPortalPage");

        return mapComposer;
    }
}
