package au.org.emii.portal;

import org.zkoss.zul.Comboitem;
import org.zkoss.zul.ComboitemRenderer;
import org.zkoss.zul.Image;
import org.zkoss.zul.Popup;


/**
 * Render the style list
 * @author geoff
 *
 */
public class StyleRenderer implements ComboitemRenderer {

	public void render(Comboitem item, Object data) throws Exception {

		WMSStyle style = (WMSStyle) data;
		
		// display the style title to the user as a label
		item.setLabel(style.getTitle());
		
		if (
				(style.getDescription() != null) && 
				(! style.getDescription().equals(""))) {
			/* set a description in grey text (by default) underneath
			 * each entry if one is available
			 */
			item.setDescription(style.getDescription());
		}


		// create an anonymous Image instance for the tooltip
		Popup tooltip = new Popup();
		new Image(style.getLegendUri()).setParent(tooltip);
		tooltip.setParent(item.getRoot());
		
		item.setTooltip(tooltip);
	}


}
