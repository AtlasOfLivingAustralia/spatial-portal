/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal.value;

/**
 * @author geoff
 */
public interface AbstractIdentifier {

    /**
     * Return a human readable dump of this object.  Skips the description
     * because it could be long and mess up the output
     *
     * @return
     */
    public String dump();

    public String getDescription();

    public String getId();

    public String getName();

    public void setDescription(String description);

    public void setId(String id);

    public void setName(String name);

}
