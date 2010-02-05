package au.org.emii.portal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlCursor;

public class MenuGroup extends AbstractIdentifier implements TreeMenuItem, Cloneable, Serializable {

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

    public void copyFrom(au.org.emii.portal.config.MenuGroup configData,
            PortalSession portalSession) {

        this.setId(configData.getId());
        this.setName(configData.getName());
        this.setDescription(configData.getDescription());

        /* iterate over the child elements.  This has to be done with an
         * xml cursor to preserve ordering.  The xmlbeans API only
         * supports getting three separate lists.
         */
        XmlCursor cursor = configData.getChildren().newCursor();
        boolean finished = false;
        cursor.toFirstChild();

        while (!finished) {
            /* cursor contains a menuGroup, dataSourceIdRef or staticLinkIdRef
             * element.
             */
            TreeMenuItem node = getNodeAtCursor(cursor, portalSession);
            if (node != null) {
                addChild(node);
            }
            finished = !cursor.toNextSibling();
        }
        cursor.dispose();
    }

    private TreeMenuItem getNodeAtCursor(XmlCursor cursor, PortalSession portalSession) {
        TreeMenuItem node = null;
        String type = cursor.getName().getLocalPart();
        if (type.equals("menuGroup")) {
            au.org.emii.portal.config.MenuGroup item = (au.org.emii.portal.config.MenuGroup) cursor.getObject().changeType(au.org.emii.portal.config.MenuGroup.type);
            node = new MenuGroup();
            ((MenuGroup) node).copyFrom(item, portalSession);
        } else if (type.equals("dataSourceIdRef")) {
            String id = cursor.getTextValue();

            /* child is a DataSource (MapLayer after parsing the config file)
             * identified by id
             */
            MapLayer mapLayer = portalSession.getMapLayerById(id);
            if (mapLayer != null) {
                node = new MenuItem(mapLayer);
            } else {
                logger.warn("dataSourceIdRef: '" + id + "' not found in portal session so not adding to menu");
            }

        } else if (type.equals("staticLinkIdRef")) {
            String id = cursor.getTextValue();

            /* child is a StaticLink (Link after parsing the config file)
             * identified by id
             */
            Link link = portalSession.getLinkById(id);

            if (link != null) {
                node = new MenuItem(link);
            }
            else {
                logger.warn("staticLinkIdRef: '" + id + "' not found in portal session so not adding to menu");
            }


        } else {
            logger.warn("unable to process unsupported menu child: " + type);
        }

        return node;
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
