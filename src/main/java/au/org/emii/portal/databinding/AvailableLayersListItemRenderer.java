package au.org.emii.portal.databinding;

import au.org.emii.portal.util.LayerUtilities;
import au.org.emii.portal.util.LayerUtilitiesImpl;
import org.springframework.beans.factory.annotation.Required;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.ListitemRenderer;

public class AvailableLayersListItemRenderer implements ListitemRenderer {

    private LayerUtilities layerUtilities = null;

    @Override
    public void render(Listitem item, Object data) throws Exception {
        String[] layer = (String[]) data;
        String name = layer[0];
        String label = layer[1];

        item.setValue(name);

        /* sometimes label will be null (badly configured server) so
         * just display the name instead
         */
        if (label == null) {
            label = name;
        }

        /* chomp stupidly long name/label, for the value displayed
         * to user.  Note we don't chomp the stored value or it we
         * wouldn't know what they'd selected
         */
        item.setLabel(layerUtilities.chompLayerName(label));
    }

    public LayerUtilities getLayerUtilities() {
        return layerUtilities;
    }

    @Required
    public void setLayerUtilities(LayerUtilities layerUtilities) {
        this.layerUtilities = layerUtilities;
    }
}
