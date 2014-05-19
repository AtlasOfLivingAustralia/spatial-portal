package au.org.emii.portal.value;

import java.io.Serializable;

public abstract class AbstractIdentifierImpl implements Cloneable, Serializable, AbstractIdentifier {

    private static final long serialVersionUID = 1L;
    private String id = null;
    private String name = null;
    private String description = null;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Return a human readable dump of this object.  Skips the description
     * because it could be long and mess up the output
     *
     * @return
     */
    @Override
    public String dump() {
        return "id=>'" + id + "', name=>'" + name + "' ";
    }

}
