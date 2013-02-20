package org.ala.layers.dto;

public class MapDTO {

    Boolean available = false;
    String url;
    String dataResourceUID;
    String dataResourceUrl;
    String dataResourceName;
    String licenseType;
    String licenseVersion;
    String rights;
    String metadataUrl;

    public Boolean getAvailable() {
        return available;
    }

    public void setAvailable(Boolean available) {
        this.available = available;
    }

    public String getLicenseType() {
        return licenseType;
    }

    public void setLicenseType(String licenseType) {
        this.licenseType = licenseType;
    }

    public String getLicenseVersion() {
        return licenseVersion;
    }

    public void setLicenseVersion(String licenseVersion) {
        this.licenseVersion = licenseVersion;
    }

    public String getMetadataUrl() {
        return metadataUrl;
    }

    public void setMetadataUrl(String metadataUrl) {
        this.metadataUrl = metadataUrl;
    }

    public String getRights() {
        return rights;

    }

    public void setRights(String rights) {
        this.rights = rights;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getDataResourceUID() {
        return dataResourceUID;
    }

    public void setDataResourceUID(String dataResourceUID) {
        this.dataResourceUID = dataResourceUID;
    }

    public String getDataResourceUrl() {
        return dataResourceUrl;
    }

    public void setDataResourceUrl(String dataResourceUrl) {
        this.dataResourceUrl = dataResourceUrl;
    }

    public String getDataResourceName() {
        return dataResourceName;
    }

    public void setDataResourceName(String dataResourceName) {
        this.dataResourceName = dataResourceName;
    }
}
