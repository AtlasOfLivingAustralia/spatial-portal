package au.org.ala.spatial.composer.tool;

import au.org.ala.spatial.StringConstants;
import au.org.ala.spatial.util.CommonData;
import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.util.LayerUtilitiesImpl;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.log4j.Logger;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.*;

import java.util.*;

/**
 * @author angus
 */
public class ContextualLayerListComposer extends UtilityComposer {

    private static final Logger LOGGER = Logger.getLogger(ContextualLayerListComposer.class);

    protected Tree tree;
    private ContextualLayerSelection contextualLayerSelection;
    private List empty = new ArrayList();

    @Override
    public void afterCompose() {
        super.afterCompose();

        iterateAndLoad();
    }

    public void iterateAndLoad() {
        try {
            List top = new ArrayList();

            Map htCat1 = new TreeMap();
            Map htCat2 = new TreeMap();

            JSONArray layerlist = CommonData.getLayerListJSONArray();
            for (int i = 0; i < layerlist.size(); i++) {
                JSONObject jo = layerlist.getJSONObject(i);

                if (!jo.getBoolean(StringConstants.ENABLED)) {
                    continue;
                }

                if ("Contextual".equalsIgnoreCase(jo.getString(StringConstants.TYPE))) {

                    DefaultTreeNode stn;
                    stn = new DefaultTreeNode(jo, empty);
                    String c1 = jo.containsKey(StringConstants.CLASSIFICATION1) ? jo.getString(StringConstants.CLASSIFICATION1) : "";
                    String c2 = jo.containsKey(StringConstants.CLASSIFICATION2) ? jo.getString(StringConstants.CLASSIFICATION2) : "";
                    addToMap(htCat1, htCat2, c1, c2, stn);
                }
            }

            for (Object o : htCat1.entrySet()) {
                JSONObject joCat = JSONObject.fromObject("{displayname:'" + ((Map.Entry) o).getKey() + "',type:'node',subtype:" + LayerUtilitiesImpl.CONTEXTUAL + "}");

                //sort 2nd level branches
                List sorted = (ArrayList) ((Map.Entry) o).getValue();
                java.util.Collections.sort(sorted, new Comparator() {

                    @Override
                    public int compare(Object a, Object b) {
                        DefaultTreeNode sa = (DefaultTreeNode) a;
                        DefaultTreeNode sb = (DefaultTreeNode) b;
                        JSONObject ja = JSONObject.fromObject(sa.getData());
                        JSONObject jb = JSONObject.fromObject(sb.getData());
                        String na = ja.getString(StringConstants.DISPLAYNAME);
                        String nb = jb.getString(StringConstants.DISPLAYNAME);
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
            LOGGER.error("Error loading contextual layer list", e);
        }
    }

    private void addToMap(Map htCat1, Map htCat2, String cat1, String cat2, DefaultTreeNode treeNode) {

        String nCat1 = cat1;
        if (cat1.trim().isEmpty() || "?".equals(cat1.trim()) || StringConstants.NULL.equals(cat1.trim())) {
            nCat1 = "Other";
        }
        if (cat2.trim().isEmpty() || "?".equals(cat2.trim()) || StringConstants.NULL.equals(cat2.trim())) {
            List alCat1 = (List) htCat1.get(cat1);
            if (alCat1 == null) {
                alCat1 = new ArrayList();
            }
            alCat1.add(treeNode);
            htCat1.put(nCat1, alCat1);

        } else {
            // first check if cat1 already exists
            // if yes, grab the cat2 list and add add to its AL
            // else, create a new one and add it to cat1.list
            String cat2Full = nCat1 + ">" + cat2;
            List alCat2 = (List) htCat2.get(cat2Full);
            if (alCat2 == null) {
                alCat2 = new ArrayList();
            }

            alCat2.add(treeNode);
            if (!htCat2.containsKey(cat2Full)) {
                htCat2.put(cat2Full, alCat2);
            }

            List alCat1 = (List) htCat1.get(cat1);
            if (alCat1 == null) {
                alCat1 = new ArrayList();
            }

            String subtype = ((JSONObject) treeNode.getData()).getString(StringConstants.TYPE);
            JSONObject joCat2 = JSONObject.fromObject("{displayname:'" + cat2Full + "',type:'node',subtype:"
                    + ((StringConstants.ENVIRONMENTAL.equalsIgnoreCase(subtype)) ? LayerUtilitiesImpl.GRID : LayerUtilitiesImpl.CONTEXTUAL)
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
            if (!htCat1.containsKey(nCat1)) {
                htCat1.put(nCat1, alCat1);
            }
        }
    }

    void initALC() {
        if (contextualLayerSelection == null) {
            try {
                contextualLayerSelection = (ContextualLayerSelection) this.getFellow("contextuallayerselectionwindow", true);
            } catch (Exception e) {
                LOGGER.error("Error initialising Contextual Layer Selection windows", e);
            }
        }
    }

    private void renderTree() {

        tree.setItemRenderer(new TreeitemRenderer() {

            @Override
            public void render(Treeitem item, Object data, int itemIdx) throws Exception {

                DefaultTreeNode t = (DefaultTreeNode) data;

                JSONObject joLayer = JSONObject.fromObject(t.getData());

                Treerow tr;
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

                String displayname = joLayer.getString(StringConstants.DISPLAYNAME);
                displayname = (displayname.contains(">")) ? displayname.split(">")[1] : displayname;
                Treecell tcName = new Treecell();
                if (!"node".equals(joLayer.getString(StringConstants.TYPE))) {
                    Html img = new Html("<i class='icon-info-sign'></i>");

                    img.addEventListener(StringConstants.ONCLICK, new EventListener() {

                        @Override
                        public void onEvent(Event event) throws Exception {
                            JSONObject jo = JSONObject.fromObject(event.getTarget().getParent().getParent().getAttribute("lyr"));
                            String s = jo.getString(StringConstants.ID);
                            String metadata = CommonData.getLayersServer() + "/layers/view/more/" + s;
                            getMapComposer().activateLink(metadata, "Metadata", false);
                        }
                    });
                    img.setParent(tcName);

                    Space sp = new Space();
                    sp.setParent(tcName);
                }
                Label lbl = new Label(displayname);
                lbl.setParent(tcName);

                if (!"node".equals(joLayer.getString(StringConstants.TYPE))) {
                    tr.setAttribute("lyr", joLayer);
                }

                // Attach onclick events:
                if (!"node".equals(joLayer.getString(StringConstants.TYPE))) {

                    // tcAdd
                    tr.addEventListener(StringConstants.ONCLICK, new EventListener() {

                        @Override
                        public void onEvent(Event event) throws Exception {
                            JSONObject joLayer = JSONObject.fromObject(tree.getSelectedItem().getTreerow().getAttribute("lyr"));
                            if (!StringConstants.CLASS.equals(joLayer.getString(StringConstants.TYPE))) {

                                String metadata = CommonData.getLayersServer() + "/layers/view/more/" + joLayer.getString("uid");

                                initALC();
                                contextualLayerSelection.setLayer(joLayer.getString(StringConstants.DISPLAYNAME), joLayer.getString("displaypath"), metadata,
                                        StringConstants.ENVIRONMENTAL.equalsIgnoreCase(joLayer.getString(StringConstants.TYPE)) ? LayerUtilitiesImpl.GRID : LayerUtilitiesImpl.CONTEXTUAL);

                            } else {
                                String classValue = joLayer.getString(StringConstants.DISPLAYNAME);
                                String layer = joLayer.getString(StringConstants.LAYERNAME);
                                String displaypath = CommonData.getGeoServer()
                                        + "/wms?service=WMS&version=1.1.0&request=GetMap&layers=ALA:Objects&format=image/png&viewparams=s:"
                                        + joLayer.getString("displaypath");

                                //Filtered requests don't work on
                                displaypath = displaypath.replace("gwc/service/", "");

                                String metadata = CommonData.getLayersServer() + "/layers/view/more/" + joLayer.getString(StringConstants.ID);
                                initALC();
                                contextualLayerSelection.setLayer(layer + " - " + classValue, displaypath, metadata,
                                        StringConstants.ENVIRONMENTAL.equalsIgnoreCase(joLayer.getString(StringConstants.TYPE)) ? LayerUtilitiesImpl.GRID : LayerUtilitiesImpl.CONTEXTUAL);
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
