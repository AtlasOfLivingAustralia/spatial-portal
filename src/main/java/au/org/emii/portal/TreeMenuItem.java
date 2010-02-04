package au.org.emii.portal;

import java.util.List;

public interface TreeMenuItem extends TreeMenuValue {
	public static final int CONCRETE_TYPE_UNKNOWN = 0;
	public static final int CONCRETE_TYPE_MENUGROUP = 1;
	public static final int CONCRETE_TYPE_MENUITEM = 2;

	public TreeMenuItem getParent();
	public void setParent(TreeMenuItem treeMenuItem); 
	public List<? extends TreeMenuItem> getChildren();
	public int getConcreteType();
	public Object clone() throws CloneNotSupportedException;
}
