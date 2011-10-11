package org.ala.spatial.analysis.web;

import au.com.bytecode.opencsv.CSVReader;

import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.util.LayerUtilities;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;
import java.io.StringReader;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.ListitemRenderer;
import org.zkoss.zul.SimpleListModel;
import java.util.ArrayList;
import org.ala.spatial.data.Facet;
import org.ala.spatial.data.Legend;
import org.ala.spatial.data.LegendObject;
import org.ala.spatial.data.Query;
import org.ala.spatial.util.CommonData;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Button;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Listheader;

public class ClassificationLegend extends UtilityComposer {

    Query query;
    String pid;
    String colourmode;
    MapLayer mapLayer;
    String imagePath = "";
    // public int colours_index;
    // public Listcell legend_cell;
    // public Image sampleColour;
    //  public int legend_counter = 0;
    //  public Div colourChooser;
    public Listbox legend;
    //   Label lblEdit;
    Button createInGroup;
    ArrayList<String> legend_lines;
    boolean readonly = false;
    Listheader countheader;
    boolean checkmarks = false;

    @Override
    public void afterCompose() {
        super.afterCompose();

        query = (Query) (Executions.getCurrent().getArg().get("query"));
        System.out.println("Query q:" + query.getQ());
        mapLayer = (MapLayer) (Executions.getCurrent().getArg().get("layer"));

        readonly = (Executions.getCurrent().getArg().get("readonly")) != null;
        colourmode = (String) (Executions.getCurrent().getArg().get("colourmode"));
        pid = (String) (Executions.getCurrent().getArg().get("pid"));

        checkmarks = (Executions.getCurrent().getArg().get("checkmarks")) != null;

        buildLegend();
    }

    public void onClick$createInGroup(Event e) {
        getMapComposer().mapSpecies(query.newFacet(Facet.parseFacet(getSelectionFacet()), true),
                "Facet of " + mapLayer.getDisplayName(), "species", -1, LayerUtilities.SPECIES, null, -1);
    }

    void buildLegend() {
        try {
            String slist = null;

            if (query != null) {
                if (colourmode.equals("grid")) {
                    slist = "name,red,green,blue,count";
                    for (int i = 0; i < 600; i += 100) {
                        if (i == 1) {
                            slist += ">";
                        } else {
                            slist += ">";
                        }

                        slist += "\n" + i + "," + LegendObject.getRGB(Legend.getLinearColour(i, 0, 500, 0xFFFFFF00, 0xFFFF0000)) + ",";
                    }
                } else {
                    slist = query.getLegend(colourmode).getTable();
                }
            } else {
                // call get
                StringBuffer sbProcessUrl = new StringBuffer();
                sbProcessUrl.append(CommonData.satServer + "/ws/layer/get?");
                sbProcessUrl.append("pid=" + URLEncoder.encode(pid, "UTF-8"));
                HttpClient client = new HttpClient();
                GetMethod get = new GetMethod(sbProcessUrl.toString());
                get.addRequestHeader("Accept", "text/plain");
                int result = client.executeMethod(get);
                slist = get.getResponseBodyAsString();
                // retrieve legend file
                String[] slista = slist.split("\n");
                client = new HttpClient();
                get = new GetMethod(CommonData.satServer + "/" + slista[1]);
                get.addRequestHeader("Accept", "text/plain");
                result = client.executeMethod(get);
                slist = get.getResponseBodyAsString();
            }

            String[] lines = slist.split("\r\n");
            if (lines.length == 1) {
                lines = slist.split("\n");
            }
            legend_lines = new ArrayList<String>();
            int i = 0;
            for (i = 1; i < lines.length; i++) {
                if (lines[i].split(",").length > 3) {
                    legend_lines.add(lines[i]);
                }
            }

//            if (checkmarks) {
//                legend.setCheckmark(checkmarks);
//                legend.setSeltype("multiple");
//            }

            /* apply something to line onclick in lb */
            legend.setItemRenderer(new ListitemRenderer() {

                public void render(Listitem li, Object data) {
                    String[] ss = null;
                    try {
                        CSVReader csv = new CSVReader(new StringReader((String) data));
                        ss = csv.readNext();
                        csv.close();
                    } catch (IOException ex) {
                        Logger.getLogger(ClassificationLegend.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    if (ss == null) {
                        return;
                    }

                    Checkbox cb = null;

                    if (checkmarks) {
                        cb = new Checkbox();
                        cb.addEventListener("onCheck", new EventListener() {

                            public void onEvent(Event event) throws Exception {
                                //reload the map layer with this highlight.
                                if (mapLayer != null) {
                                    mapLayer.setHighlight(getSelectionFacet());
                                    createInGroup.setVisible(mapLayer.getHighlight().length() > 0);
                                }
                                getMapComposer().applyChange(mapLayer);
                            }
                        });
                        if (mapLayer.getHighlight() != null && mapLayer.getHighlight().length() > 0) {
                            Facet facet = Facet.parseFacet(mapLayer.getHighlight());
                            cb.setChecked(facet.isValid(ss[0]));
                        }
                    }

                    Listcell lc;
                    lc = new Listcell();
                    if (cb != null) {
                        //Do not display checkboxes for facets that are not simple
                        if (ss[0].length() == 0 || ss[0].endsWith(" more") || ss[0].equals("Unknown")) {
                            //cb.setDisabled(true);
                        } else {
                            cb.setParent(lc);
                        }
                    }
                    lc.setParent(li);

                    if (readonly) {
                        if (ss[0].length() > 0) {
                            lc = new Listcell(ss[0]);
                        } else {
                            lc = new Listcell("Unknown");
                        }
                    } else {
                        lc = new Listcell("group " + ss[0]);
                    }
                    lc.setParent(li);

                    int red = Integer.parseInt(ss[1]);
                    int green = Integer.parseInt(ss[2]);
                    int blue = Integer.parseInt(ss[3]);

                    lc = new Listcell("   ");
                    lc.setStyle("background-color: rgb(" + red + "," + green
                            + "," + blue + "); color: rgb(" + red + "," + green
                            + "," + blue + ")");
                    lc.setParent(li);

                    //count
                    try {
                        int count = Integer.parseInt(ss[4]);
                        countheader.setVisible(true);
                        lc = new Listcell(ss[4]);
                        lc.setParent(li);
                    } catch (Exception e) {
                        countheader.setVisible(false);
                    }
                }
            });


            legend.setModel(new SimpleListModel(legend_lines));

            createInGroup.setVisible(mapLayer.getHighlight() != null && mapLayer.getHighlight().length() > 0);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    String getSelectionFacet() {
        StringBuilder values = new StringBuilder();
        for (Listitem li : (List<Listitem>) legend.getItems()) {
            if (li.getFirstChild().getChildren().size() > 0
                    && ((Checkbox) li.getFirstChild().getFirstChild()).isChecked()) {
                if (values.length() > 0) {
                    values.append("%20AND%20");
                }
                values.append(colourmode + ":\"");
                values.append(((Listcell) li.getChildren().get(1)).getLabel());
                values.append("\"");
            }
        }
        if (values.length() > 0) {
            return values.toString();
        } else {
            return "";
        }
    }
}
