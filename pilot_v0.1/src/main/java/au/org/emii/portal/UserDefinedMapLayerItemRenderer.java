package au.org.emii.portal;

import org.zkoss.zul.Image;
import org.zkoss.zul.Treecell;
import org.zkoss.zul.Treeitem;
import org.zkoss.zul.Treerow;

/**
 * Basically do the same as MapLayerItemRenderer but also
 * draw a remove icon so the user can delete layers
 * @author geoff
 *
 */
public class UserDefinedMapLayerItemRenderer extends MapLayerItemRenderer {
	public void render(Treeitem treeitem, Object data) throws Exception {
		super.render(treeitem, data);
		
		// Check we are rending a MapLayer instance - should always be...
		if (data instanceof MenuItem) {
			/* We want to make two treecells next to each other.  One will
			 * contain the label and click action, the other will contain
			 * the bin icon	 
			 */
			
			// treeitem should always have a first child which is 
			// a treecell
			Treerow row = (Treerow) treeitem.getChildren().get(0);
			
			// add a new treecell
			Treecell cell = new org.zkoss.zul.Treecell();
			cell.setParent(row);
			
			// put image in it
			Image image = new Image(Config.getLang("layer_remove_icon"));
			image.setParent(cell);
			
			cell.addEventListener("onClick", new UserDefinedMapLayerRemoveEventListener());
		}
		else {
			logger.warn(
					"Don't know how to render '" + data.getClass().getName() + "' skipping"
			);
		}
	}
}
