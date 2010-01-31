package au.org.emii.portal;

import java.io.Serializable;

public abstract class AbstractIdentifier implements Cloneable, Serializable {

	private static final long serialVersionUID = 1L;
	private String id = null;
	private String name = null;
	private String description = null;

	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	
	/**
	 * Return a human readable dump of this object.  Skips the description
	 * because it could be long and mess up the output
	 * @return
	 */
	public String dump() {
		String dump = 
			"id=>'" + id + "', name=>'" + name + "' ";
		return dump;
	}
	
}
