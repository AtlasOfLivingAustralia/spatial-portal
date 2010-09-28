package org.ala.spatial.analysis.web;

import au.org.emii.portal.composer.MapComposer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Set;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.ala.spatial.util.LayersUtil;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Image;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.ListitemRenderer;
import org.zkoss.zul.SimpleListModel;

/**
 *
 * @author ajay
 */
public class EnvironmentalList extends Listbox {

    ArrayList<ListEntry> listEntries;
    float[][] distances;
    String[] layerNames;
    float[] threasholds = {0.2f, 0.4f, 1.0f};
    SimpleListModel listModel;
    MapComposer mapComposer;
    String satServer;
    boolean environmentalOnly;

    public EnvironmentalList() {
        renderAll();
    }

    public void init(MapComposer mc, String sat_url, boolean environmental_only) {
        mapComposer = mc;
        satServer = sat_url;
        environmentalOnly = environmental_only;

        try {

            if (environmentalOnly) {

                StringBuffer sbProcessUrl = new StringBuffer();
                sbProcessUrl.append(sat_url + "/alaspatial/layers/analysis/inter_layer_association_rawnames.csv");

                System.out.println(sbProcessUrl.toString());
                HttpClient client = new HttpClient();
                GetMethod get = new GetMethod(sbProcessUrl.toString());

                get.addRequestHeader("Accept", "text/plain");

                int result = client.executeMethod(get);
                String slist = get.getResponseBodyAsString();

                String[] rows = slist.split("\n");

                //got a csv, put into column names, etc
                layerNames = new String[rows.length]; //last row is empty
                distances = new float[rows.length - 1][rows.length - 1];

                String[] line = rows[0].split(",");
                layerNames[0] = line[1];
                for (int i = 1; i < rows.length; i++) {   //last row is empty
                    line = rows[i].split(",");
                    layerNames[i] = line[0];
                    for (int j = 1; j < line.length; j++) {
                        try {
                            distances[i - 1][j - 1] = Float.parseFloat(line[j]);
                        } catch (Exception e) {
                        }
                        ;
                    }
                }
            } else {
                //contextual + environmental
                LayersUtil lu = new LayersUtil(mapComposer, satServer);
                String[] ctx = lu.getContextualLayers();
                String[] env = lu.getEnvironmentalLayers();

                layerNames = new String[ctx.length + env.length];
                for (int i = 0; i < env.length; i++) {
                    layerNames[i] = env[i];
                }
                for (int i = 0; i < ctx.length; i++) {
                    layerNames[i + env.length] = ctx[i];
                }
            }
            initLayerCatagories();

            //match up listEntries
            for (int i = 0; i < listEntries.size(); i++) {
                String entryName = listEntries.get(i).name;
                for (int j = 0; j < layerNames.length; j++) {
                    if (layerNames[j].equalsIgnoreCase(entryName)) {
                        listEntries.get(i).row_in_distances = j;
                        break;
                    }
                }

                //remove if missing
                if (listEntries.get(i).row_in_distances < 0) {
                    //        System.out.println("absent from layers assoc mx: " + listEntries.get(i).name);
                    listEntries.remove(i);
                    i--;
                } else {
                    listEntries.get(i).row_in_list = i;
                }
            }

            setupEnvironmentalLayers(layerNames, distances);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void setupEnvironmentalLayers(String[] aslist, float[][] assocMx) {
        try {
            if (aslist.length > 0) {

                setItemRenderer(new ListitemRenderer() {

                    @Override
                    public void render(Listitem li, Object data) {
                        new Listcell(((ListEntry) data).catagoryNames()).setParent(li);
                        new Listcell(((ListEntry) data).displayname).setParent(li);

                        Listcell lc = new Listcell();
                        //lc.setImage("/img/information.png");
                        lc.setParent(li);
                        lc.setValue(((ListEntry) data).uid);
                        Image img = new Image();
                        img.setSrc("/img/information.png");
                        img.addEventListener("onClick", new EventListener() {

                            @Override
                            public void onEvent(Event event) throws Exception {
                                String s = (String) ((Listcell) event.getTarget().getParent()).getValue();
                                String metadata = satServer + "/alaspatial/layers/" + s;
                                mapComposer.activateLink(metadata, "Metadata", false);
                            }
                        });
                        img.setParent(lc);

                        if (environmentalOnly) {
                            float value = ((ListEntry) data).value;
                            lc = new Listcell(" ");
                            if (threasholds[0] > value) {
                                lc.setSclass("lcRed");//setStyle("background: #bb2222;");
                            } else if (threasholds[1] > value) {
                                lc.setSclass("lcYellow");//lc.setStyle("background: #ffff22;");
                            } else {
                                lc.setSclass("lcGreen");//lc.setStyle("background: #22aa22;");
                            }
                            lc.setParent(li);
                        }
                    }
                });

                listModel = new SimpleListModel(listEntries);
                setModel(listModel);

                renderAll();
            }
        } catch (Exception e) {
            System.out.println("error setting up env list");
            e.printStackTrace(System.out);
        }
    }

    @Override
    public void toggleItemSelection(Listitem item) {
        super.toggleItemSelection(item);
        //update minimum distances here
    }

    public void updateDistances() {
        for (ListEntry le : listEntries) {
            le.value = 1;
        }

        for (Object o : getSelectedItems()) {
            int row = listEntries.get(((Listitem) o).getIndex()).row_in_distances;

            for (ListEntry le : listEntries) {
                float d = getDistance(le.row_in_distances, row);
                le.value = Math.min(le.value, d);
            }
        }

        for (int i = 0; i < listEntries.size(); i++) {
            float value = listEntries.get(i).value;
            Listcell lc = (Listcell) (getItemAtIndex(i).getLastChild());
            /*if(threasholds[0] > value){
            lc.setStyle("background: #bb2222;");
            }else if(threasholds[1] > value){
            lc.setStyle("background: #ffff22;");
            }else{
            lc.setStyle("background: #22aa22;");
            }*/
            if (threasholds[0] > value) {
                lc.setSclass("lcRed");//setStyle("background: #bb2222;");
            } else if (threasholds[1] > value) {
                lc.setSclass("lcYellow");//lc.setStyle("background: #ffff22;");
            } else {
                lc.setSclass("lcGreen");//lc.setStyle("background: #22aa22;");
            }
        }
    }

    public void onSelect(Event event) {
        if (environmentalOnly) {
            updateDistances();
        }
    }

    private float getDistance(int row, int row0) {
        //diagonal
        if (row == row0) {
            return 0;
        }

        //lower right matrix only
        int minrow, maxrow;
        if (row < row0) {
            minrow = row;
            maxrow = row0;
        } else {
            minrow = row0;
            maxrow = row;
        }

        //rows are 1-n, columns are 0-(n-1)
        if (maxrow - 1 < distances.length && minrow < distances[maxrow - 1].length) {
            return distances[maxrow - 1][minrow];
        }
        return 1;
    }

    void initLayerCatagories() {
        listEntries = new ArrayList<ListEntry>();

        try {
            String llist = (String) mapComposer.getSession().getAttribute("layerlist");

            if (llist == null) {
                String layersListURL = satServer + "/alaspatial/ws/layers/list";
                HttpClient client = new HttpClient();
                GetMethod get = new GetMethod(layersListURL);
                get.addRequestHeader("Accept", "application/json, text/javascript, */*");

                int result = client.executeMethod(get);
                llist = get.getResponseBodyAsString();
            }

            JSONArray layerlist = JSONArray.fromObject(llist);
            for (int i = 0; i < layerlist.size(); i++) {
                JSONObject jo = layerlist.getJSONObject(i);

                if (!jo.getBoolean("enabled")) {
                    continue;
                }

                if (environmentalOnly && jo.getString("type").equalsIgnoreCase("Contextual")) {
                    continue;
                }

                String c1 = jo.getString("classification1");
                String c2 = jo.getString("classification2");
                String name = jo.getString("name");
                String displayname = jo.getString("displayname");
                if (c1 == null || c1.equalsIgnoreCase("null")) {
                    c1 = "";
                }
                if (c2 == null || c2.equalsIgnoreCase("null")) {
                    c2 = "";
                }
                String uid = jo.getString("uid");

                listEntries.add(new ListEntry(name, displayname, c1, c2, 1, -1, -1, uid));
            }
        } catch (Exception e) {
            //FIXME:
        }

        java.util.Collections.sort(listEntries, new Comparator<ListEntry>() {

            public int compare(ListEntry e1, ListEntry e2) {
                //catagory 1, then catagory 2, then display name
                int c = e1.catagory1.compareTo(e2.catagory1);
                if (c == 0) {
                    c = e1.catagory2.compareTo(e2.catagory2);
                    if (c == 0) {
                        c = e1.displayname.compareTo(e2.displayname);
                    }
                }
                return c;
            }
        });
    }

    public String[] getSelectedLayers() {
        Set selectedItems = getSelectedItems();
        String[] selected = new String[selectedItems.size()];
        int i = 0;
        System.out.print("getSelectedLayers: ");
        for (Object o : selectedItems) {
            selected[i] = listEntries.get(((Listitem) o).getIndex()).name;
            i++;
            System.out.print(listEntries.get(((Listitem) o).getIndex()).displayname + ", ");
        }
        System.out.println("");
        return selected;
    }

    void selectLayers(String[] layers) {
        for (int i = 0; i < listEntries.size(); i++) {
            for (int j = 0; j < layers.length; j++) {
                if (listEntries.get(i).displayname.equalsIgnoreCase(layers[j])) {
                    selectItem(getItemAtIndex(i));
                    break;
                }
            }
        }
    }
}

class ListEntry {

    public String name;
    public String displayname;
    public String catagory1;
    public String catagory2;
    public float value;
    int row_in_list;
    int row_in_distances;
    String uid;

    public ListEntry(String name_, String displayname_, String catagory1_, String catagory2_, float value_, int row_list, int row_distances, String uid_) {
        name = name_;
        value = value_;
        row_in_list = row_list;
        row_in_distances = row_distances;
        catagory1 = catagory1_;
        catagory2 = catagory2_;
        displayname = displayname_;
        uid = uid_;
    }

    String catagoryNames() {
        if (catagory2.length() > 0) {
            return " " + catagory1 + "; " + catagory2;
        } else {
            return " " + catagory1;
        }
    }
}
