package org.ala.spatial.util;

import org.w3c.dom.*;
import javax.xml.parsers.*;

/**
 * Provides two access methods to an xml:
 * 
 * example:
 * <code>
 * <unique_tag_name>value</unique_tag_name>
 * <tagname_level_1>
 * 		<tagname_level_2>
 * 			<tagname_value1>value1</tagname_value1>
 * 			<tagname_value2>value2</tagname_value2>
 * 		</tagname_level_2>
 * 		<tagname_level_2>
 * 			<tagname_value1>2nd value 1</tagname_value1>
 * 			<tagname_value1>2nd value 2</tagname_value1>
 * 		</tagname_level_2>
 * </tagname_level_1>
 * </code>
 * 
 * 1.&nbsp;get inner contents of a tag; retrieve <code>value</code> from within <code>unique_tag_name</code>
 * 2.&nbsp;iterate through nested tags; retrieve <code>value1</code> to <code>2nd value 2</code> from within <code>tagname_level_1</code>
 * 
 * @author Adam Collins
 */
public class XmlReader {
	
	/**
	 * parsed xml file
	 */
	private Document document;
	
	/**
	 * errors returned as log entries only
	 * @param filename filename of file to load as String
	 */
	public XmlReader(String filename) {
		SpatialLogger sl = new SpatialLogger();
		document = null;
		try {			
			document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(filename);
		} catch (Exception e) {
			sl.log("XmlReader",e.toString());			
		}
		if (document == null) {
			sl.log("XmlReader","failed to read " + filename);
		}
	}

	/**
	 *
	 * @param tagname tagname as String whose inner text is to be 
	 * 	returned
	 * @return inner text of first instance of the specified tagname
	 * null if not found
	 */
	public String getValue(String tagname) {	
		if (document == null) {
			return null;
		}	
		NodeList nl = document.getElementsByTagName(tagname);
		if (nl != null && nl.getLength() > 0) {
			Node n = nl.item(0);
			if (n != null) {
				return n.getTextContent();
			}
		}
		return null;
	}

	/**
	 * Operates by locating first tagname and iterating through 
	 * subsequent branches.
	 * <code>tagname / [idx] / sub tag / [idx] / sub tag / [idx]</code>
	 * Corresponding arrays of String inputs (tag names) and int 
	 * inputs.  
	 * 
	 * For example:
	 *	<code>{"geo_tables","geo_table"}</code> and <code>{0,0}</code> 
	 *	returns empty String if exists, null if not.
	 *	<code>{"geo_tables","geo_table","geo_table_name"}</code> and <code>{0,1,0}</code> 
	 *	returns value of first geo_table_name on second geo_table 
	 *  or null.
	 * @param reference array of String corresponding to the nesting 
	 *  of tag names
	 * @param idx array of int corresponding to the n'th tag names 
	 *  found
	 * @param pairs_count the number of 
	 * @return value as string
	 * null if not found, document not loaded, reference or indices does not 
	 * contain <code>pairs_count</code> records
	 */
	public String getValue(String [] reference, int [] idx, int pairs_count) {
		SpatialLogger sl = new SpatialLogger();
		Node node;
		int [] indices;
		
		if (document == null 
				|| reference == null || pairs_count > reference.length
				|| idx == null || pairs_count > idx.length
				|| pairs_count < 1) {
			sl.log("XmlReader","invalid getValue request");
			return null;
		}
		
		indices = idx.clone();

		//begin at the top and work down
		NodeList nodelist = document.getElementsByTagName(reference[0]);
		if (nodelist == null || nodelist.getLength() == 0) {
			return null;
		}
		
		//get top level tag
		if (nodelist.getLength() < indices[0]) {
			return null;
		} else {
			node = nodelist.item(indices[0]);
		}		

		//iterate through the rest
		int k,j,i = 0;
		for (i=1; i<pairs_count; i++) {
			//confirm valid steps
			nodelist = node.getChildNodes();
			k = -1;
			for (j=0; j<nodelist.getLength() && indices[i] >= 0; j++) {
				if (nodelist.item(j).getNodeName() == reference[i])	{
					indices[i]--;
					k = j; 
				}
			}
			if (indices[i] >= 0 || k == -1) {
				//did not find the required number of nodes by this tagname
				return null;
			} else {
				//step in	
				node = nodelist.item(k);
			}				
		}

		if (node != null) {
			return node.getTextContent();
		} else {
			return null;
		}		
	}
}
