package au.org.emii.portal.menu;

import au.org.emii.portal.value.AbstractIdentifierImpl;
import au.org.emii.portal.menu.TreeMenuValue;
import au.org.emii.portal.menu.TreeMenuItem;
import java.util.List;
import au.org.emii.portal.config.xmlbeans.StaticLink;

public class Link extends AbstractIdentifierImpl implements TreeMenuValue  {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String uri = null;
	private boolean external = false;
	private TreeMenuItem parent = null;
	
	public String getUri() {
		return uri;
	}
	public void setUri(String uri) {
		this.uri = uri;
	}
	public boolean isExternal() {
		return external;
	}
	public void setExternal(boolean external) {
		this.external = external;
	}
	
	public void copyFrom(StaticLink staticLink) {
		this.setId(staticLink.getId());
		this.setName(staticLink.getName());
		this.setDescription(staticLink.getDescription());
		this.setUri(staticLink.getUri());
		this.setExternal(staticLink.getExternal());
	}
	
	/**
	 * Links do not have children
	 */
	public Object getChild(Object parent, int index) {
		return ((TreeMenuItem)parent).getChild(index);
	}
	
	/**
	 * Links do not have children
	 */
	public int getChildCount(Object parent) {
		return ((TreeMenuItem)parent).getChildCount();
	}
	
	/**
	 * Links are always leaf nodes
	 */
	public boolean isLeaf(Object parent) {
		return ((TreeMenuItem) parent).isLeaf();
	}
	public String dump(String indent) {
		return 
			"LK" + indent + dump() + " (parent=" +
			((parent == null) ? "null": parent.getId())  + ")\n";
	}
	public List<TreeMenuItem> getChildren() {
		return null;
	}
	public void setParent(TreeMenuItem parent) {
		this.parent = parent;
	}
	public TreeMenuItem getParent() {
		return parent;
	}
	
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}
	
	public Object getChild(int index) {
		return null;
	}
	public int getChildCount() {
		return 0;
	}
	public boolean isLeaf() {
		return true;
	}
	
}
