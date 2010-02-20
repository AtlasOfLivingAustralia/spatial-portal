package au.org.emii.portal.menu;

import au.org.emii.portal.AbstractIdentifierImpl;
import au.org.emii.portal.Link;
import au.org.emii.portal.MapLayer;
import au.org.emii.portal.PortalSession;
import au.org.emii.portal.TreeMenuItem;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlCursor;

public class MenuGroup extends AbstractIdentifierImpl implements TreeMenuItem, Cloneable, Serializable {

    private static final long serialVersionUID = 1L;
    private List<TreeMenuItem> children = new ArrayList<TreeMenuItem>();
    private TreeMenuItem parent = null;
    private static final int CONCRETE_TYPE = TreeMenuItem.CONCRETE_TYPE_MENUGROUP;

    // static avoids breaking serialisation
    private static Logger logger = Logger.getLogger(MenuGroup.class.getName());

    public List<TreeMenuItem> getChildren() {
        return children;
    }

    public void addChild(TreeMenuItem child) {
        child.setParent(this);
        children.add(child);
    }




    @Override
    public String dump(String indent) {
        StringBuffer dump = new StringBuffer(
                "MG" + indent + dump() + " (parent=" +
                ((parent == null) ? "NULL_PARENT!" : parent.getId()) +
                ")\n");
        indent += "  ";
        for (TreeMenuItem child : children) {
            if (child != null) {
                dump.append(child.dump(indent));
            } else {
                dump.append(indent + "null (SKIPPED)");
            }
        }
        return dump.toString();
    }

    public boolean isLeaf() {
        return !(children.size() > 0);
    }

    /**
     * Return the number of direct descendants
     * @return
     */
    public int getChildCount() {
        return children.size();
    }

    public TreeMenuItem getChild(int index) {
        return children.get(index);
    }

    public Object getChild(Object parent, int index) {
        return ((TreeMenuItem) parent).getChild(index);
    }

    public boolean isLeaf(Object parent) {
        return ((TreeMenuItem) parent).isLeaf();
    }

    public int getChildCount(Object parent) {
        return ((TreeMenuItem) parent).getChildCount();
    }

    public TreeMenuItem getParent() {
        return parent;
    }

    public void setParent(TreeMenuItem parent) {
        this.parent = parent;
    }

    public int getConcreteType() {
        return CONCRETE_TYPE;
    }

    public Object clone() throws CloneNotSupportedException {
        MenuGroup menuGroup = (MenuGroup) super.clone();
        menuGroup.parent = null;
        menuGroup.children = new ArrayList<TreeMenuItem>();
        if (children != null) {
            for (TreeMenuItem child : children) {

                // eclipse puts red pen here (on child.clone() but
                // its supported and will compile so you can ignore it
                TreeMenuItem childClone = (TreeMenuItem) child.clone();
                menuGroup.addChild((TreeMenuItem) childClone);

                // 'fix' the parent :)
                childClone.setParent(menuGroup);
            }
        }


        return menuGroup;
    }
}
