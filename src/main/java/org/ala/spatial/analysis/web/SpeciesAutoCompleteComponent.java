package org.ala.spatial.analysis.web;

import org.ala.spatial.data.BiocacheQuery;
import org.ala.spatial.data.Query;
import org.ala.spatial.data.QueryUtil;
import org.apache.commons.lang.StringUtils;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.CheckEvent;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.ForwardEvent;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Div;
import org.zkoss.zul.Vbox;

import au.org.emii.portal.composer.MapComposer;

/**
 * A combined species autocomplete item that allows users to select
 * whether or not they wish to use the supplied scientific names.
 *  
 * @author Natasha Carter (natasha.carter@csiro.au)
 *
 */
public class SpeciesAutoCompleteComponent extends Div{
    private Vbox vbox;
    private Checkbox chkUseRawName;
    private SpeciesAutoComplete autoComplete;
    
    public SpeciesAutoCompleteComponent(){
        initChildren();
        this.appendChild(vbox);
        //Executions.createComponents("", this, null);
        
    }
    /**
     * Initialise the children components that will appear within the vbox. 
     * 
     */
    private void initChildren(){
        vbox = new Vbox();
        chkUseRawName = new Checkbox("Use the scientific names supplied with the records");
        chkUseRawName.setChecked(false);
        chkUseRawName.addEventListener("onCheck", new EventListener(){

            @Override
            public void onEvent(Event event) throws Exception {
                // TODO Auto-generated method stub
                onCheck$chkUseRawName(event);
            }});
        autoComplete = new SpeciesAutoComplete();
        autoComplete.setAutodrop(true); 
        autoComplete.setWidth("330px");
        autoComplete.addEventListener("onChange", new EventListener(){

            @Override
            public void onEvent(Event event) throws Exception {
                // TODO Auto-generated method stub
                autoCompleteSelectionChanged(event);
            }
            
        });
        vbox.appendChild(chkUseRawName);
        vbox.appendChild(autoComplete);
    }
    /**
     * An event that is called when the autocomplete selection has changed. This will fire a new 
     * event to allow parent containers to update.
     * 
     * @param event
     */
    private void autoCompleteSelectionChanged(Event event){
        Events.sendEvent(new Event("onValueSelected", this));
    }
    /**
     * Returns true when the autocomplete combobox has a validly selected item. 
     * 
     * 
     * @return
     */
    public boolean hasValidItemSelected(){
        return !(autoComplete.getSelectedItem() == null || autoComplete.getSelectedItem().getValue() == null);
    }
    /**
     * Returns true when the autocomplete combo box has a selected item that is validaly annotated.  The annotation is used
     * to supply further details for the selected item based on the model.
     * 
     * @return
     */
    public boolean hasValidAnnotatedItemSelected(){
        return !(autoComplete.getSelectedItem() == null
                || autoComplete.getSelectedItem().getAnnotatedProperties() == null
                || autoComplete.getSelectedItem().getAnnotatedProperties().size() == 0);
    }
    /**
     * @return the autoComplete
     */
    public SpeciesAutoComplete getAutoComplete() {
        return autoComplete;
    }
    /**
     * Returns the query that can be used to retrieve the selected species.
     * @param mc
     * @param geokosher
     * @return
     */
    public Query getQuery(MapComposer mc,boolean forMapping, boolean[] geokosher){
        Query query;
        if(chkUseRawName.isChecked()){
            query = QueryUtil.get(autoComplete.getFacetField(), (String) autoComplete.getSelectedItem().getAnnotatedProperties().get(0), mc, forMapping, geokosher);
        } else {
            query = QueryUtil.get((String) autoComplete.getSelectedItem().getAnnotatedProperties().get(0), mc, forMapping, geokosher);
        }
        return query;
    }
    /**
     * The use "supplied" name check box has been either selected or deselected, take the appropriate 
     * action on the combo box
     * @param event
     */
    public void onCheck$chkUseRawName(Event event) {        
        boolean checked = ((CheckEvent)event).isChecked();
        BiocacheQuery q  = checked?new BiocacheQuery(null, null, null, null, false, new boolean[]{true,true,false}):null;
        autoComplete.setBiocacheQuery(q);
    }
    /**
     * Indicates whether or not the "useRawName" checkbox has been selected.
     * @return
     */
    public boolean shouldUseRawName(){
        return chkUseRawName.isChecked();
    }
    /**
     * Returns an array of the scientific name and taxon rank for the selected item. OR null
     * when no item is selected.
     * @return
     */
    public String[] getSelectedTaxonDetails(){
        if(hasValidAnnotatedItemSelected()){
            Comboitem si = autoComplete.getSelectedItem();
            if(shouldUseRawName()){
                return new String[]{autoComplete.getValue(), "unmatched"};
            } else{
                String taxon = autoComplete.getValue();
                String rank = "";

                String spVal = si.getDescription();
                if (spVal.trim().contains(": ")) {
                    taxon = spVal.trim().substring(spVal.trim().indexOf(":") + 1, spVal.trim().indexOf("-")).trim() + " (" + taxon + ")";
                    rank = spVal.trim().substring(0, spVal.trim().indexOf(":"));
                } else {
                    rank = StringUtils.substringBefore(spVal, " ").toLowerCase();
                    System.out.println("mapping rank and species: " + rank + " - " + taxon);
                }
                if (rank.equalsIgnoreCase("scientific name") || rank.equalsIgnoreCase("scientific")) {
                    rank = "taxon";
                }
                return new String[]{taxon, rank};
            }
        } else{
            return null;
        }
    }
    
}
