package org.ala.layers.dto;

public class AttributionDTO {

    String websiteUrl;
    String name;
    String uid;
    String rights;
    String licenseType;
    String licenseVersion;
    String alaPublicUrl;

    public String getAlaPublicUrl() {
        return alaPublicUrl;
    }

    public void setAlaPublicUrl(String alaPublicUrl) {
        this.alaPublicUrl = alaPublicUrl;
    }

    public String getWebsiteUrl() {
        return websiteUrl;
    }

    public void setWebsiteUrl(String websiteUrl) {
        this.websiteUrl = websiteUrl;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getRights() {
        return rights;
    }

    public void setRights(String rights) {
        this.rights = rights;
    }

    public String getLicenseVersion() {
        return licenseVersion;
    }

    public void setLicenseVersion(String licenseVersion) {
        this.licenseVersion = licenseVersion;
    }

    public String getLicenseType() {
        return licenseType;
    }

    public void setLicenseType(String licenseType) {
        this.licenseType = licenseType;
    }
}
