package org.ala.spatial.analysis.tabulation;

import java.util.List;

import org.ala.spatial.util.Layer;

/**
 * Tabulation code
 *
 * @author ajayr
 */
public interface TabulationService {
    public List<Layer> getContextualLayers();

	public List<Layer> getEnvironmentalLayers();

	public void tabulate(String species, String[] layers);

	public char[] getResults();
}
