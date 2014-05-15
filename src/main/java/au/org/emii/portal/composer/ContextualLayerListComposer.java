package au.org.emii.portal.composer;

import au.org.ala.spatial.composer.tool.ContextualLayerSelection;
import au.org.ala.spatial.util.CommonData;
import au.org.emii.portal.util.LayerUtilities;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.log4j.Logger;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.TreeMap;

/**
 * @author angus
 */
public class ContextualLayerListComposer extends UtilityComposer {

    private static Logger logger = Logger.getLogger(ContextualLayerListComposer.class);

    public Tree tree;
    private ArrayList empty = new ArrayList();

    ContextualLayerSelection contextualLayerSelection;

    @Override
    public void afterCompose() {
        super.afterCompose();

        iterateAndLoad();
    }

    public void iterateAndLoad() {
        try {
            ArrayList top = new ArrayList();

            TreeMap htCat1 = new TreeMap();
            TreeMap htCat2 = new TreeMap();

            JSONArray layerlist = CommonData.getLayerListJSONArray();
            for (int i = 0; i < layerlist.size(); i++) {
                JSONObject jo = layerlist.getJSONObject(i);

                if (!jo.getBoolean("enabled")) {
                    continue;
                }

                if (jo.getString("type").equalsIgnoreCase("Contextual")) {

                    DefaultTreeNode stn;
                    stn = new DefaultTreeNode(jo, empty);
                    String c1 = jo.containsKey("classification1") ? jo.getString("classification1") : "";
                    String c2 = jo.containsKey("classification2") ? jo.getString("classification2") : "";
                    addToMap(htCat1, htCat2, c1, c2, stn);
                }
            }

            Iterator it1 = htCat1.keySet().iterator();
            while (it1.hasNext()) {
                String catKey = (String) it1.next();
                JSONObject joCat = JSONObject.fromObject("{displayname:'" + catKey + "',type:'node',subtype:" + LayerUtilities.CONTEXTUAL + "}");

                //sort 2nd level branches
                ArrayList sorted = (ArrayList) htCat1.get(catKey);
                java.util.Collections.sort(sorted, new Comparator() {

                    @Override
                    public int compare(Object a, Object b) {
                        DefaultTreeNode sa = (DefaultTreeNode) a;
                        DefaultTreeNode sb = (DefaultTreeNode) b;
                        JSONObject ja = JSONObject.fromObject(sa.getData());
                        JSONObject jb = JSONObject.fromObject(sb.getData());
                        String na = ja.getString("displayname");
                        String nb = jb.getString("displayname");
                        na = (na.contains(">")) ? na.split(">")[1] : na;
                        nb = (nb.contains(">")) ? nb.split(">")[1] : nb;

                        return na.compareToIgnoreCase(nb);
                    }
                });
                DefaultTreeNode cat = new DefaultTreeNode(joCat, sorted);
                top.add(cat);
            }

            DefaultTreeNode root = new DefaultTreeNode("ROOT", top);
            DefaultTreeModel stm = new DefaultTreeModel(root);
            tree.setModel(stm);

            renderTree();
        } catch (Exception e) {
            logger.error("Error loading contextual layer list", e);
        }
    }

    private void addToMap(TreeMap htCat1, TreeMap htCat2, String cat1, String cat2, DefaultTreeNode treeNode) {

        if (cat1.trim().equals("") || cat1.trim().equals("?") || cat1.trim().equals("null")) {
            cat1 = "Other";
        }
        if (cat2.trim().equals("") || cat2.trim().equals("?") || cat2.trim().equals("null")) {
            ArrayList alCat1 = (ArrayList) htCat1.get(cat1);
            if (alCat1 == null) {
                alCat1 = new ArrayList();
            }
            alCat1.add(treeNode);
            htCat1.put(cat1, alCat1);

        } else {

            // first check if cat1 already exists
            // if yes, grab the cat2 list and add add to its AL
            // else, create a new one and add it to cat1.list
            String cat2_full = cat1 + ">" + cat2;
            ArrayList alCat2 = (ArrayList) htCat2.get(cat2_full);
            if (alCat2 == null) {
                alCat2 = new ArrayList();
            }

            alCat2.add(treeNode);
            if (!htCat2.containsKey(cat2_full)) {
                htCat2.put(cat2_full, alCat2);
            }

            ArrayList alCat1 = (ArrayList) htCat1.get(cat1);
            if (alCat1 == null) {
                alCat1 = new ArrayList();
            }

            String subtype = ((JSONObject) treeNode.getData()).getString("type");
            JSONObject joCat2 = JSONObject.fromObject("{displayname:'" + cat2_full + "',type:'node',subtype:"
                    + ((subtype.equalsIgnoreCase("environmental")) ? LayerUtilities.GRID : LayerUtilities.CONTEXTUAL)
                    + "}");
            DefaultTreeNode stnCat2 = new DefaultTreeNode(joCat2, alCat2);

            boolean found = false;
            for (int i = 0; i < alCat1.size(); i++) {

                if (stnCat2.toString().equals(alCat1.get(i).toString())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                alCat1.add(stnCat2);
            }
            if (!htCat1.containsKey(cat1)) {
                htCat1.put(cat1, alCat1);
            }
        }
    }

    void initALC() {
        if (contextualLayerSelection == null) {
            try {
                contextualLayerSelection = (ContextualLayerSelection) this.getFellow("contextuallayerselectionwindow", true);
            } catch (Exception e) {
                logger.error("Error initialising Contextual Layer Selection windows", e);
            }
        }
    }

    private void renderTree() {

        tree.setItemRenderer(new TreeitemRenderer() {

            @Override
            public void render(Treeitem item, Object data, int item_idx) throws Exception {

                DefaultTreeNode t = (DefaultTreeNode) data;

                JSONObject joLayer = JSONObject.fromObject(t.getData());

                Treerow tr = null;
                /*
                 * Since only one treerow is allowed, if treerow is not null,
                 * append treecells to it. If treerow is null, construct a new
                 * treerow and attach it to item.
                 */
                if (item.getTreerow() == null) {
                    tr = new Treerow();
                    tr.setParent(item);
                } else {
                    tr = item.getTreerow();
                    tr.getChildren().clear();
                }

                String displayname = joLayer.getString("displayname");
                displayname = (displayname.contains(">")) ? displayname.split(">")[1] : displayname;
                Treecell tcName = new Treecell();
                if (!joLayer.getString("type").equals("node")) {
                    Html img = new Html("<i class='icon-info-sign'></i>");
//                    img.setSrc("/img/information.png");
                    img.addEventListener("onClick", new EventListener() {

                        @Override
                        public void onEvent(Event event) throws Exception {
                            JSONObject jo = JSONObject.fromObject(event.getTarget().getParent().getParent().getAttribute("lyr"));
                            String s = jo.getString("id");
                            String metadata = CommonData.layersServer + "/layers/view/more/" + s;
                            getMapComposer().activateLink(metadata, "Metadata", false);
                        }
                    });
                    img.setParent(tcName);

                    Space sp = new Space();
                    sp.setParent(tcName);
                }
                Label lbl = new Label(displayname);
                lbl.setParent(tcName);

                if (!joLayer.getString("type").equals("node")) {
                    tr.setAttribute("lyr", joLayer);
                }

                // Attach onclick events:
                if (!joLayer.getString("type").equals("node")) {

                    // tcAdd
                    tr.addEventListener("onClick", new EventListener() {

                        @Override
                        public void onEvent(Event event) throws Exception {
                            JSONObject joLayer = JSONObject.fromObject(tree.getSelectedItem().getTreerow().getAttribute("lyr"));
                            if (!joLayer.getString("type").contentEquals("class")) {

                                String metadata = CommonData.layersServer + "/layers/view/more/" + joLayer.getString("uid");

                                initALC();
                                contextualLayerSelection.setLayer(joLayer.getString("displayname"), joLayer.getString("displaypath"), metadata,
                                        joLayer.getString("type").equalsIgnoreCase("environmental") ? LayerUtilities.GRID : LayerUtilities.CONTEXTUAL);

                            } else {
                                String classValue = joLayer.getString("displayname");
                                String layer = joLayer.getString("layername");
                                String displaypath = CommonData.geoServer
                                        + "/wms?service=WMS&version=1.1.0&request=GetMap&layers=ALA:Objects&format=image/png&viewparams=s:"
                                        + joLayer.getString("displaypath");

                                //Filtered requests don't work on
                                displaypath = displaypath.replace("gwc/service/", "");

                                String metadata = CommonData.layersServer + "/layers/view/more/" + joLayer.getString("id");
                                initALC();
                                contextualLayerSelection.setLayer(layer + " - " + classValue, displaypath, metadata, joLayer.getString("type").equalsIgnoreCase("environmental") ? LayerUtilities.GRID : LayerUtilities.CONTEXTUAL);
                            }
                        }
                    });

                }

                tcName.setParent(tr);
                item.setOpen(false);
            }
        });
    }
}
