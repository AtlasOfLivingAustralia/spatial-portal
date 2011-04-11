package org.ala.spatial.analysis.web;

import au.org.emii.portal.composer.LayerListComposer;
import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.settings.SettingsSupplementary;
import net.sf.json.JSONObject;
import org.ala.spatial.analysis.web.LayersAutoComplete;
import org.ala.spatial.analysis.web.SpeciesAutoComplete;
import org.ala.spatial.util.CommonData;
import org.zkoss.zk.ui.HtmlMacroComponent;
import org.zkoss.zk.ui.event.Event;

/**
 *
 * @author ajay
 */
public class AddLayerController extends UtilityComposer {
    
    SettingsSupplementary settingsSupplementary;
    LayersAutoComplete lac;
    String satServer;

    String treeName, treePath, treeMetadata;

    @Override
    public void afterCompose() {
        super.afterCompose();

        satServer = settingsSupplementary.getValue(CommonData.SAT_URL);
    }

    public void onClick$btnOk(Event event) {
        if(treeName != null) {
            getMapComposer().addWMSLayer(treeName,
                            treePath,
                            (float) 0.75, treeMetadata);

            getMapComposer().updateUserLogMapLayer("env - tree - add", /*joLayer.getString("uid")+*/"|"+treeName);
        }

        this.detach();
    }

    public void onClick$btnCancel(Event event) {
        this.detach();
    }

    public void onChange$lac(Event event) {
        treeName = null;
        
        LayerListComposer llc = (LayerListComposer) getFellow("layerList").getFellow("layerswindow");

        if (lac.getItemCount() > 0 && lac.getSelectedItem() != null) {
            JSONObject jo = (JSONObject) lac.getSelectedItem().getValue();
            String metadata = "";

            metadata = settingsSupplementary.getValue(CommonData.SAT_URL) + "/alaspatial/layers/" + jo.getString("uid");

            setLayer(jo.getString("displayname"), jo.getString("displaypath"), metadata);
        } else {
            JSONObject joLayer = JSONObject.fromObject(llc.tree.getSelectedItem().getTreerow().getAttribute("lyr"));
            if (!joLayer.getString("type").contentEquals("class")) {

                String metadata = satServer + "/alaspatial/layers/" + joLayer.getString("uid");

                setLayer(joLayer.getString("displayname"), joLayer.getString("displaypath"), metadata);
            } else {
                String classAttribute = joLayer.getString("classname");
                String classValue = joLayer.getString("displayname");
                String layer = joLayer.getString("layername");
                String displaypath = joLayer.getString("displaypath") + "&cql_filter=(" + classAttribute + "='" + classValue + "');include";
                //Filtered requests don't work on
                displaypath = displaypath.replace("gwc/service/", "");
                // Messagebox.show(displaypath);
                String metadata = satServer + "/alaspatial/layers/" + joLayer.getString("uid");

                setLayer(layer + " - " + classValue,     displaypath, metadata);
            }

            //close parent if it is 'addlayerwindow'
            try {
                getRoot().getFellow("addlayerwindow").detach();
            } catch (Exception e) {}
        }

        this.detach();
    }

    public void setLayer(String name, String displaypath, String metadata) {
        treeName = name;
        treePath = displaypath;
        treeMetadata = metadata;

        //fill autocomplete text
        lac.setText(name);

        //clear selection on tree
        LayerListComposer llc = (LayerListComposer) getFellow("layerList").getFellow("layerswindow");
        llc.tree.clearSelection();
    }


}
