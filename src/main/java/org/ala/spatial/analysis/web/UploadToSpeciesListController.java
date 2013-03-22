package org.ala.spatial.analysis.web;

import org.ala.spatial.data.SpeciesListUtil;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zul.Label;
import org.zkoss.zul.Textbox;

import au.org.emii.portal.composer.UtilityComposer;
/**
 * 
 * The controller associated with the upload to species list dialog
 * @author "Natasha Carter <Natasha.Carter@csiro.au>"
 *
 */
public class UploadToSpeciesListController extends UtilityComposer{
    Textbox tbName,tbDesc;
    Label tbInstructions;
    String species;
    String dataResourceUid;
    /**
     * The species string needs to be set containing a comma separated list of scientificNames to include in the list.
     * @param species
     */
    public void setSpecies(String species){
        this.species = species;
    }
    
    public void onClick$btnOk(Event event) {
        if(tbName.getValue().length()>0){
            String name = tbName.getValue();
            String description = tbDesc.getValue();
            dataResourceUid=SpeciesListUtil.createNewList(name, species, description, null, getMapComposer().getCookieValue("ALA-Auth"));
            logger.debug("The data resource uid: " + dataResourceUid);
            if(this.getParent() instanceof AddSpeciesController)
                ((AddSpeciesController)this.getParent()).updateSpeciesListMessage(dataResourceUid);
            this.detach();
        }
        else{
            tbInstructions.setValue("WARNING: Must supply a list name");
        }
    }
  
    public void onClick$btnCancel(Event event) {
        if (this.getParent().getId().equals("addtoolwindow")) {
            AddToolComposer analysisParent = (AddToolComposer) this.getParent();
            analysisParent.resetWindowFromSpeciesUpload("", "cancel");
        }
        this.detach();
    }
    
    public String getDataResourceUid(){
        return dataResourceUid;
    }
}
