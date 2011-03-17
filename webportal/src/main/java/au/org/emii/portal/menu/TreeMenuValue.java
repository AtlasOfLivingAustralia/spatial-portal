package au.org.emii.portal.menu;

import java.util.List;

public interface TreeMenuValue {
	public String getId();
	public String getName();
	public String getDescription();
	
	public Object getChild(Object parent, int index);
	public Object getChild(int index);
	public boolean isLeaf(Object parent);
	public boolean isLeaf();
	public int getChildCount(Object parent);
	public int getChildCount();
	public String dump(String indent);
	public List<? extends TreeMenuValue> getChildren();

}
