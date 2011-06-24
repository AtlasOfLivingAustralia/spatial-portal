package au.org.emii.portal.request;

import au.org.emii.portal.composer.LegendComposer;
import java.util.ArrayList;
import java.util.List;
import au.org.emii.portal.menu.MapLayer;

/**
 * Storage for objects we want to store per Desktop, not per session.
 * 
 * This is to allow temporary storage of things like zk windows which
 * we might want to store for a little while but do not want to keep
 * in the session.
 * 
 * If we do keep them in the session instead of in here we get the 
 * following problems:
 * 1) memory leaks (can't be GCed)
 * 2) doesn't work anyway (wrong desktop)
 * 
 * @author geoff
 *
 */
public class DesktopState {
	/**
	 * List of map legends we are currently displaying (so we can update
	 * them when the user selects a different style)
	 */
	private List<LegendComposer> visibleLegends = new ArrayList<LegendComposer>(); 
	
	public List<LegendComposer> getVisibleLegends() {
		return visibleLegends;
	}

	public void setVisibleLegends(List<LegendComposer> visibleLegends) {
		this.visibleLegends = visibleLegends;
	}
	
	public void addVisibleLegend(LegendComposer window) {
		visibleLegends.add(window);
	}
	
	public void removeVisibleLegend(LegendComposer window) {
		visibleLegends.remove(window);
	}
	
	/**
	 * Find the LegendComposer (legend window) containing the legend
	 * for the passed in layer
	 * @param target
	 * @return
	 */
	public LegendComposer getVisibleLegendByMapLayer(MapLayer target) {
		LegendComposer found = null;
		LegendComposer inspect;
		int i = 0;
		while ((found == null) && (i < visibleLegends.size())) {
			inspect = visibleLegends.get(i);
			if (inspect.getMapLayer() == target) {
				found = inspect;
			}
			i++;
		}
		return found;
	}

}
