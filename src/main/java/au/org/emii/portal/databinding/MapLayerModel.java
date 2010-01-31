package au.org.emii.portal.databinding;

import au.org.emii.portal.MapLayer;
import au.org.emii.portal.MenuGroup;
import au.org.emii.portal.MenuItem;
import au.org.emii.portal.TreeMenuItem;
import org.zkoss.zul.AbstractTreeModel;
import org.zkoss.zul.event.TreeDataEvent;
import org.apache.log4j.Logger;

public class MapLayerModel extends AbstractTreeModel {
	private static final long serialVersionUID = 1L;
	private MenuGroup tree = null;
	private Logger logger = Logger.getLogger(this.getClass());
	
	public MapLayerModel(Object root) {
		
		super(root);
		tree = (MenuGroup) root;
		logger.debug("tree got setup...");
	}

	public Object getRoot() {
		return tree;
	}

	public Object getChild(Object parent, int index) {
		return ((TreeMenuItem)parent).getChild(parent, index);
	}

	public int getChildCount(Object parent) {
		if (isLeaf(parent)) {
			return -1;
		}
		else {
			return ((TreeMenuItem) parent).getChildCount(parent);
		}
	}

	public boolean isLeaf(Object parent) {
		return ((TreeMenuItem) parent).isLeaf(parent);
	}
	
	/**
	 * 
	 * 
	 * Fixme - parent is going to be a LIST in some cases -how to handle?
	 * for tomorrw!
	 * 
	 * 
	 * @param mapLayer
	 * @param selected
	 */
	public void changeSelection(MapLayer mapLayer, boolean selected) {
		if (tree != null) {
			if (mapLayer != null) {
				MenuItem holder = getTreeMenuItemForMaplayer(mapLayer);
				if (holder == null) {
					logger.debug("no holder for found for MapLayer " + mapLayer.getId());
				}
				else if (holder.getParent() == null ) {
					logger.debug("node " + mapLayer.getName() + " NULL parent - refusing to changeSelection()");
				}
				else {
					int positionInTree = holder.getParent().getChildren().indexOf(holder);
					mapLayer.setListedInActiveLayers(selected);
		
						/*
						 * There may yet be a use for this block of code...
						if (selected) {
							operation = TreeDataEvent.INTERVAL_REMOVED;
						}
						else {
							operation = TreeDataEvent.INTERVAL_ADDED;
						}
						*/
							
						// for now everything is an update
						int operation = TreeDataEvent.CONTENTS_CHANGED;
							
						fireEvent(holder.getParent(),positionInTree,positionInTree,operation);
		
				}
			}
			else {
				logger.debug("MapLayer instance is null - can't perform search");
			}
		}
		else {
			logger.debug("Tree is null - can't perform search");
		}
	}


	private MenuItem getHolder(MapLayer mapLayer, TreeMenuItem search) {
		MenuItem item = null;
		if (search.getConcreteType() == TreeMenuItem.CONCRETE_TYPE_MENUITEM) {
			MenuItem menuItem = (MenuItem) search;
			if (menuItem.isHoldingMapLayerInstance(mapLayer)) {
				item = menuItem;
			}
		}		
		return item;
	}
	
	public MenuItem getTreeMenuItemForMaplayer(MapLayer mapLayer) {
		return getTreeMenuItemForMaplayer(mapLayer, tree);
	}
	
	public boolean isHoldingMapLayer(MapLayer mapLayer) {
		return (getTreeMenuItemForMaplayer(mapLayer) != null) ? true : false;
	}
	
	public MenuItem getTreeMenuItemForMaplayer(MapLayer mapLayer, TreeMenuItem search) {
		MenuItem item = getHolder(mapLayer, search);
		if (item == null) { 
			int i = 0;
			while (item == null && i < search.getChildCount(search)) {				
				item = getTreeMenuItemForMaplayer(mapLayer, (TreeMenuItem) search.getChild(search, i));
				i++;
			}
		}
		return item;
	}
	
	public void updateTreeItemRemoved(Object parent, int treePosition) {
		fireEvent(parent,treePosition,treePosition,TreeDataEvent.INTERVAL_REMOVED);
		
	}
	
	// add the last child of parent
	public boolean updateTreeItemAdded(Object parent) {
		MenuItem parentLayer = (MenuItem) parent;
		boolean treeUpdated = false;
		int lastElement = parentLayer.getChildCount(parentLayer) - 1;
		if (lastElement >= 0) {
			logger.debug("fireing tree update");
			/* note - even though this event gets fired, if the menu
			 * is not on the screen and visible, it won't be updated.
			 * 
			 * This can happen if you add to the user defined layers
			 * menu by adding map layers from the search tab.
			 * 
			 * Solution is to force rerender when changing to the 
			 * layers tab
			 */
			fireEvent(parent,lastElement,lastElement,TreeDataEvent.INTERVAL_ADDED);
			treeUpdated = true;
		}
		else {
			logger.debug(
				"not updating tree because lastElement < 0; lastElement ==" + lastElement
			);
		}
		return treeUpdated;
	}
}
