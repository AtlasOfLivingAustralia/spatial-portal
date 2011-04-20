/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.analysis.web;

import java.awt.geom.Rectangle2D;
import net.sf.json.JSONObject;
import org.ala.spatial.util.ScatterplotData;

/**
 *
 * @author ajay
 */
public class AddToolScatterplotComposer extends AddToolComposer {

    int generation_count = 1;
    ScatterplotData data;
    EnvLayersCombobox cbLayer1;
    EnvLayersCombobox cbLayer2;

    @Override
    public void afterCompose() {
        super.afterCompose();

        this.selectedMethod = "Scatterplot";
        this.totalSteps = 5;

        this.loadAreaLayers();
        this.loadSpeciesLayers();
        this.updateWindowTitle();

        this.updateName(getMapComposer().getNextAreaLayerName("My Scatterplot"));
    }

    @Override
    public void onLastPanel() {
        super.onLastPanel();
        this.updateName(getMapComposer().getNextAreaLayerName("My Scatterplot"));
    }

    @Override
    public void onFinish() {
        //super.onFinish();

        System.out.println("Area: " + getSelectedArea());
        System.out.println("Species: " + getSelectedSpecies());
        //System.out.println("Layers: " + getSelectedLayers());

        //this.detach();

        String lsid = getSelectedSpecies();
        String name = rgSpecies.getSelectedItem().getLabel();

        JSONObject jo = (JSONObject) cbLayer1.getSelectedItem().getValue();
        String lyr1name = cbLayer1.getText();
        String lyr1value = jo.getString("name");

        jo = (JSONObject) cbLayer2.getSelectedItem().getValue();
        String lyr2name = cbLayer2.getText();
        String lyr2value = jo.getString("name");

        String pid = "";
        Rectangle2D.Double selection = null;
        boolean enabled = false;

        ScatterplotData data = new ScatterplotData(lsid, name, lyr1value, lyr1name, lyr2value, lyr2name, pid, selection, enabled);

        getMapComposer().loadScatterplot(data, tToolName.getValue());

        this.detach(); 
        

    }
}
