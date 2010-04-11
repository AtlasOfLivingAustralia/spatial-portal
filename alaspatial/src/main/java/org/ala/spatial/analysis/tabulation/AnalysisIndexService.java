

package org.ala.spatial.analysis.tabulation;

/**
 * interface for indexing of point, grid and shape files for analysis
 *
 * @author adam
 *
 */
public interface AnalysisIndexService {

	/**
	 * performs update of 'indexing' for new points data
	 */
	public void occurancesUpdate();

	/**
	 * performs update of 'indexing' for a new layer (grid or shapefile)
	 *
	 * @param layername name of the layer to update as String.  To update
	 * all layers use null.
	 */
	public void layersUpdate(String layername);

	/**
	 * method to determine if the index is up to date
	 *
	 * @return true if index is up to date
	 */
	public boolean isUpdated();

}
