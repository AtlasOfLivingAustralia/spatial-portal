package org.ala.spatial.analysis.tabulation;

import java.io.RandomAccessFile;

import org.ala.spatial.util.*;

/**
 * index interface, standalone and integrated
 * 
 * can be called as separate program since indexing could take a long time
 * 
 */
public class Indexing{	
	
	/**
	 * build_all
	 * build_layer <layer name>
	 * 
	 * list_layers
	 * filter_species <partial species name>
	 * 
	 * sample <species name> [<layer name>]
	 * 
	 * @param args
	 */
	public static void main(String [] args){
		int i;
		System.out.println("org.ala.spatial.analysis.tabulation.Indexing: start");
		
		if(args.length > 0 && args[0].equals("build_all")){
			System.out.println("building all");
			
			/* some indexes have dependances on others so order is important */
			OccurancesIndex occurancesIndex = new OccurancesIndex();
			occurancesIndex.occurancesUpdate();
			
			SamplingIndex samplingIndex = new SamplingIndex();
			samplingIndex.occurancesUpdate();
			
			SpeciesListIndex speciesListIndex = new SpeciesListIndex();
			speciesListIndex.occurancesUpdate();
		}else if(args.length > 1 && args[0].equals("build_layer")){
			for(i=1;i<args.length;i++){
				System.out.println("building layer: " + args[i]);
				
				SamplingIndex samplingIndex = new SamplingIndex();
				samplingIndex.layersUpdate(args[i].replace('_',' ')); //reverse ' ' to '_'
				
				SpeciesListIndex speciesListIndex = new SpeciesListIndex();
				speciesListIndex.layersUpdate(args[i].replace('_',' ')); //reverse ' ' to '_'
			}
		}else if(args.length > 0 && args[0].equals("list_layers")){
			System.out.println("listing layers");
			
			SamplingService ss = new SamplingService();
			
			for(String s : ss.listLayers()){
				System.out.println(" " + s.replace(' ','_')); //' ' to '_'
			}
		}else if(args.length > 1 && args[0].equals("filter_species")){
			System.out.println("filtering species");
			
			String filter = args[1];
			
			for(i=2;i<args.length;i++){
				filter += " " + args[i];
			}
			
			System.out.println("filter:" + filter);
			
			SamplingService ss = new SamplingService();
			
			String [] list = ss.filterSpecies(filter,5000000);
			if(list != null){
				System.out.println("found: " + list.length);
				for(String s : list){
					System.out.println(" " + s);
				}
			}else{
				System.out.println("found: 0");
			}
		}else if(args.length > 1 && args[0].equals("sample")){
			System.out.println("sampling");
			
			SamplingService ss = new SamplingService();
			
			String [] layers = null;
			String species = args[1];
						
			for(i=2;i<args.length 
				&& args[i].length() > 0 && args[i].charAt(0) != ',' 
				&& args[i-1].length() > 0 && args[i-1].charAt(args[i-1].length()-1) != ','
				;i++){
				species += " " + args[i];
			}
			int istart = i; 		//for layers start idx
			
			/* remove any terminating ',' */
			if(species.length() > 0
				&& species.charAt(species.length()-1) == ','){
				species = species.substring(0,species.length()-1);
			}
			System.out.println("species=" + species);
			
			if(args.length > istart){
				layers = new String[args.length-istart];
				
				for(;i<args.length;i++){		//carry over 'i' from previous loop
					layers[i-istart] = args[i].replace('_',' ');	//reverse ' ' to '_'
					System.out.println("layer" + (1+i-istart) + "=" + args[i]);
				}	
			}
			
			System.out.println(ss.sampleSpecies(species, layers));
		}else if(args.length > 1 && args[0].equals("species_list")){
			System.out.println("species listing: " + args[1] + " argslength=" + args.length);
			SPLFilter [] filters = new SPLFilter[args.length-1];
			for(i=1;i<args.length;i++){
				String [] line = args[i].split(":");
		System.out.println("(" + i + ") terms count=" + line.length);
				filters[i-1] = new SPLFilter();
				filters[i-1].layername = 
					SamplingService.layerDisplayNameToName(
							line[0].replace('_',' '));		//TODO: move this
				if(line.length > 2){
					/* min/max */
					try{
						filters[i-1].minimum_value = Double.parseDouble(line[1]);
						filters[i-1].maximum_value = Double.parseDouble(line[2]);
					}catch(Exception e){
						System.out.println("in layer filter: " + args[i] 
						+ ", cannot parse " + line[1] + " or " + line[2]);
					}
				}else{
					/* catagorical */
					String [] catagories = line[1].split(",");
					filters[i].catagories = new int[catagories.length];
					try{
						for(int j=0;j<catagories.length;j++){
							filters[i-1].catagories[j] = Integer.parseInt(catagories[j]);
						}
					}catch(Exception e){
						(new SpatialLogger()).log(
								"failure to parse comma separated term in  " + line[1]);
					}
				}	
			}
			/*
			 * TODO: make SpeciesListService
			 */
			for(SPLFilter f : filters){
				System.out.println("filter: "
						+ f.layername + "," 
						+ f.catagories + "," 
						+ f.minimum_value + ","
						+ f.maximum_value);
			}
			String output = SpeciesListIndex.listSpecies(filters);
			System.out.println(">" + output);				
						
		}else if(args.length > 1 && args[0].equals("layer_extents")){
			System.out.println("list layer extends for " + args[1]);
			
			/*
			 * TODO: tidy up layerDisplayNameToName call
			 */
			String output = SpeciesListIndex.getLayerExtents(
					SamplingService.layerDisplayNameToName(
							args[1].replace('_',' ')));
			
			System.out.println(">" + output);
			/*
			"layer_filter_extents <layer_name>",
			"	print out catagories and their values, or min and max layer values"
			};*/
		}else{
			/* print usage */
			String [] usage = {
				"usage:",
				"	java -Xmx1000m -cp :postgresdriver.jar org.ala.spatial.analysis.tabulation.Indexing <command>",
				"",
				"- memory usage depends on data				",
				"- ensure postgresdriver.jar is available and with the actual filename.",
				"			",
				"commands:",
				"",
				"build_all",
				"	runs all index building functions - can take many hours.",
				"	",
				"build_layer <layer name>",
				"	runs index building functions on one updated layer by name.",
				"	can be a new layer.",
				"	",
				"list_layers",
				"	print out all indexed layers.",
				"	",
				"filter_species <partial species name>",
				"	print out all species names beginning with <partial species name>.",
				"	",
				"sample <species name> [,<layer names>]",
				"    print out csv format records for <speces name> and corresponding",
				"    data from <layer names>.",
				"",
				"species_list [<layer_name>:1,2,3,4] | [<layer_name>:1.2:3.4]",
				"	print out species list for supplied layer filters",
				"	1,2,3,4 represents catagory numbers",
				"	1.2:3.4 represents minimum of 1.2 and maximum of 3.4",
				"",
				"layer_extents <layer_name>",
				"	print out catagories and their values, or min and max layer values",
				"	useful for species_list layer filter parameters"
				};
			
			for(String s : usage){
				System.out.println(s);
			}
		}
			
		
		System.out.println("org.ala.spatial.analysis.tabulation.Indexing: end");
	}
}
