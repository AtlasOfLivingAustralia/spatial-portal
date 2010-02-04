package au.org.emii.portal.databinding;

import au.org.emii.portal.LayerUtilities;
import au.org.emii.portal.MenuGroup;
import au.org.emii.portal.MenuItem;
import au.org.emii.portal.event.MenuTreeClickEventListener;
import au.org.emii.portal.config.Config;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.Treecell;
import org.zkoss.zul.Treeitem;
import org.zkoss.zul.TreeitemRenderer;
import org.zkoss.zul.Treerow;
import org.apache.log4j.Logger;


/**
 * Custom renderer for drawing the tree menu - based loosely on the example from
 * http://www.zkoss.org/smalltalks/zkTreeModel/PersonTreeitemRenderer.java
 * 
 * @author geoff
 *
 */
public class MapLayerItemRenderer implements TreeitemRenderer {
	protected Logger logger = Logger.getLogger(this.getClass());
	
	public void render(Treeitem treeitem, Object data) throws Exception {

		if (data instanceof MenuGroup) {
			render(treeitem, (MenuGroup) data);
		}
		else if (data instanceof MenuItem) {
			render(treeitem, (MenuItem) data);
		}
		else {
			logger.warn(
					"Don't know how to render '" + data.getClass().getName() + "' skipping"
			);
		}
	}
	
	private void render(Treeitem treeitem, MenuGroup menuGroup) {
		Treerow row = setupTreerow(treeitem);
		
		String label = menuGroup.getName();
		Treecell cell = new Treecell(label);
		cell.setParent(row);
		
		// notice MenuGroup instances do NOT render a value!
		addEventListener(cell);
	}
	
	
	private void addEventListener(Treecell treecell) {
		if (! treecell.isListenerAvailable("onClick", false)) {
			treecell.addEventListener("onClick", new MenuTreeClickEventListener());
		}
	}

	private void render(Treeitem treeitem, MenuItem menuItem) {
		Treerow row = setupTreerow(treeitem);
		
		String label = menuItem.getName();
		Treecell cell = new Treecell();
		
		// This may chomp link/group objects as well but this is 
		// desired behaviour
		Label text = new Label(LayerUtilities.chompLayerName(label));
		text.setParent(cell);
		cell.setParent(row);
		
		treeitem.setValue(menuItem);

		
		// tooltip 
		treeitem.setTooltiptext(LayerUtilities.getTooltip(menuItem.getName(), menuItem.getDescription()));
		
		// for maplayers, disable the tree item if its in active
		// layers
		if (! menuItem.isValueSet()) {
			logger.info("Hiding MenuItem " + menuItem.getId() + " because it has a null value");
			treeitem.setVisible(false);
		}
		else if (menuItem.isValueMapLayerInstance()) {
			// disable map layers already listed in active layers
			treeitem.setDisabled(
				menuItem.getValueAsMapLayer().isListedInActiveLayers()
			);
		}
		else if (menuItem.isValueLinkInstance()) {
			// add a link symbol to links in the menu 
			Image image = new Image(Config.getLang("layer_link_icon"));
			image.setSclass("extlink");
			image.setParent(cell);
		}
		
		addEventListener(cell);
	}
	
	private Treerow setupTreerow(Treeitem treeitem) {
		Treerow row = null;	
		if (treeitem.getTreerow() == null) {
			row = new Treerow();
			row.setParent(treeitem);
		}
		else {
			row = treeitem.getTreerow();
			row.getChildren().clear();
		}
		return row;
	}


}
