package au.org.emii.portal;

import org.zkoss.zul.Listitem;
import org.zkoss.zul.ListitemRenderer;

public class BaseLayerListRenderer implements ListitemRenderer {

	public void render(Listitem listitem, Object data) throws Exception {
		MapLayer baseLayer = (MapLayer) data;
		listitem.setLabel(baseLayer.getName());
		listitem.setValue(baseLayer.getId());
		listitem.setTooltiptext(baseLayer.getDescription());
	}

}
