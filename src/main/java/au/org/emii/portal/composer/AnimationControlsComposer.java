package au.org.emii.portal.composer;

import au.org.emii.portal.menu.AnimationSelection;
import au.org.emii.portal.util.LayerUtilities;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.menu.MapLayerMetadata;
import au.org.emii.portal.wms.NcWMSSupport;
import au.org.emii.portal.javascript.OpenLayersJavascript;
import au.org.emii.portal.wms.ThreddsSupport;
import au.org.emii.portal.util.Validate;
import au.org.emii.portal.settings.SettingsSupplementary;
import au.org.emii.portal.lang.LanguagePack;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.AbstractMap;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.WrongValueException;
import org.zkoss.zul.Button;
import org.zkoss.zul.Label;
import org.zkoss.zul.Window;
import au.org.emii.zk.DateboxPlus;

public class AnimationControlsComposer extends GenericAutowireAutoforwardComposer {

    private static final long serialVersionUID = 1L;
    /**
     * The window containing all controls
     */
    private Window animationControls;
    /**
     * Start date
     */
    private DateboxPlus startDatebox;
    /**
     * End date
     */
    private DateboxPlus endDatebox;
    /**
     * Start animation button
     */
    private Button startButton;
    /**
     * Stop animation button
     */
    private Button stopButton;
    private Label invalidStartDate;
    private Label invalidEndDate;
    private LanguagePack languagePack = null;
    private OpenLayersJavascript openLayersJavascript = null;
    private LayerUtilities layerUtilities = null;
    private SettingsSupplementary settingsSupplementary = null;
    private NcWMSSupport ncWMSSupport = null;
    private ThreddsSupport threddsSupport = null;

    public LanguagePack getLanguagePack() {
        return languagePack;
    }

    public void setLanguagePack(LanguagePack languagePack) {
        this.languagePack = languagePack;
    }

    public boolean animationDatesValid() {
        boolean valid = false;

        Date start = startDatebox.getValue();
        Date end = endDatebox.getValue();

        if ((start != null)
                && (end != null)
                && (start.before(end))
                && startDatebox.isValid()
                && endDatebox.isValid()) {

            valid = true;
        }
        return valid;
    }

    public void onClick$startButton() {
        MapComposer mc = getMapComposer();
        if (mc.safeToPerformMapAction()) {
            MapLayer activeLayer = mc.getActiveLayersSelection(true);
            if (activeLayer != null) {
                AnimationSelection as = new AnimationSelection();
                activeLayer.setAnimationSelection(as);

                // 1) check dates are valid
                if (animationDatesValid()) {

                    // 2) populate maplayer.animationSelection
                    as.setStartDate(startDatebox.getValue());
                    as.setEndDate(endDatebox.getValue());

                    // 3) ask ncwms for the the timeStrings
                    // (will be stored within activeLayer)
                    // This process can fail if the server is down
                    switch (activeLayer.getType()) {
                        case LayerUtilities.NCWMS:
                            logger.debug("NCWMS animation settings...");
                            startNcWMSAnimation(activeLayer);
                            break;
                        case LayerUtilities.THREDDS:
                            logger.debug("THREDDS animation settings...");
                            startThreddsAnimation(activeLayer);
                            break;
                        default:
                            logger.info(
                                    "unsupported animation type requested: " + activeLayer.getType());
                    }
                } else {
                    mc.showMessage(languagePack.getLang("invalid_animation_date"));
                }
            } else {
                logger.info(
                        "user requested start animation but no layer is"
                        + "selected in active layers");
            }
        }
    }

    private void startNcWMSAnimation(MapLayer activeLayer) {
        if (ncWMSSupport.animationDateStrings(activeLayer)) {
            // NCWMS: user must now select the framerate
            animationFramesPopup(activeLayer);
        } else {
            getMapComposer().showMessage(languagePack.getLang("error_getting_timestrings"));
        }
    }

    private void startThreddsAnimation(MapLayer activeLayer) {
        /**
         * Thredds doesn't support framerates so we can just pick our
         * own dates, as long as they are within
         */
        if (threddsSupport.animationDateStrings(activeLayer)) {
            if (threddsSupport.maxFramesExceeded(activeLayer)) {
                getMapComposer().showMessage(
                        languagePack.getCompoundLang(
                        "thredds_frames_exceeded",
                        new Object[]{
                            Integer.valueOf(ThreddsSupport.MAXIMUM_ALLOWED_FRAMES),
                            Integer.valueOf(threddsSupport.countSelectedFrames(activeLayer))
                        }));
            } else {
                activateAnimation(activeLayer);
            }
        } else {
            // error - should never happen ...
            getMapComposer().showMessage(languagePack.getLang("error_getting_timestrings"));
        }
    }

    // 4) give user a popup asking how many frames, then ask mapcontroller
    // to get the party started
    private void animationFramesPopup(MapLayer activeLayer) {
        Map<String, AbstractMap<String, String>> params = new HashMap<String, AbstractMap<String, String>>();
        params.put("timeString", activeLayer.getAnimationSelection().getTimeStrings());
        AnimationFramesComposer window = (AnimationFramesComposer) Executions.createComponents("WEB-INF/zul/AnimationFrames.zul", null, params);
        window.setActiveLayer(activeLayer);
        window.doOverlapped();
    }

    public void activateAnimation(MapLayer activeLayer) {
        MapComposer mc = getMapComposer();
        //if (imageTester.testAnimationImage(activeLayer)) {
            // we requested the image and it checks out ok

            // mark the layer as animated, hide the 'start' button
            activeLayer.setCurrentlyAnimated(true);
            updateAnimationStartStopButtons(activeLayer);

            // update our row in activeLayers to show the correct
            // icon
            mc.refreshActiveLayer(activeLayer);

            // let rip
            openLayersJavascript.execute(
                    openLayersJavascript.iFrameReferences
                    + openLayersJavascript.removeMapLayer(activeLayer)
                    + openLayersJavascript.animate(activeLayer)
                    + openLayersJavascript.updateMapLayerIndexes(
                    mc.getPortalSession().getActiveLayers()));
        //} else {
        //    mc.errorMessageBrokenWMSLayer(imageTester);
        //}
    }

    private void updateAnimationStartStopButtons(MapLayer mapLayer) {
        boolean startButtonDisabled = mapLayer.isCurrentlyAnimated();
        startButton.setVisible(!startButtonDisabled);
        stopButton.setVisible(startButtonDisabled);
    }

    public void onClick$stopButton() {
        MapComposer mc = getMapComposer();
        if (mc.safeToPerformMapAction()) {
            MapLayer activeLayer = mc.getActiveLayersSelection(true);
            if (activeLayer != null) {
                activeLayer.setCurrentlyAnimated(false);
                updateAnimationStartStopButtons(activeLayer);
                mc.refreshActiveLayer(activeLayer);

                openLayersJavascript.execute(
                        openLayersJavascript.iFrameReferences
                        + openLayersJavascript.removeMapLayer(activeLayer)
                        + openLayersJavascript.activateMapLayer(activeLayer)
                        + openLayersJavascript.updateMapLayerIndexes(
                        mc.getPortalSession().getActiveLayers()));
            } else {
                logger.info(
                        "user requested stop animation but no layer is selected");
            }
        }

    }

    /**
     * Pre-select the date range in the start/end animation date
     * boxes.
     *
     * For ncWMS, we can select the whole date range with reasonable
     * performance
     * @throws ParseException
     * @throws WrongValueException
     */
    private void preSelectNcWMSDateRange(List<String> validDates) throws WrongValueException, ParseException {
        logger.debug("selecting maximum date range for ncwms");
        DateFormat sd = Validate.getShortIsoDateFormatter();
        startDatebox.setValue(
                sd.parse(validDates.get(0)));
        endDatebox.setValue(
                sd.parse(
                validDates.get(validDates.size() - 1)));
    }

    /**
     * Pre-select the date range in the start/end animation date
     * boxes.
     *
     * For Thredds, we have to restrict the selected range for
     * performance, so that only a maximum of
     * ThreddsSupport.MAXIMUM_ALLOWED_FRAMES are selected.
     *
     * We will preselect MAXIMUM_ALLOWED_FRAMES of the latest
     * dates we have data for
     * @throws ParseException
     * @throws WrongValueException
     */
    private void preSelectThreddsDateRange(List<String> validDates) throws WrongValueException, ParseException {
        logger.debug("restricting THREADS date range to " + ThreddsSupport.MAXIMUM_ALLOWED_FRAMES + " frames");
        DateFormat sd = Validate.getShortIsoDateFormatter();

        /* protect against the case where there are less than
         * MAXIMUM_ALLOWED_FRAMES of animation available
         */
        int startIndex = Math.max(0, validDates.size() - settingsSupplementary.getValueAsInt(ThreddsSupport.MAXIMUM_ALLOWED_FRAMES));
        startDatebox.setValue(
                sd.parse(validDates.get(startIndex)));
        endDatebox.setValue(
                sd.parse(
                validDates.get(validDates.size() - 1)));
    }

    public void updateAnimationControls(MapLayer currentSelection) {
        MapLayerMetadata animationParameters =
                currentSelection.getMapLayerMetadata();
        if ((layerUtilities.supportsAnimation(currentSelection.getType()))
                && (animationParameters != null)) {

            List<String> validDates = animationParameters.getDatesWithData();
            updateAnimationStartStopButtons(currentSelection);

            startDatebox.setValidDates(validDates);
            endDatebox.setValidDates(validDates);

            if (validDates.size() > 1) {
                try {
                    switch (currentSelection.getType()) {
                        case LayerUtilities.THREDDS:
                            preSelectThreddsDateRange(validDates);
                            break;
                        case LayerUtilities.NCWMS:
                            preSelectNcWMSDateRange(validDates);
                            break;
                        default:
                            logger.warn("unsupported type " + currentSelection.getType());
                    }

                    invalidStartDate.setVisible(false);
                    invalidEndDate.setVisible(false);

                    animationControls.setVisible(true);

                } catch (WrongValueException e) {
                    logger.error(e);
                } catch (ParseException e) {
                    logger.error(e);
                }
            } else {
                logger.debug(
                        "less than 2 dates available for animation so not showing controls");
            }

        } else {
            animationControls.setVisible(false);
        }
    }

    /**
     * Reset selected date range to max allowable
     */
    public void onClick$resetAnimationDateRange() {
        updateAnimationControls(getMapComposer().getActiveLayersSelection(true));
    }

    public void onChange$startDatebox() {
        logger.debug("start date changed");
        invalidStartDate.setVisible(!startDatebox.isValid());
    }

    public void onChange$endDatebox() {
        logger.debug("end date changed");
        invalidEndDate.setVisible(!endDatebox.isValid());
    }

    public OpenLayersJavascript getOpenLayersJavascript() {
        return openLayersJavascript;
    }

    public void setOpenLayersJavascript(OpenLayersJavascript openLayersJavascript) {
        this.openLayersJavascript = openLayersJavascript;
    }

    public LayerUtilities getLayerUtilities() {
        return layerUtilities;
    }

    public void setLayerUtilities(LayerUtilities layerUtilities) {
        this.layerUtilities = layerUtilities;
    }

    public SettingsSupplementary getSettingsSupplementary() {
        return settingsSupplementary;
    }

    public void setSettingsSupplementary(SettingsSupplementary settingsSupplementary) {
        this.settingsSupplementary = settingsSupplementary;
    }

    /*public ImageTester getImageTester() {
        return imageTester;
    }

    public void setImageTester(ImageTester imageTester) {
        this.imageTester = imageTester;
    }*/
}
