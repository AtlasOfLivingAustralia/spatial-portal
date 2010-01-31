package au.org.emii.portal;

import java.io.Serializable;

/**
 * Sometimes we need to be able to describe an item in a tree in
 * terms of it's parent and index as a child (eg to do menu
 * updates through zk's fireevent system).
 * 
 * @author geoff
 *
 */
public class TreeChildIdentifier implements Serializable {

	private static final long serialVersionUID = 1L;
	private int index = 0;
	private TreeMenuItem treeMenuItem = null;
	
	@SuppressWarnings("unused")
	private TreeChildIdentifier() {}
	
	/**
	 * 
	 * @param treeMenuItem
	 * @param index
	 */
	public TreeChildIdentifier(TreeMenuItem treeMenuItem, int index) {
		this.treeMenuItem = treeMenuItem;
		this.index = index;
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public TreeMenuItem getTreeMenuItem() {
		return treeMenuItem;
	}

	public void setTreeMenuItem(TreeMenuItem treeMenuItem) {
		this.treeMenuItem = treeMenuItem;
	}
}
