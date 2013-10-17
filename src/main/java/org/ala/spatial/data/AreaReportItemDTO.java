package org.ala.spatial.data;
/**
 * A DTO for the information that is necessary to display an item of the Area 
 * Report.
 * 
 * @author Natasha Carter (natasha.carter@csiro.au)
 *
 */
public class AreaReportItemDTO {
    /** The title for the area report item */
    private String title;
    /** The count associated with the item - second item to be displayed in table */
    private String count;
    /** Indicates which buttons should be included in the area report item */
    private ExtraInfoEnum[] extraInfo;
    /** The URL to display as a link */
    private String url;
    /** The title to display for the URL link */
    private String urlTitle;
    /** Whether or not the item should only consider geospatially kosher records */
    private boolean geospatialKosher = false;
    /** whether or not the area report item represents an endemic option */
    private boolean endemic = false;
    /** The type of list action to be performed */
    private ListType listType;
    /** The extra params to apply to the queries... */
    private String extraParams;
    public enum ExtraInfoEnum{
        LIST,MAP_ALL,SAMPLE
    }
    public enum ListType{
        SPECIES, DISTRIBUTION, AREA_CHECKLIST, SPECIES_CHECKLIST, BIOSTOR
    }
    
    public AreaReportItemDTO(){
        
    }
    public AreaReportItemDTO(String title){
        this.title = title;
        this.count = "Loading...";
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
        this.extraInfo = extraInfo;
    }
    /**
     * @return the url
     */
    public String getUrl() {
        return url;
    }
    /**
     * @param url the url to set
     */
    public void setUrl(String url) {
        this.url = url;
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
     * @return the urlTitle
     */
    public String getUrlTitle() {
        return urlTitle;
    }
    /**
     * @param urlTitle the urlTitle to set
     */
    public void setUrlTitle(String urlTitle) {
        this.urlTitle = urlTitle;
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
    public boolean isLoading(){
        return count.equals("Loading...");
    }
    
}
