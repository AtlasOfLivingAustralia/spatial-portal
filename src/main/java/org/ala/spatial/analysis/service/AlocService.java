package org.ala.spatial.analysis.service;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.ala.spatial.analysis.method.Aloc;
import org.ala.spatial.analysis.method.Pca;
import org.ala.spatial.util.Grid;
import org.ala.spatial.util.Layer;
import org.ala.spatial.util.SimpleRegion;
import org.ala.spatial.util.TabulationSettings;

/**
 * entry into running Aloc
 * 
 * use run
 * 
 * @author adam
 *
 */
public class AlocService {
	
	/**
	 * runs an ALOC classification
	 * @param filename output filename for image out
	 * @param layers list of layers to include in ALOC as Layer[]
	 * @param numberofgroups number groups to generate as int
	 * @param region option restrictive region
	 * @param id session id as String
	 * @return groups as int[] TODO: ???
	 */
	public static int[] run(String filename, Layer[] layers, int numberofgroups, SimpleRegion region,String id) {
    	TabulationSettings.load();
    	
    	/* get data, remove missing values, restrict by optional region */
        int i, j;
        float[][] data = null;
        j = 0;
        int width = 252, height = 210;
        String layerPath = TabulationSettings.environmental_data_path;
        if (layerPath == null) layerPath = "";
        Grid grid = null;
        for (Layer l : layers) {

            grid = new Grid(layerPath + l.name);
            
            width = grid.ncols;
            height = grid.nrows;
 
            double[] d = grid.getGrid();
            if (data == null) {
                data = new float[d.length][layers.length];
            }
            for (i = 0; i < d.length && i < data.length; i++) {
                data[i][j] = (float) d[i];
            }
            j++;
        }

        float[][] data_clean = null;
        
        int count = 0;
    	int [] mapping = null;
    	
        if (data != null && grid != null) {   
        	if(region != null){
	            //roll out missing values and values not in region
	        	int [][] cells = region.getOverlapGridCells(
	        			grid.xmin, grid.ymin, 
	        			grid.xmax, grid.ymax, 
	        			grid.ncols, grid.nrows, 
	        			null);
		        	
				for (i = 0; i < cells.length; i++) {
				    for (j = 0; j < data[0].length; j++) {
				        if (Float.isNaN(data[cells[i][0] + (height-cells[i][1]-1)*width][j])) {
				            break;
				        }
				    }
				    if (j == data[0].length) {
				        count++;
				    }
				}
				
				data_clean  = new float[count][data[0].length];
	            mapping = new int[count];
	            count = 0;
	            for (i = 0; i < cells.length; i++) {
	                for (j = 0; j < data[0].length; j++) {
	                	if (Float.isNaN(data[cells[i][0] + (height-cells[i][1]-1)*width][j])) {
	                        break;
	                    }
	                }
	                if (j == data[0].length) {
	                    for (j = 0; j < data[0].length; j++) {
	                        data_clean[count][j] = data[cells[i][0] + (height-cells[i][1]-1)*width][j];
	                        mapping[count] = cells[i][0] + (height-cells[i][1]-1)*width;
	                    }
	                    count++;
	                }
	            }

        	} else {
        		/* code for non-region */
        		for (i = 0; i < data.length; i++) {
                    for (j = 0; j < data[i].length; j++) {
                        if (Float.isNaN(data[i][j])) {
                            break;
                        }
                    }
                    if (j == data[i].length) {
                        count++;
                    }
                }
                data_clean = new float[count][data[0].length];
                mapping = new int[count];
                count = 0;
                for (i = 0; i < data.length; i++) {
                    for (j = 0; j < data[i].length; j++) {
                        if (Float.isNaN(data[i][j])) {
                            break;
                        }
                    }
                    if (j == data[i].length) {
                        for (j = 0; j < data[i].length; j++) {
                            data_clean[count][j] = data[i][j];
                            mapping[count] = i;
                        }
                        count++;
                    }
                }

        	}
            
        	/* run aloc */
            int[] groups = Aloc.runGowerMetric(data_clean, numberofgroups);

            /* calculate group means */
            double[][] group_means = new double[numberofgroups][data_clean[0].length];
            int[] group_counts = new int[numberofgroups];

            /* TODO: handle numerical overflow when calculating means */
            for (i = 0; i < groups.length; i++) {
                group_counts[groups[i]]++;
                for (j = 0; j < data_clean[i].length; j++) {
                    group_means[groups[i]][j] += data_clean[i][j];
                }
            }            
            for (i = 0; i < group_means.length; i++) {
                /* TODO: this check needs to be removed */
                if (group_counts[i] > 0) {
                    for (j = 0; j < group_means[i].length; j++) {
                        group_means[i][j] /= group_counts[i];
                    }
                }
            }

            /* get RGB for colouring group means via PCA */
            int[][] colours = Pca.getColours(group_means);
            
            /* export means + colours */
            exportMeansColours(filename + ".csv",group_means,colours,layers);
            
            /* export geoserver sld file for legend */
            exportSLD(filename + ".sld",group_means,colours,layers,id);

            /* map back as colours, grey scale for now */
            BufferedImage image = new BufferedImage(width, height,
                    BufferedImage.TYPE_INT_ARGB);
            int[] image_bytes;

            image_bytes = image.getRGB(0, 0, image.getWidth(), image.getHeight(),
                    null, 0, image.getWidth());

            /* try transparency as missing value */            
            for (i = 0; i < image_bytes.length; i++) {
                image_bytes[i] = 0x00000000;
            }
             
            /* write out onto imagebytes grouping colours per cell */
            int group;
            int[] colour = new int[3];
            for (i = 0; i < groups.length; i++) {
                group = groups[i];
                for (j = 0; j < colour.length; j++) {
                    colour[j] = (int) (colours[groups[i]][j]);
                }

                //set up rgb colour for this group
                image_bytes[mapping[i]] = 0xff000000 | ((colour[0] << 16) | (colour[1] << 8) | colour[2]);
                
            }

            /* write bytes to image */
            image.setRGB(0, 0, image.getWidth(), image.getHeight(),
                    image_bytes, 0, image.getWidth());

            /* save image */
            try {
                ImageIO.write(image, "png",
                        new File(filename));
            } catch (IOException e) {
              
            }

            return groups;
        }

        return null;
    }
    
	/**
	 * exports means and colours of a classification (ALOC) into
	 * a csv
	 * 
	 * @param filename csv filename to export into
     * @param means mean values for each legend record as [n][m]
     * where 
     * 	n is number of records
     *  m is number of layers used to generate the classification (ALOC)
     * @param colours RGB colours of legend records as [n][3] 
     * where 
     *  n is number of records
     *  [][0] is red
     *  [][1] is green
     *  [][2] is blue
     * @param layers layers used to generate the classification as Layer[]
	 */
    static void exportMeansColours(String filename, double [][] means, int [][] colours, Layer [] layers){
    	try {
    		FileWriter fw = new FileWriter(filename);
    		int i,j;
    		
    		/* header */
    		fw.append("group number");
    		fw.append("red");
    		fw.append("green");
    		fw.append("blue");
    		for (i = 0; i < layers.length; i++) {
    			fw.append(",");
    			fw.append(layers[i].display_name);    			
    		}
    		fw.append("\r\n");
    		
    		/* outputs */
    		for (i = 0; i < means.length; i++) {
    			fw.append(String.valueOf(i));
    			fw.append(",");
    			fw.append(String.valueOf(colours[i][0]));
    			fw.append(",");
    			fw.append(String.valueOf(colours[i][1]));
    			fw.append(",");
    			fw.append(String.valueOf(colours[i][2]));
    			
    			for (j = 0; j < means[i].length; j++) {
    				fw.append(",");
    				fw.append(String.valueOf(means[i][j]));
    			}
    			
    			fw.append("\r\n");
    		}
    		
    		fw.close();
    	} catch(Exception e) {
  		
    	}
    }
   

    /**
     * exports a geoserver sld file for legend generation
     * 
     * TODO: find out why it reports error when attached layer is called
     * 
     * @param filename sld filename to export into
     * @param means mean values for each legend record as [n][m]
     * where 
     * 	n is number of records
     *  m is number of layers used to generate the classification (ALOC)
     * @param colours RGB colours of legend records as [n][3] 
     * where 
     *  n is number of records
     *  [][0] is red
     *  [][1] is green
     *  [][2] is blue
     * @param layers layers used to generate the classification as Layer[]
     * @param id unique id (likely to be session_id) as String
     */
    static void exportSLD(String filename, double [][] means, int [][] colours, Layer [] layers, String id){
    	try {
    		StringBuffer sld = new StringBuffer();
    		sld.append("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>");
    		
    		/* header */
    		sld.append("<StyledLayerDescriptor version=\"1.0.0\" xmlns=\"http://www.opengis.net/sld\" xmlns:ogc=\"http://www.opengis.net/ogc\"");
			sld.append(" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
			sld.append(" xsi:schemaLocation=\"http://www.opengis.net/sld http://schemas.opengis.net/sld/1.0.0/StyledLayerDescriptor.xsd\">");
			sld.append(" <NamedLayer>");
			sld.append(" <Name>aloc_" + id + "</Name>");
			sld.append(" <UserStyle>");
			sld.append(" <Name>aloc_" + id + "</Name>");
			sld.append(" <Title>ALA ALOC distribution</Title>");
			sld.append(" <FeatureTypeStyle>");
			sld.append(" <Rule>");
			sld.append(" <RasterSymbolizer>");
			sld.append(" <ColorMap type=\"values\" >");
		   							    		
    		int i,j;
    		String s;   		
    		  		
    		/* outputs */
    		for (i = 0; i < colours.length; i++) {	
    			j = 0x00000000 | ((colours[i][0] << 16) | (colours[i][1] << 8) | colours[i][2]);    			
    			s = Integer.toHexString(j).toUpperCase();
    			while (s.length() < 6) {
    				s = "0" + s;
    			}
    			sld.append("<ColorMapEntry color=\"#" + s + "\" quantity=\"" + (i+1) + ".0\" label=\"group " + (i+1) + "\" opacity=\"1\"/>\r\n");
    		}
    		
    		/* footer */
    		sld.append("</ColorMap></RasterSymbolizer></Rule></FeatureTypeStyle></UserStyle></NamedLayer></StyledLayerDescriptor>");
    		
    		/* write */
    		FileWriter fw = new FileWriter(filename);
    		fw.append(sld.toString());
    		fw.close();
    	} catch (Exception e) {
    		
    	}
    }
}