package au.org.emii.portal.event;


import au.org.emii.portal.composer.MapComposer;
import org.apache.log4j.Logger;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zul.Treecell;
import org.zkoss.zul.Treeitem;
import org.zkoss.zul.Treerow;

public abstract class PortalEvent { 
	protected Logger logger = Logger.getLogger(this.getClass());	
	
	/**
	 * Get the treeItem marked as target in the event
	 * @param event
	 * @return
	 */
	protected Treeitem getTarget(Event event) {
		Treecell treecell = (Treecell) event.getTarget();
		Treerow treerow = null;
		Treeitem target = null;
		if (treecell != null) {
			treerow = (Treerow) treecell.getParent();
			if (treerow != null) {
				target = (Treeitem) treerow.getParent();
			}
		}
		return target;
	}
	
	
	protected MapComposer getMapComposer(Event event) {
		MapComposer mapComposer = null;
		Page page = null;
		if (	(event != null) && 
				((page = event.getPage()) != null) &&
				((mapComposer = (MapComposer) page.getFellow("mapPortalPage")) != null)) {
		}
		else {
			logger.info(
					"Unable to obtain reference to mapPortalPage instance to perform changes " +
					"I think this is a strange concurrent access problem, I've only ever seen " +
					"it once (ignoring and proceeding normally)"
			);			
		}

		return mapComposer;
	}
}
