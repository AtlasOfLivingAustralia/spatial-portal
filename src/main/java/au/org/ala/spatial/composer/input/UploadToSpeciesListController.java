package au.org.ala.spatial.composer.input;

import au.org.ala.spatial.composer.add.AddSpeciesController;
import au.org.ala.spatial.composer.tool.ToolComposer;
import au.org.ala.spatial.data.SpeciesListUtil;
import au.org.ala.spatial.util.Util;
import au.org.emii.portal.composer.UtilityComposer;
import org.apache.log4j.Logger;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zul.Label;
import org.zkoss.zul.Textbox;

/**
 * The controller associated with the upload to species list dialog
 *
 * @author "Natasha Carter <Natasha.Carter@csiro.au>"
 */
public class UploadToSpeciesListController extends UtilityComposer {

    private static Logger logger = Logger.getLogger(UploadToSpeciesListController.class);

    Textbox tbName, tbDesc;
    Label tbInstructions;
    String species;
    String dataResourceUid;

    /**
     * The species string needs to be set containing a comma separated list of
     * scientificNames to include in the list.
     *
     * @param species
     */
    public void setSpecies(String species) {
        this.species = species;
    }

    public void onClick$btnOk(Event event) {
        if (tbName.getValue().length() > 0) {
            String name = tbName.getValue();
            String description = tbDesc.getValue();
            dataResourceUid = SpeciesListUtil.createNewList(name, species, description, null, Util.getUserEmail());
            logger.debug("The data resource uid: " + dataResourceUid);
            if (this.getParent() instanceof AddSpeciesController) {
                ((AddSpeciesController) this.getParent()).updateSpeciesListMessage(dataResourceUid);
            } else if (this.getParent() instanceof ToolComposer) {
                ((ToolComposer) this.getParent()).updateSpeciesListMessage(dataResourceUid);
            }
            this.detach();
        } else {
            tbInstructions.setValue("WARNING: Must supply a list name");
        }
    }

    public void onClick$btnCancel(Event event) {
        if (this.getParent().getId().equals("addtoolwindow")) {
            ToolComposer analysisParent = (ToolComposer) this.getParent();
            analysisParent.resetWindowFromSpeciesUpload("", "cancel");
        }
        this.detach();
    }

    public String getDataResourceUid() {
        return dataResourceUid;
    }
}
