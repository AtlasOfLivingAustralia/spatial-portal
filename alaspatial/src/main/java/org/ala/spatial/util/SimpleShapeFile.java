package org.ala.spatial.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.TreeSet;
import java.io.RandomAccessFile;
import java.util.Calendar;
import java.io.FileWriter;

public class SimpleShapeFile extends Object implements Serializable{
	private static final long serialVersionUID = 1L;
	
	ShapeHeader shapeheader;
	ShapeRecords shaperecords;
	ArrayList<ComplexRegion> regions;

	DBF dbf;
	
	public SimpleShapeFile(String fileprefix){

		dbf = new DBF(fileprefix + ".dbf");

		shapeheader = new ShapeHeader(fileprefix);
		shaperecords = new ShapeRecords(fileprefix,shapeheader.getShapeType());

		regions = shaperecords.getRegions();		
	}
	
	public String [] listColumns(){
		return dbf.getColumnNames();
	}
	
	public String [] getColumnLookup(int column){
		return dbf.getColumnLookup(column);
	}
	
	public int getColumnIdx(String column_name){
		return dbf.getColumnIdx(column_name);
	}
	
	public int [] intersect(double [][] points, String [] lookup, int column){
		long start_time = Calendar.getInstance().getTimeInMillis();
		
		int i,j,v;
		String s;
		
		int [] output = new int[points.length];
	
		for(i=0;i<points.length;i++){			
			for(j=0;j<regions.size();j++){
				if(regions.get(j).isWithin(points[i][0],points[i][1])){
					s = dbf.getValue(j,column);
					v = java.util.Arrays.binarySearch(lookup,s);
					if(v < 0){
							v = -1;
					}
					output[i] = v;
					break;
				}
			}
			if(j == regions.size()){
				output[i] = -1;
			}
		}
		
		long end_time = Calendar.getInstance().getTimeInMillis();

		//System.out.println("intersect time in milliseconds: " + ( end_time - start_time ));
		
		return output;
	}

	public String getHeaderString(){
		return shapeheader.toString();
	}	
	
	public short [][] getShortMask(int column,double longitude1, double latitude1
			, double longitude2, double latitude2, int width, int height){
		int i,j,k,v;
		short [][] mask = new short[height][width];
		for(j=0;j<height;j++){
			for(k=0;k<width;k++){
				mask[j][k] = -1;
			}
		}
		
		String [] lookup = getColumnLookup(column);
		
		
		String s;
		byte [][] map;
		for(i=0;i<regions.size();i++){
			map = new byte[height][width];
			regions.get(i).getOverlapGridCells(longitude1, latitude1, longitude2, latitude2, width, height, map);
			
			s = dbf.getValue(i,column);
			v = java.util.Arrays.binarySearch(lookup,s);
			
			/* merge on first in basis for partial cells */
			int countnone = 0;
			int countsome = 0;
			
			
			for(j=0;j<map.length;j++){
				for(k=0;k<map[j].length;k++){
					if(map[j][k] >0 ){ // should be only == 1 || map[j][k] == 2){						
						mask[j][k] = (short)(v);
						countsome++;
					}else{						
						countnone++;						
					}
				}
			}
			System.out.println("obj:" + v + " " + s + " none:" + countnone + " some:" + countsome);
		}		
		return mask;		
	}
}

class ShapeHeader extends Object implements Serializable{
	private static final long serialVersionUID = 1L;
	
	int filecode;
	int filelength;
	int version;
	int shapetype;
	double [] boundingbox;

	boolean isvalid;

	public ShapeHeader(String fileprefix){		
		try{
			FileInputStream fis = new FileInputStream(fileprefix + ".shp");

		        FileChannel fc = fis.getChannel();

        		ByteBuffer buffer = ByteBuffer.allocate(1024);
			
			fc.read(buffer);
			buffer.flip();

			buffer.order(ByteOrder.BIG_ENDIAN);
			filecode = buffer.getInt();
			buffer.getInt();
			buffer.getInt();
			buffer.getInt();
			buffer.getInt();
			buffer.getInt();
			filelength = buffer.getInt();
			
			buffer.order(ByteOrder.LITTLE_ENDIAN);
			version = buffer.getInt();			
			shapetype = buffer.getInt();
			boundingbox = new double[8];
			for(int i = 0; i < 8; i++){
				boundingbox[i] = buffer.getDouble();
			}

			fis.close();

			isvalid = true;
		}catch(Exception e){
			System.out.println("loading header error: " + fileprefix + ": " + e.toString());
			e.printStackTrace();
		}
	}

	public int getShapeType(){
		return shapetype;
	}

	@Override
	public String toString(){
		StringBuffer sb = new StringBuffer();

		sb.append("\r\nFile Code: \r\n");
		sb.append(String.valueOf(filecode));

		sb.append("\r\nFile Length: \r\n");
		sb.append(String.valueOf(filelength));

		sb.append("\r\nVersion: \r\n");
		sb.append(String.valueOf(version));

		sb.append("\r\nShape Type: \r\n");
		sb.append(String.valueOf(shapetype));
		
		int i = 0;

		sb.append("\r\nXmin: \r\n");
		sb.append(String.valueOf(boundingbox[i++]));

		sb.append("\r\nYmin: \r\n");
		sb.append(String.valueOf(boundingbox[i++]));

		sb.append("\r\nXmax: \r\n");
		sb.append(String.valueOf(boundingbox[i++]));


		sb.append("\r\nYmax: \r\n");
		sb.append(String.valueOf(boundingbox[i++]));


		sb.append("\r\nZmin: \r\n");
		sb.append(String.valueOf(boundingbox[i++]));


		sb.append("\r\nZmax: \r\n");
		sb.append(String.valueOf(boundingbox[i++]));


		sb.append("\r\nMmin: \r\n");
		sb.append(String.valueOf(boundingbox[i++]));

		sb.append("\r\nMmax: \r\n");
		sb.append(String.valueOf(boundingbox[i++]));

		return sb.toString();
	}

	public boolean isValid(){
		return isvalid;
	}
}

class ShapeRecords extends Object implements Serializable{
	private static final long serialVersionUID = 1L;
	
	ArrayList<ShapeRecord> records;
	boolean isvalid;

	public ShapeRecords(String fileprefix, int shapetype){
		isvalid = false;
		try{
			FileInputStream fis = new FileInputStream(fileprefix + ".shp");

		        FileChannel fc = fis.getChannel();

        		ByteBuffer buffer = ByteBuffer.allocate((int)fc.size()-100);
			
			fc.read(buffer,100);				//records header starts at 100
			buffer.flip();
			buffer.order(ByteOrder.BIG_ENDIAN);

			records = new ArrayList();

			while(buffer.hasRemaining()){
				records.add(new ShapeRecord(buffer, shapetype));
			}

			fis.close();

			isvalid = true;
		}catch(Exception e){
			System.out.println("loading shape records error: " + fileprefix + ": " + e.toString());
			e.printStackTrace();
		}
	}

	public boolean isValid(){
		return isvalid;
	}

	public ArrayList<ComplexRegion> getRegions(){

		ArrayList<ComplexRegion> sra = new ArrayList();		

		for(int i=0;i<records.size();i++){
			ShapeRecord shr = records.get(i);
			ComplexRegion sr = new ComplexRegion();
			int points_count = 0;
			for(int j=0;j<shr.getNumberOfParts();j++){
				sr.addPolygon(shr.getPoints(j));
				points_count += shr.getPoints(j).length;			
			}
			if(points_count > 40){		//TODO: don't use arbitary limit
				sr.useMask(100,50);
			}
				
			sra.add(sr);
			System.out.print("*");
		}		

		return sra;		
	}
}

class ShapeRecord extends Object implements Serializable{
	private static final long serialVersionUID = 1L;
	
	int recordnumber;
	int contentlength;
	Shape shape;
	
	public ShapeRecord(ByteBuffer bb, int shapetype){
		bb.order(ByteOrder.BIG_ENDIAN);
		recordnumber = bb.getInt();
		contentlength = bb.getInt();

		switch(shapetype){
			case 5:
				shape = new Polygon(bb);
				break;
			default:
				System.out.println("unknown shape type: " + shapetype);
		}
	}

	@Override
	public String toString(){
		if(shape != null){
			return "Record Number: " + recordnumber + ", Content Length: " + contentlength 
				+ shape.toString();
		}

		return "Record Number: " + recordnumber + ", Content Length: " + contentlength;
	}

	public double [][] getPoints(int part){
		return shape.getPoints(part);
	}

	public int getNumberOfParts(){
		return shape.getNumberOfParts();
	}
}

class Shape extends Object implements Serializable{
	private static final long serialVersionUID = 1L;
	
	public Shape(){}
	public double [][] getPoints(int part){return null;}
	public int getNumberOfParts(){return 0;}
}

class Polygon extends Shape{
	int shapetype;
	double [] boundingbox;
	int numparts;
	int numpoints;
	int [] parts;
	double [] points;
	
	public Polygon(ByteBuffer bb){
		int i;

		bb.order(ByteOrder.LITTLE_ENDIAN);
		shapetype = bb.getInt();

		boundingbox = new double[4];
		for(i=0;i<4;i++){
			boundingbox[i] = bb.getDouble();
		}
		
		numparts = bb.getInt();

		numpoints = bb.getInt();
	
		parts = new int[numparts];
		for(i=0;i<numparts;i++){
			parts[i] = bb.getInt();
		}

		points = new double[numpoints*2];			//x,y
		for(i=0;i<numpoints*2;i++){
			points[i] = bb.getDouble();
		}		
	}

	@Override
	public String toString(){
		StringBuffer sb = new StringBuffer();

		sb.append("(");
		sb.append(String.valueOf(boundingbox[0]));	
		sb.append(", ");	
		sb.append(String.valueOf(boundingbox[1]));	

		sb.append(") (");
		sb.append(String.valueOf(boundingbox[2]));	
		sb.append(", ");	
		sb.append(String.valueOf(boundingbox[3]));

		sb.append(") parts=");
		sb.append(String.valueOf(numparts));	
		sb.append(" points=");	
		sb.append(String.valueOf(numpoints));	
	
		return sb.toString();
	}

	@Override
	public int getNumberOfParts(){
		return numparts;
	}

	@Override
	public double [][] getPoints(int part){
		double [][] output;
		int start = parts[part];
//System.out.println("part=" + part + " start=" + start);
		int end = numpoints;
		if(part < numparts-1){
			end = parts[part+1];
		}
		output = new double[end-start][2];
		for(int i=start;i<end;i++){
			output[i-start][0] = points[i*2];
			output[i-start][1] = points[i*2+1];
		}
		return output;
	}
}

class DBF extends Object implements Serializable{
	private static final long serialVersionUID = 1L;
	String filename;
	DBFHeader dbfheader;
	DBFRecords dbfrecords;

	public DBF(String filename){
		dbfheader = new DBFHeader(filename);
		System.out.println(dbfheader);

		dbfrecords = new DBFRecords(filename, dbfheader);
		//System.out.println(dbfrecords);
	}

	public int getColumnIdx(String column_name){
		return dbfheader.getColumnIdx(column_name);
	}

	public String [] getColumnNames(){
		return dbfheader.getColumnNames();
	}

	public String getValue(int row, int column){
		return dbfrecords.getValue(row,column);
	}	
	
	public String [] getColumnLookup(int column){
		TreeSet<String> ts = new TreeSet<String>();
		
		int i,len;
		len = dbfheader.getNumberOfRecords();
		for(i=0;i<len;i++){
			ts.add(getValue(i,column));
		}
		
		String [] sa = new String[ts.size()];
		ts.toArray(sa);
		
		return sa;
	}
}

class DBFHeader extends Object implements Serializable{
	int filetype;
	int [] lastupdate;
	int numberofrecords;
	int recordsoffset;
	int recordlength;
	int tableflags;
	/* int codepagemark; */
	ArrayList<DBFField> fields;
	boolean isvalid;

	public DBFHeader(String filename){
		isvalid = false;
		try{
			int i;

			FileInputStream fis = new FileInputStream(filename);

		        FileChannel fc = fis.getChannel();

        		ByteBuffer buffer = ByteBuffer.allocate((int)fc.size());
			
			fc.read(buffer);				//records header starts at 100
			buffer.flip();	
			buffer.order(ByteOrder.BIG_ENDIAN);

			filetype = (0xFF & buffer.get());
			lastupdate = new int[3];
			for(i=0;i<3;i++){
				lastupdate[i] = (0xFF & buffer.get());
			}
			numberofrecords = (0xFF & buffer.get()) + 256*((0xFF & buffer.get()) 
				+ 256*((0xFF & buffer.get()) + 256*(0xFF & buffer.get())));
			recordsoffset = (0xFF & buffer.get()) + 256*(0xFF & buffer.get());
			recordlength = (0xFF & buffer.get()) + 256*(0xFF & buffer.get());
			for(i=0;i<16;i++){
				buffer.get();
			}
			tableflags = (0xFF & buffer.get());
			/* codepagemark = */ buffer.get();
			buffer.get(); 
			buffer.get();
				
			fields = new ArrayList();
			byte nextfsr;
			while((nextfsr = buffer.get()) != 0x0D){				
				fields.add(new DBFField(nextfsr, buffer));
			}
			/* don't care dbc */

			fis.close();

			isvalid = true;
		}catch(Exception e){
			System.out.println("loading dbfheader error: " + filename + ": " + e.toString());
			e.printStackTrace();
		}
	}

	public ArrayList<DBFField> getFields(){
		return fields;
	}

	public int getRecordsOffset(){
		return recordsoffset;
	}

	public int getNumberOfRecords(){
		return numberofrecords;
	}

	public String [] getColumnNames(){
		String [] s = new String[fields.size()];
		for(int i=0;i<s.length;i++){
			s[i] = fields.get(i).getName();
		}
		return s;
	}

	public int getColumnIdx(String column_name){
		column_name = column_name.toUpperCase();
		for(int i=0;i<fields.size();i++){
//System.out.println(fields.get(i).getName() + " = " + column_name);			
			if(fields.get(i).getName().equals(column_name)){
System.out.println("field [" + column_name + " found in column: " + i);
				return i;
			}
		}
		System.out.println("field [" + column_name + "] not found");
		return -1;
	}

	@Override
	public String toString(){
		if(!isvalid){
			return "invalid header";
		}
		
		StringBuffer sb = new StringBuffer();
		
		sb.append("filetype: ");
		sb.append(String.valueOf(filetype));
		sb.append("\r\nlastupdate: ");
		sb.append(String.valueOf(lastupdate[0]));
		sb.append(" ");
		sb.append(String.valueOf(lastupdate[1]));
		sb.append(" ");
		sb.append(String.valueOf(lastupdate[2]));
		sb.append("\r\nnumberofrecords: ");
		sb.append(String.valueOf(numberofrecords));
		sb.append("\r\nrecordsoffset: ");
		sb.append(String.valueOf(recordsoffset));
		sb.append("\r\nrecordlength: ");
		sb.append(String.valueOf(recordlength));
		sb.append("\r\ntableflags: ");
		sb.append(String.valueOf(tableflags));
		sb.append("\r\nnumber of fields: ");
		sb.append(String.valueOf(fields.size()));

		for(int i=0;i<fields.size();i++){
			sb.append(fields.get(i).toString());
		}

		return sb.toString();
	}		
}

class DBFField extends Object implements Serializable{
	private static final long serialVersionUID = 1L;
	
	String name;
	char type;
	int displacement;
	int length;
	int decimals;
	int flags;
	byte [] data;		//placeholder for reading byte blocks
	/* don't care autoinc */

	public DBFField(byte firstbyte, ByteBuffer buffer){
		int i;
		byte [] ba = new byte[12];
		ba[0] = firstbyte;
		for(i=1;i<11;i++){
			ba[i] = buffer.get();
		}
		try{
			name = (new String(ba,"US-ASCII")).trim().toUpperCase();
		}catch(Exception e){
			System.out.println(e.toString());
			e.printStackTrace();
		}

		byte [] ba2 = new byte[1];
		ba2[0] = buffer.get();
		try{		
			type = (new String(ba2,"US-ASCII")).charAt(0);
		}catch(Exception e){
			System.out.println(e.toString());
			e.printStackTrace();
		}

		displacement = (0xFF & buffer.get()) + 256*((0xFF & buffer.get()) 
				+ 256*((0xFF & buffer.get()) + 256*(0xFF & buffer.get())));

		length = (0xFF & buffer.get());
		data = new byte[length];

		decimals = (0xFF & buffer.get());
		flags = (0xFF & buffer.get());	

		for(i=0;i<13;i++){
			buffer.get();
		}		
	}

	public String getName(){
		return name;
	}
	
	public char getType(){
		return type;
	}

	public byte [] getDataBlock(){
		return data;
	}	

	@Override
	public String toString(){
		return "name: " + name + " type: " + type + " displacement: " + displacement + " length: " + length + "\r\n";
	}
}

class DBFRecords extends Object implements Serializable{
	private static final long serialVersionUID = 1L;
	
	ArrayList<DBFRecord> records;
	boolean isvalid;

	public DBFRecords(String filename, DBFHeader header){
		isvalid = false;
		try{
			FileInputStream fis = new FileInputStream(filename);

		        FileChannel fc = fis.getChannel();

        		ByteBuffer buffer = ByteBuffer.allocate((int)fc.size()-header.getRecordsOffset());
			
			fc.read(buffer,header.getRecordsOffset());
			buffer.flip();	

			records = new ArrayList();
	
			int i = 0;
			ArrayList<DBFField> fields = header.getFields();
			while(i < header.getNumberOfRecords() && buffer.hasRemaining()){
				records.add(new DBFRecord(buffer, fields));
				i++;
			}

			fis.close();

			isvalid = true;
		}catch(Exception e){
			System.out.println("loading records error: " + filename + ": " + e.toString());
			e.printStackTrace();
		}
	}

	public String getValue(int row, int column){
		if(row >= 0 && row < records.size()){
			return records.get(row).getValue(column);
		}
		return "";			
	}

	@Override
	public String toString(){
		StringBuffer sb = new StringBuffer();
		for(DBFRecord r : records){
			sb.append(r.toString());
			sb.append("\r\n");
		}
		return sb.toString();
	}
}

class DBFRecord extends Object implements Serializable{
	private static final long serialVersionUID = 1L;
	String [] record;	
	int deletionflag;

	public DBFRecord(ByteBuffer buffer, ArrayList<DBFField> fields){
		deletionflag = (0xFF & buffer.get());
		record = new String[fields.size()];
		for(int i=0;i<record.length;i++){
			DBFField f = fields.get(i);
			byte [] data = f.getDataBlock();
			buffer.get(data);
			try{
				switch(f.getType()){
					case 'C':			//string
						record[i] = (new String(data,"US-ASCII")).trim();
						break;
					case 'N':			//number as string
						record[i] = (new String(data,"US-ASCII")).trim();
						break;
				}
			}catch(Exception e){
			}
		}			
	}

	public String getValue(int column){
		if(column < 0 || column >= record.length){
			return "";
		}else{
			return record[column];
		}
	}

	@Override
	public String toString(){
		StringBuffer sb = new StringBuffer();
		for(String s : record){
			sb.append(s);
			sb.append(", ");
		}
		return sb.toString();
	}
}

class IntersectionThread implements Runnable {
	Thread t;
	ArrayList<ComplexRegion> regions;
	int startpoint;
	int currentpoint;
	int endpoint;
	ArrayList<ComplexRegion> regions_;
	double [][] points;
	
	public IntersectionThread (ArrayList<ComplexRegion> regions_, double [][] points_, int startpoint_, int endpoint_) {
		t = new Thread (this);

		points = points_;
		regions = regions_;
		startpoint = startpoint_;
		endpoint = endpoint_;
		currentpoint = startpoint_;

		t.start();
	}

	public void run() {
		System.out.println("start intersecting: " + startpoint + " to " + endpoint);

		int i,j;

		for(i=currentpoint;i<endpoint;i++){
			for(j=0;j<regions.size();j++){
				if(regions.get(j).isWithin(points[i][0],points[i][1])){
					//write something
					break;
				}				
			}
			if(j == regions.size()){
				//write null
			}
		}
		
	
		System.out.println("end intersecting: " + startpoint + " to " + endpoint);
	}

	public boolean isAlive(){
		return t.isAlive();
	}
	
}
 