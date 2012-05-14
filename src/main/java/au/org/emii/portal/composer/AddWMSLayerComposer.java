/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.emii.portal.composer;

import au.org.emii.portal.util.LayerUtilities;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.session.PortalSession;
import au.org.emii.portal.wms.RemoteMap;
import au.org.emii.portal.util.Validate;
import au.org.emii.portal.lang.LanguagePack;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import org.ala.spatial.analysis.web.AddToolComposer;
import org.zkoss.zul.Button;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Slider;
import org.zkoss.zul.Textbox;

/**
 *
 * @author ajay
 */
public class AddWMSLayerComposer extends AddToolComposer {

    private static final long serialVersionUID = 1L;
    /**
     * Cache a copy after discovering map layers so we don't need to do a
     * discovery twice if user selects a server, then does select layers and
     * then does add to map
     */
    private MapLayer discoveredLayer = null;
    /**
     * List of available layer names for the current discoveredLayer
     */
    private List<String[]> availableLayers = null;
    /*
     * Autowire components
     */
    // obtain label automatically checkbox
    private Checkbox nameAutomatically;
    /**
     * div containing all controls for label (name) that we show when user
     * unticks the name automatically checkbox
     */
    private Div labelDiv;
    // label
    private Textbox label;
    private Label invalidLabel;
    // discovery mode Automatic (hiden div)
    private Div discoveryAutomatic;
    // uri
    private Textbox uri;
    private Label invalidUri;
    // version
    private Listbox version;
    // select layers (hidden div)
    private Div selectLayers;
    // layer name
    private Listbox layerName;
    private Label invalidLayerName;
    // opacity controls HOLDER
    private Div discoveryAutomaticOpacity;
    private Button selectLayersButton;
    // manual layer specification (hidden div)
    private Div discoveryManual;
    // Get map uri
    private Textbox getMapUri;
    private Label invalidGetMapUri;
    // opacity control HOLDER
    private Div discoveryManualOpacity;
    // REAL opacity controls
    private Div opacityControl;
    // opacity slider
    private Slider opacitySlider;
    private Label opacityLabel;
    // result of adding layer
    private Label resultLabel;
    private LanguagePack languagePack = null;
    private LayerUtilities layerUtilities = null;
    private RemoteMap remoteMap = null;

    public void onCheck$nameAutomatically() {
        logger.debug("onNameAutomaticallyChanged()");
        labelDiv.setVisible(!nameAutomatically.isChecked());
    }

    public void onCheck$manual() {
        logger.debug("onManual()");
        toggleAutomaticMode(false);

        // if the getMapUri field is empty and uri field isn't copy across any value
        if (getMapUri.getValue().isEmpty() && (!uri.getValue().isEmpty())) {
            getMapUri.setValue(uri.getValue());
        }

        opacityControl.setParent(discoveryManualOpacity);
        opacityControl.setVisible(true);
    }

    public void onCheck$automatic() {
        logger.debug("onAutomatic()");
        toggleAutomaticMode(true);

        // if the getMapUri field is empty and getmapuri field isn't copy across any value
        if (uri.getValue().isEmpty() && (!getMapUri.getValue().isEmpty())) {
            uri.setValue(getMapUri.getValue());
        }

        opacityControl.setParent(discoveryAutomaticOpacity);
        opacityControl.setVisible(true);
    }

    public void toggleAutomaticMode(boolean autoMode) {
        discoveryManual.setVisible(!autoMode);
        discoveryAutomatic.setVisible(autoMode);
    }

    /**
     * If the uri has been changed, then the layers listed as available are no
     * longer current, so we need to wipeout the dropdown list and hide the div
     * again (provided we are already showing them)
     */
    public void onChange$uri() {
        logger.debug("onChange$uri()");
        if (selectLayers.isVisible()) {
            discoveredLayer = null;
            availableLayers = null;
            // you have to cast to remove compiler ambiguity
            layerName.setModel((ListModel) null);
            selectLayers.setVisible(false);
            selectLayersButton.setDisabled(false);
        }
    }

    @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = "NS_DANGEROUS_NON_SHORT_CIRCUIT")
    public void onClick$addAllLayersButton() {
        logger.debug("onClick$addAllLayersButton()");

        // hide (any) previous success message
        resultLabel.setVisible(false);

        // validate - DO NOT SHORT CURCUIT HERE!
        if (validateCommon() & validateAutomaticModeCommon()) {
            if (getMapComposer().addWMSServer(
                    (nameAutomatically.isChecked())
                    ? null : Validate.escapeHtmlAndTrim(label.getValue()),
                    uri.getValue(),
                    (String) version.getSelectedItem().getValue(),
                    getOpacity(),
                    true)) {

                // all sweet
                updateResult("wms_server_added");
            }
            // addWMSServer handles showing any errors for us
        }
    }

    public void onClick$hideExtLayersButton() {
        //MapComposer mc = getMapComposer();
        //mc.closeAddLayerDiv();

        System.out.println("Do something with the onClick$hideExtLayersButton() function");
    }

    public void onClick$selectLayersButton() {
        logger.debug("onClick$selectLayersButton");

        /*
         * only check that the user has entered a uri to do getcaps on they can
         * type the label later if they missed it
         */
        if (validateAutomaticModeCommon()) {
            discoveredLayer = remoteMap.autoDiscover(
                    (nameAutomatically.isChecked())
                    ? null : Validate.escapeHtmlAndTrim(label.getValue()),
                    getOpacity(),
                    uri.getValue(),
                    Validate.escapeHtmlAndTrim((String) version.getSelectedItem().getValue()));

            // want list of all
            if (discoveredLayer != null) {
                availableLayers = discoveredLayer.getAllLayerNames();
                updateAvailableLayers();

                selectLayers.setVisible(true);
                selectLayersButton.setDisabled(true);
            } else {
                // there was an error somewhere..
                getMapComposer().errorMessageBrokenWMSServer(remoteMap);
            }
        }
    }

    private void updateAvailableLayers() {
        if (availableLayers.size() > 0) {
            layerName.setModel(new ListModelList(availableLayers));
            layerName.setDisabled(false);
            layerName.setSelectedIndex(0);
        } else {
            // no layers available - broken wms server or all layers
            // already added
            layerName.setModel(new ListModelList(new String[][]{{languagePack.getLang("ext_layer_no_layers_available"), null}}));
            layerName.setDisabled(true);
        }

    }

    /**
     * User selected a map layer after doing "select layers" and clicked "add to
     * map"
     */
    @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = "NS_DANGEROUS_NON_SHORT_CIRCUIT")
    public void onClick$addDiscoveredLayerButton() {
        logger.debug("onAddDiscoveredLayer()");

        //  DO NOT SHORT CURCUIT HERE!
        if (validateCommon() & validateAutomaticModeCommon() & validiateAutomaticModeSelectLayer()) {

            String discoveredLayerId = ((String[]) layerName.getSelectedItem().getValue())[0];
            String discoveredLayerLabel = (String) layerName.getSelectedItem().getLabel();

            /*
             * we've already interogated the WMS server to find the available
             * layers - we don't need to do this again, all we need to do is
             * select the layer they user chose by looking for the layer name
             */
            MapLayer targetLayer = discoveredLayer.findByLayer(discoveredLayerId);

            if (targetLayer != null) {
                // append the hostname...
                try {
                    targetLayer.appendDescription(
                            languagePack.getCompoundLang(
                            "user_defined_layer_description",
                            new Object[]{new URL(uri.getValue()).getHost()}),
                            true);
                } /*
                 * throw away the any exceptions - shouldn't ever happen since
                 * we already added stuff from this uri
                 */ catch (MalformedURLException e) {
                }

                getMapComposer().addUserDefinedLayerToMenu(targetLayer, true);

                // all sweet
                updateResult("wms_layer_added");

                // remove the layer from available layers to stop it being 
                // added twice
                removeLayer(discoveredLayerId);
                updateAvailableLayers();
            } else {
                /*
                 * couldn't select layer name even thought it previously existed
                 * - hmmm should probably never happen...
                 */
                getMapComposer().showMessage("Unable to select layer: '" + discoveredLayerId + "'");
            }
        }
    }

    private void removeLayer(String layerId) {
        if (availableLayers != null) {
            boolean found = false;
            int i = 0;
            while (!found && i < availableLayers.size()) {
                if (availableLayers.get(i)[0].equals(layerId)) {
                    found = true;
                    availableLayers.remove(i);
                }
                i++;
            }
        }
    }

    @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = "NS_DANGEROUS_NON_SHORT_CIRCUIT")
    public void onClick$addManualLayerButton() {
        logger.debug("onAddManualLayer()");

        // DO NOT SHORT CURCUIT HERE!
        if (validateCommon() & validateManualMode()) {
            logger.debug("adding" + getMapUri.getValue());

//			if (getMapComposer().addWMSLayer(
//					(nameAutomatically.isChecked()) ? 
//							layerUtilities.getLayers(getMapUri.getValue()): Validate.escapeHtmlAndTrim(label.getValue()),
//					getMapUri.getValue(), 
//					getOpacity())) {
//			
//				// all sweet
//				updateResult("wms_layer_added");
//			}
            if (getMapComposer().addWMSLayer("wmslayer",
                    (nameAutomatically.isChecked())
                    ? layerUtilities.getLayers(getMapUri.getValue()) : Validate.escapeHtmlAndTrim(label.getValue()),
                    getMapUri.getValue(), (float) 0.75, "", "", LayerUtilities.WMS_1_1_1, "", "") != null) {
                // all sweet
                updateResult("wms_layer_added");
            }
            // addWMSLayer takes care of displaying any error message
        }
    }

    /**
     * Called with a different message after SUCCESSFULLY adding either a wms
     * server or layer
     */
    public void updateResult(String messageKey) {
        resultLabel.setVisible(true);
        resultLabel.setValue(languagePack.getLang(messageKey));

        /*
         * if we get to here it was all sweet - change selected tab on layers
         * view to be 'user defined' so user can see their new layers
         */
        //getMapComposer().selectAndActivateTab(PortalSession.LAYER_USER_TAB);
    }

    public float getOpacity() {
        return (float) ((float) opacitySlider.getCurpos()) / 100f;
    }

    private boolean validateCommon() {
        /*
         * label - if we are obtaining name automatically (see checkbox) then we
         * ignore the input field, otherwise it must be validated
         */
        boolean valid = nameAutomatically.isChecked();
        if (!valid) {
            valid = !Validate.empty(label.getValue());
            invalidLabel.setVisible(!valid);
        }
        return valid;
    }

    private boolean validateAutomaticModeCommon() {
        // uri
        boolean valid = !Validate.invalidHttpUri(
                Validate.prefixUri(uri.getValue()));
        invalidUri.setVisible(!valid);

        /*
         * don't need to validate version - something should always be set.
         * Checks later on force it to be constrained to the list as well.
         */
        return valid;
    }

    private boolean validiateAutomaticModeSelectLayer() {
        // layer name
        boolean valid;
        if (layerName.getSelectedItem() != null) {
            valid = !Validate.empty(((String[]) layerName.getSelectedItem().getValue())[0]);
        } else {
            valid = false;
        }
        invalidLayerName.setVisible(!valid);
        return valid;
    }

    private boolean validateManualMode() {
        boolean valid = !Validate.invalidHttpUri(
                Validate.prefixUri(getMapUri.getValue()));
        invalidGetMapUri.setVisible(!valid);
        return valid;
    }

    /**
     * Update the label for the opacity control to reflect the current value
     */
    public void onScroll$opacitySlider() {
        opacityLabel.setValue(opacitySlider.getCurpos() + "%");
    }

    public void relist(MapLayer mapLayer) {
        logger.debug("requested relisting for " + mapLayer.getLayer());
        if (mapLayer != null) {
            /*
             * the original uri as entered by the user will have been changed
             * during the auto discovery process. It should be safe to compare
             * IDs though. To enfource uniqueness in IDs, they get concatenated
             * with a colon and a sequence number for layers other than the root
             * layer so we have to test for contains rather than equality here
             */
            if ((discoveredLayer != null)
                    && (mapLayer.getLayer() != null)
                    && (mapLayer.getId().contains(discoveredLayer.getId()))) {
                logger.debug("relisting maplayer");
                availableLayers.add(new String[]{mapLayer.getLayer(), mapLayer.getName()});
                updateAvailableLayers();
            }
            // otherwise it's from a different discovery - do nothing
        }
    }

    public LanguagePack getLanguagePack() {
        return languagePack;
    }

    public void setLanguagePack(LanguagePack languagePack) {
        this.languagePack = languagePack;
    }

    public LayerUtilities getLayerUtilities() {
        return layerUtilities;
    }

    public void setLayerUtilities(LayerUtilities layerUtilities) {
        this.layerUtilities = layerUtilities;
    }
}
