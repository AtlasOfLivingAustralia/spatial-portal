package au.org.ala.spatial.dto;

import au.org.ala.spatial.StringConstants;

import java.util.Map;

/**
 * A dto for the information that is necessary to display an item of the Area
 * Report.
 *
 * @author Natasha Carter (natasha.carter@csiro.au)
 */
public class AreaReportItemDTO {
    /**
     * The title for the area report item
     */
    private String title;
    /**
     * The count associated with the item - second item to be displayed in table
     */
    private String count;
    /**
     * Indicates which buttons should be included in the area report item
     */
    private ExtraInfoEnum[] extraInfo;
    /**
     * A map of URL titles to urls
     */
    private Map<String, String> urlDetails;
    /**
     * Whether or not the item should only consider geospatially kosher records
     */
    private boolean geospatialKosher = false;
    /**
     * whether or not the area report item represents an endemic option
     */
    private boolean endemic = false;
    /**
     * The type of list action to be performed
     */
    private ListType listType;
    /**
     * The extra params to apply to the queries...
     */
    private String extraParams;

    public AreaReportItemDTO() {

    }

    public AreaReportItemDTO(String title) {
        this.title = title;
        this.count = StringConstants.LOADING;
    }

    /**
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * @param title the title to set
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * @return the count
     */
    public String getCount() {
        return count;
    }

    /**
     * @param count the count to set
     */
    public void setCount(String count) {
        this.count = count;
    }

    /**
     * @return the extraInfo
     */
    public ExtraInfoEnum[] getExtraInfo() {
        return extraInfo;
    }

    /**
     * @param extraInfo the extraInfo to set
     */
    public void setExtraInfo(ExtraInfoEnum[] extraInfo) {
        this.extraInfo = extraInfo == null ? null : extraInfo.clone();
    }

    /**
     * @return the geospatialKosher
     */
    public boolean isGeospatialKosher() {
        return geospatialKosher;
    }

    /**
     * @param geospatialKosher the geospatialKosher to set
     */
    public void setGeospatialKosher(boolean geospatialKosher) {
        this.geospatialKosher = geospatialKosher;
    }

    /**
     * @return the listType
     */
    public ListType getListType() {
        return listType;
    }

    /**
     * @param listType the listType to set
     */
    public void setListType(ListType listType) {
        this.listType = listType;
    }

    /**
     * @return the endemic
     */
    public boolean isEndemic() {
        return endemic;
    }

    /**
     * @param endemic the endemic to set
     */
    public void setEndemic(boolean endemic) {
        this.endemic = endemic;
    }

    /**
     * @return the extraParams
     */
    public String getExtraParams() {
        return extraParams;
    }

    /**
     * @param extraParams the extraParams to set
     */
    public void setExtraParams(String extraParams) {
        this.extraParams = extraParams;
    }

    public boolean isLoading() {
        return StringConstants.LOADING.equals(count);
    }

    /**
     * @return the urlDetails
     */
    public Map<String, String> getUrlDetails() {
        return urlDetails;
    }

    /**
     * @param urlDetails the urlDetails to set
     */
    public void setUrlDetails(Map<String, String> urlDetails) {
        this.urlDetails = urlDetails;
    }

    public void addUrlDetails(String title, String url) {
        if (urlDetails == null) {
            urlDetails = new java.util.LinkedHashMap<String, String>();
        }
        urlDetails.put(title, url);
    }

    public enum ExtraInfoEnum {
        LIST, MAP_ALL, SAMPLE
    }

    public enum ListType {
        SPECIES, DISTRIBUTION, AREA_CHECKLIST, SPECIES_CHECKLIST, JOURNAL_MAP
    }
}
