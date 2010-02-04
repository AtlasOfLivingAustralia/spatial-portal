
package au.org.emii.portal.userdata;

import au.org.emii.portal.BoundingBox;
import au.org.emii.portal.MapLayer;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author brendon
 */
public class UserMap implements Serializable {


    public static final int VIEW_FACILITY = 0;
    public static final int VIEW_REGION = 1;
    public static final int VIEW_USER = 2;
    public static final int VIEW_REALTIME = 3;
    public final static int LAYER_TAB = 0;
    public final static int SEARCH_TAB = 1;
    public final static int LINK_TAB = 2;

    private long id;
    private List<MapLayer> activeLayers = new ArrayList<MapLayer>();
    private List<MapLayer> userDefinedLayers = new ArrayList<MapLayer>();
    private int currentView = VIEW_FACILITY;
    private MapLayer currentBaseLayer = null;
    private String selectedFacilityOrRegionId = null;
    private BoundingBox bBox = null;
    private int currentNavigationTab = LAYER_TAB;;
    private boolean maximised = false;

    public long getId() {
        return this.id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public List<MapLayer> getActiveLayers() {
        return activeLayers;
    }

    public void setActiveLayers(List<MapLayer> activeLayers) {
        this.activeLayers = activeLayers;
    }

    public BoundingBox getbBox() {
        return bBox;
    }

    public void setbBox(BoundingBox bBox) {
        this.bBox = bBox;
    }

    public MapLayer getCurrentBaseLayer() {
        return currentBaseLayer;
    }

    public void setCurrentBaseLayer(MapLayer currentBaseLayer) {
        this.currentBaseLayer = currentBaseLayer;
    }

    public int getCurrentNavigationTab() {
        return currentNavigationTab;
    }

    public void setCurrentNavigationTab(int currentNavigationTab) {
        this.currentNavigationTab = currentNavigationTab;
    }

    public int getCurrentView() {
        return currentView;
    }

    public void setCurrentView(int currentView) {
        this.currentView = currentView;
    }

    public boolean isMaximised() {
        return maximised;
    }

    public void setMaximised(boolean maximised) {
        this.maximised = maximised;
    }

    public String getSelectedFacilityOrRegionId() {
        return selectedFacilityOrRegionId;
    }

    public void setSelectedFacilityOrRegionId(String selectedFacilityOrRegionId) {
        this.selectedFacilityOrRegionId = selectedFacilityOrRegionId;
    }

    public List<MapLayer> getUserDefinedLayers() {
        return userDefinedLayers;
    }

    public void setUserDefinedLayers(List<MapLayer> userDefinedLayers) {
        this.userDefinedLayers = userDefinedLayers;
    }





}
