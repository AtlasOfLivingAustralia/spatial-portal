package org.ala.spatial.analysis.index;

import org.ala.spatial.analysis.service.ShapeIntersectionService;
import org.ala.spatial.util.Grid;
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
    public static void main(String[] args) {
        int i;
        System.out.println("org.ala.spatial.analysis.index.Indexing: start");

        if (args.length > 0 && args[0].equals("build_all")) {
            System.out.println("building all");

             TabulationSettings.load();
             OccurrencesCollection.init();
             DatasetMonitor dm = new DatasetMonitor();
             dm.initDatasetFiles();     //this performs require updates
        } else if (args.length > 0 && args[0].equals("layer_distances")) {
            TabulationSettings.load();

            LayerDistanceIndex ldi = new LayerDistanceIndex();
            ldi.occurancesUpdate();
        } else if (args.length > 0 && args[0].equals("test_grid_min_max")) {
            TabulationSettings.load();
            for (Layer l : TabulationSettings.environmental_data_files) {
                Grid g = new Grid(TabulationSettings.environmental_data_path + l.name);
                g.printMinMax();
            }
        } else if (args.length > 0 && args[0].equals("shape_distributions")) {
            TabulationSettings.load();
            ShapeIntersectionService.init();
        } else {

            /* print usage */
            String[] usage = {
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
                "       generate layer indexes file in indexes output directory",
                "	",
                "test_grid_min_max",
                "       load each grid file and test min/max in header with",
                "       min/max in loaded data",
                "shape_distributions",
                "       index shape distributions"
            };

            for (String s : usage) {
                System.out.println(s);
            }
        }

        System.out.println("org.ala.spatial.analysis.index.Indexing: end");
    }
}
