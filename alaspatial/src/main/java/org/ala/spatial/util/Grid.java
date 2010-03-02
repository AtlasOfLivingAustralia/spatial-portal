package org.ala.spatial.util;

import java.io.File;
import java.io.RandomAccessFile;

/**
 * Grid.java
 * Created on June 24, 2005, 4:12 PM
 * 
 * @author Robert Hijmans, rhijmans@berkeley.edu
 * 
 * Updated 15/2/2010, Adam
 * 
 * Interface for .gri/.grd files for now
*/
public class Grid {

  public Boolean byteorderLSB; // true if file is LSB (Intel)
  public int ncols, nrows;
  public double nodatavalue;
  public Boolean valid;
  public double[] values;
  public double xmin, xmax, ymin, ymax;
  public double xres, yres;
  String datatype;
  
  // properties
  double minval, maxval;
  byte nbytes;
  
  String filename;
  
 /**
  * loads grd for gri file reference
  * @param fname full path and file name without file extension
  * of .gri and .grd files to open
  */
  public Grid(String fname) { // construct Grid from file
	  filename = fname;
	  File grifile = new File(filename + ".gri");      
	  File grdfile = new File(filename + ".grd");
      if (grdfile.exists() && grifile.exists()) {
        readgrd(filename);       
      } else {
    	  //log error
      }
  }

//transform to file position
  private int getcellnumber(double x, double y) {
	  if (x < xmin || x > xmax || y < ymin || y > ymax) //handle invalid inputs
		  return -1;
	  
	  int col = (int)((x - xmin) / xres);
	  int row = (int)((y - ymin) / yres);
	  
	  //limit each to 0 and ncols-1/nrows-1
	  if(col < 0) col = 0;
	  if(row < 0) row = 0;
	  if(col >= ncols) col = ncols-1;
	  if(row >= nrows) row = nrows-1;
	  return (row * ncols + col);
  }


  private void setdatatype(String s) {
    s = s.toUpperCase();

    // Expected from grd file
    if (s.equals("INT1BYTE")) {
      datatype = "BYTE";
    } else if (s.equals("INT2BYTES")) {
      datatype = "SHORT";
    } else if (s.equals("INT4BYTES")) {
      datatype = "INT";
    } else if (s.equals("INT8BYTES")) {
      datatype = "LONG";
    } else if (s.equals("FLT4BYTES")) {
      datatype = "FLOAT";
    } else if (s.equals("FLT8BYTES")) {
      datatype = "DOUBLE";
    }

    // shorthand for same
    else if (s.equals("INT1B")) {
      datatype = "BYTE";
    } else if (s.equals("INT2B")) {
      datatype = "SHORT";
    } else if (s.equals("INT4B")) {
      datatype = "INT";
    } else if (s.equals("INT8B")) {
      datatype = "LONG";
    } else if (s.equals("FLT4B")) {
      datatype = "FLOAT";
    } else if (s.equals("FLT8B")) {
      datatype = "DOUBLE";
    }

    // if you rather use Java keywords...
    else if (s.equals("BYTE")) {
      datatype = "BYTE";
    } else if (s.equals("SHORT")) {
      datatype = "SHORT";
    } else if (s.equals("INT")) {
      datatype = "INT";
    } else if (s.equals("LONG")) {
      datatype = "LONG";
    } else if (s.equals("FLOAT")) {
      datatype = "FLOAT";
    } else if (s.equals("DOUBLE")) {
      datatype = "DOUBLE";
    }

    // some backwards compatibility
    else if (s.equals("INTEGER")) {
      datatype = "INT";
    } else if (s.equals("SMALLINT")) {
      datatype = "INT";
    } else if (s.equals("SINGLE")) {
      datatype = "FLOAT";
    } else if (s.equals("REAL")) {
      datatype = "FLOAT";
    } else {
      datatype = "UNKNOWN";
    }

    if (datatype.equals("BYTE")) {
      nbytes = 1;
    } else if (datatype.equals("SHORT")) {
      nbytes = 2;
    } else if (datatype.equals("INT")) {
      nbytes = 4;
    } else if (datatype.equals("LONG")) {
      nbytes = 8;
    } else if (datatype.equals("SINGLE")) {
      nbytes = 4;
    } else if (datatype.equals("DOUBLE")) {
      nbytes = 8;
    } else {
      nbytes = 0;
    }
  }
 
  // The follow Little Endian stuff is based on code by
  // Roedy Green : copyright 1996-2005 Canadian Mind Products
  // http://mindprod.com/jgloss/endian.html
  
  /**
   * 
   * @param points input array for longitude and latitude 
   *  		double[number_of_points][2]
   * @return array of .gri file values corresponding to the 
   * 		points provided
   */
  public double[] getValues(double [][] points){	  

	  //confirm inputs since they come from somewhere else
	  if(points == null || points.length == 0){
		  //System.out.println("err with points");
		  //log error
		  return null;
	  }	 
	  
	  double [] ret = new double[points.length];
	  
	  //System.out.println("expecting " + points.length + " env values with datatype " + datatype);
	  
	  int length = points.length;
	  int size,i,j,pos;
	  byte [] b = new byte[32];
	  RandomAccessFile afile;

	  try { //read of random access file can throw an exception
		  afile = new RandomAccessFile(filename + ".gri", "r");
		  
		  if (datatype == "BYTE") {
			  size = 1;
			  for (i = 0; i < length; i++) {
				  pos = getcellnumber(points[i][0],points[i][1]);
				  if(pos >= 0){
					  afile.seek(pos * size);
					  ret[i] = afile.readByte();
				  }else{
					  ret[i] = Double.NaN;        		
				  }
			  }
		  } else if (datatype == "SHORT") {
			  size = 2;
			  for (i = 0; i < length; i++) {
				  pos = getcellnumber(points[i][0],points[i][1]);
				  if(pos >= 0){
					  afile.seek(pos * size);
					  if (byteorderLSB) {                    
						  for (j = 0; j < size; j++) {
							  b[j] = afile.readByte();
						  }
						  ret[i] = lsb2short(b);
					  } else {
						  ret[i] = afile.readShort();
					  }        		
				  }else{
					  ret[i] = Double.NaN;        		
				  }
			  }
		  } else if (datatype == "INT") {
			  size = 4;
			  for (i = 0; i < length; i++) {
				  pos = getcellnumber(points[i][0],points[i][1]);
				  if(pos >= 0){
					  afile.seek(pos * size);
					  if (byteorderLSB) {
						  for (j = 0; j < size; j++) {
							  b[j] = afile.readByte();
						  }
						  ret[i] = lsb2int(b);
					  } else {
						  ret[i] = afile.readInt();
					  }
				  }else {
					  ret[i] = Double.NaN;
				  }
			  } 
		  } else if (datatype == "LONG") {
			  size = 8;
			  for (i = 0; i < length; i++) {
				  pos = getcellnumber(points[i][0],points[i][1]);
				  if(pos >= 0){
					  afile.seek(pos*size);
					  if (byteorderLSB) {              
						  for (j = 0; j < size; j++) {
							  b[j] = afile.readByte();
						  }
						  ret[i] = lsb2long(b);
					  } else {
						  ret[i] = afile.readLong();
					  }

				  } else{
					  ret[i] = Double.NaN;
				  }
			  }
		  }  else if (datatype == "FLOAT") {
			  size = 4;
			  for ( i = 0; i < length; i++) {
				  pos = getcellnumber(points[i][0],points[i][1]);
				  if(pos >= 0){

					  afile.seek(pos * size);
					  if (byteorderLSB) {

						  for ( j = 0; j < size; j++) {
							  b[j] = afile.readByte();
						  }
						  ret[i] = lsb2float(b);

					  } else {
						  ret[i] = afile.readFloat();
					  }

				  } else{
					  //System.out.println("missing env: lng=" + points[i][0] + " lat=" + points[i][0] + " pos=" + pos);
					  ret[i] = Double.NaN;
				  }
			  }
		  }      else if (datatype == "DOUBLE") {
			  size = 8;
			  for (i = 0; i < length; i++) {
				  pos = getcellnumber(points[i][0],points[i][1]);
				  if(pos >= 0){

					  afile.seek(pos * size);
					  if (byteorderLSB) {

						  for (j = 0; j < size; j++) {
							  b[j] = afile.readByte();
						  }
						  ret[i] = lsb2double(b);
					  } else {
						  ret[i] = afile.readFloat();
					  }

				  } else {
					  ret[i] = Double.NaN;
				  }
			  }
		  } else {
			  // / should not happen; catch anyway...
			  for (i = 0; i < length; i++) {
				  ret[i] = Double.NaN;
			  }
		  }
		  //replace not a number
		  for (i = 0; i < length; i++) {
			  if((float)ret[i] <= (float)nodatavalue){
				  //System.out.println("replacing missing value: " + ret[i]);
				  ret[i] = Double.NaN;
			  }
		  }
		  
		  afile.close();
	  } catch (Exception e) {
		  //log error - probably a file error
		  //System.out.println("GRID: " + e.toString());
	  }
	  return ret;
  }

  private double lsb2double(byte[] b) {
	  int accum = 0;
	  for (int i = 0; i < 8; i++) {
		  // must cast to long or shift done modulo 32
		  accum |= ((long) (b[i] & 0xff)) << i * 8;
	  }
	  return Double.longBitsToDouble(accum);
  }

  private float lsb2float(byte[] b) {
    int accum = 0;
    for (int i = 0; i < 4; i++) {
      accum |= (b[i] & 0xff) << i * 8;
    }
    return Float.intBitsToFloat(accum);
  }

  private int lsb2int(byte[] b) {
    int accum = 0;
    for (int i = 0; i < 4; i++) {
      accum |= (b[i] & 0xff) << i * 8;
    }
    return accum;
  }

  private long lsb2long(byte[] b) {
    long accum = 0;
    for (int i = 0; i < 8; i++) { // must cast to long or shift done modulo 32
      accum |= (long) (b[i] & 0xff) << i * 8;
    }
    return accum;
  }

  private short lsb2short(byte[] b) {
    return (short) (b[1] << 8 | (b[0] & 0xff));
  }
 
  private void readgrd(String filename) {    
    	IniReader ir = new IniReader(filename + ".grd");
    	
        setdatatype(ir.getStringValue("Data", "DataType"));
        //System.out.println("grd datatype=" + datatype);
    	maxval = ir.getDoubleValue("Data","MaxValue");
    	minval = ir.getDoubleValue("Data","MinValue");
    	ncols = ir.getIntegerValue("GeoReference","Columns");
    	nrows = ir.getIntegerValue("GeoReference","Rows");
    	xmin = ir.getDoubleValue("GeoReference","MinX");
    	ymin = ir.getDoubleValue("GeoReference","MinY");
    	xmax = ir.getDoubleValue("GeoReference","MaxX");
    	ymax = ir.getDoubleValue("GeoReference","MaxY");
    	xres = ir.getDoubleValue("GeoReference","ResolutionX");
    	yres = ir.getDoubleValue("GeoReference","ResolutionY");
    	if(ir.valueExists("Data","NoDataValue")){
    		nodatavalue = ir.getDoubleValue("Data","NoDataValue");
    	}else{
    		nodatavalue = Double.NaN;
    	}
    	
    	String s = ir.getStringValue("Data", "ByteOrder");

      byteorderLSB = true;
      if (s != null && s.length() > 0) {
        if (s.equals("MSB")) {
          byteorderLSB = false;
        }// default is windows (LSB), not linux or Java (MSB)
      }
    
  }

}