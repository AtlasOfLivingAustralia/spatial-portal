package org.ala.spatial.analysis.tabulation;

import java.io.Serializable;

public class IndexedRecord implements Serializable{
	static final long serialVersionUID = -3902094650522883528L;

	public IndexedRecord(String _name, int _file_start, int _file_end, 
			int _record_start, int _record_end, byte _type){
		name = _name;
		file_start = _file_start;
		file_end = _file_end;
		record_start = _record_start;
		record_end = _record_end;
		type = _type;
	}
	public String name;
	public int file_start;
	public int file_end;
	public int record_start;
	public int record_end;
	public byte type;	
}