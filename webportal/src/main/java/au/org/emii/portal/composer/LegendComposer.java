package au.org.emii.portal.composer;

import au.org.emii.portal.request.DesktopState;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.settings.SettingsSupplementary;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.Popup;

public class LegendComposer extends UtilityComposer {
	
	private static final long serialVersionUID = 1L;
    private SettingsSupplementary settingsSupplementary = null;
	
	/**
	 * MapLayer instance to draw legend for
	 */
	private MapLayer mapLayer = null;
	
	private Image image;
	public Image stickyIcon;
	public Label description;
	
	private final static String WINDOW_STACKING_BOUNDARY = "window_stacking_boundary";
	
	private final static String WINDOW_STACKING_CONSTANT = "window_stacking_constant";
	

	/**
	 * Reposition and bring to top of stack
	 */
	public void reposition() {
		logger.debug("repositioned legend");
		setTop(getTopPosition());
		setLeft(getLeftPosition());		
		setFocus(true);
	}
	
	public MapLayer getMapLayer() {
		return mapLayer;
	}

	public void setMapLayer(MapLayer mapLayer) {
		this.mapLayer = mapLayer;
		update();
	}
	
	public void update() {
		if (mapLayer != null) {
			String uri = mapLayer.getCurrentLegendUri();
			logger.debug("using '" + uri + "' for legend image");
			image.setSrc(uri);
			if (description.isVisible()) {
				// we are still in a popup so we need to set the label;
				description.setValue(mapLayer.getName());
			}
			else {
				// we are a free-floating window, just use the title
				setTitle(mapLayer.getName() + "  ");
			}
		}
		else {
			logger.info("LegendComposer.update() called with null map layer");
		}
	}
	
	/** 
	 * Deregister the legend window for updates - this method
	 * gets autowired so doesn't (mustn't!) be forwarded in 
	 * the .zul file or you get a stack overflow
	 */
	public void onClose() {
		logger.debug("deregistering legend window");
		getMapComposer().getDesktopState().removeVisibleLegend(this);
		detach();
	}
	
	
	public void extractFromPopup() {
		logger.debug("onClick$stickyIcon()");
		DesktopState ds = getMapComposer().getDesktopState();
		
		if (ds.getVisibleLegendByMapLayer(mapLayer) == null) {
		
			Popup popup = (Popup) Executions.createComponents("/WEB-INF/zul/LegendPopup.zul", getRoot(), null);
			LegendComposer lc = (LegendComposer) popup.getFirstChild();
			
			// setup the window for being movable, etc.
			lc.setBorder("normal");
			lc.setClosable(true);
			
			// extract from popup
			lc.setParent(getRoot());	
			popup.detach();
			
			lc.doOverlapped();
			
			// get rid of label and sticky icon
			lc.stickyIcon.setVisible(false);
			lc.description.setVisible(false);
							
			// hide the hover popup
			Popup parent = (Popup) getParent();
			parent.close();
			
			
			logger.debug("registering legend window");
			ds.addVisibleLegend(lc);
		
			lc.setMapLayer(mapLayer);
			lc.reposition();
		}
		else {
			logger.info(
				"LegendComposer.extractFromPopup() unable to find " +
				"reference to itself in DesktopSession instance"
			);
		}
		
	}
	
	public void onClick$stickyIcon() {
		extractFromPopup();
		reposition();
	}
		
	/**
	 * Calculate a position for left
	 */
	public String getLeftPosition() {	
		int menuWidth = settingsSupplementary.getValueAsPx("menu_default_width");
		int offset = getStackingOffset(); 
		String pos = (menuWidth + offset) + "px";
		logger.debug("left set to " + pos);
		return pos;
	}
	
	/**
	 * Get our index position in portalSession * the stack size
	 * @return
	 */
	private int getStackingOffset() {
		int index = getMapComposer().getDesktopState().getVisibleLegends().indexOf(this);
		int offset;
		if (index > -1) {
			offset = (  index *
                        settingsSupplementary.getValueAsInt(WINDOW_STACKING_BOUNDARY)) %
                        settingsSupplementary.getValueAsInt(WINDOW_STACKING_CONSTANT);
		}
		else {
			offset = 0;
			logger.info("stacking offset position requested for unregistered window");
		}
		return offset;
			
	}
	
	/**
	 * Calculate a position for top
	 */
	public String getTopPosition() {
		int north = settingsSupplementary.getValueAsPx("north_border_height");
		int offset = getStackingOffset();
		String pos =  (offset + north) + "px";
		logger.debug("top set to " + pos);
		return pos;
	}

    public SettingsSupplementary getSettingsSupplementary() {
        return settingsSupplementary;
    }

    public void setSettingsSupplementary(SettingsSupplementary settingsSupplementary) {
        this.settingsSupplementary = settingsSupplementary;
    }


}
