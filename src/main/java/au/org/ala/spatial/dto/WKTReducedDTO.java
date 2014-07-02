package au.org.ala.spatial.dto;

/**
 * Created by a on 6/05/2014.
 */
public class WKTReducedDTO {
    String originalWKT;
    String reducedWKT;
    String reducedBy;

    public WKTReducedDTO(String originalWKT, String reducedWKT, String reducedBy) {
        this.originalWKT = originalWKT;
        this.reducedWKT = reducedWKT;
        this.reducedBy = reducedBy;
    }

    public String getOriginalWKT() {
        return originalWKT;
    }

    public void setOriginalWKT(String originalWKT) {
        this.originalWKT = originalWKT;
    }

    public String getReducedWKT() {
        return reducedWKT;
    }

    public void setReducedWKT(String reducedWKT) {
        this.reducedWKT = reducedWKT;
    }

    public String getReducedBy() {
        return reducedBy;
    }

    public void setReducedBy(String reducedBy) {
        this.reducedBy = reducedBy;
    }
}
