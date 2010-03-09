package au.org.emii.portal.menu;

import au.org.emii.portal.value.AbstractIdentifierImpl;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class MenuItem extends AbstractIdentifierImpl implements TreeMenuItem, Cloneable, Serializable {

	private static final long serialVersionUID = 1L;
	private static final int CONCRETE_TYPE = TreeMenuItem.CONCRETE_TYPE_MENUITEM;
	private TreeMenuItem parent = null;
	private TreeMenuValue value = null;
	private List<MenuItem> children = new ArrayList<MenuItem>();
	
	public MenuItem(MapLayer mapLayer) {
		this.setValue(mapLayer);
		for (MapLayer child : mapLayer.getChildren()) {
			MenuItem menuChild = new MenuItem(child);
			addChild(menuChild);
		}
	}
	
	public MenuItem(Link link) {
		this.setValue(link);
	}

	@SuppressWarnings("unused")
	private MenuItem() {}
	
	public String dump(String indent) {
		String dump = 
			"MI" + indent + getName() + " (" +  getId() + ") " + 
			" parent=" + getParent().getId();
		
		if (getValue() == null) {
			dump += " value instance NULL\n"; 
		}
		else {
			dump += " value instance=" + getValue().getClass().getName() + "\n";
		}
		
		for (MenuItem child : children) {
			dump += child.dump(indent + "  ");
		}
		return dump;
	}

	public Object getChild(Object parent, int index) {
		return ((TreeMenuItem)parent).getChild(index);
	}

	public int getChildCount(Object parent) {
		return ((TreeMenuItem)parent).getChildCount();
	}

	public List<MenuItem> getChildren() {
		return children;
	}

	public TreeMenuItem getParent() {
		return parent;
	}

	public boolean isLeaf(Object parent) {
		return ((TreeMenuItem) parent).isLeaf();
	}

	public void setParent(TreeMenuItem treeMenuItem) {
		this.parent = treeMenuItem;
	}

	public TreeMenuValue getValue() {
		return value;
	}

	public void setValue(TreeMenuValue value) {
		this.value = value;
		setId(value.getId());
		setName(value.getName());
		setDescription(value.getDescription());
	}

	public void addChild(MenuItem child) {
		child.setParent(this);
		children.add(child);
	}
	
	public boolean isValueMapLayerInstance() {
		return 
			isValueSet() && (value instanceof MapLayer);
	}
	
	public MapLayer getValueAsMapLayer() {
		return (MapLayer) getValue();
	}
	
	public Link getValueAsLink() {
		return (Link) getValue();
	}
	
	public boolean isValueSet() {
		return (value != null);
	}
	
	public boolean isValueLinkInstance() {
		return 
			isValueSet() && (value instanceof Link);
	}

	public int getConcreteType() {
		return CONCRETE_TYPE;
	}
	
	/**
	 * Test whether this instance of MenuItem is holding the 
	 * passed in MapLayer instance in it's value field
	 * @param instance MapLayer instance to be tested against
	 * the value field
	 * @return true if we are holding a MapLayer instance in 
	 * the value field and it is the same MapLayer instance 
	 * as the instance parameter
	 */
	public boolean isHoldingMapLayerInstance(MapLayer instance) {
		boolean holding = false;
		if (isValueMapLayerInstance()) {
			if (getValueAsMapLayer() == instance) {
				holding = true;
			}
		}
		return holding;
	}
	
	/**
	 * Return the position of this instance of MenuItem
	 * in it's parent's children list
	 * @return position in the parent's list or -1 if 
	 * unlisted
	 */
	public int positionInTree() {
		return getParent().getChildren().indexOf(this);
	}
	
	public Object clone() throws CloneNotSupportedException {
		MenuItem clone = (MenuItem) super.clone();
		clone.parent = null;
		clone.children = new ArrayList<MenuItem>();
		clone.value = null;
		
		if (children != null) {
			for (MenuItem child : children) {
				MenuItem childClone = (MenuItem) child.clone();
				
				clone.addChild(childClone);
				
				// 'fix' the parent :)
				child.setParent(childClone);
				
				// NOTE: Value field is NOT cloned here! it has to
				// be put in by 'somthing else' - see PortalSession
			}
		}
		return clone;
	}

	public Object getChild(int index) {
		return children.get(index);
	}

	public int getChildCount() {
		return children.size();
	}

	public boolean isLeaf() {
		return ! (children.size() >0);
	}
}
