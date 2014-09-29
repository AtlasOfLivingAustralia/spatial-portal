package au.org.ala.spatial.composer.input;

import au.org.ala.spatial.util.SpeciesListUtil;
import au.org.ala.spatial.util.Util;
import au.org.emii.portal.composer.UtilityComposer;
import org.apache.log4j.Logger;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.ForwardEvent;
import org.zkoss.zul.Label;
import org.zkoss.zul.Textbox;

/**
 * The controller associated with the upload to species list dialog
 *
 * @author "Natasha Carter <Natasha.Carter@csiro.au>"
 */
public class UploadToSpeciesListController extends UtilityComposer {

    private static final Logger LOGGER = Logger.getLogger(UploadToSpeciesListController.class);

    private Textbox tbName, tbDesc;
    private Label tbInstructions;
    private String species;
    private String dataResourceUid;
    private EventListener callback;

    /**
     * The species string needs to be set containing a comma separated list of
     * scientificNames to include in the list.
     *
     * @param species
     */
    public void setSpecies(String species) {
        this.species = species;
    }

    public void setCallback(EventListener callback) {
        this.callback = callback;
    }

    public void onClick$btnOk(Event event) {
        if (tbName.getValue().length() > 0) {
            String name = tbName.getValue();
            String description = tbDesc.getValue();
            dataResourceUid = SpeciesListUtil.createNewList(name, species, description, null, Util.getUserEmail());
            LOGGER.debug("The data resource uid: " + dataResourceUid);
            if (callback != null) {
                try {
                    callback.onEvent(new ForwardEvent("", null, null, dataResourceUid));
                } catch (Exception e) {
                    LOGGER.error("failed to trigger callback from species list upload", e);
                }
            }
            this.detach();
        } else {
            tbInstructions.setValue("WARNING: Must supply a list name");
        }
    }

    public void onClick$btnCancel(Event event) {
        this.detach();
    }

    public String getDataResourceUid() {
        return dataResourceUid;
    }
}
