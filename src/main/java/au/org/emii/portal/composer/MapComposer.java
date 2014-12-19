package au.org.emii.portal.composer;

import au.com.bytecode.opencsv.CSVReader;
import au.org.ala.spatial.StringConstants;
import au.org.ala.spatial.composer.sandbox.SandboxPasteController;
import au.org.ala.spatial.composer.species.SpeciesAutoCompleteComponent;
import au.org.ala.spatial.dto.ScatterplotDataDTO;
import au.org.ala.spatial.dto.UserDataDTO;
import au.org.ala.spatial.dto.WKTReducedDTO;
import au.org.ala.spatial.logger.RemoteLogger;
import au.org.ala.spatial.util.*;
import au.org.emii.portal.databinding.ActiveLayerRenderer;
import au.org.emii.portal.javascript.OpenLayersJavascript;
import au.org.emii.portal.lang.LanguagePack;
import au.org.emii.portal.menu.HasMapLayer;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.menu.MapLayerMetadata;
import au.org.emii.portal.menu.SelectedArea;
import au.org.emii.portal.net.HttpConnection;
import au.org.emii.portal.session.PortalSession;
import au.org.emii.portal.settings.Settings;
import au.org.emii.portal.util.LayerUtilitiesImpl;
import au.org.emii.portal.util.PortalSessionUtilities;
import au.org.emii.portal.util.RemoteMap;
import au.org.emii.portal.value.BoundingBox;
import au.org.emii.portal.web.SessionInitImpl;
import au.org.emii.portal.wms.WMSStyle;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.DataHolder;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.xml.DomDriver;
import com.thoughtworks.xstream.mapper.MapperWrapper;
import com.thoughtworks.xstream.persistence.FilePersistenceStrategy;
import com.thoughtworks.xstream.persistence.PersistenceStrategy;
import com.thoughtworks.xstream.persistence.XmlArrayList;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.ala.layers.intersect.SimpleShapeFile;
import org.ala.layers.legend.Facet;
import org.ala.layers.legend.LegendObject;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.*;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.ForwardEvent;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zk.ui.util.SessionInit;
import org.zkoss.zul.Button;
import org.zkoss.zul.*;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Window;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.awt.*;
import java.io.*;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.*;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Pattern;

/**
 * ZK composer for the index.zul page
 *
 * @author geoff
 */
public class MapComposer extends GenericAutowireAutoforwardComposer {

    public static final int DEFAULT_POINT_SIZE = 3;
    public static final float DEFAULT_POINT_OPACITY = 0.6f;
    private static final Logger LOGGER = Logger.getLogger(MapComposer.class);
    private static final long serialVersionUID = 1L;

    private String featuresCSV;
    private MapLayer llc2MapLayer;
    private Query downloadSecondQuery = null;
    private String[] downloadSecondLayers = null;
    private Listbox activeLayersList;
    private ActiveLayerRenderer activeLayerRenderer = null;
    private HtmlMacroComponent contextualMenu;
    private int mapZoomLevel = 4;
    private Map activeLayerMapProperties;
    private Label lblSelectedLayer;
    private String useSpeciesWMSCache = "off";
    private List<LayerSelection> selectedLayers = new ArrayList<LayerSelection>();
    /*
     * for capturing layer loaded events signaling listeners
     */
    private String tbxLayerLoaded;
    private Map<String, EventListener> layerLoadedChangeEvents = new HashMap<String, EventListener>();
    private RemoteLogger remoteLogger;
    private Textbox currentLayerExtent;
    /**
     * A simple message dialogue to display over AnalysisToolComposer
     *
     * @param message Full text of message to show
     */
    private boolean mp = true;
    private EventListener layerLegendNameRefresh = null;
    private EventListener contextualMenuRefreshListener;
    private EventListener facetsOpenListener = null;
    private Properties settingsSupplementary = null;
    private RemoteMap remoteMap = null;
    /*
     * Autowired controls
     */
    private Iframe rawMessageIframeHack;
    private Div rawMessageHackHolder;
    private Div layerControls;
    /*
     * User data object to allow for the saving of maps and searches
     */
    private LanguagePack languagePack = null;
    private OpenLayersJavascript openLayersJavascript = null;
    private HttpConnection httpConnection = null;
    private PortalSessionUtilities portalSessionUtilities = null;
    private Settings settings = null;
    private Pattern layerNameParamPattern = Pattern.compile("ly\\.[0-9]{1,}");

    public void onClick$removeAllLayers() {
        if (safeToPerformMapAction()) {
            List<MapLayer> activeLayers = getPortalSession().getActiveLayers();
            StringBuilder script = new StringBuilder();
            while (activeLayers.size() > 1) {
                MapLayer mapLayer = activeLayers.get(0);

                if (mapLayer.getType() != LayerUtilitiesImpl.MAP) {
                    script.append(openLayersJavascript.removeMapLayer(mapLayer));

                    // skip executing JS and reseting the layer controls - do
                    // them at the end
                    deactiveLayer(mapLayer, false, false);
                }
            }
            updateLayerControls();
            refreshContextualMenu();
            openLayersJavascript.execute(
                    openLayersJavascript.getIFrameReferences()
                            + script.toString());
        }
    }

    public void safeToLoadMap(Event event) {
        mapLoaded(StringConstants.TRUE);

        //listen for map extents changes
        EventListener el = new EventListener() {

            public void onEvent(Event event) throws Exception {
                onReloadLayers(null);
            }
        };
        getLeftmenuSearchComposer().addViewportEventListener("onReloadLayers", el);
    }

    public void zoomToExtent(MapLayer selectedLayer) {
        if (selectedLayer != null) {
            LOGGER.debug("zooming to extent " + selectedLayer.getId());
            if (selectedLayer.getType() == LayerUtilitiesImpl.GEOJSON
                    || selectedLayer.getType() == LayerUtilitiesImpl.WKT
                    || selectedLayer.getType() == LayerUtilitiesImpl.KML) {
                openLayersJavascript.zoomGeoJsonExtentNow(selectedLayer);
            } else {
                openLayersJavascript.zoomLayerExtent(selectedLayer);
            }
        }
    }

    public void applyChange(MapLayer selectedLayer) {
        if (selectedLayer != null) {
            /*
             * different path for each type layer 1. symbol 2. classification
             * legend 3. prediction legend 4. other (wms)
             */
            if (selectedLayer.isDynamicStyle()) {

                Color c = new Color(selectedLayer.getRedVal(), selectedLayer.getGreenVal(), selectedLayer.getBlueVal());
                String hexColour = Integer.toHexString(c.getRGB() & 0x00ffffff);
                String rgbColour = "rgb(" + selectedLayer.getRedVal() + "," + selectedLayer.getGreenVal()
                        + "," + selectedLayer.getBlueVal() + ")";
                selectedLayer.setEnvColour(rgbColour);

                if (selectedLayer.getType() == LayerUtilitiesImpl.GEOJSON) {
                    openLayersJavascript.redrawFeatures(selectedLayer);
                } else if (selectedLayer.getType() == LayerUtilitiesImpl.WKT) {
                    openLayersJavascript.redrawWKTFeatures(selectedLayer);
                } else {
                    String envString = "";
                    if ("-1".equals(selectedLayer.getColourMode())) {
                        envString += "color:" + hexColour;
                    } else {
                        LegendObject lo = selectedLayer.getLegendObject();
                        if (lo != null && lo.getColourMode() != null) {
                            envString += "colormode:" + lo.getColourMode();
                        } else {
                            envString += "colormode:" + selectedLayer.getColourMode();
                        }
                    }
                    envString += ";name:circle;size:" + selectedLayer.getSizeVal();

                    //Opacity now handled only by openlayers
                    if ((selectedLayer.getHighlight() == null || selectedLayer.getHighlight().length() == 0
                            || !StringConstants.GRID.equals(selectedLayer.getColourMode()))
                            && selectedLayer.getSizeUncertain()) {
                        envString += ";uncertainty:1";
                    }
                    selectedLayer.setEnvParams(envString + ";opacity:1");

                    if (selectedLayer.hasChildren()) {
                        MapLayer highlightLayer = selectedLayer.getChild(0);
                        if (highlightLayer.getName().equals(selectedLayer.getName() + "_highlight")) {
                            //apply sel to envString
                            String highlightEnv = "color:000000;size:" + selectedLayer.getSizeVal() + ";opacity:0";
                            highlightLayer.setOpacity(1);
                            if (selectedLayer.getHighlight() != null && selectedLayer.getHighlight().length() > 0
                                    && !StringConstants.GRID.equals(selectedLayer.getColourMode())) {
                                if (selectedLayer.getSpeciesQuery() instanceof UserDataQuery) {
                                    try {
                                        highlightLayer.setEnvParams(highlightEnv + ";sel:"
                                                + selectedLayer.getHighlight().replace(";", "%3B"));
                                    } catch (Exception e) {
                                        LOGGER.error("error encoding highlight to UTF-8: " + selectedLayer.getHighlight(), e);
                                    }
                                } else {
                                    highlightLayer.setEnvParams(highlightEnv + ";sel:" + selectedLayer.getHighlight().replace(";", "%3B"));
                                }
                                highlightLayer.setHighlightState("show");
                            } else {
                                highlightLayer.setHighlightState("hide");
                                highlightLayer.setEnvParams(highlightEnv);
                            }
                        }
                    }

                    if (selectedLayer.isDisplayed()) {
                        reloadMapLayerNowAndIndexes(selectedLayer);
                    }
                }
            } else if (selectedLayer.getSelectedStyle() != null) {
                /*
                 * 1. classification legend has uri with ".zul" content 2.
                 * prediction legend works here *
                 */
                LOGGER.debug("******** is this ever reached? **********");
                selectedLayer.setOpacity(selectedLayer.getOpacity());
                String legendUri = selectedLayer.getSelectedStyle().getLegendUri();
                if (legendUri.contains(".zul")) {
                    //bbox is null, not required for redraw
                    addImageLayer(selectedLayer.getId(),
                            selectedLayer.getName(),
                            selectedLayer.getUri(),
                            selectedLayer.getOpacity(),
                            null, LayerUtilitiesImpl.ALOC);
                } else {
                    //redraw
                    if (selectedLayer.isDisplayed()) {
                        reloadMapLayerNowAndIndexes(selectedLayer);
                    }
                }
            } else {
                //redraw wms layer if opacity changed
                if (selectedLayer.isDisplayed()) {
                    reloadMapLayerNowAndIndexes(selectedLayer);
                }
            }
        }
    }

    public void reloadMapLayerNowAndIndexes(MapLayer selectedLayer) {
        if (safeToPerformMapAction()) {
            PortalSession portalSession = (PortalSession) Executions.getCurrent().getDesktop().getSession().getAttribute(StringConstants.PORTAL_SESSION);

            openLayersJavascript.execute(
                    openLayersJavascript.getIFrameReferences()
                            + openLayersJavascript.reloadMapLayer(selectedLayer)
                            + openLayersJavascript.updateMapLayerIndexes(
                            portalSession.getActiveLayers()));
        }
    }

    /**
     * Maps a species based on the selected item in the supplied species auto
     * complete component.
     *
     * @param sacc
     * @param sa
     * @param geospatialKosher
     */
    public void mapSpeciesFromAutocompleteComponent(SpeciesAutoCompleteComponent sacc, SelectedArea sa, boolean[] geospatialKosher, boolean mapExpertDistributions) {
        if (!sacc.hasValidAnnotatedItemSelected()) {
            return;
        }
        String[] details = sacc.getSelectedTaxonDetails();
        if (details.length > 0) {
            String taxon = details[0];
            String rank = details[1];
            Query query = sacc.getQuery((Map) getSession().getAttribute(StringConstants.USERPOINTS), false, geospatialKosher);
            Query q = QueryUtil.queryFromSelectedArea(query, sa, false, geospatialKosher);
            String wkt = sa == null ? null : sa.getWkt();
            mapSpecies(q, taxon, rank, 0, LayerUtilitiesImpl.SPECIES, wkt, -1, DEFAULT_POINT_SIZE, DEFAULT_POINT_OPACITY, Util.nextColour(), mapExpertDistributions);
            LOGGER.debug(">>>>> " + taxon + ", " + rank + " <<<<<");
        }
    }

    /**
     * Reorder the active layers list based on a d'n'd event
     *
     * @param dragged
     * @param dropped
     */
    public void reorderList(Listitem dragged, Listitem dropped) {
        LOGGER.debug(dragged.getLabel() + " dropped on " + dropped.getLabel());

        // get the position in the list
        List<MapLayer> activeLayers = getPortalSession().getActiveLayers();
        int droppedIndex = activeLayers.indexOf(dropped.getValue());

        ListModelList model = (ListModelList) activeLayersList.getModel();
        model.remove(dragged.getValue());
        model.add(droppedIndex, dragged.getValue());

        // tell openLayers to change zIndexs
        openLayersJavascript.updateMapLayerIndexesNow(activeLayers);

        redrawLayersList();

        // hide legend controls
        refreshContextualMenu();
    }

    /**
     * Remove a maplayer from the active layers list and then reinsert it at the
     * same spot - should cause this part of the list to be re-rendered.
     * <p/>
     * After re-rendering, reselect the corresponding item in its listbox, as
     * operations such as changing opacity and animation require a layer is
     * selected
     *
     * @param mapLayer
     */
    public void refreshActiveLayer(MapLayer mapLayer) {
        ListModelList model = (ListModelList) activeLayersList.getModel();
        int index = model.indexOf(mapLayer);
        if (index >= 0) {
            model.remove(index);
            model.add(index, mapLayer);

            activeLayersList.setSelectedIndex(index);
        }

        adjustActiveLayersList();
    }

    public void activateLink(String uri, String label, boolean isExternal) {
        activateLink(uri, label, isExternal, "");
    }

    public void activateLink(String uri, String label, boolean isExternal, String downloadPid) {
        closeExternalContentWindow();

        Window externalContentWindow = (Window) Executions.createComponents("WEB-INF/zul/ExternalContent.zul", layerControls, null);

        if (isExternal) {
            // change browsers current location
            Clients.evalJavaScript("window.location.href ='" + uri + "';");
        } else {

            Iframe iframe = (Iframe) externalContentWindow.getFellow("externalContentIframe");
            Html html = (Html) externalContentWindow.getFellow("externalContentHTML");

            String newUri = uri;
            if (newUri.charAt(0) == '*') {
                //html content
                newUri = newUri.substring(1);

                //url
                iframe.setHeight("0px");
                iframe.setSrc("");

                String content;
                if ("download".equalsIgnoreCase(label)) {
                    // Added fast download option -
                    // TODO refactor so this can be generated from same code that sets the downloadUrl (uri) in BiocacheQuery.java
                    String fastDownloadUrl = newUri.replaceFirst("/occurrences/download", "/occurrences/index/download");

                    StringBuilder sbContent = new StringBuilder();
                    sbContent.append("<p id='termsOfUseDownload' style='padding:10px; margin-bottom: 0;'>");
                    sbContent.append("By downloading this content you are agreeing to use it in accordance ");
                    sbContent.append("with the Atlas of Living Australia <a href='http://www.ala.org.au/about/terms-of-use/#TOUusingcontent'>Terms of Use</a>");
                    sbContent.append(" and any Data Provider Terms associated with the data download. ");
                    sbContent.append("<br/><br/>");
                    sbContent.append("Please provide the following details before downloading (* required)");
                    sbContent.append("</p>");
                    sbContent.append("    <form id='downloadForm' onSubmit='downloadSubmitButtonClick(); return false;' style='padding:10px;'>");
                    sbContent.append("        <input type='hidden' name='url' id='downloadUrl' value='").append(uri).append("'/>");
                    sbContent.append("        <input type='hidden' name='url' id='fastDownloadUrl' value='").append(fastDownloadUrl).append("'/>");
                    sbContent.append("        <fieldset>");
                    sbContent.append("            <p><label for='email'>Email</label>");
                    sbContent.append("                <input type='text' name='email' id='email' value='' size='30'  />");
                    sbContent.append("            </p>");
                    sbContent.append("            <p><label for='filename'>File Name</label>");
                    sbContent.append("                <input type='text' name='filename' id='filename' value='data' size='30'  />");
                    sbContent.append("            </p>");

                    sbContent.append("            <p><label for='reasonTypeId' style='vertical-align: top'>Download Reason *</label>");
                    sbContent.append("            <select name='reasonTypeId' id='reasonTypeId'>");
                    sbContent.append("            <option value=''>-- select a reason --</option>");
                    JSONArray dlreasons = CommonData.getDownloadReasons();
                    for (int i = 0; i < dlreasons.size(); i++) {
                        JSONObject dlr = dlreasons.getJSONObject(i);
                        sbContent.append("            <option value='").append(dlr.getInt(StringConstants.ID)).append("'>").append(dlr.getString(StringConstants.NAME)).append("</option>");
                    }
                    sbContent.append("            <select></p>");
                    sbContent.append("                    <input style='display:none' type='radio' name='downloadType' value='fast' class='tooltip' checked='checked' title='Faster download but fewer fields are included'>");

                    sbContent.append("            <p style='clear:both;'>&nbsp;</p>");
                    sbContent.append("            <p style='text-align:center;'><input type='submit' value='Download All Records' id='downloadSubmitButton'/></p>");

                    sbContent.append("        </fieldset>");
                    sbContent.append("    </form>");

                    content = sbContent.toString();
                } else {
                    content = newUri;
                }

                //content
                html.setContent(content);
                html.setStyle("overflow: scroll;padding: 0 10px;");

                //for the 'reset window' button
                ((ExternalContentComposer) externalContentWindow).setSrc("");

                //update linked button
                externalContentWindow.getFellow(StringConstants.BREAKOUT).setVisible(false);

                externalContentWindow.setContentStyle("overflow:auto");
            } else {
                //url
                iframe.setHeight("100%");
                iframe.setSrc(newUri);

                //content
                html.setContent("");

                //for the 'reset window' button
                ((ExternalContentComposer) externalContentWindow).setSrc(newUri);

                //update linked button
                ((Toolbarbutton) externalContentWindow.getFellow(StringConstants.BREAKOUT)).setHref(newUri);
                externalContentWindow.getFellow(StringConstants.BREAKOUT).setVisible(true);

                externalContentWindow.setContentStyle("overflow:visible");
            }

            if (StringUtils.isNotBlank(downloadPid)) {
                String downloadUrl = CommonData.getSatServer() + "/ws/download/" + downloadPid;
                if (downloadPid.startsWith("http")) {
                    downloadUrl = downloadPid;
                }
                ((Toolbarbutton) externalContentWindow.getFellow("download")).setHref(downloadUrl);
                externalContentWindow.getFellow("download").setVisible(true);
            } else {
                ((Toolbarbutton) externalContentWindow.getFellow("download")).setHref("");
                externalContentWindow.getFellow("download").setVisible(false);
            }

            // use the link description as the popup caption
            ((Caption) externalContentWindow.getFellow("caption")).setLabel(
                    label);
            externalContentWindow.setPosition("center");
            try {
                externalContentWindow.doModal();
            } catch (Exception e) {
                LOGGER.error("error opening information popup", e);
            }
        }
    }

    public boolean activateLayer(MapLayer mapLayer, boolean doJavaScript) {
        return activateLayer(mapLayer, doJavaScript, false);
    }

    /**
     * Activate a map layer on the map
     *
     * @param doJavaScript set false to defer execution of JavaScript which
     *                     actually adds the layer to the openlayers menu
     * @return true if the layer was added successfully, otherwise false
     */
    public boolean activateLayer(MapLayer mapLayer, boolean doJavaScript, boolean skipTree) {
        List<MapLayer> activeLayers = getPortalSession().getActiveLayers();
        boolean layerAdded = false;

        /*
         * switch to the ListModelList if we are currently using simplelistmodel
         * to display the 'no layers selected' message
         *
         * If the model is already an instance of ListModelList then the model
         * should already have the data it needs so just fire the update event.
         */
        if (!(activeLayersList.getModel() instanceof ListModelList)) {
            LOGGER.debug("changing model for Active Layers to ListModelList");
            /*
             * this is the first item being added to the list so we make it a
             * new ListModelList instance based on live data
             */
            activeLayersList.setModel(new ListModelList(activeLayers, true));

        }

        if (!activeLayers.contains(mapLayer)) {

            /*
             * assume we want to display on the map straight away - set checkbox
             * to true
             */
            activeLayersList.setItemRenderer(activeLayerRenderer);


            /*
             * use the MODEL facade to add the new layer (it's not smart enough
             * to detect the change otherwise.
             *
             * We always add to the top of the list so that newly actived map
             * layers display above existing ones
             */
            ((ListModelList) activeLayersList.getModel()).add(0, mapLayer);

            //select this map layer
            activeLayersList.setSelectedIndex(0);

            // update the map
            if (doJavaScript) {
                openLayersJavascript.activateMapLayerNow(mapLayer);
            }

            updateLayerControls();
            layerAdded = true;
        } else {
            LOGGER.debug(
                    "not displaying map layer because its already listed or is marked non-displayable");
        }

        adjustActiveLayersList();

        refreshContextualMenu();

        return layerAdded;

    }

    /**
     * Remove an item from the list of active layers and put it back in the tree
     * menu of available layers
     *
     * @param itemToRemove
     */
    public void deactiveLayer(MapLayer itemToRemove, boolean updateMapAndLayerControls, boolean recursive) {
        deactiveLayer(itemToRemove, updateMapAndLayerControls, recursive, false);
    }

    public void deactiveLayer(MapLayer itemToRemove, boolean updateMapAndLayerControls, boolean recursive
            , boolean updateOnly) {
        if (itemToRemove != null) {
            Query q = itemToRemove.getSpeciesQuery();
            if (q instanceof UserDataQuery) {
                String pid = q.getQ();

                Map<String, UserDataDTO> htUserSpecies = (Map) getMapComposer().getSession()
                        .getAttribute(StringConstants.USERPOINTS);
                if (htUserSpecies != null) {
                    htUserSpecies.remove(pid);
                }
            }

            // update the active layers list
            List<MapLayer> activeLayers = getPortalSession().getActiveLayers();
            if (activeLayers != null) {
                LOGGER.debug("obtained activelayers arraylist from session, count: " + activeLayers.size());
                if (!activeLayers.isEmpty()) {
                    ListModelList listmodel = (ListModelList) activeLayersList.getModel();
                    if (listmodel != null) {
                        listmodel.remove(itemToRemove);

                        if (activeLayers.isEmpty()) {
                            lblSelectedLayer.setValue("No layers added");
                        }
                    }
                } else {
                    LOGGER.debug("active layers list is empty, so not updating it");
                }
            }

            if (updateMapAndLayerControls) {
                if (!updateOnly) {
                    // tell openlayers to kill the layer
                    openLayersJavascript.removeMapLayerNow(itemToRemove);

                }
                updateLayerControls();
                removeFromSession(itemToRemove.getName());
            }

            // descend children if we were called recursively
            if (recursive && itemToRemove.hasChildren()) {
                for (MapLayer child : itemToRemove.getChildren()) {
                    deactiveLayer(child, updateMapAndLayerControls, true);
                }
            }
        }

        refreshContextualMenu();
    }

    /**
     * Remove an item from the list of active layers and put it back in the tree
     * menu of available layers
     *
     * @param itemToRemove
     */
    public void removeFromList(MapLayer itemToRemove) {
        if (itemToRemove != null) {
            // update the active layers list
            List<MapLayer> activeLayers = getPortalSession().getActiveLayers();
            if (activeLayers != null) {
                LOGGER.debug("obtained activelayers arraylist from session, count: " + activeLayers.size());
                if (!activeLayers.isEmpty()) {
                    ListModelList listmodel = (ListModelList) activeLayersList.getModel();
                    if (listmodel != null) {
                        listmodel.remove(itemToRemove);
                    }
                } else {
                    LOGGER.debug("active layers list is empty, so not updating it");
                }
            }
        }

        refreshContextualMenu();
    }

    /**
     * A simple message dialogue
     *
     * @param message Full text of message to show
     */
    public void showMessage(String message) {
        ErrorMessageComposer window = (ErrorMessageComposer) Executions.createComponents("WEB-INF/zul/ErrorMessage.zul"
                , null, null);
        window.setMessage(message);
        window.doOverlapped();
    }

    public void showMessage(String message, Component parent) {
        ErrorMessageComposer window = (ErrorMessageComposer) Executions.createComponents("WEB-INF/zul/ErrorMessage.zul"
                , parent, null);
        window.setMessage(message);
        if (mp) {
            try {
                window.doModal();
            } catch (Exception e) {
                LOGGER.error("error opening message window", e);
            }
        } else {
            window.doOverlapped();
        }
    }

    /**
     * Show a message dialogue. Initially a short message is shown but the user
     * can click 'show details' to get a more informative message.
     * <p/>
     * A default message title is obtained from the config file
     *
     * @param message
     * @param messageDetail
     */
    public void showMessage(String message, String messageDetail) {
        showMessage(languagePack.getLang("default_message_title"), message, messageDetail);
    }

    /**
     * Show a message dialogue. Initially a short message is shown but the user
     * can click 'show details' to get a more informative message.
     * <p/>
     * A title must be provided for the popup box
     *
     * @param title
     * @param message
     * @param messageDetail
     */
    public void showMessage(String title, String message, String messageDetail) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("messageTitle", title);
        params.put("message", message);
        params.put("messageDetail", messageDetail);

        Window window = (Window) Executions.createComponents("WEB-INF/zul/ErrorMessageWithDetail.zul", null, params);
        window.doOverlapped();
    }

    /**
     * This is a fixed size (because of zk limitations) message dialogue
     * featuring: o title o brief description o detailed description (hidden by
     * default) o link to raw data (hidden by default) o Iframe full of raw data
     * (hidden by default) There are lots of parameters for this method so if
     * you want to use it, it's easiest to write a wrapper and just call that
     * when you have a problem.
     * <p/>
     * This is not a general purpose error message - everything is fixed sizes
     * (has to be or the iframe doesn't display properly. If you want a general
     * purpose error message, use the showMessageNow() calls
     *
     * @param title           TEXT of the title or null to use default from config file
     * @param message         TEXT in config file of the error message
     * @param messageDetail   TEXT to display if user clicks 'show detail'
     * @param rawMessageTitle TEXT to display before raw output or null to
     *                        ignore
     * @param rawMessage      TEXT of raw error message or null to ignore
     */
    public void showMessage(String title,
                            String message,
                            String messageDetail,
                            String linkTitle,
                            String linkHref,
                            String rawMessageTitle,
                            String rawMessage) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("messageTitle", title);
        params.put("message", message);
        params.put("messageDetail", messageDetail);
        params.put("linkTitle", linkTitle);
        params.put("linkHref", linkHref);
        params.put("rawMessageTitle", rawMessageTitle);

        // this will be the inside of the iframe...

        /*
         * have to store the raw error message in the user's session temporarily
         * to prevent getting a big zk severe error if the iframe is requested
         * after it is supposed to have been dereferenced
         */

        rawMessageIframeHack.setContent(null);

        /*
         * the raw text can't go in with the params or it gets escaped by zk -
         * you have to do weird things with iframes and the media class instead
         */
        Window window;
        window = (Window) Executions.createComponents("WEB-INF/zul/ErrorMessageWithDetailAndRawData.zul", null, params);

        /*
         * now we grab the hidden iframe in index.zul and move it into our
         * message box - again this is to prevent a massive zk error if the
         * iframe content is requested after it's supposed to have gone
         */
        Component holder = window.getFellow("rawMessageHolder");
        rawMessageIframeHack.setParent(holder);
        window.doOverlapped();

        /*
         * at this point the user has closed the message box - we only have one
         * more thing to do to stop the big zk error and that is to grab the
         * iframe back off the error message window and put it back where we
         * found it in the index.zul page - not pretty or efficient but works a
         * treat!
         */
        rawMessageIframeHack.setParent(rawMessageHackHolder);
    }

    /**
     * Add a map layer to the user defined map layers group (My Layers)
     *
     * @param mapLayer
     */
    public void addUserDefinedLayerToMenu(MapLayer mapLayer, boolean activate) {
        if (safeToPerformMapAction()) {
            // activate the layer in openlayers and display in active layers without
            // updating the tree (because its not displayed)
            activateLayer(mapLayer, true, true);

            LOGGER.debug("leaving addUserDefinedLayerToMenu");
        }
    }

    /**
     * Initial building of the tree menu and active layers list based on the
     * values obtained from the current session.
     * <p/>
     * JavaScript for loading default map layers and setting the default zoombox
     * is in SessionInit.java
     */
    public void load() {
        LOGGER.debug("entering loadMapLayers");

        PortalSession portalSession = getPortalSession();
        List<MapLayer> activeLayers = portalSession.getActiveLayers();

        // model and renderer for active layers list
        ListModelList activeLayerModel = new ListModelList(activeLayers, true);

        // tell the list about them...
        if (activeLayers.isEmpty()) {
            MapLayer ml = remoteMap.createLocalLayer(LayerUtilitiesImpl.MAP, "Map options");
            ml.setRemoveable(false);
            activeLayers.add(ml);
        }

        activeLayersList.setModel(activeLayerModel);
        activeLayersList.setItemRenderer(activeLayerRenderer);
        activeLayersList.setSelectedIndex(activeLayerModel.size() - 1);

        updateLayerControls();
        refreshContextualMenu();

    }

    /**
     * Adds a object as a layer to the map.
     *
     * @param pid
     */
    public MapLayer addObjectByPid(String pid, String displayName) {

        JSONObject obj = JSONObject.fromObject(Util.readUrl(CommonData.getLayersServer() + "/object/" + pid));
        //add feature to the map as a new layer
        String areaName = obj.getString(StringConstants.NAME);
        MapLayer mapLayer = getMapComposer().addWMSLayer("PID:" + pid, displayName == null ? areaName : displayName
                , obj.getString(StringConstants.WMSURL), 0.6f, null, null, LayerUtilitiesImpl.WKT, null, null);
        if (mapLayer == null) {
            return null;
        }
        mapLayer.setPolygonLayer(true);

        //if the layer is a point create a radius

        String bbox = obj.getString(StringConstants.BBOX);
        String fid = obj.getString(StringConstants.FID);

        MapLayerMetadata md = mapLayer.getMapLayerMetadata();

        Facet facet = null;
        if (CommonData.getLayer(fid) != null && CommonData.getFacetLayerName(fid) != null) {
            JSONObject field = JSONObject.fromObject(Util.readUrl(CommonData.getLayersServer() + "/field/" + fid + "?pageSize=0"));

            if (field.containsKey("indb") && StringConstants.TRUE.equalsIgnoreCase(field.getString("indb"))) {
                String spid = field.getString("spid");
                md.setMoreInfo(CommonData.getLayersServer() + "/layers/view/more/" + spid);

                facet = Util.getFacetForObject(areaName, fid);
            }
        }

        try {
            double[][] bb = SimpleShapeFile.parseWKT(bbox).getBoundingBox();
            List<Double> dbb = new ArrayList<Double>();
            dbb.add(bb[0][0]);
            dbb.add(bb[0][1]);
            dbb.add(bb[1][0]);
            dbb.add(bb[1][1]);
            md.setBbox(dbb);
        } catch (Exception e) {
            LOGGER.debug("failed to parse: " + bbox, e);
        }


        if (facet != null) {
            List<Facet> facets = new ArrayList<Facet>();
            facets.add(facet);
            mapLayer.setFacets(facets);
            mapLayer.setWKT(Util.readUrl(CommonData.getLayersServer() + "/shape/wkt/" + pid));
        } else {
            //not in biocache, so add as WKT
            mapLayer.setWKT(Util.readUrl(CommonData.getLayersServer() + "/shape/wkt/" + pid));
        }

        mapLayer.setRedVal(255);
        mapLayer.setGreenVal(0);
        mapLayer.setBlueVal(0);
        mapLayer.setDynamicStyle(true);
        getMapComposer().updateLayerControls();

        return mapLayer;
    }

    /**
     * Add a WMS layer identified by the given parameters to the menu system and
     * activate it
     *
     * @param name    Name of map layer
     * @param uri     URI for the WMS service
     * @param opacity 0 for invisible, 1 for solid
     */
    public MapLayer addWMSLayer(String name, String displayName, String uri, float opacity, String metadata
            , String legendUri, int subType, String cqlfilter, String envParams) {
        return addWMSLayer(name, displayName, uri, opacity, metadata, legendUri, subType, cqlfilter, envParams, null);
    }

    /*
     * Public utility methods to interogate the state of the form controls
     */

    public MapLayer addWMSLayer(String name, String displayName, String uri, float opacity, String metadata
            , String legendUri, int subType, String cqlfilter, String envParams, Query q) {
        MapLayer mapLayer = null;
        if (safeToPerformMapAction()) {
            if (portalSessionUtilities.getUserDefinedById(getPortalSession(), uri) == null) {
                mapLayer = remoteMap.createAndTestWMSLayer(name, uri, opacity);
                mapLayer.setDisplayName(displayName);
                if (q != null) {
                    mapLayer.setSpeciesQuery(q);
                }

                //ok
                mapLayer.setSubType(subType);
                mapLayer.setCql(cqlfilter);
                mapLayer.setEnvParams(envParams);

                String newUri = CommonData.getGeoServer()
                        + "/wms?REQUEST=GetLegendGraphic&VERSION=1.0.0&FORMAT=image/png&WIDTH=20&HEIGHT=9&LAYER="
                        + mapLayer.getLayer();
                mapLayer.setDefaultStyleLegendUri(newUri);

                if (metadata != null && metadata.startsWith("http")) {
                    mapLayer.getMapLayerMetadata().setMoreInfo(metadata + "\n" + displayName);
                } else {
                    mapLayer.getMapLayerMetadata().setMoreInfo(metadata);
                }

                if (legendUri != null) {
                    WMSStyle style = new WMSStyle();
                    style.setName(StringConstants.DEFAULT);
                    style.setDescription("Default style");
                    style.setTitle(StringConstants.DEFAULT);
                    style.setLegendUri(legendUri);
                    mapLayer.addStyle(style);
                    mapLayer.setSelectedStyleIndex(1);
                    LOGGER.debug("adding WMSStyle with legendUri: " + legendUri);
                    mapLayer.setDefaultStyleLegendUriSet(true);
                }

                addUserDefinedLayerToMenu(mapLayer, true);

            } else {
                // fail
                showMessage(languagePack.getLang("wms_layer_already_exists"));
                LOGGER.debug(
                        "refusing to add a new layer with URI " + uri
                                + " because it already exists in the menu");
            }
        }
        return mapLayer;
    }

    public MapLayer getMapLayer(String label) {
        // check if layer already present
        List udl = getPortalSession().getActiveLayers();
        Iterator iudl = udl.iterator();
        LOGGER.debug("session active layers: " + udl.size() + " looking for: " + label);
        while (iudl.hasNext()) {
            MapLayer ml = (MapLayer) iudl.next();
            LOGGER.debug("layer: " + ml.getName() + " - " + ml.getId() + " - " + ml.getNameJS());
            if (ml.getName().equals(label)) {
                return ml;
            }
        }

        // now check if we can find it using the display name
        return getMapLayerDisplayName(label);
    }

    public int getMapLayerIdxInLegend(String label) {
        // check if layer already present
        List udl = getPortalSession().getActiveLayers();
        Iterator iudl = udl.iterator();
        LOGGER.debug("session active layers: " + udl.size() + " looking for: " + label);
        int pos = -1;
        while (iudl.hasNext()) {
            pos++;
            MapLayer ml = (MapLayer) iudl.next();
            LOGGER.debug("layer: " + ml.getName() + " - " + ml.getId() + " - " + ml.getNameJS());
            if (ml.getName().equals(label)) {
                return pos;
            }
        }

        // now check if we can find it using the display name
        return 0;
    }

    public MapLayer getMapLayerDisplayName(String label) {
        // check if layer already present
        List udl = getPortalSession().getActiveLayers();
        Iterator iudl = udl.iterator();
        LOGGER.debug("session active layers: " + udl.size() + " looking for: " + label);
        while (iudl.hasNext()) {
            MapLayer ml = (MapLayer) iudl.next();
            LOGGER.debug("layer: " + ml.getName() + " - " + ml.getId() + " - " + ml.getNameJS());
            if (ml.getDisplayName().equals(label)) {
                return ml;
            }
        }

        return null;
    }

    public void removeLayer(String label) {
        if (safeToPerformMapAction()) {
            MapLayer mapLayer = getMapLayer(label);
            if (mapLayer != null) {
                deactiveLayer(mapLayer, true, false);
            } else {
                // fail
                showMessage(languagePack.getLang("wms_layer_remove_error"));
                LOGGER.debug("unable to remove layer with label" + label);
            }
        }
    }

    public MapLayer addImageLayer(String id, String label, String uri, float opacity, List<Double> bbox, int subType) {
        // check if layer already present
        MapLayer imageLayer = getMapLayer(label);

        if (safeToPerformMapAction()) {
            if (imageLayer == null) {
                LOGGER.debug("activating new layer");

                //start with a new MapLayer
                imageLayer = new MapLayer();
                //set its type
                imageLayer.setType(LayerUtilitiesImpl.IMAGELAYER);
                //the name is what will appear in the active layer list
                imageLayer.setName(label);

                imageLayer.setSubType(subType);

                //where do i find the image at the moment it is only enabled for png
                imageLayer.setId(label);

                //the combination of the next two is used by openlayers to create a unique name
                //its a bit dull using the ural and layer name but its a hangover from thinking
                //everything in the world is a WMS layer
                imageLayer.setLayer("wms.png");
                imageLayer.setUri(uri);

                //set the layers opacity 0 - 1
                imageLayer.setOpacity(opacity);

                //need the bbox so the map knows where to put it
                //bbox info is stored in here
                imageLayer.getMapLayerMetadata().setBbox(bbox);

                //call this to add it to the map and also put it in the active layer list
                activateLayer(imageLayer, true, true);

            } else {
                LOGGER.debug("refreshing exisiting layer");
                imageLayer.setUri(uri);
                imageLayer.setOpacity(opacity);

                openLayersJavascript.reloadMapLayerNow(imageLayer);
            }
        }

        return imageLayer;
    }

    /**
     * Return the MapLayer instance associated with the item currently selected
     * in the active layers listbox or null if no item is currently selected.
     * <p/>
     * If nothing is selected and alertOnNoSelection is set true, show the user
     * a message and return null
     *
     * @return selected MapLayer instance or null if there is nothing selected
     */
    public MapLayer getActiveLayersSelection(boolean alertOnNoSelection) {
        MapLayer mapLayer = null;

        // only one item can be selected at a time
        Listitem selected = activeLayersList.getSelectedItem();

        if (selected != null) {
            mapLayer = selected.getValue();
        } else if (alertOnNoSelection) {
            showMessage(languagePack.getLang("active_layer_not_selected"));
        }

        return mapLayer;
    }

    public void mapLoaded(String text) {
        boolean loaded = Boolean.parseBoolean(text);

        getPortalSession().setMapLoaded(loaded);

        BoundingBox bb = getPortalSession().getDefaultBoundingBox();

        if (loaded) {
            LOGGER.debug("map is now loaded. let's try mapping.");
            MapLayer ml = loadUrlParameters();

            if (ml == null) {
                openLayersJavascript.useAdditionalScript();
            }
        }

        openLayersJavascript.zoomToBoundingBox(bb, false);
    }

    /**
     * Check if its safe to do things to the map - if it's not, show a popup box
     *
     * @return
     */
    public boolean safeToPerformMapAction() {
        boolean safe;
        if (openLayersJavascript.mapLoaded()) {
            safe = true;
        } else {
            showMessage(languagePack.getLang("map_load_error"));
            safe = false;
        }
        return safe;
    }

    //-- AfterCompose --//
    @Override
    public void afterCompose() {
        super.afterCompose();

        //window settings, for printing
        applyWindowParams();

        // showtime!
        load();

        //active layers list
    }

    /**
     * apply window parameters
     * <p/>
     * p = width in pixels,height in
     * pixels,longitude1,latitude1,longitude2,latitude2
     */
    void applyWindowParams() {
        String s = Executions.getCurrent().getParameter("p");

        //TODO: validate params
        if (s != null) {
            String[] pa = s.split(",");
            setWidth(pa[0] + "px");

            if (pa.length > 1) {
                setHeight(pa[1] + "px");
            }
        }
    }

    public String getCookieValue(String cookieName) {
        try {
            for (Cookie c : ((HttpServletRequest) Executions.getCurrent().getNativeRequest()).getCookies()) {
                if (c.getName().equals(cookieName)) {
                    return c.getValue();
                }
            }
        } catch (Exception e) {
            //does not matter if it does not work
        }

        return null;
    }

    public void mapMultiQueryLayers(boolean[] geospatialKosher, String baseBiocacheUrl, String baseWSBiocacheUrl
            , boolean supportDynamic) {
        Map<String, String> userParams = getQueryParameterMap(Executions.getCurrent().getDesktop().getQueryString());

        if (userParams != null) {
            for (Entry<String, String> entry : userParams.entrySet()) {
                String key = entry.getKey();
                if (layerNameParamPattern.matcher(key).find()) {
                    //we have a layer, retrieve the other bits
                    String layerName = entry.getValue();
                    String query = userParams.get(key + ".q");
                    String style = userParams.get(key + ".s");

                    LOGGER.debug(String.format("Add layer: '%s', query: '%s', style: '%s', key: '%s'", layerName
                            , query, style, key));

                    //format the query
                    if (query != null && query.contains(",")) {
                        String[] queryComponents = query.split(",");
                        query = StringUtils.join(queryComponents, " OR ");
                    }
                    if (query != null && style != null && layerName != null) {

                        BiocacheQuery q = new BiocacheQuery(null, null, query, null, true, geospatialKosher
                                , baseBiocacheUrl, baseWSBiocacheUrl, supportDynamic);
                        mapSpecies(q, layerName, StringConstants.SPECIES, q.getOccurrenceCount()
                                , LayerUtilitiesImpl.SPECIES, null, 0, 4, 0.8f, Integer.decode(style), false);
                    }
                }
            }
        }
    }

    /**
     * Maps environmental and contextual layers from a "&layers" param. Uses the
     * short name of the layer. e.g. "aspect" or ""
     */
    public void mapLayerFromParams() {
        Map<String, String> userParams = getQueryParameterMap(Executions.getCurrent().getDesktop().getQueryString());
        if (userParams != null) {
            String layersCSV = userParams.get("layers");
            if (StringUtils.trimToNull(layersCSV) == null) {
                return;
            }
            String[] layers = layersCSV.split(",");
            for (String s : layers) {
                JSONArray layerlist = CommonData.getLayerListJSONArray();
                for (int j = 0; j < layerlist.size(); j++) {
                    JSONObject jo = layerlist.getJSONObject(j);
                    String name = jo.getString(StringConstants.NAME);
                    if (name.equalsIgnoreCase(s)) {
                        String uid = jo.getString(StringConstants.ID);
                        String type = jo.getString(StringConstants.TYPE);
                        String treeName = StringUtils.capitalize(jo.getString(StringConstants.DISPLAYNAME));
                        String treePath = jo.getString("displaypath");
                        String legendurl = CommonData.getGeoServer()
                                + "/wms?REQUEST=GetLegendGraphic&VERSION=1.0.0&FORMAT=image/png&WIDTH=20&HEIGHT=9&LAYER=" + s;
                        String metadata = CommonData.getLayersServer() + "/layers/view/more/" + uid;
                        getMapComposer().addWMSLayer(s, treeName, treePath, (float) 0.75, metadata, legendurl,
                                StringConstants.ENVIRONMENTAL.equalsIgnoreCase(type) ?
                                        LayerUtilitiesImpl.GRID : LayerUtilitiesImpl.CONTEXTUAL, null, null, null);
                    }
                }
            }
        }
    }

    /**
     * Maps environmental and contextual layers from a "&layers" param. Uses the
     * short name of the layer. e.g. "aspect" or ""
     */
    public void mapObjectFromParams() {
        Map<String, String> userParams = getQueryParameterMap(Executions.getCurrent().getDesktop().getQueryString());
        if (userParams != null) {
            String pidsAsString = userParams.get(StringConstants.PID);
            if (StringUtils.trimToNull(pidsAsString) == null) {
                return;
            }
            String[] pids = pidsAsString.trim().split(",");
            List<MapLayer> mapLayers = getMapComposer().getPortalSession().getActiveLayers();
            Map<String, MapLayer> names = new HashMap<String, MapLayer>();
            for (MapLayer ml : mapLayers) {
                names.put(ml.getName(), ml);
            }
            for (String pid : pids) {
                if (names.get("PID:" + pid) == null) {
                    MapLayer mapLayer = getMapComposer().addObjectByPid(pid, null);
                    if (pids.length == 1) {
                        //zoom to this region
                        getMapComposer().zoomToExtent(mapLayer);
                    }
                } else if (pids.length == 1) {
                    MapLayer mapLayer = names.get("PID:" + pid);
                    getMapComposer().zoomToExtent(mapLayer);
                }
            }
        }
    }

    /**
     * Parsing of "q" and "fq" params
     *
     * @return
     */
    private MapLayer loadUrlParameters() {
        String params = null;

        try {
            String analysisLayerSelections = getCookieValue("analysis_layer_selections");
            if (analysisLayerSelections != null) {
                String[] s = URLDecoder.decode(analysisLayerSelections, StringConstants.UTF_8).split("\n");
                for (int i = 0; i < s.length; i++) {
                    String[] ls = s[i].split(" // ");
                    selectedLayers.add(new LayerSelection(ls[0], ls[1]));
                }
            }
        } catch (Exception e) {
            LOGGER.error("error loading url parameters", e);
        }

        try {
            params = Executions.getCurrent().getDesktop().getQueryString();
            LOGGER.debug("User params: " + params);

            List<Entry<String, String>> userParams = Util.getQueryParameters(params);
            StringBuilder sb = new StringBuilder();
            String qc = null;
            String bs = null;
            String ws = null;
            String wkt = null;
            int size = 4;
            float opacity = 0.6f;
            int colour = 0xff0000;
            String pointtype = "auto";
            String bb = null;
            Double lat = null;
            Double lon = null;
            Double radius = null;
            String colourBy = null;
            String savedsession = "";
            String s = null;
            boolean[] geospatialKosher = null;
            boolean supportDynamic = false;

            for (int i = 0; i < userParams.size(); i++) {
                String key = userParams.get(i).getKey();
                String value = userParams.get(i).getValue();

                if ("wmscache".equals(key)) {
                    useSpeciesWMSCache = value;
                }

                if ("species_lsid".equals(key)) {
                    sb.append("lsid:").append(value);
                } else if ("q".equals(key)) {
                    //relies on spitonparams (biocachequery)
                    s = value;

                    //biocache is unhappy with (lsid:...)
                    //remove brackets to make it work
                    if (value.startsWith("(") && value.endsWith(")") && !value.contains(" ")) {
                        s = value.substring(1, value.length() - 2);
                    }
                } else if ("fq".equals(key)) {

                    //flag geospatialKosher filters separately
                    boolean[] gk;
                    if ((gk = BiocacheQuery.parseGeospatialKosher(value)) != null) {
                        geospatialKosher = gk;
                    } else {
                        //use as-is
                        //spitonparams (biocachequery) splits these
                        sb.append("&").append(key).append("=").append(value);
                    }
                } else if ("qc".equals(key)) {
                    qc = "&qc=" + URLEncoder.encode(value, StringConstants.UTF_8);
                } else if ("bs".equals(key)) {
                    bs = value;
                } else if ("ws".equals(key)) {
                    ws = value;
                } else if (StringConstants.WKT.equals(key)) {
                    wkt = value;
                } else if ("psize".equals(key)) {
                    size = Integer.parseInt(value);
                } else if ("popacity".equals(key)) {
                    opacity = Float.parseFloat(value);
                } else if ("pcolour".equals(key)) {
                    colour = Integer.parseInt(value, 16);
                } else if ("ptype".equals(key)) {
                    pointtype = value;
                } else if (StringConstants.BBOX.equals(key)) {
                    bb = value;
                } else if ("lat".equals(key)) {
                    lat = Double.parseDouble(value);
                } else if ("lon".equals(key)) {
                    lon = Double.parseDouble(value);
                } else if ("radius".equals(key)) {
                    radius = Double.parseDouble(value);
                } else if ("ss".equals(key)) {
                    savedsession = value.trim();
                } else if ("dynamic".equals(key)) {
                    supportDynamic = Boolean.parseBoolean(value);
                } else if ("cm".equals(key)) {
                    colourBy = value.trim();
                }
            }

            if (lat != null && lon != null && radius != null) {
                //m to km
                wkt = Util.createCircleJs(lon, lat, radius * 1000);
            }

            if (StringUtils.isNotBlank(savedsession)) {
                loadUserSession(savedsession);
            } else {
                LOGGER.debug("No saved session to load");
            }

            LOGGER.debug("url query: " + sb.toString());
            if (sb.length() > 0 || (s != null && s.length() > 0)) {

                if (s != null) {
                    s += sb.toString();
                } else {
                    s = sb.toString();
                }
                BiocacheQuery q = new BiocacheQuery(null, wkt, s, null, true, geospatialKosher, bs, ws, supportDynamic);

                if (qc != null) {
                    q.setQc(qc);
                }

                if (getMapLayerDisplayName(q.getSolrName()) == null) {
                    if (bb == null) {
                        List<Double> bbox = q.getBBox();
                        String script = "map.zoomToExtent(new OpenLayers.Bounds("
                                + bbox.get(0) + "," + bbox.get(1) + "," + bbox.get(2) + "," + bbox.get(3) + ")"
                                + ".transform("
                                + "  new OpenLayers.Projection('EPSG:4326'),"
                                + "  map.getProjectionObject()));";
                        openLayersJavascript.setAdditionalScript(script);
                    }
                    //mappable attributes
                    int setGrid = -1;
                    if (pointtype.equals(StringConstants.GRID)) {
                        setGrid = 1;
                    } else if ("point".equals(pointtype)) {
                        setGrid = 0;
                    }
                    return mapSpecies(q, q.getSolrName(), StringConstants.SPECIES, q.getOccurrenceCount()
                            , LayerUtilitiesImpl.SPECIES, null, setGrid, size, opacity, colour, colourBy, true);
                }
            }

            mapMultiQueryLayers(geospatialKosher, bs, ws, supportDynamic);

        } catch (Exception e) {
            LOGGER.debug("Error loading url parameters: " + params, e);
        }

        //load any deep linked layers
        mapLayerFromParams();

        //load any deep linked objects
        mapObjectFromParams();

        return null;
    }

    public void onActivateLink(ForwardEvent event) {
        Component component = event.getOrigin().getTarget();
        Object oLink = component.getAttribute("link");
        if (oLink instanceof String) {
            String uri = (String) oLink;
            String label = (String) component.getAttribute("label");
            this.activateLink(uri, label, false);
        }
    }

    public MapLayer addPointsOfInterestLayer(String wkt, String label, String displayName) {
        MapLayer mapLayer = null;

        if (safeToPerformMapAction()) {
            if (portalSessionUtilities.getUserDefinedById(getPortalSession(), label) == null) {
                mapLayer = remoteMap.createWKTLayer(wkt, label);
                mapLayer.setDisplayName(displayName);
                mapLayer.setPointsOfInterestWS(CommonData.getLayersServer() + "/intersect/poi/wkt");

                mapLayer.setOpacity((float) 0.4);
                mapLayer.setDynamicStyle(true);

                activateLayer(mapLayer, true, true);
            } else {
                // fail
                showMessage("WKT layer already exists");
                LOGGER.debug(
                        "refusing to add a new layer with name " + label
                                + " because it already exists in the menu");
            }
        }

        return mapLayer;
    }

    public MapLayer addWKTLayer(String wkt, String label, String displayName) {

        if (wkt != null) {
            wkt = Util.fixWkt(wkt);
        }
        MapLayer mapLayer = null;

        if (safeToPerformMapAction()) {
            if (portalSessionUtilities.getUserDefinedById(getPortalSession(), label) == null) {
                mapLayer = remoteMap.createWKTLayer(wkt, label);
                mapLayer.setDisplayName(displayName);

                mapLayer.setOpacity((float) 0.4);
                mapLayer.setDynamicStyle(true);

                activateLayer(mapLayer, true, true);
            } else {
                // fail
                showMessage("WKT layer already exists");
                LOGGER.debug(
                        "refusing to add a new layer with name " + label
                                + " because it already exists in the menu");
            }
        }

        return mapLayer;
    }

    /**
     * Destroy session and reload page
     * <p/>
     * - added confirmation message, needs to be event driven since message box
     * always returning '1' with current zk settings.
     */
    public void onClick$reloadPortal() {
        // user confirmation for whole map reset
        try {
            Messagebox.show("Reset map to initial empty state, with no layers and default settings?", "Reset Map",
                    Messagebox.YES | Messagebox.NO, Messagebox.QUESTION, new EventListener() {

                        @Override
                        public void onEvent(Event event) throws Exception {
                            if ((Integer) event.getData() == Messagebox.YES) {
                                //reset map
                                reloadPortal();
                            }
                        }
                    });

        } catch (Exception e) {
            LOGGER.error("error reloading portal", e);
        }
    }

    /**
     * split from button for yes/no event processing
     * <p/>
     * see onClick$reloadPortal()
     */
    void reloadPortal() {
        // grab the portaluser instance so that logged in users stay logged in...
        PortalSession portalSession = getPortalSession();

        // remove the old species filters
        getSession().removeAttribute("speciesfilters");

        // create a new session from the master session
        SessionInit sessionInit = new SessionInitImpl();
        try {
            sessionInit.init(Sessions.getCurrent(), null);
        } catch (Exception ex) {
            LOGGER.error("Error creating replacement session information after user clicked reload portal", ex);

        }
        // check PortalSession is not null - if it is, something is badly wrong
        // so redirect to error page
        portalSession = getPortalSession();
        if (portalSession == null) {
            LOGGER.error("Null portal session created after clicking reset map, sending user to error page");
            try {
                Executions.getCurrent().forward("/WEB-INF/jsp/Error.jsp");
            } catch (IOException ex) {
                LOGGER.error("error redirecting to error page", ex);
            }
        } else {
            // all good - put the portal user back in, then force a page reload
            Executions.getCurrent().sendRedirect(CommonData.getWebportalServer() + "/");
        }
    }

    /**
     * Reload all species layers
     */
    public void onReloadLayers(Event event) {
        String tbxReloadLayers;

        if (event == null) {
            mapZoomLevel = getLeftmenuSearchComposer().getZoom();

            tbxReloadLayers = (new StringBuffer()).append("z=").append(String.valueOf(mapZoomLevel)).append("&amp;b=")
                    .append(getLeftmenuSearchComposer().getViewportBoundingBox().toString()).toString();
        } else {
            tbxReloadLayers = (String) event.getData();

            String s = tbxReloadLayers;
            int s1 = s.indexOf("z=");
            int s2 = s.indexOf('&');
            if (s1 >= 0) {
                if (s2 >= 0) {
                    mapZoomLevel = Integer.parseInt(s.substring(s1 + 2, s2));
                } else {
                    mapZoomLevel = Integer.parseInt(s.substring(s1 + 2));
                }
            }

        }
        LOGGER.debug("tbxReloadLayers.getValue(): " + tbxReloadLayers);
        // iterate thru' active map layers
        List udl = getPortalSession().getActiveLayers();
        Iterator iudl = udl.iterator();
        List<String> processedLayers = new ArrayList();
        String reloadScript = "";
        while (iudl.hasNext()) {
            MapLayer ml = (MapLayer) iudl.next();
            if (processedLayers.contains(ml.getName())) {
                LOGGER.debug(ml.getName() + " already processed.");
                continue;
            }
            LOGGER.debug("checking reload layer: " + ml.getName() + " - " + ml.getId() + " - "
                    + ml.getNameJS() + " -> type: " + ml.getType() + "," + ml.getGeometryType());

            processedLayers.add(ml.getName());
        }

        if (reloadScript.length() > 0) {
            openLayersJavascript.execute(
                    openLayersJavascript.getIFrameReferences()
                            + reloadScript);
        }
    }

    /**
     * generate pdf for from innerHTML of printHack txtbox
     * <p/>
     * dependant on whatever the ends up being on the map tab
     * <p/>
     * uses wkhtmltoimage (wkhtmltopdf not working?)
     */
    public void onClick$onPrint(Event event) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put(StringConstants.PRINT_PARAMS, event.getData());
        params.put(StringConstants.BASE_LAYER, getBaseMap());
        params.put(StringConstants.BOUNDING_BOX, getLeftmenuSearchComposer().getViewportBoundingBox());
        params.put(StringConstants.LAYERS, getPortalSession().getActiveLayers());

        openModal("WEB-INF/zul/Printing.zul", params, "printingwindow");
    }

    /**
     * watch for layer loaded signals
     */
    public void onLayerLoaded(Event event) {
        tbxLayerLoaded = (String) event.getData();
        for (EventListener el : layerLoadedChangeEvents.values()) {
            try {
                el.onEvent(null);
            } catch (Exception ex) {
                LOGGER.error("error firing layer loaded listener", ex);
            }
        }
    }

    public String getLayerLoaded() {
        return tbxLayerLoaded;
    }

    public void addLayerLoadedEventListener(String eventName, EventListener eventListener) {
        layerLoadedChangeEvents.put(eventName, eventListener);
    }

    public void removeLayerLoadedEventListener(String eventName) {
        layerLoadedChangeEvents.remove(eventName);
    }

    public MapLayer mapPointsOfInterest(String wkt, String label, String displayName) {
        return addPointsOfInterestLayer(wkt, label, displayName);
    }

    /**
     * gets a species map that doesn't have colourby set
     */
    public MapLayer mapSpecies(Query sq, String species, String rank, int count, int subType, String wkt
            , int setGrid, int size, float opacity, int colour, boolean mapExpertDistribution) {
        return mapSpecies(sq, species, rank, count, subType, wkt, setGrid, size, opacity, colour, null
                , mapExpertDistribution);
    }

    public MapLayer mapSpecies(Query sq, String species, String rank, int count, int subType, String wkt
            , int setGrid, int size, float opacity, int colour, String colourBy, boolean mapExpertDistribution) {

        String newSpecies = species;
        String newRank = rank;
        if (newSpecies == null) {
            newSpecies = sq.getName();
            newRank = sq.getRank();
        }

        int newCount = count;
        if (newCount < 0) {
            newCount = sq.getOccurrenceCount();
        }

        //use # of points cutoff
        boolean grid;
        if (setGrid == 0) {
            grid = false;
        } else {
            grid = setGrid == 1 || sq.getOccurrenceCount() >
                    Integer.parseInt(getSettingsSupplementary().getProperty("points_cluster_threshold"));
        }
        MapLayer ml = mapSpeciesFilter(sq, newSpecies, newRank, newCount, subType, wkt, grid, size, opacity, colour
                , mapExpertDistribution);

        if (ml != null) {
            if (colourBy != null) {
                ml.setColourMode(colourBy);
            }
            //for Active Area mapping
            ml.getMapLayerMetadata().setOccurrencesCount(newCount);

            String layerType = "Species - Search";
            if (newSpecies.startsWith("Occurrences in ")) {
                layerType = "Species - Occurrences";
            } else if (StringConstants.SPECIES_ASSEMBLAGE.equals(newSpecies)) {
                layerType = "Species - Assemblage";
            } else if (newSpecies.contains(StringConstants.MY_FACET)) {
                layerType = "Species - Facet";
            }
            if (subType == LayerUtilitiesImpl.SPECIES_UPLOAD) {
                layerType = "Import - Species";
            }
            if (subType == LayerUtilitiesImpl.SPECIES && "user".equals(newRank)) {
                layerType = "Import - LSID";
            }
            if (sq instanceof BiocacheQuery) {
                BiocacheQuery bq = (BiocacheQuery) sq;
                String extra = bq.getWS() + "|" + bq.getBS() + "|" + bq.getFullQ(false);
                remoteLogger.logMapSpecies(ml.getDisplayName(), bq.getLsids(), wkt, layerType, extra);

            } else if (sq instanceof UserDataQuery) {
                remoteLogger.logMapSpecies(ml.getDisplayName(), "user-" + sq.getSpeciesCount() + " records", wkt
                        , layerType, sq.getMetadataHtml());
            } else {
                remoteLogger.logMapSpecies(ml.getDisplayName(), newSpecies, wkt, layerType, sq.getMetadataHtml());
            }

            updateLayerControls();
            refreshContextualMenu();
            //changes need to be apply when the colour by is not null this allows the map to be updated to
            // reflect the correct facet
            if (colourBy != null) {
                applyChange(ml);
            }
        }

        return ml;
    }

    void addLsidBoundingBoxToMetadata(MapLayerMetadata md, Query query) {
        //get bounding box for lsid
        List<Double> bb = query.getBBox();

        md.setBbox(bb);
    }

    public String getLayerBoundingBox(MapLayer ml) {
        String bbox = null;

        if (ml.getMapLayerMetadata().getBboxString() != null) {
            bbox = ml.getMapLayerMetadata().getBboxString();
        } else {
            Clients.evalJavaScript("jq('$currentLayerExtent')[0].innerHTML=map.getLayersByName('"
                    + ml.getName() + "')[0].getExtent().transform(map.getProjectionObject()," +
                    "new OpenLayers.Projection('EPSG:4326')).toString();");
            String bboxstr = currentLayerExtent.getValue();
            LOGGER.debug("Got bboxstr: " + bboxstr);
            if (bboxstr != null) {
                String[] b = bboxstr.split(",");
                List<Double> bb = new ArrayList<Double>();
                bb.add(Double.parseDouble(b[0]));
                bb.add(Double.parseDouble(b[1]));
                bb.add(Double.parseDouble(b[2]));
                bb.add(Double.parseDouble(b[3]));

                ml.getMapLayerMetadata().setBbox(bb);

                bbox = bboxstr;
            }
        }

        return bbox;
    }

    private void loadDistributionMap(String lsids, String wkt) {
        String newWkt = wkt;
        if (CommonData.WORLD_WKT.equals(newWkt)) {
            newWkt = null;
        }
        //test for a valid lsid match
        String[] wmsNames = CommonData.getSpeciesDistributionWMS(lsids);
        String[] spcode = CommonData.getSpeciesDistributionSpcode(lsids);
        MapLayer ml;
        if (wmsNames.length > 0 && (newWkt == null || newWkt.equals(CommonData.WORLD_WKT))) {
            //add all
            for (int i = 0; i < wmsNames.length; i++) {
                if (getMapLayerWMS(wmsNames[i]) == null) {
                    //map this layer with its recorded scientific name
                    String scientific = JSONObject.fromObject(Util.readUrl(CommonData.getLayersServer()
                            + "/distribution/" + spcode[i])).getString(StringConstants.SCIENTIFIC);
                    String layerName = getNextAreaLayerName(scientific);
                    String html = Util.getMetadataHtmlForDistributionOrChecklist(spcode[i], null, layerName);
                    ml = addWMSLayer(layerName, getNextAreaLayerName("Expert distribution: " + scientific)
                            , wmsNames[i], 0.35f, html, null, LayerUtilitiesImpl.WKT, null, null);
                    ml.setSPCode(spcode[i]);
                    setupMapLayerAsDistributionArea(ml);
                }
            }
        } else if (wmsNames.length > 0 && newWkt != null && !newWkt.equals(CommonData.WORLD_WKT)) {
            String url = CommonData.getLayersServer() + "/distributions";
            try {
                HttpClient client = new HttpClient();
                PostMethod post = new PostMethod(url);
                post.addParameter(StringConstants.WKT, newWkt);
                post.addParameter(StringConstants.LSIDS, lsids);
                post.addRequestHeader(StringConstants.ACCEPT, StringConstants.JSON_JAVASCRIPT_ALL);
                int result = client.executeMethod(post);
                if (result == 200) {
                    String txt = post.getResponseBodyAsString();
                    JSONArray ja = JSONArray.fromObject(txt);
                    List<String> found = new ArrayList();
                    for (int i = 0; i < ja.size(); i++) {
                        JSONObject jo = ja.getJSONObject(i);
                        if (jo.containsKey(StringConstants.WMSURL)) {
                            found.add(jo.getString(StringConstants.WMSURL));
                        }
                    }
                    for (int i = 0; i < wmsNames.length; i++) {
                        if (getMapLayerWMS(wmsNames[i]) == null) {
                            String scientific = JSONObject.fromObject(Util.readUrl(CommonData.getLayersServer()
                                    + "/distribution/" + spcode[i])).getString(StringConstants.SCIENTIFIC);
                            String layerName = getNextAreaLayerName(scientific + " area " + (i + 1));
                            String html = Util.getMetadataHtmlForDistributionOrChecklist(spcode[i], null, layerName);
                            ml = addWMSLayer(layerName, getNextAreaLayerName("Expert distribution: " + scientific)
                                    , found.get(i), 0.35f, html, null, LayerUtilitiesImpl.WKT, null, null);
                            ml.setSPCode(spcode[i]);
                            setupMapLayerAsDistributionArea(ml);
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.error("error posting distributions: " + url);
            }
        }

        openChecklistSpecies(lsids, newWkt, true);
    }

    void openChecklistSpecies(String lsids, String wkt, boolean mapIfOnlyOne) {
        try {
            //species checklists
            String[] finallist = Util.getDistributionsOrChecklists(StringConstants.CHECKLISTS, wkt, lsids, null);

            //open for optional mapping of areas
            if (finallist.length > 1) {
                if (mapIfOnlyOne && finallist.length == 2) {
                    try {
                        String[] row;
                        CSVReader csv = new CSVReader(new StringReader(finallist[1]));
                        row = csv.readNext();
                        csv.close();
                        if (getMapLayerWMS(CommonData.getSpeciesChecklistWMSFromSpcode(row[0])[1]) == null) {
                            //map it
                            String[] mapping = CommonData.getSpeciesChecklistWMSFromSpcode(row[0]);
                            String displayName = mapping[0] + " area";
                            if (row[11] != null && row[11].length() > 0) {
                                displayName = row[11];
                            }

                            String layerName = getNextAreaLayerName(row[0] + " area");
                            String html = Util.getMetadataHtmlForDistributionOrChecklist(row[0], row, layerName);

                            MapLayer ml = getMapComposer().addWMSLayer(layerName, displayName, mapping[1], 0.6f, html
                                    , null, LayerUtilitiesImpl.WKT, null, null);
                            ml.setSPCode(row[0]);
                            setupMapLayerAsDistributionArea(ml);
                        }
                    } catch (Exception e) {
                        LOGGER.error("error opening checklist species", e);
                    }
                } else {
                    if (hasFellow(StringConstants.DISTRIBUTION_RESULTS)) {
                        getFellowIfAny(StringConstants.DISTRIBUTION_RESULTS).detach();
                    }

                    Map params = new HashMap();
                    params.put(StringConstants.TITLE, "Checklist species");
                    params.put(StringConstants.SIZE, String.valueOf(finallist.length - 1));
                    params.put(StringConstants.TABLE, finallist);
                    Window window = (Window) Executions.createComponents(
                            "WEB-INF/zul/results/AnalysisDistributionResults.zul", this, params);

                    try {
                        window.doModal();
                    } catch (Exception e) {
                        LOGGER.error("error opening checklist species dialog", e);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("error opening distribution area dialog", e);
        }
    }

    void openAreaChecklist(String geomIdx, String lsids, String wkt) {
        try {
            //checklist species
            String[] finallist = Util.getDistributionsOrChecklists(StringConstants.CHECKLISTS, wkt, lsids, geomIdx);

            if (hasFellow(StringConstants.DISTRIBUTION_RESULTS)) {
                getFellowIfAny(StringConstants.DISTRIBUTION_RESULTS).detach();
            }
            Map params = new HashMap();
            params.put(StringConstants.TITLE, "Checklist species");
            params.put(StringConstants.SIZE, String.valueOf(finallist.length - 1));
            params.put(StringConstants.TABLE, finallist);
            Window window = (Window) Executions.createComponents("WEB-INF/zul/results/AnalysisDistributionResults.zul"
                    , this, params);

            try {
                window.doModal();
            } catch (Exception e) {
                LOGGER.error("error opening checklist species dialog", e);
            }
        } catch (Exception e) {
            LOGGER.error("error opening distribution area dialog", e);
        }
    }

    void openDistributionSpecies(String lsids, String wkt) {
        try {
            //expert distributions
            String[] distributions = Util.getDistributionsOrChecklists(StringConstants.DISTRIBUTIONS, wkt, lsids, null);

            //open for optional mapping of areas
            if (distributions.length > 0) {
                if (hasFellow(StringConstants.DISTRIBUTION_RESULTS)) {
                    getFellowIfAny(StringConstants.DISTRIBUTION_RESULTS).detach();
                }

                Map params = new HashMap();
                params.put(StringConstants.TABLE, distributions);
                params.put(StringConstants.TITLE, "Expert distributions");
                params.put(StringConstants.SIZE, String.valueOf(distributions.length - 1));

                Window window = (Window) Executions.createComponents(
                        "WEB-INF/zul/results/AnalysisDistributionResults.zul", this, params);

                try {
                    window.doModal();
                } catch (Exception e) {
                    LOGGER.error("error opening analysisdistributionresults.zul", e);
                }
            }

        } catch (Exception e) {
            LOGGER.error("error opening distribution species", e);
        }
    }

    public void setupMapLayerAsDistributionArea(MapLayer mapLayer) {
        try {
            //identify the spcode from the url
            String spcode = mapLayer.getSPCode();
            String url = CommonData.getLayersServer() + "/distribution/" + spcode;
            String jsontxt = Util.readUrl(url);
            if (jsontxt == null || jsontxt.length() == 0) {
                url = CommonData.getLayersServer() + "/checklist/" + spcode;
                jsontxt = Util.readUrl(url);
            }
            if (jsontxt == null || jsontxt.length() == 0) {
                LOGGER.debug("******** failed to find wkt for " + mapLayer.getUri() + " > " + spcode);
                return;
            }
            JSONObject jo = JSONObject.fromObject(jsontxt);
            if (!jo.containsKey(StringConstants.GEOMETRY)) {
                return;
            }
            mapLayer.setWKT(jo.getString(StringConstants.GEOMETRY));
            mapLayer.setPolygonLayer(true);

            Facet facet = null;
            if (jo.containsKey(StringConstants.PID) && jo.containsKey(StringConstants.AREA_NAME)) {
                JSONObject object = JSONObject.fromObject(Util.readUrl(CommonData.getLayersServer() + "/object/"
                        + jo.containsKey(StringConstants.PID)));

                //only get field data if it is an intersected layer (to exclude layers containing points)
                if (CommonData.getLayer((String) object.get(StringConstants.FID)) != null) {
                    facet = Util.getFacetForObject(jo.getString(StringConstants.AREA_NAME)
                            , (String) object.get(StringConstants.FID));
                }
            }
            if (facet != null) {
                List<Facet> facets = new ArrayList<Facet>();
                facets.add(facet);
                mapLayer.setFacets(facets);
            }
            MapLayerMetadata md = mapLayer.getMapLayerMetadata();

            try {
                double[][] bb;

                if (jo.containsKey("bounding_box")) {
                    bb = SimpleShapeFile.parseWKT(jo.getString("bounding_box")).getBoundingBox();
                } else {
                    bb = SimpleShapeFile.parseWKT(jo.getString(StringConstants.GEOMETRY)).getBoundingBox();
                }
                List<Double> bbox = new ArrayList<Double>();
                bbox.add(bb[0][0]);
                bbox.add(bb[0][1]);
                bbox.add(bb[1][0]);
                bbox.add(bb[1][1]);
                md.setBbox(bbox);
            } catch (Exception e) {
                LOGGER.error("failed to parse wkt in : " + url, e);
            }

            //add colour!
            mapLayer.setRedVal(255);
            mapLayer.setGreenVal(0);
            mapLayer.setBlueVal(0);
            mapLayer.setDynamicStyle(true);

            warnForLargeWKT(mapLayer);

        } catch (Exception e) {
            LOGGER.error("error setting up distributions map layer", e);
        }
    }

    public void warnForLargeWKT(MapLayer ml) {
        //display warning for large wkt that does not have a facet
        if (ml.getFacets() == null
                && ml.getWKT().length() > Integer.parseInt(CommonData.getSettings().getProperty("max_q_wkt_length"))) {
            WKTReducedDTO reduced = Util.reduceWKT(ml.getWKT());
            ml.setWKT(reduced.getReducedWKT());
            getMapComposer().showMessage("WARNING: The polygon displayed has reduced resolution to enable " +
                    "subsequent analyses.\r\n"
                    + reduced.getReducedBy());
        }
    }

    MapLayer mapSpeciesFilter(Query q, String species, String rank, int count, int subType, String wkt, boolean grid
            , int size, float opacity, int colour, boolean mapExpertDistributions) {
        String filter = q.getQ();

        //just in case it fails
        if (mapExpertDistributions) {
            try {
                if (q instanceof BiocacheQuery) {
                    String lsids = ((BiocacheQuery) q).getLsids();
                    List<String> extraLsids = ((BiocacheQuery) q).getLsidFromExtraParams();
                    if (lsids != null && lsids.length() > 0) {
                        loadDistributionMap(lsids, wkt);
                    }
                    for (String extraLsid : extraLsids) {
                        if (extraLsid != null && extraLsid.length() > 0) {
                            LOGGER.debug("loading layer for: " + extraLsid);
                            loadDistributionMap(extraLsid, wkt);
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.error("failed to map species distribution areas", e);
            }
        }

        MapLayer ml = mapSpeciesWMSByFilter(getNextAreaLayerName(species), filter, subType, q, grid, size, opacity
                , colour);

        if (ml != null) {
            addToSession(ml.getName(), filter);
            MapLayerMetadata md = ml.getMapLayerMetadata();

            md.setOccurrencesCount(count);

            ml.setClustered(false);

            if (grid) {
                ml.setColourMode(StringConstants.GRID);
            }

            addLsidBoundingBoxToMetadata(md, q);
        }

        return ml;
    }

    MapLayer mapSpeciesWMSByFilter(String label, String filter, int subType, Query query, boolean grid, int size
            , float opacity, int colour) {
        String uri;

        int r = (colour >> 16) & 0x000000ff;
        int g = (colour >> 8) & 0x000000ff;
        int b = (colour) & 0x000000ff;
        int sz = size;
        int uncertaintyCheck = 0;
        float op = opacity;

        if (activeLayerMapProperties != null) {
            r = (Integer) activeLayerMapProperties.get(StringConstants.RED);
            b = (Integer) activeLayerMapProperties.get(StringConstants.BLUE);
            g = (Integer) activeLayerMapProperties.get(StringConstants.GREEN);
            sz = (Integer) activeLayerMapProperties.get(StringConstants.SIZE);
            op = (Float) activeLayerMapProperties.get(StringConstants.OPACITY);
            uncertaintyCheck = (Integer) activeLayerMapProperties.get(StringConstants.UNCERTAINTY);
        }

        if (subType == LayerUtilitiesImpl.SCATTERPLOT) {
            //set defaults for scatterplot
            r = 0;
            g = 0;
            b = 255;
            op = 1;
            sz = 4;
        }

        Color c = new Color(r, g, b);
        String hexColour = Integer.toHexString(c.getRGB() & 0x00ffffff);
        String envString = "";
        if (grid) {
            //colour mode is in 'filter' but need to move it to envString
            envString += "colormode:grid";
        } else {
            envString = "color:" + hexColour;
        }
        envString += ";name:circle;size:" + sz + ";opacity:1";
        if (uncertaintyCheck > 0) {
            envString += ";uncertainty:1";
        }

        uri = query.getUrl();
        uri += "service=WMS&version=1.1.0&request=GetMap&styles=&format=image/png";
        uri += "&layers=ALA:occurrences";
        uri += "&transparent=true";
        uri += (query.getQc() == null ? "" : query.getQc());
        uri += "&CACHE=" + useSpeciesWMSCache;
        uri += "&CQL_FILTER=";

        LOGGER.debug("Mapping: " + label + " with " + uri + filter);

        try {
            if (safeToPerformMapAction()) {
                if (getMapLayer(label) == null) {
                    MapLayer ml = addWMSLayer(label, label, uri + filter, op, null, null, subType, ""
                            , envString, query);
                    if (ml != null) {
                        ml.setDynamicStyle(true);
                        ml.setEnvParams(envString);
                        ml.setGeometryType(LayerUtilitiesImpl.POINT);

                        ml.setBlueVal(b);
                        ml.setGreenVal(g);
                        ml.setRedVal(r);
                        ml.setSizeVal(sz);
                        ml.setOpacity(op);

                        ml.setClustered(false);

                        ml.setSpeciesQuery(query);

                        updateLayerControls();

                        //create highlight layer
                        MapLayer mlHighlight = (MapLayer) ml.clone();
                        mlHighlight.setName(ml.getName() + "_highlight");
                        ml.addChild(mlHighlight);

                        return ml;
                    } else {
                        // fail
                        //hide error, might be clustering zoom in
                        LOGGER.debug("adding WMS layer failed ");
                    }
                } else {
                    //need to cleanup any additional scripts outstanding
                    openLayersJavascript.useAdditionalScript();

                    // fail
                    LOGGER.debug(
                            "refusing to add a new layer with URI " + uri
                                    + " because it already exists in the menu");
                }
            }
        } catch (Exception ex) {
            LOGGER.error("error mapSpeciesByNameRank:", ex);
        }
        return null;
    }

    public LeftMenuSearchComposer getLeftmenuSearchComposer() {
        return (LeftMenuSearchComposer) getFellow("leftMenuSearch").getFellow("leftSearch");
    }

    public Iframe getRawMessageIframeHack() {
        return rawMessageIframeHack;
    }

    public void setRawMessageIframeHack(Iframe rawMessageIframeHack) {
        this.rawMessageIframeHack = rawMessageIframeHack;
    }

    public Div getRawMessageHackHolder() {
        return rawMessageHackHolder;
    }

    public void setRawMessageHackHolder(Div rawMessageHackHolder) {
        this.rawMessageHackHolder = rawMessageHackHolder;
    }

    public LanguagePack getLanguagePack() {
        return languagePack;
    }

    public void setLanguagePack(LanguagePack languagePack) {
        this.languagePack = languagePack;
    }

    public Properties getSettingsSupplementary() {
        settingsSupplementary = CommonData.getSettings();
        return CommonData.getSettings();
    }

    public void setSettingsSupplementary(Properties settingsSupplementary) {
        this.settingsSupplementary = settingsSupplementary;
    }

    public OpenLayersJavascript getOpenLayersJavascript() {
        return openLayersJavascript;
    }

    public void setOpenLayersJavascript(OpenLayersJavascript openLayersJavascript) {
        this.openLayersJavascript = openLayersJavascript;
    }

    public HttpConnection getHttpConnection() {
        return httpConnection;
    }

    public void setHttpConnection(HttpConnection httpConnection) {
        this.httpConnection = httpConnection;
    }

    public RemoteMap getRemoteMap() {
        return remoteMap;
    }

    public void setRemoteMap(RemoteMap remoteMap) {
        this.remoteMap = remoteMap;
    }

    public ActiveLayerRenderer getActiveLayerRenderer() {
        return activeLayerRenderer;
    }

    public void setActiveLayerRenderer(ActiveLayerRenderer activeLayerRenderer) {
        this.activeLayerRenderer = activeLayerRenderer;
    }

    public PortalSessionUtilities getPortalSessionUtilities() {
        return portalSessionUtilities;
    }

    public void setPortalSessionUtilities(PortalSessionUtilities portalSessionUtilities) {
        this.portalSessionUtilities = portalSessionUtilities;
    }

    public Settings getSettings() {
        return settings;
    }

    public void setSettings(Settings settings) {
        this.settings = settings;
    }

    public boolean isLayerControlVisible() {
        return layerControls.isVisible();
    }

    /*
     * for Events.echoEvent(Constants.OPEN_URL,mapComposer,url as string)
     */
    public void openUrl(Event event) {
        String s = (String) event.getData();

        LOGGER.debug("\n\n******\n\ns: " + s + "\n\n******\n\n");

        String url = "";
        String header = "";
        String download = "";
        String[] data = s.split("\n");
        if (!ArrayUtils.isEmpty(data)) {

            LOGGER.debug("data.length: " + data.length);

            if (data.length == 1) {
                url = data[0];
            }
            if (data.length == 2) {
                url = data[0];
                header = data[1];

                // now the 'header' might be a 'header' or 'download link'
                if (header.startsWith("pid:")) {
                    download = header;
                    header = "";
                }
            }
            if (data.length == 3) {
                url = data[0];
                header = data[1];
                download = data[2];
            }

            if (download.length() > 0 && download.startsWith(StringConstants.PID)) {
                download = download.substring(4);
            }

            activateLink(url, header, false, download);
        }

    }

    /*
     * for Events.echoEvent(Constants.OPEN_HTML,mapComposer,url as string)
     *
     * first line of String input is header
     */
    public void openHTML(Event event) {
        String html = (String) event.getData();
        String[] lines = html.split("\n");
        String header = lines[0];
        String url = "*" + html.substring(header.length()).trim();
        activateLink(url, header, false);
    }

    public String getViewArea() {
        //default to: current view to be dynamically returned on usage
        BoundingBox bb = getMapComposer().getLeftmenuSearchComposer().getViewportBoundingBox();

        //NC 20130319: Change the order of the rectangle polygon so that it is going in the correct direction for SOLR
        return StringConstants.POLYGON + "(("
                + bb.getMinLongitude() + " " + bb.getMinLatitude() + ","
                + bb.getMaxLongitude() + " " + bb.getMinLatitude() + ","
                + bb.getMaxLongitude() + " " + bb.getMaxLatitude() + ","
                + bb.getMinLongitude() + " " + bb.getMaxLatitude() + ","
                + bb.getMinLongitude() + " " + bb.getMinLatitude() + "))";
    }

    public Session getSession() {
        return Sessions.getCurrent();
    }

    public int getMapZoom() {
        return mapZoomLevel;
    }

    public void addToSession(String species, String filter) {
        Map speciesfilters = (Map) getSession().getAttribute("speciesfilters");
        if (speciesfilters == null) {
            speciesfilters = new HashMap();
        }
        speciesfilters.put(species, filter);
        getSession().setAttribute("speciesfilters", speciesfilters);
    }

    public void removeFromSession(String species) {
        Map speciesfilters = (Map) Sessions.getCurrent().getAttribute("speciesfilters");
        if (speciesfilters != null) {
            speciesfilters.remove(species);
            getSession().setAttribute("speciesfilters", speciesfilters);
        }
    }

    public void loadScatterplot(ScatterplotDataDTO data, String lyrName) {
        MapLayer ml = mapSpecies(data.getQuery(), data.getSpeciesName(), StringConstants.SPECIES, 0
                , LayerUtilitiesImpl.SCATTERPLOT, null,
                0, DEFAULT_POINT_SIZE, DEFAULT_POINT_OPACITY, Util.nextColour(), false);
        ml.setDisplayName(lyrName);
        ml.setSubType(LayerUtilitiesImpl.SCATTERPLOT);
        ml.setType(LayerUtilitiesImpl.SCATTERPLOT);
        ml.setScatterplotDataDTO(data);
        addUserDefinedLayerToMenu(ml, true);
        updateLayerControls();
        refreshContextualMenu();
    }

    public String getNextAreaLayerName(String layerPrefix) {
        String newLayerPrefix = layerPrefix;
        if (getMapLayer(newLayerPrefix) == null && getMapLayerDisplayName(newLayerPrefix) == null) {
            return newLayerPrefix;
        }

        newLayerPrefix += " ";
        int i = 1;
        while (getMapLayer(newLayerPrefix + i) != null
                || getMapLayerDisplayName(newLayerPrefix + i) != null) {
            i++;
        }
        return newLayerPrefix + i;
    }

    public String getNextActiveAreaLayerName(String areaName) {
        String newAreaName = areaName;
        if (newAreaName == null) {
            newAreaName = "Active area";
        } else if (newAreaName.trim().isEmpty()) {
            newAreaName = "Active area";
        }
        return "Occurrences in " + newAreaName + " ";
    }

    public void onClick$btnAddSpecies(Event event) {
        openModal("WEB-INF/zul/add/AddSpecies.zul", null, "addspecieswindow");
    }

    public void onClick$btnAddArea(Event event) {
        openModal("WEB-INF/zul/add/AddArea.zul", null, StringConstants.ADDAREAWINDOW);
    }

    public void onClick$btnAddLayer(Event event) {
        openModal("WEB-INF/zul/add/AddLayer.zul", null, "addlayerwindow");
    }

    public void onClick$btnAddFacet(Event event) {
        openModal("WEB-INF/zul/add/AddFacet.zul", null, StringConstants.ADDFACETWINDOW);
    }

    public void onClick$btnAddWMSLayer(Event event) {
        openModal("WEB-INF/zul/add/AddWMSLayer.zul", null, "addwmslayerwindow");
    }

    public void onClick$btnAddGDM(Event event) {
        openModal("WEB-INF/zul/tool/GDM.zul", null, StringConstants.ADDTOOLWINDOW);
    }

    public void onClick$btnAddMaxent(Event event) {
        openModal("WEB-INF/zul/tool/Maxent.zul", null, StringConstants.ADDTOOLWINDOW);
    }

    public void onClick$btnAddSampling(Event event) {
        openModal("WEB-INF/zul/tool/Sampling.zul", null, StringConstants.ADDTOOLWINDOW);
    }

    public void onClick$btnAddAloc(Event event) {
        openModal("WEB-INF/zul/tool/ALOC.zul", null, StringConstants.ADDTOOLWINDOW);
    }

    public void onClick$btnAddScatterplot(Event event) {
        openModal("WEB-INF/zul/tool/Scatterplot.zul", null, StringConstants.ADDTOOLWINDOW);
    }

    public void onClick$btnAddScatterplotList(Event event) {
        openModal("WEB-INF/zul/tool/ScatterplotList.zul", null, StringConstants.ADDTOOLWINDOW);
    }

    public void runTabulation(Event event) {
        openModal("WEB-INF/zul/tool/Tabulation.zul", null, StringConstants.ADDTOOLWINDOW);
    }

    public void onClick$btnAreaReport(Event event) {
        openModal("WEB-INF/zul/tool/AreaReport.zul", null, StringConstants.ADDTOOLWINDOW);
    }

    public void onClick$btnAreaReportPDF(Event event) {
        openModal("WEB-INF/zul/tool/AreaReportPDF.zul", null, StringConstants.ADDTOOLWINDOW);
    }

    public void runNearestLocalityAction(Event event) {
        remoteLogger.logMapAnalysis("Nearest locality", "Tool - Nearest locality", "", "", "", "", "", "");
    }

    public void onClick$btnSpeciesList(Event event) {
        openModal("WEB-INF/zul/tool/SpeciesList.zul", null, StringConstants.ADDTOOLWINDOW);
    }

    public void onClick$btnSitesBySpecies(Event event) {
        openModal("WEB-INF/zul/tool/SitesBySpecies.zul", null, StringConstants.ADDTOOLWINDOW);
    }

    public void onClick$btnPhylogeneticDiversity(Event event) {
        openModal("WEB-INF/zul/tool/PhylogeneticDiversity.zul", null, StringConstants.ADDTOOLWINDOW);
    }

    public Window openModal(String page, Map<String, Object> params, String windowname) {
        //remove any existing window with the same name (bug somewhere?)
        if (windowname != null && getFellowIfAny(windowname) != null) {
            getFellowIfAny(windowname).detach();
        }

        Window window = (Window) Executions.createComponents(page, this, params);

        try {
            window.doModal();
        } catch (Exception e) {
            LOGGER.error("error opening dialog: " + page, e);
        }
        return window;
    }

    void openOverlapped(String page) {
        Window window = (Window) Executions.createComponents(page, this, null);
        try {
            window.doOverlapped();
        } catch (Exception e) {
            LOGGER.error("error opening overlapped dialog: " + page, e);
        }
    }

    public void updateLayerControls() {
        adjustActiveLayersList();

        //remove any scatterplot legend
        Component c = getFellowIfAny("scatterplotlayerlegend");
        if (c != null) {
            c.detach();
        }

        //remove children
        for (int i = layerControls.getChildren().size() - 1; i >= 0; i--) {
            layerControls.getChildren().get(i).detach();
        }

        MapLayer selectedLayer = this.getActiveLayersSelection(false);
        if (selectedLayer == null) {
            if (activeLayersList.getItemCount() > 0) {
                activeLayersList.setSelectedIndex(0);
                selectedLayer = (MapLayer) activeLayersList.getModel().getElementAt(0);
            } else {
                return;
            }
        }

        lblSelectedLayer.setValue(selectedLayer.getDisplayName());

        String page;
        Window window;
        if (selectedLayer.getType() == LayerUtilitiesImpl.SCATTERPLOT) {
            page = "WEB-INF/zul/legend/LayerLegendScatterplot.zul";
        } else if (selectedLayer.getType() == LayerUtilitiesImpl.MAP) {
            page = "WEB-INF/zul/legend/MapOptions.zul";
        } else {
            Map params = new HashMap();
            llc2MapLayer = selectedLayer;
            params.put("map_layer", llc2MapLayer);
            window = (Window) Executions.createComponents("WEB-INF/zul/legend/LayerLegendGeneral.zul", layerControls
                    , params);

            try {
                window.doEmbedded();
            } catch (Exception e) {
                LOGGER.error("error setting up layer legend", e);
            }

            return;
        }

        window = (Window) Executions.createComponents(page, layerControls, null);
        try {
            if (window instanceof HasMapLayer) {
                ((HasMapLayer) window).setMapLayer(selectedLayer);
            }
        } catch (Exception e) {
            LOGGER.error("failed to set map layer to window: " + (window != null ? window.getId() : "window is null"), e);
        }
        try {
            window.doEmbedded();
        } catch (Exception e) {
            LOGGER.error("error setting layer legend", e);
        }
    }

    public void redrawLayersList() {
        int idx = activeLayersList.getSelectedIndex();
        List<MapLayer> activeLayers = getPortalSession().getActiveLayers();
        activeLayersList.setModel(new ListModelList(activeLayers, true));
        activeLayersList.setSelectedIndex(idx);

        if (layerLegendNameRefresh != null) {
            try {
                layerLegendNameRefresh.onEvent(new ForwardEvent("", this, null, llc2MapLayer.getDisplayName()));
            } catch (Exception e) {
                LOGGER.error("failed to refresh legend name with current map layer", e);
            }
        }

        lblSelectedLayer.setValue(llc2MapLayer.getDisplayName());
        adjustActiveLayersList();
    }

    public void onSelect$activeLayersList(Event event) {
        updateLayerControls();

        refreshContextualMenu();
    }

    public List<MapLayer> getPolygonLayers() {
        List<MapLayer> list = new ArrayList<MapLayer>();
        List<MapLayer> allLayers = getPortalSession().getActiveLayers();
        for (int i = 0; i < allLayers.size(); i++) {
            if (allLayers.get(i).isPolygonLayer()) {
                list.add(allLayers.get(i));
            }
        }

        return list;
    }

    public List<MapLayer> getSpeciesLayers() {
        List<MapLayer> list = new ArrayList<MapLayer>();
        List<MapLayer> allLayers = getPortalSession().getActiveLayers();
        for (int i = 0; i < allLayers.size(); i++) {
            if (allLayers.get(i).getSpeciesQuery() != null) {
                list.add(allLayers.get(i));
            }
        }

        return list;
    }

    public List<MapLayer> getGridLayers() {
        List<MapLayer> list = new ArrayList<MapLayer>();
        List<MapLayer> allLayers = getPortalSession().getActiveLayers();
        for (int i = 0; i < allLayers.size(); i++) {
            if (allLayers.get(i).isGridLayer()
                    && allLayers.get(i).getSubType() != LayerUtilitiesImpl.MAXENT
                    && allLayers.get(i).getSubType() != LayerUtilitiesImpl.GDM
                    && allLayers.get(i).getSubType() != LayerUtilitiesImpl.ALOC) {
                list.add(allLayers.get(i));
            }
        }

        return list;
    }

    public List<MapLayer> getAnalysisLayers() {
        List<MapLayer> list = new ArrayList<MapLayer>();
        List<MapLayer> allLayers = getPortalSession().getActiveLayers();
        for (int i = 0; i < allLayers.size(); i++) {
            if (allLayers.get(i).getSubType() == LayerUtilitiesImpl.MAXENT
                    || allLayers.get(i).getSubType() == LayerUtilitiesImpl.GDM
                    || allLayers.get(i).getSubType() == LayerUtilitiesImpl.ALOC
                    || allLayers.get(i).getSubType() == LayerUtilitiesImpl.ODENSITY
                    || allLayers.get(i).getSubType() == LayerUtilitiesImpl.SRICHNESS) {
                list.add(allLayers.get(i));
            }
        }

        return list;
    }

    public List<MapLayer> getContextualLayers() {
        List<MapLayer> list = new ArrayList<MapLayer>();
        List<MapLayer> allLayers = getPortalSession().getActiveLayers();
        for (int i = 0; i < allLayers.size(); i++) {
            if (allLayers.get(i).isContextualLayer()
                    && allLayers.get(i).getSubType() != LayerUtilitiesImpl.ALOC) {
                list.add(allLayers.get(i));
            }
        }

        return list;
    }

    public boolean isSelectedLayer(MapLayer ml) {
        return ml == getActiveLayersSelection(false);
    }

    public void setContextualMenuRefreshListener(EventListener contextualMenuRefreshListener) {
        this.contextualMenuRefreshListener = contextualMenuRefreshListener;
    }

    public void refreshContextualMenu() {
        if (contextualMenuRefreshListener != null) {
            try {
                contextualMenuRefreshListener.onEvent(null);
            } catch (Exception e) {
                LOGGER.error("failed to refresh contextual menu", e);
            }
        }
        adjustActiveLayersList();
    }

    /**
     * Searches the occurrences at a given point and then maps the polygon
     * feature found at the location (for the current top contextual layer).
     *
     * @param event triggered by the usual javascript trickery
     */
    public void exportArea(Event event) {
        openModal("WEB-INF/zul/output/ExportLayer.zul", null, StringConstants.ADDTOOLWINDOW);
    }

    public void onBaseMap(Event event) {
        String newBaseMap = (String) event.getData();
        getPortalSession().setBaseLayer(newBaseMap);
    }

    public String getBaseMap() {
        return getPortalSession().getBaseLayer();
    }

    public void adjustActiveLayersList() {
        if (activeLayersList != null && activeLayersList.getItems() != null) {
            for (Listitem li : activeLayersList.getItems()) {
                if (li.getValue() != null
                        && ((MapLayer) li.getValue()).getType() == LayerUtilitiesImpl.MAP) {

                    Listcell lc = (Listcell) li.getLastChild();

                    int checkedCount = 0;
                    for (Listitem i : activeLayersList.getItems()) {
                        if (i.getFirstChild().getFirstChild() != null
                                && ((Checkbox) i.getFirstChild().getFirstChild()).isChecked()) {
                            checkedCount++;
                        }
                    }

                    //Buttons are created in the ActiveLayerRenderer for the base map layer.
                    //update label
                    Div div = (Div) lc.getLastChild();
                    Button unsel = (Button) div.getLastChild();
                    Button sel = (Button) unsel.getPreviousSibling();
                    Button remove = (Button) sel.getPreviousSibling();

                    //if all but map options layer is selected, title = 'unselect all'
                    if (activeLayersList.getItemCount() == 1) {
                        sel.setVisible(false);
                        unsel.setVisible(false);
                        remove.setVisible(false);
                    } else if (checkedCount == activeLayersList.getItemCount()) {
                        sel.setDisabled(true);
                        unsel.setDisabled(false);
                        sel.setVisible(true);
                        unsel.setVisible(true);
                        remove.setVisible(true);
                    } else if (checkedCount == 1) {
                        sel.setDisabled(false);
                        unsel.setDisabled(true);
                        sel.setVisible(true);
                        unsel.setVisible(true);
                        remove.setVisible(true);
                    } else {
                        sel.setDisabled(false);
                        unsel.setDisabled(false);
                        sel.setVisible(true);
                        unsel.setVisible(true);
                        remove.setVisible(true);
                    }
                }
            }
        }
    }

    public void setLayersVisible(boolean show) {
        PortalSession portalSession = (PortalSession) Executions.getCurrent()
                .getDesktop()
                .getSession()
                .getAttribute(StringConstants.PORTAL_SESSION);

        for (Listitem li : activeLayersList.getItems()) {
            if (li.getValue() == null
                    || !"Map options".equals(((MapLayer) li.getValue()).getName())) {

                Checkbox cb = (Checkbox) li.getFirstChild().getFirstChild();

                if (show && !cb.isChecked()) {
                    openLayersJavascript.execute(
                            openLayersJavascript.getIFrameReferences()
                                    + openLayersJavascript.activateMapLayer((MapLayer) li.getValue(), false, true)
                                    + openLayersJavascript.updateMapLayerIndexes(
                                    portalSession.getActiveLayers()
                            )
                    );
                } else if (!show && cb.isChecked()) {
                    openLayersJavascript.removeMapLayerNow((MapLayer) li.getValue());
                }

                cb.setChecked(show);
                cb.setTooltiptext(show ? "Hide" : "Show");


            }
        }

        refreshContextualMenu();
    }

    public void importAnalysis(Event event) {
        openModal("WEB-INF/zul/input/ImportAnalysis.zul", null, "importanalysis");
    }

    private Map<String, String> getQueryParameterMap(String params) {
        if (params == null || params.length() == 0) {
            return null;
        }

        Map<String, String> map = new HashMap<String, String>();
        for (String s : params.split("&")) {
            String[] keyvalue = s.split("=");
            if (keyvalue.length >= 2) {
                String key = keyvalue[0];
                String value = keyvalue[1];
                try {
                    value = URLDecoder.decode(value, StringConstants.UTF_8);
                } catch (Exception e) {
                    LOGGER.error("error decoding to UTF-8: " + value, e);
                }
                map.put(key, value);
            }
        }

        return map;
    }

    public List<LayerSelection> getLayerSelections() {
        return selectedLayers;
    }

    public void addLayerSelection(LayerSelection ls) {
        //check for duplication
        for (int i = 0; i < selectedLayers.size(); i++) {
            if (selectedLayers.get(i).equalsList(ls)) {
                selectedLayers.get(i).setLastUse(System.currentTimeMillis());
                return;
            }
        }

        //add
        selectedLayers.add(ls);

        //if more than 10, remove the recod with oldest use
        if (selectedLayers.size() > 20) {
            int oldestIdx = 0;
            for (int i = 1; i < selectedLayers.size(); i++) {
                if (selectedLayers.get(i).getLastUse()
                        < selectedLayers.get(oldestIdx).getLastUse()) {
                    oldestIdx = i;
                }
            }
            selectedLayers.remove(oldestIdx);
        }
    }

    public void onClick$openDistributions(Event event) {
        String lsids = (String) event.getData();
        if (lsids != null && lsids.length() > 0) {
            closeExternalContentWindow();
            openDistributionSpecies(lsids, null);
        }
    }

    public void onClick$openChecklists(Event event) {
        String lsids = (String) event.getData();
        if (lsids != null && lsids.length() > 0) {
            closeExternalContentWindow();
            openChecklistSpecies(lsids, null, false);
        }
    }

    public void onClick$openAreaChecklist(Event event) {
        String geomIdx = (String) event.getData();
        if (geomIdx != null && geomIdx.length() > 0) {
            closeExternalContentWindow();
            openAreaChecklist(geomIdx, null, null);
        }
    }

    void closeExternalContentWindow() {
        //close any prevously opened externalcontentwindow
        try {
            Component c = getFellowIfAny("externalContentWindow");
            if (c != null) {
                LOGGER.debug("found externalContentWindow, closing");
                c.detach();
            }
        } catch (Exception e) {
            LOGGER.error("error closing externalContentWindow window", e);
        }
    }

    public Object getMapLayerWMS(String wmsurl) {
        // check if layer already present
        List udl = getPortalSession().getActiveLayers();
        Iterator iudl = udl.iterator();
        while (iudl.hasNext()) {
            MapLayer ml = (MapLayer) iudl.next();
            if (ml.getUri() != null && ml.getUri().equals(wmsurl)) {
                return ml;
            }
        }

        return null;
    }

    /**
     * Species are imported through the species list tool.
     *
     * @param event
     */
    public void importSpecies(Event event) {

        String type = (String) event.getData();
        if (type != null && type.length() > 0) {
            if ("assemblage".equalsIgnoreCase(type)) {
                Map params = new HashMap();
                params.put("enableImportAssemblage", true);
                openModal("WEB-INF/zul/add/AddSpecies.zul", params, "addspecieswindow");

            } else {
                if (StringUtils.isNotEmpty((String) CommonData.getSettings().getProperty("sandbox.url", null))
                        && CommonData.getSettings().getProperty("import.points.layers-service", "false").equals("false")) {
                    SandboxPasteController spc = (SandboxPasteController) Executions.createComponents("WEB-INF/zul/sandbox/SandboxPaste.zul", getMapComposer(), null);
                    spc.setAddToMap(true);
                    spc.doModal();
                } else {
                    Map params = new HashMap();
                    params.put("setTbInstructions", "3. Select file (comma separated ID (text), " +
                            "longitude (decimal degrees), latitude(decimal degrees))");
                    params.put("addToMap", true);
                    openModal("WEB-INF/zul/input/UploadSpecies.zul", params, "uploadspecieswindow");
                }


            }
        }
    }

    public void generatePoints(Event event) {
        openModal("WEB-INF/zul/tool/PointGeneration.zul", null, "addtoolwindow");
    }

    public void importAreas(Event event) {
        openModal("WEB-INF/zul/input/ImportAreas.zul", null, StringConstants.ADDAREAWINDOW);
    }

    public void saveUserSession(Event event) {
        LOGGER.debug("saving session");

        PrintWriter out = null;
        try {
            String jsessionid = getCookieValue("JSESSIONID");
            if (jsessionid == null) {
                jsessionid = "test";
            }

            String sfld = getSettingsSupplementary().getProperty(StringConstants.ANALYSIS_OUTPUT_DIR)
                    + "session/" + jsessionid;

            // if the session dir exists, then clear it,
            // if not let's create it.
            File sessfolder = new File(sfld + "/");
            if (!sessfolder.exists()) {
                sessfolder.mkdirs();
            } else {
                FileUtils.deleteDirectory(sessfolder);
                sessfolder.mkdirs();
            }

            StringBuilder sbSession = new StringBuilder();
            sbSession.append(String.valueOf(mapZoomLevel)).append(",").append(getLeftmenuSearchComposer()
                    .getViewportBoundingBox().toString());
            sbSession.append(System.getProperty("line.separator"));

            PersistenceStrategy strategy = new FilePersistenceStrategy(new File(sfld));
            List list = new XmlArrayList(strategy);

            String scatterplotNames = "";
            List udl = getPortalSession().getActiveLayers();
            Iterator iudl = udl.iterator();
            while (iudl.hasNext()) {
                MapLayer ml = (MapLayer) iudl.next();
                if (ml.getType() != LayerUtilitiesImpl.MAP) {
                    if (ml.getSubType() == LayerUtilitiesImpl.SCATTERPLOT) {
                        list.add(ml.getScatterplotDataDTO());
                        scatterplotNames += ((scatterplotNames.length() > 1) ? "___" + ml.getName() : ml.getName());
                    } else {
                        list.add(ml);
                    }
                }
            }
            sbSession.append("scatterplotNames=").append(scatterplotNames);
            sbSession.append(System.getProperty("line.separator"));

            out = new PrintWriter(new BufferedWriter(new FileWriter(sfld + "/details.txt")));
            out.write(sbSession.toString());
            out.close();

            String sessionurl = CommonData.getWebportalServer() + "/?ss=" + jsessionid;

            activateLink("*" + "<p>Your session has been saved and now available to share at <br />"
                    + "<a href='" + sessionurl + "'>" + sessionurl + "</a>"
                    + "<br />(Right-click on the link and to copy the link to clipboard)" + "</p>"
                    , "Saved session", false, "");
        } catch (IOException ex) {
            LOGGER.error("Unable to save session data: ", ex);
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    public void loadUserSession(String sessionid) {
        Scanner scanner = null;
        try {

            String sfld = getSettingsSupplementary().getProperty(StringConstants.ANALYSIS_OUTPUT_DIR)
                    + "session/" + sessionid;

            File sessfolder = new File(sfld);
            if (!sessfolder.exists()) {
                showMessage("Session information does not exist. Please provide a valid session id");
                return;
            }

            scanner = new Scanner(new File(sfld + "/details.txt"));

            // first grab the zoom level and bounding box
            String[] mapdetails = scanner.nextLine().split(",");

            BoundingBox bb = new BoundingBox();
            bb.setMinLongitude(Float.parseFloat(mapdetails[1]));
            bb.setMinLatitude(Float.parseFloat(mapdetails[2]));
            bb.setMaxLongitude(Float.parseFloat(mapdetails[3]));
            bb.setMaxLatitude(Float.parseFloat(mapdetails[4]));
            openLayersJavascript.setAdditionalScript(openLayersJavascript.zoomToBoundingBox(bb, true));

            String[] scatterplotNames = null;
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.startsWith("scatterplotNames")) {
                    scatterplotNames = line.substring(17).split("___");
                }
            }
            ArrayUtils.reverse(scatterplotNames);

            // ignore fields not found
            XStream xstream =
                    new XStream(new DomDriver()) {

                        protected MapperWrapper wrapMapper(MapperWrapper next) {
                            return new MapperWrapper(next) {
                                public boolean shouldSerializeMember(Class definedIn, String fieldName) {
                                    if (definedIn == Object.class || !super.shouldSerializeMember(definedIn, fieldName))
                                        System.out.println("faled to read: " + definedIn + ", " + fieldName);

                                    return definedIn != Object.class ? super.shouldSerializeMember(definedIn, fieldName) : false;
                                }
                            };
                        }

                        @Override
                        public Object unmarshal(HierarchicalStreamReader reader) {
                            Object o = super.unmarshal(reader);
                            if (o instanceof BiocacheQuery)
                                ((BiocacheQuery) o).getFullQ(false);
                            return o;
                        }

                        @Override
                        public Object unmarshal(HierarchicalStreamReader reader, Object root) {
                            Object o = super.unmarshal(reader, root);
                            if (o instanceof BiocacheQuery)
                                ((BiocacheQuery) o).getFullQ(false);
                            return o;
                        }

                        @Override
                        public Object unmarshal(HierarchicalStreamReader reader, Object root, DataHolder dataHolder) {
                            Object o = super.unmarshal(reader, root, dataHolder);
                            if (o instanceof BiocacheQuery)
                                ((BiocacheQuery) o).getFullQ(false);
                            return o;
                        }
                    };


            PersistenceStrategy strategy = new FilePersistenceStrategy(new File(sfld), xstream);

            List list = new XmlArrayList(strategy);

            ListIterator it = list.listIterator(list.size());
            int scatterplotIndex = 0;
            while (it.hasPrevious()) {
                Object o = it.previous();
                MapLayer ml = null;
                if (o instanceof MapLayer) {
                    ml = (MapLayer) o;
                    LOGGER.debug("Loading " + ml.getName() + " -> " + ml.isDisplayed());
                    addUserDefinedLayerToMenu(ml, false);
                } else if (o instanceof ScatterplotDataDTO) {
                    ScatterplotDataDTO spdata = (ScatterplotDataDTO) o;
                    loadScatterplot(spdata, "My Scatterplot " + scatterplotIndex++);

                }

                if (ml != null) {
                    addUserDefinedLayerToMenu(ml, true);
                }
            }

        } catch (Exception e) {
            try {

                File f = new File("/data/sessions/" + sessionid + ".txt");

                PrintWriter pw = new PrintWriter(f);

                e.printStackTrace(pw);

                pw.close();

            } catch (Exception ex) {

            }
            LOGGER.error("Unable to load session data", e);
            showMessage("Unable to load session data");

        } finally {
            if (scanner != null) {
                scanner.close();
            }
            try {


                File f = new File("/data/sessions/ok/" + sessionid + ".txt");

                FileUtils.writeStringToFile(f, "ok");

            } catch (Exception ex) {

            }
        }
    }

    public void updateAdhocGroup(Event event) {
        String[] params = ((String) event.getData()).split("\n");
        MapLayer ml = getMapLayer(params[0]);
        Query query;
        if (ml != null && (query = ml.getSpeciesQuery()) != null) {
            query.flagRecord(params[1], StringConstants.TRUE.equalsIgnoreCase(params[2]));
        }
        updateLayerControls();
    }

    public void downloadSecond(Event event) {
        SamplingDownloadUtil.downloadSecond(this, downloadSecondQuery, downloadSecondLayers);
    }

    public void openFacets(Event event) {
        if (facetsOpenListener != null) {
            try {
                facetsOpenListener.onEvent(null);
            } catch (Exception e) {
                LOGGER.error("failed to open the colour (facets) list", e);
            }
        }
    }

    public void onClick$downloadFeaturesCSV(Event event) {
        if (featuresCSV != null) {
            Filedownload.save(featuresCSV, "application/csv", "pointFeatures.csv");
        }
    }

    public void replaceWKTwithWMS(MapLayer ml) {
        //don't replace if it has a facet or is mapped with WMS or is WKT ENVELOPE
        if (ml.getFacets() != null
                || ml.getWKT() == null
                || ml.getWKT().startsWith(StringConstants.ENVELOPE)
                || ml.getUri() != null) {
            return;
        }

        //only replace if WKT upload is successful
        String pid = UserShapes.upload(ml.getWKT(), ml.getDisplayName(), ml.getDescription(), Util.getUserEmail(), CommonData.getSettings().getProperty("api_key"));
        if (pid != null) {
            //2. remove old layer
            deactiveLayer(ml, true, false);

            //1. create new layer
            MapLayer newml = addObjectByPid(pid, ml.getDisplayName());
            newml.setMapLayerMetadata(ml.getMapLayerMetadata());
            newml.setAreaSqKm(ml.getAreaSqKm());
            newml.setUserDefinedLayer(ml.isUserDefinedLayer());
            newml.setDescription(ml.getDescription());
            newml.setWKT(ml.getWKT());
        }
    }

    public void setLabelSelectedLayer(String labelSelectedLayer) {
        lblSelectedLayer.setValue(labelSelectedLayer);
    }

    public void setFacetsOpenListener(EventListener facetsOpenListener) {
        this.facetsOpenListener = facetsOpenListener;
    }

    public void setLayerLegendNameRefresh(EventListener layerLegendNameRefresh) {
        this.layerLegendNameRefresh = layerLegendNameRefresh;
    }

    public void setFeaturesCSV(String featuresCSV) {
        this.featuresCSV = featuresCSV;
    }

    public void setDownloadSecondQuery(Query downloadSecondQuery) {
        this.downloadSecondQuery = downloadSecondQuery;
    }

    public void setDownloadSecondLayers(String[] downloadSecondLayers) {
        this.downloadSecondLayers = downloadSecondLayers == null ? null : downloadSecondLayers.clone();
    }
}
