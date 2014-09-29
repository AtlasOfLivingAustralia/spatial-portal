/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.ala.spatial.composer.add;

import au.org.ala.spatial.StringConstants;
import au.org.ala.spatial.util.CommonData;
import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.lang.LanguagePack;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.menu.MapLayerMetadata;
import au.org.emii.portal.util.LayerUtilities;
import au.org.emii.portal.util.LayerUtilitiesImpl;
import au.org.emii.portal.util.Validate;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.geotools.data.ows.CRSEnvelope;
import org.geotools.data.ows.Layer;
import org.geotools.data.ows.WMSCapabilities;
import org.geotools.data.wms.WebMapServer;
import org.geotools.data.wms.request.GetMapRequest;
import org.geotools.data.wms.xml.MetadataURL;
import org.zkoss.zul.*;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * @author ajay
 */
public class AddWMSLayerComposer extends UtilityComposer {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(AddWMSLayerComposer.class);
    private Label linkall1, linkall2, linkall3, linkall4, linksingle1;
    /**
     * Cache a copy after discovering map layers so we don't need to do a
     * discovery twice if user selects a server, then does select layers and
     * then does add to map
     */
    private WMSCapabilities discoveredLayer = null;
    /**
     * List of available layer names for the current discoveredLayer
     */
    private List<String[]> availableLayers = null;
    private WebMapServer wmsServer = null;
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
    private Button addManualLayerButton;
    private Button addDiscoveredLayerButton;
    private LanguagePack languagePack = null;
    private LayerUtilities layerUtilities = null;

    public void onCheck$nameAutomatically() {
        LOGGER.debug("onNameAutomaticallyChanged()");
        labelDiv.setVisible(!nameAutomatically.isChecked());
    }

    public void onCheck$manual() {
        LOGGER.debug("onManual()");
        toggleAutomaticMode(false);

        // if the getMapUri field is empty and uri field isn't copy across any value
        if (getMapUri.getValue().isEmpty() && (!uri.getValue().isEmpty())) {
            getMapUri.setValue(uri.getValue());
        }

        opacityControl.setParent(discoveryManualOpacity);
        opacityControl.setVisible(true);
    }

    public void onCheck$automatic() {
        LOGGER.debug("onAutomatic()");
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
        addManualLayerButton.setVisible(!autoMode);

        discoveryAutomatic.setVisible(autoMode);
        addDiscoveredLayerButton.setVisible(autoMode);
    }

    /**
     * If the uri has been changed, then the layers listed as available are no
     * longer current, so we need to wipeout the dropdown list and hide the div
     * again (provided we are already showing them)
     */
    public void onChange$uri() {
        LOGGER.debug("onChange$uri()");
        if (selectLayers.isVisible()) {
            discoveredLayer = null;
            availableLayers = null;
            // you have to cast to remove compiler ambiguity
            layerName.setModel((ListModel) null);
            selectLayers.setVisible(false);
            selectLayersButton.setDisabled(false);
            addDiscoveredLayerButton.setVisible(false);
        }
    }

    @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = "NS_DANGEROUS_NON_SHORT_CIRCUIT")
    public void onClick$addAllLayersButton() {
        LOGGER.debug("onClick$addAllLayersButton()");

        // hide (any) previous success message
        resultLabel.setVisible(false);

        // validate - DO NOT SHORT CURCUIT HERE!
        if (validateCommon() & validateAutomaticModeCommon()) {
            try {

                wmsServer = new WebMapServer(new URL(uri.getValue()));

                discoveredLayer = wmsServer.getCapabilities();
                MapLayer mapLayer = new MapLayer();
                mapLayer.setName(Validate.escapeHtmlAndTrim(label.getValue()));

                String ur = uri.getValue();
                mapLayer.setUri(ur);
                mapLayer.setLayer(layerUtilities.getLayers(ur));
                mapLayer.setOpacity(opacitySlider.getCurpos() / 100.0f);
                mapLayer.setImageFormat(layerUtilities.getImageFormat(ur));

                /* attempt to retrieve bounding box */
                List<Double> bbox = layerUtilities.getBBox(ur);
                if (bbox != null) {
                    MapLayerMetadata md = new MapLayerMetadata();
                    md.setBbox(bbox);
                    md.setMoreInfo(ur);
                    mapLayer.setMapLayerMetadata(md);

                }

                /* we don't want our user to have to type loads
                 * when adding a new layer so we just assume/generate
                 * values for the id and description
                 */
                String name = (nameAutomatically.isChecked())
                        ? "" : Validate.escapeHtmlAndTrim(label.getValue());
                mapLayer.setId(uri + name.replaceAll("\\s+", ""));
                mapLayer.setDescription(name);

                // wms version
                String version = layerUtilities.getVersionValue(ur);
                mapLayer.setType(layerUtilities.internalVersion(version));

                getMapComposer().addUserDefinedLayerToMenu(mapLayer, true);

                updateResult("wms_server_added");

                // addWMSServer handles showing any errors for us
            } catch (Exception e) {
                LOGGER.error("failed to get layer from url", e);
            }
        }

    }

    public void onClick$selectLayersButton() {
        LOGGER.debug("onClick$selectLayersButton");

        /*
         * only check that the user has entered a uri to do getcaps on they can
         * type the label later if they missed it
         */


        if (validateAutomaticModeCommon()) {

            try {
                wmsServer = new WebMapServer(new URL(uri.getValue()));
                discoveredLayer = wmsServer.getCapabilities();

                // want list of all
                if (discoveredLayer != null) {
                    availableLayers = new ArrayList<String[]>();
                    //skip first layer,
                    for (int i = 1; i < discoveredLayer.getLayerList().size(); i++) {
                        availableLayers.add(new String[]{String.valueOf(i), discoveredLayer.getLayerList().get(i).getName()
                                + "," + discoveredLayer.getLayerList().get(i).getTitle()});
                    }
                    updateAvailableLayers();

                    selectLayers.setVisible(true);
                    selectLayersButton.setDisabled(true);
                    addDiscoveredLayerButton.setVisible(true);
                } else {
                    // there was an error somewhere..
                    getMapComposer().showMessage(CommonData.lang("error_adding_layer"));
                }
            } catch (Exception e) {
                LOGGER.error("map request", e);
            }
        }
    }

    private void updateAvailableLayers() {
        if (!availableLayers.isEmpty()) {
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
        LOGGER.debug("onAddDiscoveredLayer()");

        //  DO NOT SHORT CURCUIT HERE!
        if (validateCommon() & validateAutomaticModeCommon() & validiateAutomaticModeSelectLayer()) {

            String discoveredLayerId = ((String[]) layerName.getSelectedItem().getValue())[0];

            /*
             * we've already interogated the WMS server to find the available
             * layers - we don't need to do this again, all we need to do is
             * select the layer they user chose by looking for the layer name
             */
            Layer targetLayer = discoveredLayer.getLayerList().get(Integer.parseInt(discoveredLayerId));

            if (targetLayer != null) {
                MapLayer mapLayer = new MapLayer();
                mapLayer.setName(targetLayer.getName());

                GetMapRequest mapRequest = wmsServer.createGetMapRequest();
                mapRequest.addLayer(targetLayer);
                LOGGER.debug(mapRequest.getFinalURL());
                mapRequest.setFormat(StringConstants.IMAGE_PNG);

                String url = mapRequest.getFinalURL().toString();
                mapLayer.setUri(url);
                mapLayer.setLayer(targetLayer.getName());
                mapLayer.setOpacity(opacitySlider.getCurpos() / 100.0f);
                mapLayer.setImageFormat(layerUtilities.getImageFormat(url));

                /* attempt to retrieve bounding box */
                List<Double> bbox = layerUtilities.getBBox(url);
                if (bbox != null) {
                    CRSEnvelope e = targetLayer.getLatLonBoundingBox();
                    bbox.set(0, e.getMinX());
                    bbox.set(1, e.getMinY());
                    bbox.set(2, e.getMaxX());
                    bbox.set(3, e.getMaxY());
                    MapLayerMetadata md = new MapLayerMetadata();
                    md.setBbox(bbox);
                    md.setMoreInfo(targetLayer.getName() + "\n" + makeMetadataHtml(targetLayer));
                    mapLayer.setMapLayerMetadata(md);
                }

                /* we don't want our user to have to type loads
                 * when adding a new layer so we just assume/generate
                 * values for the id and description
                 */
                String name = (nameAutomatically.isChecked())
                        ? targetLayer.getName() + ", " + targetLayer.getTitle() : Validate.escapeHtmlAndTrim(label.getValue());
                mapLayer.setId(url + name.replaceAll("\\s+", ""));
                mapLayer.setDescription(name);

                // wms version
                String version = layerUtilities.getVersionValue(url);
                mapLayer.setType(layerUtilities.internalVersion(version));

                getMapComposer().addUserDefinedLayerToMenu(mapLayer, true);

                // all sweet
                updateResult(StringConstants.WMS_LAYER_ADDED);

                // remove the layer from available layers to stop it being 
                // added twice
                removeLayer(discoveredLayerId);
                updateAvailableLayers();
            } else {

                getMapComposer().showMessage(CommonData.lang("error_selecting_layer")
                        + ": '" + discoveredLayerId + "'");
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
        LOGGER.debug("onAddManualLayer()");

        // DO NOT SHORT CURCUIT HERE!
        if (validateCommon() & validateManualMode()) {
            LOGGER.debug("adding" + getMapUri.getValue());

            if (getMapComposer().addWMSLayer("wmslayer",
                    (nameAutomatically.isChecked())
                            ? layerUtilities.getLayers(getMapUri.getValue()) : Validate.escapeHtmlAndTrim(label.getValue()),
                    getMapUri.getValue(), (float) 0.75, "", "", LayerUtilitiesImpl.WMS_1_1_1, "", "") != null) {
                // all sweet
                updateResult(StringConstants.WMS_LAYER_ADDED);
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
    }

    public float getOpacity() {
        return (float) opacitySlider.getCurpos() / 100f;
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
        valid = layerName.getSelectedItem() != null && !Validate.empty(((String[]) layerName.getSelectedItem().getValue())[0]);
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

    String makeMetadataHtml(Layer layer) {
        StringBuilder sb = new StringBuilder();

        sb.append("<html>");
        sb.append("<br>name: ").append(layer.getName());
        sb.append("<br>");
        sb.append("title: ").append(layer.getTitle());
        sb.append("<br>");
        sb.append("abstract: ").append(layer.get_abstract());
        sb.append("<br>");
        sb.append("keywords: ");
        if (layer.getKeywords() != null) {
            for (String s : layer.getKeywords()) {
                sb.append(s).append(", ");
            }
        }
        if (layer.getMetadataURL() != null && !layer.getMetadataURL().isEmpty()) {
            sb.append("<br>");
            sb.append("metadata URL: ");
            for (MetadataURL url : layer.getMetadataURL()) {
                sb.append("<a target='_blank' href='").append(url.getUrl().toString().replace("'", "''"))
                        .append("'>").append(StringEscapeUtils.escapeHtml(url.getUrl().toString())).append("</a>, ");
            }
        }
        sb.append("<br>");
        String bbox = layer.getLatLonBoundingBox().getMinX() + " " + layer.getLatLonBoundingBox().getMinY()
                + "," + layer.getLatLonBoundingBox().getMaxX() + " " + layer.getLatLonBoundingBox().getMaxY();
        sb.append("bounding box: ").append(bbox);

        sb.append("</html>");
        return sb.toString();
    }

    public void onClick$useAllLink1() {
        uri.setText(linkall1.getValue());
    }

    public void onClick$useAllLink2() {
        uri.setText(linkall2.getValue());
    }

    public void onClick$useAllLink3() {
        uri.setText(linkall3.getValue());
    }

    public void onClick$useAllLink4() {
        uri.setText(linkall4.getValue());
    }

    public void onClick$useSingleLink1() {
        getMapUri.setText(linksingle1.getValue());
    }
}
