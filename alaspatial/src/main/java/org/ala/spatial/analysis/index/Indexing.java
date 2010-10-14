package org.ala.spatial.analysis.index;

import org.ala.spatial.util.Layer;
import org.ala.spatial.util.TabulationSettings;

/**
 * index interface, standalone and integrated
 * 
 * can be called as separate program since indexing could take a long time
 * 
 * TODO: update for incremental update and new service classes
 * 
 */
public class Indexing {	

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
	System.out.println("org.ala.spatial.analysis.index.Indexing: start");
		
		if(args.length > 0 && args[0].equals("build_all")){
			System.out.println("building all");
			
			/* some indexes have dependances on others so order is important */
			OccurrencesIndex occurancesIndex = new OccurrencesIndex();
			occurancesIndex.occurancesUpdate();
			
			SamplingIndex samplingIndex = new SamplingIndex();
			samplingIndex.occurancesUpdate();
			
			FilteringIndex speciesListIndex = new FilteringIndex();
			speciesListIndex.occurancesUpdate();
                }else if(args.length > 0 && args[0].equals("build_sampling")){
			SamplingIndex samplingIndex = new SamplingIndex();
			samplingIndex.occurancesUpdate();
                }else if(args.length > 0 && args[0].equals("build_filtering")){
			FilteringIndex speciesListIndex = new FilteringIndex();
			speciesListIndex.occurancesUpdate();
		}else if(args.length > 1 && args[0].equals("build_layer")){
			for(i=1;i<args.length;i++){
				System.out.println("building layer: " + args[i]);
				
				SamplingIndex samplingIndex = new SamplingIndex();
				samplingIndex.layersUpdate(args[i].replace('_',' ')); //reverse ' ' to '_'
				
				FilteringIndex speciesListIndex = new FilteringIndex();
				speciesListIndex.layersUpdate(args[i].replace('_',' ')); //reverse ' ' to '_'
			}
		}else if(args.length > 0 && args[0].equals("layer_distances")){
                    TabulationSettings.load();

                    LayerDistanceIndex ldi = new LayerDistanceIndex();
                    ldi.occurancesUpdate();
                }else {

			/* print usage */
			String [] usage = {
				"usage:",
				"	java -Xmx1800m org.ala.spatial.analysis.index.Indexing <command>",
				"",
				"- memory usage depends on data				",	
				"- tabulation_settings.xml must be up to date",
				"			",
				"commands:",
				"",
				"build_all",
				"	runs all index building functions.",
                                "       memory usage dependant on number occurrences data and ",
                                "       number of indexes required.",
				"	",
                                "build_sampling",
				"	runs sampling index building functions.",
				"	",
                                "build_filtering",
				"	runs filtering index building functions.",
				"	",
				"build_layer <layer name>",
				"	runs index building functions on one updated layer by name.",
				"	can be a new layer.",
				"	",				
                                "layer_distances",
                                "       generate layer indexes file in indexes output directory"
				};
			
			for(String s : usage){
				System.out.println(s);
			}
		}			
		
		System.out.println("org.ala.spatial.analysis.index.Indexing: end");
	}
}
