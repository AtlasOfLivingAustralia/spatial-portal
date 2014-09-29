package au.org.ala.spatial.composer.species;

import au.org.ala.spatial.StringConstants;
import au.org.ala.spatial.util.BiocacheQuery;
import au.org.ala.spatial.util.Query;
import au.org.ala.spatial.util.QueryUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.zkoss.zk.ui.event.CheckEvent;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Div;
import org.zkoss.zul.Vbox;

import java.util.Map;

/**
 * A combined species autocomplete item that allows users to select whether or
 * not they wish to use the supplied scientific names.
 *
 * @author Natasha Carter (natasha.carter@csiro.au)
 */
public class SpeciesAutoCompleteComponent extends Div {

    private static final Logger LOGGER = Logger.getLogger(SpeciesAutoCompleteComponent.class);

    private Vbox vbox;
    private Checkbox chkUseRawName;
    private SpeciesAutoComplete autoComplete;

    public SpeciesAutoCompleteComponent() {
        initChildren();
        this.appendChild(vbox);

    }

    /**
     * Initialise the children components that will appear within the vbox.
     */
    private void initChildren() {
        vbox = new Vbox();
        chkUseRawName = new Checkbox("Use the scientific names supplied with the records");
        chkUseRawName.setChecked(false);
        autoComplete = new SpeciesAutoComplete();
        autoComplete.setAutodrop(true);
        autoComplete.setWidth("330px");
        vbox.appendChild(chkUseRawName);
        vbox.appendChild(autoComplete);
    }

    /**
     * An event that is called when the autocomplete selection has changed. This
     * will fire a new event to allow parent containers to update.
     *
     * @param event
     */
    public void onChange$autoComplete(Event event) {
        Events.sendEvent(new Event("onValueSelected", this));
    }

    /**
     * Returns true when the autocomplete combobox has a validly selected item.
     *
     * @return
     */
    public boolean hasValidItemSelected() {
        return !(autoComplete.getSelectedItem() == null || autoComplete.getSelectedItem().getValue() == null);
    }

    /**
     * Returns true when the autocomplete combo box has a selected item that is
     * validaly annotated. The annotation is used to supply further details for
     * the selected item based on the model.
     *
     * @return
     */
    public boolean hasValidAnnotatedItemSelected() {
        return !(autoComplete.getSelectedItem() == null
                || autoComplete.getSelectedItem().getAnnotatedProperties() == null
                || autoComplete.getSelectedItem().getAnnotatedProperties().isEmpty());
    }

    /**
     * @return the autoComplete
     */
    public SpeciesAutoComplete getAutoComplete() {
        return autoComplete;
    }

    /**
     * Returns the query that can be used to retrieve the selected species.
     *
     * @param geokosher
     * @return
     */
    public Query getQuery(Map points, boolean forMapping, boolean[] geokosher) {
        Query query;
        if (chkUseRawName.isChecked()) {
            query = QueryUtil.get(autoComplete.getFacetField(), autoComplete.getSelectedItem().getAnnotatedProperties().get(0), forMapping, geokosher);
        } else {
            query = QueryUtil.get(autoComplete.getSelectedItem().getAnnotatedProperties().get(0), points, forMapping, geokosher);
        }
        return query;
    }

    /**
     * The use "supplied" name check box has been either selected or deselected,
     * take the appropriate action on the combo box
     *
     * @param event
     */
    public void onCheck$chkUseRawName(Event event) {
        boolean checked = ((CheckEvent) event).isChecked();
        BiocacheQuery q = checked ? new BiocacheQuery(null, null, null, null, false, new boolean[]{true, true, false}) : null;
        autoComplete.setBiocacheQuery(q);
    }

    /**
     * Indicates whether or not the "useRawName" checkbox has been selected.
     *
     * @return
     */
    public boolean shouldUseRawName() {
        return chkUseRawName.isChecked();
    }

    /**
     * Returns an array of the scientific name and taxon rank for the selected
     * item. OR null when no item is selected.
     *
     * @return
     */
    public String[] getSelectedTaxonDetails() {
        if (hasValidAnnotatedItemSelected()) {
            Comboitem si = autoComplete.getSelectedItem();
            if (shouldUseRawName()) {
                return new String[]{autoComplete.getValue(), "unmatched" };
            } else {
                String taxon = autoComplete.getValue();
                String rank;

                String spVal = si.getDescription();
                if (spVal.trim().contains(": ")) {
                    taxon = spVal.trim().substring(spVal.trim().indexOf(":") + 1, spVal.trim().indexOf("-")).trim() + " (" + taxon + ")";
                    rank = spVal.trim().substring(0, spVal.trim().indexOf(":"));
                } else {
                    rank = StringUtils.substringBefore(spVal, " ").toLowerCase();
                    LOGGER.debug("mapping rank and species: " + rank + " - " + taxon);
                }
                if (StringConstants.SCIENTIFICNAME.equalsIgnoreCase(rank) || StringConstants.SCIENTIFIC.equalsIgnoreCase(rank)) {
                    rank = StringConstants.TAXON;
                }
                return new String[]{taxon, rank};
            }
        } else {
            return new String[0];
        }
    }

}
