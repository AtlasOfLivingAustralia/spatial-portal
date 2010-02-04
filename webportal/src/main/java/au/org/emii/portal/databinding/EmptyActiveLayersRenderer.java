package au.org.emii.portal.databinding;

import au.org.emii.portal.event.ActiveLayerDNDEventListener;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.ListitemRenderer;

public class EmptyActiveLayersRenderer implements ListitemRenderer {

	public void render(Listitem item, Object data) throws Exception {
		item.setDraggable("false");
		item.setDroppable("true");
		
		item.setLabel((String) data);
		item.addEventListener("onDrop", new ActiveLayerDNDEventListener());
	}

}
