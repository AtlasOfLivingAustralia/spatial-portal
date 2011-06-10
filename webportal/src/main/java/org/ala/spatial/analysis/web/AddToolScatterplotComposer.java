/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.analysis.web;

import java.awt.geom.Rectangle2D;
import net.sf.json.JSONObject;
import org.ala.spatial.util.ScatterplotData;
import org.zkoss.zul.Checkbox;

/**
 *
 * @author ajay
 */
public class AddToolScatterplotComposer extends AddToolComposer {

    int generation_count = 1;
    ScatterplotData data;
    EnvLayersCombobox cbLayer1;
    EnvLayersCombobox cbLayer2;
    SpeciesAutoComplete bgSearchSpeciesAuto;
    Checkbox chkShowEnvIntersection;

    @Override
    public void afterCompose() {
        super.afterCompose();

        this.selectedMethod = "Scatterplot";
        this.totalSteps = 6;

        this.loadAreaLayers();
        this.loadSpeciesLayers();
        this.loadAreaLayersHighlight();
        this.loadSpeciesLayersBk();
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
        String name = getSelectedSpeciesName();

        JSONObject jo = (JSONObject) cbLayer1.getSelectedItem().getValue();
        String lyr1name = cbLayer1.getText();
        String lyr1value = jo.getString("name");

        jo = (JSONObject) cbLayer2.getSelectedItem().getValue();
        String lyr2name = cbLayer2.getText();
        String lyr2value = jo.getString("name");

        String pid = "";
        Rectangle2D.Double selection = null;
        boolean enabled = true;

        String backgroundLsid = getSelectedSpeciesBk();
        if (bgSearchSpeciesAuto.getSelectedItem() != null
                && bgSearchSpeciesAuto.getSelectedItem().getAnnotatedProperties() != null
                && bgSearchSpeciesAuto.getSelectedItem().getAnnotatedProperties().size() > 0) {
            backgroundLsid = (String) (bgSearchSpeciesAuto.getSelectedItem().getAnnotatedProperties().get(0));
        }

        String filterWkt = getSelectedArea();
        String highlightWkt = getSelectedAreaHighlight();

        boolean envGrid = chkShowEnvIntersection.isChecked();

        ScatterplotData data = new ScatterplotData(lsid, name, lyr1value, 
                lyr1name, lyr2value, lyr2name, pid, selection, enabled,
                backgroundLsid, filterWkt, highlightWkt, envGrid);

        getMapComposer().loadScatterplot(data, tToolName.getValue());

        this.detach();
    }
}
